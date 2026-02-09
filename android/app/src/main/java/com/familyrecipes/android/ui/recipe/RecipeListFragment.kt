package com.familyrecipes.android.ui.recipe

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
import com.familyrecipes.android.ui.adapter.RecipeAdapter
import kotlinx.coroutines.launch

/**
 * 菜谱列表Fragment
 */
class RecipeListFragment : Fragment() {

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
            // TODO: 跳转到详情
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recipeAdapter
        }
    }

    private fun setupListeners() {
        // 下拉刷新
        binding.swipeRefresh.setOnRefreshListener {
            loadRecipes()
        }
    }

    private fun loadRecipes() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().searchRecipes()
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data
                    recipes.clear()
                    data?.list?.let { recipes.addAll(it) }
                    recipeAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

