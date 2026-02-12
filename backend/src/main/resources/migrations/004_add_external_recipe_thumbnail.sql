-- 添加外部链接缩略图字段
-- 设置为较大的长度以支持长URL，但不支持base64图片
ALTER TABLE external_recipe ADD COLUMN thumbnail VARCHAR(1000) AFTER source;

