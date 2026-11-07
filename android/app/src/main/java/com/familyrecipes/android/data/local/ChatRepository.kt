package com.familyrecipes.android.data.local

import androidx.room.withTransaction
import com.familyrecipes.android.data.model.*
import kotlinx.coroutines.CancellationException
import com.familyrecipes.android.data.remote.ApiClient
import com.google.gson.Gson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class PendingMessage(val clientId: String, val groupId: Long, val content: String,
    val type: String = "text", val error: String? = null, val referenceId: Long? = null)
object ChatRepository {
    private val mutex = Mutex()
    private val json = Gson()
    private val db get() = LocalStore.instance
    private val dao get() = db.records()
    private fun checkAccount(account: String) { check(account == LocalStore.account && PreferenceManager.isLoggedIn()) }
    suspend fun enqueue(account: String, groupId: Long, content: String, type: String = "text", referenceId: Long? = null) {
        checkAccount(account)
        val pending = PendingMessage(UUID.randomUUID().toString(), groupId, content, type, referenceId = referenceId)
        db.withTransaction {
            dao.put(LocalRecord(account, "outbox", pending.clientId, json.toJson(pending)))
            if(type == "text" && dao.get(account, "draft", "chat:$groupId")?.trim() == content.trim())
                dao.delete(account, "draft", "chat:$groupId")
        }
    }
    suspend fun removePending(account: String, clientId: String) {
        checkAccount(account)
        mutex.withLock { dao.delete(account, "outbox", clientId) }
    }
    suspend fun sync(account: String, groupId: Long): List<Message> = mutex.withLock {
        checkAccount(account)
        // Idempotent REST sends survive lost acknowledgements and process death.
        for (row in dao.list(account, "outbox")) {
            val pending = json.fromJson(row.json, PendingMessage::class.java)
            if(pending.groupId != groupId) continue
            checkAccount(account)
            try {
                val api = ApiClient.getService()
                val result = when(pending.type) {
                    "meal_share" -> api.shareMeal(requireNotNull(pending.referenceId), ChatShareRequest(groupId, pending.clientId))
                    "recipe_share" -> api.shareRecipeChat(requireNotNull(pending.referenceId), ChatShareRequest(groupId, pending.clientId))
                    else -> api.sendMessage(SendMessageRequest(groupId, pending.content, pending.type, pending.clientId))
                }
                checkAccount(account)
                if(result.isSuccessful && result.body()?.code == 200 && result.body()?.data?.id != null) {
                    val saved = result.body()!!.data!!
                    // Persist the acknowledged message and remove its outbox row atomically.
                    // Never advance the receive cursor here: other senders may precede this message.
                    db.withTransaction {
                        dao.put(LocalRecord(account, "chat:$groupId", saved.id.toString(), json.toJson(saved)))
                        dao.delete(account, "outbox", pending.clientId)
                    }
                } else {
                    val error = com.familyrecipes.android.data.remote.ApiErrors.message(result, "HTTP ${result.code()}")
                    dao.put(row.copy(json = json.toJson(pending.copy(error = error))))
                }
            } catch(e: CancellationException) { throw e }
              catch(e: Exception) {
                dao.put(row.copy(json = json.toJson(pending.copy(error = "network"))))
                break // Retain pending messages; still attempt receive catch-up below.
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
