package com.familyrecipes.mapper;

import com.familyrecipes.entity.RecipeIngredient;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 菜谱食材关联Mapper
 */
@Mapper
public interface RecipeIngredientMapper {

    @Select("SELECT ri.*, i.name as 'ingredient.name', i.category as 'ingredient.category' " +
            "FROM recipe_ingredient ri " +
            "LEFT JOIN ingredient i ON ri.ingredient_id = i.id " +
            "WHERE ri.recipe_id = #{recipeId}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "recipeId", column = "recipe_id"),
        @Result(property = "ingredientId", column = "ingredient_id"),
        @Result(property = "amount", column = "amount"),
        @Result(property = "isMain", column = "is_main"),
        @Result(property = "ingredient.id", column = "ingredient_id"),
        @Result(property = "ingredient.name", column = "name"),
        @Result(property = "ingredient.category", column = "category")
    })
    List<RecipeIngredient> findByRecipeId(Long recipeId);

    @Insert("INSERT INTO recipe_ingredient(recipe_id, ingredient_id, amount, is_main) " +
            "VALUES(#{recipeId}, #{ingredientId}, #{amount}, #{isMain})")
    int insert(RecipeIngredient recipeIngredient);

    @Delete("DELETE FROM recipe_ingredient WHERE recipe_id = #{recipeId}")
    int deleteByRecipeId(Long recipeId);

    @Select("SELECT DISTINCT recipe_id FROM recipe_ingredient WHERE ingredient_id IN " +
            "<foreach collection='ingredientIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>")
    List<Long> findRecipeIdsByIngredientIds(@Param("ingredientIds") List<Long> ingredientIds);
}

