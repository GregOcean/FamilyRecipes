package com.familyrecipes.mapper;

import com.familyrecipes.entity.User;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 菜谱厨师关联Mapper
 */
@Mapper
public interface RecipeCookMapper {

    @Select("SELECT u.* FROM user u " +
            "INNER JOIN recipe_cook rc ON u.id = rc.user_id " +
            "WHERE rc.recipe_id = #{recipeId}")
    List<User> findCooksByRecipeId(Long recipeId);

    @Insert("INSERT INTO recipe_cook(recipe_id, user_id, skill_level) " +
            "VALUES(#{recipeId}, #{userId}, #{skillLevel})")
    int insert(@Param("recipeId") Long recipeId, @Param("userId") Long userId, 
               @Param("skillLevel") Integer skillLevel);

    @Delete("DELETE FROM recipe_cook WHERE recipe_id = #{recipeId} AND user_id = #{userId}")
    int delete(@Param("recipeId") Long recipeId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM recipe_cook WHERE recipe_id = #{recipeId} AND user_id = #{userId}")
    int existsByCookAndRecipe(@Param("recipeId") Long recipeId, @Param("userId") Long userId);
}

