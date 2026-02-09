package com.familyrecipes.entity;

import lombok.Data;

/**
 * 菜谱标签实体类
 */
@Data
public class RecipeTag {
    private Long id;
    private Long recipeId;
    private String tagType;  // meal_time, dish_type, main_ingredient, special
    private String tagValue;
    
    // 标签类型常量
    public static final String TYPE_MEAL_TIME = "meal_time";  // 时段：早餐/午餐/晚餐/夜宵/点心
    public static final String TYPE_DISH_TYPE = "dish_type";  // 类型：汤/炒菜/面点/糕点
    public static final String TYPE_MAIN_INGREDIENT = "main_ingredient";  // 主食材：牛/羊/鸡/米/面
    public static final String TYPE_SPECIAL = "special";  // 特殊需求：无葱姜蒜/宝宝餐
}

