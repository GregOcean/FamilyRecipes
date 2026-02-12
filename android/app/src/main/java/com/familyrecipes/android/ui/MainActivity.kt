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
        
        // 默认显示推荐页面
        if (savedInstanceState == null) {
            val navigateTo = intent.getStringExtra("navigate_to")
            when (navigateTo) {
                "fridge" -> binding.bottomNavigation.selectedItemId = R.id.nav_fridge
                "profile" -> binding.bottomNavigation.selectedItemId = R.id.nav_profile
                else -> binding.bottomNavigation.selectedItemId = R.id.nav_recommend
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
        
        // 搜索按钮点击事件
        binding.btnSearch.setOnClickListener {
            performSearch()
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
                    R.id.nav_recommend -> {
                        showFragment(RecommendFragment())
                        return@OnItemSelectedListener true
                    }
                    R.id.nav_recipes -> {
                        showFragment(RecipeListFragment())
                        return@OnItemSelectedListener true
                    }
                    R.id.nav_fridge -> {
                        showFragment(FridgeFragment())
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
            val intent = Intent(this, EditRecipeActivity::class.java)
            startActivity(intent)
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
            val intent = Intent(this, AddIngredientActivity::class.java)
            startActivity(intent)
        }
        
        bottomSheetDialog.show()
    }
}

