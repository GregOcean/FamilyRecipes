-- ==================== 社交功能群组角色调整 ====================
-- 从三角色（owner/admin/member）简化为二角色（manager/member）
-- 最大成员数从100改为5

-- 1. 修改group_chat表，添加manager_id字段并调整max_members默认值
ALTER TABLE `group_chat` 
ADD COLUMN `manager_id` BIGINT NOT NULL COMMENT '群管理员ID（有且只有1个）' AFTER `creator_id`,
ADD CONSTRAINT `fk_group_manager` FOREIGN KEY (`manager_id`) REFERENCES `user`(`id`),
ADD INDEX `idx_manager` (`manager_id`),
MODIFY COLUMN `max_members` INT DEFAULT 5 COMMENT '最大成员数（默认5人，会员可扩展）';

-- 2. 将已有群组的manager_id设置为creator_id
UPDATE `group_chat` SET `manager_id` = `creator_id`;

-- 3. 修改group_member表的role枚举值
ALTER TABLE `group_member` 
MODIFY COLUMN `role` ENUM('manager', 'member') DEFAULT 'member' 
COMMENT '角色：manager-群管理员，member-普通成员';

-- 4. 更新已有成员的角色（owner和admin统一改为manager）
UPDATE `group_member` SET `role` = 'manager' WHERE `role` IN ('owner', 'admin');

-- 5. 验证数据
SELECT 
    g.id,
    g.name,
    g.creator_id,
    g.manager_id,
    g.member_count,
    g.max_members
FROM `group_chat` g
LIMIT 10;

SELECT 
    gm.id,
    gm.group_id,
    gm.user_id,
    gm.role,
    u.username
FROM `group_member` gm
LEFT JOIN `user` u ON gm.user_id = u.id
LIMIT 10;

