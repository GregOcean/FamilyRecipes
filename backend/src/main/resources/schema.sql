SET NAMES utf8mb4;
-- Fresh database only. Run manually once; never on application startup.
-- 家庭菜谱管理系统数据库表结构

-- 用户表
CREATE TABLE `user` (
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
CREATE TABLE `recipe` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '菜谱ID',
    `name` VARCHAR(100) NOT NULL COMMENT '菜名',
    `description` TEXT COMMENT '描述',
    `cover_image` VARCHAR(255) COMMENT '封面图片URL',
    `cooking_time` INT COMMENT '烹饪时间（分钟）',
    `difficulty` TINYINT COMMENT '难度（1-5）',
    `servings` INT COMMENT '份数',
    `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `dislike_count` INT NOT NULL DEFAULT 0,
    `visibility` VARCHAR(20) NOT NULL DEFAULT 'private',
    `shared_group_ids` JSON,
    `owner_group_id` BIGINT,
    `favorite_count` INT DEFAULT 0 COMMENT '收藏次数',
    `recently_cooked_count` INT DEFAULT 0 COMMENT '最近烹饪次数',
    `last_cooked_at` TIMESTAMP NULL COMMENT '最后烹饪时间',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`creator_id`) REFERENCES `user`(`id`),
    INDEX idx_creator (`creator_id`),
    INDEX idx_name (`name`),
    INDEX idx_recently_cooked (`recently_cooked_count` DESC, `last_cooked_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱表';

-- 菜谱标签表
CREATE TABLE `recipe_tag` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    `recipe_id` BIGINT NOT NULL COMMENT '菜谱ID',
    `tag_type` VARCHAR(20) NOT NULL COMMENT '标签类型（meal_time/dish_type/main_ingredient/special）',
    `tag_value` VARCHAR(50) NOT NULL COMMENT '标签值',
    FOREIGN KEY (`recipe_id`) REFERENCES `recipe`(`id`) ON DELETE CASCADE,
    INDEX idx_recipe (`recipe_id`),
    INDEX idx_tag (`tag_type`, `tag_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱标签表';

-- 食材表
CREATE TABLE `ingredient` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '食材ID',
    `name` VARCHAR(50) NOT NULL COMMENT '食材名称',
    `category` VARCHAR(20) COMMENT '分类（蔬菜/肉类/调料等）',
    `category_id` BIGINT,
    `unit` VARCHAR(20) COMMENT '常用单位（个、瓶、克、袋等）',
    INDEX idx_name (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食材表';

-- 菜谱食材关联表
CREATE TABLE `recipe_ingredient` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `recipe_id` BIGINT NOT NULL COMMENT '菜谱ID',
    `ingredient_id` BIGINT NOT NULL COMMENT '食材ID',
    `amount` VARCHAR(50) COMMENT '用量（如：200g，2个）',
    `is_main` BOOLEAN DEFAULT FALSE COMMENT '是否主要食材',
    FOREIGN KEY (`recipe_id`) REFERENCES `recipe`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`ingredient_id`) REFERENCES `ingredient`(`id`),
    INDEX idx_recipe (`recipe_id`),
    INDEX idx_ingredient (`ingredient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱食材关联表';

-- 烹饪步骤表
CREATE TABLE `cooking_step` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '步骤ID',
    `recipe_id` BIGINT NOT NULL COMMENT '菜谱ID',
    `step_number` INT NOT NULL COMMENT '步骤序号',
    `description` TEXT NOT NULL COMMENT '步骤描述',
    `image` VARCHAR(255) COMMENT '步骤图片URL',
    `duration` INT COMMENT '该步骤耗时（分钟）',
    FOREIGN KEY (`recipe_id`) REFERENCES `recipe`(`id`) ON DELETE CASCADE,
    INDEX idx_recipe (`recipe_id`, `step_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烹饪步骤表';

-- 外链食谱表
CREATE TABLE `external_recipe` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `recipe_id` BIGINT NOT NULL COMMENT '关联的本地菜谱ID',
    `title` VARCHAR(200) NOT NULL COMMENT '外链标题',
    `url` VARCHAR(500) NOT NULL COMMENT '外链URL',
    `thumbnail` VARCHAR(1000),
    `notes` VARCHAR(2000),
    `sort_order` INT NOT NULL DEFAULT 0,
    `source` VARCHAR(50) COMMENT '来源（如：下厨房、豆果）',
    `added_by` BIGINT NOT NULL COMMENT '添加者ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (`recipe_id`) REFERENCES `recipe`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`added_by`) REFERENCES `user`(`id`),
    INDEX idx_recipe (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外链食谱表';

-- 会做该菜的人（多对多）
CREATE TABLE `recipe_cook` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `recipe_id` BIGINT NOT NULL COMMENT '菜谱ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `skill_level` TINYINT DEFAULT 3 COMMENT '熟练度（1-5）',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (`recipe_id`) REFERENCES `recipe`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    UNIQUE KEY uk_recipe_user (`recipe_id`, `user_id`),
    INDEX idx_user (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱厨师关联表';

-- 用户收藏表
CREATE TABLE `user_favorite` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `recipe_id` BIGINT NOT NULL COMMENT '菜谱ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`recipe_id`) REFERENCES `recipe`(`id`) ON DELETE CASCADE,
    UNIQUE KEY uk_user_recipe (`user_id`, `recipe_id`),
    INDEX idx_recipe (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- 冰箱食材表
CREATE TABLE `fridge_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `ingredient_id` BIGINT NOT NULL COMMENT '食材ID',
    `category_override_id` BIGINT NULL COMMENT '仅当前库存的分类覆盖',
    `amount` VARCHAR(50) COMMENT '数量',
    `purchase_date` DATE COMMENT '购买日期',
    `expiry_date` DATE NOT NULL COMMENT '过期日期',
    `storage_location` VARCHAR(50) COMMENT '存储位置（冷藏/冷冻/常温）',
    `status` VARCHAR(20) DEFAULT 'normal' COMMENT '状态（normal/expiring/expired/consumed）',
    `owner_user_id` BIGINT,
    `image_url` VARCHAR(500),
    `consumed_at` TIMESTAMP NULL COMMENT '消耗时间',
    `notes` VARCHAR(255) COMMENT '备注',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`ingredient_id`) REFERENCES `ingredient`(`id`),
    INDEX idx_user (`user_id`),
    INDEX idx_expiry (`expiry_date`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='冰箱食材表';

-- 提醒设置表
CREATE TABLE `reminder_setting` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `days_before_expiry` INT DEFAULT 3 COMMENT '提前多少天提醒',
    `reminder_time` TIME DEFAULT '17:00:00' COMMENT '提醒时间',
    `enabled` BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY uk_user (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提醒设置表';

-- 烹饪记录表
CREATE TABLE `cooking_history` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `recipe_id` BIGINT NOT NULL COMMENT '菜谱ID',
    `user_id` BIGINT NOT NULL COMMENT '烹饪者ID',
    `cooked_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '烹饪时间',
    `rating` TINYINT COMMENT '评分（1-5）',
    `notes` TEXT COMMENT '笔记',
    `images` TEXT COMMENT '成品图片URL（JSON数组）',
    FOREIGN KEY (`recipe_id`) REFERENCES `recipe`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    INDEX idx_recipe (`recipe_id`, `cooked_at` DESC),
    INDEX idx_user (`user_id`, `cooked_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烹饪记录表';


CREATE TABLE user_dislike (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipe_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_recipe (user_id, recipe_id),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ==================== 社交功能相关表 ====================

-- 好友关系表
CREATE TABLE `friendship` (
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
CREATE TABLE `group_chat` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '群组ID',
    `name` VARCHAR(100) NOT NULL COMMENT '群组名称',
    `avatar` VARCHAR(500) COMMENT '群组头像URL',
    `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
    `manager_id` BIGINT NOT NULL COMMENT '群管理员ID（有且只有1个）',
    `member_count` INT DEFAULT 0 COMMENT '成员数量',
    `max_members` INT DEFAULT 10 COMMENT '最大成员数',
    `description` VARCHAR(500) COMMENT '群组描述',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`creator_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`manager_id`) REFERENCES `user`(`id`),
    INDEX `idx_creator` (`creator_id`),
    INDEX `idx_manager` (`manager_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组表';

-- 群组成员表
CREATE TABLE `group_member` (
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
CREATE TABLE `message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    `group_id` BIGINT NOT NULL COMMENT '群组ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `message_type` ENUM('text', 'sticker', 'system') DEFAULT 'text' COMMENT '消息类型',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    FOREIGN KEY (`group_id`) REFERENCES `group_chat`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`sender_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    INDEX `idx_group_time` (`group_id`, `created_at`),
    INDEX `idx_sender` (`sender_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 用户最后读取消息位置表（用于未读消息计数）
CREATE TABLE `user_read_position` (
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


-- 创建配置相关表的迁移脚本
-- 执行日期：2026-02-11

-- 1. 常见食材配置表
CREATE TABLE `config_ingredients` (
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
CREATE TABLE `config_storage_locations` (
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
CREATE TABLE `config_categories` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `is_enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_enabled (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食材分类配置表';

-- 4. 系统文本配置表（用于存储各种显示文本）
CREATE TABLE `config_texts` (
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


-- 创建库存分类表
CREATE TABLE inventory_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT NULL COMMENT '父分类ID，NULL表示一级分类',
    icon VARCHAR(50) DEFAULT NULL COMMENT '图标名称',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存分类表';


CREATE TABLE media_file (
    filename VARCHAR(64) PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Fresh-install schema: a family group can manage several physical kitchens.
CREATE TABLE kitchen (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 group_id BIGINT NOT NULL,
 name VARCHAR(100) NOT NULL,
 FOREIGN KEY(group_id) REFERENCES group_chat(id) ON DELETE CASCADE,
 INDEX idx_kitchen_group(group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE fridge_item ADD kitchen_id BIGINT NOT NULL,
 ADD CONSTRAINT fk_fridge_kitchen FOREIGN KEY(kitchen_id) REFERENCES kitchen(id) ON DELETE CASCADE,
 ADD INDEX idx_fridge_kitchen(kitchen_id,status,expiry_date);
ALTER TABLE recipe ADD kitchen_id BIGINT NULL,
 ADD CONSTRAINT fk_recipe_kitchen FOREIGN KEY(kitchen_id) REFERENCES kitchen(id) ON DELETE SET NULL;

ALTER TABLE message ADD client_message_id VARCHAR(64) NULL,
 ADD UNIQUE KEY uk_message_client(sender_id,client_message_id),
 ADD INDEX idx_group_cursor(group_id,id);

CREATE TABLE recipe_image (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 recipe_id BIGINT NOT NULL,
 url VARCHAR(500) NOT NULL,
 category VARCHAR(32) NOT NULL,
 caption VARCHAR(1000),
 position INT NOT NULL,
 FOREIGN KEY(recipe_id) REFERENCES recipe(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE media_file ADD category VARCHAR(32) NOT NULL DEFAULT 'other',
 ADD deleted_at DATETIME NULL, ADD purge_after DATETIME NULL, ADD purged_at DATETIME NULL;

-- Public starter recipes have no login account; users cannot edit these system entries.
ALTER TABLE recipe MODIFY creator_id BIGINT NULL;
ALTER TABLE ingredient MODIFY name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL, ADD aliases JSON NULL, ADD processing_method VARCHAR(50) NULL,
 ADD base_ingredient_id BIGINT NULL, ADD UNIQUE KEY uk_ingredient_name(name);

-- Registration email verification (additive local setup, no account data changes).
CREATE TABLE IF NOT EXISTS registration_email_code (
 email VARCHAR(100) PRIMARY KEY,
 code_hash CHAR(64),
 expires_at BIGINT NOT NULL DEFAULT 0,
 issued_at BIGINT NOT NULL DEFAULT 0,
 window_start BIGINT NOT NULL DEFAULT 0,
 send_count INT NOT NULL DEFAULT 0,
 attempts INT NOT NULL DEFAULT 0,
 active BOOLEAN NOT NULL DEFAULT FALSE,
 consumed BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS registration_email_rate (
 address_hash CHAR(64) PRIMARY KEY,
 window_start BIGINT NOT NULL,
 send_count INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- Apply once to an existing development database. Fresh databases use schema.sql.
ALTER TABLE recipe ADD COLUMN source_recipe_id BIGINT NULL;
ALTER TABLE recipe ADD COLUMN source_recipe_name VARCHAR(100) NULL;
CREATE TABLE meal_arrangement (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 owner_id BIGINT NOT NULL,
 kitchen_id BIGINT NOT NULL,
 kind VARCHAR(10) NOT NULL,
 title VARCHAR(100) NOT NULL,
 meal_date DATE NULL,
 meal VARCHAR(20) NULL,
 dishes JSON NOT NULL,
 shopping JSON NOT NULL,
 source_id BIGINT NULL,
 revision INT NOT NULL DEFAULT 1,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 INDEX idx_meal_owner(owner_id,updated_at)
);
CREATE TABLE meal_share (
 meal_id BIGINT NOT NULL, group_id BIGINT NOT NULL,
 PRIMARY KEY(meal_id,group_id)
);
-- Additive table for explicit chat sharing. No changes to existing recipe ownership.
CREATE TABLE IF NOT EXISTS recipe_chat_share (
  recipe_id BIGINT NOT NULL,
  group_id BIGINT NOT NULL,
  shared_by BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (recipe_id, group_id),
  INDEX idx_recipe_chat_group (group_id)
);
