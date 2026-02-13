package com.familyrecipes.android.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.familyrecipes.android.data.local.PreferenceManager
import com.familyrecipes.android.ui.MainActivity

/**
 * 登录状态检查工具类
 */
object AuthUtil {
    
    /**
     * 检查用户是否已登录
     */
    fun isLoggedIn(): Boolean {
        return PreferenceManager.isLoggedIn() && PreferenceManager.userId > 0
    }
    
    /**
     * 检查登录状态，如果未登录则提示并跳转到"我的"页面
     * @param context 上下文
     * @param action 操作名称（用于提示）
     * @return true 表示已登录，false 表示未登录
     */
    fun requireLogin(context: Context, action: String = "此操作"): Boolean {
        if (!isLoggedIn()) {
            Toast.makeText(context, "${action}需要登录，请先登录", Toast.LENGTH_LONG).show()
            navigateToProfile(context)
            return false
        }
        return true
    }
    
    /**
     * 跳转到"我的"页面（包含登录功能）
     */
    fun navigateToProfile(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra("navigate_to", "profile")
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        context.startActivity(intent)
    }
    
    /**
     * 获取当前用户ID，如果未登录返回 null
     */
    fun getCurrentUserId(): Long? {
        return if (isLoggedIn()) PreferenceManager.userId else null
    }
    
    /**
     * 获取当前用户ID，如果未登录抛出异常
     */
    fun requireCurrentUserId(): Long {
        if (!isLoggedIn()) {
            throw IllegalStateException("User not logged in")
        }
        return PreferenceManager.userId
    }
}

