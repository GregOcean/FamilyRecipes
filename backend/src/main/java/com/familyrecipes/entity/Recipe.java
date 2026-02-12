package com.familyrecipes.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜谱实体类
 */
@Data
public class Recipe {
    private Long id;
    private String name;
    private String description;
    private String coverImage;
    private Integer cookingTime;
    private Integer difficulty;
    private Integer servings;
    private Long creatorId;
    private Integer viewCount;
    private Integer favoriteCount;
    private Integer dislikeCount;
    private Integer recentlyCookedCount;
    private LocalDateTime lastCookedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 关联数据（不存数据库，用于返回）
    private User creator;
    private List<RecipeTag> tags;
    private List<RecipeIngredient> ingredients;
    private List<CookingStep> steps;
    private List<ExternalRecipe> externalRecipes;
    private List<User> cooks; // 会做这道菜的人
    private Boolean isFavorite; // 当前用户是否收藏
    private Boolean isDisliked; // 当前用户是否差评
}

