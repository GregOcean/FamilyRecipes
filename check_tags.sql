-- 查看所有菜谱及其tag
SELECT 
    r.id,
    r.name,
    rt.tag_type,
    rt.tag_value
FROM recipe r
LEFT JOIN recipe_tag rt ON r.id = rt.recipe_id
WHERE r.name LIKE '%红烧牛肉%'
ORDER BY r.id, rt.id;

-- 查看所有tag
SELECT * FROM recipe_tag ORDER BY recipe_id, id;

