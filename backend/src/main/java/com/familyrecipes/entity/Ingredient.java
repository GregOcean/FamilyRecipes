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
}

