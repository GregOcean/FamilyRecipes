package com.familyrecipes.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.work.*
import com.familyrecipes.android.data.local.ConfigManager
import com.familyrecipes.android.data.local.PreferenceManager
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.worker.ExpiryReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * 应用程序Application类
 */
class FamilyRecipesApp : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "fridge_expiry_reminder"
        const val NOTIFICATION_CHANNEL_NAME = "食材过期提醒"
        
        lateinit var instance: FamilyRecipesApp
            private set
    }
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        // Restore the user preference before creating activities.
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            getSharedPreferences("appearance", MODE_PRIVATE).getInt("mode",
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM))
        instance = this
        
        // 初始化SharedPreferences
        PreferenceManager.init(this)
        
        // 初始化API客户端
        ApiClient.init(this)
        
        // 初始化配置管理器（异步加载）
        initializeConfig()
        
        // 创建通知渠道
        createNotificationChannel()
        
        // 启动定时检查任务
        scheduleExpiryCheck()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("ChatSync",ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<com.familyrecipes.android.worker.ChatSyncWorker>(15,TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
        if(Build.VERSION.SDK_INT>=26) getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("family_messages","家庭消息",NotificationManager.IMPORTANCE_DEFAULT))
    }
    
    /**
     * 初始化配置管理器
     */
    private fun initializeConfig() {
        applicationScope.launch {
            try {
                Log.d("FamilyRecipesApp", "Initializing config...")
                ConfigManager.initialize(this@FamilyRecipesApp)
                Log.d("FamilyRecipesApp", "Config initialized successfully")
            } catch (e: Exception) {
                Log.e("FamilyRecipesApp", "Failed to initialize config", e)
            }
        }
    }

    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "用于提醒冰箱中即将过期的食材"
                enableVibration(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 安排定期检查过期食材
     */
    private fun scheduleExpiryCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<ExpiryReminderWorker>(
            15, TimeUnit.MINUTES  // 到用户设定时间后，每天最多提醒一次
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "ExpiryReminderWork",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
    }
}

