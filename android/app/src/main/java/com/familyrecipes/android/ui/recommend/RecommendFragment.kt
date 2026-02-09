package com.familyrecipes.android.ui.recommend

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
import com.familyrecipes.android.databinding.FragmentRecommendBinding
import com.familyrecipes.android.ui.SearchableFragment
import com.familyrecipes.android.ui.adapter.RecipeAdapter
import com.familyrecipes.android.ui.recipe.RecipeDetailActivity
import kotlinx.coroutines.launch

/**
 * 推荐Fragment
 */
class RecommendFragment : Fragment(), SearchableFragment {

    private var _binding: FragmentRecommendBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var recipeAdapter: RecipeAdapter
    private val recipes = mutableListOf<Recipe>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecommendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupListeners()
        loadRecommendations()
    }
    
    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter(recipes) { recipe ->
            openRecipeDetail(recipe)
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recipeAdapter
        }
    }
    
    private fun openRecipeDetail(recipe: Recipe) {
        if (recipe.id == null) {
            Toast.makeText(requireContext(), "菜谱ID无效", Toast.LENGTH_SHORT).show()
            return
        }
        
        val intent = Intent(requireContext(), RecipeDetailActivity::class.java).apply {
            putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.id!!)
        }
        startActivity(intent)
    }
    
    private fun setupListeners() {
        binding.btnRandomRecommend.setOnClickListener {
            loadRecommendations()
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            loadRecommendations()
        }
    }
    
    private fun loadRecommendations(keyword: String? = null) {
        lifecycleScope.launch {
            try {
                // 使用相同的搜索接口，但可以添加不同的排序规则
                val response = if (keyword.isNullOrEmpty()) {
                    // 推荐页面按浏览量或收藏量排序
                    ApiClient.getService().searchRecipes(orderBy = "view_count")
                } else {
                    ApiClient.getService().searchRecipes(keyword = keyword)
                }
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data
                    recipes.clear()
                    data?.list?.let { recipes.addAll(it) }
                    recipeAdapter.notifyDataSetChanged()
                    
                    if (recipes.isEmpty() && !keyword.isNullOrEmpty()) {
                        Toast.makeText(context, "没有找到相关菜谱", Toast.LENGTH_SHORT).show()
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
        binding.swipeRefresh.isRefreshing = true
        loadRecommendations(keyword)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

