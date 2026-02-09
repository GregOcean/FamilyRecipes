package com.familyrecipes.android.ui

import android.os.Bundle
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
                        // 添加菜谱
                        Toast.makeText(this, "添加菜谱功能开发中", Toast.LENGTH_SHORT).show()
                        // TODO: 跳转到添加菜谱页面
                        // startActivity(Intent(this, EditRecipeActivity::class.java))
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

