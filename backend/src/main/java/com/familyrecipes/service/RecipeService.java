package com.familyrecipes.service;

import com.familyrecipes.common.PageResult;
import com.familyrecipes.entity.*;
import com.familyrecipes.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜谱服务
 */
@Service
public class RecipeService {

    @Autowired
    private RecipeMapper recipeMapper;

    @Autowired
    private RecipeTagMapper recipeTagMapper;

    @Autowired
    private RecipeIngredientMapper recipeIngredientMapper;

    @Autowired
    private CookingStepMapper cookingStepMapper;

    @Autowired
    private ExternalRecipeMapper externalRecipeMapper;

    @Autowired
    private RecipeCookMapper recipeCookMapper;

    @Autowired
    private UserFavoriteMapper userFavoriteMapper;
    
    @Autowired
    private UserDislikeMapper userDislikeMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 创建菜谱
     */
    @Transactional
    public Recipe createRecipe(Recipe recipe, List<RecipeTag> tags, 
                               List<RecipeIngredient> ingredients,
                               List<CookingStep> steps,
                               List<Long> cookUserIds) {
        // 插入菜谱
        recipeMapper.insert(recipe);
        Long recipeId = recipe.getId();

        // 插入标签
        if (tags != null) {
            for (RecipeTag tag : tags) {
                tag.setRecipeId(recipeId);
                recipeTagMapper.insert(tag);
            }
        }

        // 插入食材
        if (ingredients != null) {
            for (RecipeIngredient ingredient : ingredients) {
                ingredient.setRecipeId(recipeId);
                recipeIngredientMapper.insert(ingredient);
            }
        }

        // 插入步骤
        if (steps != null) {
            for (CookingStep step : steps) {
                step.setRecipeId(recipeId);
                cookingStepMapper.insert(step);
            }
        }

        // 插入会做的人
        if (cookUserIds != null) {
            for (Long userId : cookUserIds) {
                recipeCookMapper.insert(recipeId, userId, 3);
            }
        }

        return getRecipeDetail(recipeId, recipe.getCreatorId());
    }

    /**
     * 更新菜谱
     */
    @Transactional
    public Recipe updateRecipe(Recipe recipe, List<RecipeTag> tags,
                               List<RecipeIngredient> ingredients,
                               List<CookingStep> steps,
                               List<Long> cookUserIds) {
        recipeMapper.update(recipe);
        Long recipeId = recipe.getId();

        // 删除并重新插入标签
        recipeTagMapper.deleteByRecipeId(recipeId);
        if (tags != null) {
            for (RecipeTag tag : tags) {
                tag.setRecipeId(recipeId);
                recipeTagMapper.insert(tag);
            }
        }

        // 删除并重新插入食材
        recipeIngredientMapper.deleteByRecipeId(recipeId);
        if (ingredients != null) {
            for (RecipeIngredient ingredient : ingredients) {
                ingredient.setRecipeId(recipeId);
                recipeIngredientMapper.insert(ingredient);
            }
        }

        // 删除并重新插入步骤
        cookingStepMapper.deleteByRecipeId(recipeId);
        if (steps != null) {
            for (CookingStep step : steps) {
                step.setRecipeId(recipeId);
                cookingStepMapper.insert(step);
            }
        }

        return getRecipeDetail(recipeId, recipe.getCreatorId());
    }

    /**
     * 删除菜谱
     */
    @Transactional
    public void deleteRecipe(Long recipeId, Long userId) {
        Recipe recipe = recipeMapper.findById(recipeId);
        if (recipe == null) {
            throw new RuntimeException("菜谱不存在");
        }
        if (!recipe.getCreatorId().equals(userId)) {
            throw new RuntimeException("无权删除此菜谱");
        }
        recipeMapper.delete(recipeId);
    }

    /**
     * 获取菜谱详情
     */
    public Recipe getRecipeDetail(Long recipeId, Long currentUserId) {
        Recipe recipe = recipeMapper.findById(recipeId);
        if (recipe == null) {
            return null;
        }

        // 加载关联数据
        recipe.setCreator(userMapper.findById(recipe.getCreatorId()));
        recipe.setTags(recipeTagMapper.findByRecipeId(recipeId));
        recipe.setIngredients(recipeIngredientMapper.findByRecipeId(recipeId));
        recipe.setSteps(cookingStepMapper.findByRecipeId(recipeId));
        recipe.setExternalRecipes(externalRecipeMapper.findByRecipeId(recipeId));
        recipe.setCooks(recipeCookMapper.findCooksByRecipeId(recipeId));

        // 设置当前用户是否收藏
        if (currentUserId != null) {
            boolean isFavorite = userFavoriteMapper.existsByUserAndRecipe(currentUserId, recipeId) > 0;
            recipe.setIsFavorite(isFavorite);
        }

        // 增加浏览数
        recipeMapper.incrementViewCount(recipeId);

        return recipe;
    }

    /**
     * 搜索菜谱
     */
    public PageResult<Recipe> searchRecipes(String keyword, String tagType, String tagValue,
                                           List<Long> ingredientIds, Long creatorId,
                                           Long favoritedByUserId, Long cookedByUserId,
                                           String creatorUsername, String orderBy,
                                           Integer pageNum, Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", keyword);
        params.put("tagType", tagType);
        params.put("tagValue", tagValue);
        params.put("ingredientIds", ingredientIds);
        params.put("creatorId", creatorId);
        params.put("favoritedByUserId", favoritedByUserId);
        params.put("cookedByUserId", cookedByUserId);
        params.put("creatorUsername", creatorUsername);
        params.put("orderBy", orderBy);
        params.put("offset", (pageNum - 1) * pageSize);
        params.put("limit", pageSize);

        List<Recipe> recipes = recipeMapper.search(params);
        Long total = recipeMapper.countSearch(params);

        // 加载创建者信息
        for (Recipe recipe : recipes) {
            recipe.setCreator(userMapper.findById(recipe.getCreatorId()));
            recipe.setTags(recipeTagMapper.findByRecipeId(recipe.getId()));
        }

        return new PageResult<>(recipes, total, pageNum, pageSize);
    }

    /**
     * 收藏/取消收藏菜谱
     */
    @Transactional
    public boolean toggleFavorite(Long userId, Long recipeId) {
        int exists = userFavoriteMapper.existsByUserAndRecipe(userId, recipeId);
        if (exists > 0) {
            // 取消收藏
            userFavoriteMapper.delete(userId, recipeId);
            recipeMapper.decrementFavoriteCount(recipeId);
            return false;
        } else {
            // 添加收藏
            userFavoriteMapper.insert(userId, recipeId);
            recipeMapper.incrementFavoriteCount(recipeId);
            return true;
        }
    }
    
    /**
     * 差评/取消差评菜谱
     */
    @Transactional
    public boolean toggleDislike(Long userId, Long recipeId) {
        int exists = userDislikeMapper.existsByUserAndRecipe(userId, recipeId);
        if (exists > 0) {
            // 取消差评
            userDislikeMapper.delete(userId, recipeId);
            recipeMapper.decrementDislikeCount(recipeId);
            return false;
        } else {
            // 添加差评
            userDislikeMapper.insert(userId, recipeId);
            recipeMapper.incrementDislikeCount(recipeId);
            return true;
        }
    }

    /**
     * 添加外链食谱
     */
    public void addExternalRecipe(ExternalRecipe externalRecipe) {
        externalRecipeMapper.insert(externalRecipe);
    }

    /**
     * 记录烹饪（增加烹饪次数）
     */
    @Transactional
    public void recordCooking(Long recipeId) {
        recipeMapper.incrementCookedCount(recipeId);
    }

    /**
     * 获取最近常做的菜
     */
    public List<Recipe> getRecentlyCooked(int limit) {
        return recipeMapper.findRecentlyCooked(limit);
    }

    /**
     * 获取所有标签值
     */
    public List<String> getTagValues(String tagType) {
        return recipeTagMapper.findDistinctValuesByType(tagType);
    }
}

