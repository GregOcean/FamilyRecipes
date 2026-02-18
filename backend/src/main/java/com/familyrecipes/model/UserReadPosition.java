package com.familyrecipes.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户读取位置实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserReadPosition {
    private Long id;
    private Long userId;
    private Long groupId;
    private Long lastReadMessageId;
    private LocalDateTime lastReadAt;
}

