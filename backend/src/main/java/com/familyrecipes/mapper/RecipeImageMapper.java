package com.familyrecipes.mapper;
import com.familyrecipes.entity.RecipeImage;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface RecipeImageMapper {
 @Select("SELECT url,category,caption,position FROM recipe_image WHERE recipe_id=#{id} ORDER BY position,id") List<RecipeImage> list(Long id);
 @Delete("DELETE FROM recipe_image WHERE recipe_id=#{id}") void delete(Long id);
 @Insert("INSERT INTO recipe_image(recipe_id,url,category,caption,position) VALUES(#{recipeId},#{image.url},#{image.category},#{image.caption},#{image.position})")
 void insert(@Param("recipeId")Long recipeId,@Param("image")RecipeImage image);
}
