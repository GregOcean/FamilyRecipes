-- Additive table for explicit chat sharing. No changes to existing recipe ownership.
CREATE TABLE IF NOT EXISTS recipe_chat_share (
  recipe_id BIGINT NOT NULL,
  group_id BIGINT NOT NULL,
  shared_by BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (recipe_id, group_id),
  INDEX idx_recipe_chat_group (group_id)
);
