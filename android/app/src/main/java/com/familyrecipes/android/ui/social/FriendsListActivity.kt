package com.familyrecipes.android.ui.social

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyrecipes.android.data.model.Friendship
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.databinding.ActivityFriendsListBinding
import kotlinx.coroutines.launch

/**
 * 好友列表Activity
 */
class FriendsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFriendsListBinding
    private val friends = mutableListOf<Friendship>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFriendsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadFriends()
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

    private fun loadFriends() {
        lifecycleScope.launch {
            try {
                binding.swipeRefresh.isRefreshing = true
                val response = ApiClient.getService().getFriendsList()

                if (response.isSuccessful && response.body()?.code == 200) {
                    friends.clear()
                    response.body()?.data?.let { friends.addAll(it) }
                    // TODO: 通知adapter刷新
                    Toast.makeText(this@FriendsListActivity, "加载成功，共${friends.size}个好友", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@FriendsListActivity, "加载失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FriendsListActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
}

