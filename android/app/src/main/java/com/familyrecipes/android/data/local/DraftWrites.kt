package com.familyrecipes.android.data.local
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
/** Ordered writes outlive a destroyed screen; no disk I/O on the main thread. */
object DraftWrites {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    @Volatile private var failure: Throwable? = null
    init { scope.launch {
        for(write in writes) try { write() }
        catch(e:CancellationException) { throw e }
        catch(e:Exception) { failure=e }
    } }
    fun put(account: String, key: String, json: String) {
        writes.trySend { LocalStore.instance.records().put(LocalRecord(account,"draft",key,json));failure=null }
    }
    suspend fun flush() {
        val done = CompletableDeferred<Unit>()
        writes.send { failure?.let { done.completeExceptionally(it) } ?: done.complete(Unit) }; done.await()
    }
    suspend fun clear(account: String, key: String) {
        flush(); LocalStore.instance.records().delete(account,"draft",key)
    }
}
