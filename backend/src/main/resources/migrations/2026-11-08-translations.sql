CREATE TABLE IF NOT EXISTS content_translations (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 source_hash CHAR(64) NOT NULL,
 target_language VARCHAR(8) NOT NULL,
 engine VARCHAR(180) NOT NULL,
 source_text MEDIUMTEXT NOT NULL,
 kind VARCHAR(16) NOT NULL,
 translated_text MEDIUMTEXT,
 status VARCHAR(16) NOT NULL DEFAULT 'pending',
 attempts INT NOT NULL DEFAULT 0,
 last_error VARCHAR(100),
 next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY translation_identity(source_hash,target_language,engine),
 INDEX translation_queue(status,next_attempt_at)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS recipe_provenance (
 recipe_id BIGINT PRIMARY KEY,
 source_identity CHAR(64) NOT NULL UNIQUE,
 metadata JSON NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(recipe_id) REFERENCES recipe(id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
