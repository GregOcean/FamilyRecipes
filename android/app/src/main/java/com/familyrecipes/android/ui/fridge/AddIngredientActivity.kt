package com.familyrecipes.android.ui.fridge

import com.familyrecipes.android.ui.design.UiStrings
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.core.widget.doAfterTextChanged
import com.familyrecipes.android.data.local.*
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

    private val account=LocalStore.account
    private var draftReady=false
    private var saved=false
    private data class InventoryDraft(val text:String,val image:String?,val reservedFor:Long?,val parsed:IngredientParser.ParseResult?=null)
    private var parsedDraft:IngredientParser.ParseResult?=null
    private val draftKey get()="inventory:$kitchenId"
    private val kitchenId = PreferenceManager.currentKitchenId
    private var reservedFor: Long? = null
    private var selectedInventoryCategory: Long? = null
    private var ingredientLookupJob: kotlinx.coroutines.Job? = null
    private lateinit var binding: ActivityAddIngredientBinding
    private var selectedImageUri: Uri? = null
    private var currentPhotoPath: String? = null

    // 图片选择器
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                lifecycleScope.launch {
                    try {
                        selectedImageUri=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            val file=java.io.File(filesDir,"inventory-${java.util.UUID.randomUUID()}")
                            contentResolver.openInputStream(uri)!!.use { input -> file.outputStream().use { input.copyTo(it) } }
                            android.net.Uri.fromFile(file)
                        }
                        saveDraft()
                        binding.ivPreview.setImageURI(selectedImageUri)
                        binding.ivPreview.visibility=android.view.View.VISIBLE
                        binding.btnRemoveImage.visibility=android.view.View.VISIBLE
                    } catch(e:kotlinx.coroutines.CancellationException) { throw e }
                      catch(_:Exception) { Toast.makeText(this@AddIngredientActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_715a3f2df4),Toast.LENGTH_SHORT).show() }
                }
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
                saveDraft()
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
        if(kitchenId==null) { android.widget.Toast.makeText(this,UiStrings.s(com.familyrecipes.android.R.string.ui_eb006cd6f1),android.widget.Toast.LENGTH_LONG).show();finish();return }
        binding.toolbar.inflateMenu(com.familyrecipes.android.R.menu.inventory_options)
        binding.toolbar.setOnMenuItemClickListener { chooseReservedFor(); true }
        binding.etIngredientInfo.isEnabled=false
        binding.etIngredientInfo.doAfterTextChanged { saveDraft() }
        lifecycleScope.launch {
            val text=LocalStore.instance.records().get(account,"draft",draftKey)
            if(text!=null && binding.etIngredientInfo.text.isNullOrEmpty()) {
                val draft=com.google.gson.Gson().fromJson(text,InventoryDraft::class.java)
                binding.etIngredientInfo.setText(draft.text);reservedFor=draft.reservedFor;parsedDraft=draft.parsed
                draft.image?.let { selectedImageUri=android.net.Uri.parse(it);binding.ivPreview.setImageURI(selectedImageUri);binding.ivPreview.visibility=android.view.View.VISIBLE }
            }
            draftReady=true
            binding.etIngredientInfo.isEnabled=true
            parsedDraft?.let { showParseResultDialog(it) }
        }

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
            saveDraft()
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
        val options = arrayOf(UiStrings.s(com.familyrecipes.android.R.string.ui_bed9ec1e84), UiStrings.s(com.familyrecipes.android.R.string.ui_83c39abd16))
        AlertDialog.Builder(this)
            .setTitle(UiStrings.s(com.familyrecipes.android.R.string.ui_67d5682523))
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
        if(parsedDraft!=null) { showParseResultDialog(parsedDraft!!);return }
        val inputText = binding.etIngredientInfo.text.toString().trim()

        if (inputText.isEmpty()) {
            Toast.makeText(this, UiStrings.s(com.familyrecipes.android.R.string.ui_9923b97d63), Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSave.isEnabled = false
        binding.btnSave.text = UiStrings.s(com.familyrecipes.android.R.string.ui_c5437f5fb7)

        lifecycleScope.launch {
            try {
                // 1. 解析输入信息
                val parseResult = IngredientParser.parse(inputText)

                // 2. 验证是否识别出食材名称
                if (parseResult.ingredientName.isNullOrEmpty()) {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = UiStrings.s(com.familyrecipes.android.R.string.ui_be5fbbe34c)
                    showParseFailureDialog()
                    return@launch
                }

                // 库存共享；预留对象仅用于标注。
                showParseResultDialog(parseResult)

            } catch (e: Exception) {
                Toast.makeText(
                    this@AddIngredientActivity,
                    "解析失败: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
                binding.btnSave.isEnabled = true
                binding.btnSave.text = UiStrings.s(com.familyrecipes.android.R.string.ui_be5fbbe34c)
            }
        }
    }

    /**
     * 显示解析失败对话框
     */
    private fun showParseFailureDialog() {
        AlertDialog.Builder(this)
            .setTitle(UiStrings.s(com.familyrecipes.android.R.string.ui_6346129bb0))
            .setMessage(UiStrings.s(com.familyrecipes.android.R.string.ui_0c7e7ae95f))
            .setPositiveButton(UiStrings.s(com.familyrecipes.android.R.string.ui_8347a927c0)) { dialog, _ ->
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
        selectedInventoryCategory = parseResult.categoryId
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
        dialog.setOnDismissListener { ingredientLookupJob?.cancel() }

        // 监听保质期输入变化，显示/隐藏警告
        dialogBinding.etExpiryDays.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val days = s.toString().toIntOrNull() ?: 0
                if (days in 1..3) {
                    dialogBinding.warningContainer.visibility = View.VISIBLE
                    dialogBinding.tvWarning.text = UiStrings.f(com.familyrecipes.android.R.string.expiry_days_warning, days)
                } else {
                    dialogBinding.warningContainer.visibility = View.GONE
                }
            }
        })

        // 初始检查保质期
        if (parseResult.expiryDays in 1..3) {
            dialogBinding.warningContainer.visibility = View.VISIBLE
            dialogBinding.tvWarning.text = UiStrings.f(com.familyrecipes.android.R.string.expiry_days_warning, parseResult.expiryDays)
        }

        fun preserveParsedDraft() {
            parsedDraft=IngredientParser.ParseResult(dialogBinding.etIngredientName.text.toString(),
                dialogBinding.etAmount.text.toString(),dialogBinding.etExpiryDays.text.toString().toIntOrNull() ?: 0,
                dialogBinding.etStorageLocation.text.toString(),parseResult.notes,reservedFor,selectedInventoryCategory)
            saveDraft()
        }
        listOf(dialogBinding.etIngredientName,dialogBinding.etAmount,dialogBinding.etExpiryDays,dialogBinding.etStorageLocation)
            .forEach { it.doAfterTextChanged { preserveParsedDraft() } }
        preserveParsedDraft()
        // 返回修改按钮
        dialogBinding.btnCancel.setOnClickListener {
            parsedDraft=null;saveDraft()
            dialog.dismiss()
            binding.btnSave.isEnabled = true
            binding.btnSave.text = UiStrings.s(com.familyrecipes.android.R.string.ui_be5fbbe34c)
        }

        // 确认保存按钮
        dialogBinding.btnSave.setOnClickListener {
            val name = dialogBinding.etIngredientName.text.toString().trim()
            val amount = dialogBinding.etAmount.text.toString().trim()
            val expiryDaysStr = dialogBinding.etExpiryDays.text.toString().trim()
            val storageLocation = dialogBinding.etStorageLocation.text.toString().trim()
            val ownerUserId = reservedFor

            // 验证输入
            if (name.isEmpty()) {
                Toast.makeText(this, UiStrings.s(com.familyrecipes.android.R.string.ui_2fbc875882), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (expiryDaysStr.isEmpty()) {
                Toast.makeText(this, UiStrings.s(com.familyrecipes.android.R.string.ui_421b4b18d2), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val expiryDays = expiryDaysStr.toIntOrNull()
            if (expiryDays == null || expiryDays <= 0) {
                Toast.makeText(this, UiStrings.s(com.familyrecipes.android.R.string.ui_5caf5e3a24), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (expiryDays > 3650) {
                Toast.makeText(this, UiStrings.s(com.familyrecipes.android.R.string.ui_13e858b0eb), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 创建修改后的结果
            val modifiedResult = IngredientParser.ParseResult(
                ingredientName = name,
                amount = amount.ifEmpty { null },
                expiryDays = expiryDays,
                storageLocation = storageLocation.ifEmpty { "冰箱冷藏" },
                notes = parseResult.notes,
                ownerUserId = ownerUserId,
                categoryId = selectedInventoryCategory
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
        val input = dialogBinding.etIngredientName
        input.threshold = 1
        var available = emptyList<com.familyrecipes.android.data.model.InventoryCategory>()
        var matched = false
        var lookupComplete = false
        dialogBinding.spinnerCategory.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) { }
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!lookupComplete) return
                selectedInventoryCategory = available.getOrNull(position)?.id
                parsedDraft = parsedDraft?.copy(categoryId = selectedInventoryCategory)
                saveDraft()
            }
        }
        fun lookup(reset: Boolean) {
            ingredientLookupJob?.cancel()
            lookupComplete = false
            dialogBinding.btnSave.isEnabled = false
            dialogBinding.spinnerCategory.isEnabled = false
            if (reset) selectedInventoryCategory = null
            val name = input.text.toString().trim()
            dialogBinding.tvIngredientMatch.text = if (name.isEmpty()) UiStrings.s(com.familyrecipes.android.R.string.ui_2fbc875882) else UiStrings.s(com.familyrecipes.android.R.string.ui_6ba5301b0b)
            if (name.isEmpty()) return
            ingredientLookupJob = lifecycleScope.launch {
                try {
                    kotlinx.coroutines.delay(250)
                    if (available.isEmpty()) {
                        val response = ApiClient.getService().getAllCategories()
                        check(response.isSuccessful && response.body()?.code == 200)
                        available = response.body()?.data.orEmpty()
                        check(available.isNotEmpty())
                        dialogBinding.spinnerCategory.adapter = ArrayAdapter(this@AddIngredientActivity,
                            android.R.layout.simple_spinner_dropdown_item, available.map { "${it.icon.orEmpty()} ${it.name}" })
                    }
                    val preview = ApiClient.getService().previewIngredient(name)
                    check(preview.isSuccessful && preview.body()?.code == 200)
                    val ingredient = requireNotNull(preview.body()?.data)
                    val suggestions = ApiClient.getService().searchIngredients(name)
                    check(suggestions.isSuccessful && suggestions.body()?.code == 200)
                    if (input.text.toString().trim() != name) return@launch
                    matched = ingredient.id != null
                    selectedInventoryCategory = selectedInventoryCategory ?: ingredient.categoryId
                    val index = available.indexOfFirst { it.id == selectedInventoryCategory }
                    check(index >= 0)
                    dialogBinding.spinnerCategory.setSelection(index)
                    dialogBinding.spinnerCategory.isEnabled = true
                    dialogBinding.tvIngredientMatch.text = if (matched)
                        "已匹配：${ingredient.name} · ${ingredient.category}（可调整本条库存分类）"
                        else UiStrings.s(com.familyrecipes.android.R.string.ui_c58dc542ee)
                    input.setAdapter(ArrayAdapter(this@AddIngredientActivity, android.R.layout.simple_dropdown_item_1line,
                        suggestions.body()?.data.orEmpty().map { it.name }))
                    if (input.hasFocus() && !matched && !suggestions.body()?.data.isNullOrEmpty()) input.showDropDown()
                    parsedDraft = parsedDraft?.copy(categoryId = selectedInventoryCategory)
                    saveDraft()
                    lookupComplete = true
                    dialogBinding.btnSave.isEnabled = true
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                  catch (_: Exception) {
                    dialogBinding.tvIngredientMatch.text = UiStrings.s(com.familyrecipes.android.R.string.ui_2bc418d3e3)
                }
            }
        }
        input.doAfterTextChanged { lookup(true) }
        dialogBinding.tvIngredientMatch.setOnClickListener { lookup(false) }
        lookup(false)
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
                    Toast.makeText(this, UiStrings.s(com.familyrecipes.android.R.string.ui_53a656c5e7), Toast.LENGTH_SHORT).show()
                    expiryDaysInput.setText("1")
                } else if (diffInDays == 0) {
                    Toast.makeText(this, UiStrings.s(com.familyrecipes.android.R.string.ui_26eb926612), Toast.LENGTH_SHORT).show()
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

        datePickerDialog.setTitle(UiStrings.s(com.familyrecipes.android.R.string.ui_b44d2867ab))
        datePickerDialog.show()
    }

    /**
     * 执行保存操作
     */
    private fun performSave(parseResult: IngredientParser.ParseResult) {
        binding.btnSave.text = UiStrings.s(com.familyrecipes.android.R.string.ui_2a33020e5b)

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
                val ingredient = getOrCreateIngredient(parseResult.ingredientName!!, null)
                android.util.Log.d("AddIngredient", "✅ 获取到食材: ID=${ingredient.id}, 名称=${ingredient.name}")

                // 3. 计算过期日期
                val expiryDate = IngredientParser.calculateExpiryDate(parseResult.expiryDays)
                android.util.Log.d("AddIngredient", "过期日期: $expiryDate")

                // 3.5 检查用户ID
                val currentUserId = PreferenceManager.userId
                android.util.Log.d("AddIngredient", "当前用户ID: $currentUserId")
                android.util.Log.d("AddIngredient", "是否已登录: ${PreferenceManager.isLoggedIn()}")

                if (currentUserId <= 0) {
                    android.util.Log.e("AddIngredient", "❌ 用户ID无效，无法添加食材")
                    Toast.makeText(this@AddIngredientActivity, UiStrings.s(com.familyrecipes.android.R.string.ui_47bfe84b0c), Toast.LENGTH_LONG).show()
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = UiStrings.s(com.familyrecipes.android.R.string.ui_be5fbbe34c)
                    return@launch
                }

                // 4. 创建食材项
                val fridgeItem = FridgeItem(
                    kitchenId = kitchenId,
                    id = null,
                    userId = currentUserId,
                    ingredientId = ingredient.id,
                    categoryOverrideId = parseResult.categoryId?.takeIf { it != ingredient.categoryId },
                    amount = parseResult.amount,
                    purchaseDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    expiryDate = expiryDate,
                    storageLocation = parseResult.storageLocation ?: "冰箱",  // 使用用户输入的存储位置
                    status = FridgeItem.STATUS_NORMAL,
                    notes = parseResult.notes,
                    ownerUserId = reservedFor,
                    imageUrl = imageUrl,
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
                    Toast.makeText(this@AddIngredientActivity, UiStrings.s(com.familyrecipes.android.R.string.ui_d730bee903), Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    saved=true
                    DraftWrites.clear(account,draftKey)
                    finish()
                } else {
                    val errorMsg = response.body()?.message ?: UiStrings.s(com.familyrecipes.android.R.string.ui_6452a05591)
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
                    UiStrings.f(com.familyrecipes.android.R.string.network_error_value, e.message),
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            } finally {
                binding.btnSave.isEnabled = true
                binding.btnSave.text = UiStrings.s(com.familyrecipes.android.R.string.ui_be5fbbe34c)
                android.util.Log.d("AddIngredient", "========== 保存流程结束 ==========")
            }
        }
    }

    /**
     * 上传图片
     */
    private fun saveDraft() {
        if(!draftReady || saved)return
        DraftWrites.put(account,draftKey,com.google.gson.Gson().toJson(InventoryDraft(binding.etIngredientInfo.text.toString(),selectedImageUri?.toString(),reservedFor,parsedDraft)))
    }
    override fun onStop() { saveDraft();super.onStop() }

    private fun chooseReservedFor() {
        lifecycleScope.launch {
            try {
                val kitchen=ApiClient.getService().getKitchens().body()?.data?.find { it.id==kitchenId } ?: return@launch
                val group=ApiClient.getService().getGroupDetail(requireNotNull(kitchen.groupId))
                check(group.isSuccessful && group.body()?.code==200)
                val members=group.body()?.data?.members.orEmpty()
                val labels=arrayOf(UiStrings.s(com.familyrecipes.android.R.string.ui_e0d982c2fd)) + members.map { it.user?.username ?: "成员 ${it.userId}" }
                androidx.appcompat.app.AlertDialog.Builder(this@AddIngredientActivity).setTitle(UiStrings.s(com.familyrecipes.android.R.string.ui_6cec1849f4))
                    .setItems(labels) { _, index -> reservedFor=if(index==0) null else members[index-1].userId;saveDraft() }.show()
            } catch(e: kotlinx.coroutines.CancellationException) { throw e }
              catch(_: Exception) { android.widget.Toast.makeText(this@AddIngredientActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_d27ba70966),android.widget.Toast.LENGTH_SHORT).show() }
        }
    }

    private suspend fun uploadImage(uri: Uri): String {
        val file = uriToFile(uri)
        try {
            val body = MultipartBody.Part.createFormData("file", file.name,
                file.asRequestBody("image/*".toMediaTypeOrNull()))
            val response = ApiClient.getService().uploadImage(body,"inventory")
            val url = response.body()?.data
            if (!response.isSuccessful || response.body()?.code != 200 || url.isNullOrBlank()) {
                throw java.io.IOException(UiStrings.s(com.familyrecipes.android.R.string.ui_40e3aba8c8))
            }
            return url
        } finally { file.delete() }
    }

    /**
     * 获取或创建食材
     * 原则：用户输入什么就保存什么，只查找完全匹配的食材
     */
    private suspend fun getOrCreateIngredient(name: String, categoryId: Long?): Ingredient {
        // Resolve exact name/alias and creation in one server operation; a failed search never creates a duplicate.
        val response = ApiClient.getService().createIngredient(Ingredient(id=null, name=name, category=null, categoryId=categoryId))
        if (!response.isSuccessful || response.body()?.code != 200)
            throw Exception(com.familyrecipes.android.data.remote.ApiErrors.message(response, UiStrings.s(com.familyrecipes.android.R.string.ui_1e1b18a33f)))
        return requireNotNull(response.body()?.data)
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

