CREATE TABLE IF NOT EXISTS password_reset_code LIKE registration_email_code;
CREATE TABLE IF NOT EXISTS user_auth_version (
 user_id BIGINT PRIMARY KEY, version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS catalog_admin_job (
 id CHAR(36) PRIMARY KEY, status VARCHAR(20) NOT NULL, recipes JSON NOT NULL,
 created_by BIGINT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 error VARCHAR(200), INDEX idx_catalog_admin_status(status,created_at)
);
CREATE TABLE IF NOT EXISTS catalog_media_credit (
 recipe_id BIGINT NOT NULL, source_hash CHAR(64) NOT NULL, metadata JSON NOT NULL,
 media_url VARCHAR(500) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(recipe_id,source_hash)
);
