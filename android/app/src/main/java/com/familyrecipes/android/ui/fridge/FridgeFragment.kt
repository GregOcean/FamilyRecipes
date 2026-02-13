package com.familyrecipes.android.ui.fridge

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.familyrecipes.android.data.model.FridgeItem
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.databinding.FragmentFridgeBinding
import com.familyrecipes.android.ui.SearchableFragment
import com.familyrecipes.android.ui.adapter.FridgeAdapter
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

/**
 * 冰箱管理Fragment
 */
class FridgeFragment : Fragment(), SearchableFragment {

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
        
        // 检查登录状态
        if (!com.familyrecipes.android.util.AuthUtil.isLoggedIn()) {
            showLoginPrompt()
            return
        }
        
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
        
        // 设置滑动手势
        setupSwipeGesture()
    }
    
    private fun setupSwipeGesture() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = items[position]
                
                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        // 左滑删除
                        deleteItem(item.id!!)
                    }
                    ItemTouchHelper.RIGHT -> {
                        // 右滑标记为已消耗
                        markAsConsumed(item.id!!)
                    }
                }
            }
            
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = Paint()
                
                if (dX > 0) {
                    // 右滑 - 已消耗（绿色背景）
                    paint.color = Color.parseColor("#4CAF50")
                    val background = RectF(
                        itemView.left.toFloat(),
                        itemView.top.toFloat(),
                        itemView.left + dX,
                        itemView.bottom.toFloat()
                    )
                    c.drawRect(background, paint)
                    
                    // 绘制文字
                    paint.color = Color.WHITE
                    paint.textSize = 48f
                    paint.textAlign = Paint.Align.LEFT
                    c.drawText(
                        "已消耗",
                        itemView.left + 40f,
                        itemView.top + (itemView.height / 2f) + 15f,
                        paint
                    )
                } else if (dX < 0) {
                    // 左滑 - 删除（红色背景）
                    paint.color = Color.parseColor("#F44336")
                    val background = RectF(
                        itemView.right + dX,
                        itemView.top.toFloat(),
                        itemView.right.toFloat(),
                        itemView.bottom.toFloat()
                    )
                    c.drawRect(background, paint)
                    
                    // 绘制文字
                    paint.color = Color.WHITE
                    paint.textSize = 48f
                    paint.textAlign = Paint.Align.RIGHT
                    c.drawText(
                        "删除",
                        itemView.right - 40f,
                        itemView.top + (itemView.height / 2f) + 15f,
                        paint
                    )
                }
                
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })
        
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
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

    private fun loadFridgeItems(keyword: String? = null) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().getFridgeItems()
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    items.clear()
                    val allItems = response.body()?.data ?: emptyList()
                    
                    // 如果有搜索关键词，进行本地过滤
                    val filteredItems = if (!keyword.isNullOrEmpty()) {
                        allItems.filter { 
                            it.ingredient?.name?.contains(keyword, ignoreCase = true) == true ||
                            it.storageLocation?.contains(keyword, ignoreCase = true) == true
                        }
                    } else {
                        allItems
                    }
                    
                    items.addAll(filteredItems)
                    fridgeAdapter.notifyDataSetChanged()
                    
                    if (!keyword.isNullOrEmpty()) {
                        if (filteredItems.isEmpty()) {
                            Toast.makeText(context, "没有找到相关食材", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "找到 ${filteredItems.size} 个食材", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // 检查即将过期的食材
                        checkExpiringItems()
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
    
    private fun loadConsumedItems(keyword: String? = null) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().getConsumedItems()
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    items.clear()
                    val allItems = response.body()?.data ?: emptyList()
                    
                    // 如果有搜索关键词，进行本地过滤
                    val filteredItems = if (!keyword.isNullOrEmpty()) {
                        allItems.filter { 
                            it.ingredient?.name?.contains(keyword, ignoreCase = true) == true ||
                            it.storageLocation?.contains(keyword, ignoreCase = true) == true
                        }
                    } else {
                        allItems
                    }
                    
                    items.addAll(filteredItems)
                    fridgeAdapter.notifyDataSetChanged()
                    
                    if (!keyword.isNullOrEmpty()) {
                        if (filteredItems.isEmpty()) {
                            Toast.makeText(context, "没有找到相关食材", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "找到 ${filteredItems.size} 个食材", Toast.LENGTH_SHORT).show()
                        }
                    } else if (filteredItems.isEmpty()) {
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
        // 检查登录状态
        if (!com.familyrecipes.android.util.AuthUtil.requireLogin(requireContext(), "标记食材为已消耗")) {
            // 恢复列表状态
            fridgeAdapter.notifyDataSetChanged()
            return
        }
        
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
        // 检查登录状态
        if (!com.familyrecipes.android.util.AuthUtil.requireLogin(requireContext(), "删除食材")) {
            // 恢复列表状态
            fridgeAdapter.notifyDataSetChanged()
            return
        }
        
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
    
    override fun performSearch(keyword: String) {
        // 食材页面：只搜索用户自己的食材（本地过滤）
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                // 重新加载数据
                when (currentTab) {
                    TAB_CURRENT -> loadFridgeItems(keyword)
                    TAB_CONSUMED -> loadConsumedItems(keyword)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "搜索失败: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    /**
     * 显示登录提示
     */
    private fun showLoginPrompt() {
        // 隐藏主要内容
        binding.recyclerView.visibility = View.GONE
        binding.swipeRefresh.visibility = View.GONE
        binding.tabLayout.visibility = View.GONE
        
        // 显示提示信息（可以用一个TextView或者空状态视图）
        android.widget.Toast.makeText(context, "冰箱功能需要登录后使用", android.widget.Toast.LENGTH_LONG).show()
        
        // 自动跳转到"我的"页面
        com.familyrecipes.android.util.AuthUtil.navigateToProfile(requireContext())
    }
    
    override fun onResume() {
        super.onResume()
        // 每次回到此页面时检查登录状态
        if (com.familyrecipes.android.util.AuthUtil.isLoggedIn()) {
            // 如果已登录但界面还未初始化，则初始化
            if (binding.recyclerView.visibility == View.GONE) {
                binding.recyclerView.visibility = View.VISIBLE
                binding.swipeRefresh.visibility = View.VISIBLE
                binding.tabLayout.visibility = View.VISIBLE
                
                setupRecyclerView()
                setupTabLayout()
                setupListeners()
                loadData()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

