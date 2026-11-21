package com.familyrecipes.android.ui.recipe

import com.familyrecipes.android.ui.design.UiStrings
import android.os.Bundle
import android.view.View
import androidx.core.view.doOnLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.familyrecipes.android.R
import com.familyrecipes.android.data.local.PreferenceManager
import com.familyrecipes.android.data.model.Recipe
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.databinding.ActivityRecipeDetailBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import com.familyrecipes.android.ui.design.Ui

/**
 * 菜谱详情页
 */
class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeDetailBinding
    private var recipeId: Long = 0
    private var recipe: Recipe? = null
    private var loadJob:Job?=null
    private var loadedContext=""
    private var loadedAccount=""
    private var recordsJob:Job?=null
    private var pendingAction:(()->Unit)?=null
    private var reactionBusy=false
    private fun Int.dp()=(this*resources.displayMetrics.density).toInt()
    private val contextKey get()="${com.familyrecipes.android.data.local.LocalStore.account}:${ApiClient.contentLanguage()}"
    private val editLauncher=registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) {
        if(it.resultCode==RESULT_OK)loadRecipeDetail()
    }

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
            Ui.toast(this,e)
            finish()
        }
    }

    override fun onResume(){
        super.onResume()
        if(::binding.isInitialized && ((recipe!=null && loadedContext!=contextKey) || (pendingAction!=null && PreferenceManager.isLoggedIn()))) {
            if(loadedAccount!=com.familyrecipes.android.data.local.LocalStore.account){recipe=null;binding.contentLayout.visibility=View.GONE}
            loadRecipeDetail()
        }
    }
    private fun authenticated(action:()->Unit) {
        if(com.familyrecipes.android.util.AuthUtil.requireLogin(this))action() else pendingAction=action
    }
    private fun returnUpdatedRecipe() {
        recipe?.let{setResult(RESULT_OK,android.content.Intent().putExtra("updated_recipe",com.google.gson.Gson().toJson(it)))}
    }

    private fun showMoreActions(recipe:Recipe) {
        val ui=com.familyrecipes.android.ui.design.Ui
        val sheet=com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val content=ui.box(this)
        content.addView(ui.text(this,getString(R.string.recipe_more_actions),22f,true))
        fun action(label:String,icon:Int,run:()->Unit) {
            content.addView(ui.entry(this,label,"",icon) {
                sheet.dismiss()
                authenticated(run)
            })
        }
        action(getString(R.string.ui_ad1a148b7b),R.drawable.ic_recipe_simple) {
            startActivity(android.content.Intent(this,EditRecipeActivity::class.java).putExtra("adapt_source_id",recipeId))
        }
        val known=recipe.cooks.orEmpty().any { it.id==PreferenceManager.userId }
        action(getString(if(known) R.string.ui_c07569f86e else R.string.ui_0191cf83a3),R.drawable.ic_book_outline) {
            lifecycleScope.launch {
                try {
                    ui.ok(ApiClient.getService().setKnown(recipeId,mapOf("known" to !known)))
                    loadRecipeDetail()
                } catch(e:kotlinx.coroutines.CancellationException) { throw e }
                  catch(e:Exception) { ui.toast(this@RecipeDetailActivity,e) }
            }
        }
        listOf(Triple("order",R.string.ui_b26b310008,R.drawable.ic_ui_order),
            Triple("prep",R.string.ui_7a73e91a1d,R.drawable.ic_ui_prep)).forEach { (kind,label,icon) ->
            action(getString(label),icon) {
                startActivity(android.content.Intent(this,com.familyrecipes.android.ui.meal.MealEditorActivity::class.java)
                    .putExtra("kind",kind).putExtra("recipe_id",recipe.id).putExtra("recipe_name",recipe.name))
            }
        }
        action(getString(R.string.ui_00f9d79879),R.drawable.ic_clock_filled) {
            editLauncher.launch(android.content.Intent(this,CookingRecordActivity::class.java).putExtra("recipe_id",recipe.id))
        }
        sheet.setContentView(ui.scroll(this,content))
        sheet.show()
    }

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

        loadJob?.cancel()
        binding.progressBar.visibility = if(recipe==null) View.VISIBLE else View.GONE
        binding.detailError.visibility=View.GONE
        binding.btnRetry.setOnClickListener{loadRecipeDetail()}
        val requestContext=contextKey
        val account=com.familyrecipes.android.data.local.LocalStore.account
        val cacheKey="$recipeId:${ApiClient.contentLanguage()}"

        loadJob=lifecycleScope.launch {
            try {
                if(recipe==null) {
                    val cached=com.familyrecipes.android.data.local.LocalStore.instance.records().get(account,"recipe_detail",cacheKey)
                    runCatching{com.google.gson.Gson().fromJson(cached,Recipe::class.java)}.getOrNull()?.let {
                        recipe=it;loadedAccount=account;displayRecipe(it)
                    }
                }
                val response = ApiClient.getService().getRecipeDetail(recipeId)
                if(requestContext!=contextKey)return@launch
                if (response.isSuccessful && response.body()?.code == 200) {
                    recipe = response.body()?.data
                    recipe?.let { item ->
                        loadedContext=requestContext;loadedAccount=account;displayRecipe(item)
                        if(PreferenceManager.isLoggedIn()){val action=pendingAction;pendingAction=null;action?.invoke()}
                        try {
                            com.familyrecipes.android.data.local.RecipeHistory.record(account,item)
                            com.familyrecipes.android.data.local.LocalStore.instance.records().put(com.familyrecipes.android.data.local.LocalRecord(account,"recipe_detail",cacheKey,com.google.gson.Gson().toJson(item)))
                        } catch(e:CancellationException){throw e} catch(_:Exception){ /* Reading remains available if history cannot be saved. */ }
                    }
                } else {
                    if(response.code() in listOf(401,403,404)) {
                        recipe=null;binding.contentLayout.visibility=View.GONE
                        com.familyrecipes.android.data.local.LocalStore.instance.records().delete(account,"recipe_detail",cacheKey)
                    }
                    throw retrofit2.HttpException(response)
                }
            } catch(e:CancellationException){throw e
            } catch (e: Exception) {
                binding.detailError.visibility=View.VISIBLE
                binding.tvDetailError.text=Ui.errorMessage(this@RecipeDetailActivity,e)
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
        
        binding.btnEdit.visibility=if(currentUserId>0 && recipe.creatorId==currentUserId) View.VISIBLE else View.GONE
        binding.btnEdit.setOnClickListener{editLauncher.launch(android.content.Intent(this,EditRecipeActivity::class.java).putExtra("recipe_id",recipe.id))}
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
        recipe.tags?.filter { it.tagValue.trim().removePrefix("#") !in listOf("", "待确认") }?.distinctBy { (it.displayValue ?: it.tagValue).trim().removePrefix("#").trim().lowercase(java.util.Locale.ROOT) }?.forEach { tag ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                // 如果tagValue已经包含#，就不再添加；否则添加#
                val displayText = "#${UiStrings.domain((tag.displayValue ?: tag.tagValue).trim().removePrefix("#").trim())}"
                text = displayText
                isClickable = true
                isCheckable = false
                chipBackgroundColor=android.content.res.ColorStateList.valueOf(Ui.pale)
                setTextColor(Ui.ink)
                textSize=14f
                chipStrokeWidth=0f
                chipMinHeight=32.dp().toFloat()
                setEnsureMinTouchTargetSize(true)
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
            binding.tvDescription.text = com.familyrecipes.android.util.RecipeDocument.render(com.familyrecipes.android.util.RecipeText.clean(recipe.description.orEmpty()))
            binding.tvDescription.visibility = View.VISIBLE
            binding.descriptionHeading.visibility=View.VISIBLE
            val collapsed=!recipe.steps.isNullOrEmpty()
            binding.tvDescription.maxLines=if(collapsed)4 else Int.MAX_VALUE
            binding.tvDescription.ellipsize=if(collapsed)android.text.TextUtils.TruncateAt.END else null
            binding.btnExpandDescription.visibility=View.GONE
            binding.tvDescription.doOnLayout {
                val layout=binding.tvDescription.layout
                binding.btnExpandDescription.visibility=if(collapsed && layout!=null && (0 until layout.lineCount).any{layout.getEllipsisCount(it)>0})View.VISIBLE else View.GONE
            }
            binding.btnExpandDescription.setOnClickListener {binding.tvDescription.maxLines=Int.MAX_VALUE;binding.tvDescription.ellipsize=null;binding.btnExpandDescription.visibility=View.GONE}
        } else {
            binding.tvDescription.visibility = View.GONE
            binding.descriptionHeading.visibility=View.GONE
            binding.btnExpandDescription.visibility=View.GONE
        }
        
        binding.gallery.removeAllViews()
        val ui=com.familyrecipes.android.ui.design.Ui
        recipe.sourceRecipeName?.let{binding.gallery.addView(ui.text(this,UiStrings.f(com.familyrecipes.android.R.string.adapted_value, it),15f))}
        if(!recipe.ingredients.isNullOrEmpty()) binding.gallery.addView(ui.text(this,getString(R.string.review_recipe_ingredients),21f,true))
        recipe.ingredients.orEmpty().forEach{
            val row=ui.row(this).apply{setPadding(0,2.dp(),0,2.dp())}
            row.addView(ui.text(this,it.ingredient?.name.orEmpty(),17f),android.widget.LinearLayout.LayoutParams(0,-2,1f))
            row.addView(ui.text(this,it.amount.orEmpty(),16f).apply{setTextColor(Ui.muted);maxWidth=(resources.displayMetrics.widthPixels*0.35f).toInt();setPadding(12.dp(),0,0,0)})
            binding.gallery.addView(row)
        }
        if(!recipe.steps.isNullOrEmpty()) binding.gallery.addView(ui.text(this,getString(R.string.review_recipe_steps),21f,true))
        recipe.steps.orEmpty().forEach{step->
            binding.gallery.addView(ui.text(this,getString(R.string.review_recipe_step,step.stepNumber),15f,true).apply{setTextColor(Ui.blue);setPadding(0,16.dp(),0,6.dp())})
            binding.gallery.addView(ui.text(this,com.familyrecipes.android.util.RecipeText.clean(step.description),18f).apply{setLineSpacing(4.dp().toFloat(),1.08f)})
            step.image?.takeIf{it!=recipe.coverImage}?.let{addPhoto(it,null)}
        }
        val galleryUrls=com.familyrecipes.android.util.RecipeEditorRules.galleryUrls(recipe.coverImage,recipe.steps.orEmpty().map{it.image},recipe.images.orEmpty().map{it.url})
        if(galleryUrls.isNotEmpty())binding.gallery.addView(ui.text(this,getString(R.string.review_recipe_photos),21f,true))
        galleryUrls.forEach{url->addPhoto(url,recipe.images.orEmpty().firstOrNull{it.url==url}?.caption)}
        if(recipe.description.isNullOrBlank() && recipe.steps.isNullOrEmpty() && recipe.ingredients.isNullOrEmpty())binding.gallery.addView(ui.text(this,getString(R.string.review_recipe_empty),16f))
        binding.btnMore.setOnClickListener { showMoreActions(recipe) }
        val history=android.widget.LinearLayout(this).apply{orientation=android.widget.LinearLayout.VERTICAL};binding.gallery.addView(history)
        recordsJob?.cancel()
        recordsJob=lifecycleScope.launch{try{val records=ui.data(ApiClient.getService().cookingRecords(recipeId));if(records.isNotEmpty())history.addView(ui.text(this@RecipeDetailActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_a40e588383),21f,true));records.forEach{r->history.addView(ui.text(this@RecipeDetailActivity,"${r.cookedAt.take(10)}\n${r.notes}"));com.google.gson.Gson().fromJson(r.images,Array<String>::class.java).orEmpty().forEach{url->val image=android.widget.ImageView(this@RecipeDetailActivity).apply{adjustViewBounds=true};history.addView(image);Glide.with(this@RecipeDetailActivity).load(com.familyrecipes.android.util.MediaImages.model(url)).into(image)}}}catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){history.addView(ui.text(this@RecipeDetailActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_f24e502e92)))}}
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
        returnUpdatedRecipe()
    }

    private fun addPhoto(url:String,caption:String?) {
        val image=android.widget.ImageView(this).apply{adjustViewBounds=true;maxHeight=(resources.displayMetrics.heightPixels*0.55f).toInt();scaleType=android.widget.ImageView.ScaleType.FIT_CENTER;contentDescription=caption ?: getString(R.string.review_image_description)}
        binding.gallery.addView(image,android.widget.LinearLayout.LayoutParams(-1,-2).apply{topMargin=8.dp();bottomMargin=8.dp()})
        Glide.with(this).load(com.familyrecipes.android.util.MediaImages.model(url)).signature(com.bumptech.glide.signature.ObjectKey(PreferenceManager.userId)).into(image)
        if(!caption.isNullOrBlank())binding.gallery.addView(Ui.text(this,caption,14f).apply{setTextColor(Ui.muted)})
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

    private fun toggleFavorite() = authenticated {
        if(!reactionBusy) {
            reactionBusy=true;binding.btnFavorite.isEnabled=false;binding.btnDislike.isEnabled=false
            lifecycleScope.launch {
                try {
                    val favorite=Ui.data(ApiClient.getService().toggleFavorite(recipeId))
                    recipe=recipe?.let{it.copy(isFavorite=favorite,favoriteCount=((it.favoriteCount?:0)+if(favorite)1 else -1).coerceAtLeast(0))}
                    updateFavoriteButton(favorite)
                    binding.tvFavoriteCount.text=getString(R.string.recipe_favorites_compact,recipe?.favoriteCount?:0)
                    returnUpdatedRecipe()
                } catch(e:CancellationException){throw e} catch(e:Exception){Ui.toast(this@RecipeDetailActivity,e)}
                finally{reactionBusy=false;binding.btnFavorite.isEnabled=true;binding.btnDislike.isEnabled=true}
            }
        }
    }

    private fun toggleDislike() = authenticated {
        if(!reactionBusy) {
            reactionBusy=true;binding.btnFavorite.isEnabled=false;binding.btnDislike.isEnabled=false
            lifecycleScope.launch {
                try {
                    val disliked=Ui.data(ApiClient.getService().toggleDislike(recipeId))
                    recipe=recipe?.let{it.copy(isDisliked=disliked,dislikeCount=((it.dislikeCount?:0)+if(disliked)1 else -1).coerceAtLeast(0))}
                    updateDislikeButton(disliked);returnUpdatedRecipe()
                } catch(e:CancellationException){throw e} catch(e:Exception){Ui.toast(this@RecipeDetailActivity,e)}
                finally{reactionBusy=false;binding.btnFavorite.isEnabled=true;binding.btnDislike.isEnabled=true}
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
        
        val platform=com.familyrecipes.android.util.LinkPlatform.fromUrl(link.url)
        holder.tvTitle.text = link.title.ifBlank { link.url }
        holder.ivThumbnail.setImageDrawable(com.familyrecipes.android.ui.design.PlatformBadge(platform))
        holder.ivThumbnail.contentDescription=platform.label
        holder.tvNotes.text=link.notes.orEmpty()
        holder.tvNotes.visibility=if(link.notes.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
        
        holder.itemView.setOnClickListener { onOpenClick(link.url) }
        holder.tvTitle.setOnClickListener { onOpenClick(link.url) }

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

