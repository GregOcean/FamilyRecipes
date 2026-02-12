package com.familyrecipes.android.ui.recipe

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.familyrecipes.android.R
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
                text = tag.tagValue
                isClickable = false
                setChipBackgroundColorResource(R.color.light_gray)
                setTextColor(getColor(R.color.text_primary))
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
}

