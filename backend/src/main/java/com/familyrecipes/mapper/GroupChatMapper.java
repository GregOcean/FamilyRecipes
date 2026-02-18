package com.familyrecipes.mapper;

import com.familyrecipes.model.GroupChat;
import com.familyrecipes.model.GroupMember;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 群组Mapper
 */
@Mapper
public interface GroupChatMapper {
    
    /**
     * 创建群组
     */
    @Insert("INSERT INTO group_chat (name, avatar, creator_id, manager_id, member_count, max_members, description) " +
            "VALUES (#{name}, #{avatar}, #{creatorId}, #{managerId}, #{memberCount}, #{maxMembers}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertGroup(GroupChat group);
    
    /**
     * 根据ID查询群组
     */
    @Select("SELECT * FROM group_chat WHERE id = #{id}")
    GroupChat getGroupById(@Param("id") Long id);
    
    /**
     * 获取用户的群组列表
     */
    @Select("SELECT g.* FROM group_chat g " +
            "INNER JOIN group_member gm ON g.id = gm.group_id " +
            "WHERE gm.user_id = #{userId} " +
            "ORDER BY g.updated_at DESC")
    List<GroupChat> getUserGroups(@Param("userId") Long userId);
    
    /**
     * 更新群组信息
     */
    @Update("UPDATE group_chat SET name = #{name}, avatar = #{avatar}, " +
            "description = #{description} WHERE id = #{id}")
    int updateGroup(GroupChat group);
    
    /**
     * 更新群组成员数量
     */
    @Update("UPDATE group_chat SET member_count = #{memberCount} WHERE id = #{id}")
    int updateMemberCount(@Param("id") Long id, @Param("memberCount") Integer memberCount);
    
    /**
     * 删除群组
     */
    @Delete("DELETE FROM group_chat WHERE id = #{id}")
    int deleteGroup(@Param("id") Long id);
    
    /**
     * 添加群成员
     */
    @Insert("INSERT INTO group_member (group_id, user_id, role, nickname) " +
            "VALUES (#{groupId}, #{userId}, #{role}, #{nickname})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertGroupMember(GroupMember member);
    
    /**
     * 获取群成员列表
     */
    @Select("SELECT gm.*, " +
            "u.id as 'user.id', u.username as 'user.username', " +
            "u.email as 'user.email', u.avatar as 'user.avatar' " +
            "FROM group_member gm " +
            "LEFT JOIN user u ON gm.user_id = u.id " +
            "WHERE gm.group_id = #{groupId} " +
            "ORDER BY gm.role ASC, gm.joined_at ASC")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "groupId", column = "group_id"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "role", column = "role"),
        @Result(property = "nickname", column = "nickname"),
        @Result(property = "joinedAt", column = "joined_at"),
        @Result(property = "user.id", column = "user.id"),
        @Result(property = "user.username", column = "user.username"),
        @Result(property = "user.email", column = "user.email"),
        @Result(property = "user.avatar", column = "user.avatar")
    })
    List<GroupMember> getGroupMembers(@Param("groupId") Long groupId);
    
    /**
     * 获取群成员信息
     */
    @Select("SELECT * FROM group_member WHERE group_id = #{groupId} AND user_id = #{userId}")
    GroupMember getGroupMember(@Param("groupId") Long groupId, @Param("userId") Long userId);
    
    /**
     * 更新群成员角色
     */
    @Update("UPDATE group_member SET role = #{role} WHERE group_id = #{groupId} AND user_id = #{userId}")
    int updateMemberRole(@Param("groupId") Long groupId, @Param("userId") Long userId, @Param("role") String role);
    
    /**
     * 删除群成员
     */
    @Delete("DELETE FROM group_member WHERE group_id = #{groupId} AND user_id = #{userId}")
    int deleteGroupMember(@Param("groupId") Long groupId, @Param("userId") Long userId);
    
    /**
     * 获取群成员数量
     */
    @Select("SELECT COUNT(*) FROM group_member WHERE group_id = #{groupId}")
    int getMemberCount(@Param("groupId") Long groupId);
    
    /**
     * 检查用户是否在群中
     */
    @Select("SELECT COUNT(*) > 0 FROM group_member WHERE group_id = #{groupId} AND user_id = #{userId}")
    boolean isGroupMember(@Param("groupId") Long groupId, @Param("userId") Long userId);
}

