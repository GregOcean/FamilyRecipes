-- 初始化默认用户数据（用于开发阶段）

-- 插入默认用户（ID=1），密码为 123456
INSERT INTO `user` (id, email, password, username, avatar, created_at, updated_at)
VALUES (1, 'default@familyrecipes.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1VZjV4D8KQiwQNQON9gABU1Q2TaQZB6', '家肴用户_DEV', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    email = VALUES(email),
    username = VALUES(username),
    updated_at = VALUES(updated_at);

-- 说明：
-- 密码：123456（已使用BCrypt加密）
-- 邮箱：default@familyrecipes.com
-- 用户名：家肴用户_DEV
-- 用途：开发阶段不登录时使用此默认用户记录操作

