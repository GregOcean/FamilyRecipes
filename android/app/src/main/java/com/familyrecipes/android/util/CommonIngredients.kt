package com.familyrecipes.android.util

import com.familyrecipes.android.data.local.ConfigManager

/**
 * 常见食材数据库（已迁移到ConfigManager，保留作为兼容层）
 * @deprecated 使用 ConfigManager 代替
 */
object CommonIngredients {
    
    /**
     * 常见食材分类列表
     * 从ConfigManager获取，如果未初始化则使用默认值
     */
    val INGREDIENTS: List<String>
        get() = ConfigManager.getIngredients()
    
    /**
     * 常见存储位置列表
     * 从ConfigManager获取，如果未初始化则使用默认值
     */
    val STORAGE_LOCATIONS: List<String>
        get() = ConfigManager.getStorageLocations()
    
    /**
     * 根据输入过滤食材
     */
    fun filter(query: String): List<String> {
        if (query.isEmpty()) {
            return emptyList()
        }
        
        return INGREDIENTS.filter { 
            it.contains(query, ignoreCase = true) || 
            it.startsWith(query, ignoreCase = true)
        }.take(10)  // 最多显示10个建议
    }
}

