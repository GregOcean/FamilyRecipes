package com.familyrecipes.android.data.local

import androidx.room.withTransaction
import com.familyrecipes.android.data.model.*
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.data.remote.ApiErrors
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

// Nullable state supports records written before delivery states were introduced.
data class PendingMessage(val clientId:String,val groupId:Long,val content:String,
    val type:String="text",val error:String?=null,val referenceId:Long?=null,
    val sequence:Long=0,val state:String?="queued",val attempts:Int=0,val nextAttemptAt:Long=0,val errorKind:String?=null)
class ChatSyncException(val status:Int,message:String):Exception(message)
object ChatRepository {
    private val locks=ConcurrentHashMap<String,Mutex>()
    private val sequence=AtomicLong(System.currentTimeMillis()*1000)
    private val json=Gson()
    private val db get()=LocalStore.instance
    private val dao get()=db.records()
    private fun lock(account:String,group:Long)=locks.getOrPut("$account:$group"){Mutex()}
    private fun checkAccount(account:String){
        if(account!=LocalStore.account || !PreferenceManager.isLoggedIn())throw ChatSyncException(401,"Session changed")
    }
    suspend fun enqueue(account:String,groupId:Long,content:String,type:String="text",referenceId:Long?=null) {
        checkAccount(account)
        val pending=PendingMessage(UUID.randomUUID().toString(),groupId,content,type,referenceId=referenceId,
            sequence=sequence.updateAndGet { maxOf(it+1,System.currentTimeMillis()*1000) })
        db.withTransaction {
            dao.put(LocalRecord(account,"outbox",pending.clientId,json.toJson(pending)))
            if(type=="text" && dao.get(account,"draft","chat:$groupId")?.trim()==content.trim()) dao.delete(account,"draft","chat:$groupId")
        }
        scheduleSync()
    }
    fun scheduleSync() {
        val context=com.familyrecipes.android.FamilyRecipesApp.instance
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork("ChatOutbox",
            androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE,
            androidx.work.OneTimeWorkRequestBuilder<com.familyrecipes.android.worker.ChatSyncWorker>()
                .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build()).build())
    }
    suspend fun removePending(account:String,clientId:String) {
        checkAccount(account)
        val pending=dao.get(account,"outbox",clientId)?.let {json.fromJson(it,PendingMessage::class.java)} ?: return
        lock(account,pending.groupId).withLock {dao.delete(account,"outbox",clientId)}
        scheduleSync()
    }
    suspend fun retryPending(account:String,groupId:Long,clientId:String) {
        lock(account,groupId).withLock {
            checkAccount(account)
            val pending=dao.get(account,"outbox",clientId)?.let{json.fromJson(it,PendingMessage::class.java)} ?: return@withLock
            if(pending.groupId==groupId)dao.updateJson(account,"outbox",clientId,json.toJson(pending.copy(state="queued",error=null,errorKind=null,nextAttemptAt=0,attempts=0)))
        }
        scheduleSync()
    }
    suspend fun unavailableGroup(account:String,groupId:Long) = lock(account,groupId).withLock {
        checkAccount(account)
        dao.list(account,"outbox").forEach { row ->
            val item=json.fromJson(row.json,PendingMessage::class.java)
            if(item.groupId==groupId && item.state!="unavailable")
                dao.updateJson(account,"outbox",row.key,json.toJson(item.copy(state="unavailable",errorKind="unavailable",error="group_unavailable",nextAttemptAt=0)))
        }
    }
    suspend fun editFailed(account:String,groupId:Long,clientId:String,content:String) {
        require(content.isNotBlank() && content.length<=4000)
        lock(account,groupId).withLock {
            checkAccount(account)
            val old=dao.get(account,"outbox",clientId)?.let{json.fromJson(it,PendingMessage::class.java)} ?: return@withLock
            check(old.groupId==groupId && old.type=="text" && old.errorKind=="invalid")
            val replacement=old.copy(clientId=UUID.randomUUID().toString(),content=content,state="queued",error=null,errorKind=null,attempts=0,nextAttemptAt=0)
            db.withTransaction{dao.delete(account,"outbox",clientId);dao.put(LocalRecord(account,"outbox",replacement.clientId,json.toJson(replacement)))}
        }
        scheduleSync()
    }
    suspend fun markRead(account:String,groupId:Long,id:Long) = locks.getOrPut("$account:read:$groupId"){Mutex()}.withLock {
        checkAccount(account)
        val key="read:$groupId"
        if((dao.get(account,"cursor",key)?.toLongOrNull()?:0)>=id)return@withLock
        val response=ApiClient.getAccountService(account).markAsRead(mapOf("groupId" to groupId,"messageId" to id))
        checkAccount(account)
        if(response.isSuccessful && response.body()?.code==200){
            dao.put(LocalRecord(account,"cursor",key,id.toString()))
            GroupCache.markRead(account,groupId,id)
        }
    }
    suspend fun loadOlder(account:String,groupId:Long,before:Long):Int = lock(account,groupId).withLock {
        checkAccount(account)
        val response=ApiClient.getAccountService(account).messageHistory(groupId,before)
        if(!response.isSuccessful || response.body()?.code!=200)throw ChatSyncException(response.code(),ApiErrors.message(response,"加载失败"))
        checkAccount(account)
        val rows=response.body()?.data.orEmpty()
        db.withTransaction{rows.forEach {dao.put(LocalRecord(account,"chat:$groupId",it.id.toString(),json.toJson(it)))}}
        rows.size
    }
    suspend fun sync(account:String,groupId:Long):List<Message> = lock(account,groupId).withLock {
        checkAccount(account)
        // Old outbox rows receive a stable sequence once, before recording any failures.
        val rows=dao.list(account,"outbox")
        val pendingRows=DeliveryPolicy.ordered(rows.mapIndexed {index,row ->
            val parsed=json.fromJson(row.json,PendingMessage::class.java)
            val item=if(parsed.sequence==0L)parsed.copy(sequence=index+1L) else parsed
            if(item!=parsed)dao.updateJson(account,"outbox",row.key,json.toJson(item))
            item
        }.filter{it.groupId==groupId})
        for(pending in pendingRows) {
            checkAccount(account)
            if(!DeliveryPolicy.canAutoSend(pending.state,pending.nextAttemptAt,System.currentTimeMillis()))break
            try {
                val api=ApiClient.getAccountService(account)
                val result=when(pending.type){
                    "meal_share"->api.shareMeal(requireNotNull(pending.referenceId),ChatShareRequest(groupId,pending.clientId))
                    "recipe_share"->api.shareRecipeChat(requireNotNull(pending.referenceId),ChatShareRequest(groupId,pending.clientId))
                    else->api.sendMessage(SendMessageRequest(groupId,pending.content,pending.type,pending.clientId))
                }
                checkAccount(account)
                if(result.isSuccessful && result.body()?.code==200 && result.body()?.data?.id!=null){
                    val saved=result.body()!!.data!!
                    db.withTransaction {dao.put(LocalRecord(account,"chat:$groupId",saved.id.toString(),json.toJson(saved)));dao.delete(account,"outbox",pending.clientId)}
                } else {
                    val code=if(result.isSuccessful)result.body()?.code?:400 else result.code()
                    saveFailure(account,pending,DeliveryPolicy.failureKind(code),ApiErrors.message(result,"HTTP $code"))
                    if(code==401 || code==403 || code==404)throw ChatSyncException(code,"消息不可发送")
                    break
                }
            }catch(e:CancellationException){throw e}catch(e:ChatSyncException){throw e}
            catch(e:Exception){checkAccount(account);saveFailure(account,pending,"temporary","network");break}
        }
        val incoming=mutableListOf<Message>()
        var cursor=dao.get(account,"cursor",groupId.toString())?.toLongOrNull()
        if(cursor==null) {
            // First open shows recent history immediately; older history is loaded on demand.
            val response=ApiClient.getAccountService(account).messageHistory(groupId)
            if(!response.isSuccessful || response.body()?.code!=200)throw ChatSyncException(response.code(),ApiErrors.message(response,"同步失败"))
            checkAccount(account)
            val recent=response.body()?.data.orEmpty()
            cursor=recent.maxOfOrNull{it.id?:0}?:0L
            db.withTransaction {
                recent.forEach{dao.put(LocalRecord(account,"chat:$groupId",it.id.toString(),json.toJson(it)))}
                dao.put(LocalRecord(account,"cursor",groupId.toString(),cursor.toString()))
            }
            incoming.addAll(recent)
        } else {
            // Bound each pass so another group or the UI can make progress during a large catch-up.
            repeat(5) {
                checkAccount(account)
                val response=ApiClient.getAccountService(account).syncMessages(groupId,cursor!!)
                if(!response.isSuccessful || response.body()?.code!=200)throw ChatSyncException(response.code(),ApiErrors.message(response,"同步失败"))
                checkAccount(account)
                val page=response.body()?.data.orEmpty()
                if(page.isEmpty())return@withLock incoming
                val next=page.maxOf{it.id?:0};check(next>cursor!!)
                db.withTransaction {
                    page.forEach{dao.put(LocalRecord(account,"chat:$groupId",it.id.toString(),json.toJson(it)))}
                    dao.put(LocalRecord(account,"cursor",groupId.toString(),next.toString()))
                }
                incoming.addAll(page);cursor=next
                if(page.size<200)return@withLock incoming
            }
        }
        incoming
    }
    private suspend fun saveFailure(account:String,p:PendingMessage,kind:String,error:String){
        val attempt=p.attempts+1
        dao.updateJson(account,"outbox",p.clientId,json.toJson(p.copy(error=error,errorKind=kind,attempts=attempt,
            state=when {kind=="auth"->"auth";kind=="unavailable"->"unavailable";kind=="temporary" && attempt<4->"retrying";else->"failed"},
            nextAttemptAt=if(kind=="temporary")System.currentTimeMillis()+DeliveryPolicy.retryDelay(attempt) else 0)))
    }
    suspend fun translateRecent(account:String,groupId:Long) {
        checkAccount(account)
        try {
            val language=ApiClient.contentLanguage()
            val candidates=dao.recent(account,"chat:$groupId",100).map{json.fromJson(it.json,Message::class.java)}.filter{
                it.id!=null && it.messageType=="text" && (it.translation?.language!=language || it.translation.status !in listOf("ready","original","unchanged"))}
            if(candidates.isEmpty())return
            val response=ApiClient.getAccountService(account).chatTranslations(ChatTranslationRequest(groupId,candidates.mapNotNull{it.id}))
            checkAccount(account)
            if(response.isSuccessful && response.body()?.code==200)db.withTransaction {
                candidates.forEach {message->response.body()?.data?.get(message.id.toString())?.let {translation->
                    if(translation!=message.translation)dao.updateJson(account,"chat:$groupId",message.id.toString(),json.toJson(message.copy(translation=translation)))
                }}
            }
        }catch(e:CancellationException){throw e}catch(_:Exception){}
    }
}
