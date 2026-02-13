-- 家庭菜谱管理系统数据库表结构（无外键约束版本）

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `email` VARCHAR(100) UNIQUE NOT NULL COMMENT '邮箱（登录账号）',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_email (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 菜谱表
CREATE TABLE IF NOT EXISTS `recipe` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '菜谱ID',
    `name` VARCHAR(100) NOT NULL COMMENT '菜名',
    `description` TEXT COMMENT '描述',
    `cover_image` VARCHAR(255) COMMENT '封面图片URL',
    `cooking_time` INT COMMENT '烹饪时间（分钟）',
    `difficulty` TINYINT COMMENT '难度（1-5）',
    `servings` INT COMMENT '份数',
    `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `favorite_count` INT DEFAULT 0 COMMENT '收藏次数',
    `dislike_count` INT DEFAULT 0 COMMENT '差评次数',
    `recently_cooked_count` INT DEFAULT 0 COMMENT '最近烹饪次数',
    `last_cooked_at` TIMESTAMP NULL COMMENT '最后烹饪时间',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_creator (`creator_id`),
    INDEX idx_name (`name`),
    INDEX idx_recently_cooked (`recently_cooked_count` DESC, `last_cooked_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱表';

-- 菜谱标签表
CREATE TABLE IF NOT EXISTS `recipe_tag` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    `recipe_id` BIGINT NOT NULL COMMENT '菜谱ID',
    `tag_type` VARCHAR(20) NOT NULL COMMENT '标签类型（meal_time/dish_type/main_ingredient/special）',
    `tag_value` VARCHAR(50) NOT NULL COMMENT '标签值',
    INDEX idx_recipe (`recipe_id`),
    INDEX idx_tag (`tag_type`, `tag_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱标签表';

-- 食材表
CREATE TABLE IF NOT EXISTS `ingredient` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '食材ID',
    `name` VARCHAR(50) NOT NULL COMMENT '食材名称',
    `category` VARCHAR(20) COMMENT '分类（蔬菜/肉类/调料等）',
    `unit` VARCHAR(20) COMMENT '常用单位（个、瓶、克、袋等）',
    INDEX idx_name (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食材表';

-- 菜谱食材关联表
CREATE TABLE IF NOT EXISTS `recipe_ingredient` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `recipe_id` BIGINT NOT NULL COMMENT '菜谱ID',
    `ingredient_id` BIGINT NOT NULL COMMENT '食材ID',
    `amount` VARCHAR(50) COMMENT '用量（如：200g，2个）',
    `is_main` BOOLEAN DEFAULT FALSE COMMENT '是否主要食材',
    INDEX idx_recipe (`recipe_id`),
    INDEX idx_ingredient (`ingredient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱食材关联表';

-- 烹饪步骤表
CREATE TABLE IF NOT EXISTS `cooking_step` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '步骤ID',
    `recipe_id` BIGINT NOT NULL COMMENT '菜谱ID',
    `step_number` INT NOT NULL COMMENT '步骤序号',
    `description` TEXT NOT NULL COMMENT '步骤描述',
    `image` VARCHAR(255) COMMENT '步骤图片URL',
    `duration` INT COMMENT '该步骤耗时（分钟）',
    INDEX idx_recipe (`recipe_id`, `step_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烹饪步骤表';

-- 外链食谱表
CREATE TABLE IF NOT EXISTS `external_recipe` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `recipe_id` BIGINT NOT NULL COMMENT '关联的本地菜谱ID',
    `title` VARCHAR(200) NOT NULL COMMENT '外链标题',
    `url` VARCHAR(500) NOT NULL COMMENT '外链URL',
    `source` VARCHAR(50) COMMENT '来源（如：下厨房、豆果）',
    `thumbnail` VARCHAR(500) COMMENT '缩略图URL',
    `added_by` BIGINT NOT NULL COMMENT '添加者ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_recipe (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外链食谱表';

-- 会做该菜的人（多对多）
CREATE TABLE IF NOT EXISTS `recipe_cook` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `recipe_id` BIGINT NOT NULL COMMENT '菜谱ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `skill_level` TINYINT DEFAULT 3 COMMENT '熟练度（1-5）',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_recipe_user (`recipe_id`, `user_id`),
    INDEX idx_user (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱厨师关联表';

-- 用户收藏表
CREATE TABLE IF NOT EXISTS `user_favorite` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `recipe_id` BIGINT NOT NULL COMMENT '菜谱ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    UNIQUE KEY uk_user_recipe (`user_id`, `recipe_id`),
    INDEX idx_recipe (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- 用户差评表
CREATE TABLE IF NOT EXISTS `user_dislike` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `recipe_id` BIGINT NOT NULL COMMENT '菜谱ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '差评时间',
    UNIQUE KEY uk_user_recipe (`user_id`, `recipe_id`),
    INDEX idx_recipe (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户差评表';

-- 冰箱食材表
CREATE TABLE IF NOT EXISTS `fridge_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `ingredient_id` BIGINT NOT NULL COMMENT '食材ID',
    `amount` VARCHAR(50) COMMENT '数量',
    `purchase_date` DATE COMMENT '购买日期',
    `expiry_date` DATE NOT NULL COMMENT '过期日期',
    `storage_location` VARCHAR(50) COMMENT '存储位置（冷藏/冷冻/常温）',
    `status` VARCHAR(20) DEFAULT 'normal' COMMENT '状态（normal/expiring/expired/consumed）',
    `consumed_at` TIMESTAMP NULL COMMENT '消耗时间',
    `notes` VARCHAR(255) COMMENT '备注',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user (`user_id`),
    INDEX idx_expiry (`expiry_date`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='冰箱食材表';

-- 提醒设置表
CREATE TABLE IF NOT EXISTS `reminder_setting` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `days_before_expiry` INT DEFAULT 3 COMMENT '提前多少天提醒',
    `reminder_time` TIME DEFAULT '09:00:00' COMMENT '提醒时间',
    `enabled` BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提醒设置表';

-- 烹饪记录表
CREATE TABLE IF NOT EXISTS `cooking_history` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `recipe_id` BIGINT NOT NULL COMMENT '菜谱ID',
    `user_id` BIGINT NOT NULL COMMENT '烹饪者ID',
    `cooked_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '烹饪时间',
    `rating` TINYINT COMMENT '评分（1-5）',
    `notes` TEXT COMMENT '笔记',
    `images` TEXT COMMENT '成品图片URL（JSON数组）',
    INDEX idx_recipe (`recipe_id`, `cooked_at` DESC),
    INDEX idx_user (`user_id`, `cooked_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烹饪记录表';

