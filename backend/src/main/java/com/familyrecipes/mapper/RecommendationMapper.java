package com.familyrecipes.mapper;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface RecommendationMapper {
 @Select("<script>SELECT r.id FROM recipe r WHERE " + RecipeMapper.VISIBLE +
   " AND NOT EXISTS(SELECT 1 FROM user_dislike d WHERE d.user_id=#{viewerId} AND d.recipe_id=r.id) " +
   "<if test=\"mode == 'inventory'\"> AND EXISTS(SELECT 1 FROM recipe_cook c JOIN group_member gm ON gm.user_id=c.user_id JOIN kitchen k ON k.group_id=gm.group_id WHERE c.recipe_id=r.id AND k.id=#{kitchenId}) " +
   "AND EXISTS(SELECT 1 FROM recipe_ingredient ri JOIN fridge_item f ON f.ingredient_id=ri.ingredient_id WHERE ri.recipe_id=r.id AND f.kitchen_id=#{kitchenId} AND f.status IN ('normal','expiring','expired') AND f.expiry_date &gt;= #{today}) </if>" +
   " ORDER BY <choose><when test=\"mode == 'inventory'\"> " +
   "(SELECT COUNT(DISTINCT ri.ingredient_id) FROM recipe_ingredient ri JOIN fridge_item f ON f.ingredient_id=ri.ingredient_id WHERE ri.recipe_id=r.id AND f.kitchen_id=#{kitchenId} AND f.status IN ('normal','expiring','expired') AND f.expiry_date &gt;= #{today}) / GREATEST(1,(SELECT COUNT(*) FROM recipe_ingredient ri WHERE ri.recipe_id=r.id)) DESC, " +
   "</when><otherwise> (LEAST(5,(SELECT COUNT(*) FROM recipe_tag t WHERE t.recipe_id=r.id AND EXISTS(SELECT 1 FROM recipe_tag liked JOIN user_favorite uf ON uf.recipe_id=liked.recipe_id WHERE uf.user_id=#{viewerId} AND liked.tag_type=t.tag_type AND liked.tag_value=t.tag_value))) + " +
   "LEAST(3,(SELECT COUNT(*) FROM cooking_history h WHERE h.user_id=#{viewerId} AND h.recipe_id=r.id)) + MOD(CRC32(CONCAT(r.id,':',#{seed})),100)/20.0) DESC, </otherwise></choose> r.id DESC LIMIT 20</script>")
 List<Long> recommend(@Param("viewerId")Long userId,@Param("kitchenId")Long kitchenId,@Param("mode")String mode,@Param("seed")String seed,@Param("today")java.time.LocalDate today);
}
