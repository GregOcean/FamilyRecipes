package com.familyrecipes.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 用户差评Mapper
 */
@Mapper
public interface UserDislikeMapper {

    @Select("SELECT recipe_id FROM user_dislike WHERE user_id = #{userId}")
    List<Long> findRecipeIdsByUserId(Long userId);

    @Insert("INSERT INTO user_dislike(user_id, recipe_id) VALUES(#{userId}, #{recipeId})")
    int insert(@Param("userId") Long userId, @Param("recipeId") Long recipeId);

    @Delete("DELETE FROM user_dislike WHERE user_id = #{userId} AND recipe_id = #{recipeId}")
    int delete(@Param("userId") Long userId, @Param("recipeId") Long recipeId);

    @Select("SELECT COUNT(*) FROM user_dislike WHERE user_id = #{userId} AND recipe_id = #{recipeId}")
    int existsByUserAndRecipe(@Param("userId") Long userId, @Param("recipeId") Long recipeId);

    @Select("SELECT COUNT(*) FROM user_dislike WHERE recipe_id = #{recipeId}")
    int countByRecipeId(Long recipeId);
}

