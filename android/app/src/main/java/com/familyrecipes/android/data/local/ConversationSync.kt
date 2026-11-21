package com.familyrecipes.android.data.local

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.familyrecipes.android.FamilyRecipesApp
import com.familyrecipes.android.R
import com.familyrecipes.android.data.model.ChatContent
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.design.Ui
import com.familyrecipes.android.ui.social.GroupChatActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Foreground polling and WorkManager share one account-scoped receive/notification path. */
object ConversationSync {
    private val mutex=Mutex()
    suspend fun refresh(account:String=LocalStore.account,notify:Boolean=true) = mutex.withLock {
        if(!PreferenceManager.isLoggedIn() || account!=LocalStore.account)return@withLock
        val user=PreferenceManager.userId
        val groups=Ui.data(ApiClient.getAccountService(account).getGroupsList())
        if(account!=LocalStore.account)return@withLock
        GroupCache.save(account,groups)
        val dao=LocalStore.instance.records()
        val pending=dao.list(account,"outbox").map{com.google.gson.Gson().fromJson(it.json,PendingMessage::class.java)}
        val accessible=groups.mapNotNull{it.id}.toSet()
        pending.filter{it.state!="unavailable"}.map{it.groupId}.distinct().filter{it !in accessible}.forEach{id ->
            // The list snapshot can predate a newly created group; confirm access before blocking its outbox.
            try { ChatRepository.sync(account,id) }
            catch(e:ChatSyncException){
                if(e.status==403 || e.status==404)ChatRepository.unavailableGroup(account,id) else throw e
            }
        }
        for(group in groups){
            if(account!=LocalStore.account)return@withLock
            val id=group.id?:continue
            val latest=group.lastMessage?.id?:0
            val cursor=dao.get(account,"cursor",id.toString())?.toLongOrNull()
            if(latest>(cursor?:0) || cursor==null || pending.any{it.groupId==id}){
                try {
                    val received=ChatRepository.sync(account,id)
                    if(account!=LocalStore.account)return@withLock
                    val seen=dao.get(account,"chat-notified",id.toString())?.toLongOrNull()?:0
                    val unread=group.unreadCount?:0
                    val messages=received.filter{it.senderId!=user && (it.id?:0)>maxOf(seen,group.lastReadMessageId?:0)}
                    // A first sync is not automatically silent: server unread state is authoritative.
                    if(notify && messages.isNotEmpty() && GroupChatActivity.visibleGroup!=id){
                        val manager=NotificationManagerCompat.from(FamilyRecipesApp.instance)
                        if(manager.areNotificationsEnabled()){
                            val context=FamilyRecipesApp.instance
                            val intent=Intent(context,GroupChatActivity::class.java).putExtra("notification_account",account)
                                .putExtra(GroupChatActivity.EXTRA_GROUP_ID,id).putExtra(GroupChatActivity.EXTRA_GROUP_NAME,group.name)
                            val action=PendingIntent.getActivity(context,(account+":"+id).hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                            val text=if(messages.any{ChatContent.mentions(it.content,user)})context.getString(R.string.chat_mentioned)
                                else if(messages.all{it.messageType=="system"})com.familyrecipes.android.ui.social.ChatSystemText.display(context,ChatContent.plain(messages.last().content))
                                else context.getString(R.string.chat_notification,maxOf(unread,messages.size))
                            try {
                                val posted=ChatNotifications.publish(account,ChatNotifications.tag(account),id.toInt(),NotificationCompat.Builder(context,"family_messages")
                                    .setSmallIcon(R.drawable.ic_notification).setContentTitle(group.name).setContentText(text)
                                    .setAutoCancel(true).setContentIntent(action).build())
                                if(posted)dao.put(LocalRecord(account,"chat-notified",id.toString(),messages.maxOf{it.id?:0}.toString()))
                            }catch(_:SecurityException){}
                        }
                    }
                }catch(e:CancellationException){throw e}catch(e:ChatSyncException){if(e.status==401)throw e}
            }
        }
    }
}
object ChatNotifications {
    fun tag(account:String)="chat:$account"
    fun publish(account:String,tag:String,id:Int,notification:android.app.Notification):Boolean = synchronized(PreferenceManager) {
        if(account!=LocalStore.account || !PreferenceManager.isLoggedIn())return@synchronized false
        val manager=NotificationManagerCompat.from(FamilyRecipesApp.instance)
        if(!manager.areNotificationsEnabled())return@synchronized false
        manager.notify(tag,id,notification)
        true
    }
    fun clear(account:String){
        val context=FamilyRecipesApp.instance
        val system=context.getSystemService(android.app.NotificationManager::class.java)
        system.activeNotifications.filter{it.tag==tag(account) || it.tag=="favorite:$account" || it.tag=="friend-invitations:$account" || (it.tag==null && it.id>=2000)}
            .forEach{system.cancel(it.tag,it.id)}
    }
    fun clearGroup(account:String,group:Long){NotificationManagerCompat.from(FamilyRecipesApp.instance).cancel(tag(account),group.toInt())}
}
