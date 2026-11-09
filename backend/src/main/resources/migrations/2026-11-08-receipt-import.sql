-- Idempotency ledger; keep rows so network retries never duplicate inventory.
CREATE TABLE IF NOT EXISTS receipt_import (
    user_id BIGINT NOT NULL,
    request_id VARCHAR(36) NOT NULL,
    kitchen_id BIGINT NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    item_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(user_id, request_id)
);
