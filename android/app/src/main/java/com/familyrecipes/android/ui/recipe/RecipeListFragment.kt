package com.familyrecipes.android.ui.recipe

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.familyrecipes.android.data.model.Recipe
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.databinding.FragmentRecipeListBinding
import com.familyrecipes.android.ui.SearchableFragment
import com.familyrecipes.android.ui.adapter.RecipeAdapter
import kotlinx.coroutines.launch

/**
 * 菜谱列表Fragment
 */
class RecipeListFragment : Fragment(), SearchableFragment {

    private var _binding: FragmentRecipeListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var recipeAdapter: RecipeAdapter
    private val recipes = mutableListOf<Recipe>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupListeners()
        loadRecipes()
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter(recipes) { recipe ->
            // 点击菜谱，跳转到详情页
            openRecipeDetail(recipe)
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recipeAdapter
        }
    }
    
    private fun openRecipeDetail(recipe: Recipe) {
        // 检查菜谱ID是否有效
        if (recipe.id == null) {
            android.widget.Toast.makeText(requireContext(), "菜谱ID无效", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        val intent = Intent(requireContext(), RecipeDetailActivity::class.java).apply {
            putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.id!!)
        }
        startActivity(intent)
    }
    
    override fun onResume() {
        super.onResume()
        // 从详情页返回时刷新数据
        loadRecipes()
    }

    private fun setupListeners() {
        // 下拉刷新
        binding.swipeRefresh.setOnRefreshListener {
            loadRecipes()
        }
    }

    private fun loadRecipes(keyword: String? = null) {
        lifecycleScope.launch {
            try {
                // 只搜索当前用户的菜谱（使用keyword参数）
                val response = ApiClient.getService().searchRecipes(keyword = keyword)
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    recipes.clear()
                    
                    val data = response.body()?.data as? com.familyrecipes.android.data.model.PageResult<*>
                    data?.list?.let { list ->
                        recipes.addAll(list.filterIsInstance<com.familyrecipes.android.data.model.Recipe>())
                    }
                    
                    recipeAdapter.notifyDataSetChanged()
                    
                    if (!keyword.isNullOrEmpty()) {
                        if (recipes.isEmpty()) {
                            Toast.makeText(context, "没有找到相关菜谱", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "找到 ${recipes.size} 个菜谱", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    override fun performSearch(keyword: String) {
        // 菜谱页面：只搜索用户自己的菜谱，在当前页面显示
        binding.swipeRefresh.isRefreshing = true
        loadRecipes(keyword)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

