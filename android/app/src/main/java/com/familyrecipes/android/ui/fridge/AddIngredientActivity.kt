package com.familyrecipes.android.ui.fridge

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
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
import com.familyrecipes.android.databinding.DialogEditParseResultBinding
import com.familyrecipes.android.util.CommonIngredients
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
     * 显示解析结果确认对话框（带编辑功能）
     */
    private fun showParseResultDialog(parseResult: IngredientParser.ParseResult) {
        val dialogBinding = DialogEditParseResultBinding.inflate(LayoutInflater.from(this))
        
        // 设置初始值
        dialogBinding.etIngredientName.setText(parseResult.ingredientName ?: "")
        dialogBinding.etAmount.setText(parseResult.amount ?: "")
        dialogBinding.etExpiryDays.setText(parseResult.expiryDays.toString())
        dialogBinding.etStorageLocation.setText(parseResult.storageLocation ?: "冰箱冷藏")
        
        // 设置食材名称自动补全
        setupAutoComplete(dialogBinding)
        
        // 设置存储位置下拉选择
        setupStorageLocationDropdown(dialogBinding)
        
        // 设置保质期日历选择器
        setupExpiryDatePicker(dialogBinding)
        
        // 创建对话框
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()
        
        // 监听保质期输入变化，显示/隐藏警告
        dialogBinding.etExpiryDays.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val days = s.toString().toIntOrNull() ?: 0
                if (days in 1..3) {
                    dialogBinding.warningContainer.visibility = View.VISIBLE
                    dialogBinding.tvWarning.text = "保质期仅剩${days}天，请尽快食用！"
                } else {
                    dialogBinding.warningContainer.visibility = View.GONE
                }
            }
        })
        
        // 初始检查保质期
        if (parseResult.expiryDays in 1..3) {
            dialogBinding.warningContainer.visibility = View.VISIBLE
            dialogBinding.tvWarning.text = "保质期仅剩${parseResult.expiryDays}天，请尽快食用！"
        }
        
        // 返回修改按钮
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
            binding.btnSave.isEnabled = true
            binding.btnSave.text = "保存"
        }
        
        // 确认保存按钮
        dialogBinding.btnSave.setOnClickListener {
            val name = dialogBinding.etIngredientName.text.toString().trim()
            val amount = dialogBinding.etAmount.text.toString().trim()
            val expiryDaysStr = dialogBinding.etExpiryDays.text.toString().trim()
            val storageLocation = dialogBinding.etStorageLocation.text.toString().trim()
            
            // 验证输入
            if (name.isEmpty()) {
                Toast.makeText(this, "请输入食材名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (expiryDaysStr.isEmpty()) {
                Toast.makeText(this, "请输入保质期", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val expiryDays = expiryDaysStr.toIntOrNull()
            if (expiryDays == null || expiryDays <= 0) {
                Toast.makeText(this, "保质期必须是大于0的整数", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (expiryDays > 3650) {
                Toast.makeText(this, "保质期不能超过10年（3650天）", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // 创建修改后的结果
            val modifiedResult = IngredientParser.ParseResult(
                ingredientName = name,
                amount = amount.ifEmpty { null },
                expiryDays = expiryDays,
                storageLocation = storageLocation.ifEmpty { "冰箱冷藏" },
                notes = parseResult.notes
            )
            
            dialog.dismiss()
            performSave(modifiedResult)
        }
        
        dialog.show()
    }
    
    /**
     * 设置食材名称自动补全
     */
    private fun setupAutoComplete(dialogBinding: DialogEditParseResultBinding) {
        val autoCompleteTextView = dialogBinding.etIngredientName
        
        // 初始化适配器，直接使用所有食材列表
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            CommonIngredients.INGREDIENTS
        )
        autoCompleteTextView.setAdapter(adapter)
        
        // 设置阈值：输入1个字符后显示下拉
        autoCompleteTextView.threshold = 1
        
        Log.d("AddIngredient", "AutoComplete setup complete, total ingredients: ${CommonIngredients.INGREDIENTS.size}")
        
        // 监听文本变化进行过滤
        autoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                Log.d("AddIngredient", "User input: '$query'")
                
                // AutoCompleteTextView 会自动根据输入过滤 adapter 中的数据
                // 这里不需要手动更新 adapter
            }
        })
        
        // 点击输入框时，如果没有输入内容，显示所有常见食材
        autoCompleteTextView.setOnClickListener {
            if (autoCompleteTextView.text.isEmpty()) {
                autoCompleteTextView.showDropDown()
            }
        }
    }
    
    /**
     * 设置存储位置下拉选择
     */
    private fun setupStorageLocationDropdown(dialogBinding: DialogEditParseResultBinding) {
        val storageLocationView = dialogBinding.etStorageLocation
        
        // 初始化适配器，使用常见存储位置列表
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            CommonIngredients.STORAGE_LOCATIONS
        )
        storageLocationView.setAdapter(adapter)
        
        // 设置阈值：输入1个字符后显示下拉
        storageLocationView.threshold = 1
        
        Log.d("AddIngredient", "Storage location dropdown setup complete, total locations: ${CommonIngredients.STORAGE_LOCATIONS.size}")
        
        // 点击输入框时，显示所有存储位置选项
        storageLocationView.setOnClickListener {
            storageLocationView.showDropDown()
        }
        
        // 获取焦点时也显示下拉列表
        storageLocationView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                storageLocationView.showDropDown()
            }
        }
    }
    
    /**
     * 设置保质期日历选择器
     */
    private fun setupExpiryDatePicker(dialogBinding: DialogEditParseResultBinding) {
        val expiryDaysInput = dialogBinding.etExpiryDays
        val textInputLayout = dialogBinding.tilExpiryDays
        
        // 点击右侧日历图标时打开日期选择器
        textInputLayout.setEndIconOnClickListener {
            showDatePickerForExpiry(expiryDaysInput)
        }
        
        // 长按输入框也可以打开日期选择器（提供额外的交互方式）
        expiryDaysInput.setOnLongClickListener {
            showDatePickerForExpiry(expiryDaysInput)
            true
        }
    }
    
    /**
     * 显示日期选择器用于选择过期日期
     */
    private fun showDatePickerForExpiry(expiryDaysInput: com.google.android.material.textfield.TextInputEditText) {
        val calendar = java.util.Calendar.getInstance()
        
        // 如果已经有天数，设置为对应的日期
        val currentDays = expiryDaysInput.text.toString().toIntOrNull()
        if (currentDays != null && currentDays > 0) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, currentDays)
        }
        
        val datePickerDialog = android.app.DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                // 计算选择的日期与今天的天数差
                val selectedCalendar = java.util.Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth, 0, 0, 0)
                selectedCalendar.set(java.util.Calendar.MILLISECOND, 0)
                
                val today = java.util.Calendar.getInstance()
                today.set(java.util.Calendar.HOUR_OF_DAY, 0)
                today.set(java.util.Calendar.MINUTE, 0)
                today.set(java.util.Calendar.SECOND, 0)
                today.set(java.util.Calendar.MILLISECOND, 0)
                
                val diffInMillis = selectedCalendar.timeInMillis - today.timeInMillis
                val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
                
                if (diffInDays < 0) {
                    Toast.makeText(this, "过期日期不能早于今天", Toast.LENGTH_SHORT).show()
                    expiryDaysInput.setText("1")
                } else if (diffInDays == 0) {
                    Toast.makeText(this, "今天过期，设置为1天", Toast.LENGTH_SHORT).show()
                    expiryDaysInput.setText("1")
                } else {
                    expiryDaysInput.setText(diffInDays.toString())
                    
                    // 显示友好的提示信息
                    val dateFormat = java.text.SimpleDateFormat("yyyy年MM月dd日", java.util.Locale.CHINA)
                    val dateStr = dateFormat.format(selectedCalendar.time)
                    Toast.makeText(this, "将在 $dateStr 过期（${diffInDays}天后）", Toast.LENGTH_SHORT).show()
                }
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
        
        // 设置日期选择器的最小日期为今天
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        
        // 设置最大日期为10年后
        val maxCalendar = java.util.Calendar.getInstance()
        maxCalendar.add(java.util.Calendar.YEAR, 10)
        datePickerDialog.datePicker.maxDate = maxCalendar.timeInMillis
        
        datePickerDialog.setTitle("选择过期日期")
        datePickerDialog.show()
    }
    
    /**
     * 执行保存操作
     */
    private fun performSave(parseResult: IngredientParser.ParseResult) {
        binding.btnSave.text = "保存中..."
        
        lifecycleScope.launch {
            try {
                android.util.Log.d("AddIngredient", "========== 开始保存食材 ==========")
                android.util.Log.d("AddIngredient", "食材名称: ${parseResult.ingredientName}")
                android.util.Log.d("AddIngredient", "数量: ${parseResult.amount}")
                android.util.Log.d("AddIngredient", "保质期: ${parseResult.expiryDays}天")
                
                // 1. 上传图片（如果有）
                var imageUrl: String? = null
                selectedImageUri?.let { uri ->
                    android.util.Log.d("AddIngredient", "开始上传图片...")
                    imageUrl = uploadImage(uri)
                    android.util.Log.d("AddIngredient", "图片上传完成: $imageUrl")
                }

                // 2. 创建或获取食材
                val ingredient = getOrCreateIngredient(parseResult.ingredientName!!)
                android.util.Log.d("AddIngredient", "✅ 获取到食材: ID=${ingredient.id}, 名称=${ingredient.name}")

                // 3. 计算过期日期
                val expiryDate = IngredientParser.calculateExpiryDate(parseResult.expiryDays)
                android.util.Log.d("AddIngredient", "过期日期: $expiryDate")

                // 4. 创建食材项
                val fridgeItem = FridgeItem(
                    id = null,
                    userId = PreferenceManager.userId,
                    ingredientId = ingredient.id,
                    amount = parseResult.amount,
                    purchaseDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    expiryDate = expiryDate,
                    storageLocation = parseResult.storageLocation ?: "冰箱",  // 使用用户输入的存储位置
                    status = FridgeItem.STATUS_NORMAL,
                    notes = parseResult.notes,
                    ingredient = null
                )
                
                android.util.Log.d("AddIngredient", "准备添加到冰箱:")
                android.util.Log.d("AddIngredient", "  - 用户ID: ${fridgeItem.userId}")
                android.util.Log.d("AddIngredient", "  - 食材ID: ${fridgeItem.ingredientId}")
                android.util.Log.d("AddIngredient", "  - 数量: ${fridgeItem.amount}")
                android.util.Log.d("AddIngredient", "  - 过期日期: ${fridgeItem.expiryDate}")

                // 5. 添加到冰箱
                val response = ApiClient.getService().addFridgeItem(fridgeItem)
                android.util.Log.d("AddIngredient", "添加到冰箱响应: ${response.isSuccessful}, code=${response.body()?.code}")

                if (response.isSuccessful && response.body()?.code == 200) {
                    android.util.Log.d("AddIngredient", "✅ 添加成功！")
                    Toast.makeText(this@AddIngredientActivity, "添加成功！", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    val errorMsg = response.body()?.message ?: "添加失败"
                    android.util.Log.e("AddIngredient", "❌ 添加失败: $errorMsg")
                    Toast.makeText(
                        this@AddIngredientActivity,
                        errorMsg,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("AddIngredient", "❌ 异常: ${e.message}", e)
                Toast.makeText(
                    this@AddIngredientActivity,
                    "网络错误: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            } finally {
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "保存"
                android.util.Log.d("AddIngredient", "========== 保存流程结束 ==========")
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
        android.util.Log.d("AddIngredient", "--- 开始获取或创建食材: $name ---")
        
        // 先搜索是否已存在
        val searchResponse = ApiClient.getService().searchIngredients(name)
        android.util.Log.d("AddIngredient", "搜索食材响应: ${searchResponse.isSuccessful}, code=${searchResponse.body()?.code}")
        
        if (searchResponse.isSuccessful && searchResponse.body()?.code == 200) {
            val existingIngredients = searchResponse.body()?.data
            android.util.Log.d("AddIngredient", "找到 ${existingIngredients?.size ?: 0} 个相关食材")
            
            val exactMatch = existingIngredients?.find { it.name.equals(name, ignoreCase = true) }
            if (exactMatch != null) {
                android.util.Log.d("AddIngredient", "✅ 找到完全匹配的食材: ID=${exactMatch.id}, 名称=${exactMatch.name}")
                return exactMatch
            }
        }

        // 不存在则创建
        android.util.Log.d("AddIngredient", "食材不存在，开始创建...")
        val newIngredient = Ingredient(
            id = null,
            name = name,
            category = "其他",
            unit = null
        )
        android.util.Log.d("AddIngredient", "准备创建的食材: name=$name, category=其他, unit=null")

        val createResponse = ApiClient.getService().createIngredient(newIngredient)
        android.util.Log.d("AddIngredient", "创建食材响应: ${createResponse.isSuccessful}, code=${createResponse.body()?.code}")
        
        if (createResponse.isSuccessful && createResponse.body()?.code == 200) {
            val created = createResponse.body()?.data!!
            android.util.Log.d("AddIngredient", "✅ 食材创建成功！ID=${created.id}, 名称=${created.name}, 分类=${created.category}")
            return created
        }

        android.util.Log.e("AddIngredient", "❌ 创建食材失败: ${createResponse.body()?.message}")
        throw Exception("创建食材失败: ${createResponse.body()?.message ?: "未知错误"}")
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

