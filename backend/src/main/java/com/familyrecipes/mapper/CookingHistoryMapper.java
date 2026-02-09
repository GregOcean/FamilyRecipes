package com.familyrecipes.mapper;

import com.familyrecipes.entity.CookingHistory;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 烹饪记录Mapper
 */
@Mapper
public interface CookingHistoryMapper {

    @Select("SELECT * FROM cooking_history WHERE recipe_id = #{recipeId} " +
            "ORDER BY cooked_at DESC LIMIT #{limit}")
    List<CookingHistory> findByRecipeId(@Param("recipeId") Long recipeId, @Param("limit") int limit);

    @Select("SELECT * FROM cooking_history WHERE user_id = #{userId} " +
            "ORDER BY cooked_at DESC LIMIT #{limit}")
    List<CookingHistory> findByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    @Insert("INSERT INTO cooking_history(recipe_id, user_id, rating, notes, images) " +
            "VALUES(#{recipeId}, #{userId}, #{rating}, #{notes}, #{images})")
    int insert(CookingHistory history);
}

