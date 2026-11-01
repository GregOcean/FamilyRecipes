package com.familyrecipes.android.data.local

import androidx.room.withTransaction
import com.familyrecipes.android.data.model.Message
import com.familyrecipes.android.data.remote.ApiClient
import com.google.gson.Gson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class PendingMessage(val clientId: String, val groupId: Long, val content: String,
    val type: String = "text", val error: String? = null)
object ChatRepository {
    private val mutex = Mutex()
    private val json = Gson()
    private val db get() = LocalStore.instance
    private val dao get() = db.records()
    private fun checkAccount(account: String) { check(account == LocalStore.account && PreferenceManager.isLoggedIn()) }
    suspend fun enqueue(account: String, groupId: Long, content: String, type: String = "text") {
        checkAccount(account)
        val pending = PendingMessage(UUID.randomUUID().toString(), groupId, content, type)
        db.withTransaction {
            dao.put(LocalRecord(account, "outbox", pending.clientId, json.toJson(pending)))
            if(type == "text") dao.delete(account, "draft", "chat:$groupId")
        }
    }
    suspend fun sync(account: String, groupId: Long): List<Message> = mutex.withLock {
        checkAccount(account)
        // Idempotent REST sends survive lost acknowledgements and process death.
        for (row in dao.list(account, "outbox")) {
            val pending = json.fromJson(row.json, PendingMessage::class.java)
            if(pending.groupId != groupId) continue
            checkAccount(account)
            val result = ApiClient.getService().sendMessage(mapOf("group_id" to groupId,
                "content" to pending.content, "message_type" to pending.type, "client_message_id" to pending.clientId))
            checkAccount(account)
            if(result.isSuccessful && result.body()?.code == 200) {
                dao.delete(account, "outbox", pending.clientId)
            } else {
                dao.put(row.copy(json = json.toJson(pending.copy(error = "发送失败（${result.code()}），点此重试"))))
                if(result.code() == 401 || result.code() == 403 || result.code() == 404) break
            }
        }
        val incoming = mutableListOf<Message>()
        var cursor = dao.get(account, "cursor", groupId.toString())?.toLongOrNull() ?: 0L
        while(true) {
            checkAccount(account)
            val result = ApiClient.getService().syncMessages(groupId, cursor)
            checkAccount(account)
            check(result.isSuccessful && result.body()?.code == 200) { "消息同步失败" }
            val page = result.body()?.data.orEmpty()
            if(page.isEmpty()) break
            val next = page.maxOf { it.id ?: 0 }
            check(next > cursor) { "消息游标未前进" }
            db.withTransaction {
                page.forEach { dao.put(LocalRecord(account, "chat:$groupId", it.id.toString(), json.toJson(it))) }
                dao.put(LocalRecord(account, "cursor", groupId.toString(), next.toString()))
            }
            incoming.addAll(page)
            cursor = next
            if(page.size < 200) break
        }
        incoming
    }
}
