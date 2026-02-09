package com.familyrecipes.android.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyrecipes.android.data.model.Recipe
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.databinding.ActivityFavoriteRecipesBinding
import com.familyrecipes.android.ui.adapter.RecipeAdapter
import com.familyrecipes.android.ui.recipe.RecipeDetailActivity
import kotlinx.coroutines.launch

/**
 * 我的收藏页面
 */
class FavoriteRecipesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoriteRecipesBinding
    private lateinit var recipeAdapter: RecipeAdapter
    private val recipes = mutableListOf<Recipe>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityFavoriteRecipesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        loadFavorites()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter(recipes) { recipe ->
            openRecipeDetail(recipe)
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@FavoriteRecipesActivity)
            adapter = recipeAdapter
        }
    }
    
    private fun openRecipeDetail(recipe: Recipe) {
        if (recipe.id == null) {
            Toast.makeText(this, "菜谱ID无效", Toast.LENGTH_SHORT).show()
            return
        }
        
        val intent = Intent(this, RecipeDetailActivity::class.java).apply {
            putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.id!!)
        }
        startActivity(intent)
    }
    
    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            loadFavorites()
        }
    }

    private fun loadFavorites() {
        binding.swipeRefresh.isRefreshing = true
        
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().getMyFavorites()
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data
                    recipes.clear()
                    data?.list?.let { recipes.addAll(it) }
                    recipeAdapter.notifyDataSetChanged()
                    
                    // 显示空状态
                    if (recipes.isEmpty()) {
                        binding.emptyView.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.emptyView.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(
                        this@FavoriteRecipesActivity,
                        "加载失败：${response.body()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@FavoriteRecipesActivity,
                    "网络错误: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
}

