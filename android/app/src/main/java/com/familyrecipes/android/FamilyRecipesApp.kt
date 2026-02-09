package com.familyrecipes.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.*
import com.familyrecipes.android.data.local.PreferenceManager
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.worker.ExpiryReminderWorker
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

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 初始化SharedPreferences
        PreferenceManager.init(this)
        
        // 初始化API客户端
        ApiClient.init(this)
        
        // 创建通知渠道
        createNotificationChannel()
        
        // 启动定时检查任务
        scheduleExpiryCheck()
    }

    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "用于提醒冰箱中即将过期的食材"
                enableVibration(true)
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
            1, TimeUnit.DAYS  // 每天检查一次
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.HOURS)  // 首次延迟1小时
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "ExpiryReminderWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
    }
}

