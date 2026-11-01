SET NAMES utf8mb4;
-- Public configuration only. No default account. Run once on a fresh database.
-- 插入常见食材数据
INSERT INTO `config_ingredients` (`name`, `category`, `sort_order`) VALUES
-- 肉类
('牛肉', '肉类', 1), ('牛腱子', '肉类', 2), ('牛里脊', '肉类', 3), ('牛排', '肉类', 4), ('牛腩', '肉类', 5),
('猪肉', '肉类', 11), ('猪里脊', '肉类', 12), ('猪五花', '肉类', 13), ('猪蹄', '肉类', 14), ('猪尾巴', '肉类', 15),
('羊肉', '肉类', 21), ('羊排', '肉类', 22), ('羊腿', '肉类', 23), ('羊蝎子', '肉类', 24),
('鸡肉', '肉类', 31), ('鸡腿', '肉类', 32), ('鸡翅', '肉类', 33), ('鸡胸肉', '肉类', 34), ('鸡爪', '肉类', 35),
('鸭肉', '肉类', 41), ('鸭腿', '肉类', 42), ('鸭翅', '肉类', 43), ('鸭舌', '肉类', 44), ('鸭血', '肉类', 45),

-- 海鲜水产
('鱼', '海鲜', 101), ('草鱼', '海鲜', 102), ('鲈鱼', '海鲜', 103), ('鲫鱼', '海鲜', 104), ('带鱼', '海鲜', 105),
('三文鱼', '海鲜', 106), ('鳕鱼', '海鲜', 107), ('黄鱼', '海鲜', 108),
('虾', '海鲜', 111), ('大虾', '海鲜', 112), ('基围虾', '海鲜', 113), ('龙虾', '海鲜', 114), ('虾仁', '海鲜', 115),
('螃蟹', '海鲜', 121), ('大闸蟹', '海鲜', 122), ('梭子蟹', '海鲜', 123),
('蛤蜊', '海鲜', 131), ('扇贝', '海鲜', 132), ('生蚝', '海鲜', 133), ('鱿鱼', '海鲜', 134),

-- 蔬菜类
('白菜', '蔬菜', 201), ('大白菜', '蔬菜', 202), ('娃娃菜', '蔬菜', 203), ('小白菜', '蔬菜', 204),
('菠菜', '蔬菜', 211), ('生菜', '蔬菜', 212), ('油麦菜', '蔬菜', 213), ('芹菜', '蔬菜', 214), ('香菜', '蔬菜', 215),
('番茄', '蔬菜', 221), ('西红柿', '蔬菜', 222), ('土豆', '蔬菜', 223), ('红薯', '蔬菜', 224), ('山药', '蔬菜', 225),
('黄瓜', '蔬菜', 231), ('冬瓜', '蔬菜', 232), ('南瓜', '蔬菜', 233), ('丝瓜', '蔬菜', 234), ('苦瓜', '蔬菜', 235),
('茄子', '蔬菜', 241), ('青椒', '蔬菜', 242), ('尖椒', '蔬菜', 243), ('彩椒', '蔬菜', 244), ('辣椒', '蔬菜', 245),
('豆角', '蔬菜', 251), ('四季豆', '蔬菜', 252), ('豇豆', '蔬菜', 253), ('荷兰豆', '蔬菜', 254), ('豌豆', '蔬菜', 255),
('胡萝卜', '蔬菜', 261), ('白萝卜', '蔬菜', 262), ('莲藕', '蔬菜', 263), ('竹笋', '蔬菜', 264),
('玉米', '蔬菜', 271), ('西兰花', '蔬菜', 272), ('菜花', '蔬菜', 273),
('蘑菇', '蔬菜', 281), ('香菇', '蔬菜', 282), ('平菇', '蔬菜', 283), ('金针菇', '蔬菜', 284), ('杏鲍菇', '蔬菜', 285),

-- 水果类
('苹果', '水果', 301), ('香蕉', '水果', 302), ('橙子', '水果', 303), ('橘子', '水果', 304),
('梨', '水果', 311), ('雪梨', '水果', 312), ('葡萄', '水果', 313), ('提子', '水果', 314),
('西瓜', '水果', 321), ('哈密瓜', '水果', 322), ('草莓', '水果', 323), ('蓝莓', '水果', 324),
('桃子', '水果', 331), ('芒果', '水果', 332), ('榴莲', '水果', 333), ('火龙果', '水果', 334),

-- 蛋奶类
('鸡蛋', '蛋奶', 401), ('鸭蛋', '蛋奶', 402), ('鹌鹑蛋', '蛋奶', 403),
('牛奶', '蛋奶', 411), ('纯牛奶', '蛋奶', 412), ('酸奶', '蛋奶', 413), ('奶酪', '蛋奶', 414),

-- 豆制品
('豆腐', '豆制品', 501), ('嫩豆腐', '豆制品', 502), ('老豆腐', '豆制品', 503), ('豆腐干', '豆制品', 504),

-- 主食类
('米', '主食', 601), ('大米', '主食', 602), ('面粉', '主食', 603), ('面条', '主食', 604),
('馒头', '主食', 611), ('花卷', '主食', 612), ('包子', '主食', 613), ('饺子', '主食', 614),

-- 调味品
('盐', '调味品', 701), ('糖', '调味品', 702), ('酱油', '调味品', 703), ('醋', '调味品', 704),
('料酒', '调味品', 711), ('食用油', '调味品', 712), ('葱', '调味品', 713), ('姜', '调味品', 714), ('蒜', '调味品', 715);

-- 插入存储位置数据
INSERT INTO `config_storage_locations` (`name`, `icon`, `sort_order`, `is_default`) VALUES
('冰箱冷藏', 'fridge_cold', 1, 1),
('冰箱冷冻', 'fridge_freeze', 2, 0),
('橱柜', 'cabinet', 3, 0),
('常温', 'room_temp', 4, 0),
('阴凉处', 'cool_place', 5, 0),
('冷藏室', 'fridge_compartment', 6, 0),
('冷冻室', 'freezer_compartment', 7, 0),
('保鲜层', 'fresh_compartment', 8, 0),
('蔬菜室', 'vegetable_compartment', 9, 0),
('冰柜', 'freezer', 10, 0);

-- 插入分类数据
INSERT INTO `config_categories` (`name`, `sort_order`) VALUES
('肉类', 1),
('海鲜', 2),
('蔬菜', 3),
('水果', 4),
('蛋奶', 5),
('豆制品', 6),
('主食', 7),
('调味品', 8),
('其他', 99);

-- 插入系统文本配置
INSERT INTO `config_texts` (`config_key`, `config_value`, `description`, `category`) VALUES
('status.normal', '正常', '食材状态-正常', 'status'),
('status.expiring', '即将过期', '食材状态-即将过期', 'status'),
('status.expired', '已过期', '食材状态-已过期', 'status'),
('status.consumed', '已消耗', '食材状态-已消耗', 'status'),
('button.consume', '已消耗', '按钮文本-标记已消耗', 'button'),
('button.delete', '删除', '按钮文本-删除', 'button'),
('button.save', '保存', '按钮文本-保存', 'button'),
('button.cancel', '取消', '按钮文本-取消', 'button'),
('button.back_to_edit', '返回修改', '按钮文本-返回修改', 'button'),
('button.confirm_save', '确认保存', '按钮文本-确认保存', 'button');


-- Inventory taxonomy: food type only; storage and processing are separate attributes.
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
(13,'待分类','📦',13,NULL);

-- Starter library v1: curated names/aliases, not scraped platform content.
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('牛肉','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('牛腱子','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('牛里脊','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('牛排','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('牛腩','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪肉','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪里脊','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪五花','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','["五花肉"]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪蹄','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪尾巴','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('羊肉','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('羊排','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('羊腿','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('羊蝎子','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸡肉','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸡腿','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸡翅','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸡胸肉','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸡爪','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸭肉','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸭腿','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸭翅','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸭舌','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸭血','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('草鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鲈鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鲫鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('带鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('三文鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','["鲑鱼"]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鳕鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('黄鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('虾','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('大虾','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('基围虾','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('龙虾','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('虾仁','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('螃蟹','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('大闸蟹','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('梭子蟹','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('蛤蜊','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('扇贝','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('生蚝','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鱿鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('白菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('大白菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('娃娃菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('小白菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('菠菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('生菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('油麦菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('芹菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('香菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('番茄','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','["西红柿"]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('土豆','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','["马铃薯", "洋芋"]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('红薯','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('山药','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('黄瓜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('冬瓜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('南瓜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('丝瓜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('苦瓜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('茄子','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('青椒','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('尖椒','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('彩椒','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('辣椒','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('豆角','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('四季豆','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('豇豆','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('荷兰豆','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('豌豆','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('胡萝卜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('白萝卜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('莲藕','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('竹笋','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('玉米','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('西兰花','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('菜花','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('蘑菇','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('香菇','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('平菇','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('金针菇','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('杏鲍菇','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('苹果','水果',(SELECT id FROM inventory_categories WHERE name='水果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('香蕉','水果',(SELECT id FROM inventory_categories WHERE name='水果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('橙子','水果',(SELECT id FROM inventory_categories WHERE name='水果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('橘子','水果',(SELECT id FROM inventory_categories WHERE name='水果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('梨','水果',(SELECT id FROM inventory_categories WHERE name='水果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('雪梨','水果',(SELECT id FROM inventory_categories WHERE name='水果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('葡萄','水果',(SELECT id FROM inventory_categories WHERE name='水果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('提子','水果',(SELECT id FROM inventory_categories WHERE name='水果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('西瓜','水果',(SELECT id FROM inventory_categories WHERE name='水果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('哈密瓜','水果',(SELECT id FROM inventory_categories WHERE name='水果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('草莓','水果',(SELECT id FROM inventory_categories WHERE name='水果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('蓝莓','水果',(SELECT id FROM inventory_categories WHERE name='水果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('桃子','水果',(SELECT id FROM inventory_categories WHERE name='水果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('芒果','水果',(SELECT id FROM inventory_categories WHERE name='水果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('榴莲','水果',(SELECT id FROM inventory_categories WHERE name='水果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('火龙果','水果',(SELECT id FROM inventory_categories WHERE name='水果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸡蛋','蛋奶',(SELECT id FROM inventory_categories WHERE name='蛋奶' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸭蛋','蛋奶',(SELECT id FROM inventory_categories WHERE name='蛋奶' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鹌鹑蛋','蛋奶',(SELECT id FROM inventory_categories WHERE name='蛋奶' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('牛奶','蛋奶',(SELECT id FROM inventory_categories WHERE name='蛋奶' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('纯牛奶','蛋奶',(SELECT id FROM inventory_categories WHERE name='蛋奶' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('酸奶','蛋奶',(SELECT id FROM inventory_categories WHERE name='蛋奶' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('奶酪','蛋奶',(SELECT id FROM inventory_categories WHERE name='蛋奶' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('豆腐','豆制品',(SELECT id FROM inventory_categories WHERE name='豆制品' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('嫩豆腐','豆制品',(SELECT id FROM inventory_categories WHERE name='豆制品' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('老豆腐','豆制品',(SELECT id FROM inventory_categories WHERE name='豆制品' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('豆腐干','豆制品',(SELECT id FROM inventory_categories WHERE name='豆制品' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('米','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('大米','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('面粉','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('面条','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('馒头','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('花卷','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('包子','熟食速食',(SELECT id FROM inventory_categories WHERE name='熟食速食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('饺子','熟食速食',(SELECT id FROM inventory_categories WHERE name='熟食速食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('盐','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('糖','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('酱油','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('醋','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('料酒','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('食用油','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('葱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('姜','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('蒜','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','["大蒜"]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪前腿肉','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪后腿肉','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪梅花肉','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪肩肉','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪肋排','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪小排','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪脊骨','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪筒骨','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪颈肉','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪皮','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪耳','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪肚','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪肝','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪心','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪腰','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪大肠','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪舌','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪血','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪肉末','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('牛上脑','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('牛眼肉','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('牛西冷','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('牛腱芯','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('牛胸肉','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('牛肋条','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('牛尾','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('牛筋','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('牛肚','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('牛百叶','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('牛舌','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('牛肉末','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('羊肩肉','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('羊腱','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('羊肉卷','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸡全腿','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸡翅根','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸡翅中','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸡翅尖','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸡胗','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸡肝','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸡心','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸡骨架','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸭胸肉','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸭胗','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸭肝','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸭脖','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鹅肉','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鹌鹑','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('兔肉','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('腊肉','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]','腌制');
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('腊肠','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]','腌制');
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('咸猪肉','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]','腌制');
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('金华火腿','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]','腌制');
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('培根','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]','烟熏');
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('烟熏肉','肉禽',(SELECT id FROM inventory_categories WHERE name='肉禽' LIMIT 1),'克','[]','烟熏');
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('熟火腿','熟食速食',(SELECT id FROM inventory_categories WHERE name='熟食速食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('午餐肉','熟食速食',(SELECT id FROM inventory_categories WHERE name='熟食速食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('牛肉丸','熟食速食',(SELECT id FROM inventory_categories WHERE name='熟食速食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪肉丸','熟食速食',(SELECT id FROM inventory_categories WHERE name='熟食速食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸡肉丸','熟食速食',(SELECT id FROM inventory_categories WHERE name='熟食速食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鱼丸','熟食速食',(SELECT id FROM inventory_categories WHERE name='熟食速食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鲅鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鲢鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('罗非鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('多宝鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鲳鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鲶鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('秋刀鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('金枪鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鲭鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('银鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鳗鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('黄鳝','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('泥鳅','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('墨鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('章鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('海参','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鲍鱼','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('青口','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('贝柱','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('干贝','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('虾皮','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('虾米','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('蟹肉','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鱼豆腐','水产',(SELECT id FROM inventory_categories WHERE name='水产' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('上海青','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('菜心','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('芥蓝','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('茼蒿','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('苋菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('空心菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('韭菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('韭黄','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('蒜苗','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('蒜薹','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('莴笋','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('荠菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('马齿苋','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('卷心菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('紫甘蓝','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('抱子甘蓝','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('西葫芦','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('佛手瓜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('秋葵','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('芦笋','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('茭白','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('慈菇','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('芋头','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('荸荠','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('蚕豆','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('毛豆','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('黄豆芽','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('绿豆芽','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('口蘑','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('白玉菇','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('蟹味菇','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('茶树菇','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猴头菇','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('木耳','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('银耳','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('海带','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('紫菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('裙带菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('香葱','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('大葱','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('生姜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('洋葱','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('柠檬','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('香茅','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('南姜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('罗勒','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('薄荷','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('迷迭香','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('百里香','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('欧芹','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('内酯豆腐','豆制品',(SELECT id FROM inventory_categories WHERE name='豆制品' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('北豆腐','豆制品',(SELECT id FROM inventory_categories WHERE name='豆制品' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('南豆腐','豆制品',(SELECT id FROM inventory_categories WHERE name='豆制品' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('绢豆腐','豆制品',(SELECT id FROM inventory_categories WHERE name='豆制品' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('千张','豆制品',(SELECT id FROM inventory_categories WHERE name='豆制品' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('腐竹','豆制品',(SELECT id FROM inventory_categories WHERE name='豆制品' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('油豆腐','豆制品',(SELECT id FROM inventory_categories WHERE name='豆制品' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('豆腐泡','豆制品',(SELECT id FROM inventory_categories WHERE name='豆制品' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('素鸡','豆制品',(SELECT id FROM inventory_categories WHERE name='豆制品' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('豆皮','豆制品',(SELECT id FROM inventory_categories WHERE name='豆制品' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('面筋','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('豆豉','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('纳豆','豆制品',(SELECT id FROM inventory_categories WHERE name='豆制品' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('豆浆','饮品',(SELECT id FROM inventory_categories WHERE name='饮品' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('白砂糖','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('冰糖','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('红糖','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('蜂蜜','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('生抽','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('老抽','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('蒸鱼豉油','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('蚝油','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('米醋','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('陈醋','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('白醋','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('黄酒','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('米酒','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('味精','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鸡精','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('胡椒粉','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('黑胡椒','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('白胡椒','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('花椒','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('青花椒','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('干辣椒','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('辣椒粉','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('八角','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('桂皮','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('香叶','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('小茴香','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('孜然','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('草果','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('丁香','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('陈皮','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('肉豆蔻','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('五香粉','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('十三香','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('姜黄','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('咖喱粉','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('烟熏红椒粉','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('郫县豆瓣酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('黄豆酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('甜面酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('海鲜酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('柱侯酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('芝麻酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('花生酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('番茄酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('沙茶酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('辣椒酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('剁椒','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('老干妈','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('豆腐乳','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('南乳','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('味噌','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('日式酱油','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('味醂','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('柴鱼片','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('昆布','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('韩式辣椒酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('韩式大酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鱼露','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('虾酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('罗望子酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('泰式红咖喱酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('泰式绿咖喱酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('椰浆','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('椰奶','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('意面番茄酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('青酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('黄芥末酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('第戎芥末酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('蛋黄酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('伍斯特酱','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('苹果醋','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('黑醋','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('橄榄油','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('芝麻油','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('花生油','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('菜籽油','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('玉米油','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('猪油','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('黄油','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('辣椒油','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('糯米','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('小米','干货杂粮',(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('燕麦','干货杂粮',(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('薏米','干货杂粮',(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('玉米面','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('高筋面粉','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('中筋面粉','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('低筋面粉','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('玉米淀粉','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('土豆淀粉','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('木薯淀粉','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('糯米粉','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('粘米粉','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('酵母','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('泡打粉','调味料',(SELECT id FROM inventory_categories WHERE name='调味料' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('米粉','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('河粉','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('意大利面','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('粉丝','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('粉条','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('年糕','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('饺子皮','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('馄饨皮','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('春卷皮','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('面包糠','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('面包','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('米饭','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('荞麦面','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('乌冬面','米面主食',(SELECT id FROM inventory_categories WHERE name='米面主食' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('咸鸭蛋','蛋奶',(SELECT id FROM inventory_categories WHERE name='蛋奶' LIMIT 1),'克','[]','腌制');
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('松花蛋','蛋奶',(SELECT id FROM inventory_categories WHERE name='蛋奶' LIMIT 1),'克','["皮蛋"]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('淡奶油','蛋奶',(SELECT id FROM inventory_categories WHERE name='蛋奶' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('酸奶油','蛋奶',(SELECT id FROM inventory_categories WHERE name='蛋奶' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('奶油奶酪','蛋奶',(SELECT id FROM inventory_categories WHERE name='蛋奶' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('马苏里拉奶酪','蛋奶',(SELECT id FROM inventory_categories WHERE name='蛋奶' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('帕尔马干酪','蛋奶',(SELECT id FROM inventory_categories WHERE name='蛋奶' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('切达奶酪','蛋奶',(SELECT id FROM inventory_categories WHERE name='蛋奶' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('炼乳','蛋奶',(SELECT id FROM inventory_categories WHERE name='蛋奶' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('奶粉','蛋奶',(SELECT id FROM inventory_categories WHERE name='蛋奶' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('红枣','干货杂粮',(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('枸杞','干货杂粮',(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('莲子','干货杂粮',(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('百合','干货杂粮',(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('桂圆','干货杂粮',(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('干香菇','干货杂粮',(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('干木耳','干货杂粮',(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('干银耳','干货杂粮',(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('梅干菜','干货杂粮',(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('榨菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('雪菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('酸菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]','腌制');
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('泡菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]','腌制');
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('咸菜','蔬菜菌菇',(SELECT id FROM inventory_categories WHERE name='蔬菜菌菇' LIMIT 1),'克','[]','腌制');
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('笋干','干货杂粮',(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('花生','零食坚果',(SELECT id FROM inventory_categories WHERE name='零食坚果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('芝麻','零食坚果',(SELECT id FROM inventory_categories WHERE name='零食坚果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('核桃','零食坚果',(SELECT id FROM inventory_categories WHERE name='零食坚果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('腰果','零食坚果',(SELECT id FROM inventory_categories WHERE name='零食坚果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('杏仁','零食坚果',(SELECT id FROM inventory_categories WHERE name='零食坚果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('葡萄干','零食坚果',(SELECT id FROM inventory_categories WHERE name='零食坚果' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('黄豆','干货杂粮',(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1),'克','["大豆"]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('红豆','干货杂粮',(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('绿豆','干货杂粮',(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('黑豆','干货杂粮',(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('鹰嘴豆','干货杂粮',(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1),'克','[]',NULL);
INSERT INTO ingredient(name,category,category_id,unit,aliases,processing_method) VALUES('扁豆','干货杂粮',(SELECT id FROM inventory_categories WHERE name='干货杂粮' LIMIT 1),'克','[]',NULL);
INSERT INTO recipe(name,description,cooking_time,difficulty,servings,creator_id,visibility) VALUES('番茄炒蛋','公共入门菜谱，可按口味调整。
1. 番茄洗净切块，鸡蛋打散。
2. 锅内热油，倒入蛋液，炒至凝固后盛出。
3. 用锅内余油炒软番茄，加少量水炒出汁。
4. 放回鸡蛋，加盐翻炒均匀。',15,1,2,NULL,'public');
SET @starter_recipe_id=LAST_INSERT_ID();
INSERT INTO recipe_tag(recipe_id,tag_type,tag_value) VALUES(@starter_recipe_id,'cuisine','家常菜'),(@starter_recipe_id,'custom','公共入门');
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'2个',1 FROM ingredient WHERE name='番茄';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'3个',1 FROM ingredient WHERE name='鸡蛋';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'适量',0 FROM ingredient WHERE name='盐';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'适量',0 FROM ingredient WHERE name='菜籽油';
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,1,'番茄洗净切块，鸡蛋打散。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,2,'锅内热油，倒入蛋液，炒至凝固后盛出。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,3,'用锅内余油炒软番茄，加少量水炒出汁。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,4,'放回鸡蛋，加盐翻炒均匀。');
INSERT INTO recipe(name,description,cooking_time,difficulty,servings,creator_id,visibility) VALUES('清炒西兰花','公共入门菜谱，可按口味调整。
1. 西兰花分成小朵，洗净；蒜切末。
2. 水烧开，放入西兰花焯至颜色变亮，捞出沥干。
3. 锅内热油炒香蒜末，加入西兰花翻炒至熟，加盐调味。',15,1,2,NULL,'public');
SET @starter_recipe_id=LAST_INSERT_ID();
INSERT INTO recipe_tag(recipe_id,tag_type,tag_value) VALUES(@starter_recipe_id,'cuisine','家常菜'),(@starter_recipe_id,'custom','公共入门');
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'1颗',1 FROM ingredient WHERE name='西兰花';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'2瓣',1 FROM ingredient WHERE name='蒜';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'适量',0 FROM ingredient WHERE name='盐';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'适量',0 FROM ingredient WHERE name='菜籽油';
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,1,'西兰花分成小朵，洗净；蒜切末。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,2,'水烧开，放入西兰花焯至颜色变亮，捞出沥干。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,3,'锅内热油炒香蒜末，加入西兰花翻炒至熟，加盐调味。');
INSERT INTO recipe(name,description,cooking_time,difficulty,servings,creator_id,visibility) VALUES('醋溜土豆丝','公共入门菜谱，可按口味调整。
1. 土豆去皮切细丝，用清水洗去表面淀粉后沥干。
2. 热锅放油，倒入土豆丝翻炒。
3. 加米醋和盐，炒至断生即可。',20,1,2,NULL,'public');
SET @starter_recipe_id=LAST_INSERT_ID();
INSERT INTO recipe_tag(recipe_id,tag_type,tag_value) VALUES(@starter_recipe_id,'cuisine','家常菜'),(@starter_recipe_id,'custom','公共入门');
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'2个',1 FROM ingredient WHERE name='土豆';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'1勺',1 FROM ingredient WHERE name='米醋';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'适量',0 FROM ingredient WHERE name='盐';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'适量',0 FROM ingredient WHERE name='菜籽油';
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,1,'土豆去皮切细丝，用清水洗去表面淀粉后沥干。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,2,'热锅放油，倒入土豆丝翻炒。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,3,'加米醋和盐，炒至断生即可。');
INSERT INTO recipe(name,description,cooking_time,difficulty,servings,creator_id,visibility) VALUES('香菇炖鸡','公共入门菜谱，可按口味调整。
1. 鸡腿切块，香菇洗净切块。
2. 鸡块冷水入锅煮出浮沫，捞出冲净。
3. 鸡块、香菇和姜放入锅，加热水与生抽。
4. 煮开后转小火炖至鸡肉熟透、软嫩，加盐调味。',50,1,2,NULL,'public');
SET @starter_recipe_id=LAST_INSERT_ID();
INSERT INTO recipe_tag(recipe_id,tag_type,tag_value) VALUES(@starter_recipe_id,'cuisine','家常菜'),(@starter_recipe_id,'custom','公共入门');
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'2只',1 FROM ingredient WHERE name='鸡腿';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'6朵',1 FROM ingredient WHERE name='香菇';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'3片',0 FROM ingredient WHERE name='生姜';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'1勺',0 FROM ingredient WHERE name='生抽';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'适量',0 FROM ingredient WHERE name='盐';
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,1,'鸡腿切块，香菇洗净切块。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,2,'鸡块冷水入锅煮出浮沫，捞出冲净。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,3,'鸡块、香菇和姜放入锅，加热水与生抽。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,4,'煮开后转小火炖至鸡肉熟透、软嫩，加盐调味。');
INSERT INTO recipe(name,description,cooking_time,difficulty,servings,creator_id,visibility) VALUES('紫菜蛋花汤','公共入门菜谱，可按口味调整。
1. 鸡蛋打散，紫菜检查并按包装说明处理。
2. 锅中加水烧开，放入紫菜。
3. 缓缓倒入蛋液，待蛋花凝固后加盐和芝麻油。',10,1,2,NULL,'public');
SET @starter_recipe_id=LAST_INSERT_ID();
INSERT INTO recipe_tag(recipe_id,tag_type,tag_value) VALUES(@starter_recipe_id,'cuisine','家常菜'),(@starter_recipe_id,'custom','公共入门');
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'一小把',1 FROM ingredient WHERE name='紫菜';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'1个',1 FROM ingredient WHERE name='鸡蛋';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'适量',0 FROM ingredient WHERE name='盐';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'几滴',0 FROM ingredient WHERE name='芝麻油';
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,1,'鸡蛋打散，紫菜检查并按包装说明处理。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,2,'锅中加水烧开，放入紫菜。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,3,'缓缓倒入蛋液，待蛋花凝固后加盐和芝麻油。');
INSERT INTO recipe(name,description,cooking_time,difficulty,servings,creator_id,visibility) VALUES('蒜蓉蒸虾','公共入门菜谱，可按口味调整。
1. 虾去虾线，洗净摆盘。
2. 蒜切末，用少量热油炒香，加生抽拌匀。
3. 蒜蓉铺在虾上，放入水已烧开的蒸锅，蒸至虾肉完全熟透。',20,1,2,NULL,'public');
SET @starter_recipe_id=LAST_INSERT_ID();
INSERT INTO recipe_tag(recipe_id,tag_type,tag_value) VALUES(@starter_recipe_id,'cuisine','粤菜'),(@starter_recipe_id,'custom','公共入门');
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'300克',1 FROM ingredient WHERE name='大虾';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'5瓣',1 FROM ingredient WHERE name='蒜';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'1勺',0 FROM ingredient WHERE name='生抽';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'适量',0 FROM ingredient WHERE name='菜籽油';
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,1,'虾去虾线，洗净摆盘。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,2,'蒜切末，用少量热油炒香，加生抽拌匀。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,3,'蒜蓉铺在虾上，放入水已烧开的蒸锅，蒸至虾肉完全熟透。');
INSERT INTO recipe(name,description,cooking_time,difficulty,servings,creator_id,visibility) VALUES('麻婆豆腐','公共入门菜谱，可按口味调整。
1. 豆腐切块，放入热水中浸热；淀粉加水调匀。
2. 热锅放油，炒熟牛肉末，再加入豆瓣酱炒香。
3. 加水煮开，放入豆腐，小火煮入味。
4. 加入淀粉水轻推至汤汁变稠，撒少量花椒粉。',25,1,2,NULL,'public');
SET @starter_recipe_id=LAST_INSERT_ID();
INSERT INTO recipe_tag(recipe_id,tag_type,tag_value) VALUES(@starter_recipe_id,'cuisine','川菜'),(@starter_recipe_id,'custom','公共入门');
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'1块',1 FROM ingredient WHERE name='嫩豆腐';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'80克',1 FROM ingredient WHERE name='牛肉末';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'1勺',0 FROM ingredient WHERE name='郫县豆瓣酱';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'少量',0 FROM ingredient WHERE name='花椒';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'1小勺',0 FROM ingredient WHERE name='玉米淀粉';
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,1,'豆腐切块，放入热水中浸热；淀粉加水调匀。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,2,'热锅放油，炒熟牛肉末，再加入豆瓣酱炒香。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,3,'加水煮开，放入豆腐，小火煮入味。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,4,'加入淀粉水轻推至汤汁变稠，撒少量花椒粉。');
INSERT INTO recipe(name,description,cooking_time,difficulty,servings,creator_id,visibility) VALUES('日式味噌汤','公共入门菜谱，可按口味调整。
1. 昆布放水中浸泡后加热，接近沸腾时捞出。
2. 豆腐切小块，与泡开的裙带菜一起加入汤中煮热。
3. 关小火，把味噌在少量汤中化开后倒回锅中拌匀。',15,1,2,NULL,'public');
SET @starter_recipe_id=LAST_INSERT_ID();
INSERT INTO recipe_tag(recipe_id,tag_type,tag_value) VALUES(@starter_recipe_id,'cuisine','日式'),(@starter_recipe_id,'custom','公共入门');
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'1勺',1 FROM ingredient WHERE name='味噌';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'半块',1 FROM ingredient WHERE name='绢豆腐';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'少量',0 FROM ingredient WHERE name='裙带菜';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'一小片',0 FROM ingredient WHERE name='昆布';
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,1,'昆布放水中浸泡后加热，接近沸腾时捞出。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,2,'豆腐切小块，与泡开的裙带菜一起加入汤中煮热。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,3,'关小火，把味噌在少量汤中化开后倒回锅中拌匀。');
INSERT INTO recipe(name,description,cooking_time,difficulty,servings,creator_id,visibility) VALUES('番茄意大利面','公共入门菜谱，可按口味调整。
1. 按面条包装说明煮面，留少量煮面水。
2. 蒜切末，番茄切丁。橄榄油加热后炒香蒜末。
3. 放入番茄，加少量盐煮成酱汁。
4. 面条加入酱汁，按需加煮面水，拌匀收汁。',25,1,2,NULL,'public');
SET @starter_recipe_id=LAST_INSERT_ID();
INSERT INTO recipe_tag(recipe_id,tag_type,tag_value) VALUES(@starter_recipe_id,'cuisine','意式'),(@starter_recipe_id,'custom','公共入门');
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'180克',1 FROM ingredient WHERE name='意大利面';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'2个',1 FROM ingredient WHERE name='番茄';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'2瓣',0 FROM ingredient WHERE name='蒜';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'适量',0 FROM ingredient WHERE name='橄榄油';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'适量',0 FROM ingredient WHERE name='盐';
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,1,'按面条包装说明煮面，留少量煮面水。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,2,'蒜切末，番茄切丁。橄榄油加热后炒香蒜末。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,3,'放入番茄，加少量盐煮成酱汁。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,4,'面条加入酱汁，按需加煮面水，拌匀收汁。');
INSERT INTO recipe(name,description,cooking_time,difficulty,servings,creator_id,visibility) VALUES('青椒炒腊肉','公共入门菜谱，可按口味调整。
1. 腊肉按包装说明处理后切薄片；青椒切块，蒜切片。
2. 锅内放少量油，炒熟腊肉至出油。
3. 加入蒜和青椒继续翻炒至熟；先尝味再决定是否加盐。',25,1,2,NULL,'public');
SET @starter_recipe_id=LAST_INSERT_ID();
INSERT INTO recipe_tag(recipe_id,tag_type,tag_value) VALUES(@starter_recipe_id,'cuisine','家常菜'),(@starter_recipe_id,'custom','公共入门');
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'150克',1 FROM ingredient WHERE name='腊肉';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'2个',1 FROM ingredient WHERE name='青椒';
INSERT INTO recipe_ingredient(recipe_id,ingredient_id,amount,is_main) SELECT @starter_recipe_id,id,'2瓣',0 FROM ingredient WHERE name='蒜';
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,1,'腊肉按包装说明处理后切薄片；青椒切块，蒜切片。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,2,'锅内放少量油，炒熟腊肉至出油。');
INSERT INTO cooking_step(recipe_id,step_number,description) VALUES(@starter_recipe_id,3,'加入蒜和青椒继续翻炒至熟；先尝味再决定是否加盐。');

-- Explicitly curated taste labels for the built-in starter recipes.
INSERT INTO recipe_tag(recipe_id,tag_type,tag_value)
SELECT id,'taste',CASE name WHEN '番茄炒蛋' THEN '酸甜' WHEN '醋溜土豆丝' THEN '酸爽' WHEN '麻婆豆腐' THEN '麻辣' WHEN '青椒炒腊肉' THEN '香辣' WHEN '番茄意大利面' THEN '酸甜' WHEN '清炒西兰花' THEN '清淡' ELSE '鲜香' END
FROM recipe WHERE creator_id IS NULL AND visibility='public';
