package com.familyrecipes.android.data.local

import com.familyrecipes.android.data.model.GroupChat
import com.google.gson.Gson

/** Durable snapshots partitioned by server and account, like the chat outbox. */
object GroupCache {
    private val gson = Gson()
    suspend fun list(account: String): List<GroupChat> =
        LocalStore.instance.records().get(account, "groups", "list")?.let {
            runCatching { gson.fromJson(it, Array<GroupChat>::class.java).toList() }.getOrDefault(emptyList())
        }.orEmpty()
    suspend fun save(account: String, groups: List<GroupChat>) {
        check(account == LocalStore.account)
        LocalStore.instance.records().put(LocalRecord(account, "groups", "list", gson.toJson(groups)))
    }
    suspend fun detail(account: String, id: Long): GroupChat? =
        LocalStore.instance.records().get(account, "group-detail", id.toString())?.let {
            runCatching { gson.fromJson(it, GroupChat::class.java) }.getOrNull()
        }
    suspend fun saveDetail(account: String, group: GroupChat) {
        check(account == LocalStore.account)
        LocalStore.instance.records().put(LocalRecord(account, "group-detail", group.id.toString(), gson.toJson(group)))
    }
}
