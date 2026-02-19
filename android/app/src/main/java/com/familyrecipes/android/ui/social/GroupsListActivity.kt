package com.familyrecipes.android.ui.social

import android.content.Intent
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
import com.familyrecipes.android.data.model.GroupChat
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.databinding.ActivityGroupsListBinding
import kotlinx.coroutines.launch

/**
 * 群组列表Activity
 */
class GroupsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupsListBinding
    private val groups = mutableListOf<GroupChat>()
    private lateinit var adapter: GroupsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        loadGroups()
    }

    override fun onResume() {
        super.onResume()
        // 每次回到页面时刷新列表
        loadGroups()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = GroupsAdapter(groups) { group ->
            // 点击群组，跳转到群聊界面
            val intent = Intent(this, GroupChatActivity::class.java).apply {
                putExtra(GroupChatActivity.EXTRA_GROUP_ID, group.id)
                putExtra(GroupChatActivity.EXTRA_GROUP_NAME, group.name)
                putExtra(GroupChatActivity.EXTRA_MEMBER_COUNT, group.memberCount ?: 0)
            }
            startActivity(intent)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabCreateGroup.setOnClickListener {
            // 跳转到创建群组页面
            val intent = Intent(this, CreateGroupActivity::class.java)
            startActivity(intent)
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            loadGroups()
        }
    }

    private fun loadGroups() {
        lifecycleScope.launch {
            try {
                binding.swipeRefresh.isRefreshing = true
                val response = ApiClient.getService().getGroupsList()

                if (response.isSuccessful && response.body()?.code == 200) {
                    groups.clear()
                    response.body()?.data?.let { groups.addAll(it) }
                    adapter.notifyDataSetChanged()
                    
                    // 显示/隐藏空状态
                    if (groups.isEmpty()) {
                        binding.recyclerView.visibility = View.GONE
                        binding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        binding.recyclerView.visibility = View.VISIBLE
                        binding.tvEmpty.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(this@GroupsListActivity, "加载失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@GroupsListActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
}

/**
 * 群组列表适配器
 */
class GroupsAdapter(
    private val groups: List<GroupChat>,
    private val onItemClick: (GroupChat) -> Unit
) : RecyclerView.Adapter<GroupsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.iv_avatar)
        val tvName: TextView = view.findViewById(R.id.tv_name)
        val tvDescription: TextView = view.findViewById(R.id.tv_description)
        val tvMemberCount: TextView = view.findViewById(R.id.tv_member_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val group = groups[position]
        
        holder.tvName.text = group.name
        holder.tvDescription.text = group.description ?: "暂无简介"
        holder.tvMemberCount.text = "${group.memberCount ?: 0}人"
        
        // 设置默认头像
        holder.ivAvatar.setImageResource(R.drawable.ic_groups)
        
        holder.itemView.setOnClickListener {
            onItemClick(group)
        }
    }

    override fun getItemCount() = groups.size
}

