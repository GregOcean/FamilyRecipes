package com.familyrecipes.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 群组实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupChat {
    private Long id;
    private String name;
    private String avatar;
    private Long creatorId;
    private Long managerId;  // 群管理员ID（有且只有1个）
    private Integer memberCount;
    private Integer maxMembers;  // 默认5，会员可扩展
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 关联查询字段（非数据库字段）
    private List<GroupMember> members;  // 群成员列表
    private Message lastMessage;  // 最后一条消息
    private Integer unreadCount;  // 未读消息数（针对当前用户）
}

