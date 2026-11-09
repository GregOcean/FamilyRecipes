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
    private data class Row(var name:String="", var quantity:String="", var storage:String="", var days:String="")
    private data class Draft(var rows:MutableList<Row> = mutableListOf(), var image:String?=null, var cameraImage:String?=null, var pending:ReceiptImportRequest?=null)
    private val account=PreferenceManager.userId
    private val kitchen=PreferenceManager.currentKitchenId
    private val json=Gson()
    private val prefs by lazy { getSharedPreferences("receipt_drafts",MODE_PRIVATE) }
    private val key get()="receipt:$account:$kitchen"
    private var draft=Draft()
    private var busy=false
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
        if(account<=0 || kitchen==null || !PreferenceManager.isLoggedIn()){finish();return}
        draft=runCatching { json.fromJson(prefs.getString(key,null),Draft::class.java) }.getOrNull() ?: Draft()
        render()
    }
    private fun persist(){prefs.edit().putString(key,json.toJson(draft)).apply()}
    private fun currentAccount()=account==PreferenceManager.userId
    private fun render() {
        val root=Ui.box(this)
        root.addView(Ui.row(this).apply {
            addView(Ui.back(this@ReceiptActivity){finish()})
            addView(Ui.text(this@ReceiptActivity,getString(R.string.receipt_title),22f,true))
        })
        body=Ui.box(this,0)
        body.addView(Ui.text(this,getString(R.string.receipt_help),14f))
        if(draft.pending!=null)body.addView(Ui.text(this,getString(R.string.receipt_pending)))
        if(draft.pending==null) {
            body.addView(Ui.button(this,getString(R.string.receipt_gallery)){replace { gallery.launch("image/*") }}.apply {isEnabled=!busy})
            body.addView(Ui.button(this,getString(R.string.receipt_camera)){replace { takePhoto() }}.apply {isEnabled=!busy})
            if(draft.image?.let{File(it).length()>0}==true)body.addView(Ui.button(this,getString(R.string.receipt_retry_scan)){replace{recognize()}}.apply{isEnabled=!busy})
        }
        if(busy)body.addView(Ui.text(this,getString(if(draft.pending==null)R.string.receipt_working else R.string.receipt_saving)))
        val locked=busy || draft.pending!=null
        draft.rows.toList().forEach { row ->
            val card=Ui.card(this)
            fun field(label:Int,value:String,change:(String)->Unit,numeric:Boolean=false) {
                card.addView(Ui.text(this@ReceiptActivity,getString(label),14f,true))
                card.addView(Ui.field(this@ReceiptActivity,getString(label),value).apply {
                    isEnabled=!locked
                    if(numeric)inputType=android.text.InputType.TYPE_CLASS_NUMBER
                    doAfterTextChanged{change(it.toString());persist()}
                })
            }
            field(R.string.receipt_name,row.name,{row.name=it})
            field(R.string.receipt_quantity,row.quantity,{row.quantity=it})
            card.addView(Ui.text(this,getString(R.string.receipt_storage),14f,true))
            val values=listOf("","常温","冷藏","冷冻")
            card.addView(Ui.select(this,listOf(R.string.receipt_unspecified,R.string.receipt_room,R.string.receipt_cold,R.string.receipt_frozen).map{getString(it)},values.indexOf(row.storage).coerceAtLeast(0)) { i -> row.storage=values[i];persist() }.apply {isEnabled=!locked})
            field(R.string.receipt_days,row.days,{row.days=it},true)
            if(!locked)card.addView(Ui.button(this,getString(R.string.receipt_remove)){draft.rows.remove(row);persist();render()})
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
                val items=Ui.data(ApiClient.getVisionService().recognizeReceipt(kitchen!!,part))
                if(!currentAccount())return@launch
                draft.rows=items.map{Row(it.name.orEmpty(),it.quantity.orEmpty(),it.storage.orEmpty(),it.estimatedDays?.toString().orEmpty())}.toMutableList()
                persist()
                if(items.isEmpty())Toast.makeText(this@ReceiptActivity,R.string.receipt_empty,Toast.LENGTH_LONG).show()
            } catch(e:CancellationException){throw e} catch(e:Exception){Ui.toast(this@ReceiptActivity,e)}
            finally{busy=false;render()}
        }
    }
    private fun save() {
        if(busy || !currentAccount())return
        if(draft.pending==null) {
            if(draft.rows.isEmpty() || draft.rows.any{it.name.isBlank() || it.name.length>50 || it.quantity.isBlank() || it.quantity.length>50 || it.storage !in listOf("常温","冷藏","冷冻") || (it.days.toIntOrNull() ?: 0) !in 1..3650}) {
                Toast.makeText(this,R.string.receipt_invalid,Toast.LENGTH_LONG).show();return
            }
            draft.pending=ReceiptImportRequest(UUID.randomUUID().toString(),kitchen!!,draft.rows.map{ReceiptItem(it.name.trim(),it.quantity.trim(),it.storage,it.days.toInt())})
            persist()
        }
        busy=true;render()
        lifecycleScope.launch {
            try {
                val response=ApiClient.getVisionService().importReceipt(draft.pending!!)
                // These responses guarantee no new batch was committed; allow correcting the draft.
                if(response.code() in listOf(400,403,404)) {draft.pending=null;persist()}
                val count=Ui.data(response)
                prefs.edit().remove(key).apply();draft.image?.let{File(it).delete()}
                Toast.makeText(this@ReceiptActivity,getString(R.string.receipt_success,count),Toast.LENGTH_LONG).show();finish()
            } catch(e:CancellationException){throw e} catch(e:Exception){Ui.toast(this@ReceiptActivity,e)}
            finally{busy=false;if(!isFinishing)render()}
        }
    }
}
