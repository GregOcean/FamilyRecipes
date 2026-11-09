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
    String MEAL_SHARED = "EXISTS (SELECT 1 FROM meal_arrangement ma JOIN meal_share ms ON ms.meal_id=ma.id JOIN group_member mg ON mg.group_id=ms.group_id WHERE mg.user_id=#{viewerId} AND JSON_CONTAINS(ma.dishes, JSON_OBJECT('recipe_id', r.id)))";
    String CHAT_SHARED = "EXISTS (SELECT 1 FROM recipe_chat_share cs JOIN group_member cg ON cg.group_id=cs.group_id WHERE cs.recipe_id=r.id AND cg.user_id=#{viewerId})";
    String VISIBLE = "(" + "(r.visibility = 'public' OR r.creator_id = #{viewerId} OR (r.visibility = 'group' AND EXISTS (SELECT 1 FROM group_member gm WHERE gm.user_id = #{viewerId} AND (gm.group_id = r.owner_group_id OR JSON_CONTAINS(COALESCE(r.shared_group_ids, '[]'), CAST(gm.group_id AS JSON))))))" + " OR " + MEAL_SHARED + " OR " + CHAT_SHARED + ")";

    @Select("SELECT COUNT(*) > 0 FROM recipe r WHERE r.id = #{id} AND " + VISIBLE)
    boolean isVisible(@Param("id") Long id, @Param("viewerId") Long viewerId);

    @Select("SELECT * FROM recipe WHERE id = #{id} FOR UPDATE")
    Recipe lockById(Long id);


    @Select("SELECT * FROM recipe WHERE id = #{id}")
    Recipe findById(Long id);

    @Insert("INSERT INTO recipe(name, description, cover_image, cooking_time, difficulty, " +
            "servings, creator_id, visibility, shared_group_ids, owner_group_id, kitchen_id, source_recipe_id, source_recipe_name) " +
            "VALUES(#{name}, #{description}, #{coverImage}, " +
            "#{cookingTime}, #{difficulty}, #{servings}, #{creatorId}, " +
            "#{visibility}, #{sharedGroupIds}, #{ownerGroupId}, #{kitchenId}, #{sourceRecipeId}, #{sourceRecipeName})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Recipe recipe);

    @Update("UPDATE recipe SET name=#{name}, description=#{description}, " +
            "cover_image=#{coverImage}, cooking_time=#{cookingTime}, difficulty=#{difficulty}, " +
            "servings=#{servings}, visibility=#{visibility}, shared_group_ids=#{sharedGroupIds}, " +
            "kitchen_id=#{kitchenId}, owner_group_id=#{ownerGroupId} WHERE id=#{id} AND creator_id=#{creatorId}")
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
    @Select("SELECT r.* FROM recipe r WHERE recently_cooked_count > 0 AND " + VISIBLE +
            " ORDER BY recently_cooked_count DESC, last_cooked_at DESC LIMIT #{limit}")
    List<Recipe> findRecentlyCooked(@Param("limit") int limit, @Param("viewerId") Long viewerId);
    
    // 按关键词搜索菜谱（用于全局搜索）
    @Select("SELECT r.* FROM recipe r WHERE (name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR description LIKE CONCAT('%', #{keyword}, '%') OR (EXISTS(SELECT 1 FROM content_translations ct WHERE ct.source_hash=SHA2(r.name,256) AND ct.kind='content' AND ct.status='ready' AND ct.translated_text LIKE CONCAT('%',#{keyword},'%')) OR EXISTS(SELECT 1 FROM content_translations ct WHERE ct.source_hash=SHA2(r.description,256) AND ct.kind='content' AND ct.status='ready' AND ct.translated_text LIKE CONCAT('%',#{keyword},'%')))) AND " + VISIBLE)
    List<Recipe> searchByKeyword(@Param("keyword") String keyword, @Param("viewerId") Long viewerId);
    
    // 按tag精确搜索菜谱
    @Select("SELECT DISTINCT r.* FROM recipe r " +
            "INNER JOIN recipe_tag rt ON r.id = rt.recipe_id " +
            "WHERE rt.tag_value = #{tagValue} AND " + VISIBLE)
    List<Recipe> searchByTag(@Param("tagValue") String tagValue, @Param("viewerId") Long viewerId);
    @Select("SELECT DISTINCT rt.tag_value FROM recipe_tag rt JOIN recipe r ON r.id=rt.recipe_id WHERE rt.tag_type=#{tagType} AND " + VISIBLE)
    List<String> visibleTags(@Param("tagType") String tagType, @Param("viewerId") Long viewerId);
}

