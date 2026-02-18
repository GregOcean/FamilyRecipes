package com.familyrecipes.model;

import com.familyrecipes.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 群组成员实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMember {
    private Long id;
    private Long groupId;
    private Long userId;
    private MemberRole role;
    private String nickname;  // 群昵称
    private LocalDateTime joinedAt;
    
    // 关联查询字段（非数据库字段）
    private User user;  // 用户信息
    
    public enum MemberRole {
        manager,   // 群管理员（有且只有1个）
        member     // 普通成员
    }
}

