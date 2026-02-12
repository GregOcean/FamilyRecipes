package com.familyrecipes.mapper;

import com.familyrecipes.entity.Recipe;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

/**
 * 菜谱Mapper
 */
@Mapper
public interface RecipeMapper {

    @Select("SELECT * FROM recipe WHERE id = #{id}")
    Recipe findById(Long id);

    @Insert("INSERT INTO recipe(name, description, cover_image, cooking_time, difficulty, " +
            "servings, creator_id) VALUES(#{name}, #{description}, #{coverImage}, " +
            "#{cookingTime}, #{difficulty}, #{servings}, #{creatorId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Recipe recipe);

    @Update("UPDATE recipe SET name=#{name}, description=#{description}, " +
            "cover_image=#{coverImage}, cooking_time=#{cookingTime}, difficulty=#{difficulty}, " +
            "servings=#{servings} WHERE id=#{id}")
    int update(Recipe recipe);

    @Delete("DELETE FROM recipe WHERE id = #{id}")
    int delete(Long id);

    // 复杂查询使用XML
    List<Recipe> search(Map<String, Object> params);
    
    Long countSearch(Map<String, Object> params);

    @Update("UPDATE recipe SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(Long id);
    
    @Update("UPDATE recipe SET favorite_count = favorite_count + 1 WHERE id = #{id}")
    int incrementFavoriteCount(Long id);
    
    @Update("UPDATE recipe SET favorite_count = GREATEST(favorite_count - 1, 0) WHERE id = #{id}")
    int decrementFavoriteCount(Long id);
    
    @Update("UPDATE recipe SET dislike_count = dislike_count + 1 WHERE id = #{id}")
    int incrementDislikeCount(Long id);
    
    @Update("UPDATE recipe SET dislike_count = GREATEST(dislike_count - 1, 0) WHERE id = #{id}")
    int decrementDislikeCount(Long id);

    @Update("UPDATE recipe SET recently_cooked_count = recently_cooked_count + 1, " +
            "last_cooked_at = NOW() WHERE id = #{id}")
    int incrementCookedCount(Long id);

    @Select("SELECT * FROM recipe WHERE creator_id = #{userId} ORDER BY created_at DESC")
    List<Recipe> findByCreatorId(Long userId);

    // 获取最近常做的菜
    @Select("SELECT * FROM recipe WHERE recently_cooked_count > 0 " +
            "ORDER BY recently_cooked_count DESC, last_cooked_at DESC LIMIT #{limit}")
    List<Recipe> findRecentlyCooked(int limit);
    
    // 按关键词搜索菜谱（用于全局搜索）
    @Select("SELECT * FROM recipe WHERE name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR description LIKE CONCAT('%', #{keyword}, '%')")
    List<Recipe> searchByKeyword(String keyword);
}

