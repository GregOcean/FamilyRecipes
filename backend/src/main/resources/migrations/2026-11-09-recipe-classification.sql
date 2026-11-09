CREATE TABLE IF NOT EXISTS recipe_classification (
    recipe_id BIGINT PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_hash VARCHAR(64),
    model VARCHAR(100),
    result_json JSON,
    FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
