package com.familyrecipes.android.data.local

import android.content.Context
import android.util.Log
import com.familyrecipes.android.data.model.*
import com.familyrecipes.android.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 配置管理器
 * 负责从服务器获取配置并缓存到本地
 */
object ConfigManager {
    
    private const val TAG = "ConfigManager"
    private const val PREFS_NAME = "app_config"
    private const val KEY_LAST_UPDATE = "last_update_time"
    private const val UPDATE_INTERVAL = 24 * 60 * 60 * 1000L // 24小时更新一次
    
    // 内存缓存
    private var appConfig: AppConfig? = null
    private var ingredientsList: List<String> = emptyList()
    private var storageLocationsList: List<String> = emptyList()
    private var textsMap: Map<String, String> = emptyMap()
    
    /**
     * 初始化配置（应用启动时调用）
     */
    suspend fun initialize(context: Context, forceRefresh: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0)
            val now = System.currentTimeMillis()
            
            // 判断是否需要更新
            if (forceRefresh || appConfig == null || (now - lastUpdate) > UPDATE_INTERVAL) {
                Log.d(TAG, "Loading config from server...")
                loadConfigFromServer()
                
                // 更新最后更新时间
                prefs.edit().putLong(KEY_LAST_UPDATE, now).apply()
                Log.d(TAG, "Config loaded successfully")
            } else {
                Log.d(TAG, "Using cached config")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize config", e)
            // 使用默认配置
            useDefaultConfig()
        }
    }
    
    /**
     * 从服务器加载配置
     */
    private suspend fun loadConfigFromServer() {
        val response = ApiClient.getService().getAllConfigs()
        if (response.isSuccessful && response.body()?.code == 200) {
            val serverConfig = response.body()?.data
            if (serverConfig != null) {
                // texts已经是Map<String, String>了，直接使用
                val textsMap = serverConfig.texts
                
                // 找到默认存储位置
                val defaultLocation = serverConfig.storageLocations
                    .firstOrNull { it.isDefault }
                    ?: serverConfig.defaultStorageLocation
                
                // 创建AppConfig对象
                appConfig = AppConfig(
                    ingredients = serverConfig.ingredients.filter { it.isEnabled },
                    storageLocations = serverConfig.storageLocations.filter { it.isEnabled },
                    categories = serverConfig.categories.filter { it.isEnabled },
                    texts = textsMap,
                    defaultStorageLocation = defaultLocation
                )
                
                // 提取为简单列表和Map
                ingredientsList = appConfig!!.ingredients.map { it.name }
                storageLocationsList = appConfig!!.storageLocations.map { it.name }
                this.textsMap = textsMap
                
                Log.d(TAG, "Loaded ${ingredientsList.size} ingredients, " +
                          "${storageLocationsList.size} storage locations, " +
                          "${textsMap.size} text configs")
            }
        } else {
            throw Exception("Failed to load config from server: ${response.code()}")
        }
    }
    
    /**
     * 使用默认配置（当服务器不可用时）
     */
    private fun useDefaultConfig() {
        Log.w(TAG, "Using default config as fallback")
        ingredientsList = getDefaultIngredients()
        storageLocationsList = getDefaultStorageLocations()
        textsMap = getDefaultTexts()
    }
    
    /**
     * 获取常见食材列表
     */
    fun getIngredients(): List<String> {
        return ingredientsList.ifEmpty { getDefaultIngredients() }
    }
    
    /**
     * 获取存储位置列表
     */
    fun getStorageLocations(): List<String> {
        return storageLocationsList.ifEmpty { getDefaultStorageLocations() }
    }
    
    /**
     * 获取默认存储位置
     */
    fun getDefaultStorageLocation(): String {
        return appConfig?.defaultStorageLocation?.name ?: "冰箱冷藏"
    }
    
    /**
     * 获取文本配置
     */
    fun getText(key: String, defaultValue: String = ""): String {
        return textsMap[key] ?: defaultValue
    }
    
    /**
     * 获取完整配置对象
     */
    fun getAppConfig(): AppConfig? {
        return appConfig
    }
    
    /**
     * 清除缓存（调试用）
     */
    fun clearCache(context: Context) {
        appConfig = null
        ingredientsList = emptyList()
        storageLocationsList = emptyList()
        textsMap = emptyMap()
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
    
    // ========== 默认配置（作为fallback） ==========
    
    private fun getDefaultIngredients(): List<String> {
        return listOf(
            // 肉类
            "猪肉", "猪里脊", "猪五花", "猪蹄", "猪尾巴",
            "牛肉", "牛腱子", "牛里脊", "牛排", "牛腩",
            "鸡肉", "鸡腿", "鸡翅", "鸡胸肉", "鸡爪",
            "羊肉", "羊排", "羊腿",
            "鸭肉", "鸭腿", "鸭翅",
            
            // 海鲜
            "鱼", "草鱼", "鲈鱼", "带鱼", "三文鱼",
            "虾", "大虾", "基围虾", "虾仁",
            "螃蟹", "大闸蟹", "梭子蟹",
            
            // 蔬菜
            "白菜", "大白菜", "小白菜", "菠菜", "生菜",
            "番茄", "西红柿", "土豆", "红薯", "山药",
            "黄瓜", "冬瓜", "南瓜", "茄子", "青椒",
            "豆角", "胡萝卜", "白萝卜", "玉米",
            
            // 水果
            "苹果", "香蕉", "橙子", "橘子", "梨",
            "葡萄", "西瓜", "草莓", "芒果",
            
            // 蛋奶
            "鸡蛋", "鸭蛋", "牛奶", "酸奶", "奶酪",
            
            // 豆制品
            "豆腐", "豆腐干",
            
            // 主食
            "米", "大米", "面粉", "面条", "馒头",
            
            // 调味品
            "盐", "糖", "酱油", "醋", "料酒", "食用油", "葱", "姜", "蒜"
        )
    }
    
    private fun getDefaultStorageLocations(): List<String> {
        return listOf(
            "冰箱冷藏",
            "冰箱冷冻",
            "橱柜",
            "常温",
            "阴凉处",
            "冷藏室",
            "冷冻室",
            "保鲜层",
            "蔬菜室",
            "冰柜"
        )
    }
    
    private fun getDefaultTexts(): Map<String, String> {
        return mapOf(
            "status.normal" to "正常",
            "status.expiring" to "即将过期",
            "status.expired" to "已过期",
            "status.consumed" to "已消耗",
            "button.consume" to "已消耗",
            "button.delete" to "删除",
            "button.save" to "保存",
            "button.cancel" to "取消",
            "button.back_to_edit" to "返回修改",
            "button.confirm_save" to "确认保存"
        )
    }
}

