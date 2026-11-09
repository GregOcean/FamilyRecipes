CREATE TABLE IF NOT EXISTS recipe_view_history (
 user_id BIGINT NOT NULL,
 recipe_id BIGINT NOT NULL,
 viewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(user_id,recipe_id),
 INDEX idx_recent_views(user_id,viewed_at),
 FOREIGN KEY(user_id) REFERENCES user(id) ON DELETE CASCADE,
 FOREIGN KEY(recipe_id) REFERENCES recipe(id) ON DELETE CASCADE
);

-- Unknown is explicit: never infer "not spicy" from missing information.
INSERT INTO recipe_tag(recipe_id,tag_type,tag_value)
 SELECT r.id,'taste','待确认' FROM recipe r WHERE NOT EXISTS
 (SELECT 1 FROM recipe_tag t WHERE t.recipe_id=r.id AND t.tag_type='taste');
INSERT INTO recipe_tag(recipe_id,tag_type,tag_value)
 SELECT r.id,'cuisine','待确认' FROM recipe r WHERE NOT EXISTS
 (SELECT 1 FROM recipe_tag t WHERE t.recipe_id=r.id AND t.tag_type='cuisine');
