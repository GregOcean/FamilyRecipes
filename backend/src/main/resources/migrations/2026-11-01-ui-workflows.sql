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
