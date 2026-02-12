package com.familyrecipes.android.ui.recipe

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.familyrecipes.android.R
import com.familyrecipes.android.data.local.PreferenceManager
import com.familyrecipes.android.data.model.Recipe
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.databinding.ActivityRecipeDetailBinding
import kotlinx.coroutines.launch

/**
 * 菜谱详情页
 */
class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeDetailBinding
    private var recipeId: Long = 0
    private var recipe: Recipe? = null

    companion object {
        const val EXTRA_RECIPE_ID = "recipe_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
            setContentView(binding.root)

            recipeId = intent.getLongExtra(EXTRA_RECIPE_ID, 0)
            
            setupToolbar()
            loadRecipeDetail()
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(this, "初始化失败: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupToolbar() {
        // 简单设置返回按钮，不使用 ActionBar
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.toolbar.title = "菜谱详情"
    }

    private fun loadRecipeDetail() {
        if (recipeId == 0L) {
            android.widget.Toast.makeText(this, "菜谱ID无效", android.widget.Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().getRecipeDetail(recipeId)
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    recipe = response.body()?.data
                    recipe?.let { displayRecipe(it) }
                } else {
                    android.widget.Toast.makeText(
                        this@RecipeDetailActivity,
                        "加载失败：${response.body()?.message ?: "未知错误"}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    this@RecipeDetailActivity,
                    "网络错误：${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
                finish()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun displayRecipe(recipe: Recipe) {
        binding.contentLayout.visibility = View.VISIBLE
        
        // 设置标题
        binding.toolbar.title = recipe.name
        binding.tvName.text = recipe.name
        
        // 显示编辑按钮（如果是当前用户创建的菜谱）
        val currentUserId = PreferenceManager.userId
        val isLoggedIn = PreferenceManager.isLoggedIn()
        val authToken = PreferenceManager.authToken
        
        android.util.Log.d("RecipeDetail", """
            当前用户ID: $currentUserId
            菜谱创建者ID: ${recipe.creatorId}
            是否登录: $isLoggedIn
            Token: ${authToken?.take(20)}...
        """.trimIndent())
        
        // 暂时：如果是默认用户(id=1)创建的菜谱，也显示编辑按钮
        if ((currentUserId > 0 && recipe.creatorId == currentUserId) || 
            (recipe.creatorId == 1L && currentUserId == 0L)) {
            binding.btnEdit.visibility = View.VISIBLE
            binding.btnEdit.setOnClickListener {
                // 跳转到编辑页面
                val intent = android.content.Intent(this, EditRecipeActivity::class.java)
                intent.putExtra("recipe_id", recipe.id)
                startActivity(intent)
            }
        } else {
            binding.btnEdit.visibility = View.GONE
        }
        
        // 设置封面图片
        if (!recipe.coverImage.isNullOrEmpty()) {
            Glide.with(this)
                .load(recipe.coverImage)
                .placeholder(R.drawable.placeholder_recipe)
                .into(binding.ivCover)
            binding.ivCover.visibility = View.VISIBLE
        } else {
            binding.ivCover.visibility = View.GONE
        }
        
        // 设置创建者信息
        binding.tvCreator.text = "创建者：${recipe.creator?.username ?: "未知"}"
        
        // 设置标签
        binding.chipGroup.removeAllViews()
        recipe.tags?.forEach { tag ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                // 如果tagValue已经包含#，就不再添加；否则添加#
                val displayText = if (tag.tagValue.startsWith("#")) {
                    tag.tagValue
                } else {
                    "#${tag.tagValue}"
                }
                text = displayText
                isClickable = true
                isCheckable = false
                setChipBackgroundColorResource(R.color.primary)
                setTextColor(getColor(R.color.white))
                setOnClickListener {
                    // 点击tag跳转到搜索页面，搜索该tag（移除#前缀）
                    val searchTag = tag.tagValue.removePrefix("#")
                    searchByTag(searchTag)
                }
            }
            binding.chipGroup.addView(chip)
        }
        
        // 设置描述
        if (!recipe.description.isNullOrEmpty()) {
            binding.tvDescription.text = recipe.description
            binding.tvDescription.visibility = View.VISIBLE
        } else {
            binding.tvDescription.visibility = View.GONE
        }
        
        // 设置统计信息
        binding.tvViewCount.text = "${recipe.viewCount ?: 0}"
        binding.tvFavoriteCount.text = "${recipe.favoriteCount ?: 0}"
        
        // 设置烹饪时间和难度
        if (recipe.cookingTime != null) {
            binding.tvCookingTime.text = "${recipe.cookingTime}分钟"
            binding.tvCookingTime.visibility = View.VISIBLE
        } else {
            binding.tvCookingTime.visibility = View.GONE
        }
        
        if (recipe.difficulty != null) {
            binding.tvDifficulty.text = when (recipe.difficulty) {
                1 -> "简单"
                2 -> "较易"
                3 -> "适中"
                4 -> "较难"
                5 -> "困难"
                else -> "未知"
            }
            binding.tvDifficulty.visibility = View.VISIBLE
        } else {
            binding.tvDifficulty.visibility = View.GONE
        }
        
        // 设置份数
        if (recipe.servings != null) {
            binding.tvServings.text = "${recipe.servings}人份"
            binding.tvServings.visibility = View.VISIBLE
        } else {
            binding.tvServings.visibility = View.GONE
        }
        
        // 设置外部链接
        if (!recipe.externalRecipes.isNullOrEmpty()) {
            binding.cardExternalLinks.visibility = View.VISIBLE
            val adapter = ExternalLinkDetailAdapter(
                recipe.externalRecipes!!,
                onCopyClick = { url ->
                    // 复制链接到剪贴板
                    copyToClipboard(url)
                },
                onOpenClick = { url ->
                    // 点击外部链接，在浏览器中打开
                    android.util.Log.d("RecipeDetail", "准备打开外部链接: $url")
                    openUrlInBrowser(url)
                }
            )
            binding.recyclerExternalLinks.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
            binding.recyclerExternalLinks.adapter = adapter
        } else {
            binding.cardExternalLinks.visibility = View.GONE
        }
        
        // 设置收藏按钮
        updateFavoriteButton(recipe.isFavorite == true)
        binding.btnFavorite.setOnClickListener {
            toggleFavorite()
        }
        
        // 设置差评按钮
        updateDislikeButton(recipe.isDisliked == true)
        binding.btnDislike.setOnClickListener {
            toggleDislike()
        }
    }

    private fun updateFavoriteButton(isFavorite: Boolean) {
        if (isFavorite) {
            binding.btnFavorite.text = "已收藏"
            binding.btnFavorite.setIconResource(R.drawable.ic_favorite_filled)
        } else {
            binding.btnFavorite.text = "收藏"
            binding.btnFavorite.setIconResource(R.drawable.ic_favorite_border)
        }
    }
    
    private fun updateDislikeButton(isDisliked: Boolean) {
        if (isDisliked) {
            binding.btnDislike.setIconResource(R.drawable.ic_thumb_down)
            binding.btnDislike.strokeColor = getColorStateList(R.color.error)
        } else {
            binding.btnDislike.setIconResource(R.drawable.ic_thumb_down_outline)
            binding.btnDislike.strokeColor = getColorStateList(R.color.gray)
        }
    }

    private fun toggleFavorite() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().toggleFavorite(recipeId)
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    val isFavorite = response.body()?.data ?: false
                    updateFavoriteButton(isFavorite)
                    
                    // 更新收藏数
                    recipe?.let {
                        val newCount = if (isFavorite) {
                            (it.favoriteCount ?: 0) + 1
                        } else {
                            maxOf((it.favoriteCount ?: 0) - 1, 0)
                        }
                        recipe = it.copy(
                            isFavorite = isFavorite,
                            favoriteCount = newCount
                        )
                        binding.tvFavoriteCount.text = "$newCount"
                    }
                    
                    // 设置 RESULT_OK 以便列表刷新
                    setResult(RESULT_OK)
                    
                    val message = if (isFavorite) "已收藏" else "已取消收藏"
                    android.widget.Toast.makeText(this@RecipeDetailActivity, message, android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(
                        this@RecipeDetailActivity,
                        "操作失败",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    this@RecipeDetailActivity,
                    "网络错误",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
        }
    }
    
    private fun toggleDislike() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().toggleDislike(recipeId)
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    val isDisliked = response.body()?.data ?: false
                    updateDislikeButton(isDisliked)
                    
                    // 更新差评数
                    recipe?.let {
                        val newCount = if (isDisliked) {
                            (it.dislikeCount ?: 0) + 1
                        } else {
                            maxOf((it.dislikeCount ?: 0) - 1, 0)
                        }
                        recipe = it.copy(
                            isDisliked = isDisliked,
                            dislikeCount = newCount
                        )
                    }
                    
                    // 设置 RESULT_OK 以便列表刷新
                    setResult(RESULT_OK)
                    
                    val message = if (isDisliked) "已标记为不喜欢" else "已取消标记"
                    android.widget.Toast.makeText(this@RecipeDetailActivity, message, android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(
                        this@RecipeDetailActivity,
                        "操作失败",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    this@RecipeDetailActivity,
                    "网络错误",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 通过tag搜索
     */
    private fun searchByTag(tagValue: String) {
        val intent = android.content.Intent(this, com.familyrecipes.android.ui.search.SearchResultActivity::class.java).apply {
            putExtra(com.familyrecipes.android.ui.search.SearchResultActivity.EXTRA_KEYWORD, "#$tagValue")
            putExtra(com.familyrecipes.android.ui.search.SearchResultActivity.EXTRA_PRIORITY_TYPE, "relevance")
        }
        startActivity(intent)
    }
    
    /**
     * 复制链接到剪贴板
     */
    private fun copyToClipboard(url: String) {
        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("外部链接", url)
        clipboard.setPrimaryClip(clip)
        
        android.util.Log.d("RecipeDetail", "已复制链接: $url")
        android.widget.Toast.makeText(this, "链接已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 在浏览器中打开URL，避免被App拦截
     */
    private fun openUrlInBrowser(url: String) {
        try {
            android.util.Log.d("RecipeDetail", "尝试打开URL: $url")
            
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            
            // 添加标志，确保在新任务中打开
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            
            // 获取所有能处理此Intent的应用
            val packageManager = packageManager
            val activities = packageManager.queryIntentActivities(intent, 0)
            
            android.util.Log.d("RecipeDetail", "找到 ${activities.size} 个可以打开链接的应用")
            
            // 尝试找到浏览器应用（排除小红书等App）
            val browsers = activities.filter { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                // 排除已知的非浏览器应用
                !packageName.contains("xiaohongshu") && 
                !packageName.contains("xhs") &&
                (packageName.contains("chrome") || 
                 packageName.contains("browser") || 
                 packageName.contains("firefox") ||
                 packageName.contains("opera") ||
                 packageName.contains("edge") ||
                 packageName.contains("webview"))
            }
            
            android.util.Log.d("RecipeDetail", "找到 ${browsers.size} 个浏览器应用")
            
            if (browsers.isNotEmpty()) {
                // 如果找到浏览器，使用第一个浏览器
                intent.setPackage(browsers[0].activityInfo.packageName)
                android.util.Log.d("RecipeDetail", "使用浏览器: ${browsers[0].activityInfo.packageName}")
                startActivity(intent)
            } else {
                // 如果没有找到浏览器，显示选择器
                android.util.Log.d("RecipeDetail", "未找到浏览器，显示选择器")
                val chooser = android.content.Intent.createChooser(intent, "选择浏览器打开")
                startActivity(chooser)
            }
        } catch (e: Exception) {
            android.util.Log.e("RecipeDetail", "打开链接失败", e)
            android.widget.Toast.makeText(this, "无法打开链接: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}

/**
 * 外部链接详情页适配器（只读）
 */
class ExternalLinkDetailAdapter(
    private val links: List<com.familyrecipes.android.data.model.ExternalRecipe>,
    private val onCopyClick: (String) -> Unit,
    private val onOpenClick: (String) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<ExternalLinkDetailAdapter.LinkViewHolder>() {

    class LinkViewHolder(view: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val ivThumbnail: android.widget.ImageView = view.findViewById(com.familyrecipes.android.R.id.iv_thumbnail)
        val tvTitle: android.widget.TextView = view.findViewById(com.familyrecipes.android.R.id.tv_title)
        val tvSource: android.widget.TextView = view.findViewById(com.familyrecipes.android.R.id.tv_source)
        val btnCopy: android.widget.ImageView = view.findViewById(com.familyrecipes.android.R.id.btn_copy)
        val btnOpen: android.widget.ImageView = view.findViewById(com.familyrecipes.android.R.id.btn_open)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): LinkViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(com.familyrecipes.android.R.layout.item_external_link_detail, parent, false)
        return LinkViewHolder(view)
    }

    override fun onBindViewHolder(holder: LinkViewHolder, position: Int) {
        val link = links[position]
        
        holder.tvTitle.text = link.title
        holder.tvSource.text = link.source ?: "网络"
        
        // 加载缩略图
        if (!link.thumbnail.isNullOrEmpty()) {
            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(link.thumbnail)
                .placeholder(com.familyrecipes.android.R.drawable.placeholder_recipe)
                .into(holder.ivThumbnail)
        } else {
            holder.ivThumbnail.setImageResource(com.familyrecipes.android.R.drawable.placeholder_recipe)
        }
        
        // 复制按钮
        holder.btnCopy.setOnClickListener {
            onCopyClick(link.url)
        }
        
        // 打开按钮
        holder.btnOpen.setOnClickListener {
            onOpenClick(link.url)
        }
    }

    override fun getItemCount(): Int = links.size
}

