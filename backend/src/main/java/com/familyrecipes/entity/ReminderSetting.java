package com.familyrecipes.entity;

import lombok.Data;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * 提醒设置实体类
 */
@Data
public class ReminderSetting {
    private Long id;
    private Long userId;
    private Integer daysBeforeExpiry;
    private LocalTime reminderTime;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

