package com.familyrecipes.android.ui.message

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val root = Ui.box(requireContext())
        search = Ui.field(requireContext(), "搜索群聊")
        root.addView(search)
        val heading=Ui.row(requireContext())
        heading.addView(Ui.text(requireContext(),"消息",23f,true),LinearLayout.LayoutParams(0,-2,1f))
        heading.addView(Ui.button(requireContext(),"发起群聊"){startActivity(Intent(requireContext(),CreateGroupActivity::class.java))})
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
                try {
                    groups = Ui.data(ApiClient.getService().getGroupsList())
                    draw()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    list.removeAllViews()
                    list.addView(Ui.button(requireContext(), "${e.message} · 重试") { load() })
                }
            }
    }

    private fun draw() {
        list.removeAllViews()
        groups
            .filter { it.name.contains(search.text.toString(), true) }
            .forEach { g ->
                val card=Ui.entry(requireContext(),g.name,
                    (if((g.unreadCount?:0)>0) "${g.unreadCount} 条未读 · " else "")+(g.lastMessage?.content ?: "${g.memberCount?:1} 位成员 · 一起讨论吃什么"),com.familyrecipes.android.R.drawable.ic_ui_users) {
                    startActivity(Intent(requireContext(),GroupChatActivity::class.java).putExtra("group_id",g.id).putExtra("group_name",g.name).putExtra("member_count",g.memberCount?:1))
                }
                list.addView(card)
            }
        if (list.childCount == 0)
            list.addView(Ui.text(requireContext(), "暂时没有群聊消息。一个人也能使用厨房、点菜和备菜。"))
        list.addView(Ui.entry(requireContext(),"食材提醒","查看临期食材，及时安排",com.familyrecipes.android.R.drawable.ic_clock_filled){(activity as? com.familyrecipes.android.ui.MainActivity)?.showPage("stock")})

    }

    override fun onDestroyView() {
        job?.cancel()
        super.onDestroyView()
    }
}
