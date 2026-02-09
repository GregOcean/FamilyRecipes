package com.familyrecipes.mapper;

import com.familyrecipes.entity.ExternalRecipe;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 外链食谱Mapper
 */
@Mapper
public interface ExternalRecipeMapper {

    @Select("SELECT * FROM external_recipe WHERE recipe_id = #{recipeId}")
    List<ExternalRecipe> findByRecipeId(Long recipeId);

    @Insert("INSERT INTO external_recipe(recipe_id, title, url, source, added_by) " +
            "VALUES(#{recipeId}, #{title}, #{url}, #{source}, #{addedBy})")
    int insert(ExternalRecipe externalRecipe);

    @Delete("DELETE FROM external_recipe WHERE id = #{id}")
    int delete(Long id);
}

