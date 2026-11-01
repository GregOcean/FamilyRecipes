package com.familyrecipes.android.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.familyrecipes.android.FamilyRecipesApp
import com.familyrecipes.android.R
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.MainActivity

/**
 * 食材过期提醒Worker
 */
class ExpiryReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!com.familyrecipes.android.data.local.PreferenceManager.isLoggedIn()) return Result.success()
        val accountKey=com.familyrecipes.android.data.local.LocalStore.account
        val accountId = com.familyrecipes.android.data.local.PreferenceManager.userId
        return try {
            val setting = ApiClient.getService().getReminderSetting()
            if (setting.code() == 401) return Result.success()
            if (!setting.isSuccessful || setting.body()?.code != 200) return Result.retry()
            if (setting.body()?.data?.enabled != true) return Result.success()
            val now=java.util.Calendar.getInstance()
            val configured=setting.body()?.data?.reminderTime ?: "17:00:00"
            val day=java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.ROOT).format(now.time)
            val dao=com.familyrecipes.android.data.local.LocalStore.instance.records()
            if(!ReminderPolicy.due(true,configured,now.get(java.util.Calendar.HOUR_OF_DAY)*60+now.get(java.util.Calendar.MINUTE),
                day,dao.get(accountKey,"notification","expiry")))return Result.success()
            if(!androidx.core.app.NotificationManagerCompat.from(applicationContext).areNotificationsEnabled())return Result.success()
            // 获取即将过期的食材
            val response = ApiClient.getService().getExpiringItems()
            
            if (response.code() == 401) return Result.success()
            if (!response.isSuccessful || response.body()?.code != 200) return Result.retry()
            if (accountId != com.familyrecipes.android.data.local.PreferenceManager.userId) return Result.success()
            if (response.isSuccessful) {
                val items = response.body()?.data
                
                if (!items.isNullOrEmpty()) {
                    // 发送通知
                    sendNotification(items.size)
                    dao.put(com.familyrecipes.android.data.local.LocalRecord(accountKey,"notification","expiry",day))
                }
            }
            
            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun sendNotification(count: Int) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "fridge")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            FamilyRecipesApp.NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("食材即将过期")
            .setContentText("您有 $count 个食材即将过期，请及时处理")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        
        notificationManager.notify(1001, notification)
    }
}

