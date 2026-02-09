package com.familyrecipes.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 烹饪记录实体类
 */
@Data
public class CookingHistory {
    private Long id;
    private Long recipeId;
    private Long userId;
    private LocalDateTime cookedAt;
    private Integer rating;
    private String notes;
    private String images;  // JSON数组字符串
    
    // 关联数据
    private Recipe recipe;
    private User user;
}

