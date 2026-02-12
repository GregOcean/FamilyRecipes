package com.familyrecipes.controller;

import com.familyrecipes.common.PageResult;
import com.familyrecipes.common.Result;
import com.familyrecipes.entity.*;
import com.familyrecipes.service.RecipeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜谱控制器
 */
@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    @Autowired
    private RecipeService recipeService;

    /**
     * 创建菜谱
     */
    @PostMapping
    public Result<Recipe> createRecipe(@RequestBody CreateRecipeRequest request, 
                                       HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            request.getRecipe().setCreatorId(userId);
            
            Recipe recipe = recipeService.createRecipe(
                request.getRecipe(),
                request.getTags(),
                request.getIngredients(),
                request.getSteps(),
                request.getCookUserIds()
            );
            return Result.success(recipe);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新菜谱
     */
    @PutMapping("/{id}")
    public Result<Recipe> updateRecipe(@PathVariable Long id,
                                       @RequestBody CreateRecipeRequest request,
                                       HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            request.getRecipe().setId(id);
            request.getRecipe().setCreatorId(userId);
            
            Recipe recipe = recipeService.updateRecipe(
                request.getRecipe(),
                request.getTags(),
                request.getIngredients(),
                request.getSteps(),
                request.getCookUserIds()
            );
            return Result.success(recipe);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除菜谱
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteRecipe(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            recipeService.deleteRecipe(id, userId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取菜谱详情
     */
    @GetMapping("/{id}")
    public Result<Recipe> getRecipeDetail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Recipe recipe = recipeService.getRecipeDetail(id, userId);
        if (recipe == null) {
            return Result.error(404, "菜谱不存在");
        }
        return Result.success(recipe);
    }

    /**
     * 搜索菜谱
     */
    @GetMapping("/search")
    public Result<PageResult<Recipe>> searchRecipes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tagType,
            @RequestParam(required = false) String tagValue,
            @RequestParam(required = false) List<Long> ingredientIds,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) String creatorUsername,
            @RequestParam(required = false) String orderBy,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        PageResult<Recipe> result = recipeService.searchRecipes(
            keyword, tagType, tagValue, ingredientIds, creatorId,
            null, null, creatorUsername, orderBy, pageNum, pageSize
        );
        return Result.success(result);
    }

    /**
     * 获取我创建的菜谱
     */
    @GetMapping("/my-created")
    public Result<PageResult<Recipe>> getMyCreatedRecipes(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        PageResult<Recipe> result = recipeService.searchRecipes(
            null, null, null, null, userId,
            null, null, null, null, pageNum, pageSize
        );
        return Result.success(result);
    }

    /**
     * 获取我收藏的菜谱
     */
    @GetMapping("/my-favorites")
    public Result<PageResult<Recipe>> getMyFavorites(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        PageResult<Recipe> result = recipeService.searchRecipes(
            null, null, null, null, null,
            userId, null, null, null, pageNum, pageSize
        );
        return Result.success(result);
    }

    /**
     * 收藏/取消收藏
     */
    @PostMapping("/{id}/favorite")
    public Result<Boolean> toggleFavorite(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            boolean isFavorite = recipeService.toggleFavorite(userId, id);
            return Result.success(isFavorite);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 差评/取消差评
     */
    @PostMapping("/{id}/dislike")
    public Result<Boolean> toggleDislike(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            boolean isDisliked = recipeService.toggleDislike(userId, id);
            return Result.success(isDisliked);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 添加外链食谱
     */
    @PostMapping("/{id}/external-links")
    public Result<Void> addExternalRecipe(@PathVariable Long id,
                                           @RequestBody ExternalRecipe externalRecipe,
                                           HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            externalRecipe.setRecipeId(id);
            externalRecipe.setAddedBy(userId);
            recipeService.addExternalRecipe(externalRecipe);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 记录烹饪
     */
    @PostMapping("/{id}/cook")
    public Result<Void> recordCooking(@PathVariable Long id) {
        try {
            recipeService.recordCooking(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取最近常做的菜
     */
    @GetMapping("/recently-cooked")
    public Result<List<Recipe>> getRecentlyCooked(@RequestParam(defaultValue = "10") int limit) {
        List<Recipe> recipes = recipeService.getRecentlyCooked(limit);
        return Result.success(recipes);
    }

    /**
     * 获取标签选项
     */
    @GetMapping("/tags/{tagType}")
    public Result<List<String>> getTagValues(@PathVariable String tagType) {
        List<String> values = recipeService.getTagValues(tagType);
        return Result.success(values);
    }

    @Data
    static class CreateRecipeRequest {
        private Recipe recipe;
        private List<RecipeTag> tags;
        private List<RecipeIngredient> ingredients;
        private List<CookingStep> steps;
        private List<Long> cookUserIds;
    }
}

