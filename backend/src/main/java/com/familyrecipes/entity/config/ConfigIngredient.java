package com.familyrecipes.entity.config;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 常见食材配置实体类
 */
@Data
public class ConfigIngredient {
    private Integer id;
    private String name;
    private String category;
    private Integer sortOrder;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

