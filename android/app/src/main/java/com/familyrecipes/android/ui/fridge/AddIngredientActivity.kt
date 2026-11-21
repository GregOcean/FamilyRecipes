package com.familyrecipes.android.ui.fridge

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.familyrecipes.android.R
import com.familyrecipes.android.data.local.*
import com.familyrecipes.android.data.model.*
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.design.Ui
import com.familyrecipes.android.util.InventoryImages
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.LocalDate
import java.util.UUID

/** One editable form. Product label stays separate from the canonical food identity. */
class AddIngredientActivity:AppCompatActivity() {
    private data class Draft(var text:String="",var amount:String="",var storage:String="冷藏",var expiry:String="",var image:String?=null,
        var camera:String?=null,var ingredientId:Long?=null,var categoryId:Long?=null,var ownerId:Long?=null,var mutation:String=UUID.randomUUID().toString(),
        var uploaded:String?=null,var pending:FridgeItem?=null,var expiryManual:Boolean=false)
    private val account=LocalStore.account
    private var kitchen=PreferenceManager.currentKitchenId
    private val key get()="inventory-v2:$kitchen"
    private data class LegacyDraft(val text:String?,val image:String?,val reservedFor:Long?,val parsed:com.familyrecipes.android.util.IngredientParser.ParseResult?)
    private var draft=Draft()
    private var ready=false;private var busy=false;private var binding=false;private var saved=false
    private var lookupJob:Job?=null
    private var lookupRevision=0
    private lateinit var name:EditText;private lateinit var amount:EditText;private lateinit var expiry:EditText
    private lateinit var matched:TextView;private lateinit var image:ImageView;private lateinit var save:com.google.android.material.button.MaterialButton
    private lateinit var content:LinearLayout
    private val cameraPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){if(it)camera() else Toast.makeText(this,R.string.inventory_camera_denied,Toast.LENGTH_LONG).show()}
    private val photograph=registerForActivityResult(ActivityResultContracts.TakePicture()){ok->val path=draft.camera;draft.camera=null;persist();if(path!=null){if(ok)choose(Uri.fromFile(File(path))) else File(path).delete()}}
    private val gallery=registerForActivityResult(ActivityResultContracts.GetContent()){if(it!=null)choose(it)}
    override fun onCreate(state:Bundle?){super.onCreate(state)
        if(!com.familyrecipes.android.util.AuthUtil.requireLogin(this)){finish();return}
        kitchen=state?.getLong("inventory_kitchen")?.takeIf{it>0}?:kitchen
        initialize(state)
    }
    private fun loading(state:Bundle?,failed:Boolean=false){
        val root=Ui.box(this);root.addView(Ui.back(this){finish()})
        root.addView(Ui.text(this,getString(if(failed)R.string.inventory_loading_failed else R.string.inventory_loading),18f))
        if(failed)root.addView(Ui.button(this,getString(R.string.social_retry),true){initialize(state)}) else root.addView(ProgressBar(this))
        setContentView(root)
    }
    private fun initialize(state:Bundle?){
        loading(state)
        lifecycleScope.launch{
            if(kitchen==null){try{kitchen=Ui.data(ApiClient.getAccountService(account).getKitchens()).firstOrNull()?.id;PreferenceManager.currentKitchenId=kitchen}catch(e:CancellationException){throw e}catch(e:Exception){loading(state,true);return@launch}}
            if(!active()){finish();return@launch}
            if(kitchen==null){loading(state,true);return@launch}
            draft=runCatching{LocalStore.instance.records().get(account,"draft",key)?.let{Gson().fromJson(it,Draft::class.java)}}.getOrNull()
                ?:runCatching{LocalStore.instance.records().get(account,"draft","inventory:$kitchen")?.let{raw->
                    val legacy=Gson().fromJson(raw,LegacyDraft::class.java)
                    Draft(text=legacy.text.orEmpty(),amount=legacy.parsed?.amount.orEmpty(),storage=legacy.parsed?.storageLocation?:"冷藏",
                        image=legacy.image?.let{Uri.parse(it).path},ownerId=legacy.reservedFor,categoryId=legacy.parsed?.categoryId)
                }}.getOrNull()?:Draft()
            draft.storage=when {draft.storage.contains("冷冻")||draft.storage.contains("froz",true)||draft.storage.contains("freez",true)->"冷冻";draft.storage.contains("冷藏")||draft.storage.contains("冰箱")||draft.storage.contains("refrig",true)->"冷藏";draft.storage.contains("常温")||draft.storage.contains("room",true)->"常温";else->""}
            ready=true;persist();render();if(draft.text.isNotBlank()&&draft.pending==null)lookup(false)
            val incomingName=intent.getStringExtra("ingredient_name")
            val incomingId=intent.getLongExtra("ingredient_id",0).takeIf{it>0}
            if(state==null&&incomingId!=null&&!incomingName.isNullOrBlank()){
                val fill={draft.image?.let{File(it).delete()};draft=Draft(text=incomingName,ingredientId=incomingId);persist();render();lookup(false)}
                if(draft.pending==null&&draft.text.isBlank())fill()
                else if(draft.pending==null)AlertDialog.Builder(this@AddIngredientActivity).setMessage(R.string.inventory_replace_draft).setNegativeButton(android.R.string.cancel,null).setPositiveButton(android.R.string.ok){_,_->fill()}.show()
            }
        }
    }
    override fun onSaveInstanceState(state:Bundle){kitchen?.let{state.putLong("inventory_kitchen",it)};super.onSaveInstanceState(state)}
    private fun persist(){if(ready&&!saved)DraftWrites.put(account,key,Gson().toJson(draft))}
    private fun active()=account==LocalStore.account&&PreferenceManager.isLoggedIn()
    override fun onStop(){persist();super.onStop()}
    private fun render(){
        val root=Ui.box(this);root.addView(Ui.row(this).apply{addView(Ui.back(this@AddIngredientActivity){finish()});addView(Ui.text(this@AddIngredientActivity,getString(R.string.ui_43589f00eb),22f,true))})
        content=Ui.box(this,0)
        val scans=Ui.row(this)
        scans.addView(Ui.button(this,getString(R.string.receipt_title)){startActivity(Intent(this,ReceiptActivity::class.java).putExtra("mode","receipt"))},LinearLayout.LayoutParams(0,-2,1f))
        scans.addView(Ui.button(this,getString(R.string.inventory_item_scan)){startActivity(Intent(this,ReceiptActivity::class.java).putExtra("mode","item"))},LinearLayout.LayoutParams(0,-2,1f));content.addView(scans)
        name=Ui.field(this,getString(R.string.receipt_name),draft.text);content.addView(name)
        matched=Ui.text(this,getString(R.string.inventory_unresolved),14f);content.addView(matched)
        amount=Ui.field(this,getString(R.string.receipt_quantity),draft.amount);content.addView(amount)
        val storageValues=listOf("","常温","冷藏","冷冻")
        content.addView(Ui.select(this,listOf(R.string.receipt_unspecified,R.string.receipt_room,R.string.receipt_cold,R.string.receipt_frozen).map{getString(it)},storageValues.indexOf(draft.storage).coerceAtLeast(0)){index->
            if(draft.storage!=storageValues[index]){draft.storage=storageValues[index];persist();if(!draft.expiryManual)estimate()}
        })
        expiry=Ui.field(this,getString(R.string.inventory_date_optional),draft.expiry);content.addView(expiry)
        content.addView(Ui.text(this,getString(R.string.inventory_estimated),13f))
        content.addView(Ui.button(this,getString(R.string.inventory_use_estimate)){draft.expiryManual=false;estimate()})
        content.addView(Ui.button(this,getString(R.string.ui_e19581a0b5)){selectCategory()})
        content.addView(Ui.button(this,getString(R.string.ui_6cec1849f4)){selectOwner()})
        image=ImageView(this).apply{adjustViewBounds=true;maxHeight=(180*resources.displayMetrics.density).toInt();visibility=View.GONE};content.addView(image)
        draft.image?.let{image.setImageURI(Uri.fromFile(File(it)));image.visibility=View.VISIBLE}
        val photos=Ui.row(this);photos.addView(Ui.button(this,getString(R.string.ui_69f5326297)){AlertDialog.Builder(this).setItems(arrayOf(getString(R.string.scan_camera),getString(R.string.inventory_gallery))){_,which->if(which==0)camera()else gallery.launch("image/*")}.show()},LinearLayout.LayoutParams(0,-2,1f))
        photos.addView(Ui.button(this,getString(R.string.receipt_remove)){draft.image?.let{File(it).delete()};draft.image=null;draft.uploaded=null;image.visibility=View.GONE;persist()},LinearLayout.LayoutParams(0,-2,1f));content.addView(photos)
        if(draft.pending!=null)content.addView(Ui.text(this,getString(R.string.inventory_pending_save)))
        root.addView(Ui.scroll(this,content),LinearLayout.LayoutParams(-1,0,1f))
        save=Ui.button(this,getString(if(draft.pending==null)R.string.ui_be5fbbe34c else R.string.inventory_retry_save),true){save()};root.addView(save);setContentView(root)
        name.doAfterTextChanged{if(!binding){draft.text=it.toString();draft.ingredientId=null;persist();lookup(true)}}
        amount.doAfterTextChanged{if(!binding){draft.amount=it.toString();persist()}}
        expiry.doAfterTextChanged{if(!binding){draft.expiry=it.toString();draft.expiryManual=true;persist()}}
        setBusy(busy)
    }
    private fun setBusy(value:Boolean){busy=value;fun enable(v:View,enabled:Boolean){v.isEnabled=enabled;if(v is android.view.ViewGroup)for(i in 0 until v.childCount)enable(v.getChildAt(i),enabled)};enable(content,!busy&&draft.pending==null);save.isEnabled=!busy}
    private fun lookup(reset:Boolean){
        lookupJob?.cancel();val generation=++lookupRevision
        if(draft.text.isBlank()){draft.ingredientId=null;matched.text=getString(R.string.inventory_unresolved);matched.setOnClickListener(null);return}
        lookupJob=lifecycleScope.launch{try{delay(300)
            val raw=draft.text
            val result=Ui.data(ApiClient.getAccountService(account).resolveIngredientEntries(IngredientEntriesRequest(listOf(IngredientEntry(raw,draft.ingredientId)),false,draft.storage))).first()
            if(generation!=lookupRevision||!active()||draft.text!=raw)return@launch
            draft.ingredientId=result.ingredient?.id
            matched.text=if(result.matched)getString(R.string.inventory_match,result.ingredient?.name) else getString(R.string.inventory_unresolved)
            matched.setOnClickListener{selectMatch(result)}
            binding=true
            if(draft.amount.isBlank()&&result.amount!=null){draft.amount=result.amount;amount.setText(draft.amount)}
            if(!draft.expiryManual){draft.expiryManual=result.expiryDate!=null;draft.expiry=result.expiryDate?:result.estimatedDays?.let{LocalDate.now().plusDays(it.toLong()).toString()}.orEmpty();expiry.setText(draft.expiry)}
            binding=false;persist()
        }catch(e:CancellationException){throw e}catch(e:Exception){if(generation==lookupRevision){matched.text=getString(R.string.inventory_unresolved);matched.setOnClickListener{lookup(false)}}}}
    }
    private fun selectMatch(result:IngredientResolution){
        lifecycleScope.launch{try{
            val search=Ui.data(ApiClient.getAccountService(account).searchIngredients(result.name))
            val choices=(result.candidates+search).distinctBy{it.id}
            AlertDialog.Builder(this@AddIngredientActivity).setTitle(R.string.inventory_select_match)
                .setItems((choices.map{it.name}+getString(R.string.inventory_keep_name)).toTypedArray()){_,i->
                    draft.ingredientId=choices.getOrNull(i)?.id;persist()
                    if(i>=choices.size)matched.text=getString(R.string.inventory_unresolved)
                    if(i<choices.size){matched.text=getString(R.string.inventory_match,choices[i].name);if(!draft.expiryManual)estimate()}
                }.show()
        }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@AddIngredientActivity,e)}}
    }
    private fun estimate(){if(!::expiry.isInitialized||draft.text.isBlank()||draft.pending!=null)return
        val id=draft.ingredientId;val text=draft.text;val storage=draft.storage
        lifecycleScope.launch{try{val estimate=Ui.data(ApiClient.getAccountService(account).estimateIngredient(IngredientEstimateRequest(id,text,storage)))
            if(active()&&draft.ingredientId==id&&draft.text==text&&draft.storage==storage&&!draft.expiryManual){draft.expiry=estimate.estimatedDays?.let{LocalDate.now().plusDays(it.toLong()).toString()}.orEmpty();binding=true;expiry.setText(draft.expiry);binding=false;persist()}
        }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@AddIngredientActivity,e)}}
    }
    private fun camera(){if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){cameraPermission.launch(Manifest.permission.CAMERA);return}
        try{val file=File.createTempFile("inventory-camera-",".jpg",cacheDir);draft.camera=file.absolutePath;persist();photograph.launch(FileProvider.getUriForFile(this,"$packageName.fileprovider",file))}
        catch(e:Exception){Toast.makeText(this,R.string.inventory_camera_missing,Toast.LENGTH_LONG).show()}}
    private fun choose(uri:Uri){if(busy)return;setBusy(true);lifecycleScope.launch{try{val photo=InventoryImages.copy(this@AddIngredientActivity,uri);draft.image?.let{File(it).delete()};draft.image=photo.absolutePath;draft.uploaded=null;persist();image.setImageURI(Uri.fromFile(photo));image.visibility=View.VISIBLE}
        catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@AddIngredientActivity,e)}finally{if(uri.scheme=="file"&&uri.path?.let{File(it).parentFile==cacheDir}==true)File(uri.path!!).delete();setBusy(false)}}}
    private fun selectCategory(){lifecycleScope.launch{try{val choices=Ui.data(ApiClient.getAccountService(account).getAllCategories());AlertDialog.Builder(this@AddIngredientActivity).setTitle(R.string.ui_e19581a0b5).setItems((listOf(getString(R.string.inventory_keep_name))+choices.map{"${it.icon.orEmpty()} ${it.name}"}).toTypedArray()){_,i->draft.categoryId=choices.getOrNull(i-1)?.id;persist()}.show()}catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@AddIngredientActivity,e)}}}
    private fun selectOwner(){lifecycleScope.launch{try{val chosen=Ui.data(ApiClient.getAccountService(account).getKitchens()).first{it.id==this@AddIngredientActivity.kitchen};val members=Ui.data(ApiClient.getAccountService(account).getGroupDetail(requireNotNull(chosen.groupId))).members.orEmpty();AlertDialog.Builder(this@AddIngredientActivity).setTitle(R.string.ui_6cec1849f4).setItems((listOf(getString(R.string.ui_e0d982c2fd))+members.map{it.user?.username?:it.userId.toString()}).toTypedArray()){_,i->draft.ownerId=members.getOrNull(i-1)?.userId;persist()}.show()}catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@AddIngredientActivity,e)}}}
    private fun save(){if(busy||!active())return
        if(draft.pending==null&&(draft.text.isBlank()||draft.expiry.isNotBlank()&&runCatching{LocalDate.parse(draft.expiry)}.isFailure)){expiry.error=getString(R.string.inventory_date_optional);return}
        lookupJob?.cancel();lookupRevision++;setBusy(true)
        lifecycleScope.launch{try{
            if(draft.pending==null){
                val result=Ui.data(ApiClient.getAccountService(account).resolveIngredientEntries(IngredientEntriesRequest(listOf(IngredientEntry(draft.text,draft.ingredientId,draft.amount.ifBlank{null})),true,draft.storage))).first()
                if(draft.image!=null&&draft.uploaded==null){val f=File(draft.image!!);draft.uploaded=Ui.data(ApiClient.getAccountService(account).uploadImage(MultipartBody.Part.createFormData("file",f.name,f.asRequestBody("image/jpeg".toMediaType())),"inventory"));persist()}
                draft.pending=FridgeItem(kitchenId=kitchen,id=null,userId=PreferenceManager.userId,ingredientId=requireNotNull(result.ingredient?.id),amount=draft.amount.ifBlank{result.amount},purchaseDate=LocalDate.now().toString(),expiryDate=draft.expiry.ifBlank{null},storageLocation=draft.storage.ifBlank{null},status="normal",notes=draft.text,ingredient=null,ownerUserId=draft.ownerId,imageUrl=draft.uploaded,categoryOverrideId=draft.categoryId,displayName=result.name,clientMutationId=draft.mutation);persist()
            }
            DraftWrites.flush()
            val response=ApiClient.getAccountService(account).addFridgeItem(draft.pending!!)
            if(response.code() in listOf(400,403,404)){draft.pending=null;persist()}
            Ui.data(response);saved=true;DraftWrites.clear(account,key);DraftWrites.clear(account,"inventory:$kitchen");draft.image?.let{File(it).delete()};setResult(Activity.RESULT_OK);finish()
        }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@AddIngredientActivity,e)}finally{if(!isFinishing){render();setBusy(false)}}}
    }
}
