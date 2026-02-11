-- 创建配置相关表的迁移脚本
-- 执行日期：2026-02-11

-- 1. 常见食材配置表
CREATE TABLE IF NOT EXISTS `config_ingredients` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `name` VARCHAR(50) NOT NULL COMMENT '食材名称',
    `category` VARCHAR(20) COMMENT '分类',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `is_enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (`category`),
    INDEX idx_enabled (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='常见食材配置表';

-- 2. 存储位置配置表
CREATE TABLE IF NOT EXISTS `config_storage_locations` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `name` VARCHAR(50) NOT NULL COMMENT '存储位置名称',
    `icon` VARCHAR(50) COMMENT '图标标识',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `is_default` TINYINT(1) DEFAULT 0 COMMENT '是否默认',
    `is_enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_enabled (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存储位置配置表';

-- 3. 食材分类配置表
CREATE TABLE IF NOT EXISTS `config_categories` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `is_enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_enabled (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食材分类配置表';

-- 4. 系统文本配置表（用于存储各种显示文本）
CREATE TABLE IF NOT EXISTS `config_texts` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `config_key` VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    `config_value` VARCHAR(500) COMMENT '配置值',
    `description` VARCHAR(200) COMMENT '描述',
    `category` VARCHAR(50) COMMENT '配置分类',
    `is_enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_key (`config_key`),
    INDEX idx_category (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统文本配置表';

-- 插入常见食材数据
INSERT INTO `config_ingredients` (`name`, `category`, `sort_order`) VALUES
-- 肉类
('牛肉', '肉类', 1), ('牛腱子', '肉类', 2), ('牛里脊', '肉类', 3), ('牛排', '肉类', 4), ('牛腩', '肉类', 5),
('猪肉', '肉类', 11), ('猪里脊', '肉类', 12), ('猪五花', '肉类', 13), ('猪蹄', '肉类', 14), ('猪尾巴', '肉类', 15),
('羊肉', '肉类', 21), ('羊排', '肉类', 22), ('羊腿', '肉类', 23), ('羊蝎子', '肉类', 24),
('鸡肉', '肉类', 31), ('鸡腿', '肉类', 32), ('鸡翅', '肉类', 33), ('鸡胸肉', '肉类', 34), ('鸡爪', '肉类', 35),
('鸭肉', '肉类', 41), ('鸭腿', '肉类', 42), ('鸭翅', '肉类', 43), ('鸭舌', '肉类', 44), ('鸭血', '肉类', 45),

-- 海鲜水产
('鱼', '海鲜', 101), ('草鱼', '海鲜', 102), ('鲈鱼', '海鲜', 103), ('鲫鱼', '海鲜', 104), ('带鱼', '海鲜', 105),
('三文鱼', '海鲜', 106), ('鳕鱼', '海鲜', 107), ('黄鱼', '海鲜', 108),
('虾', '海鲜', 111), ('大虾', '海鲜', 112), ('基围虾', '海鲜', 113), ('龙虾', '海鲜', 114), ('虾仁', '海鲜', 115),
('螃蟹', '海鲜', 121), ('大闸蟹', '海鲜', 122), ('梭子蟹', '海鲜', 123),
('蛤蜊', '海鲜', 131), ('扇贝', '海鲜', 132), ('生蚝', '海鲜', 133), ('鱿鱼', '海鲜', 134),

-- 蔬菜类
('白菜', '蔬菜', 201), ('大白菜', '蔬菜', 202), ('娃娃菜', '蔬菜', 203), ('小白菜', '蔬菜', 204),
('菠菜', '蔬菜', 211), ('生菜', '蔬菜', 212), ('油麦菜', '蔬菜', 213), ('芹菜', '蔬菜', 214), ('香菜', '蔬菜', 215),
('番茄', '蔬菜', 221), ('西红柿', '蔬菜', 222), ('土豆', '蔬菜', 223), ('红薯', '蔬菜', 224), ('山药', '蔬菜', 225),
('黄瓜', '蔬菜', 231), ('冬瓜', '蔬菜', 232), ('南瓜', '蔬菜', 233), ('丝瓜', '蔬菜', 234), ('苦瓜', '蔬菜', 235),
('茄子', '蔬菜', 241), ('青椒', '蔬菜', 242), ('尖椒', '蔬菜', 243), ('彩椒', '蔬菜', 244), ('辣椒', '蔬菜', 245),
('豆角', '蔬菜', 251), ('四季豆', '蔬菜', 252), ('豇豆', '蔬菜', 253), ('荷兰豆', '蔬菜', 254), ('豌豆', '蔬菜', 255),
('胡萝卜', '蔬菜', 261), ('白萝卜', '蔬菜', 262), ('莲藕', '蔬菜', 263), ('竹笋', '蔬菜', 264),
('玉米', '蔬菜', 271), ('西兰花', '蔬菜', 272), ('菜花', '蔬菜', 273),
('蘑菇', '蔬菜', 281), ('香菇', '蔬菜', 282), ('平菇', '蔬菜', 283), ('金针菇', '蔬菜', 284), ('杏鲍菇', '蔬菜', 285),

-- 水果类
('苹果', '水果', 301), ('香蕉', '水果', 302), ('橙子', '水果', 303), ('橘子', '水果', 304),
('梨', '水果', 311), ('雪梨', '水果', 312), ('葡萄', '水果', 313), ('提子', '水果', 314),
('西瓜', '水果', 321), ('哈密瓜', '水果', 322), ('草莓', '水果', 323), ('蓝莓', '水果', 324),
('桃子', '水果', 331), ('芒果', '水果', 332), ('榴莲', '水果', 333), ('火龙果', '水果', 334),

-- 蛋奶类
('鸡蛋', '蛋奶', 401), ('鸭蛋', '蛋奶', 402), ('鹌鹑蛋', '蛋奶', 403),
('牛奶', '蛋奶', 411), ('纯牛奶', '蛋奶', 412), ('酸奶', '蛋奶', 413), ('奶酪', '蛋奶', 414),

-- 豆制品
('豆腐', '豆制品', 501), ('嫩豆腐', '豆制品', 502), ('老豆腐', '豆制品', 503), ('豆腐干', '豆制品', 504),

-- 主食类
('米', '主食', 601), ('大米', '主食', 602), ('面粉', '主食', 603), ('面条', '主食', 604),
('馒头', '主食', 611), ('花卷', '主食', 612), ('包子', '主食', 613), ('饺子', '主食', 614),

-- 调味品
('盐', '调味品', 701), ('糖', '调味品', 702), ('酱油', '调味品', 703), ('醋', '调味品', 704),
('料酒', '调味品', 711), ('食用油', '调味品', 712), ('葱', '调味品', 713), ('姜', '调味品', 714), ('蒜', '调味品', 715);

-- 插入存储位置数据
INSERT INTO `config_storage_locations` (`name`, `icon`, `sort_order`, `is_default`) VALUES
('冰箱冷藏', 'fridge_cold', 1, 1),
('冰箱冷冻', 'fridge_freeze', 2, 0),
('橱柜', 'cabinet', 3, 0),
('常温', 'room_temp', 4, 0),
('阴凉处', 'cool_place', 5, 0),
('冷藏室', 'fridge_compartment', 6, 0),
('冷冻室', 'freezer_compartment', 7, 0),
('保鲜层', 'fresh_compartment', 8, 0),
('蔬菜室', 'vegetable_compartment', 9, 0),
('冰柜', 'freezer', 10, 0);

-- 插入分类数据
INSERT INTO `config_categories` (`name`, `sort_order`) VALUES
('肉类', 1),
('海鲜', 2),
('蔬菜', 3),
('水果', 4),
('蛋奶', 5),
('豆制品', 6),
('主食', 7),
('调味品', 8),
('其他', 99);

-- 插入系统文本配置
INSERT INTO `config_texts` (`config_key`, `config_value`, `description`, `category`) VALUES
('status.normal', '正常', '食材状态-正常', 'status'),
('status.expiring', '即将过期', '食材状态-即将过期', 'status'),
('status.expired', '已过期', '食材状态-已过期', 'status'),
('status.consumed', '已消耗', '食材状态-已消耗', 'status'),
('button.consume', '已消耗', '按钮文本-标记已消耗', 'button'),
('button.delete', '删除', '按钮文本-删除', 'button'),
('button.save', '保存', '按钮文本-保存', 'button'),
('button.cancel', '取消', '按钮文本-取消', 'button'),
('button.back_to_edit', '返回修改', '按钮文本-返回修改', 'button'),
('button.confirm_save', '确认保存', '按钮文本-确认保存', 'button');

