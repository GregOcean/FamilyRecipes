package com.familyrecipes.android.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.familyrecipes.android.R
import com.familyrecipes.android.data.model.Recipe
import com.familyrecipes.android.databinding.ItemRecipeBinding

/**
 * 菜谱列表适配器
 */
class RecipeAdapter(
    private val recipes: List<Recipe>,
    private val onItemClick: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemRecipeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(recipe: Recipe) {
            binding.tvName.text = recipe.name
            
            // 显示标签
            val tags = recipe.tags?.joinToString(" · ") { it.tagValue } ?: ""
            binding.tvTags.text = tags
            
            // 加载封面图片
            if (!recipe.coverImage.isNullOrEmpty()) {
                Glide.with(binding.ivCover.context)
                    .load(recipe.coverImage)
                    .placeholder(R.drawable.placeholder_recipe)
                    .into(binding.ivCover)
            }
            
            // 显示统计信息
            binding.tvStats.text = "❤ ${recipe.favoriteCount ?: 0}  👎 ${recipe.dislikeCount ?: 0}  👁 ${recipe.viewCount ?: 0}"
            
            binding.root.setOnClickListener {
                onItemClick(recipe)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecipeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(recipes[position])
    }

    override fun getItemCount() = recipes.size
}

