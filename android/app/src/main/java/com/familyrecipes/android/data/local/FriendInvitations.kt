package com.familyrecipes.android.data.local

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.familyrecipes.android.FamilyRecipesApp
import com.familyrecipes.android.R
import com.familyrecipes.android.data.model.FriendInvitation
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.design.Ui
import com.familyrecipes.android.ui.social.FriendsListActivity
import com.google.gson.Gson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Persisted per account; a pending request alerts once, never on every poll. */
object FriendInvitations {
    private val mutex = Mutex()
    private val gson = Gson()
    fun incoming(items: List<FriendInvitation>, user: Long) = items.filter { it.recipientId == user }
    fun unseen(items: List<FriendInvitation>, user: Long, seen: Set<Long>) =
        incoming(items, user).filter { it.id !in seen }
    suspend fun cached(account: String): List<FriendInvitation> =
        LocalStore.instance.records().get(account,"friend-invitations","pending")?.let {
            runCatching { gson.fromJson(it,Array<FriendInvitation>::class.java).toList() }.getOrNull()
        }.orEmpty()

    suspend fun sync(notify: Boolean = true): List<FriendInvitation> = mutex.withLock {
        if(!PreferenceManager.isLoggedIn()) return@withLock emptyList()
        val account=LocalStore.account
        val user=PreferenceManager.userId
        val items=Ui.data(ApiClient.getService().friendInvitations())
        if(account!=LocalStore.account) return@withLock emptyList()
        val dao=LocalStore.instance.records()
        dao.put(LocalRecord(account,"friend-invitations","pending",gson.toJson(items)))
        val prefs=FamilyRecipesApp.instance.getSharedPreferences("friend-alerts",0)
        val seen=prefs.getStringSet(account,emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
        val pending=incoming(items,user)
        val fresh=unseen(items,user,seen)
        val context=FamilyRecipesApp.instance
        val manager=NotificationManagerCompat.from(context)
        val tag="friend-invitations:$account"
        if(pending.isEmpty() || !notify) manager.cancel(tag,1)
        if(notify && fresh.isNotEmpty() && manager.areNotificationsEnabled()) {
            val intent=Intent(context,FriendsListActivity::class.java).putExtra("invitation_account",account)
            val target=PendingIntent.getActivity(context,account.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            try {
                manager.notify(tag,1,NotificationCompat.Builder(context,"family_messages")
                    .setSmallIcon(R.drawable.ic_notification).setContentTitle(context.getString(R.string.friends_title))
                    .setContentText(context.getString(R.string.friend_incoming_count,pending.size))
                    .setContentIntent(target).setAutoCancel(true).build())
            } catch (_: SecurityException) { return@withLock items }
        }
        // Opening the list counts as seeing requests even with notifications disabled.
        if(!notify || manager.areNotificationsEnabled())
            prefs.edit().putStringSet(account,pending.map { it.id.toString() }.toSet()).apply()
        items
    }
}
