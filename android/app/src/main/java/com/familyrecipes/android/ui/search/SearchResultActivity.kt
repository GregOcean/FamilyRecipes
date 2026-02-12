package com.familyrecipes.android.ui.search

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.databinding.ActivitySearchResultBinding
import com.familyrecipes.android.ui.fridge.AddIngredientActivity
import com.familyrecipes.android.ui.recipe.RecipeDetailActivity
import kotlinx.coroutines.launch

/**
 * 搜索结果页面 - 展示混合搜索结果（菜谱+食材）
 */
class SearchResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchResultBinding
    private lateinit var searchResultAdapter: SearchResultAdapter
    private val searchItems = mutableListOf<com.familyrecipes.android.data.model.SearchItem>()

    companion object {
        const val EXTRA_KEYWORD = "keyword"
        const val EXTRA_PRIORITY_TYPE = "priority_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val keyword = intent.getStringExtra(EXTRA_KEYWORD) ?: ""
        val priorityType = intent.getStringExtra(EXTRA_PRIORITY_TYPE) ?: "relevance"

        setupToolbar(keyword)
        setupRecyclerView()
        performSearch(keyword, priorityType)
    }

    private fun setupToolbar(keyword: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "搜索: $keyword"
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        searchResultAdapter = SearchResultAdapter(
            searchItems,
            onRecipeClick = { item ->
                // 点击菜谱，跳转到详情页
                val intent = Intent(this, RecipeDetailActivity::class.java).apply {
                    putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, item.id)
                }
                startActivity(intent)
            },
            onIngredientClick = { item ->
                // 点击食材，可以跳转到相关菜谱或添加到冰箱
                Toast.makeText(
                    this,
                    "点击食材: ${item.name}（可查看相关菜谱或添加到冰箱）",
                    Toast.LENGTH_SHORT
                ).show()
                // TODO: 实现食材详情或相关菜谱页面
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SearchResultActivity)
            adapter = searchResultAdapter
        }
    }

    private fun performSearch(keyword: String, priorityType: String) {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().globalSearch(
                    keyword = keyword,
                    priorityType = priorityType
                )

                if (response.isSuccessful && response.body()?.code == 200) {
                    val searchResult = response.body()?.data
                    searchItems.clear()
                    searchResult?.items?.let { searchItems.addAll(it) }
                    searchResultAdapter.notifyDataSetChanged()

                    // 更新统计信息
                    val recipeCount = searchResult?.recipeCount ?: 0
                    val ingredientCount = searchResult?.ingredientCount ?: 0
                    binding.tvResultStats.text = "找到 ${recipeCount} 个菜谱, ${ingredientCount} 个食材"

                    if (searchItems.isEmpty()) {
                        Toast.makeText(this@SearchResultActivity, "没有找到相关结果", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@SearchResultActivity, "搜索失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SearchResultActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        // 下拉刷新
        binding.swipeRefresh.setOnRefreshListener {
            performSearch(keyword, priorityType)
        }
    }
}

