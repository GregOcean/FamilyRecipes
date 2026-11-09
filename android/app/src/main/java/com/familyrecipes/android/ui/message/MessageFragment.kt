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
    private val permission=registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { draw() }

    override fun onViewCreated(view:View,state:Bundle?) {
        super.onViewCreated(view,state)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val account=LocalStore.account
                LocalStore.instance.records().observe(account,"friend-invitations").collect {
                    if(account==LocalStore.account) {
                        invitationCount=FriendInvitations.incoming(FriendInvitations.cached(account),PreferenceManager.userId).size
                        draw()
                    }
                }
            }
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val root = Ui.box(requireContext())
        search = Ui.field(requireContext(), UiStrings.s(com.familyrecipes.android.R.string.ui_da6878f718))
        root.addView(search)
        val heading=Ui.row(requireContext())
        heading.addView(Ui.text(requireContext(),UiStrings.s(com.familyrecipes.android.R.string.ui_ff692f04ac),23f,true),LinearLayout.LayoutParams(0,-2,1f))
        heading.addView(Ui.button(requireContext(),UiStrings.s(com.familyrecipes.android.R.string.ui_634926c4ca)){startActivity(Intent(requireContext(),com.familyrecipes.android.ui.settings.ManagementActivity::class.java).putExtra("groups",true))})
        root.addView(heading)
        list = Ui.box(requireContext(), 0)
        root.addView(Ui.scroll(requireContext(), list), LinearLayout.LayoutParams(-1, 0, 1f))
        search.doAfterTextChanged { draw() }
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
                    val fresh = Ui.data(ApiClient.getService().getGroupsList())
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
        list.removeAllViews()
        list.addView(Ui.entry(requireContext(),getString(R.string.friends_title),
            if(invitationCount>0)getString(R.string.friend_incoming_count,invitationCount) else getString(R.string.friend_entry_hint),R.drawable.ic_friends) {
            startActivity(Intent(requireContext(),FriendsListActivity::class.java))
        })
        if(!androidx.core.app.NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
            list.addView(Ui.button(requireContext(),getString(R.string.friend_notification_enable)) {
                val prefs=requireContext().getSharedPreferences("message-permission",0)
                if(android.os.Build.VERSION.SDK_INT>=33 && !prefs.getBoolean("asked",false)) {
                    prefs.edit().putBoolean("asked",true).apply()
                    permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,requireContext().packageName))
            })
        }
        if (offline) list.addView(Ui.button(requireContext(), getString(
            if(groups.isEmpty()) R.string.groups_offline_empty else R.string.groups_offline_cached)) { load() })
        groups
            .sortedByDescending { it.id == PreferenceManager.currentGroupId }
            .filter { it.name.contains(search.text.toString(), true) }
            .forEach { g ->
                val card=Ui.entry(requireContext(),g.name,
                    (if((g.unreadCount?:0)>0) UiStrings.f(com.familyrecipes.android.R.string.unread_count, g.unreadCount) else "")+(g.lastMessage?.let { if(it.messageType=="sticker") UiStrings.s(com.familyrecipes.android.R.string.chat_sticker) else com.familyrecipes.android.data.model.ChatContent.plain(it.content).substringBefore("familyrecipes://") } ?: UiStrings.f(com.familyrecipes.android.R.string.group_members_preview, g.memberCount?:1)),com.familyrecipes.android.R.drawable.ic_ui_users) {
                    startActivity(Intent(requireContext(),GroupChatActivity::class.java).putExtra("group_id",g.id).putExtra("group_name",g.name).putExtra("member_count",g.memberCount?:1))
                }
                list.addView(card)
            }
        if (groups.isEmpty() && !offline)
            list.addView(Ui.text(requireContext(), UiStrings.s(com.familyrecipes.android.R.string.ui_315e99b47a)))
        list.addView(Ui.entry(requireContext(),UiStrings.s(com.familyrecipes.android.R.string.ui_19268a99cb),UiStrings.s(com.familyrecipes.android.R.string.ui_7bd7075cda),com.familyrecipes.android.R.drawable.ic_clock_filled){(activity as? com.familyrecipes.android.ui.MainActivity)?.showPage("stock")})

    }

    override fun onDestroyView() {
        job?.cancel()
        super.onDestroyView()
    }
}
