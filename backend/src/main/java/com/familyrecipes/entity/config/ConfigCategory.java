package com.familyrecipes.entity.config;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 食材分类配置实体类
 */
@Data
public class ConfigCategory {
    private Integer id;
    private String name;
    private Integer sortOrder;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

