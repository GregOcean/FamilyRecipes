package com.familyrecipes.android.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.familyrecipes.android.R
import com.familyrecipes.android.databinding.ActivityMainBinding
import com.familyrecipes.android.ui.fridge.FridgeFragment
import com.familyrecipes.android.ui.profile.ProfileFragment
import com.familyrecipes.android.ui.recipe.RecipeListFragment
import com.familyrecipes.android.ui.recommend.RecommendFragment
import com.google.android.material.navigation.NavigationBarView

/**
 * 主Activity - 包含底部导航
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        disableTintForAddButton()
        setupKeyboardBehavior()
        
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
        // 点击 Fragment 容器时隐藏键盘
        binding.fragmentContainer.setOnClickListener {
            hideKeyboard()
        }
        
        // 搜索框失去焦点时隐藏键盘
        binding.searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                hideKeyboard()
            }
        }
        
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
     * 执行搜索
     */
    private fun performSearch() {
        val keyword = binding.searchEditText.text.toString().trim()
        if (keyword.isEmpty()) {
            Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 隐藏键盘
        hideKeyboard()
        
        // 调用当前Fragment的搜索方法
        currentFragment?.let { fragment ->
            if (fragment is SearchableFragment) {
                fragment.performSearch(keyword)
            } else {
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
    
    private fun disableTintForAddButton() {
        // 禁用中间加号按钮的自动着色，保持原始颜色
        binding.bottomNavigation.itemIconTintList = null
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
                    R.id.nav_add -> {
                        // 添加菜谱 - 跳转到添加页面
                        val intent = Intent(this, com.familyrecipes.android.ui.recipe.EditRecipeActivity::class.java)
                        startActivity(intent)
                        return@OnItemSelectedListener false // 不切换Fragment
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
}

