package com.familyrecipes.mapper;

import com.familyrecipes.entity.CookingStep;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 烹饪步骤Mapper
 */
@Mapper
public interface CookingStepMapper {

    @Select("SELECT * FROM cooking_step WHERE recipe_id = #{recipeId} ORDER BY step_number")
    List<CookingStep> findByRecipeId(Long recipeId);

    @Insert("INSERT INTO cooking_step(recipe_id, step_number, description, image, duration) " +
            "VALUES(#{recipeId}, #{stepNumber}, #{description}, #{image}, #{duration})")
    int insert(CookingStep step);

    @Delete("DELETE FROM cooking_step WHERE recipe_id = #{recipeId}")
    int deleteByRecipeId(Long recipeId);
}

