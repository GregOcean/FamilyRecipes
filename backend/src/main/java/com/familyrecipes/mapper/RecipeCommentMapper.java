package com.familyrecipes.mapper;

import com.familyrecipes.entity.RecipeComment;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 菜谱评论Mapper
 */
@Mapper
public interface RecipeCommentMapper {

    @Select("SELECT c.*, u.id AS \"user.id\", u.username AS \"user.username\", u.avatar AS \"user.avatar\" " +
            "FROM recipe_comment c JOIN user u ON u.id = c.user_id " +
            "WHERE c.recipe_id = #{recipeId} ORDER BY c.created_at ASC")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "recipeId", column = "recipe_id"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "content", column = "content"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "user.id", column = "user.id"),
        @Result(property = "user.username", column = "user.username"),
        @Result(property = "user.avatar", column = "user.avatar")
    })
    List<RecipeComment> findByRecipeId(@Param("recipeId") Long recipeId);

    @Insert("INSERT INTO recipe_comment(recipe_id, user_id, content) VALUES(#{recipeId}, #{userId}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RecipeComment comment);

    @Select("SELECT * FROM recipe_comment WHERE id = #{id}")
    RecipeComment findById(Long id);

    @Delete("DELETE FROM recipe_comment WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM recipe_comment WHERE recipe_id = #{recipeId}")
    int countByRecipeId(Long recipeId);
}
