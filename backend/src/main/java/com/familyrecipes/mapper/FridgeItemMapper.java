package com.familyrecipes.mapper;

import com.familyrecipes.entity.FridgeItem;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.util.List;

/**
 * 冰箱食材Mapper
 */
@Mapper
public interface FridgeItemMapper {

    @Select("SELECT fi.*, (SELECT name FROM inventory_categories WHERE id=fi.category_override_id) category_override_name, (SELECT username FROM user WHERE id=fi.owner_user_id) owner_username, i.name as 'ingredient.name', i.aliases as 'ingredient.aliases', i.category as 'ingredient.category', i.category_id as 'ingredient.categoryId' " +
            "FROM fridge_item fi " +
            "LEFT JOIN ingredient i ON fi.ingredient_id = i.id " +
            "WHERE fi.kitchen_id = #{kitchenId} AND fi.status IN ('normal','expiring','expired') " +
            "ORDER BY fi.expiry_date")
    @Results(id="fridgeWithIngredient", value={
        @Result(column="id", property="id"),
        @Result(column="ingredient_id", property="ingredientId"),
        @Result(column="ingredient_id", property="ingredient.id"),
        @Result(column="ingredient.name", property="ingredient.name"),
        @Result(column="ingredient.aliases", property="ingredient.aliases"),
        @Result(column="ingredient.category", property="ingredient.category"),
        @Result(column="ingredient.categoryId", property="ingredient.categoryId")
    })
    List<FridgeItem> findByKitchenId(Long kitchenId);
    
    @Select("SELECT fi.*, (SELECT name FROM inventory_categories WHERE id=fi.category_override_id) category_override_name, (SELECT username FROM user WHERE id=fi.owner_user_id) owner_username, i.name as 'ingredient.name', i.aliases as 'ingredient.aliases', i.category as 'ingredient.category', i.category_id as 'ingredient.categoryId' " +
            "FROM fridge_item fi " +
            "LEFT JOIN ingredient i ON fi.ingredient_id = i.id " +
            "WHERE fi.kitchen_id = #{kitchenId} AND fi.status = 'consumed' " +
            "ORDER BY fi.consumed_at DESC")
    @ResultMap("fridgeWithIngredient")
    List<FridgeItem> findConsumedByKitchenId(Long kitchenId);

    @Select("SELECT * FROM fridge_item WHERE id = #{id}")
    FridgeItem findById(Long id);

    @Insert("INSERT INTO fridge_item(user_id, kitchen_id, ingredient_id, amount, purchase_date, " +
            "expiry_date, storage_location, status, notes, owner_user_id, image_url, category_override_id) " +
            "VALUES(#{userId}, #{kitchenId}, #{ingredientId}, #{amount}, #{purchaseDate}, #{expiryDate}, " +
            "#{storageLocation}, #{status}, #{notes}, #{ownerUserId}, #{imageUrl}, #{categoryOverrideId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FridgeItem item);

    @Update("UPDATE fridge_item SET amount=#{amount}, purchase_date=#{purchaseDate}, " +
            "expiry_date=#{expiryDate}, storage_location=#{storageLocation}, " +
            "category_override_id=#{categoryOverrideId}, notes=#{notes}, owner_user_id=#{ownerUserId}, image_url=#{imageUrl} WHERE id=#{id} AND kitchen_id=#{kitchenId}")
    int update(FridgeItem item);

    @Update("UPDATE fridge_item SET status='consumed', consumed_at=NOW() WHERE id=#{id} AND kitchen_id=#{kitchenId} AND status IN ('normal','expiring','expired')")
    int markAsConsumed(@Param("id") Long id, @Param("kitchenId") Long kitchenId);

    @Update("UPDATE fridge_item SET status=#{status}, consumed_at=CASE WHEN #{status}='consumed' THEN NOW() ELSE NULL END WHERE id=#{id} AND kitchen_id=#{kitchenId}")
    int changeStatus(@Param("id") Long id,@Param("kitchenId") Long kitchenId,@Param("status") String status);

    @Delete("DELETE FROM fridge_item WHERE id = #{id} AND kitchen_id = #{kitchenId}")
    int delete(@Param("id") Long id, @Param("kitchenId") Long kitchenId);

    // 查找即将过期的食材
    @Select("SELECT fi.*, (SELECT name FROM inventory_categories WHERE id=fi.category_override_id) category_override_name, (SELECT username FROM user WHERE id=fi.owner_user_id) owner_username, i.name as 'ingredient.name', i.aliases as 'ingredient.aliases', i.category as 'ingredient.category', i.category_id as 'ingredient.categoryId', ou.username as owner_username FROM fridge_item fi " +
            "LEFT JOIN ingredient i ON fi.ingredient_id = i.id " +
            "LEFT JOIN user ou ON fi.owner_user_id = ou.id " +
            "WHERE EXISTS(SELECT 1 FROM kitchen k JOIN group_member gm ON gm.group_id=k.group_id WHERE k.id=fi.kitchen_id AND gm.user_id=#{userId}) AND fi.status IN ('normal','expiring','expired') " +
            "AND fi.expiry_date BETWEEN #{startDate} AND #{endDate}")
    @ResultMap("fridgeWithIngredient")
    List<FridgeItem> findExpiring(@Param("userId") Long userId, 
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    // 更新过期状态
    @Update("UPDATE fridge_item SET status = " +
            "CASE " +
            "  WHEN expiry_date < #{today} THEN 'expired' " +
            "  WHEN expiry_date <= DATE_ADD(#{today}, INTERVAL #{daysBeforeExpiry} DAY) THEN 'expiring' " +
            "  ELSE 'normal' " +
            "END " +
            "WHERE kitchen_id = #{kitchenId} AND status IN ('normal','expiring','expired')")
    int updateExpiryStatus(@Param("kitchenId") Long kitchenId, @Param("daysBeforeExpiry") int daysBeforeExpiry, @Param("today") LocalDate today);
}

