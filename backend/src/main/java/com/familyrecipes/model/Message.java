package com.familyrecipes.model;

import com.familyrecipes.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 消息实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private Long id;
    private String clientMessageId;
    private Long groupId;
    private Long senderId;
    private String content;
    private MessageType messageType;
    // A message timestamp is an instant. REST and WebSocket serialize it with a UTC offset.
    // Keep this contract specific to chat; recipe dates and scheduled meals have separate semantics.
    private Instant createdAt;
    
    // 关联查询字段（非数据库字段）
    private User sender;  // 发送者信息
    
    public enum MessageType {
        text,    // 文本消息
        sticker,
        image,   // 图片消息
        system   // 系统消息
    }
}

