-- 添加 dislike_count 字段到 recipe 表
ALTER TABLE recipe ADD COLUMN dislike_count INT DEFAULT 0 NOT NULL AFTER favorite_count;

-- 创建 user_dislike 表（类似 user_favorite）
CREATE TABLE IF NOT EXISTS user_dislike (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipe_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_recipe (user_id, recipe_id),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

