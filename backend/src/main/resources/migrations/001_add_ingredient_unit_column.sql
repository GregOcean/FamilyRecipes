-- 为食材表添加 unit 列的迁移脚本
-- 执行日期：2026-02-11

-- 添加 unit 列
ALTER TABLE `ingredient` 
ADD COLUMN `unit` VARCHAR(20) COMMENT '常用单位（个、瓶、克、袋等）' AFTER `category`;

-- 说明：
-- 此列用于存储食材的常用单位
-- 允许为 NULL，因为旧数据可能没有单位信息

