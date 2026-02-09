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

    @Select("SELECT fi.*, i.name as 'ingredient.name', i.category as 'ingredient.category' " +
            "FROM fridge_item fi " +
            "LEFT JOIN ingredient i ON fi.ingredient_id = i.id " +
            "WHERE fi.user_id = #{userId} AND fi.status != 'consumed' " +
            "ORDER BY fi.expiry_date")
    List<FridgeItem> findByUserId(Long userId);

    @Select("SELECT * FROM fridge_item WHERE id = #{id}")
    FridgeItem findById(Long id);

    @Insert("INSERT INTO fridge_item(user_id, ingredient_id, amount, purchase_date, " +
            "expiry_date, storage_location, status, notes) " +
            "VALUES(#{userId}, #{ingredientId}, #{amount}, #{purchaseDate}, #{expiryDate}, " +
            "#{storageLocation}, #{status}, #{notes})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FridgeItem item);

    @Update("UPDATE fridge_item SET amount=#{amount}, purchase_date=#{purchaseDate}, " +
            "expiry_date=#{expiryDate}, storage_location=#{storageLocation}, " +
            "status=#{status}, notes=#{notes} WHERE id=#{id}")
    int update(FridgeItem item);

    @Update("UPDATE fridge_item SET status='consumed', consumed_at=NOW() WHERE id=#{id}")
    int markAsConsumed(Long id);

    @Delete("DELETE FROM fridge_item WHERE id = #{id}")
    int delete(Long id);

    // 查找即将过期的食材
    @Select("SELECT fi.*, i.name as 'ingredient.name' FROM fridge_item fi " +
            "LEFT JOIN ingredient i ON fi.ingredient_id = i.id " +
            "WHERE fi.user_id = #{userId} AND fi.status = 'normal' " +
            "AND fi.expiry_date BETWEEN #{startDate} AND #{endDate}")
    List<FridgeItem> findExpiring(@Param("userId") Long userId, 
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    // 更新过期状态
    @Update("UPDATE fridge_item SET status = " +
            "CASE " +
            "  WHEN expiry_date < CURDATE() THEN 'expired' " +
            "  WHEN expiry_date <= DATE_ADD(CURDATE(), INTERVAL #{daysBeforeExpiry} DAY) THEN 'expiring' " +
            "  ELSE 'normal' " +
            "END " +
            "WHERE user_id = #{userId} AND status != 'consumed'")
    int updateExpiryStatus(@Param("userId") Long userId, @Param("daysBeforeExpiry") int daysBeforeExpiry);
}

