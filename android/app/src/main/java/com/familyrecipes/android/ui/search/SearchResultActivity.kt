package com.familyrecipes.android.ui.search

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.R
import com.familyrecipes.android.util.AuthUtil
import com.familyrecipes.android.databinding.ActivitySearchResultBinding
import com.familyrecipes.android.ui.fridge.AddIngredientActivity
import com.familyrecipes.android.ui.recipe.RecipeDetailActivity
import kotlinx.coroutines.launch

/**
 * 搜索结果页面 - 展示混合搜索结果（菜谱+食材）
 */
class SearchResultActivity : AppCompatActivity() {

    private val model by lazy { androidx.lifecycle.ViewModelProvider(this)[SearchViewModel::class.java] }
    private lateinit var binding: ActivitySearchResultBinding
    private lateinit var searchResultAdapter: SearchResultAdapter
    private var pendingIngredient:com.familyrecipes.android.data.model.SearchItem?=null
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

    override fun onResume() {
        super.onResume()
        model.search(intent.getStringExtra(EXTRA_KEYWORD).orEmpty(),intent.getStringExtra(EXTRA_PRIORITY_TYPE) ?: "relevance")
        if(AuthUtil.isLoggedIn()){val item=pendingIngredient;pendingIngredient=null;item?.let(::openIngredient)}
    }
    private fun openIngredient(item:com.familyrecipes.android.data.model.SearchItem) {
        startActivity(Intent(this,AddIngredientActivity::class.java).putExtra("ingredient_id",item.id).putExtra("ingredient_name",item.name))
    }

    private fun setupToolbar(keyword: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.review_search_title,keyword)
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
                if(AuthUtil.requireLogin(this))openIngredient(item) else pendingIngredient=item
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SearchResultActivity)
            adapter = searchResultAdapter
        }
    }

    private fun performSearch(keyword: String, priorityType: String) {
        model.search(keyword,priorityType)
        binding.swipeRefresh.setOnRefreshListener { model.search(keyword,priorityType,true) }
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                model.state.collect { state ->
                    binding.swipeRefresh.isRefreshing=state.loading
                    searchItems.clear();searchItems.addAll(state.result?.items.orEmpty())
                    searchResultAdapter.notifyDataSetChanged()
                    binding.tvResultStats.text=state.error ?: getString(R.string.review_search_count,state.result?.recipeCount ?: 0,state.result?.ingredientCount ?: 0)
                    binding.tvResultStats.setOnClickListener {if(state.error!=null)model.search(keyword,priorityType,true)}
                }
            }
        }
    }
}
