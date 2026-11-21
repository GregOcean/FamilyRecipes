package com.familyrecipes.android.ui.message

import com.familyrecipes.android.ui.design.UiStrings
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.familyrecipes.android.data.local.*
import com.familyrecipes.android.R
import com.familyrecipes.android.data.model.GroupChat
import com.familyrecipes.android.data.model.Message
import com.familyrecipes.android.data.model.ChatContent
import com.google.gson.Gson
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.design.Ui
import com.familyrecipes.android.ui.social.*
import kotlinx.coroutines.*

class MessageFragment : Fragment() {
    private lateinit var list: LinearLayout
    private lateinit var search: EditText
    private var groups = listOf<GroupChat>()
    private var job: Job? = null
    private var offline = false
    private var invitationCount = 0
    private var favoriteCount = 0
    private var searchJob: Job? = null
    private var hits = listOf<Message>()
    private var searching = false
    private var cachedOnly = false
    private var more = false


    override fun onViewCreated(view:View,state:Bundle?) {
        super.onViewCreated(view,state)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val account=LocalStore.account
                launch { GroupCache.observe(account).collect { fresh -> if(account==LocalStore.account){groups=fresh;draw()} } }
                kotlinx.coroutines.flow.combine(LocalStore.instance.records().observe(account,"friend-invitations"),LocalStore.instance.records().observe(account,"favorite-notifications")){a,b->a to b}.collect {
                    if(account==LocalStore.account) {
                        favoriteCount=FavoriteNotifications.cached(account).unread
                        invitationCount=FriendInvitations.incoming(FriendInvitations.cached(account),PreferenceManager.userId).size
                        draw()
                    }
                }
            }
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val root = Ui.box(requireContext())
        search = Ui.field(requireContext(), getString(R.string.chat_search_hint))
        root.addView(search)
        val heading=Ui.row(requireContext())
        heading.addView(Ui.text(requireContext(),UiStrings.s(com.familyrecipes.android.R.string.ui_ff692f04ac),23f,true),LinearLayout.LayoutParams(0,-2,1f))
        heading.addView(Ui.button(requireContext(),UiStrings.s(com.familyrecipes.android.R.string.ui_634926c4ca)){startActivity(Intent(requireContext(),com.familyrecipes.android.ui.settings.ManagementActivity::class.java).putExtra("groups",true))})
        root.addView(heading)
        list = Ui.box(requireContext(), 0)
        root.addView(Ui.scroll(requireContext(), list), LinearLayout.LayoutParams(-1, 0, 1f))
        search.filters = arrayOf(android.text.InputFilter.LengthFilter(100))
        search.doAfterTextChanged { findMessages() }
        return root
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) load()
    }

    private fun load() {
        if (!::list.isInitialized) return
        job?.cancel()
        job =
            viewLifecycleOwner.lifecycleScope.launch {
                val account = LocalStore.account
                groups = GroupCache.list(account)
                offline = false
                draw()
                try {
                    try { FavoriteNotifications.sync() } catch(e:CancellationException){throw e} catch(_:Exception){}
                    favoriteCount=FavoriteNotifications.cached(account).unread
                    val fresh = Ui.data(ApiClient.getAccountService(account).getGroupsList())
                    if (account != LocalStore.account) return@launch
                    GroupCache.save(account, fresh)
                    groups = fresh
                    draw()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (account != LocalStore.account) return@launch
                    offline = true
                    draw()
                }
            }
    }

    private fun draw() {
        if (!isAdded || !::list.isInitialized) return
        list.removeAllViews()
        if(search.text.toString().isNotBlank()) {
            list.addView(Ui.text(requireContext(),getString(if(searching) R.string.chat_search_loading else if(cachedOnly) R.string.chat_search_cached else R.string.chat_search_results),14f))
            hits.forEach { message ->
                val group=groups.firstOrNull { it.id==message.groupId }
                list.addView(Ui.entry(requireContext(),group?.name ?: getString(R.string.chat_search_results),
                    "${message.sender?.username.orEmpty()} · ${com.familyrecipes.android.util.ChatTime.label(message.createdAt)}\n${ChatContent.plain(message.content)}",
                    R.drawable.ic_chat_filled,
                    requireContext().getColor(R.color.message_group_icon),requireContext().getColor(R.color.message_group_bg)) {
                    val account=LocalStore.account
                    viewLifecycleOwner.lifecycleScope.launch {
                        LocalStore.instance.records().put(LocalRecord(account,"chat:${message.groupId}",message.id.toString(),Gson().toJson(message)))
                        if(account==LocalStore.account) startActivity(Intent(requireContext(),GroupChatActivity::class.java)
                            .putExtra("group_id",message.groupId).putExtra("group_name",group?.name.orEmpty())
                            .putExtra("message_id",message.id ?: 0L))
                    }
                })
            }
            if(!searching && hits.isEmpty()) list.addView(Ui.text(requireContext(),getString(R.string.chat_search_empty)))
            if(!searching && more) list.addView(Ui.button(requireContext(),getString(R.string.chat_search_more)){findMessages(true)})
            if(!searching && cachedOnly) list.addView(Ui.button(requireContext(),getString(R.string.chat_search_retry)){findMessages()})
            return
        }
        list.addView(Ui.text(requireContext(),getString(R.string.messages_groups),16f,true))
        if (offline) list.addView(Ui.button(requireContext(), getString(
            if(groups.isEmpty()) R.string.groups_offline_empty else R.string.groups_offline_cached)) { load() })
        groups
            .sortedByDescending { it.id == PreferenceManager.currentGroupId }
            .forEach { g ->
                val card=Ui.entry(requireContext(),g.name,
                    (if((g.unreadCount?:0)>0) UiStrings.f(com.familyrecipes.android.R.string.unread_count, g.unreadCount) else "")+(g.lastMessage?.let { if(it.messageType=="sticker") UiStrings.s(com.familyrecipes.android.R.string.chat_sticker) else if(it.messageType=="system") com.familyrecipes.android.ui.social.ChatSystemText.display(requireContext(),it.content) else com.familyrecipes.android.data.model.ChatContent.plain(it.content).substringBefore("familyrecipes://") } ?: resources.getQuantityString(R.plurals.social_group_members_preview,g.memberCount?:1,g.memberCount?:1)),com.familyrecipes.android.R.drawable.ic_ui_users,
                    requireContext().getColor(R.color.message_group_icon),requireContext().getColor(R.color.message_group_bg)) {
                    startActivity(Intent(requireContext(),GroupChatActivity::class.java).putExtra("group_id",g.id).putExtra("group_name",g.name).putExtra("member_count",g.memberCount?:1))
                }
                list.addView(card)
            }
        if (groups.isEmpty() && !offline)
            list.addView(Ui.text(requireContext(), UiStrings.s(com.familyrecipes.android.R.string.ui_315e99b47a)))

        list.addView(Ui.text(requireContext(),getString(R.string.messages_alerts),16f,true))
        list.addView(Ui.entry(requireContext(),getString(R.string.favorite_alerts),
            getString(if(favoriteCount>0)R.string.favorite_alert_count else R.string.favorite_alert_empty,favoriteCount),R.drawable.ic_book_outline,
            Ui.blue,Ui.warm) {startActivity(Intent(requireContext(),FavoriteNotificationsActivity::class.java))})
        list.addView(Ui.entry(requireContext(),getString(R.string.friends_title),
            if(invitationCount>0)getString(R.string.friend_incoming_count,invitationCount) else getString(R.string.friend_entry_hint),R.drawable.ic_friends,
            requireContext().getColor(R.color.message_friend_icon),requireContext().getColor(R.color.message_friend_bg)) {
            startActivity(Intent(requireContext(),FriendsListActivity::class.java))
        })
        list.addView(Ui.entry(requireContext(),UiStrings.s(com.familyrecipes.android.R.string.ui_19268a99cb),UiStrings.s(com.familyrecipes.android.R.string.ui_7bd7075cda),com.familyrecipes.android.R.drawable.ic_clock_filled,requireContext().getColor(R.color.message_reminder_icon),requireContext().getColor(R.color.message_reminder_bg)){(activity as? com.familyrecipes.android.ui.MainActivity)?.showPage("stock")})

    }

    private fun findMessages(append:Boolean=false) {
        searchJob?.cancel()
        val query=search.text.toString().trim()
        if(query.isEmpty()) { hits=emptyList();searching=false;draw();return }
        val account=LocalStore.account
        val before=if(append) hits.lastOrNull()?.id ?: Long.MAX_VALUE else Long.MAX_VALUE
        if(!append) hits=emptyList()
        searching=true;more=false;cachedOnly=false;draw()
        searchJob=viewLifecycleOwner.lifecycleScope.launch {
            delay(300)
            try {
                val page=Ui.data(ApiClient.getAccountService(account).searchMessages(query,before))
                if(account!=LocalStore.account) return@launch
                hits=if(append) hits+page.take(50) else page.take(50)
                more=page.size>50
            } catch(e:CancellationException) { throw e }
              catch(e:Exception) {
                if(account!=LocalStore.account) return@launch
                val cached=GroupCache.list(account).flatMap { group ->
                    LocalStore.instance.records().list(account,"chat:${group.id}").mapNotNull {
                        runCatching { Gson().fromJson(it.json,Message::class.java) }.getOrNull()
                    }
                }.filter { it.messageType=="text" && ChatContent.plain(it.content).contains(query,true) }
                    .sortedByDescending { it.id }
                hits=cached;cachedOnly=true;more=false
            }
            if(account==LocalStore.account) { searching=false;draw() }
        }
    }

    override fun onDestroyView() {
        job?.cancel()
        searchJob?.cancel()
        super.onDestroyView()
    }
}
