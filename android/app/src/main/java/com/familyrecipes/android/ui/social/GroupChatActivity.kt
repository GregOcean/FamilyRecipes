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

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.familyrecipes.android.data.local.*
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.CancellationException
/** Durable chat: WebSocket is a wake-up hint; database cursors define delivery. */
class GroupChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGroupChatBinding
    private val messages = mutableListOf<Message>()
    private lateinit var adapter: MessageAdapter
    private var groupId = 0L
    private val account = LocalStore.account
    private val userId = PreferenceManager.userId
    private val json = Gson()
    private var restoring = true
    private var draftRevision = 0
    private val ws by lazy { WebSocketManager.getInstance() }
    companion object {
        const val EXTRA_GROUP_ID = "group_id"
        const val EXTRA_GROUP_NAME = "group_name"
        const val EXTRA_MEMBER_COUNT = "member_count"
        var visibleGroup: Long? = null
    }
    private val notificationPermission=registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }
    private val stickerPicker = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if(uri != null) lifecycleScope.launch {
            try {
                val file = java.io.File.createTempFile("sticker", ".png", cacheDir)
                try {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        contentResolver.openInputStream(uri)!!.use { input -> file.outputStream().use { input.copyTo(it) } }
                    }
                    val body = okhttp3.MultipartBody.Part.createFormData("file", file.name,
                        file.asRequestBody("image/*".toMediaType()))
                    val response = ApiClient.getService().uploadImage(body, "sticker")
                    check(response.isSuccessful && response.body()?.code == 200)
                    ChatRepository.enqueue(account, groupId, response.body()!!.data!!, "sticker")
                    sync()
                } finally { file.delete() }
            } catch(e: CancellationException) { throw e }
              catch(e: Exception) { toast("表情包未发送，请重试") }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupChatBinding.inflate(layoutInflater); setContentView(binding.root)
        groupId = intent.getLongExtra(EXTRA_GROUP_ID, 0)
        if(groupId == 0L) { finish(); return }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.tvGroupName.text = intent.getStringExtra(EXTRA_GROUP_NAME)
        binding.btnSearch.visibility = View.GONE
        binding.btnSettings.contentDescription = "重试待发送消息"
        binding.btnSettings.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this).setItems(arrayOf("重试待发送消息","允许后台消息通知")) { _,index ->
                if(index==0) lifecycleScope.launch { sync() }
                else if(android.os.Build.VERSION.SDK_INT>=33) notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }.show()
        }
        binding.btnSticker.setOnClickListener { stickerPicker.launch("image/*") }
        adapter = MessageAdapter(messages, userId)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvMessages.adapter = adapter
        val dao = LocalStore.instance.records()
        lifecycleScope.launch {
            val revision = draftRevision
            val draft = dao.get(account, "draft", "chat:$groupId")
            if(revision == draftRevision) binding.etMessage.setText(draft.orEmpty())
            restoring = false
        }
        binding.etMessage.doAfterTextChanged {
            draftRevision++
            if(!restoring) DraftWrites.put(account, "chat:$groupId", it.toString())
        }
        binding.btnSend.setOnClickListener {
            val content = binding.etMessage.text.toString().trim()
            if(content.isBlank()) return@setOnClickListener
            binding.btnSend.isEnabled = false
            lifecycleScope.launch {
                try {
                    DraftWrites.flush()
                    ChatRepository.enqueue(account, groupId, content)
                    if(binding.etMessage.text.toString().trim() == content) binding.etMessage.setText("")
                    sync()
                } catch(e: CancellationException) { throw e }
                  catch(e: Exception) { toast("消息已保留，联网后重试") }
                finally { binding.btnSend.isEnabled = true }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(dao.observe(account, "chat:$groupId"), dao.observe(account, "outbox")) { rows, pending ->
                        rows.map { json.fromJson(it.json, Message::class.java) }.sortedBy { it.id } to
                            pending.map { json.fromJson(it.json, PendingMessage::class.java) }.filter { it.groupId == groupId }
                    }.collect { (history, pending) ->
                        messages.clear(); messages.addAll(history)
                        pending.forEach { messages.add(Message(id=null, groupId=groupId, senderId=userId,
                            content=if(it.type=="sticker") "[表情包] 待发送" else "${it.content}\n${it.error ?: "待发送"}",
                            messageType="text", createdAt=null, sender=null, clientMessageId=it.clientId)) }
                        adapter.notifyDataSetChanged()
                        if(messages.isNotEmpty()) binding.rvMessages.scrollToPosition(messages.size-1)
                        history.lastOrNull()?.id?.let { id ->
                            try { ApiClient.getService().markAsRead(mapOf("groupId" to groupId, "messageId" to id)) }
                            catch(e: CancellationException) { throw e } catch(_: Exception) { }
                        }
                    }
                }
                launch { while(true) { sync(); delay(10000) } }
                launch { ws.messageFlow.collect { if(it.groupId == groupId) sync() } }
            }
        }
    }
    override fun onStart() { super.onStart(); visibleGroup=groupId; ws.connect(groupId,userId) }
    override fun onStop() { visibleGroup=null; ws.disconnect(); super.onStop() }
    private suspend fun sync() {
        try { ChatRepository.sync(account, groupId) }
        catch(e: CancellationException) { throw e }
        catch(_: Exception) { /* Persistent outbox remains visible; periodic sync retries. */ }
    }
    private fun toast(text: String) { Toast.makeText(this,text,Toast.LENGTH_SHORT).show() }
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
                holder.tvMessage.text = if(message.messageType == "sticker") "" else message.content
                android.text.util.Linkify.addLinks(holder.tvMessage, android.text.util.Linkify.WEB_URLS)
                android.text.util.Linkify.addLinks(holder.tvMessage,java.util.regex.Pattern.compile("familyrecipes://meal/[0-9]+"),null)
                holder.tvMessage.movementMethod=android.text.method.LinkMovementMethod.getInstance()
                holder.ivSticker.visibility = if(message.messageType == "sticker") View.VISIBLE else View.GONE
                if(message.messageType == "sticker") com.bumptech.glide.Glide.with(holder.itemView)
                    .load(com.familyrecipes.android.util.MediaImages.model(message.content))
                    .signature(com.bumptech.glide.signature.ObjectKey(PreferenceManager.userId)).into(holder.ivSticker)
                holder.tvTime.text = formatTime(message.createdAt)
            }
            is ReceivedMessageViewHolder -> {
                holder.tvMessage.text = if(message.messageType == "sticker") "" else message.content
                android.text.util.Linkify.addLinks(holder.tvMessage, android.text.util.Linkify.WEB_URLS)
                android.text.util.Linkify.addLinks(holder.tvMessage,java.util.regex.Pattern.compile("familyrecipes://meal/[0-9]+"),null)
                holder.tvMessage.movementMethod=android.text.method.LinkMovementMethod.getInstance()
                holder.ivSticker.visibility = if(message.messageType == "sticker") View.VISIBLE else View.GONE
                if(message.messageType == "sticker") com.bumptech.glide.Glide.with(holder.itemView)
                    .load(com.familyrecipes.android.util.MediaImages.model(message.content))
                    .signature(com.bumptech.glide.signature.ObjectKey(PreferenceManager.userId)).into(holder.ivSticker)
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
        val ivSticker: ImageView = ImageView(view.context).also { image ->
            (view.findViewById<TextView>(R.id.tv_message).parent as ViewGroup).addView(image, ViewGroup.LayoutParams(240,240))
        }
        val tvMessage: TextView = view.findViewById(R.id.tv_message)
        val tvTime: TextView = view.findViewById(R.id.tv_time)
        val ivAvatar: ImageView = view.findViewById(R.id.iv_avatar)
    }

    class ReceivedMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivSticker: ImageView = ImageView(view.context).also { image ->
            (view.findViewById<TextView>(R.id.tv_message).parent as ViewGroup).addView(image, ViewGroup.LayoutParams(240,240))
        }
        val tvMessage: TextView = view.findViewById(R.id.tv_message)
        val tvTime: TextView = view.findViewById(R.id.tv_time)
        val tvSenderName: TextView = view.findViewById(R.id.tv_sender_name)
        val ivAvatar: ImageView = view.findViewById(R.id.iv_avatar)
    }
}

