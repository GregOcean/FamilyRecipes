package com.familyrecipes.android.data.model

import com.google.gson.annotations.SerializedName

/**
 * 配置相关数据模型
 */

/**
 * 常见食材配置
 */
data class ConfigIngredient(
    val id: Int,
    val name: String,
    val category: String?,
    @SerializedName("sort_order") val sortOrder: Int,
    @SerializedName("is_enabled") val isEnabled: Boolean
)

/**
 * 存储位置配置
 */
data class ConfigStorageLocation(
    val id: Int,
    val name: String,
    val icon: String?,
    @SerializedName("sort_order") val sortOrder: Int,
    @SerializedName("is_default") val isDefault: Boolean,
    @SerializedName("is_enabled") val isEnabled: Boolean
)

/**
 * 食材分类配置
 */
data class ConfigCategory(
    val id: Int,
    val name: String,
    @SerializedName("sort_order") val sortOrder: Int,
    @SerializedName("is_enabled") val isEnabled: Boolean
)

/**
 * 系统文本配置
 */
data class ConfigText(
    val id: Int,
    @SerializedName("config_key") val configKey: String,
    @SerializedName("config_value") val configValue: String,
    val description: String?,
    val category: String?,
    @SerializedName("is_enabled") val isEnabled: Boolean
)

/**
 * 完整配置数据
 */
data class AppConfig(
    val ingredients: List<ConfigIngredient>,
    @SerializedName("storage_locations") val storageLocations: List<ConfigStorageLocation>,
    val categories: List<ConfigCategory>,
    val texts: Map<String, String>,
    @SerializedName("default_storage_location") val defaultStorageLocation: ConfigStorageLocation?
)

