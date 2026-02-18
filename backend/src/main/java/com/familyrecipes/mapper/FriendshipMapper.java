package com.familyrecipes.mapper;

import com.familyrecipes.entity.User;
import com.familyrecipes.model.Friendship;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 好友关系Mapper
 */
@Mapper
public interface FriendshipMapper {
    
    /**
     * 添加好友关系（双向）
     */
    @Insert("INSERT INTO friendship (user_id, friend_id, nickname, status) " +
            "VALUES (#{userId}, #{friendId}, #{nickname}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertFriendship(Friendship friendship);
    
    /**
     * 根据用户ID和好友ID查询好友关系
     */
    @Select("SELECT * FROM friendship WHERE user_id = #{userId} AND friend_id = #{friendId}")
    Friendship getFriendship(@Param("userId") Long userId, @Param("friendId") Long friendId);
    
    /**
     * 获取用户的好友列表
     */
    @Select("SELECT f.*, " +
            "u.id as 'friend.id', u.username as 'friend.username', " +
            "u.email as 'friend.email', u.avatar as 'friend.avatar', " +
            "u.created_at as 'friend.created_at' " +
            "FROM friendship f " +
            "LEFT JOIN user u ON f.friend_id = u.id " +
            "WHERE f.user_id = #{userId} AND f.status = 'accepted' " +
            "ORDER BY f.created_at DESC")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "friendId", column = "friend_id"),
        @Result(property = "nickname", column = "nickname"),
        @Result(property = "status", column = "status"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "friend.id", column = "friend.id"),
        @Result(property = "friend.username", column = "friend.username"),
        @Result(property = "friend.email", column = "friend.email"),
        @Result(property = "friend.avatar", column = "friend.avatar"),
        @Result(property = "friend.createdAt", column = "friend.created_at")
    })
    List<Friendship> getFriendsList(@Param("userId") Long userId);
    
    /**
     * 删除好友关系
     */
    @Delete("DELETE FROM friendship WHERE user_id = #{userId} AND friend_id = #{friendId}")
    int deleteFriendship(@Param("userId") Long userId, @Param("friendId") Long friendId);
    
    /**
     * 更新好友备注
     */
    @Update("UPDATE friendship SET nickname = #{nickname} WHERE user_id = #{userId} AND friend_id = #{friendId}")
    int updateNickname(@Param("userId") Long userId, @Param("friendId") Long friendId, @Param("nickname") String nickname);
    
    /**
     * 检查是否为好友关系
     */
    @Select("SELECT COUNT(*) > 0 FROM friendship " +
            "WHERE user_id = #{userId} AND friend_id = #{friendId} AND status = 'accepted'")
    boolean isFriend(@Param("userId") Long userId, @Param("friendId") Long friendId);
}

