-- One-time adjustment for the pre-release v1 taxonomy. Do not rerun after user classification edits.
ALTER TABLE fridge_item ADD COLUMN category_override_id BIGINT NULL COMMENT '仅当前库存的分类覆盖';
START TRANSACTION;
CREATE TEMPORARY TABLE inventory_reclassification AS SELECT id, CASE category WHEN '蔬菜' THEN 1 WHEN '肉类' THEN 2 WHEN '海鲜' THEN 3 WHEN '蛋奶' THEN 4 WHEN '豆制品' THEN 5 WHEN '主食' THEN 6 WHEN '水果' THEN 7 WHEN '调味料' THEN 8 WHEN '调味品' THEN 8 WHEN '干货' THEN 9 ELSE 13 END AS new_id FROM ingredient;
INSERT INTO inventory_categories (id,name,icon,sort_order,parent_id) VALUES
(1,'蔬菜菌菇','🥬',1,NULL),
(2,'肉禽','🥩',2,NULL),
(3,'水产','🐟',3,NULL),
(4,'蛋奶','🥛',4,NULL),
(5,'豆制品','🫘',5,NULL),
(6,'米面主食','🍚',6,NULL),
(7,'水果','🍎',7,NULL),
(8,'调味料','🧂',8,NULL),
(9,'干货杂粮','🌾',9,NULL),
(10,'零食坚果','🥜',10,NULL),
(11,'饮品','🥤',11,NULL),
(12,'熟食速食','🍱',12,NULL),
(13,'待分类','📦',13,NULL) ON DUPLICATE KEY UPDATE name=VALUES(name),icon=VALUES(icon),sort_order=VALUES(sort_order),parent_id=NULL;
UPDATE ingredient i JOIN inventory_reclassification r ON r.id=i.id SET i.category_id=r.new_id;
UPDATE ingredient SET category_id=10 WHERE name IN ('花生','芝麻','核桃','腰果','杏仁','葡萄干');
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='饮品' LIMIT 1) WHERE name='豆浆';
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1) WHERE name='豆豉';
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1) WHERE name='面筋';
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1) WHERE name='小米';
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1) WHERE name='燕麦';
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1) WHERE name='薏米';
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='熟食速食' LIMIT 1) WHERE name='包子';
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='熟食速食' LIMIT 1) WHERE name='饺子';
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='熟食速食' LIMIT 1) WHERE name='熟火腿';
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='熟食速食' LIMIT 1) WHERE name='午餐肉';
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='熟食速食' LIMIT 1) WHERE name='牛肉丸';
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='熟食速食' LIMIT 1) WHERE name='猪肉丸';
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='熟食速食' LIMIT 1) WHERE name='鸡肉丸';
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='熟食速食' LIMIT 1) WHERE name='鱼丸';
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1) WHERE name='酵母';
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1) WHERE name='泡打粉';
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1) WHERE name='榨菜';
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1) WHERE name='雪菜';
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1) WHERE name='酸菜';
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1) WHERE name='泡菜';
UPDATE ingredient SET category_id=(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1) WHERE name='咸菜';
UPDATE ingredient i JOIN inventory_categories c ON c.id=i.category_id SET i.category=c.name;
DELETE FROM inventory_categories WHERE id>13;
DROP TEMPORARY TABLE inventory_reclassification;
COMMIT;
