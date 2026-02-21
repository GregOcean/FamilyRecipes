package com.familyrecipes.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存分类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCategory {
    private Long id;
    private String name;
    private Long parentId;
    private String icon;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 非数据库字段 - 用于返回子分类
    private List<InventoryCategory> children;
}

