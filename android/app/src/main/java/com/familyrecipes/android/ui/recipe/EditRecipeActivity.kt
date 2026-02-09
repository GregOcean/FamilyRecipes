package com.familyrecipes.android.ui.recipe

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.familyrecipes.android.R
import com.familyrecipes.android.data.model.*
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.databinding.ActivityEditRecipeBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

/**
 * 添加/编辑菜谱页面
 */
class EditRecipeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditRecipeBinding
    private val selectedImages = mutableListOf<Uri>()
    private val externalLinks = mutableListOf<String>()
    private lateinit var imageAdapter: ImageGridAdapter
    private lateinit var linkAdapter: ExternalLinkAdapter
    
    // 选择图片的启动器
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages.addAll(uris)
            imageAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityEditRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupImageGrid()
        setupTags()
        setupExternalLinks()
        setupPublishButton()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupImageGrid() {
        imageAdapter = ImageGridAdapter(selectedImages) {
            // 点击添加按钮
            pickImageLauncher.launch("image/*")
        }
        
        binding.recyclerImages.apply {
            layoutManager = GridLayoutManager(this@EditRecipeActivity, 3)
            adapter = imageAdapter
        }
    }

    private fun setupTags() {
        // 时段标签
        val timeTags = listOf("早餐", "午餐", "晚餐", "夜宵", "下午茶", "点心")
        timeTags.forEach { tag ->
            val chip = Chip(this).apply {
                text = tag
                isCheckable = true
                setChipBackgroundColorResource(R.color.light_gray)
                setTextColor(getColor(R.color.text_primary))
            }
            binding.chipGroupTime.addView(chip)
        }

        // 类型标签
        val typeTags = listOf("汤", "炒菜", "面食", "糕点", "凉菜", "主食")
        typeTags.forEach { tag ->
            val chip = Chip(this).apply {
                text = tag
                isCheckable = true
                setChipBackgroundColorResource(R.color.light_gray)
                setTextColor(getColor(R.color.text_primary))
            }
            binding.chipGroupType.addView(chip)
        }

        // 主食材标签
        val ingredientTags = listOf("牛肉", "羊肉", "鸡肉", "猪肉", "鱼", "米", "面", "蔬菜")
        ingredientTags.forEach { tag ->
            val chip = Chip(this).apply {
                text = tag
                isCheckable = true
                setChipBackgroundColorResource(R.color.light_gray)
                setTextColor(getColor(R.color.text_primary))
            }
            binding.chipGroupIngredient.addView(chip)
        }

        // 特殊需求标签
        val specialTags = listOf("无葱姜蒜", "宝宝餐", "低糖", "低盐", "素食")
        specialTags.forEach { tag ->
            val chip = Chip(this).apply {
                text = tag
                isCheckable = true
                setChipBackgroundColorResource(R.color.light_gray)
                setTextColor(getColor(R.color.text_primary))
            }
            binding.chipGroupSpecial.addView(chip)
        }
    }

    private fun setupExternalLinks() {
        linkAdapter = ExternalLinkAdapter(externalLinks) { position ->
            // 删除链接
            externalLinks.removeAt(position)
            linkAdapter.notifyItemRemoved(position)
        }
        
        binding.recyclerExternalLinks.apply {
            layoutManager = LinearLayoutManager(this@EditRecipeActivity)
            adapter = linkAdapter
        }

        // 添加外部链接按钮
        binding.btnAddExternalLink.setOnClickListener {
            showAddLinkDialog()
        }
    }

    private fun showAddLinkDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_link, null)
        val etLinkUrl = dialogView.findViewById<EditText>(R.id.et_link_url)
        val etLinkTitle = dialogView.findViewById<EditText>(R.id.et_link_title)

        AlertDialog.Builder(this)
            .setTitle("添加外部链接")
            .setView(dialogView)
            .setPositiveButton("添加") { _, _ ->
                val url = etLinkUrl.text.toString().trim()
                val title = etLinkTitle.text.toString().trim()
                if (url.isNotEmpty()) {
                    val link = if (title.isNotEmpty()) "$title|$url" else url
                    externalLinks.add(link)
                    linkAdapter.notifyItemInserted(externalLinks.size - 1)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun setupPublishButton() {
        binding.btnPublish.setOnClickListener {
            val title = binding.etRecipeTitle.text.toString().trim()

            if (title.isEmpty()) {
                binding.etRecipeTitle.error = "请输入菜名"
                return@setOnClickListener
            }

            publishRecipe()
        }
    }

    private fun publishRecipe() {
        // 禁用发布按钮，防止重复提交
        binding.btnPublish.isEnabled = false
        binding.btnPublish.text = "发布中..."

        lifecycleScope.launch {
            try {
                // 1. 上传图片（如果有）
                val imageUrls = if (selectedImages.isNotEmpty()) {
                    uploadImages()
                } else {
                    emptyList()
                }

                // 2. 准备菜谱数据
                val title = binding.etRecipeTitle.text.toString().trim()
                val content = binding.etRecipeContent.text.toString().trim()
                val selectedTags = getSelectedTags()

                // 3. 准备外部链接（作为额外的图片或步骤）
                // 外部链接暂时存储在description中，后续可以优化
                val fullDescription = if (externalLinks.isNotEmpty()) {
                    val linksText = externalLinks.joinToString("\n") { link ->
                        val parts = link.split("|")
                        if (parts.size == 2) {
                            "${parts[0]}: ${parts[1]}"
                        } else {
                            link
                        }
                    }
                    "$content\n\n外部链接：\n$linksText"
                } else {
                    content
                }

                val recipe = Recipe(
                    id = null,
                    name = title,
                    description = fullDescription,
                    coverImage = imageUrls.firstOrNull(), // 主图
                    cookingTime = null,
                    difficulty = null,
                    servings = null,
                    creatorId = null, // 后端会从token中获取
                    viewCount = null,
                    favoriteCount = null,
                    recentlyCookedCount = null,
                    createdAt = null,
                    creator = null,
                    tags = null,
                    ingredients = null,
                    steps = null,
                    externalRecipes = null,
                    cooks = null,
                    isFavorite = null
                )

                // 4. 准备标签数据
                val tags = selectedTags.map { tagValue ->
                    RecipeTag(
                        id = null,
                        recipeId = null,
                        tagType = getTagType(tagValue),
                        tagValue = tagValue
                    )
                }

                // 5. 创建请求对象
                val request = CreateRecipeRequest(
                    recipe = recipe,
                    tags = tags,
                    ingredients = null, // 暂时不处理食材
                    steps = null, // 暂时不处理步骤
                    cookUserIds = null // 暂时不处理会做的人
                )

                // 6. 调用API创建菜谱
                val response = ApiClient.getService().createRecipe(request)

                if (response.isSuccessful && response.body()?.code == 200) {
                    android.widget.Toast.makeText(this@EditRecipeActivity, "发布成功！", android.widget.Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    android.widget.Toast.makeText(
                        this@EditRecipeActivity,
                        "发布失败：${response.body()?.message ?: "未知错误"}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    this@EditRecipeActivity,
                    "发布失败：${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            } finally {
                binding.btnPublish.isEnabled = true
                binding.btnPublish.text = "发布"
            }
        }
    }

    private suspend fun uploadImages(): List<String> {
        val uploadedUrls = mutableListOf<String>()
        
        for (uri in selectedImages) {
            try {
                // 将URI转换为文件
                val file = uriToFile(uri)
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                // 上传图片
                val response = ApiClient.getService().uploadImage(body)
                if (response.isSuccessful && response.body()?.code == 200) {
                    response.body()?.data?.let { uploadedUrls.add(it) }
                }

                // 删除临时文件
                file.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return uploadedUrls
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val tempFile = File(cacheDir, "temp_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(tempFile)
        
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        
        return tempFile
    }

    private fun getTagType(tagValue: String): String {
        // 根据标签值判断标签类型
        return when (tagValue) {
            in listOf("早餐", "午餐", "晚餐", "夜宵", "下午茶", "点心") -> "time"
            in listOf("汤", "炒菜", "面食", "糕点", "凉菜", "主食") -> "type"
            in listOf("牛肉", "羊肉", "鸡肉", "猪肉", "鱼", "米", "面", "蔬菜") -> "ingredient"
            in listOf("无葱姜蒜", "宝宝餐", "低糖", "低盐", "素食") -> "special"
            else -> "other"
        }
    }

    private fun getSelectedTags(): List<String> {
        val tags = mutableListOf<String>()
        
        // 收集所有选中的标签
        for (i in 0 until binding.chipGroupTime.childCount) {
            val chip = binding.chipGroupTime.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                tags.add(chip.text.toString())
            }
        }
        
        for (i in 0 until binding.chipGroupType.childCount) {
            val chip = binding.chipGroupType.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                tags.add(chip.text.toString())
            }
        }
        
        for (i in 0 until binding.chipGroupIngredient.childCount) {
            val chip = binding.chipGroupIngredient.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                tags.add(chip.text.toString())
            }
        }
        
        for (i in 0 until binding.chipGroupSpecial.childCount) {
            val chip = binding.chipGroupSpecial.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                tags.add(chip.text.toString())
            }
        }
        
        return tags
    }
}

/**
 * 图片网格适配器
 */
class ImageGridAdapter(
    private val images: MutableList<Uri>,
    private val onAddClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_IMAGE = 0
        const val TYPE_ADD = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < images.size) TYPE_IMAGE else TYPE_ADD
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_IMAGE) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_image_preview, parent, false)
            ImageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_add_image, parent, false)
            AddImageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ImageViewHolder -> {
                holder.imageView.setImageURI(images[position])
                holder.btnDelete.setOnClickListener {
                    images.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, itemCount)
                }
            }
            is AddImageViewHolder -> {
                holder.itemView.setOnClickListener {
                    onAddClick()
                }
            }
        }
    }

    override fun getItemCount(): Int = images.size + 1 // +1 for add button

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.iv_image)
        val btnDelete: View = view.findViewById(R.id.btn_delete)
    }

    class AddImageViewHolder(view: View) : RecyclerView.ViewHolder(view)
}

/**
 * 外部链接适配器
 */
class ExternalLinkAdapter(
    private val links: MutableList<String>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ExternalLinkAdapter.LinkViewHolder>() {

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): LinkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_external_link, parent, false)
        return LinkViewHolder(view)
    }

    override fun onBindViewHolder(holder: LinkViewHolder, position: Int) {
        val linkData = links[position]
        val parts = linkData.split("|")
        
        if (parts.size == 2) {
            holder.tvTitle.text = parts[0]
            holder.tvUrl.text = parts[1]
        } else {
            holder.tvTitle.text = "外部链接 ${position + 1}"
            holder.tvUrl.text = linkData
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount(): Int = links.size

    class LinkViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: android.widget.TextView = view.findViewById(R.id.tv_link_title)
        val tvUrl: android.widget.TextView = view.findViewById(R.id.tv_link_url)
        val btnDelete: View = view.findViewById(R.id.btn_delete_link)
    }
}

