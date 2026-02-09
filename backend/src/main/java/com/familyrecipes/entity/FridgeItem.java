package com.familyrecipes.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 冰箱食材实体类
 */
@Data
public class FridgeItem {
    private Long id;
    private Long userId;
    private Long ingredientId;
    private String amount;
    private LocalDate purchaseDate;
    private LocalDate expiryDate;
    private String storageLocation;
    private String status;  // normal, expiring, expired, consumed
    private LocalDateTime consumedAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 关联数据
    private Ingredient ingredient;
    
    // 状态常量
    public static final String STATUS_NORMAL = "normal";
    public static final String STATUS_EXPIRING = "expiring";
    public static final String STATUS_EXPIRED = "expired";
    public static final String STATUS_CONSUMED = "consumed";
}

