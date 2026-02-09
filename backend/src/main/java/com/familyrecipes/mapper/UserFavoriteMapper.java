package com.familyrecipes.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 用户收藏Mapper
 */
@Mapper
public interface UserFavoriteMapper {

    @Select("SELECT recipe_id FROM user_favorite WHERE user_id = #{userId}")
    List<Long> findRecipeIdsByUserId(Long userId);

    @Insert("INSERT INTO user_favorite(user_id, recipe_id) VALUES(#{userId}, #{recipeId})")
    int insert(@Param("userId") Long userId, @Param("recipeId") Long recipeId);

    @Delete("DELETE FROM user_favorite WHERE user_id = #{userId} AND recipe_id = #{recipeId}")
    int delete(@Param("userId") Long userId, @Param("recipeId") Long recipeId);

    @Select("SELECT COUNT(*) FROM user_favorite WHERE user_id = #{userId} AND recipe_id = #{recipeId}")
    int existsByUserAndRecipe(@Param("userId") Long userId, @Param("recipeId") Long recipeId);

    @Select("SELECT COUNT(*) FROM user_favorite WHERE recipe_id = #{recipeId}")
    int countByRecipeId(Long recipeId);
}

