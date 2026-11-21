package com.familyrecipes.android.data.local

import com.familyrecipes.android.data.model.FavoriteNotificationPage
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.design.Ui
import com.google.gson.Gson

import kotlinx.coroutines.sync.withLock

object FavoriteNotifications {
    private val mutex=kotlinx.coroutines.sync.Mutex()
    suspend fun cached(account:String=LocalStore.account):FavoriteNotificationPage =
        LocalStore.instance.records().get(account,"favorite-notifications","latest")?.let {
            runCatching { Gson().fromJson(it,FavoriteNotificationPage::class.java) }.getOrNull()
        } ?: FavoriteNotificationPage()
    suspend fun sync() = mutex.withLock {
        if(!PreferenceManager.isLoggedIn()) return@withLock
        val account=LocalStore.account
        val page=Ui.data(ApiClient.getAccountService(account).favoriteNotifications())
        if(account!=LocalStore.account)return@withLock
        LocalStore.instance.records().put(LocalRecord(account,"favorite-notifications","latest",Gson().toJson(page)))
        val context=com.familyrecipes.android.FamilyRecipesApp.instance
        val prefs=context.getSharedPreferences("favorite-alerts",0)
        val seen=prefs.getLong(account,0)
        val latest=page.items.maxOfOrNull { it.id } ?: 0
        val manager=androidx.core.app.NotificationManagerCompat.from(context)
        val tag="favorite:$account"
        if(page.unread==0)manager.cancel(tag,2)
        if(page.items.any { !it.isRead && it.id>seen } && manager.areNotificationsEnabled()) {
            val intent=android.content.Intent(context,com.familyrecipes.android.ui.message.FavoriteNotificationsActivity::class.java).putExtra("notification_account",account)
            val pending=android.app.PendingIntent.getActivity(context,account.hashCode(),intent,android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
            try {
                if(!ChatNotifications.publish(account,tag,2,androidx.core.app.NotificationCompat.Builder(context,"family_messages")
                    .setSmallIcon(com.familyrecipes.android.R.drawable.ic_notification)
                    .setContentTitle(context.getString(com.familyrecipes.android.R.string.favorite_alerts))
                    .setContentText(context.getString(com.familyrecipes.android.R.string.favorite_alert_count,page.unread))
                    .setContentIntent(pending).setAutoCancel(true).build()))return@withLock
            }catch(_:SecurityException){return@withLock}
        }
        if(manager.areNotificationsEnabled())prefs.edit().putLong(account,maxOf(seen,latest)).apply()
    }
}
