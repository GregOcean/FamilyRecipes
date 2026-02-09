package com.familyrecipes.android.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences管理类
 */
object PreferenceManager {
    private const val PREF_NAME = "family_recipes_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_BASE_URL = "base_url"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Token相关
    var authToken: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    // 用户信息
    var userId: Long
        get() = prefs.getLong(KEY_USER_ID, 0)
        set(value) = prefs.edit().putLong(KEY_USER_ID, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    // 服务器地址
    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, "http://10.0.2.2:8080") ?: "http://10.0.2.2:8080"
        set(value) = prefs.edit().putString(KEY_BASE_URL, value).apply()

    // 是否已登录
    fun isLoggedIn(): Boolean {
        return !authToken.isNullOrEmpty()
    }

    // 清除登录信息
    fun clearUserData() {
        prefs.edit().apply {
            remove(KEY_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_NAME)
            apply()
        }
    }

    // 保存登录信息
    fun saveLoginInfo(token: String, userId: Long, email: String, userName: String) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putLong(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, userName)
            apply()
        }
    }
}

