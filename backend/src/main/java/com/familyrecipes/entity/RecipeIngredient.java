package com.familyrecipes.entity;

import lombok.Data;

/**
 * 菜谱食材关联实体类
 */
@Data
public class RecipeIngredient {
    private Long id;
    private Long recipeId;
    private Long ingredientId;
    private String amount;
    private Boolean isMain;
    
    // 关联数据
    private Ingredient ingredient;
}

