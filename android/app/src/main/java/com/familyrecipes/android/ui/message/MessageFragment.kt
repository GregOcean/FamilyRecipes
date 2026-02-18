package com.familyrecipes.android.ui.message

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyrecipes.android.data.model.GroupChat
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.databinding.FragmentMessageBinding
import com.familyrecipes.android.ui.social.GroupsListActivity
import kotlinx.coroutines.launch

/**
 * 消息Fragment - 显示群组列表和系统通知
 */
class MessageFragment : Fragment() {

    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!
    private val groups = mutableListOf<GroupChat>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupListeners()
        setupRecyclerView()
        loadGroups()
    }

    private fun setupListeners() {
        // 爱心点赞
        binding.layoutLikes.setOnClickListener {
            // TODO: 跳转到爱心点赞页面
            android.widget.Toast.makeText(context, "爱心点赞功能开发中", android.widget.Toast.LENGTH_SHORT).show()
        }

        // 新增粉丝
        binding.layoutFollowers.setOnClickListener {
            // TODO: 跳转到新增粉丝页面
            android.widget.Toast.makeText(context, "新增粉丝功能开发中", android.widget.Toast.LENGTH_SHORT).show()
        }

        // 我被需要
        binding.layoutNeeded.setOnClickListener {
            // TODO: 跳转到我被需要页面
            android.widget.Toast.makeText(context, "我被需要功能开发中", android.widget.Toast.LENGTH_SHORT).show()
        }

        // 查看所有群组
        binding.tvViewAll.setOnClickListener {
            val intent = Intent(requireContext(), GroupsListActivity::class.java)
            startActivity(intent)
        }

        // 刷新
        binding.swipeRefresh.setOnRefreshListener {
            loadGroups()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        // TODO: 设置群组adapter
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
                    android.util.Log.d("MessageFragment", "加载成功，共${groups.size}个群组")
                }
            } catch (e: Exception) {
                android.util.Log.e("MessageFragment", "加载群组失败", e)
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadGroups()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

