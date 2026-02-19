package com.familyrecipes.android.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyrecipes.android.R
import com.familyrecipes.android.databinding.ActivityMainBinding
import com.familyrecipes.android.databinding.BottomSheetAddMenuBinding
import com.familyrecipes.android.ui.adapter.SearchHistoryAdapter
import com.familyrecipes.android.ui.fridge.AddIngredientActivity
import com.familyrecipes.android.ui.fridge.FridgeFragment
import com.familyrecipes.android.ui.home.HomeFragment
import com.familyrecipes.android.ui.message.MessageFragment
import com.familyrecipes.android.ui.profile.ProfileFragment
import com.familyrecipes.android.ui.recipe.EditRecipeActivity
import com.familyrecipes.android.ui.recipe.RecipeListFragment
import com.familyrecipes.android.ui.recommend.RecommendFragment
import com.familyrecipes.android.ui.search.SearchResultActivity
import com.familyrecipes.android.util.SearchHistoryManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationBarView

/**
 * 主Activity - 包含底部导航
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentFragment: Fragment? = null
    private lateinit var searchHistoryManager: SearchHistoryManager
    private var searchHistoryAdapter: SearchHistoryAdapter? = null
    private var currentKitchenName: String = "我的厨房" // 当前厨房名称

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        searchHistoryManager = SearchHistoryManager(this)
        
        setupBottomNavigation()
        disableTintForIcons()
        setupFabButton()
        setupKeyboardBehavior()
        setupSearchHistory()
        loadCurrentKitchen()
        
        // 默认显示首页
        if (savedInstanceState == null) {
            val navigateTo = intent.getStringExtra("navigate_to")
            when (navigateTo) {
                "fridge" -> binding.bottomNavigation.selectedItemId = R.id.nav_fridge
                "message" -> binding.bottomNavigation.selectedItemId = R.id.nav_message
                "profile" -> binding.bottomNavigation.selectedItemId = R.id.nav_profile
                else -> binding.bottomNavigation.selectedItemId = R.id.nav_home
            }
        }
    }
    
    private fun setupKeyboardBehavior() {
        // 点击 Fragment 容器时隐藏键盘和搜索历史
        binding.fragmentContainer.setOnClickListener {
            hideKeyboard()
            hideSearchHistory()
        }
        
        // 搜索框获得焦点时显示历史记录
        binding.searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && binding.searchEditText.text.isEmpty()) {
                showSearchHistory()
            } else if (!hasFocus) {
                hideKeyboard()
                // 延迟隐藏历史记录，以便点击历史记录项有效
                binding.searchHistoryContainer.postDelayed({
                    hideSearchHistory()
                }, 200)
            }
        }
        
        // 监听搜索框文本变化
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (binding.searchEditText.hasFocus()) {
                    if (s.isNullOrEmpty()) {
                        showSearchHistory()
                    } else {
                        hideSearchHistory()
                    }
                }
            }
        })
        
        // 切换厨房按钮点击事件
        binding.btnSwitchKitchen.setOnClickListener {
            showSwitchKitchenDialog()
        }
        
        // 添加社交功能按钮点击事件（消息Tab使用）
        binding.btnAddSocial.setOnClickListener {
            showAddSocialMenu()
        }
        
        // 搜索框回车键事件
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
    }
    
    /**
     * 设置搜索历史
     */
    private fun setupSearchHistory() {
        binding.searchHistoryRecyclerView.layoutManager = LinearLayoutManager(this)
        
        // 清空历史按钮
        binding.btnClearHistory.setOnClickListener {
            searchHistoryManager.clearHistory()
            updateSearchHistory()
            Toast.makeText(this, "搜索历史已清空", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 显示搜索历史
     */
    private fun showSearchHistory() {
        val history = searchHistoryManager.getHistory()
        if (history.isNotEmpty()) {
            updateSearchHistory()
            binding.searchHistoryContainer.visibility = View.VISIBLE
        } else {
            binding.searchHistoryContainer.visibility = View.GONE
        }
    }
    
    /**
     * 隐藏搜索历史
     */
    private fun hideSearchHistory() {
        binding.searchHistoryContainer.visibility = View.GONE
    }
    
    /**
     * 更新搜索历史列表
     */
    private fun updateSearchHistory() {
        val history = searchHistoryManager.getHistory()
        searchHistoryAdapter = SearchHistoryAdapter(
            history = history,
            onItemClick = { keyword ->
                // 点击历史记录，填充到搜索框并执行搜索
                binding.searchEditText.setText(keyword)
                binding.searchEditText.setSelection(keyword.length)
                hideSearchHistory()
                performSearch()
            },
            onDeleteClick = { keyword ->
                // 删除单条历史记录
                searchHistoryManager.removeHistory(keyword)
                updateSearchHistory()
                
                // 如果没有历史记录了，隐藏卡片
                if (searchHistoryManager.getHistory().isEmpty()) {
                    hideSearchHistory()
                }
            }
        )
        binding.searchHistoryRecyclerView.adapter = searchHistoryAdapter
    }
    
    /**
     * 执行搜索
     */
    private fun performSearch() {
        val keyword = binding.searchEditText.text.toString().trim()
        if (keyword.isEmpty()) {
            Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 添加到搜索历史
        searchHistoryManager.addHistory(keyword)
        
        // 隐藏键盘和搜索历史
        hideKeyboard()
        hideSearchHistory()
        
        // 清空搜索框焦点
        binding.searchEditText.clearFocus()
        
        // 根据当前Fragment决定搜索方式
        when (currentFragment) {
            is RecipeListFragment, is FridgeFragment -> {
                // 菜谱和食材页面：在当前页面搜索（只搜自己的数据）
                (currentFragment as? SearchableFragment)?.performSearch(keyword)
            }
            is RecommendFragment -> {
                // 推荐页面：全局搜索，跳转到搜索结果页面
                val intent = Intent(this, SearchResultActivity::class.java).apply {
                    putExtra(SearchResultActivity.EXTRA_KEYWORD, keyword)
                    putExtra(SearchResultActivity.EXTRA_PRIORITY_TYPE, "relevance")
                }
                startActivity(intent)
            }
            is HomeFragment -> {
                // 首页：全局搜索，跳转到搜索结果页面
                val intent = Intent(this, SearchResultActivity::class.java).apply {
                    putExtra(SearchResultActivity.EXTRA_KEYWORD, keyword)
                    putExtra(SearchResultActivity.EXTRA_PRIORITY_TYPE, "relevance")
                }
                startActivity(intent)
            }
            else -> {
                // 其他页面不支持搜索
                Toast.makeText(this, "当前页面不支持搜索", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 隐藏软键盘
     */
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocusView = currentFocus
        if (currentFocusView != null) {
            imm.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
            currentFocusView.clearFocus()
        }
    }
    
    /**
     * 显示切换厨房对话框
     */
    private fun showSwitchKitchenDialog() {
        Toast.makeText(this, "切换厨房功能开发中\n当前：$currentKitchenName", Toast.LENGTH_LONG).show()
        // TODO: 实现切换厨房功能
        // 1. 获取用户加入的所有群组/厨房
        // 2. 显示厨房列表对话框
        // 3. 切换当前厨房上下文
        // 4. 刷新所有数据（菜谱、食材、推荐等）
        // 5. 调用 updateCurrentKitchen(kitchenName) 更新显示
    }
    
    /**
     * 显示添加社交功能菜单（消息Tab使用）
     */
    private fun showAddSocialMenu() {
        val options = arrayOf("创建群聊", "扫一扫")
        
        android.app.AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // 创建群聊
                        val intent = android.content.Intent(this, com.familyrecipes.android.ui.social.CreateGroupActivity::class.java)
                        startActivity(intent)
                    }
                    1 -> {
                        // 扫一扫
                        val intent = android.content.Intent(this, com.familyrecipes.android.ui.social.ScanQRActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
            .show()
    }
    
    /**
     * 加载当前厨房信息
     */
    private fun loadCurrentKitchen() {
        // 从SharedPreferences加载当前选中的厨房
        val savedKitchen = com.familyrecipes.android.data.local.PreferenceManager.currentKitchenName
        if (!savedKitchen.isNullOrEmpty()) {
            currentKitchenName = savedKitchen
        } else {
            // 如果没有保存的厨房，使用默认值
            currentKitchenName = "我的厨房"
            // TODO: 后续可以从后端获取用户的默认群组/厨房
        }
        
        updateCurrentKitchen(currentKitchenName)
    }
    
    /**
     * 更新当前厨房名称显示
     */
    private fun updateCurrentKitchen(kitchenName: String) {
        currentKitchenName = kitchenName
        binding.tvCurrentKitchen.text = kitchenName
        
        // 如果厨房名称超过4个字，启用跑马灯效果
        if (kitchenName.length > 4) {
            binding.tvCurrentKitchen.isSelected = true
            binding.tvCurrentKitchen.requestFocus()
        } else {
            binding.tvCurrentKitchen.isSelected = false
        }
    }
    
    /**
     * 点击搜索框外的区域时隐藏键盘
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (view is android.widget.EditText) {
                val outRect = android.graphics.Rect()
                view.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    view.clearFocus()
                    hideKeyboard()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
    
    private fun disableTintForIcons() {
        // 禁用底部导航栏图标的自动着色，保持原始彩色图标
        binding.bottomNavigation.itemIconTintList = null
    }

    private fun setupFabButton() {
        binding.fabAdd.setOnClickListener {
            showAddMenu()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(
            NavigationBarView.OnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        showFragment(HomeFragment())
                        return@OnItemSelectedListener true
                    }
                    R.id.nav_fridge -> {
                        showFragment(FridgeFragment())
                        return@OnItemSelectedListener true
                    }
                    R.id.nav_message -> {
                        showFragment(MessageFragment())
                        return@OnItemSelectedListener true
                    }
                    R.id.nav_profile -> {
                        showFragment(ProfileFragment())
                        return@OnItemSelectedListener true
                    }
                }
                false
            }
        )
    }

    private fun showFragment(fragment: Fragment) {
        if (currentFragment === fragment) return
        
        val transaction = supportFragmentManager.beginTransaction()
        
        currentFragment?.let {
            transaction.hide(it)
        }
        
        if (fragment.isAdded) {
            transaction.show(fragment)
        } else {
            transaction.add(R.id.fragment_container, fragment)
        }
        
        transaction.commit()
        currentFragment = fragment
        
        // 根据Fragment类型控制搜索栏和按钮显示/隐藏
        when (fragment) {
            is ProfileFragment -> {
                // 我的页面：隐藏搜索栏
                binding.toolbar.visibility = View.GONE
            }
            is MessageFragment -> {
                // 消息页面：显示搜索栏，显示添加按钮，隐藏切换厨房按钮
                binding.toolbar.visibility = View.VISIBLE
                binding.btnSwitchKitchen.visibility = View.GONE
                binding.btnAddSocial.visibility = View.VISIBLE
            }
            else -> {
                // 其他页面：显示搜索栏，显示切换厨房按钮，隐藏添加按钮
                binding.toolbar.visibility = View.VISIBLE
                binding.btnSwitchKitchen.visibility = View.VISIBLE
                binding.btnAddSocial.visibility = View.GONE
            }
        }
    }
    
    /**
     * 显示底部添加菜单
     */
    private fun showAddMenu() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val binding = BottomSheetAddMenuBinding.inflate(LayoutInflater.from(this))
        bottomSheetDialog.setContentView(binding.root)
        
        // 菜谱按钮
        binding.btnAddRecipe.setOnClickListener {
            bottomSheetDialog.dismiss()
            // 检查登录状态
            if (com.familyrecipes.android.util.AuthUtil.requireLogin(this, "发布菜谱")) {
                val intent = Intent(this, EditRecipeActivity::class.java)
                startActivity(intent)
            }
        }
        
        // 作品按钮
        binding.btnAddWork.setOnClickListener {
            bottomSheetDialog.dismiss()
            Toast.makeText(this, "作品功能开发中...", Toast.LENGTH_SHORT).show()
            // TODO: 跳转到添加作品页面
        }
        
        // 菜单按钮
        binding.btnAddMenu.setOnClickListener {
            bottomSheetDialog.dismiss()
            Toast.makeText(this, "菜单功能开发中...", Toast.LENGTH_SHORT).show()
            // TODO: 跳转到添加菜单页面
        }
        
        // 食材按钮
        binding.btnAddIngredient.setOnClickListener {
            bottomSheetDialog.dismiss()
            // 检查登录状态
            if (com.familyrecipes.android.util.AuthUtil.requireLogin(this, "添加食材")) {
                val intent = Intent(this, AddIngredientActivity::class.java)
                startActivity(intent)
            }
        }
        
        bottomSheetDialog.show()
    }
}

