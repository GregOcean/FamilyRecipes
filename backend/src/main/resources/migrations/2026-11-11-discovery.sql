
CREATE TABLE IF NOT EXISTS recipe_favorite_notification (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 recipient_id BIGINT NOT NULL,
 actor_id BIGINT NOT NULL,
 recipe_id BIGINT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 read_at TIMESTAMP NULL,
 UNIQUE KEY one_favorite_notice (recipient_id,actor_id,recipe_id),
 INDEX recipient_unread (recipient_id,read_at,id),
 FOREIGN KEY (recipient_id) REFERENCES `user`(id) ON DELETE CASCADE,
 FOREIGN KEY (actor_id) REFERENCES `user`(id) ON DELETE CASCADE,
 FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS recipe_enrichment (
 recipe_id BIGINT PRIMARY KEY,
 status VARCHAR(30) NOT NULL DEFAULT 'failed',
 attempts INT NOT NULL DEFAULT 0,
 next_attempt_at TIMESTAMP NULL,
 source_hash VARCHAR(64),
 model VARCHAR(100),
 result_json JSON,
 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE
);

-- v3 replaces the old coarse-to-province refinement queue.
UPDATE recipe_cuisine_refinement SET status='superseded',last_error='SupersededByTaxonomyV3' WHERE status IN ('pending','running');
