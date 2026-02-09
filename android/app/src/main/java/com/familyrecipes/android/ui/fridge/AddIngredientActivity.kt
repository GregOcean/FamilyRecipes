package com.familyrecipes.android.ui.fridge

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.familyrecipes.android.data.local.PreferenceManager
import com.familyrecipes.android.data.model.FridgeItem
import com.familyrecipes.android.data.model.Ingredient
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.databinding.ActivityAddIngredientBinding
import com.familyrecipes.android.util.IngredientParser
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 添加食材页面
 */
class AddIngredientActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddIngredientBinding
    private var selectedImageUri: Uri? = null
    private var currentPhotoPath: String? = null

    // 图片选择器
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                binding.ivPreview.setImageURI(uri)
                binding.ivPreview.visibility = android.view.View.VISIBLE
                binding.btnRemoveImage.visibility = android.view.View.VISIBLE
            }
        }
    }

    // 拍照
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let { path ->
                val file = File(path)
                selectedImageUri = Uri.fromFile(file)
                binding.ivPreview.setImageURI(selectedImageUri)
                binding.ivPreview.visibility = android.view.View.VISIBLE
                binding.btnRemoveImage.visibility = android.view.View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddIngredientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupListeners() {
        // 添加图片
        binding.btnAddImage.setOnClickListener {
            showImagePickerDialog()
        }

        // 移除图片
        binding.btnRemoveImage.setOnClickListener {
            selectedImageUri = null
            binding.ivPreview.visibility = android.view.View.GONE
            binding.btnRemoveImage.visibility = android.view.View.GONE
        }

        // 保存按钮
        binding.btnSave.setOnClickListener {
            saveIngredient()
        }
    }

    /**
     * 显示图片选择对话框
     */
    private fun showImagePickerDialog() {
        val options = arrayOf("拍照", "从相册选择")
        AlertDialog.Builder(this)
            .setTitle("选择图片来源")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePicture()
                    1 -> pickImage()
                }
            }
            .show()
    }

    /**
     * 拍照
     */
    private fun takePicture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        
        // 创建临时文件
        val photoFile = createImageFile()
        currentPhotoPath = photoFile.absolutePath
        
        val photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        takePictureLauncher.launch(intent)
    }

    /**
     * 从相册选择
     */
    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    /**
     * 创建图片文件
     */
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File(storageDir, "INGREDIENT_${timeStamp}.jpg")
    }

    /**
     * 保存食材
     */
    private fun saveIngredient() {
        val inputText = binding.etIngredientInfo.text.toString().trim()

        if (inputText.isEmpty()) {
            Toast.makeText(this, "请输入食材信息", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSave.isEnabled = false
        binding.btnSave.text = "识别中..."

        lifecycleScope.launch {
            try {
                // 1. 解析输入信息
                val parseResult = IngredientParser.parse(inputText)
                
                // 2. 验证是否识别出食材名称
                if (parseResult.ingredientName.isNullOrEmpty()) {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "保存"
                    showParseFailureDialog()
                    return@launch
                }
                
                // 3. 显示识别结果确认
                showParseResultDialog(parseResult)
                
            } catch (e: Exception) {
                Toast.makeText(
                    this@AddIngredientActivity,
                    "解析失败: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "保存"
            }
        }
    }
    
    /**
     * 显示解析失败对话框
     */
    private fun showParseFailureDialog() {
        AlertDialog.Builder(this)
            .setTitle("❌ 无法识别食材")
            .setMessage("无法从输入中识别出食材名称。\n\n请确保至少输入食材名称，例如：\n• 鸡蛋\n• 牛奶一瓶\n• 草莓7天内吃完")
            .setPositiveButton("修改") { dialog, _ -> 
                dialog.dismiss()
            }
            .show()
    }
    
    /**
     * 显示解析结果确认对话框
     */
    private fun showParseResultDialog(parseResult: IngredientParser.ParseResult) {
        val message = buildString {
            append("✓ 食材名称：${parseResult.ingredientName}\n")
            if (!parseResult.amount.isNullOrEmpty()) {
                append("✓ 数量：${parseResult.amount}\n")
            }
            append("✓ 保质期：${parseResult.expiryDays}天")
            
            if (parseResult.expiryDays <= 3) {
                append(" ⚠️")
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("✅ 识别结果")
            .setMessage(message)
            .setPositiveButton("确认保存") { dialog, _ ->
                dialog.dismiss()
                performSave(parseResult)
            }
            .setNegativeButton("修改") { dialog, _ ->
                dialog.dismiss()
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "保存"
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * 执行保存操作
     */
    private fun performSave(parseResult: IngredientParser.ParseResult) {
        binding.btnSave.text = "保存中..."
        
        lifecycleScope.launch {
            try {
                // 1. 上传图片（如果有）
                var imageUrl: String? = null
                selectedImageUri?.let { uri ->
                    imageUrl = uploadImage(uri)
                }

                // 2. 创建或获取食材
                val ingredient = getOrCreateIngredient(parseResult.ingredientName!!)

                // 3. 计算过期日期
                val expiryDate = IngredientParser.calculateExpiryDate(parseResult.expiryDays)

                // 4. 创建食材项
                val fridgeItem = FridgeItem(
                    id = null,
                    userId = PreferenceManager.userId,
                    ingredientId = ingredient.id,
                    amount = parseResult.amount,
                    purchaseDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    expiryDate = expiryDate,
                    storageLocation = imageUrl,
                    status = FridgeItem.STATUS_NORMAL,
                    notes = parseResult.notes,
                    ingredient = null
                )

                // 5. 添加到冰箱
                val response = ApiClient.getService().addFridgeItem(fridgeItem)

                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(this@AddIngredientActivity, "添加成功！", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(
                        this@AddIngredientActivity,
                        response.body()?.message ?: "添加失败",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AddIngredientActivity,
                    "网络错误: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            } finally {
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "保存"
            }
        }
    }

    /**
     * 上传图片
     */
    private suspend fun uploadImage(uri: Uri): String? {
        try {
            val file = uriToFile(uri)
            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestBody)

            val response = ApiClient.getService().uploadImage(multipartBody)

            if (response.isSuccessful && response.body()?.code == 200) {
                return response.body()?.data
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 获取或创建食材
     */
    private suspend fun getOrCreateIngredient(name: String): Ingredient {
        // 先搜索是否已存在
        val searchResponse = ApiClient.getService().searchIngredients(name)
        if (searchResponse.isSuccessful && searchResponse.body()?.code == 200) {
            val existingIngredients = searchResponse.body()?.data
            val exactMatch = existingIngredients?.find { it.name.equals(name, ignoreCase = true) }
            if (exactMatch != null) {
                return exactMatch
            }
        }

        // 不存在则创建
        val newIngredient = Ingredient(
            id = null,
            name = name,
            category = "其他",
            unit = null
        )

        val createResponse = ApiClient.getService().createIngredient(newIngredient)
        if (createResponse.isSuccessful && createResponse.body()?.code == 200) {
            return createResponse.body()?.data!!
        }

        throw Exception("创建食材失败")
    }

    /**
     * 将Uri转换为File
     */
    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val tempFile = File(cacheDir, "temp_${System.currentTimeMillis()}.jpg")
        tempFile.outputStream().use { outputStream ->
            inputStream?.copyTo(outputStream)
        }
        return tempFile
    }
}

