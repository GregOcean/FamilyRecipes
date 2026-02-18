package com.familyrecipes.model;

import com.familyrecipes.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private Long id;
    private Long groupId;
    private Long senderId;
    private String content;
    private MessageType messageType;
    private LocalDateTime createdAt;
    
    // 关联查询字段（非数据库字段）
    private User sender;  // 发送者信息
    
    public enum MessageType {
        text,    // 文本消息
        image,   // 图片消息
        system   // 系统消息
    }
}

