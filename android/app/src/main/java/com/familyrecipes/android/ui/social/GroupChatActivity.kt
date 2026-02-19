package com.familyrecipes.android.ui.social

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.familyrecipes.android.R
import com.familyrecipes.android.data.local.PreferenceManager
import com.familyrecipes.android.data.model.Message
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.databinding.ActivityGroupChatBinding
import com.familyrecipes.android.websocket.WebSocketManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 群聊天Activity - WebSocket版本
 */
class GroupChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupChatBinding
    private val messages = mutableListOf<Message>()
    private lateinit var adapter: MessageAdapter
    private lateinit var wsManager: WebSocketManager
    
    private var groupId: Long = 0
    private var groupName: String = ""
    private var memberCount: Int = 0
    private val currentUserId: Long by lazy { PreferenceManager.userId ?: 0 }

    companion object {
        const val EXTRA_GROUP_ID = "group_id"
        const val EXTRA_GROUP_NAME = "group_name"
        const val EXTRA_MEMBER_COUNT = "member_count"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityGroupChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取群组信息
        groupId = intent.getLongExtra(EXTRA_GROUP_ID, 0)
        groupName = intent.getStringExtra(EXTRA_GROUP_NAME) ?: ""
        memberCount = intent.getIntExtra(EXTRA_MEMBER_COUNT, 0)

        if (groupId == 0L) {
            Toast.makeText(this, "群组ID错误", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        wsManager = WebSocketManager.getInstance()

        setupToolbar()
        setupRecyclerView()
        setupInputArea()
        loadHistoryMessages()
        connectWebSocket()
        observeMessages()
    }

    override fun onDestroy() {
        super.onDestroy()
        wsManager.disconnect()
    }

    /**
     * 设置工具栏
     */
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        binding.tvGroupName.text = "$groupName ($memberCount)"
        
        binding.btnSearch.setOnClickListener {
            Toast.makeText(this, "搜索功能开发中", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnSettings.setOnClickListener {
            Toast.makeText(this, "群设置功能开发中", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        adapter = MessageAdapter(messages, currentUserId)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter
    }

    /**
     * 设置输入区域
     */
    private fun setupInputArea() {
        binding.btnSend.setOnClickListener {
            val content = binding.etMessage.text.toString().trim()
            
            if (content.isEmpty()) {
                Toast.makeText(this, "请输入消息", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            sendMessage(content)
        }
    }

    /**
     * 加载历史消息
     */
    private fun loadHistoryMessages() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().getMessages(
                    groupId = groupId,
                    pageNum = 1,
                    pageSize = 50
                )

                if (response.isSuccessful && response.body()?.code == 200) {
                    messages.clear()
                    response.body()?.data?.let { messages.addAll(it) }
                    adapter.notifyDataSetChanged()
                    scrollToBottom()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 连接WebSocket
     */
    private fun connectWebSocket() {
        wsManager.connect(groupId, currentUserId)
        
        // 监听连接状态
        lifecycleScope.launch {
            wsManager.connectionState.collectLatest { state ->
                when (state) {
                    is WebSocketManager.ConnectionState.Connected -> {
                        Toast.makeText(this@GroupChatActivity, "已连接", Toast.LENGTH_SHORT).show()
                    }
                    is WebSocketManager.ConnectionState.Disconnected -> {
                        Toast.makeText(this@GroupChatActivity, "连接断开: ${state.reason}", Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * 监听WebSocket消息
     */
    private fun observeMessages() {
        lifecycleScope.launch {
            wsManager.messageFlow.collectLatest { message ->
                // 收到新消息
                messages.add(message)
                adapter.notifyItemInserted(messages.size - 1)
                scrollToBottom()
            }
        }
    }

    /**
     * 发送消息（通过WebSocket）
     */
    private fun sendMessage(content: String) {
        wsManager.sendMessage(groupId, currentUserId, content)
        binding.etMessage.setText("")
    }

    /**
     * 滚动到底部
     */
    private fun scrollToBottom() {
        if (messages.isNotEmpty()) {
            binding.rvMessages.smoothScrollToPosition(messages.size - 1)
        }
    }
}

/**
 * 消息适配器
 */
class MessageAdapter(
    private val messages: List<Message>,
    private val currentUserId: Long
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        
        when (holder) {
            is SentMessageViewHolder -> {
                holder.tvMessage.text = message.content
                holder.tvTime.text = formatTime(message.createdAt)
            }
            is ReceivedMessageViewHolder -> {
                holder.tvMessage.text = message.content
                holder.tvTime.text = formatTime(message.createdAt)
                holder.tvSenderName.text = message.sender?.username ?: "未知用户"
            }
        }
    }

    override fun getItemCount() = messages.size

    private fun formatTime(dateString: String?): String {
        if (dateString == null) return ""
        try {
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(dateString)
            return if (date != null) timeFormat.format(date) else ""
        } catch (e: Exception) {
            return ""
        }
    }

    class SentMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tv_message)
        val tvTime: TextView = view.findViewById(R.id.tv_time)
        val ivAvatar: ImageView = view.findViewById(R.id.iv_avatar)
    }

    class ReceivedMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tv_message)
        val tvTime: TextView = view.findViewById(R.id.tv_time)
        val tvSenderName: TextView = view.findViewById(R.id.tv_sender_name)
        val ivAvatar: ImageView = view.findViewById(R.id.iv_avatar)
    }
}

