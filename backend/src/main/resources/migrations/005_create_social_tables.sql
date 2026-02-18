-- ==================== 社交功能相关表 ====================

-- 好友关系表
CREATE TABLE IF NOT EXISTS `friendship` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '好友关系ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `friend_id` BIGINT NOT NULL COMMENT '好友ID',
    `nickname` VARCHAR(50) COMMENT '好友备注名',
    `status` ENUM('pending', 'accepted', 'blocked') DEFAULT 'accepted' COMMENT '好友状态',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`friend_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_friendship` (`user_id`, `friend_id`),
    INDEX `idx_user` (`user_id`),
    INDEX `idx_friend` (`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';

-- 群组表
CREATE TABLE IF NOT EXISTS `group_chat` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '群组ID',
    `name` VARCHAR(100) NOT NULL COMMENT '群组名称',
    `avatar` VARCHAR(500) COMMENT '群组头像URL',
    `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
    `manager_id` BIGINT NOT NULL COMMENT '群管理员ID（有且只有1个）',
    `member_count` INT DEFAULT 0 COMMENT '成员数量',
    `max_members` INT DEFAULT 5 COMMENT '最大成员数（默认5人，会员可扩展）',
    `description` VARCHAR(500) COMMENT '群组描述',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`creator_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`manager_id`) REFERENCES `user`(`id`),
    INDEX `idx_creator` (`creator_id`),
    INDEX `idx_manager` (`manager_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组表';

-- 群组成员表
CREATE TABLE IF NOT EXISTS `group_member` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `group_id` BIGINT NOT NULL COMMENT '群组ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` ENUM('manager', 'member') DEFAULT 'member' COMMENT '角色：manager-群管理员，member-普通成员',
    `nickname` VARCHAR(50) COMMENT '群昵称',
    `joined_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    FOREIGN KEY (`group_id`) REFERENCES `group_chat`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
    INDEX `idx_group` (`group_id`),
    INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组成员表';

-- 消息表
CREATE TABLE IF NOT EXISTS `message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    `group_id` BIGINT NOT NULL COMMENT '群组ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `message_type` ENUM('text', 'image', 'system') DEFAULT 'text' COMMENT '消息类型',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    FOREIGN KEY (`group_id`) REFERENCES `group_chat`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`sender_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    INDEX `idx_group_time` (`group_id`, `created_at`),
    INDEX `idx_sender` (`sender_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 用户最后读取消息位置表（用于未读消息计数）
CREATE TABLE IF NOT EXISTS `user_read_position` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `group_id` BIGINT NOT NULL COMMENT '群组ID',
    `last_read_message_id` BIGINT COMMENT '最后读取的消息ID',
    `last_read_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '最后读取时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`group_id`) REFERENCES `group_chat`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_group` (`user_id`, `group_id`),
    INDEX `idx_user` (`user_id`),
    INDEX `idx_group` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户读取位置表';

