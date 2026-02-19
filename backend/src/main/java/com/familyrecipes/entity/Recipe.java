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
    
    // 权限相关
    private String visibility; // public-公开, private-仅自己, group-群组
    private String sharedGroupIds; // 分享的群组ID列表（JSON数组）
    private Long ownerGroupId; // 所属群组ID
    
    // 关联数据（不存数据库，用于返回）
    private User creator;
    private List<RecipeTag> tags;
    private List<RecipeIngredient> ingredients;
    private List<CookingStep> steps;
    private List<ExternalRecipe> externalRecipes;
    private List<User> cooks; // 会做这道菜的人
    private Boolean isFavorite; // 当前用户是否收藏
    private Boolean isDisliked; // 当前用户是否差评
    
    /**
     * 可见性枚举
     */
    public enum Visibility {
        PUBLIC("public", "公开可见"),
        PRIVATE("private", "仅自己可见"),
        GROUP("group", "群组可见");
        
        private final String code;
        private final String label;
        
        Visibility(String code, String label) {
            this.code = code;
            this.label = label;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getLabel() {
            return label;
        }
    }
}

