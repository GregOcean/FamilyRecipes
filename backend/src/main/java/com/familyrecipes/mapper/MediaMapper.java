package com.familyrecipes.mapper;
import org.apache.ibatis.annotations.*;

@Mapper
public interface MediaMapper {
    @Insert("INSERT INTO media_file(filename,owner_id) VALUES(#{filename},#{ownerId})")
    void insert(@Param("filename") String filename, @Param("ownerId") Long ownerId);
    @Select("SELECT owner_id FROM media_file WHERE filename=#{filename} AND deleted_at IS NULL FOR UPDATE")
    Long owner(String filename);
    @Select("SELECT COUNT(*) FROM recipe r WHERE " + RecipeMapper.VISIBLE +
        " AND (r.cover_image=#{url} OR EXISTS(SELECT 1 FROM cooking_step s WHERE s.recipe_id=r.id AND s.image=#{url}) OR EXISTS(SELECT 1 FROM recipe_image ri WHERE ri.recipe_id=r.id AND ri.url=#{url}) OR EXISTS(SELECT 1 FROM external_recipe e WHERE e.recipe_id=r.id AND e.thumbnail=#{url}))")
    int visibleReferences(@Param("url") String url, @Param("viewerId") Long viewerId);

    @Update("UPDATE media_file SET category=#{category} WHERE filename=#{filename}")
    void category(@Param("filename")String filename,@Param("category")String category);
    @Select("SELECT category FROM media_file WHERE filename=#{filename} AND deleted_at IS NULL") String categoryOf(String filename);
    @Select("SELECT COUNT(*) FROM fridge_item f JOIN kitchen k ON k.id=f.kitchen_id JOIN group_member gm ON gm.group_id=k.group_id WHERE gm.user_id=#{viewerId} AND f.image_url=#{url}")
    int fridgeReferences(@Param("url")String url,@Param("viewerId")Long viewerId);
    @Select("SELECT COUNT(*) FROM message m JOIN group_member gm ON gm.group_id=m.group_id WHERE gm.user_id=#{viewerId} AND m.message_type='sticker' AND m.content=#{url}")
    int stickerReferences(@Param("url")String url,@Param("viewerId")Long viewerId);
    @Select("SELECT (SELECT COUNT(*) FROM recipe WHERE cover_image=#{url}) + (SELECT COUNT(*) FROM recipe_image WHERE url=#{url}) + (SELECT COUNT(*) FROM cooking_step WHERE image=#{url}) + (SELECT COUNT(*) FROM external_recipe WHERE thumbnail=#{url}) + (SELECT COUNT(*) FROM fridge_item WHERE image_url=#{url}) + (SELECT COUNT(*) FROM message WHERE message_type='sticker' AND content=#{url}) + (SELECT COUNT(*) FROM user WHERE avatar=#{url}) + (SELECT COUNT(*) FROM group_chat WHERE avatar=#{url}) + (SELECT COUNT(*) FROM cooking_history WHERE JSON_VALID(images) AND JSON_CONTAINS(images,JSON_QUOTE(#{url})))")
    int references(String url);
    @Select("SELECT filename FROM media_file WHERE filename>#{after} AND purged_at IS NULL AND (deleted_at IS NOT NULL OR created_at < NOW() - INTERVAL 1 DAY) ORDER BY filename LIMIT 500")
    java.util.List<String> candidates(String after);
    @Select("SELECT filename,category,deleted_at,purge_after,purged_at FROM media_file WHERE owner_id=#{userId} AND deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    java.util.List<java.util.Map<String,Object>> trash(Long userId);
    @Select("SELECT * FROM media_file WHERE filename=#{filename} FOR UPDATE") java.util.Map<String,Object> lock(String filename);
    @Update("UPDATE media_file SET deleted_at=NOW(),purge_after=NOW()+INTERVAL 30 DAY WHERE filename=#{filename} AND deleted_at IS NULL") void trashFile(String filename);
    @Update("UPDATE media_file SET deleted_at=NULL,purge_after=NULL WHERE filename=#{filename} AND purged_at IS NULL") void restore(String filename);
    @Select("SELECT COUNT(*) FROM media_file WHERE filename=#{filename} AND purge_after<=NOW() AND purged_at IS NULL") int due(String filename);
    @Update("UPDATE media_file SET purged_at=NOW() WHERE filename=#{filename}") void purged(String filename);
}
