package com.familyrecipes.android.ui.fridge

import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.familyrecipes.android.R
import com.familyrecipes.android.data.local.PreferenceManager
import com.familyrecipes.android.data.model.ReceiptItem
import com.familyrecipes.android.data.model.ReceiptImportRequest
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.design.Ui
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.UUID

/** Local, account/kitchen-scoped draft. A submitted batch is immutable until its result is known. */
class ReceiptActivity : AppCompatActivity() {
    private data class Row(var name:String="", var quantity:String="", var storage:String="", var days:String="",var ingredientId:Long?=null,var expiry:String="")
    private data class Draft(var rows:MutableList<Row> = mutableListOf(), var image:String?=null, var cameraImage:String?=null, var pending:ReceiptImportRequest?=null)
    private val mode get()=if(intent.getStringExtra("mode")=="item")"item" else "receipt"
    private val scopeAccount=com.familyrecipes.android.data.local.LocalStore.account
    private val account=PreferenceManager.userId
    private val server=PreferenceManager.baseUrl.trimEnd('/')
    private var kitchen=PreferenceManager.currentKitchenId
    private val json=Gson()
    private val prefs by lazy { getSharedPreferences("receipt_drafts",MODE_PRIVATE) }
    private val key get()="receipt:$server:$account:$kitchen:$mode"
    private var draft=Draft()
    private var busy=false
    private var expandedRow:Int?=null
    private lateinit var body:LinearLayout
    private val cameraPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted)takePhoto() else Toast.makeText(this,R.string.receipt_camera_permission,Toast.LENGTH_LONG).show()
    }
    private val gallery=registerForActivityResult(ActivityResultContracts.GetContent()) { uri -> if(uri!=null) prepare(uri) }
    private val camera=registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path=draft.cameraImage;draft.cameraImage=null;persist()
        if(path!=null) {if(ok)prepare(Uri.fromFile(File(path))) else File(path).delete()}
    }
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        if(account<=0 || !com.familyrecipes.android.util.AuthUtil.requireLogin(this)){finish();return}
        kitchen=state?.getLong("receipt_kitchen")?.takeIf{it>0}?:kitchen
        initialize(state)
    }
    private fun loading(state:Bundle?,failed:Boolean=false){
        val root=Ui.box(this);root.addView(Ui.back(this){finish()})
        root.addView(Ui.text(this,getString(if(failed)R.string.inventory_loading_failed else R.string.inventory_loading),18f))
        if(failed)root.addView(Ui.button(this,getString(R.string.social_retry),true){initialize(state)}) else root.addView(android.widget.ProgressBar(this))
        setContentView(root)
    }
    private fun initialize(state:Bundle?){
        loading(state)
        lifecycleScope.launch{
            if(kitchen==null){try{kitchen=Ui.data(ApiClient.getAccountService(scopeAccount).getKitchens()).firstOrNull()?.id;PreferenceManager.currentKitchenId=kitchen}catch(e:CancellationException){throw e}catch(e:Exception){loading(state,true);return@launch}}
            if(!currentAccount()){finish();return@launch}
            if(kitchen==null){loading(state,true);return@launch}
            draft=runCatching { json.fromJson(prefs.getString(key,null),Draft::class.java) }.getOrNull()
                ?:if(mode=="receipt")runCatching { json.fromJson(prefs.getString("receipt:$account:$kitchen",null),Draft::class.java) }.getOrNull()?:Draft() else Draft()
            persist();render()
        }
    }
    override fun onSaveInstanceState(state:Bundle){kitchen?.let{state.putLong("receipt_kitchen",it)};super.onSaveInstanceState(state)}
    private fun persist(){prefs.edit().putString(key,json.toJson(draft)).apply()}
    private fun currentAccount()=account==PreferenceManager.userId&&server==PreferenceManager.baseUrl.trimEnd('/')&&PreferenceManager.isLoggedIn()
    private fun render() {
        val root=Ui.box(this)
        root.addView(Ui.row(this).apply {
            addView(Ui.back(this@ReceiptActivity){finish()})
            addView(Ui.text(this@ReceiptActivity,getString(if(mode=="item")R.string.inventory_item_scan else R.string.receipt_title),22f,true))
        })
        body=Ui.box(this,0)
        body.addView(Ui.text(this,getString(if(mode=="item")R.string.inventory_item_help else R.string.receipt_help),14f))
        if(draft.pending!=null)body.addView(Ui.text(this,getString(R.string.receipt_pending)))
        if(draft.pending==null) {
            body.addView(Ui.button(this,getString(R.string.inventory_gallery)){replace { gallery.launch("image/*") }}.apply {isEnabled=!busy})
            body.addView(Ui.button(this,getString(R.string.inventory_camera)){replace { takePhoto() }}.apply {isEnabled=!busy})
            if(draft.image?.let{File(it).length()>0}==true)body.addView(Ui.button(this,getString(R.string.receipt_retry_scan)){replace{recognize()}}.apply{isEnabled=!busy})
        }
        if(busy)body.addView(Ui.text(this,getString(if(draft.pending==null)R.string.receipt_working else R.string.receipt_saving)))
        val locked=busy || draft.pending!=null
        draft.rows.toList().forEachIndexed { index,row ->
            val card=Ui.card(this)
            val heading=Ui.row(this)
            heading.addView(Ui.field(this,getString(R.string.receipt_name),row.name).apply {
                isEnabled=!locked;doAfterTextChanged{if(row.name!=it.toString()){row.name=it.toString();row.ingredientId=null;persist()}}
            },LinearLayout.LayoutParams(0,-2,2f))
            heading.addView(Ui.field(this,getString(R.string.receipt_quantity),row.quantity).apply {
                isEnabled=!locked;doAfterTextChanged{row.quantity=it.toString();persist()}
            },LinearLayout.LayoutParams(0,-2,1f));card.addView(heading)
            val storageLabels=mapOf("常温" to R.string.receipt_room,"冷藏" to R.string.receipt_cold,"冷冻" to R.string.receipt_frozen)
            val label=storageLabels[row.storage]?.let{getString(it)}?:getString(R.string.receipt_unspecified)
            card.addView(Ui.button(this,"$label · ${row.expiry.ifBlank { row.days.ifBlank { getString(R.string.inventory_unknown_expiry) } }} ▾"){expandedRow=if(expandedRow==index)null else index;render()})
            if(expandedRow==index){
                val values=listOf("","常温","冷藏","冷冻")
                card.addView(Ui.select(this,listOf(R.string.receipt_unspecified,R.string.receipt_room,R.string.receipt_cold,R.string.receipt_frozen).map{getString(it)},values.indexOf(row.storage).coerceAtLeast(0)){i->if(row.storage!=values[i]){row.storage=values[i];row.days="";persist();estimate(row)}}.apply{isEnabled=!locked})
                card.addView(Ui.field(this,getString(R.string.inventory_optional_expiry),row.days).apply{isEnabled=!locked;inputType=android.text.InputType.TYPE_CLASS_NUMBER;doAfterTextChanged{row.days=it.toString();persist()}})
                card.addView(Ui.field(this,getString(R.string.inventory_date_optional),row.expiry).apply{isEnabled=!locked;doAfterTextChanged{row.expiry=it.toString();persist()}})
                if(!locked){
                    card.addView(Ui.button(this,getString(R.string.inventory_select_match)){match(row)})
                    card.addView(Ui.button(this,getString(R.string.receipt_remove)){draft.rows.remove(row);expandedRow=null;persist();render()})
                }
            }
            body.addView(card)
        }
        if(draft.rows.isNotEmpty()) {
            body.addView(Ui.button(this,getString(if(draft.pending==null)R.string.receipt_save else R.string.receipt_retry),true){save()}.apply{isEnabled=!busy})
            if(!locked)body.addView(Ui.button(this,getString(R.string.receipt_clear)){replace{draft.rows.clear();persist();render()}})
        }
        root.addView(Ui.scroll(this,body),LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
    }
    private fun replace(action:()->Unit) {
        if(busy)return
        if(draft.rows.isEmpty())action() else AlertDialog.Builder(this).setMessage(R.string.receipt_replace)
            .setNegativeButton(android.R.string.cancel,null).setPositiveButton(android.R.string.ok){_,_->action()}.show()
    }
    private fun takePhoto() {
        if(androidx.core.content.ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)!=android.content.pm.PackageManager.PERMISSION_GRANTED) {
            cameraPermission.launch(android.Manifest.permission.CAMERA);return
        }
        try {
            val file=File.createTempFile("receipt-camera-",".jpg",cacheDir)
            draft.cameraImage=file.absolutePath;persist()
            camera.launch(FileProvider.getUriForFile(this,"$packageName.fileprovider",file))
        } catch(e:Exception){Ui.toast(this,e)}
    }
    private fun prepare(uri:Uri) {
        if(busy)return
        busy=true;render()
        lifecycleScope.launch {
            try {
                val file=withContext(Dispatchers.IO) {
                    val options=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true}
                    contentResolver.openInputStream(uri).use{android.graphics.BitmapFactory.decodeStream(it,null,options)}
                    require(options.outWidth>0 && options.outHeight>0){getString(R.string.receipt_image_error)}
                    options.inSampleSize=1
                    while(maxOf(options.outWidth,options.outHeight)/options.inSampleSize>2400)options.inSampleSize*=2
                    options.inJustDecodeBounds=false
                    var bitmap=contentResolver.openInputStream(uri).use{android.graphics.BitmapFactory.decodeStream(it,null,options)} ?: error(getString(R.string.receipt_image_error))
                    // Camera JPEGs often use EXIF orientation rather than rotated pixels.
                    val orientation=contentResolver.openInputStream(uri).use { input ->
                        if(input==null)1 else runCatching { android.media.ExifInterface(input).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,1) }.getOrDefault(1)
                    }
                    val matrix=android.graphics.Matrix().apply {
                        when(orientation){2->setScale(-1f,1f);3->setRotate(180f);4->setScale(1f,-1f);5->{setRotate(90f);postScale(-1f,1f)};6->setRotate(90f);7->{setRotate(-90f);postScale(-1f,1f)};8->setRotate(-90f)}
                    }
                    if(orientation in 2..8){val rotated=android.graphics.Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true);if(rotated!==bitmap)bitmap.recycle();bitmap=rotated}
                    val dest=File.createTempFile("receipt-",".jpg",cacheDir)
                    try{dest.outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,90,it)}}finally{bitmap.recycle()}
                    require(dest.length()<=8*1024*1024){getString(R.string.receipt_image_error)}
                    dest
                }
                draft.image?.let{if(it!=file.absolutePath)File(it).delete()};draft.image=file.absolutePath;persist()
                if(uri.scheme=="file" && uri.path?.let{File(it).parentFile==cacheDir && File(it).name.startsWith("receipt-camera-")}==true)File(uri.path!!).delete()
                busy=false;recognize()
            } catch(e:CancellationException){throw e} catch(e:Exception){busy=false;render();Ui.toast(this@ReceiptActivity,e)}
        }
    }
    private fun recognize() {
        if(busy || !currentAccount())return
        val file=draft.image?.let{File(it)} ?: return
        busy=true;render()
        lifecycleScope.launch {
            try {
                val part=MultipartBody.Part.createFormData("image","receipt.jpg",file.asRequestBody("image/jpeg".toMediaType()))
                val items=Ui.data(ApiClient.getAccountVisionService(scopeAccount).recognizeReceipt(kitchen!!,part,mode))
                if(!currentAccount())return@launch
                draft.rows=items.map{Row(it.name.orEmpty(),it.quantity.orEmpty(),it.storage.orEmpty(),it.estimatedDays?.toString().orEmpty(),it.ingredientId,it.expiryDate.orEmpty())}.toMutableList()
                persist()
                if(items.isEmpty())Toast.makeText(this@ReceiptActivity,R.string.receipt_empty,Toast.LENGTH_LONG).show()
            } catch(e:CancellationException){throw e} catch(e:Exception){Ui.toast(this@ReceiptActivity,e)}
            finally{busy=false;render()}
        }
    }
    private fun match(row:Row){
        lifecycleScope.launch {try {
            val result=Ui.data(ApiClient.getAccountService(scopeAccount).resolveIngredientEntries(com.familyrecipes.android.data.model.IngredientEntriesRequest(listOf(com.familyrecipes.android.data.model.IngredientEntry(row.name)),false,row.storage))).first()
            val choices=(listOfNotNull(result.ingredient)+result.candidates).distinctBy{it.id}
            AlertDialog.Builder(this@ReceiptActivity).setTitle(R.string.inventory_select_match)
                .setItems((choices.map{it.name}+getString(R.string.inventory_keep_name)).toTypedArray()){_,i->row.ingredientId=choices.getOrNull(i)?.id;persist();render()}.show()
        }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@ReceiptActivity,e)}}
    }
    private fun estimate(row:Row){
        val storage=row.storage;val name=row.name
        lifecycleScope.launch{try{val result=Ui.data(ApiClient.getAccountService(scopeAccount).estimateIngredient(com.familyrecipes.android.data.model.IngredientEstimateRequest(row.ingredientId,name,storage)))
            if(currentAccount()&&!busy&&draft.pending==null&&draft.rows.contains(row)&&row.storage==storage&&row.name==name){row.days=result.estimatedDays?.toString().orEmpty();persist();render()}
        }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@ReceiptActivity,e)}}
    }
    private fun save() {
        if(busy || !currentAccount())return
        if(draft.pending==null) {
            if(draft.rows.isEmpty() || draft.rows.any{it.name.isBlank() || it.name.length>50 || it.quantity.length>50 || it.storage !in listOf("","常温","冷藏","冷冻") || it.days.isNotBlank() && (it.days.toIntOrNull() ?: 0) !in 1..3650 || it.expiry.isNotBlank() && runCatching{java.time.LocalDate.parse(it.expiry)}.isFailure}) {
                Toast.makeText(this,R.string.inventory_receipt_optional,Toast.LENGTH_LONG).show();return
            }
            draft.pending=ReceiptImportRequest(UUID.randomUUID().toString(),kitchen!!,draft.rows.map{ReceiptItem(it.name.trim(),it.quantity.trim().ifBlank{null},it.storage.ifBlank{null},it.days.toIntOrNull(),it.ingredientId,it.expiry.ifBlank{null})})
            persist()
        }
        busy=true;render()
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO){check(prefs.edit().putString(key,json.toJson(draft)).commit()){ "Unable to save draft" }}
                val response=ApiClient.getAccountService(scopeAccount).importReceipt(draft.pending!!)
                // These responses guarantee no new batch was committed; allow correcting the draft.
                if(response.code() in listOf(400,403,404)) {draft.pending=null;persist()}
                val count=Ui.data(response)
                prefs.edit().remove(key).remove("receipt:$account:$kitchen").apply();draft.image?.let{File(it).delete()}
                Toast.makeText(this@ReceiptActivity,getString(R.string.receipt_success,count),Toast.LENGTH_LONG).show();finish()
            } catch(e:CancellationException){throw e} catch(e:Exception){Ui.toast(this@ReceiptActivity,e)}
            finally{busy=false;if(!isFinishing)render()}
        }
    }
}
