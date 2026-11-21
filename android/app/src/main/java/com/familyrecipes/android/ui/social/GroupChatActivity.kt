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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody

/** Durable REST delivery; WebSocket only wakes the cursor-based receiver. */
class GroupChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGroupChatBinding
    private var searchTargetLocated = false
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
    private var translationJob:Job?=null
    private var readJob:Job?=null
    private val window=MutableStateFlow(100)
    private val beforeWindow=MutableStateFlow(Long.MAX_VALUE)
    private var loadingOlder=false
    private var historyEnd=false
    private var blockedStatus:Int?=null
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
                    val url = Ui.data(ApiClient.getAccountService(account).uploadImage(body, "sticker"))
                    ChatRepository.enqueue(account, groupId, url, "sticker")
                    requestSync()
                } finally { file.delete() }
            } catch(e: CancellationException) { throw e }
              catch(e: Exception) { Ui.toast(this@GroupChatActivity, e) }
        }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        if(!PreferenceManager.isLoggedIn() || intent.getStringExtra("notification_account")?.let{it!=account}==true){
            toast(s(R.string.social_account_changed));finish();return
        }
        binding = ActivityGroupChatBinding.inflate(layoutInflater); setContentView(binding.root)
        groupId = intent.getLongExtra(EXTRA_GROUP_ID, 0)
        val searchTarget=intent.getLongExtra("message_id",0)
        if(searchTarget>0){beforeWindow.value=if(searchTarget==Long.MAX_VALUE)searchTarget else searchTarget+1
            lifecycleScope.launch{try{ChatRepository.loadOlder(account,groupId,beforeWindow.value)}catch(e:CancellationException){throw e}catch(_:Exception){}}
        }
        if(groupId <= 0) { finish(); return }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.tvGroupName.text = intent.getStringExtra(EXTRA_GROUP_NAME)
        fun manage(){startActivity(Intent(this,com.familyrecipes.android.ui.settings.GroupSettingsActivity::class.java).putExtra("group_id",groupId))}
        binding.btnMembers.setOnClickListener { manage() }
        binding.tvGroupName.setOnClickListener { manage() }
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
        binding.tvSync.setOnClickListener {
            when(blockedStatus){401->{startActivity(Intent(this,com.familyrecipes.android.ui.MainActivity::class.java).putExtra("navigate_to","profile").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));finish()};403,404->finish();else->requestSync()}
        }
        binding.btnNewMessages.setOnClickListener{
            if(beforeWindow.value!=Long.MAX_VALUE){beforeWindow.value=Long.MAX_VALUE;window.value=100;searchTargetLocated=true}
            if(messages.isNotEmpty())binding.rvMessages.smoothScrollToPosition(messages.lastIndex)
        }
        adapter = MessageAdapter(messages, userId, ::pendingAction)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvMessages.adapter = adapter
        binding.rvMessages.addOnScrollListener(object:androidx.recyclerview.widget.RecyclerView.OnScrollListener(){
            override fun onScrolled(view:androidx.recyclerview.widget.RecyclerView,dx:Int,dy:Int){
                markVisibleRead()
                val layout=view.layoutManager as LinearLayoutManager
                if(dy<0 && layout.findFirstVisibleItemPosition()<3)loadOlder()
                if(!view.canScrollVertically(1))binding.btnNewMessages.visibility=if(beforeWindow.value==Long.MAX_VALUE)View.GONE else View.VISIBLE
            }
        })
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
                    combine(combine(window,beforeWindow){limit,before->limit to before}.flatMapLatest{(limit,before)->dao.observeBefore(account,"chat:$groupId",before,limit)}, dao.observe(account, "outbox")) { rows, pending ->
                        rows.map { json.fromJson(it.json, Message::class.java) }.sortedBy { it.id } to
                            pending.map { json.fromJson(it.json, PendingMessage::class.java) }.filter { it.groupId == groupId }.sortedBy{it.sequence}
                    }.collect { (history, pending) ->
                        val layout=binding.rvMessages.layoutManager as LinearLayoutManager
                        val first=layout.findFirstVisibleItemPosition()
                        val anchorId=messages.getOrNull(first)?.id
                        val anchorTop=layout.findViewByPosition(first)?.top ?: 0
                        val atBottom = !binding.rvMessages.canScrollVertically(1)
                        val previousTail=messages.lastOrNull()?.let { it.clientMessageId ?: it.id?.toString() }
                        val knownIds = history.mapNotNull { it.clientMessageId }.toSet()
                        messages.clear(); messages.addAll(history)
                        pending.filter { it.clientId !in knownIds }.forEach { item ->
                            messages.add(Message(id=null, groupId=groupId, senderId=userId,
                                content=item.content, messageType=if(item.type == "sticker") "sticker" else "text",
                                createdAt=null, sender=null, clientMessageId=item.clientId, localError=when(item.state){"auth"->s(R.string.social_login_expired);"unavailable"->s(R.string.social_group_unavailable);"retrying"->s(R.string.social_retry_later);else->item.error}))
                        }
                        adapter.notifyDataSetChanged()
                        val tail=messages.lastOrNull()
                        val sentNew=tail?.senderId==userId && (tail.clientMessageId ?: tail.id?.toString())!=previousTail
                        val target=intent.getLongExtra("message_id",0L)
                        val targetIndex=messages.indexOfFirst { it.id==target }
                        if(!searchTargetLocated && target>0 && targetIndex>=0) {
                            binding.rvMessages.scrollToPosition(targetIndex)
                            searchTargetLocated=true
                        } else if(!atBottom && anchorId!=null && !sentNew) {
                            val anchorIndex=messages.indexOfFirst { it.id==anchorId }
                            if(anchorIndex>=0) layout.scrollToPositionWithOffset(anchorIndex,anchorTop)
                        } else if((atBottom || sentNew) && messages.isNotEmpty()) binding.rvMessages.scrollToPosition(messages.lastIndex)
                        if(!atBottom && !sentNew && tail?.senderId!=userId && (tail?.clientMessageId?:tail?.id?.toString())!=previousTail)
                            binding.btnNewMessages.visibility=View.VISIBLE
                        binding.rvMessages.post{markVisibleRead()}
                    }
                }
                launch { while(true) { requestSync(); delay(10000) } }
                launch { ws.messageFlow.collect { if(it.groupId == groupId) requestSync() } }
            }
        }
    }
    override fun onStart() { super.onStart();if(!::binding.isInitialized)return
        if(account!=LocalStore.account || !PreferenceManager.isLoggedIn()){finish();return}
        visibleGroup=groupId;ChatNotifications.clearGroup(account,groupId);ws.connect(groupId,userId)
    }
    override fun onStop() {
        readJob?.cancel()
        if(visibleGroup==groupId){visibleGroup=null;ws.disconnect()}
        super.onStop()
    }
    private fun requestSync() {
        if(blockedStatus!=null || syncJob?.isActive == true) return
        syncJob = lifecycleScope.launch {
            try {
                ChatRepository.sync(account, groupId)
                binding.tvSync.visibility = View.GONE
                if(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))ws.connect(groupId,userId)
                if(translationJob?.isActive!=true)translationJob=lifecycleScope.launch{ChatRepository.translateRecent(account,groupId)}
            } catch(e: CancellationException) { throw e }
              catch(e: Exception) {
                blockedStatus=(e as? ChatSyncException)?.status?.takeIf{it in listOf(401,403,404)}
                binding.tvSync.text = s(when(blockedStatus){401->R.string.social_login_expired;403,404->R.string.social_group_unavailable;else->R.string.chat_offline})
                binding.tvSync.visibility = View.VISIBLE
                if(blockedStatus!=null)binding.btnSend.isEnabled=false
                android.util.Log.w("GroupChat", "Sync failed: ${e.javaClass.simpleName}")
            }
        }
    }
    private fun markVisibleRead(){
        if(!::binding.isInitialized || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || account!=LocalStore.account)return
        val layout=binding.rvMessages.layoutManager as LinearLayoutManager
        val last=layout.findLastVisibleItemPosition()
        if(last<0)return
        val id=messages.take(last+1).mapNotNull{it.id}.maxOrNull()?:return
        readJob?.cancel();readJob=lifecycleScope.launch{
            delay(300)
            if(!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))return@launch
            try{ChatRepository.markRead(account,groupId,id)}catch(e:CancellationException){throw e}catch(_:Exception){}
        }
    }
    private fun loadOlder(){
        if(loadingOlder || historyEnd || messages.isEmpty())return
        val oldest=messages.mapNotNull{it.id}.minOrNull()?:return
        loadingOlder=true
        // Reveal already cached pages even if the following network fetch is offline.
        window.value+=50
        lifecycleScope.launch{try{
            val count=ChatRepository.loadOlder(account,groupId,oldest)
            historyEnd=count<50
        }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@GroupChatActivity,e)}finally{loadingOlder=false}}
    }
    private suspend fun loadMembers(): Boolean {
        GroupCache.detail(account,groupId)?.let { cached ->
            members=cached.members.orEmpty()
            adapter.members=members
            adapter.notifyDataSetChanged()
        }
        return try {
            val group = Ui.data(ApiClient.getAccountService(account).getGroupDetail(groupId))
            if(account!=LocalStore.account) return false
            GroupCache.saveDetail(account,group)
            members = group.members.orEmpty()
            binding.tvGroupName.text = resources.getQuantityString(R.plurals.social_chat_group_count,members.size,group.name,members.size)
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
            }.apply { textSize=28f; contentDescription=face; minHeight=dp(48);insetTop=0;insetBottom=0
                backgroundTintList=android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            }, LinearLayout.LayoutParams(0,dp(48),1f)) }
            root.addView(row)
        }
        root.addView(Ui.button(this,s(R.string.chat_upload_sticker)) { hideEmoji(); stickerPicker.launch("image/*") })
        binding.emojiPanel.visibility=View.VISIBLE
        binding.btnSticker.isSelected=true
    }
    private fun showShare() {
        val (dialog, root)=sheet(s(R.string.chat_share))
        val actions=listOf(
            Triple(s(R.string.chat_share_order),R.drawable.ic_ui_order,{dialog.dismiss();chooseMeal("order")}),
            Triple(s(R.string.chat_share_prep),R.drawable.ic_ui_prep,{dialog.dismiss();chooseMeal("prep")}),
            Triple(s(R.string.chat_share_recipe),R.drawable.ic_recipe_simple,{dialog.dismiss();recipePicker.launch(Intent(this,RecipePickerActivity::class.java))}),
            Triple(s(R.string.chat_mention),R.drawable.ic_ui_users,{dialog.dismiss();showMembers(true)})
        )
        actions.chunked(2).forEach{pair->val row=Ui.row(this);pair.forEach{(title,icon,action)->
            val tint=when(icon){R.drawable.ic_ui_order->Ui.warm;R.drawable.ic_ui_prep->Ui.green;R.drawable.ic_ui_users->Ui.purple;else->Ui.pale}
            row.addView(Ui.tile(this,title,icon,tint,action),LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(dp(4),dp(4),dp(4),dp(4))})};root.addView(row)}
    }
    private fun chooseMeal(kind: String) {
        val (dialog,root)=sheet(s(if(kind=="order") R.string.chat_share_order else R.string.chat_share_prep))
        val body=Ui.box(this,0);root.addView(body)
        fun load() { lifecycleScope.launch {
            body.removeAllViews();body.addView(Ui.text(this@GroupChatActivity,s(R.string.chat_loading)))
            try {
                val kitchens=Ui.data(ApiClient.getAccountService(account).getKitchens())
                val meals=kitchens.flatMap { kitchen -> Ui.data(ApiClient.getAccountService(account).meals(kitchen.id!!)) }
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
        val clientId=message.clientMessageId?:return
        lifecycleScope.launch {
            val pending=LocalStore.instance.records().get(account,"outbox",clientId)?.let{json.fromJson(it,PendingMessage::class.java)}?:return@launch
            val queue=DeliveryPolicy.ordered(LocalStore.instance.records().list(account,"outbox").map{json.fromJson(it.json,PendingMessage::class.java)}.filter{it.groupId==groupId})
            val blockedByEarlier=queue.takeWhile{it.clientId!=clientId}.any{it.state in setOf("failed","auth","unavailable")}
            val actions=mutableListOf<Pair<String,()->Unit>>()
            if(pending.state !in listOf("auth","unavailable"))actions.add(s(R.string.social_retry_one) to {
                lifecycleScope.launch{try{ChatRepository.retryPending(account,groupId,clientId);requestSync()}
                    catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@GroupChatActivity,e)}}
            })
            if(pending.type=="text" && pending.errorKind=="invalid")actions.add(s(R.string.social_edit_failed) to {
                val input=Ui.field(this@GroupChatActivity,s(R.string.ui_17e8e26968),ChatMentions.decode(pending.content).toString(),true)
                val edit=AlertDialog.Builder(this@GroupChatActivity).setTitle(R.string.social_edit_failed).setView(input)
                    .setNegativeButton(R.string.social_cancel,null).setPositiveButton(R.string.social_send,null).create()
                edit.setOnShowListener{edit.getButton(-1).setOnClickListener{
                    val content=input.text.toString().trim()
                    if(content.isBlank() || content.length>4000){input.error=s(R.string.chat_length);return@setOnClickListener}
                    edit.getButton(-1).isEnabled=false
                    lifecycleScope.launch{try{ChatRepository.editFailed(account,groupId,clientId,content);edit.dismiss();requestSync()}
                        catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@GroupChatActivity,e);edit.getButton(-1).isEnabled=true}}
                }};edit.show()
            })
            actions.add(s(R.string.chat_remove_pending) to {lifecycleScope.launch{try{ChatRepository.removePending(account,clientId);requestSync()}
                catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@GroupChatActivity,e)}}})
            AlertDialog.Builder(this@GroupChatActivity).setTitle(if(blockedByEarlier)s(R.string.social_retry_queued) else if(message.localError=="network")s(R.string.chat_offline) else message.localError?:s(R.string.chat_pending))
                .setItems(actions.map{it.first}.toTypedArray()){_,which->actions[which].second()}.show()
        }
    }
    private fun dp(n:Int)=(resources.displayMetrics.density*n).toInt()
    private fun toast(text:String) { Toast.makeText(this,text,Toast.LENGTH_SHORT).show() }
}
