package com.familyrecipes.model;

import com.familyrecipes.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 好友关系实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Friendship {
    private Long id;
    private Long userId;
    private Long friendId;
    private String nickname;  // 好友备注名
    private FriendshipStatus status;
    private LocalDateTime createdAt;
    
    // 关联查询字段（非数据库字段）
    private User friend;  // 好友信息
    
    public enum FriendshipStatus {
        pending,   // 待接受
        accepted,  // 已接受
        blocked    // 已屏蔽
    }
}

