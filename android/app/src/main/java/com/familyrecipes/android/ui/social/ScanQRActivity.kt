package com.familyrecipes.android.ui.social

import com.familyrecipes.android.ui.design.UiStrings
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
        
        // Restored instances already have a scanner/permission request in progress.
        if (savedInstanceState != null) return
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
                Toast.makeText(this, UiStrings.s(com.familyrecipes.android.R.string.ui_77c507528d), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startScan() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt(UiStrings.s(com.familyrecipes.android.R.string.ui_4b88cfb089))
        integrator.setCameraId(0)
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(false)
        integrator.captureActivity = QRScannerActivity::class.java
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, UiStrings.s(com.familyrecipes.android.R.string.ui_5087367b47), Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, UiStrings.s(com.familyrecipes.android.R.string.ui_224b61b7a1), Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@ScanQRActivity, UiStrings.s(com.familyrecipes.android.R.string.ui_415109cfac), Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(
                        this@ScanQRActivity,
                        response.body()?.message ?: UiStrings.s(com.familyrecipes.android.R.string.ui_f45a3d8a26),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ScanQRActivity, UiStrings.f(com.familyrecipes.android.R.string.network_error_value, e.message), Toast.LENGTH_SHORT).show()
                e.printStackTrace()
                finish()
            }
        }
    }
}

