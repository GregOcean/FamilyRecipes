package com.familyrecipes.android.ui.fridge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyrecipes.android.data.model.FridgeItem
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.databinding.FragmentFridgeBinding
import com.familyrecipes.android.ui.adapter.FridgeAdapter
import kotlinx.coroutines.launch

/**
 * 冰箱管理Fragment
 */
class FridgeFragment : Fragment() {

    private var _binding: FragmentFridgeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var fridgeAdapter: FridgeAdapter
    private val items = mutableListOf<FridgeItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFridgeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupListeners()
        loadFridgeItems()
    }

    private fun setupRecyclerView() {
        fridgeAdapter = FridgeAdapter(items, 
            onConsumeClick = { item ->
                markAsConsumed(item.id!!)
            },
            onDeleteClick = { item ->
                deleteItem(item.id!!)
            },
            onItemClick = { item ->
                // 点击食材，跳转到相关菜谱列表
                navigateToRecipesByIngredient(item)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = fridgeAdapter
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            loadFridgeItems()
        }
        
        binding.fabAdd.setOnClickListener {
            // TODO: 添加食材对话框
            Toast.makeText(context, "添加食材功能开发中", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun navigateToRecipesByIngredient(item: FridgeItem) {
        item.ingredient?.let { ingredient ->
            // TODO: 跳转到菜谱列表页面，传递食材ID
            Toast.makeText(
                context, 
                "查看包含「${ingredient.name}」的菜谱（功能待实现）", 
                Toast.LENGTH_SHORT
            ).show()
            
            // 实际使用时：
            // val intent = Intent(requireContext(), RecipeListActivity::class.java)
            // intent.putExtra("ingredient_id", ingredient.id)
            // intent.putExtra("ingredient_name", ingredient.name)
            // startActivity(intent)
        }
    }

    private fun loadFridgeItems() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().getFridgeItems()
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    items.clear()
                    response.body()?.data?.let { items.addAll(it) }
                    fridgeAdapter.notifyDataSetChanged()
                    
                    // 检查即将过期的食材
                    checkExpiringItems()
                } else {
                    Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun checkExpiringItems() {
        val expiringCount = items.count { it.status == FridgeItem.STATUS_EXPIRING }
        val expiredCount = items.count { it.status == FridgeItem.STATUS_EXPIRED }
        
        if (expiringCount > 0 || expiredCount > 0) {
            val message = buildString {
                if (expiredCount > 0) append("${expiredCount}个食材已过期\n")
                if (expiringCount > 0) append("${expiringCount}个食材即将过期")
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun markAsConsumed(itemId: Long) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().markAsConsumed(itemId)
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(context, "已标记为消耗", Toast.LENGTH_SHORT).show()
                    loadFridgeItems()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "操作失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteItem(itemId: Long) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().deleteFridgeItem(itemId)
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                    loadFridgeItems()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "操作失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

