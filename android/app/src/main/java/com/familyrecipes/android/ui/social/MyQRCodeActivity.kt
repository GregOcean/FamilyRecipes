package com.familyrecipes.android.ui.social

import com.familyrecipes.android.ui.design.UiStrings
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.familyrecipes.android.data.local.PreferenceManager
import com.familyrecipes.android.databinding.ActivityMyQrCodeBinding
import com.familyrecipes.android.util.QRCodeUtil

/**
 * 我的名片码Activity
 */
class MyQRCodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyQrCodeBinding
    private var qrCodeBitmap: Bitmap? = null

    companion object {
        private const val REQUEST_STORAGE_PERMISSION = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyQrCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadUserInfo()
        generateQRCode()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationIcon(com.familyrecipes.android.R.drawable.ic_arrow_back)
        binding.toolbar.navigationContentDescription=UiStrings.s(com.familyrecipes.android.R.string.ui_5f411223ca)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadUserInfo() {
        binding.tvUsername.text = PreferenceManager.userName ?: UiStrings.s(com.familyrecipes.android.R.string.ui_2878420820)
        binding.tvUserId.text = "ID: ${PreferenceManager.userId}"
    }

    private fun generateQRCode() {
        val userId = PreferenceManager.userId
        if (userId == null) {
            Toast.makeText(this, UiStrings.s(com.familyrecipes.android.R.string.ui_70189fcd83), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val content = QRCodeUtil.generateUserCardContent(userId)
        qrCodeBitmap = QRCodeUtil.createQRCode(content, 600, 600)

        if (qrCodeBitmap != null) {
            binding.ivQrCode.setImageBitmap(qrCodeBitmap)
        } else {
            Toast.makeText(this, UiStrings.s(com.familyrecipes.android.R.string.ui_60cb8f1898), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        binding.btnSaveQr.setOnClickListener {
            if (checkStoragePermission()) {
                saveQRCodeToGallery()
            } else {
                requestStoragePermission()
            }
        }
    }

    private fun checkStoragePermission(): Boolean {
        if(android.os.Build.VERSION.SDK_INT>=29)return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_STORAGE_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveQRCodeToGallery()
            } else {
                Toast.makeText(this, UiStrings.s(com.familyrecipes.android.R.string.ui_524fc5fded), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveQRCodeToGallery() {
        val bitmap = qrCodeBitmap
        if (bitmap == null) {
            Toast.makeText(this, UiStrings.s(com.familyrecipes.android.R.string.ui_dfac8f84fe), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "家肴_名片码_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if(android.os.Build.VERSION.SDK_INT>=29)put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FamilyRecipes")
            }

            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }
                Toast.makeText(this, UiStrings.s(com.familyrecipes.android.R.string.ui_04cd1d0438), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, UiStrings.s(com.familyrecipes.android.R.string.ui_6de920b4e4), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

