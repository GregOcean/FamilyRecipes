package com.familyrecipes.android.ui.fridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyrecipes.android.data.model.FridgeItem
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.databinding.FragmentFridgeBinding
import com.familyrecipes.android.ui.adapter.FridgeAdapter
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

/**
 * 冰箱管理Fragment
 */
class FridgeFragment : Fragment() {

    private var _binding: FragmentFridgeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var fridgeAdapter: FridgeAdapter
    private val items = mutableListOf<FridgeItem>()
    
    private var currentTab = TAB_CURRENT  // 当前选中的Tab
    
    companion object {
        private const val TAB_CURRENT = 0    // 当前库存
        private const val TAB_CONSUMED = 1   // 消耗历史
    }
    
    // 添加食材结果监听
    private val addIngredientLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadData()
        }
    }

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
        setupTabLayout()
        setupListeners()
        loadData()
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
    
    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: TAB_CURRENT
                loadData()
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }
        
        binding.fabAdd.setOnClickListener {
            val intent = Intent(requireContext(), AddIngredientActivity::class.java)
            addIngredientLauncher.launch(intent)
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
    
    private fun loadData() {
        when (currentTab) {
            TAB_CURRENT -> loadFridgeItems()
            TAB_CONSUMED -> loadConsumedItems()
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
                Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    private fun loadConsumedItems() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().getConsumedItems()
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    items.clear()
                    response.body()?.data?.let { items.addAll(it) }
                    fridgeAdapter.notifyDataSetChanged()
                    
                    if (items.isEmpty()) {
                        Toast.makeText(context, "暂无消耗记录", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, "已标记为用完", Toast.LENGTH_SHORT).show()
                    loadData()
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
                    loadData()
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

