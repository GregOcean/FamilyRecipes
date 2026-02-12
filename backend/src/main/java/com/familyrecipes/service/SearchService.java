package com.familyrecipes.service;

import com.familyrecipes.entity.Ingredient;
import com.familyrecipes.entity.Recipe;
import com.familyrecipes.mapper.IngredientMapper;
import com.familyrecipes.mapper.RecipeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 全局搜索服务
 */
@Service
public class SearchService {
    
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    @Autowired
    private RecipeMapper recipeMapper;

    @Autowired
    private IngredientMapper ingredientMapper;

    /**
     * 全局搜索
     * @param keyword 搜索关键词
     * @param priorityType 优先类型：recipe、ingredient、relevance
     * @param pageNum 页码
     * @param pageSize 每页数量
     */
    public Map<String, Object> globalSearch(String keyword, String priorityType, Integer pageNum, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        
        log.info("开始搜索 - keyword: {}, priorityType: {}", keyword, priorityType);
        
        // 检查是否是tag搜索（如果关键词以#开头）
        boolean isTagSearch = keyword != null && keyword.startsWith("#");
        String searchTerm = isTagSearch ? keyword.substring(1) : keyword;
        
        log.info("isTagSearch: {}, searchTerm: {}", isTagSearch, searchTerm);
        
        // 搜索食材
        List<Ingredient> ingredients = isTagSearch ? new ArrayList<>() : ingredientMapper.searchIngredients(searchTerm);
        
        // 搜索菜谱
        List<Recipe> recipes;
        if (isTagSearch) {
            // 通过tag精确搜索
            log.info("通过tag搜索: {}", searchTerm);
            recipes = recipeMapper.searchByTag(searchTerm);
            log.info("找到 {} 个菜谱", recipes.size());
        } else {
            // 通过关键词模糊搜索
            log.info("通过关键词搜索: {}", searchTerm);
            recipes = recipeMapper.searchByKeyword(searchTerm);
            log.info("找到 {} 个菜谱", recipes.size());
        }
        
        // 根据优先类型排序
        List<Map<String, Object>> items = new ArrayList<>();
        
        switch (priorityType) {
            case "ingredient":
                // 食材优先：食材在前，菜谱在后
                addIngredients(items, ingredients);
                addRecipes(items, recipes);
                break;
            case "recipe":
                // 菜谱优先：菜谱在前，食材在后
                addRecipes(items, recipes);
                addIngredients(items, ingredients);
                break;
            case "relevance":
            default:
                // 相关性：混合排序（简单实现：按名称匹配度）
                List<Map<String, Object>> allItems = new ArrayList<>();
                addIngredients(allItems, ingredients);
                addRecipes(allItems, recipes);
                
                // 按相关性排序（优先完全匹配，然后是包含关键词）
                items = allItems.stream()
                    .sorted((a, b) -> {
                        String nameA = (String) a.get("name");
                        String nameB = (String) b.get("name");
                        
                        boolean exactMatchA = nameA.equals(keyword);
                        boolean exactMatchB = nameB.equals(keyword);
                        
                        if (exactMatchA && !exactMatchB) return -1;
                        if (!exactMatchA && exactMatchB) return 1;
                        
                        boolean startsWithA = nameA.startsWith(keyword);
                        boolean startsWithB = nameB.startsWith(keyword);
                        
                        if (startsWithA && !startsWithB) return -1;
                        if (!startsWithA && startsWithB) return 1;
                        
                        return 0;
                    })
                    .collect(Collectors.toList());
                break;
        }
        
        // 分页
        int total = items.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        
        List<Map<String, Object>> pagedItems = fromIndex < total 
            ? items.subList(fromIndex, toIndex) 
            : new ArrayList<>();
        
        result.put("items", pagedItems);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        result.put("ingredientCount", ingredients.size());
        result.put("recipeCount", recipes.size());
        
        return result;
    }
    
    private void addIngredients(List<Map<String, Object>> items, List<Ingredient> ingredients) {
        for (Ingredient ingredient : ingredients) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", "ingredient");
            item.put("id", ingredient.getId());
            item.put("name", ingredient.getName());
            item.put("category", ingredient.getCategory());
            items.add(item);
        }
    }
    
    private void addRecipes(List<Map<String, Object>> items, List<Recipe> recipes) {
        for (Recipe recipe : recipes) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", "recipe");
            item.put("id", recipe.getId());
            item.put("name", recipe.getName());
            item.put("description", recipe.getDescription());
            item.put("coverImageUrl", recipe.getCoverImage());  // 后端字段是coverImage，但API返回coverImageUrl
            item.put("difficulty", getDifficultyString(recipe.getDifficulty()));  // 转换为字符串
            item.put("cookTime", recipe.getCookingTime());  // 后端字段是cookingTime
            items.add(item);
        }
    }
    
    /**
     * 将难度等级转换为字符串
     */
    private String getDifficultyString(Integer difficulty) {
        if (difficulty == null) return null;
        switch (difficulty) {
            case 1: return "easy";
            case 2: return "medium";
            case 3: return "hard";
            default: return "medium";
        }
    }
}

