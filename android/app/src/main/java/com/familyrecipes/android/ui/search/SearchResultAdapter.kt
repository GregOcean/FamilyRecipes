package com.familyrecipes.android.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.familyrecipes.android.R
import com.familyrecipes.android.data.model.SearchItem
import com.familyrecipes.android.databinding.ItemSearchRecipeBinding
import com.familyrecipes.android.databinding.ItemSearchIngredientBinding

/**
 * 搜索结果适配器 - 支持混合显示菜谱和食材
 */
class SearchResultAdapter(
    private val items: List<SearchItem>,
    private val onRecipeClick: (SearchItem) -> Unit,
    private val onIngredientClick: (SearchItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_RECIPE = 0
        private const val TYPE_INGREDIENT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].type == "recipe") TYPE_RECIPE else TYPE_INGREDIENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_RECIPE -> {
                val binding = ItemSearchRecipeBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                RecipeViewHolder(binding)
            }
            else -> {
                val binding = ItemSearchIngredientBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                IngredientViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is RecipeViewHolder -> holder.bind(item, onRecipeClick)
            is IngredientViewHolder -> holder.bind(item, onIngredientClick)
        }
    }

    override fun getItemCount() = items.size

    class RecipeViewHolder(private val binding: ItemSearchRecipeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SearchItem, onClick: (SearchItem) -> Unit) {
            binding.tvRecipeName.text = item.name
            binding.tvRecipeDescription.text = item.description ?: "暂无描述"

            // 加载封面图片
            if (!item.coverImageUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(item.coverImageUrl)
                    .placeholder(R.drawable.ic_recipe)
                    .error(R.drawable.ic_recipe)
                    .centerCrop()
                    .into(binding.ivRecipeCover)
            } else {
                binding.ivRecipeCover.setImageResource(R.drawable.ic_recipe)
            }

            // 显示难度和时间
            val difficultyText = when (item.difficulty) {
                "easy" -> "简单"
                "medium" -> "中等"
                "hard" -> "困难"
                else -> "未知"
            }
            binding.tvDifficulty.text = difficultyText
            binding.tvCookTime.text = item.cookTime?.let { "${it}分钟" } ?: "未知"

            binding.root.setOnClickListener { onClick(item) }
        }
    }

    class IngredientViewHolder(private val binding: ItemSearchIngredientBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SearchItem, onClick: (SearchItem) -> Unit) {
            binding.tvIngredientName.text = item.name
            binding.tvIngredientCategory.text = item.category ?: "其他"

            // 根据分类显示不同的图标
            val iconRes = when (item.category) {
                "肉类" -> R.drawable.ic_food
                "蔬菜" -> R.drawable.ic_food
                "水果" -> R.drawable.ic_food
                "海鲜水产" -> R.drawable.ic_food
                else -> R.drawable.ic_food
            }
            binding.ivIngredientIcon.setImageResource(iconRes)

            binding.root.setOnClickListener { onClick(item) }
        }
    }
}

