package com.familyrecipes.android.data.local

import com.familyrecipes.android.data.model.GroupChat
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Durable, account-scoped conversation summaries observed by navigation and the message list. */
object GroupCache {
    private val gson=Gson()
    private val mutex=Mutex()
    private fun decode(text:String?)=text?.let{runCatching{gson.fromJson(it,Array<GroupChat>::class.java).toList()}.getOrNull()}.orEmpty()
    suspend fun list(account:String):List<GroupChat> = decode(LocalStore.instance.records().get(account,"groups","list"))
    fun observe(account:String):Flow<List<GroupChat>> = LocalStore.instance.records().observe(account,"groups").map {rows->decode(rows.firstOrNull{it.key=="list"}?.json)}
    suspend fun save(account:String,groups:List<GroupChat>)=mutex.withLock {
        check(account==LocalStore.account)
        LocalStore.instance.records().put(LocalRecord(account,"groups","list",gson.toJson(groups)))
    }
    suspend fun markRead(account:String,group:Long,id:Long)=mutex.withLock {
        if(account!=LocalStore.account)return@withLock
        val updated=list(account).map{if(it.id==group && (it.lastMessage?.id?:0)<=id)it.copy(unreadCount=0) else it}
        LocalStore.instance.records().put(LocalRecord(account,"groups","list",gson.toJson(updated)))
    }
    suspend fun remove(account:String,id:Long)=mutex.withLock {
        if(account!=LocalStore.account)return@withLock
        LocalStore.instance.records().put(LocalRecord(account,"groups","list",gson.toJson(list(account).filter{it.id!=id})))
        LocalStore.instance.records().delete(account,"group-detail",id.toString())
    }
    suspend fun detail(account:String,id:Long):GroupChat? = LocalStore.instance.records().get(account,"group-detail",id.toString())?.let{
        runCatching{gson.fromJson(it,GroupChat::class.java)}.getOrNull()}
    suspend fun saveDetail(account:String,group:GroupChat){
        check(account==LocalStore.account)
        LocalStore.instance.records().put(LocalRecord(account,"group-detail",group.id.toString(),gson.toJson(group)))
    }
}
