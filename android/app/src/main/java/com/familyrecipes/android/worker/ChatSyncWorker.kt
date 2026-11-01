package com.familyrecipes.android.worker
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.familyrecipes.android.data.local.*
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.social.GroupChatActivity
import kotlinx.coroutines.CancellationException
class ChatSyncWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result {
        if(!PreferenceManager.isLoggedIn())return Result.success()
        val account=LocalStore.account
        val userId=PreferenceManager.userId
        try {
            val response=ApiClient.getService().getGroupsList()
            if(response.code()==401)return Result.success()
            if(!response.isSuccessful || response.body()?.code!=200)return Result.retry()
            for(group in response.body()?.data.orEmpty()) {
                if(account!=LocalStore.account)return Result.success()
                val id=group.id ?: continue
                val initial=LocalStore.instance.records().get(account,"cursor",id.toString())==null
                val messages=ChatRepository.sync(account,id).filter { it.senderId!=userId && it.messageType!="system" }
                if(initial || messages.isEmpty() || GroupChatActivity.visibleGroup==id)continue
                val manager=NotificationManagerCompat.from(applicationContext)
                if(!manager.areNotificationsEnabled())continue
                val intent=Intent(applicationContext,GroupChatActivity::class.java).putExtra(GroupChatActivity.EXTRA_GROUP_ID,id)
                    .putExtra(GroupChatActivity.EXTRA_GROUP_NAME,group.name)
                val pending=PendingIntent.getActivity(applicationContext,id.toInt(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                manager.notify(id.toInt()+2000,NotificationCompat.Builder(applicationContext,"family_messages")
                    .setSmallIcon(com.familyrecipes.android.R.drawable.ic_notification).setContentTitle(group.name)
                    .setContentText("${messages.size} 条新消息").setAutoCancel(true).setContentIntent(pending).build())
            }
            return Result.success()
        } catch(e:CancellationException) { throw e }
          catch(_:Exception) { return Result.retry() }
    }
}
