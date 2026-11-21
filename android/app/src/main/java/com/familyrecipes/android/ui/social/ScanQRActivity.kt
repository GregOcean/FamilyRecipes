package com.familyrecipes.android.ui.social

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.familyrecipes.android.R
import com.familyrecipes.android.data.local.PreferenceManager
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.design.Ui
import com.familyrecipes.android.util.QRCodeUtil
import com.familyrecipes.android.util.InventoryImages
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.*

/** QR scanning identifies a person; sending an invitation is a separate confirmed action. */
class ScanQRActivity:AppCompatActivity() {
    private val scopeAccount=com.familyrecipes.android.data.local.LocalStore.account
    private val account=PreferenceManager.userId
    private val server=PreferenceManager.baseUrl
    private fun active()=PreferenceManager.isLoggedIn()&&account==PreferenceManager.userId&&server==PreferenceManager.baseUrl
    private var busy=false
    private var pendingId:Long?=null
    private lateinit var status:android.widget.TextView
    private val cameraPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){if(it)startCamera()else Toast.makeText(this,R.string.inventory_camera_denied,Toast.LENGTH_LONG).show()}
    private val gallery=registerForActivityResult(ActivityResultContracts.GetContent()){uri->
        if(uri!=null&&!busy){busy=true;lifecycleScope.launch{try{
            val file=InventoryImages.copy(this@ScanQRActivity,uri)
            val content=withContext(Dispatchers.Default){try{val bitmap=BitmapFactory.decodeFile(file.absolutePath)?:error("Invalid image");try{val pixels=IntArray(bitmap.width*bitmap.height);bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height);MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width,bitmap.height,pixels))),mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),DecodeHintType.TRY_HARDER to true)).text}finally{bitmap.recycle()}}finally{file.delete()}}
            handle(content)
        }catch(e:CancellationException){throw e}catch(e:Exception){status.setText(R.string.scan_retry)}finally{busy=false}}}
    }
    override fun onCreate(state:Bundle?){super.onCreate(state)
        if(!com.familyrecipes.android.util.AuthUtil.requireLogin(this)){finish();return}
        pendingId=state?.getLong("pending_id")?.takeIf{it>0}
        val root=Ui.box(this)
        root.addView(Ui.row(this).apply{addView(Ui.back(this@ScanQRActivity){finish()});addView(Ui.text(this@ScanQRActivity,getString(R.string.scan_friend_title),22f,true))})
        status=Ui.text(this,"");root.addView(status)
        root.addView(Ui.button(this,getString(R.string.scan_camera),true){startCamera()})
        root.addView(Ui.button(this,getString(R.string.scan_gallery)){gallery.launch("image/*")})
        root.addView(Ui.button(this,getString(R.string.social_retry)){pendingId?.let{showCard(it)}?:startCamera()})
        setContentView(root)
        if(pendingId!=null)showCard(pendingId!!) else if(state==null)startCamera()
    }
    override fun onSaveInstanceState(state:Bundle){pendingId?.let{state.putLong("pending_id",it)};super.onSaveInstanceState(state)}
    private fun startCamera(){if(busy)return
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){cameraPermission.launch(Manifest.permission.CAMERA);return}
        try{IntentIntegrator(this).apply{setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);setPrompt(getString(R.string.scan_friend_title));setCameraId(0);setBeepEnabled(false);setBarcodeImageEnabled(false);captureActivity=QRScannerActivity::class.java}.initiateScan()}
        catch(e:Exception){status.setText(R.string.inventory_camera_missing)}
    }
    @Deprecated("Used by ZXing integration") override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        val result=IntentIntegrator.parseActivityResult(requestCode,resultCode,data)
        if(result!=null)result.contents?.let{handle(it)} else super.onActivityResult(requestCode,resultCode,data)
    }
    private fun handle(content:String){if(!active())return;val id=QRCodeUtil.parseUserCardContent(content)
        if(id==null){status.setText(R.string.scan_invalid);return}
        if(id==PreferenceManager.userId){status.setText(R.string.scan_self);return}
        pendingId=id;showCard(id)
    }
    private fun showCard(id:Long){
        if(!active())return
        lifecycleScope.launch{try{
            val user=Ui.data(ApiClient.getAccountService(scopeAccount).searchFriends(id.toString())).firstOrNull{it.id==id}
            if(!active()||pendingId!=id)return@launch
            if(user==null){status.setText(R.string.friend_not_found);return@launch}
            AlertDialog.Builder(this@ScanQRActivity).setMessage(getString(R.string.scan_confirm_friend,user.username,id))
                .setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.friend_invite){_,_->send(id)}.show()
        }catch(e:CancellationException){throw e}catch(e:Exception){status.text=getString(R.string.scan_retry);Ui.toast(this@ScanQRActivity,e)}}
    }
    private fun send(id:Long){if(busy||!active())return;busy=true
        lifecycleScope.launch{try{Ui.ok(ApiClient.getAccountService(scopeAccount).addFriend(mapOf("friendId" to id)));status.setText(R.string.friend_pending);pendingId=null;setResult(Activity.RESULT_OK)}
        catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@ScanQRActivity,e)}finally{busy=false}}
    }
}
