package com.familyrecipes.android.ui.social

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        loadGroups()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        // TODO: 设置adapter
    }

    private fun setupListeners() {
        binding.fabCreateGroup.setOnClickListener {
            // TODO: 打开创建群组对话框
            Toast.makeText(this, "创建群组功能开发中", Toast.LENGTH_SHORT).show()
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
                    // TODO: 通知adapter刷新
                    Toast.makeText(this@GroupsListActivity, "加载成功，共${groups.size}个群组", Toast.LENGTH_SHORT).show()
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

