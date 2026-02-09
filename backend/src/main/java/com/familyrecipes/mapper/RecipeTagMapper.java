package com.familyrecipes.mapper;

import com.familyrecipes.entity.RecipeTag;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 菜谱标签Mapper
 */
@Mapper
public interface RecipeTagMapper {

    @Select("SELECT * FROM recipe_tag WHERE recipe_id = #{recipeId}")
    List<RecipeTag> findByRecipeId(Long recipeId);

    @Insert("INSERT INTO recipe_tag(recipe_id, tag_type, tag_value) " +
            "VALUES(#{recipeId}, #{tagType}, #{tagValue})")
    int insert(RecipeTag tag);

    @Delete("DELETE FROM recipe_tag WHERE recipe_id = #{recipeId}")
    int deleteByRecipeId(Long recipeId);

    @Select("SELECT DISTINCT tag_value FROM recipe_tag WHERE tag_type = #{tagType} ORDER BY tag_value")
    List<String> findDistinctValuesByType(String tagType);
}

