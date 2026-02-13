package com.familyrecipes.android.ui.recipe

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
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
import com.familyrecipes.android.util.WebPageParser
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
    private val externalLinks = mutableListOf<ParsedExternalLink>()  // 改为结构化数据
    private lateinit var imageAdapter: ImageGridAdapter
    private lateinit var linkAdapter: ExternalLinkAdapter
    
    private var recipeId: Long? = null  // 如果是编辑模式，这里保存菜谱ID
    private var existingRecipe: Recipe? = null  // 保存现有菜谱数据
    
    /**
     * 解析后的外部链接数据
     */
    data class ParsedExternalLink(
        val title: String,
        val url: String,
        val source: String,
        val thumbnail: String?
    )
    
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

        // 检查是否是编辑模式
        recipeId = intent.getLongExtra("recipe_id", -1L).takeIf { it > 0 }
        
        setupToolbar()
        setupImageGrid()
        setupExternalLinks()
        setupPublishButton()
        
        // 如果是编辑模式，加载现有数据
        if (recipeId != null) {
            binding.toolbar.title = "编辑菜谱"
            binding.btnPublish.text = "保存"
            loadExistingRecipe(recipeId!!)
        }
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
        val progressParsing = dialogView.findViewById<ProgressBar>(R.id.progress_parsing)
        val tvParsingStatus = dialogView.findViewById<TextView>(R.id.tv_parsing_status)

        val dialog = AlertDialog.Builder(this)
            .setTitle("添加外部链接")
            .setView(dialogView)
            .setPositiveButton("添加", null)  // 先设置为null，稍后手动处理
            .setNegativeButton("取消", null)
            .create()
        
        dialog.show()
        
        // 手动处理添加按钮点击
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val url = etLinkUrl.text.toString().trim()
            
            if (url.isEmpty()) {
                android.widget.Toast.makeText(this, "请输入链接地址", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                android.widget.Toast.makeText(this, "请输入有效的网址（以http://或https://开头）", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // 禁用按钮，显示解析进度
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
            etLinkUrl.isEnabled = false
            progressParsing.visibility = View.VISIBLE
            tvParsingStatus.visibility = View.VISIBLE
            tvParsingStatus.text = "正在解析网页..."
            
            // 异步解析网页
            lifecycleScope.launch {
                try {
                    val parsed = WebPageParser.parseUrl(url)
                    
                    if (parsed != null) {
                        // 解析成功
                        val link = ParsedExternalLink(
                            title = parsed.title,
                            url = parsed.originalUrl,
                            source = parsed.source,
                            thumbnail = parsed.thumbnailUrl
                        )
                        externalLinks.add(link)
                        linkAdapter.notifyItemInserted(externalLinks.size - 1)
                        
                        android.widget.Toast.makeText(
                            this@EditRecipeActivity,
                            "添加成功：${parsed.title}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    } else {
                        // 解析失败，使用默认值
                        tvParsingStatus.text = "解析失败，使用默认信息"
                        val link = ParsedExternalLink(
                            title = "外部菜谱",
                            url = url,
                            source = "网络",
                            thumbnail = null
                        )
                        externalLinks.add(link)
                        linkAdapter.notifyItemInserted(externalLinks.size - 1)
                        dialog.dismiss()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EditRecipe", "解析链接失败", e)
                    tvParsingStatus.text = "解析失败：${e.message}"
                    
                    // 仍然可以添加，但使用默认值
                    android.widget.Toast.makeText(
                        this@EditRecipeActivity,
                        "无法解析网页，已使用默认信息添加",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    
                    val link = ParsedExternalLink(
                        title = "外部菜谱",
                        url = url,
                        source = "网络",
                        thumbnail = null
                    )
                    externalLinks.add(link)
                    linkAdapter.notifyItemInserted(externalLinks.size - 1)
                    dialog.dismiss()
                } finally {
                    // 恢复按钮状态
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = true
                    etLinkUrl.isEnabled = true
                    progressParsing.visibility = View.GONE
                }
            }
        }
    }

    private fun setupPublishButton() {
        binding.btnPublish.setOnClickListener {
            val title = binding.etRecipeTitle.text.toString().trim()

            if (title.isEmpty()) {
                binding.etRecipeTitle.error = "请输入菜名"
                return@setOnClickListener
            }

            if (recipeId != null) {
                updateRecipe()
            } else {
                publishRecipe()
            }
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
                val tags = parseTags(binding.etTags.text.toString())
                val currentUserId = com.familyrecipes.android.data.local.PreferenceManager.userId

                val recipe = Recipe(
                    id = null,
                    name = title,
                    description = content,
                    coverImage = imageUrls.firstOrNull(), // 主图
                    cookingTime = null,
                    difficulty = null,
                    servings = null,
                    creatorId = if (currentUserId > 0) currentUserId else null,
                    viewCount = null,
                    favoriteCount = null,
                    dislikeCount = null,
                    recentlyCookedCount = null,
                    createdAt = null,
                    creator = null,
                    tags = null,
                    ingredients = null,
                    steps = null,
                    externalRecipes = null,
                    cooks = null,
                    isFavorite = null,
                    isDisliked = null
                )

                // 3. 准备标签数据
                val tagObjects = tags.map { tagValue ->
                    RecipeTag(
                        id = null,
                        recipeId = null,
                        tagType = "custom", // 自定义标签都是custom类型
                        tagValue = tagValue
                    )
                }
                
                // 4. 准备外部链接数据
                val externalRecipeObjects = externalLinks.map { link ->
                    ExternalRecipe(
                        id = null,
                        recipeId = null,
                        title = link.title,
                        url = link.url,
                        source = link.source,
                        thumbnail = link.thumbnail,
                        addedBy = if (currentUserId > 0) currentUserId else null,
                        createdAt = null
                    )
                }

                // 5. 创建请求对象
                val request = CreateRecipeRequest(
                    recipe = recipe,
                    tags = tagObjects,
                    ingredients = null, // 暂时不处理食材
                    steps = null, // 暂时不处理步骤
                    cookUserIds = null, // 暂时不处理会做的人
                    externalRecipes = externalRecipeObjects  // 添加外部链接
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
    
    private fun updateRecipe() {
        // 禁用按钮，防止重复提交
        binding.btnPublish.isEnabled = false
        binding.btnPublish.text = "保存中..."

        lifecycleScope.launch {
            try {
                // 1. 上传新图片（如果有）
                val imageUrls = if (selectedImages.isNotEmpty()) {
                    uploadImages()
                } else {
                    // 保留原有封面
                    listOfNotNull(existingRecipe?.coverImage)
                }

                // 2. 准备菜谱数据
                val title = binding.etRecipeTitle.text.toString().trim()
                val content = binding.etRecipeContent.text.toString().trim()
                val tags = parseTags(binding.etTags.text.toString())

                val recipe = Recipe(
                    id = recipeId,
                    name = title,
                    description = content,
                    coverImage = imageUrls.firstOrNull(),
                    cookingTime = existingRecipe?.cookingTime,
                    difficulty = existingRecipe?.difficulty,
                    servings = existingRecipe?.servings,
                    creatorId = existingRecipe?.creatorId,
                    viewCount = null,
                    favoriteCount = null,
                    dislikeCount = null,
                    recentlyCookedCount = null,
                    createdAt = null,
                    creator = null,
                    tags = null,
                    ingredients = null,
                    steps = null,
                    externalRecipes = null,
                    cooks = null,
                    isFavorite = null,
                    isDisliked = null
                )

                // 3. 准备标签数据
                val tagObjects = tags.map { tagValue ->
                    RecipeTag(
                        id = null,
                        recipeId = recipeId,
                        tagType = "custom",
                        tagValue = tagValue
                    )
                }
                
                // 4. 准备外部链接数据
                val currentUserId = com.familyrecipes.android.data.local.PreferenceManager.userId
                val externalRecipeObjects = externalLinks.map { link ->
                    ExternalRecipe(
                        id = null,
                        recipeId = recipeId,
                        title = link.title,
                        url = link.url,
                        source = link.source,
                        thumbnail = link.thumbnail,
                        addedBy = if (currentUserId > 0) currentUserId else null,
                        createdAt = null
                    )
                }

                // 5. 创建请求对象
                val request = CreateRecipeRequest(
                    recipe = recipe,
                    tags = tagObjects,
                    ingredients = null,
                    steps = null,
                    cookUserIds = null,
                    externalRecipes = externalRecipeObjects  // 添加外部链接
                )

                // 6. 调用API更新菜谱
                val response = ApiClient.getService().updateRecipe(recipeId!!, request)

                if (response.isSuccessful && response.body()?.code == 200) {
                    android.widget.Toast.makeText(this@EditRecipeActivity, "保存成功！", android.widget.Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    android.widget.Toast.makeText(
                        this@EditRecipeActivity,
                        "保存失败：${response.body()?.message ?: "未知错误"}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    this@EditRecipeActivity,
                    "保存失败：${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            } finally {
                binding.btnPublish.isEnabled = true
                binding.btnPublish.text = "保存"
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

    /**
     * 解析tag输入
     * 支持#开头的tag，用空格分隔
     * 例如：#午餐 #家常菜 #快手 -> ["午餐", "家常菜", "快手"]
     */
    private fun parseTags(input: String): List<String> {
        if (input.isBlank()) return emptyList()
        
        // 按空格分割
        return input.trim()
            .split(Regex("\\s+"))
            .mapNotNull { tag ->
                // 移除#前缀，过滤空字符串
                val cleaned = tag.trim().removePrefix("#")
                if (cleaned.isNotEmpty()) cleaned else null
            }
            .distinct() // 去重
    }
    
    /**
     * 加载现有菜谱数据
     */
    private fun loadExistingRecipe(id: Long) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().getRecipeDetail(id)
                if (response.isSuccessful && response.body()?.code == 200) {
                    existingRecipe = response.body()?.data
                    displayExistingRecipe(existingRecipe!!)
                } else {
                    android.widget.Toast.makeText(
                        this@EditRecipeActivity,
                        "加载菜谱失败",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    this@EditRecipeActivity,
                    "加载菜谱失败：${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
                finish()
            }
        }
    }
    
    /**
     * 显示现有菜谱数据
     */
    private fun displayExistingRecipe(recipe: Recipe) {
        // 填充菜名
        binding.etRecipeTitle.setText(recipe.name)
        
        // 填充内容描述
        binding.etRecipeContent.setText(recipe.description)
        
        // 填充tags（确保带#号显示）
        val tagsText = recipe.tags?.joinToString(" ") { 
            if (it.tagValue.startsWith("#")) {
                it.tagValue
            } else {
                "#${it.tagValue}"
            }
        } ?: ""
        binding.etTags.setText(tagsText)
        
        // 加载外部链接
        recipe.externalRecipes?.forEach { externalRecipe ->
            val link = ParsedExternalLink(
                title = externalRecipe.title,
                url = externalRecipe.url,
                source = externalRecipe.source ?: "网络",
                thumbnail = externalRecipe.thumbnail
            )
            externalLinks.add(link)
        }
        linkAdapter.notifyDataSetChanged()
        
        // TODO: 加载图片（需要将URL转换为Uri）
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
    private val links: MutableList<EditRecipeActivity.ParsedExternalLink>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ExternalLinkAdapter.LinkViewHolder>() {

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): LinkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_external_link, parent, false)
        return LinkViewHolder(view)
    }

    override fun onBindViewHolder(holder: LinkViewHolder, position: Int) {
        val link = links[position]
        
        holder.tvTitle.text = link.title
        holder.tvSource.text = "来源：${link.source}"
        holder.tvUrl.text = link.url
        
        // 加载缩略图
        if (link.thumbnail != null) {
            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(link.thumbnail)
                .placeholder(R.drawable.ic_food)
                .error(R.drawable.ic_food)
                .into(holder.ivThumbnail)
        } else {
            holder.ivThumbnail.setImageResource(R.drawable.ic_food)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount(): Int = links.size

    class LinkViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.iv_thumbnail)
        val tvTitle: android.widget.TextView = view.findViewById(R.id.tv_link_title)
        val tvSource: android.widget.TextView = view.findViewById(R.id.tv_link_source)
        val tvUrl: android.widget.TextView = view.findViewById(R.id.tv_link_url)
        val btnDelete: View = view.findViewById(R.id.btn_delete_link)
    }
}

