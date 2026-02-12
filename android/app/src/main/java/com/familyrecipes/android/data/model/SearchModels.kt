package com.familyrecipes.android.data.model

import com.google.gson.annotations.SerializedName

/**
 * 全局搜索结果
 */
data class GlobalSearchResult(
    val items: List<SearchItem>,
    val total: Int,
    @SerializedName("pageNum") val pageNum: Int,
    @SerializedName("pageSize") val pageSize: Int,
    @SerializedName("ingredientCount") val ingredientCount: Int,
    @SerializedName("recipeCount") val recipeCount: Int
)

/**
 * 搜索结果项（可以是食材或菜谱）
 */
data class SearchItem(
    val type: String,  // "ingredient" 或 "recipe"
    val id: Long,
    val name: String,
    val category: String? = null,  // 食材的分类
    val description: String? = null,  // 菜谱的描述
    @SerializedName("coverImageUrl") val coverImageUrl: String? = null,  // 菜谱的封面
    val difficulty: String? = null,  // 菜谱的难度
    @SerializedName("cookTime") val cookTime: Int? = null  // 菜谱的烹饪时间
)

