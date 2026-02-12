package com.familyrecipes.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 外链食谱实体类
 */
@Data
public class ExternalRecipe {
    private Long id;
    private Long recipeId;
    private String title;
    private String url;
    private String source;
    private String thumbnail;  // 缩略图URL
    private Long addedBy;
    private LocalDateTime createdAt;
    
    // 关联数据
    private User user;
}

