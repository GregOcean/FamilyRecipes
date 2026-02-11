package com.familyrecipes.entity;

import lombok.Data;

/**
 * 食材实体类
 */
@Data
public class Ingredient {
    private Long id;
    private String name;
    private String category;
    private String unit;  // 单位（如：个、瓶、克、袋等）
}

