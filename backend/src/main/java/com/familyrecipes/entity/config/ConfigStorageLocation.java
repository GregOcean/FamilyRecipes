package com.familyrecipes.entity.config;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 存储位置配置实体类
 */
@Data
public class ConfigStorageLocation {
    private Integer id;
    private String name;
    private String icon;
    private Integer sortOrder;
    private Boolean isDefault;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

