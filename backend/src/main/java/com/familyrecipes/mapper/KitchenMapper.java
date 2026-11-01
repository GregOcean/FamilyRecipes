package com.familyrecipes.mapper;
import com.familyrecipes.entity.Kitchen;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface KitchenMapper {
    @Select("SELECT k.*, g.name group_name FROM kitchen k JOIN group_chat g ON g.id=k.group_id JOIN group_member m ON m.group_id=k.group_id WHERE m.user_id=#{userId} ORDER BY k.group_id,k.id")
    List<Kitchen> list(Long userId);
    @Select("SELECT * FROM kitchen WHERE id=#{id}") Kitchen find(Long id);
    @Insert("INSERT INTO kitchen(group_id,name) VALUES(#{groupId},#{name})")
    @Options(useGeneratedKeys=true,keyProperty="id") void insert(Kitchen kitchen);
    @Update("UPDATE kitchen SET name=#{name} WHERE id=#{id}") void rename(Kitchen kitchen);
}
