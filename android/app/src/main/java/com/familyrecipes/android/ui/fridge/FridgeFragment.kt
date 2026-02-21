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
import com.familyrecipes.android.data.model.InventoryCategory
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.databinding.FragmentFridgeBinding
import com.familyrecipes.android.ui.SearchableFragment
import com.familyrecipes.android.ui.adapter.FridgeAdapter
import com.familyrecipes.android.ui.fridge.adapter.CategorySidebarAdapter
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

/**
 * 冰箱管理Fragment
 */
class FridgeFragment : Fragment(), SearchableFragment {

    private var _binding: FragmentFridgeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var fridgeAdapter: FridgeAdapter
    private lateinit var categoryAdapter: CategorySidebarAdapter
    private val items = mutableListOf<FridgeItem>()
    private val allItems = mutableListOf<FridgeItem>() // 保存所有数据用于筛选
    private val categories = mutableListOf<InventoryCategory>()
    
    private var currentTab = TAB_CURRENT  // 当前选中的Tab
    private var selectedCategoryId: Long? = null // 当前选中的分类ID
    
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
        
        setupCategorySidebar()
        setupRecyclerView()
        setupTabLayout()
        setupListeners()
        loadCategories()
        loadData()
    }

    /**
     * 设置分类侧边栏
     */
    private fun setupCategorySidebar() {
        categoryAdapter = CategorySidebarAdapter { category, isExpanded ->
            // 点击分类时的回调
            selectedCategoryId = category?.id
            filterItemsByCategory()
        }
        
        binding.categorySidebar.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoryAdapter
        }
    }

    /**
     * 加载分类数据
     */
    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().getCategoryTree()
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    val categoryList = response.body()?.data ?: emptyList()
                    categories.clear()
                    categories.addAll(categoryList)
                    categoryAdapter.submitList(categoryList)
                }
            } catch (e: Exception) {
                android.util.Log.e("FridgeFragment", "加载分类失败", e)
            }
        }
    }

    /**
     * 根据分类筛选库存
     */
    private fun filterItemsByCategory() {
        items.clear()
        
        if (selectedCategoryId == null) {
            // 显示全部
            items.addAll(allItems)
        } else {
            // 筛选指定分类
            items.addAll(allItems.filter { it.ingredient?.categoryId == selectedCategoryId })
        }
        
        fridgeAdapter.notifyDataSetChanged()
        
        // 更新适配器选中状态
        categoryAdapter.setSelectedCategory(selectedCategoryId)
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
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                // 根据当前tab决定允许的滑动方向
                val swipeFlags = if (currentTab == TAB_CONSUMED) {
                    // 消耗历史：只允许左滑删除
                    ItemTouchHelper.LEFT
                } else {
                    // 当前库存：允许左滑删除和右滑消耗
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                }
                return makeMovementFlags(0, swipeFlags)
            }
            
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
                        // 右滑标记为已消耗（仅在当前库存tab可用）
                        if (currentTab == TAB_CURRENT) {
                            markAsConsumed(item.id!!)
                        }
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
                
                // 只在当前库存tab显示右滑"已消耗"提示
                if (dX > 0 && currentTab == TAB_CURRENT) {
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
    
    /**
     * 点击食材，查看相关菜谱
     * 即使食材ID不存在或没有关联菜谱，也应优雅处理
     */
    private fun navigateToRecipesByIngredient(item: FridgeItem) {
        item.ingredient?.let { ingredient ->
            android.util.Log.d("FridgeFragment", "点击食材: ${ingredient.name}, ID: ${ingredient.id}")
            
            // 跳转到主页的菜谱tab，并传递食材信息用于搜索
            Toast.makeText(
                context, 
                "查看包含「${ingredient.name}」的菜谱（功能待实现）", 
                Toast.LENGTH_SHORT
            ).show()
            
            // TODO: 实际实现时的代码
            // 方式1: 跳转到菜谱列表并按食材过滤
            // val intent = Intent(requireContext(), RecipeListActivity::class.java)
            // intent.putExtra("ingredient_id", ingredient.id)
            // intent.putExtra("ingredient_name", ingredient.name)
            // startActivity(intent)
            
            // 方式2: 切换到主页的菜谱tab并设置搜索关键词
            // (parentFragment?.parentFragment as? MainActivity)?.switchToRecipeTabWithSearch(ingredient.name)
            
            // 注意：即使ingredient.id为null或后端没有关联的菜谱，
            // 界面应该显示"暂无包含该食材的菜谱"而不是崩溃
        } ?: run {
            // ingredient为null的异常情况
            android.util.Log.w("FridgeFragment", "点击的食材对象ingredient为null")
            Toast.makeText(context, "食材信息缺失", Toast.LENGTH_SHORT).show()
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
                    allItems.clear()
                    allItems.addAll(response.body()?.data ?: emptyList())
                    
                    // 应用分类筛选
                    filterItemsByCategory()
                    
                    // 如果有搜索关键词，进一步过滤
                    if (!keyword.isNullOrEmpty()) {
                        val searchFiltered = items.filter { 
                            it.ingredient?.name?.contains(keyword, ignoreCase = true) == true ||
                            it.storageLocation?.contains(keyword, ignoreCase = true) == true
                        }
                        items.clear()
                        items.addAll(searchFiltered)
                        fridgeAdapter.notifyDataSetChanged()
                        
                        if (searchFiltered.isEmpty()) {
                            Toast.makeText(context, "没有找到相关库存", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "找到 ${searchFiltered.size} 个库存", Toast.LENGTH_SHORT).show()
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
                    allItems.clear()
                    allItems.addAll(response.body()?.data ?: emptyList())
                    
                    // 应用分类筛选
                    filterItemsByCategory()
                    
                    // 如果有搜索关键词，进一步过滤
                    if (!keyword.isNullOrEmpty()) {
                        val searchFiltered = items.filter { 
                            it.ingredient?.name?.contains(keyword, ignoreCase = true) == true ||
                            it.storageLocation?.contains(keyword, ignoreCase = true) == true
                        }
                        items.clear()
                        items.addAll(searchFiltered)
                        fridgeAdapter.notifyDataSetChanged()
                        
                        if (searchFiltered.isEmpty()) {
                            Toast.makeText(context, "没有找到相关库存", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "找到 ${searchFiltered.size} 个库存", Toast.LENGTH_SHORT).show()
                        }
                    } else if (items.isEmpty()) {
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
        val expiringCount = allItems.count { it.status == FridgeItem.STATUS_EXPIRING }
        val expiredCount = allItems.count { it.status == FridgeItem.STATUS_EXPIRED }
        
        if (expiringCount > 0 || expiredCount > 0) {
            val message = buildString {
                if (expiredCount > 0) append("${expiredCount}个库存已过期\n")
                if (expiringCount > 0) append("${expiringCount}个库存即将过期")
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun markAsConsumed(itemId: Long) {
        // 检查登录状态
        if (!com.familyrecipes.android.util.AuthUtil.requireLogin(requireContext(), "标记库存为已消耗")) {
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
        if (!com.familyrecipes.android.util.AuthUtil.requireLogin(requireContext(), "删除库存")) {
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
        // 库存页面：只搜索用户自己的库存（本地过滤）
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
        binding.categorySidebar.visibility = View.GONE
        
        // 显示提示信息（可以用一个TextView或者空状态视图）
        android.widget.Toast.makeText(context, "库存功能需要登录后使用", android.widget.Toast.LENGTH_LONG).show()
        
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
                binding.categorySidebar.visibility = View.VISIBLE
                
                setupCategorySidebar()
                setupRecyclerView()
                setupTabLayout()
                setupListeners()
                loadCategories()
            }
            // 无论是否初始化过，都刷新数据（从添加库存页面返回时需要）
            loadData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

