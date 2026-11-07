package com.familyrecipes.android.ui.recommend

import com.familyrecipes.android.ui.design.UiStrings
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

    private var mode="discovery"
    private var loadJob:kotlinx.coroutines.Job?=null
    private var generation=0
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
            Toast.makeText(requireContext(), UiStrings.s(com.familyrecipes.android.R.string.ui_a6b4953271), Toast.LENGTH_SHORT).show()
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
        loadRecommendations()
    }
    
    private fun setupListeners() {
        binding.btnRandomRecommend.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle(UiStrings.s(com.familyrecipes.android.R.string.ui_9c9495d8a1))
                .setItems(arrayOf(UiStrings.s(com.familyrecipes.android.R.string.ui_f9352c8c09),UiStrings.s(com.familyrecipes.android.R.string.ui_9a8544afbf))) { _,index ->
                    mode=if(index==0) "discovery" else "inventory"
                    binding.btnRandomRecommend.text=if(index==0) UiStrings.s(com.familyrecipes.android.R.string.ui_74127479b9) else UiStrings.s(com.familyrecipes.android.R.string.ui_a64965a7fc)
                    loadRecommendations()
                }.show()
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            loadRecommendations()
        }
    }
    
    private fun loadRecommendations(keyword: String? = null) {
        val revision=++generation
        loadJob?.cancel()
        loadJob=viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = if (keyword.isNullOrEmpty()) {
                    // 推荐页面按浏览量或收藏量排序
                    ApiClient.getService().recommendations(mode,com.familyrecipes.android.data.local.PreferenceManager.currentKitchenId,java.util.UUID.randomUUID().toString())
                } else {
                    // 使用全局搜索，按相关性排序
                    ApiClient.getService().globalSearch(
                        keyword = keyword,
                        priorityType = "relevance"
                    )
                }
                
                if(revision!=generation)return@launch
                if (response.isSuccessful && response.body()?.code == 200) {
                    recipes.clear()
                    
                    if (keyword.isNullOrEmpty()) {
                        // 普通搜索，直接使用菜谱列表
                        val data = response.body()?.data as? com.familyrecipes.android.data.model.PageResult<*>
                        data?.list?.let { list ->
                            recipes.addAll(list.filterIsInstance<com.familyrecipes.android.data.model.Recipe>())
                        }
                    } else {
                        // 全局搜索，提取type为recipe的项
                        val searchResult = response.body()?.data as? com.familyrecipes.android.data.model.GlobalSearchResult
                        
                        // 先打印日志，看看返回了什么
                        android.util.Log.d("RecommendFragment", "搜索结果: total=${searchResult?.total}, recipeCount=${searchResult?.recipeCount}, ingredientCount=${searchResult?.ingredientCount}")
                        android.util.Log.d("RecommendFragment", "items: ${searchResult?.items}")
                        
                        searchResult?.items?.filter { it.type == "recipe" }?.forEach { item ->
                            val recipe = com.familyrecipes.android.data.model.Recipe(
                                id = item.id,
                                name = item.name,
                                description = item.description,
                                coverImage = item.coverImageUrl,
                                cookingTime = item.cookTime,
                                difficulty = if (item.difficulty == "easy") 1 else if (item.difficulty == "medium") 2 else if (item.difficulty == "hard") 3 else 2,
                                servings = null,
                                creatorId = null,
                                viewCount = null,
                                favoriteCount = null,
                                dislikeCount = null,
                                recentlyCookedCount = null,
                                createdAt = null,
                                creator = null,
                                tags = null,
                                ingredients = null,
                                steps = null,
                                externalRecipes = null,
                                cooks = null,
                                isFavorite = null,
                                isDisliked = null
                            )
                            recipes.add(recipe)
                        }
                        
                        // 显示搜索统计
                        val totalCount = searchResult?.total ?: 0
                        val recipeCount = searchResult?.recipeCount ?: 0
                        val ingredientCount = searchResult?.ingredientCount ?: 0
                        
                        if (totalCount > 0) {
                            val message = if (recipeCount > 0 && ingredientCount > 0) {
                                "找到 ${recipeCount} 个菜谱, ${ingredientCount} 个食材"
                            } else if (recipeCount > 0) {
                                "找到 ${recipeCount} 个菜谱"
                            } else if (ingredientCount > 0) {
                                "找到 ${ingredientCount} 个食材（可在食材页面查看）"
                            } else {
                                UiStrings.s(com.familyrecipes.android.R.string.ui_14254561ee)
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    recipeAdapter.notifyDataSetChanged()
                    
                    if (recipes.isEmpty() && !keyword.isNullOrEmpty()) {
                        Toast.makeText(context, UiStrings.s(com.familyrecipes.android.R.string.ui_11548282f0), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, UiStrings.s(com.familyrecipes.android.R.string.ui_866b795eae), Toast.LENGTH_SHORT).show()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(context, UiStrings.f(com.familyrecipes.android.R.string.network_error_value, e.message), Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                if(revision==generation) _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }
    
    override fun performSearch(keyword: String) {
        // 推荐页面的搜索由 MainActivity 处理，跳转到 SearchResultActivity
        // 这个方法不应该被调用，但为了接口完整性保留
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

