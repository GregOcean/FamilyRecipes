package com.familyrecipes.mapper;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface RecommendationMapper {
 @Select("<script>SELECT r.id FROM recipe r WHERE " + RecipeMapper.VISIBLE +
   "<if test=\"mode == 'inventory'\"> AND EXISTS(SELECT 1 FROM recipe_cook c JOIN group_member gm ON gm.user_id=c.user_id JOIN kitchen k ON k.group_id=gm.group_id WHERE c.recipe_id=r.id AND k.id=#{kitchenId}) " +
   "AND EXISTS(SELECT 1 FROM recipe_ingredient ri JOIN fridge_item f ON f.ingredient_id=ri.ingredient_id WHERE ri.recipe_id=r.id AND f.kitchen_id=#{kitchenId} AND f.status IN ('normal','expiring','expired') AND f.expiry_date &gt;= #{today}) </if>" +
   " ORDER BY <choose><when test=\"mode == 'inventory'\"> " +
   "(SELECT COUNT(DISTINCT ri.ingredient_id) FROM recipe_ingredient ri JOIN fridge_item f ON f.ingredient_id=ri.ingredient_id WHERE ri.recipe_id=r.id AND f.kitchen_id=#{kitchenId} AND f.status IN ('normal','expiring','expired') AND f.expiry_date &gt;= #{today}) / GREATEST(1,(SELECT COUNT(*) FROM recipe_ingredient ri WHERE ri.recipe_id=r.id)) DESC, " +
   "</when><otherwise> " + RecommendationScore.SQL + " DESC, </otherwise></choose> r.id DESC LIMIT 20</script>")
 List<Long> recommend(@Param("viewerId")Long userId,@Param("kitchenId")Long kitchenId,@Param("mode")String mode,@Param("seed")String seed,@Param("today")java.time.LocalDate today);
}
