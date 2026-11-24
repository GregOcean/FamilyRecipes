-- 家人免登录查看菜单和勾选采购项。可重复执行。
SET @q = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='meal_arrangement' AND column_name='public_token')=0,'ALTER TABLE meal_arrangement ADD COLUMN public_token VARCHAR(64) NULL','SELECT 1');
PREPARE q FROM @q; EXECUTE q; DEALLOCATE PREPARE q;
SET @q = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='meal_arrangement' AND index_name='idx_meal_public_token')=0,'ALTER TABLE meal_arrangement ADD UNIQUE KEY idx_meal_public_token(public_token)','SELECT 1');
PREPARE q FROM @q; EXECUTE q; DEALLOCATE PREPARE q;
