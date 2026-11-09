package com.familyrecipes.android.ui.recipe

import com.familyrecipes.android.ui.design.UiStrings
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

    override fun onResume(){super.onResume();if(::binding.isInitialized && recipe!=null)loadRecipeDetail()}

    private fun setupToolbar() {
        // 简单设置返回按钮，不使用 ActionBar
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.toolbar.title = UiStrings.s(com.familyrecipes.android.R.string.ui_fbb7e7ea47)
    }

    private fun loadRecipeDetail() {
        if (recipeId == 0L) {
            android.widget.Toast.makeText(this, UiStrings.s(com.familyrecipes.android.R.string.ui_a6b4953271), android.widget.Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val historyAccount=com.familyrecipes.android.data.local.LocalStore.account
                val response = ApiClient.getService().getRecipeDetail(recipeId)
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    recipe = response.body()?.data
                    recipe?.let { item -> displayRecipe(item); try { com.familyrecipes.android.data.local.RecipeHistory.record(historyAccount,item) } catch(e:kotlinx.coroutines.CancellationException){throw e} catch(_:Exception){ /* History must not prevent reading a recipe. */ } }
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
                    UiStrings.f(com.familyrecipes.android.R.string.network_error_value, e.message),
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
        
        binding.btnEdit.visibility=if(currentUserId>0 && recipe.creatorId==currentUserId) View.VISIBLE else View.GONE
        binding.btnEdit.setOnClickListener{startActivity(android.content.Intent(this,EditRecipeActivity::class.java).putExtra("recipe_id",recipe.id))}
        // 设置封面图片
        if (!recipe.coverImage.isNullOrEmpty()) {
            Glide.with(this)
                .load(com.familyrecipes.android.util.MediaImages.model(recipe.coverImage))
                .signature(com.bumptech.glide.signature.ObjectKey(com.familyrecipes.android.data.local.PreferenceManager.userId))
                .placeholder(R.drawable.placeholder_recipe)
                .into(binding.ivCover)
            binding.ivCover.visibility = View.VISIBLE
        } else {
            binding.ivCover.visibility = View.GONE
        }
        
        // 设置创建者信息
        binding.tvCreator.text = UiStrings.f(com.familyrecipes.android.R.string.creator_value, recipe.creator?.username ?: UiStrings.s(com.familyrecipes.android.R.string.ui_1622dc9b6b))
        
        // 设置标签
        binding.chipGroup.removeAllViews()
        recipe.tags?.filter { it.tagValue.trim().removePrefix("#") !in listOf("", "待确认") }?.distinctBy { it.tagValue }?.forEach { tag ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                // 如果tagValue已经包含#，就不再添加；否则添加#
                val displayText = "#${(tag.displayValue ?: tag.tagValue).removePrefix("#")}"
                text = displayText
                isClickable = true
                isCheckable = false
                setChipBackgroundColorResource(R.color.primary)
                setTextColor(getColor(R.color.on_primary))
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
        
        binding.gallery.removeAllViews()
        recipe.images.orEmpty().forEach { photo ->
            val image=android.widget.ImageView(this).apply { adjustViewBounds=true }
            binding.gallery.addView(image,android.widget.LinearLayout.LayoutParams(-1,-2))
            com.bumptech.glide.Glide.with(this).load(com.familyrecipes.android.util.MediaImages.model(photo.url))
                .signature(com.bumptech.glide.signature.ObjectKey(com.familyrecipes.android.data.local.PreferenceManager.userId)).into(image)
            if(!photo.caption.isNullOrBlank()) binding.gallery.addView(android.widget.TextView(this).apply { text=photo.caption;setPadding(0,8,0,24) })
        }
        val ui=com.familyrecipes.android.ui.design.Ui
        recipe.sourceRecipeName?.let{binding.gallery.addView(ui.text(this,UiStrings.f(com.familyrecipes.android.R.string.adapted_value, it),15f))}
        binding.gallery.addView(ui.text(this,UiStrings.s(com.familyrecipes.android.R.string.ui_c45c4747bb),21f,true))
        recipe.ingredients.orEmpty().forEach{binding.gallery.addView(ui.text(this,"${it.ingredient?.name.orEmpty()}  ${it.amount.orEmpty()}"))}
        binding.gallery.addView(ui.text(this,UiStrings.s(com.familyrecipes.android.R.string.ui_fea10d57e2),21f,true))
        recipe.steps.orEmpty().forEach{step->binding.gallery.addView(ui.text(this,"${step.stepNumber}. ${step.description}"));step.image?.let{url->val image=android.widget.ImageView(this).apply{adjustViewBounds=true};binding.gallery.addView(image);Glide.with(this).load(com.familyrecipes.android.util.MediaImages.model(url)).into(image)}}
        binding.gallery.addView(ui.button(this,UiStrings.s(com.familyrecipes.android.R.string.ui_ad1a148b7b)){
            startActivity(android.content.Intent(this,EditRecipeActivity::class.java).putExtra("adapt_source_id",recipeId))
        })
        val known=recipe.cooks.orEmpty().any{it.id==currentUserId}
        binding.gallery.addView(ui.button(this,if(known)UiStrings.s(com.familyrecipes.android.R.string.ui_c07569f86e) else UiStrings.s(com.familyrecipes.android.R.string.ui_0191cf83a3)){
            lifecycleScope.launch{try{ui.ok(ApiClient.getService().setKnown(recipeId,mapOf("known" to !known)));loadRecipeDetail()}catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){ui.toast(this@RecipeDetailActivity,e)}}
        })
        listOf("order" to UiStrings.s(com.familyrecipes.android.R.string.ui_b26b310008),"prep" to UiStrings.s(com.familyrecipes.android.R.string.ui_7a73e91a1d)).forEach{(kind,label)->binding.gallery.addView(ui.button(this,label){startActivity(android.content.Intent(this,com.familyrecipes.android.ui.meal.MealEditorActivity::class.java).putExtra("kind",kind).putExtra("recipe_id",recipe.id).putExtra("recipe_name",recipe.name))})}
        binding.gallery.addView(ui.button(this,UiStrings.s(com.familyrecipes.android.R.string.ui_00f9d79879)){startActivity(android.content.Intent(this,CookingRecordActivity::class.java).putExtra("recipe_id",recipe.id))})
        val history=android.widget.LinearLayout(this).apply{orientation=android.widget.LinearLayout.VERTICAL};binding.gallery.addView(history)
        lifecycleScope.launch{try{val records=ui.data(ApiClient.getService().cookingRecords(recipeId));history.addView(ui.text(this@RecipeDetailActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_a40e588383),21f,true));records.forEach{r->history.addView(ui.text(this@RecipeDetailActivity,"${r.cookedAt.take(10)}\n${r.notes}"));com.google.gson.Gson().fromJson(r.images,Array<String>::class.java).orEmpty().forEach{url->val image=android.widget.ImageView(this@RecipeDetailActivity).apply{adjustViewBounds=true};history.addView(image);Glide.with(this@RecipeDetailActivity).load(com.familyrecipes.android.util.MediaImages.model(url)).into(image)}}}catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){history.addView(ui.text(this@RecipeDetailActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_f24e502e92)))}}
        // 设置统计信息
        binding.tvViewCount.text = getString(R.string.recipe_views_compact, recipe.viewCount ?: 0)
        binding.tvFavoriteCount.text = getString(R.string.recipe_favorites_compact, recipe.favoriteCount ?: 0)
        
        // 设置烹饪时间和难度
        if (recipe.cookingTime != null) {
            binding.tvCookingTime.text = UiStrings.f(com.familyrecipes.android.R.string.minutes_value, recipe.cookingTime)
            binding.tvCookingTime.visibility = View.VISIBLE
        } else {
            binding.tvCookingTime.visibility = View.GONE
        }
        
        if (recipe.difficulty != null) {
            binding.tvDifficulty.text = UiStrings.domain(when (recipe.difficulty) {
                1 -> "简单"
                2 -> "较易"
                3 -> "适中"
                4 -> "较难"
                5 -> "困难"
                else -> UiStrings.s(com.familyrecipes.android.R.string.ui_1622dc9b6b)
            })
            binding.tvDifficulty.visibility = View.VISIBLE
        } else {
            binding.tvDifficulty.visibility = View.GONE
        }
        
        // 设置份数
        if (recipe.servings != null) {
            binding.tvServings.text = UiStrings.f(com.familyrecipes.android.R.string.servings_value, recipe.servings)
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
            binding.btnFavorite.text = UiStrings.s(com.familyrecipes.android.R.string.ui_934ebd87e6)
            binding.btnFavorite.setIconResource(R.drawable.ic_favorite_filled)
        } else {
            binding.btnFavorite.text = UiStrings.s(com.familyrecipes.android.R.string.ui_ae336cd047)
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
        // 检查登录状态
        if (!com.familyrecipes.android.util.AuthUtil.requireLogin(this, UiStrings.s(com.familyrecipes.android.R.string.ui_ae336cd047))) {
            return
        }
        
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
                        binding.tvFavoriteCount.text = getString(R.string.recipe_favorites_compact, newCount)
                    }
                    
                    // 设置 RESULT_OK 以便列表刷新
                    setResult(RESULT_OK)
                    
                    val message = if (isFavorite) UiStrings.s(com.familyrecipes.android.R.string.ui_934ebd87e6) else UiStrings.s(com.familyrecipes.android.R.string.ui_3a906c714a)
                    android.widget.Toast.makeText(this@RecipeDetailActivity, message, android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(
                        this@RecipeDetailActivity,
                        UiStrings.s(com.familyrecipes.android.R.string.ui_5fa802bef5),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    this@RecipeDetailActivity,
                    UiStrings.s(com.familyrecipes.android.R.string.ui_0bffc62bb3),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
        }
    }
    
    private fun toggleDislike() {
        // 检查登录状态
        if (!com.familyrecipes.android.util.AuthUtil.requireLogin(this, UiStrings.s(com.familyrecipes.android.R.string.ui_e62fbccc78))) {
            return
        }
        
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
                    
                    val message = if (isDisliked) UiStrings.s(com.familyrecipes.android.R.string.ui_6d02714217) else UiStrings.s(com.familyrecipes.android.R.string.ui_bf709ae4c8)
                    android.widget.Toast.makeText(this@RecipeDetailActivity, message, android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(
                        this@RecipeDetailActivity,
                        UiStrings.s(com.familyrecipes.android.R.string.ui_5fa802bef5),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    this@RecipeDetailActivity,
                    UiStrings.s(com.familyrecipes.android.R.string.ui_0bffc62bb3),
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
        val clip = android.content.ClipData.newPlainText(UiStrings.s(com.familyrecipes.android.R.string.ui_42ca66bafb), url)
        clipboard.setPrimaryClip(clip)
        
        android.util.Log.d("RecipeDetail", "已复制链接: $url")
        android.widget.Toast.makeText(this, UiStrings.s(com.familyrecipes.android.R.string.ui_deb26c26fb), android.widget.Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 使用完整链接打开系统默认的应用或浏览器
     */
    private fun openUrlInBrowser(url: String) {
        try {
            val target = com.familyrecipes.android.util.ExternalLinkUrl.validate(url)
            // Let Android route the complete deep link to the user's preferred app/browser.
            // Never launch a package's home activity or reconstruct its query parameters.
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(target))
                .addCategory(android.content.Intent.CATEGORY_BROWSABLE)
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, UiStrings.s(R.string.ui_72c1c093e4), android.widget.Toast.LENGTH_LONG).show()
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
        val tvNotes: android.widget.TextView = view.findViewById(com.familyrecipes.android.R.id.tv_link_notes)
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
        holder.tvSource.text = link.source ?: UiStrings.s(com.familyrecipes.android.R.string.ui_7ddbe15c84)
        holder.tvNotes.text=link.notes.orEmpty()
        holder.tvNotes.visibility=if(link.notes.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
        
        // 加载缩略图
        if (!link.thumbnail.isNullOrEmpty()) {
            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(com.familyrecipes.android.util.MediaImages.model(link.thumbnail))
                .signature(com.bumptech.glide.signature.ObjectKey(com.familyrecipes.android.data.local.PreferenceManager.userId))
                .placeholder(com.familyrecipes.android.R.drawable.placeholder_recipe)
                .into(holder.ivThumbnail)
        } else {
            holder.ivThumbnail.setImageResource(com.familyrecipes.android.R.drawable.placeholder_recipe)
        }
        
        holder.itemView.setOnClickListener { onOpenClick(link.url) }
        holder.tvTitle.setOnClickListener { onOpenClick(link.url) }
        holder.tvSource.setOnClickListener { onOpenClick(link.url) }

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

