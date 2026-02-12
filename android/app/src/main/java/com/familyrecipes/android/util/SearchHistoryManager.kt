package com.familyrecipes.android.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 搜索历史管理器
 */
class SearchHistoryManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val PREFS_NAME = "search_history"
        private const val KEY_HISTORY = "history_list"
        private const val MAX_HISTORY_SIZE = 10
    }
    
    /**
     * 添加搜索历史
     */
    fun addHistory(keyword: String) {
        if (keyword.isBlank()) return
        
        val history = getHistory().toMutableList()
        
        // 如果已存在，先移除
        history.remove(keyword)
        
        // 添加到最前面
        history.add(0, keyword)
        
        // 保持最多10条
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(history.size - 1)
        }
        
        // 保存
        saveHistory(history)
    }
    
    /**
     * 获取搜索历史
     */
    fun getHistory(): List<String> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 清除搜索历史
     */
    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }
    
    /**
     * 删除单条历史记录
     */
    fun removeHistory(keyword: String) {
        val history = getHistory().toMutableList()
        history.remove(keyword)
        saveHistory(history)
    }
    
    /**
     * 保存历史记录
     */
    private fun saveHistory(history: List<String>) {
        val json = gson.toJson(history)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }
}

