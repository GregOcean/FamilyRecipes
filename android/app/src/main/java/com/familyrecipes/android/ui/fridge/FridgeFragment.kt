package com.familyrecipes.android.ui.fridge

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.widget.doAfterTextChanged
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

    private var loadJob: kotlinx.coroutines.Job? = null
    private var loadGeneration=0
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
        binding.btnCategories.setOnClickListener {
            val choices=categories.flatMap{listOf(it)+it.children.orEmpty()}
            androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle("选择分类").setItems((listOf("全部分类")+choices.map{"${it.icon.orEmpty()} ${it.name}"}).toTypedArray()){_,index->
                val selected=choices.getOrNull(index-1);selectedCategoryId=selected?.id;binding.btnCategories.text=(selected?.name?:"全部分类")+" ▾";filterItemsByCategory()
            }.show()
        }
        binding.etInventorySearch.doAfterTextChanged { filterItemsByCategory() }
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
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.getService().getCategoryTree()
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    val categoryList = response.body()?.data ?: emptyList()
                    categories.clear()
                    categories.addAll(categoryList)
                    categoryAdapter.submitList(categoryList)
                    filterItemsByCategory()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
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
        val query = binding.etInventorySearch.text.toString().trim()
        val searched = allItems.filter { currentTab!=2 || it.status==FridgeItem.STATUS_EXPIRING || it.status==FridgeItem.STATUS_EXPIRED }.filter { query.isEmpty() || it.ingredient?.name?.contains(query, ignoreCase=true) == true || it.ingredient?.aliases?.contains(query, ignoreCase=true) == true }
        val ids = selectedCategoryId?.let { com.familyrecipes.android.util.InventoryCategories.descendants(it, categories) }
        items.addAll(searched.filter { ids == null || (it.categoryOverrideId ?: it.ingredient?.categoryId) in ids })
        val counts = categories.associate { category ->
            val descendants = com.familyrecipes.android.util.InventoryCategories.descendants(category.id, categories)
            category.id to searched.count { (it.categoryOverrideId ?: it.ingredient?.categoryId) in descendants }
        }
        categoryAdapter.setCounts(counts, searched.size)
        binding.tvInventoryEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        binding.tvInventoryEmpty.text = if (query.isNotEmpty()) "未找到匹配物品，可切换“全部”或修改搜索词"
            else "这个分类还没有物品\n点击 + 添加，或查看其他分类"

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
            },
            onManage = { showItemActions(it) }
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
            ItemTouchHelper.RIGHT
        ) {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                // 根据当前tab决定允许的滑动方向
                val swipeFlags = if (currentTab == TAB_CONSUMED) {
                    // 消耗历史：只允许左滑删除
                    0
                } else {
                    // 当前库存：允许左滑删除和右滑消耗
                    ItemTouchHelper.RIGHT
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
                val item = items.getOrNull(position) ?: return
                
                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        // 左滑删除
                        deleteItem(item.id!!)
                    }
                    ItemTouchHelper.RIGHT -> {
                        // 右滑标记为已消耗（仅在当前库存tab可用）
                        if (currentTab != TAB_CONSUMED) {
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
                
                // 只在当前库存tab显示右滑"已吃完"提示
                if (dX > 0 && currentTab != TAB_CONSUMED) {
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
                        "已吃完",
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
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val account=com.familyrecipes.android.data.local.LocalStore.account
                val dao=com.familyrecipes.android.data.local.LocalStore.instance.records()
                val key="stock:${item.id}"
                val draft=dao.get(account,"draft",key)?.let { com.google.gson.Gson().fromJson(it,FridgeItem::class.java) } ?: item
                val kitchen=ApiClient.getService().getKitchens().body()?.data?.find { it.id==item.kitchenId } ?: return@launch
                val members=ApiClient.getService().getGroupDetail(requireNotNull(kitchen.groupId)).body()?.data?.members.orEmpty()
                val box=android.widget.LinearLayout(requireContext()).apply { orientation=android.widget.LinearLayout.VERTICAL;setPadding(32,16,32,16) }
                fun field(label:String,value:String?)=android.widget.EditText(requireContext()).apply { hint=label;setText(value);box.addView(this) }
                val amount=field("数量",draft.amount)
                val expiry=field("到期日期 YYYY-MM-DD",draft.expiryDate)
                val location=field("存放位置",draft.storageLocation)
                val notes=field("备注",draft.notes)
                val owner=android.widget.Spinner(requireContext()).apply {
                    adapter=android.widget.ArrayAdapter(requireContext(),android.R.layout.simple_spinner_dropdown_item,
                        listOf("全家共享（无预留）")+members.map { "预留给 ${it.user?.username ?: it.userId}" })
                    setSelection(members.indexOfFirst { it.userId==draft.ownerUserId }+1)
                }
                box.addView(owner)
                val categoriesResponse = ApiClient.getService().getAllCategories()
                check(categoriesResponse.isSuccessful && categoriesResponse.body()?.code == 200)
                val choices = categoriesResponse.body()?.data.orEmpty()
                val categoryLabel = android.widget.TextView(requireContext()).apply { text="物品分类（仅修改这条库存）" }
                box.addView(categoryLabel)
                val classification = android.widget.Spinner(requireContext()).apply {
                    adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item,
                        listOf("跟随食材库：${item.ingredient?.category.orEmpty()}") + choices.map { "${it.icon.orEmpty()} ${it.name}" })
                    setSelection(choices.indexOfFirst { it.id == draft.categoryOverrideId } + 1)
                }
                box.addView(classification)

                fun value()=item.copy(amount=amount.text.toString(),expiryDate=expiry.text.toString(),
                    categoryOverrideId=choices.getOrNull(classification.selectedItemPosition-1)?.id,
                    storageLocation=location.text.toString(),notes=notes.text.toString(),
                    ownerUserId=if(owner.selectedItemPosition==0)null else members[owner.selectedItemPosition-1].userId)
                fun saveDraft()=com.familyrecipes.android.data.local.DraftWrites.put(account,key,com.google.gson.Gson().toJson(value()))
                listOf(amount,expiry,location,notes).forEach { it.doAfterTextChanged { saveDraft() } }
                owner.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent:android.widget.AdapterView<*>?,view:View?,position:Int,id:Long) { saveDraft() }
                    override fun onNothingSelected(parent:android.widget.AdapterView<*>?) { }
                }
                classification.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent:android.widget.AdapterView<*>?,view:View?,position:Int,id:Long) { saveDraft() }
                    override fun onNothingSelected(parent:android.widget.AdapterView<*>?) { }
                }
                val scroll=android.widget.ScrollView(requireContext()).apply { addView(box) }
                val dialog=androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle("编辑 ${item.ingredient?.name ?: "库存"}")
                    .setView(scroll).setPositiveButton("保存",null).setNegativeButton("取消",null).create()
                dialog.show();dialog.getButton(-1).setOnClickListener {
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val response=ApiClient.getService().updateFridgeItem(item.id!!,value())
                            check(response.isSuccessful && response.body()?.code==200)
                            com.familyrecipes.android.data.local.DraftWrites.clear(account,key)
                            dialog.dismiss();loadData()
                        } catch(e:kotlinx.coroutines.CancellationException) { throw e }
                          catch(_:Exception) { expiry.error="保存失败，检查日期和网络后重试" }
                    }
                }
            } catch(e:kotlinx.coroutines.CancellationException) { throw e }
              catch(_:Exception) { Toast.makeText(context,"编辑信息加载失败，请重试",Toast.LENGTH_SHORT).show() }
        }
    }

    private fun loadData() {
        when (currentTab) {
            TAB_CURRENT, 2 -> loadFridgeItems()
            TAB_CONSUMED -> loadConsumedItems()
        }
    }

    private fun loadFridgeItems(keyword: String? = null) {
        val revision=++loadGeneration
        val kitchen=com.familyrecipes.android.data.local.PreferenceManager.currentKitchenId
        loadJob?.cancel()
        loadJob=viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.getService().getFridgeItems()
                
                if(revision!=loadGeneration || kitchen!=com.familyrecipes.android.data.local.PreferenceManager.currentKitchenId)return@launch
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
                        // Expiry badges are shown in rows; reminders are rate limited by the worker.
                    }
                } else {
                    Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                if(revision==loadGeneration) _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }
    
    private fun loadConsumedItems(keyword: String? = null) {
        val revision=++loadGeneration
        val kitchen=com.familyrecipes.android.data.local.PreferenceManager.currentKitchenId
        loadJob?.cancel()
        loadJob=viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.getService().getConsumedItems()
                
                if(revision!=loadGeneration || kitchen!=com.familyrecipes.android.data.local.PreferenceManager.currentKitchenId)return@launch
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
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                if(revision==loadGeneration) _binding?.swipeRefresh?.isRefreshing = false
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

    private fun showItemActions(item:FridgeItem) {
        val consumed=item.status==FridgeItem.STATUS_CONSUMED
        val labels=arrayOf(if(consumed)"恢复到库存" else "已吃完 / 用完","修改剩余数量","变质或丢弃","删除误录记录")
        androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle(item.ingredient?.name).setItems(labels){_,index->
            when(index){0->changeStatus(item.id!!,if(consumed)"normal" else "consumed");1->navigateToRecipesByIngredient(item);2->changeStatus(item.id!!,"discarded");3->changeStatus(item.id!!,"deleted")}
        }.show()
    }
    private fun markAsConsumed(itemId:Long)=changeStatus(itemId,"consumed")
    private fun deleteItem(itemId:Long)=changeStatus(itemId,"deleted")
    private fun changeStatus(itemId:Long,status:String,undo:Boolean=false){
        val previous=allItems.find{it.id==itemId}?.status?:"normal"
        viewLifecycleOwner.lifecycleScope.launch{try{
            com.familyrecipes.android.ui.design.Ui.ok(ApiClient.getService().changeStockStatus(itemId,mapOf("status" to status)))
            loadData()
            if(!undo)com.google.android.material.snackbar.Snackbar.make(binding.root,when(status){"consumed"->"已移到已吃完";"discarded"->"已标记丢弃";"deleted"->"已删除记录";else->"已恢复库存"},8000)
                .setAction("撤销"){changeStatus(itemId,if(previous=="consumed")"consumed" else "normal",true)}.show()
        }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){fridgeAdapter.notifyDataSetChanged();com.familyrecipes.android.ui.design.Ui.toast(requireContext(),e)}}
    }
    override fun onHiddenChanged(hidden:Boolean){super.onHiddenChanged(hidden);if(!hidden && ::fridgeAdapter.isInitialized)loadData()}

    override fun performSearch(keyword: String) {
        // 库存页面：只搜索用户自己的库存（本地过滤）
        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 重新加载数据
                when (currentTab) {
                    TAB_CURRENT, 2 -> loadFridgeItems(keyword)
                    TAB_CONSUMED -> loadConsumedItems(keyword)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(context, "搜索失败: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                _binding?.swipeRefresh?.isRefreshing = false
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

