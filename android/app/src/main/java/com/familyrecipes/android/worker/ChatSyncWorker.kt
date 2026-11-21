package com.familyrecipes.android.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.familyrecipes.android.data.local.*
import kotlinx.coroutines.CancellationException

/** Network-constrained fallback. Foreground uses the same coordinator; this is not device push. */
class ChatSyncWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params){
    override suspend fun doWork():Result {
        if(!PreferenceManager.isLoggedIn())return Result.success()
        val account=LocalStore.account
        return try{
            var notificationFailed=false
            try{FavoriteNotifications.sync()}catch(e:CancellationException){throw e}catch(_:Exception){notificationFailed=true}
            try{FriendInvitations.sync()}catch(e:CancellationException){throw e}catch(_:Exception){notificationFailed=true}
            ConversationSync.refresh(account)
            if(account!=LocalStore.account)return Result.success()
            val retryable=LocalStore.instance.records().list(account,"outbox").any{
                val item=com.google.gson.Gson().fromJson(it.json,PendingMessage::class.java)
                item.state !in setOf("failed","auth","unavailable")
            }
            if(retryable || notificationFailed)Result.retry() else Result.success()
        }catch(e:CancellationException){throw e}catch(e:ChatSyncException){if(e.status==401)Result.success() else Result.retry()}
        catch(_:Exception){Result.retry()}
    }
}
