package com.familyrecipes.android.ui.social

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyrecipes.android.data.model.GroupChat
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.databinding.ActivityGroupsListBinding
import kotlinx.coroutines.launch

/**
 * 我创建的群列表Activity
 */
class CreatedGroupsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupsListBinding
    private val groups = mutableListOf<GroupChat>()
    private lateinit var adapter: GroupsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 修改标题
        binding.toolbar.title = "我创建的群"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        setupRecyclerView()
        setupListeners()
        loadGroups()
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            loadGroups()
        }
        
        // 创建群组按钮
        binding.fabCreateGroup.setOnClickListener {
            val intent = Intent(this, CreateGroupActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadGroups()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = GroupsAdapter(groups) { group ->
            // 点击群组，跳转到群聊界面
            val intent = Intent(this, GroupChatActivity::class.java).apply {
                putExtra(GroupChatActivity.EXTRA_GROUP_ID, group.id)
                putExtra(GroupChatActivity.EXTRA_GROUP_NAME, group.name)
                putExtra(GroupChatActivity.EXTRA_MEMBER_COUNT, group.memberCount ?: 0)
            }
            startActivity(intent)
        }
        binding.recyclerView.adapter = adapter
    }

    private fun loadGroups() {
        lifecycleScope.launch {
            try {
                binding.swipeRefresh.isRefreshing = true
                val response = ApiClient.getService().getCreatedGroups()

                if (response.isSuccessful && response.body()?.code == 200) {
                    groups.clear()
                    response.body()?.data?.let { groups.addAll(it) }
                    adapter.notifyDataSetChanged()

                    if (groups.isEmpty()) {
                        binding.recyclerView.visibility = View.GONE
                        binding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        binding.recyclerView.visibility = View.VISIBLE
                        binding.tvEmpty.visibility = View.GONE
                    }
                } else {
                    android.widget.Toast.makeText(this@CreatedGroupsActivity, "加载失败", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@CreatedGroupsActivity, "网络错误: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
}
