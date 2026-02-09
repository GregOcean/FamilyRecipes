package com.familyrecipes.entity;

import lombok.Data;

/**
 * 烹饪步骤实体类
 */
@Data
public class CookingStep {
    private Long id;
    private Long recipeId;
    private Integer stepNumber;
    private String description;
    private String image;
    private Integer duration;
}

