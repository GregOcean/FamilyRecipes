package com.familyrecipes.android.websocket

import android.util.Log
import com.familyrecipes.android.data.local.PreferenceManager
import okhttp3.HttpUrl.Companion.toHttpUrl
import com.familyrecipes.android.data.model.Message
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * WebSocket管理器
 */
class WebSocketManager private constructor() {

    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // 消息流
    private val _messageFlow = MutableSharedFlow<Message>(replay = 0)
    val messageFlow: SharedFlow<Message> = _messageFlow
    
    // 连接状态流
    private val _connectionState = MutableSharedFlow<ConnectionState>(replay = 1)
    val connectionState: SharedFlow<ConnectionState> = _connectionState
    
    private var generation = 0
    private var connected = false
    private var currentGroupId: Long? = null
    private var currentUserId: Long? = null

    companion object {
        private const val TAG = "WebSocketManager"
        
        @Volatile
        private var instance: WebSocketManager? = null

        fun getInstance(): WebSocketManager {
            return instance ?: synchronized(this) {
                instance ?: WebSocketManager().also { instance = it }
            }
        }
    }

    /**
     * 连接WebSocket
     */
    fun connect(groupId: Long, userId: Long) {
        if (webSocket != null && currentGroupId == groupId && currentUserId == userId && connected) {
            Log.d(TAG, "WebSocket已连接到群组: $groupId")
            return
        }

        disconnect()

        val attempt = generation
        currentGroupId = groupId
        currentUserId = userId

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MINUTES) // 无限期读取
            .build()

        val token = PreferenceManager.authToken ?: return
        val base = PreferenceManager.baseUrl.toHttpUrl()
        val endpoint = base.newBuilder().encodedPath("/ws/chat").query(null).build().toString()
            .replaceFirst("https://", "wss://").replaceFirst("http://", "ws://")
        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (attempt != generation) {
                    webSocket.close(1000, "Stale connection")
                    return
                }
                connected = true
                scope.launch {
                    _connectionState.emit(ConnectionState.Connected)
                }
                // 发送订阅消息
                subscribeToGroup(groupId)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {

                if (attempt == generation) handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (attempt != generation) return
                connected = false
                Log.e(TAG, "WebSocket连接失败", t)
                scope.launch {
                    _connectionState.emit(ConnectionState.Disconnected(t.message))
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket关闭中: $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (attempt != generation) return
                connected = false
                scope.launch {
                    _connectionState.emit(ConnectionState.Disconnected(reason))
                }
            }
        })
    }

    /**
     * 订阅群组消息
     */
    private fun subscribeToGroup(groupId: Long) {
        val subscribeMsg = JSONObject().apply {
            put("command", "SUBSCRIBE")
            put("destination", "/topic/group/$groupId")
            put("id", "sub-$groupId")
        }
        webSocket?.send(subscribeMsg.toString())
        Log.d(TAG, "已订阅群组: $groupId")
    }

    /**
     * 发送消息
     */
    fun sendMessage(groupId: Long, userId: Long, content: String): Boolean {
        if (!connected) return false
        val message = JSONObject().apply {
            put("command", "SEND")
            put("destination", "/app/chat.sendMessage")
            put("body", JSONObject().apply {
                put("groupId", groupId)
                put("content", content)
                put("messageType", "text")
            })
        }
        
        return webSocket?.send(message.toString()) == true
    }

    /**
     * 处理收到的消息
     */
    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            
            // 检查是否是消息体
            if (json.has("command") && json.getString("command") == "MESSAGE") {
                val body = json.getJSONObject("body").toString()
                val message = gson.fromJson(body, Message::class.java)
                
                scope.launch {
                    _messageFlow.emit(message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析消息失败", e)
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        generation++
        connected = false
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        currentGroupId = null
        currentUserId = null
        Log.d(TAG, "WebSocket已断开")
    }

    /**
     * 连接状态
     */
    sealed class ConnectionState {
        object Connected : ConnectionState()
        data class Disconnected(val reason: String?) : ConnectionState()
        object Connecting : ConnectionState()
    }
}

