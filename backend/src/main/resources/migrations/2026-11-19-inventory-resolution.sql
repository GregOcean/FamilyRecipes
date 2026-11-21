SET @q = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='ingredient' AND column_name='is_catalog')=0,'ALTER TABLE ingredient ADD is_catalog BOOLEAN NOT NULL DEFAULT TRUE','SELECT 1');
PREPARE q FROM @q; EXECUTE q; DEALLOCATE PREPARE q;
SET @q = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fridge_item' AND column_name='display_name')=0,'ALTER TABLE fridge_item ADD display_name VARCHAR(100) NULL','SELECT 1');
PREPARE q FROM @q; EXECUTE q; DEALLOCATE PREPARE q;
SET @q = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fridge_item' AND column_name='client_mutation_id')=0,'ALTER TABLE fridge_item ADD client_mutation_id VARCHAR(36) NULL','SELECT 1');
PREPARE q FROM @q; EXECUTE q; DEALLOCATE PREPARE q;
SET @q = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='fridge_item' AND index_name='uk_stock_mutation')=0,'ALTER TABLE fridge_item ADD UNIQUE KEY uk_stock_mutation(user_id,client_mutation_id)','SELECT 1');
PREPARE q FROM @q; EXECUTE q; DEALLOCATE PREPARE q;
ALTER TABLE fridge_item MODIFY expiry_date DATE NULL;
CREATE TABLE IF NOT EXISTS ingredient_user_alias (
 user_id BIGINT NOT NULL,alias_text VARCHAR(100) NOT NULL,ingredient_id BIGINT NOT NULL,
 PRIMARY KEY(user_id,alias_text),FOREIGN KEY(user_id) REFERENCES user(id) ON DELETE CASCADE,
 FOREIGN KEY(ingredient_id) REFERENCES ingredient(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
