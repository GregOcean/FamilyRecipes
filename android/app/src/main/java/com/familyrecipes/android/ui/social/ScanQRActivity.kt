package com.familyrecipes.android.ui.social

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.util.QRCodeUtil
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity
import kotlinx.coroutines.launch

/**
 * 扫码添加好友Activity
 */
class ScanQRActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (checkCameraPermission()) {
            startScan()
        } else {
            requestCameraPermission()
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScan()
            } else {
                Toast.makeText(this, "需要相机权限才能扫码", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startScan() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("扫描好友二维码")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(false)
        integrator.captureActivity = CaptureActivity::class.java
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "取消扫码", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                handleQRCodeResult(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleQRCodeResult(content: String) {
        val friendId = QRCodeUtil.parseUserCardContent(content)
        if (friendId == null) {
            Toast.makeText(this, "无效的二维码", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 添加好友
        addFriend(friendId)
    }

    private fun addFriend(friendId: Long) {
        lifecycleScope.launch {
            try {
                val request = mapOf("friendId" to friendId)
                val response = ApiClient.getService().addFriend(request)

                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(this@ScanQRActivity, "添加好友成功", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(
                        this@ScanQRActivity,
                        response.body()?.message ?: "添加好友失败",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ScanQRActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
                finish()
            }
        }
    }
}

