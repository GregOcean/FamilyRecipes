-- 007_add_recipe_permissions.sql
-- 为菜谱添加权限管理功能

-- 添加权限类型列
ALTER TABLE recipe ADD COLUMN visibility VARCHAR(20) DEFAULT 'group' COMMENT '可见性：public-公开, private-仅自己, group-群组';

-- 添加分享的群组ID列表（JSON格式存储）
ALTER TABLE recipe ADD COLUMN shared_group_ids TEXT COMMENT '分享的群组ID列表（JSON数组）';

-- 添加所属群组ID（创建时的默认群组）
ALTER TABLE recipe ADD COLUMN owner_group_id BIGINT COMMENT '所属群组ID';

-- 添加索引以提升查询性能
CREATE INDEX idx_recipe_visibility ON recipe(visibility);
CREATE INDEX idx_recipe_owner_group_id ON recipe(owner_group_id);

