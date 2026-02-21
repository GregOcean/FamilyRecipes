-- 创建库存分类表
CREATE TABLE IF NOT EXISTS inventory_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT NULL COMMENT '父分类ID，NULL表示一级分类',
    icon VARCHAR(50) DEFAULT NULL COMMENT '图标名称',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存分类表';

-- 插入默认的一级分类
INSERT INTO inventory_categories (id, name, icon, sort_order, parent_id) VALUES
(1, '原料', '🥕', 1, NULL),
(2, '成品', '🍱', 2, NULL),
(3, '饮品', '🥤', 3, NULL),
(4, '面点', '🥐', 4, NULL),
(5, '调味料', '🧂', 5, NULL),
(6, '零食', '🍪', 6, NULL),
(7, '其他', '📦', 7, NULL);

-- 插入二级分类 - 原料
INSERT INTO inventory_categories (name, icon, sort_order, parent_id) VALUES
('蔬菜', '🥬', 1, 1),
('肉类', '🥩', 2, 1),
('海鲜', '🦐', 3, 1),
('水果', '🍎', 4, 1),
('蛋奶', '🥚', 5, 1);

-- 插入二级分类 - 成品
INSERT INTO inventory_categories (name, icon, sort_order, parent_id) VALUES
('剩菜', '🍲', 1, 2),
('外卖', '🥡', 2, 2),
('熟食', '🍗', 3, 2);

-- 插入二级分类 - 饮品
INSERT INTO inventory_categories (name, icon, sort_order, parent_id) VALUES
('酒精', '🍺', 1, 3),
('无糖', '💧', 2, 3),
('茶饮', '🍵', 3, 3),
('汽水', '🥤', 4, 3),
('乳制品', '🥛', 5, 3);

-- 插入二级分类 - 面点
INSERT INTO inventory_categories (name, icon, sort_order, parent_id) VALUES
('中式', '🥟', 1, 4),
('西式', '🥐', 2, 4);

-- 插入二级分类 - 调味料
INSERT INTO inventory_categories (name, icon, sort_order, parent_id) VALUES
('酱料', '🫙', 1, 5),
('干货', '🌰', 2, 5),
('香料', '🌿', 3, 5);

-- 插入二级分类 - 零食
INSERT INTO inventory_categories (name, icon, sort_order, parent_id) VALUES
('膨化', '🍿', 1, 6),
('糖果', '🍬', 2, 6),
('坚果', '🌰', 3, 6);

-- 为ingredients表添加category_id字段
ALTER TABLE ingredients 
ADD COLUMN category_id BIGINT DEFAULT NULL COMMENT '分类ID，关联inventory_categories表（无外键约束）' 
AFTER name;

-- 添加索引以提高查询性能
ALTER TABLE ingredients 
ADD INDEX idx_category_id (category_id);

-- 为现有食材设置默认分类（蔬菜）
UPDATE ingredients 
SET category_id = (SELECT id FROM inventory_categories WHERE name = '蔬菜' AND parent_id = 1 LIMIT 1)
WHERE category_id IS NULL;

