-- 家人免登录查看菜单和勾选采购项。
ALTER TABLE meal_arrangement ADD COLUMN public_token VARCHAR(64) NULL;
CREATE UNIQUE INDEX idx_meal_public_token ON meal_arrangement(public_token);
