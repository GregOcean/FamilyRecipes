CREATE TABLE IF NOT EXISTS recipe_cuisine_refinement (
    recipe_id BIGINT NOT NULL,
    taxonomy_version VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_hash VARCHAR(64) NOT NULL,
    original_cuisines JSON NOT NULL,
    result_json JSON,
    model VARCHAR(100),
    last_error VARCHAR(100),
    requested_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (recipe_id, taxonomy_version),
    KEY idx_cuisine_refinement_queue (status, next_attempt_at),
    FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
