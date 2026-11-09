package com.familyrecipes.android.ui.social

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Spanned
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyrecipes.android.R
import com.familyrecipes.android.data.local.*
import com.familyrecipes.android.data.model.*
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.databinding.ActivityGroupChatBinding
import com.familyrecipes.android.ui.design.Ui
import com.familyrecipes.android.ui.design.UiStrings
import com.familyrecipes.android.ui.recipe.RecipePickerActivity
import com.familyrecipes.android.websocket.WebSocketManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody

/** Durable REST delivery; WebSocket only wakes the cursor-based receiver. */
class GroupChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGroupChatBinding
    private val messages = mutableListOf<Message>()
    private lateinit var adapter: MessageAdapter
    private var members = emptyList<GroupMember>()
    private var groupId = 0L
    private val account = LocalStore.account
    private val userId = PreferenceManager.userId
    private val json = Gson()
    private var restoring = true
    private var draftRevision = 0
    private var choosingMembers = false
    private var syncJob: Job? = null
    private val ws by lazy { WebSocketManager.getInstance() }
    companion object {
        const val EXTRA_GROUP_ID = "group_id"
        const val EXTRA_GROUP_NAME = "group_name"
        const val EXTRA_MEMBER_COUNT = "member_count"
        var visibleGroup: Long? = null
    }
    private fun s(id: Int) = getString(id)
    private val recipePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode == Activity.RESULT_OK) {
            val dishes = runCatching { json.fromJson(result.data?.getStringExtra("dishes"), Array<MealDish>::class.java).orEmpty().toList() }.getOrDefault(emptyList())
            if(dishes.isNotEmpty()) confirmShare(dishes.joinToString("、") { it.name }) {
                dishes.forEach { dish -> dish.recipeId?.let { id ->
                    ChatRepository.enqueue(account, groupId, "菜谱：${dish.name}\nfamilyrecipes://recipe/$id", "recipe_share", id)
                } }
            }
        }
    }
    private val stickerPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri != null) lifecycleScope.launch {
            try {
                val file = java.io.File.createTempFile("sticker", ".png", cacheDir)
                try {
                    withContext(Dispatchers.IO) {
                        contentResolver.openInputStream(uri)!!.use { input -> file.outputStream().use { input.copyTo(it) } }
                    }
                    val body = okhttp3.MultipartBody.Part.createFormData("file", file.name, file.asRequestBody("image/*".toMediaType()))
                    val url = Ui.data(ApiClient.getService().uploadImage(body, "sticker"))
                    ChatRepository.enqueue(account, groupId, url, "sticker")
                    requestSync()
                } finally { file.delete() }
            } catch(e: CancellationException) { throw e }
              catch(e: Exception) { Ui.toast(this@GroupChatActivity, e) }
        }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        binding = ActivityGroupChatBinding.inflate(layoutInflater); setContentView(binding.root)
        groupId = intent.getLongExtra(EXTRA_GROUP_ID, 0)
        if(groupId <= 0) { finish(); return }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.tvGroupName.text = intent.getStringExtra(EXTRA_GROUP_NAME)
        binding.btnMembers.setOnClickListener { showMembers(false) }
        binding.tvGroupName.setOnClickListener { showMembers(false) }
        binding.btnMention.setOnClickListener { showMembers(true) }
        binding.btnSticker.setOnClickListener { showEmoji() }
        binding.etMessage.setOnTouchListener { _, event ->
            if(event.action == android.view.MotionEvent.ACTION_DOWN) hideEmoji()
            false
        }
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(binding.emojiPanel.visibility == View.VISIBLE) hideEmoji() else finish()
            }
        })
        binding.btnShare.setOnClickListener { showShare() }
        binding.tvSync.setOnClickListener { requestSync() }
        adapter = MessageAdapter(messages, userId, ::pendingAction)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvMessages.adapter = adapter
        lifecycleScope.launch { loadMembers() }
        val dao = LocalStore.instance.records()
        lifecycleScope.launch {
            val revision = draftRevision
            val draft = dao.get(account, "draft", "chat:$groupId")
            if(revision == draftRevision) {
                binding.etMessage.setText(ChatMentions.decode(draft.orEmpty()))
                binding.etMessage.setSelection(binding.etMessage.length())
            }
            restoring = false
        }
        binding.etMessage.doAfterTextChanged { text ->
            draftRevision++
            if(!restoring) {
                DraftWrites.put(account, "chat:$groupId", ChatMentions.encode(text ?: ""))
                val cursor = binding.etMessage.selectionStart
                if(cursor > 0 && text?.getOrNull(cursor - 1) in listOf('@', '＠') && !choosingMembers)
                    showMembers(true, cursor - 1)
            }
        }
        binding.btnSend.setOnClickListener {
            val content = ChatMentions.encode(binding.etMessage.text).trim()
            if(content.isBlank()) return@setOnClickListener
            if(content.length > 4000) { toast(s(R.string.chat_length)); return@setOnClickListener }
            binding.btnSend.isEnabled = false
            lifecycleScope.launch {
                try {
                    DraftWrites.flush()
                    ChatRepository.enqueue(account, groupId, content)
                    if(ChatMentions.encode(binding.etMessage.text).trim() == content) binding.etMessage.setText("")
                    requestSync()
                } catch(e: CancellationException) { throw e }
                  catch(e: Exception) { Ui.toast(this@GroupChatActivity, e) }
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
                        val atBottom = !binding.rvMessages.canScrollVertically(1)
                        val previousTail=messages.lastOrNull()?.let { it.clientMessageId ?: it.id?.toString() }
                        val knownIds = history.mapNotNull { it.clientMessageId }.toSet()
                        messages.clear(); messages.addAll(history)
                        pending.filter { it.clientId !in knownIds }.forEach { item ->
                            messages.add(Message(id=null, groupId=groupId, senderId=userId,
                                content=item.content, messageType=if(item.type == "sticker") "sticker" else "text",
                                createdAt=null, sender=null, clientMessageId=item.clientId, localError=item.error))
                        }
                        adapter.notifyDataSetChanged()
                        val tail=messages.lastOrNull()
                        val sentNew=tail?.senderId==userId && (tail.clientMessageId ?: tail.id?.toString())!=previousTail
                        if((atBottom || sentNew) && messages.isNotEmpty()) binding.rvMessages.scrollToPosition(messages.lastIndex)
                        history.lastOrNull()?.id?.let { id ->
                            try { ApiClient.getService().markAsRead(mapOf("groupId" to groupId, "messageId" to id)) }
                            catch(e: CancellationException) { throw e } catch(_: Exception) { }
                        }
                    }
                }
                launch { while(true) { requestSync(); delay(10000) } }
                launch { ws.messageFlow.collect { if(it.groupId == groupId) requestSync() } }
            }
        }
    }
    override fun onStart() { super.onStart(); visibleGroup=groupId; ws.connect(groupId,userId) }
    override fun onStop() { visibleGroup=null; ws.disconnect(); super.onStop() }
    private fun requestSync() {
        if(syncJob?.isActive == true) return
        syncJob = lifecycleScope.launch {
            try {
                ChatRepository.sync(account, groupId)
                binding.tvSync.visibility = View.GONE
            } catch(e: CancellationException) { throw e }
              catch(e: Exception) {
                binding.tvSync.text = s(R.string.chat_offline)
                binding.tvSync.visibility = View.VISIBLE
                android.util.Log.w("GroupChat", "Sync failed: ${e.javaClass.simpleName}")
            }
        }
    }
    private suspend fun loadMembers(): Boolean {
        GroupCache.detail(account,groupId)?.let { cached ->
            members=cached.members.orEmpty()
            adapter.members=members
            adapter.notifyDataSetChanged()
        }
        return try {
            val group = Ui.data(ApiClient.getService().getGroupDetail(groupId))
            if(account!=LocalStore.account) return false
            GroupCache.saveDetail(account,group)
            members = group.members.orEmpty()
            binding.tvGroupName.text = getString(R.string.chat_group_count, group.name, members.size)
            adapter.members = members
            adapter.notifyDataSetChanged()
            true
        } catch(e: CancellationException) { throw e }
          catch(_: Exception) { members.isNotEmpty() }
    }
    private fun sheet(title: String = ""): Pair<BottomSheetDialog, LinearLayout> {
        val dialog = BottomSheetDialog(this)
        val root = Ui.box(this)
        if (title.isNotBlank()) root.addView(Ui.text(this, title, 22f, true))
        dialog.setContentView(Ui.scroll(this, root))
        dialog.show()
        return dialog to root
    }
    private fun showMembers(mention: Boolean, typedAt: Int? = null) {
        if(choosingMembers) return
        choosingMembers = true
        val (dialog, root) = sheet(s(if(mention) R.string.chat_mention else R.string.chat_members))
        dialog.setOnDismissListener { choosingMembers = false }
        val body = Ui.box(this, 0); root.addView(body)
        fun load() { lifecycleScope.launch {
            body.removeAllViews(); body.addView(Ui.text(this@GroupChatActivity, s(R.string.chat_loading)))
            val success = loadMembers()
            if(!dialog.isShowing) return@launch
            body.removeAllViews()
            if(!success) {
                body.addView(Ui.button(this@GroupChatActivity, s(R.string.chat_failed_load)) { load() }); return@launch
            }
            if(!mention)body.addView(Ui.button(this@GroupChatActivity,getString(R.string.group_manage_members)) {
                dialog.dismiss()
                startActivity(Intent(this@GroupChatActivity,com.familyrecipes.android.ui.settings.GroupSettingsActivity::class.java).putExtra("group_id",groupId))
            })
            body.addView(Ui.text(this@GroupChatActivity, s(R.string.chat_member_hint), 14f))
            if(members.none { it.userId != userId }) body.addView(Ui.text(this@GroupChatActivity, s(R.string.chat_no_members)))
            members.forEach { member ->
                val id = member.userId ?: return@forEach
                val name = member.nickname?.takeIf { it.isNotBlank() } ?: member.user?.username ?: getString(R.string.chat_member_fallback,id)
                val row = Ui.row(this@GroupChatActivity)
                row.addView(com.familyrecipes.android.ui.design.UserAvatar.create(this@GroupChatActivity, member.user?.username ?: name, member.user?.avatar), LinearLayout.LayoutParams(dp(44),dp(44)))
                row.addView(Ui.button(this@GroupChatActivity, if(id == userId) "$name · ${s(R.string.chat_you)}" else "@$name") {
                    if(id != userId) {
                        val input = binding.etMessage
                        val start = if(typedAt != null && input.text.getOrNull(typedAt) in listOf('@', '＠')) typedAt else input.selectionStart.coerceAtLeast(0)
                        val end = if(start == typedAt) start + 1 else input.selectionEnd.coerceAtLeast(start)
                        val label = "@$name"
                        val inserted = android.text.SpannableStringBuilder("$label ")
                        inserted.setSpan(MentionSpan(id,label),0,label.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        input.text.replace(start,end,inserted); input.setSelection(start+inserted.length)
                        dialog.dismiss(); input.requestFocus()
                    }
                }, LinearLayout.LayoutParams(0,-2,1f))
                body.addView(row)
            }
        } }
        load()
    }
    private fun hideEmoji() {
        binding.emojiPanel.visibility = View.GONE
        binding.btnSticker.isSelected = false
    }
    private fun showEmoji() {
        if(binding.emojiPanel.visibility == View.VISIBLE) { hideEmoji(); return }
        (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
            .hideSoftInputFromWindow(binding.etMessage.windowToken, 0)
        val root = binding.emojiContent
        root.removeAllViews()
        val emoji = listOf(
            "😊","🥰","😂","🥺",
            "😋","🤤","😍","🥳",
            "👍","👏","❤️","😅",
            "🍚","🍜","🥟","🍲",
            "🥬","🥕","🥩","🍤"
        )
        emoji.chunked(4).forEach { line ->
            val row = Ui.row(this)
            line.forEach { face -> row.addView(Ui.button(this,face) {
                val input=binding.etMessage
                val start=input.selectionStart.coerceAtLeast(0)
                val end=input.selectionEnd.coerceAtLeast(0)
                input.text.replace(minOf(start,end),maxOf(start,end),face)
                input.setSelection(minOf(start,end)+face.length)
            }.apply { textSize=28f; contentDescription=face }, LinearLayout.LayoutParams(0,dp(56),1f)) }
            root.addView(row)
        }
        root.addView(Ui.button(this,s(R.string.chat_upload_sticker)) { hideEmoji(); stickerPicker.launch("image/*") })
        binding.emojiPanel.visibility=View.VISIBLE
        binding.btnSticker.isSelected=true
    }
    private fun showShare() {
        val (dialog, root)=sheet(s(R.string.chat_share))
        root.addView(Ui.text(this,s(R.string.chat_share_hint),14f))
        root.addView(Ui.entry(this,s(R.string.chat_share_order),s(R.string.chat_choose_meal),R.drawable.ic_ui_order) { dialog.dismiss(); chooseMeal("order") })
        root.addView(Ui.entry(this,s(R.string.chat_share_prep),s(R.string.chat_choose_meal),R.drawable.ic_ui_prep) { dialog.dismiss(); chooseMeal("prep") })
        root.addView(Ui.entry(this,s(R.string.chat_share_recipe),s(R.string.chat_share_hint),R.drawable.ic_recipe_simple) {
            dialog.dismiss(); recipePicker.launch(Intent(this,RecipePickerActivity::class.java))
        })
    }
    private fun chooseMeal(kind: String) {
        val (dialog,root)=sheet(s(if(kind=="order") R.string.chat_share_order else R.string.chat_share_prep))
        val body=Ui.box(this,0);root.addView(body)
        fun load() { lifecycleScope.launch {
            body.removeAllViews();body.addView(Ui.text(this@GroupChatActivity,s(R.string.chat_loading)))
            try {
                val kitchens=Ui.data(ApiClient.getService().getKitchens())
                val meals=kitchens.flatMap { kitchen -> Ui.data(ApiClient.getService().meals(kitchen.id!!)) }
                    .filter { it.ownerId==userId && it.kind==kind }.distinctBy { it.id }
                if(!dialog.isShowing) return@launch
                body.removeAllViews()
                if(meals.isEmpty()) {
                    dialog.dismiss()
                    val kitchen=kitchens.firstOrNull { it.groupId==groupId } ?: kitchens.firstOrNull { it.id==PreferenceManager.currentKitchenId }
                    startActivity(Intent(this@GroupChatActivity,com.familyrecipes.android.ui.meal.MealEditorActivity::class.java)
                        .putExtra("kind",kind).putExtra("kitchen_id",kitchen?.id?:0L))
                    return@launch
                }
                meals.forEach { meal ->
                    val subtitle=listOfNotNull(kitchens.find{it.id==meal.kitchenId}?.name,meal.mealDate,meal.meal?.let{UiStrings.domain(it)})
                        .filter{it.isNotBlank()}.joinToString(" · ")
                    body.addView(Ui.entry(this@GroupChatActivity,meal.title,subtitle,R.drawable.ic_ui_order) {
                        confirmShare(meal.title) {
                            val prefix=if(kind=="order") "点菜" else "备菜"
                            val content="$prefix：${meal.title}\n${meal.dishes.joinToString("、"){it.name}}\nfamilyrecipes://meal/${meal.id}"
                            ChatRepository.enqueue(account,groupId,content,"meal_share",meal.id)
                            dialog.dismiss()
                        }
                    })
                }
            } catch(e:CancellationException) { throw e }
              catch(_:Exception) { body.removeAllViews();body.addView(Ui.button(this@GroupChatActivity,s(R.string.chat_failed_load)){load()}) }
        } }
        load()
    }
    private fun confirmShare(title: String, send: suspend () -> Unit) {
        AlertDialog.Builder(this).setTitle(R.string.chat_send_confirm).setMessage("$title\n\n${s(R.string.chat_share_hint)}")
            .setNegativeButton(R.string.chat_cancel,null).setPositiveButton(R.string.chat_send) { _,_ -> lifecycleScope.launch {
                try { send(); requestSync() }
                catch(e:CancellationException) { throw e }
                catch(e:Exception) { Ui.toast(this@GroupChatActivity,e) }
            } }.show()
    }
    private fun pendingAction(message: Message) {
        val clientId=message.clientMessageId ?: return
        val actions=arrayOf(s(R.string.chat_retry_action),s(R.string.chat_remove_pending))
        AlertDialog.Builder(this).setTitle(if(message.localError == "network") s(R.string.chat_offline) else message.localError ?: s(R.string.chat_pending))
            .setItems(actions) { _,which -> lifecycleScope.launch {
                when(which) {
                    0 -> requestSync()
                    1 -> ChatRepository.removePending(account,clientId)
                }
            } }.show()
    }
    private fun dp(n:Int)=(resources.displayMetrics.density*n).toInt()
    private fun toast(text:String) { Toast.makeText(this,text,Toast.LENGTH_SHORT).show() }
}
