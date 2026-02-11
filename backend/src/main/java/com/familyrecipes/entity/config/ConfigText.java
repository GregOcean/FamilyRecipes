package com.familyrecipes.entity.config;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统文本配置实体类
 */
@Data
public class ConfigText {
    private Integer id;
    private String configKey;
    private String configValue;
    private String description;
    private String category;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

