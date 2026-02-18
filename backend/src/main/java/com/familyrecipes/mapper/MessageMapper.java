package com.familyrecipes.mapper;

import com.familyrecipes.model.Message;
import com.familyrecipes.model.UserReadPosition;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 消息Mapper
 */
@Mapper
public interface MessageMapper {
    
    /**
     * 发送消息
     */
    @Insert("INSERT INTO message (group_id, sender_id, content, message_type) " +
            "VALUES (#{groupId}, #{senderId}, #{content}, #{messageType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMessage(Message message);
    
    /**
     * 获取群组消息列表（分页）
     */
    @Select("SELECT m.*, " +
            "u.id as 'sender.id', u.username as 'sender.username', " +
            "u.avatar as 'sender.avatar' " +
            "FROM message m " +
            "LEFT JOIN user u ON m.sender_id = u.id " +
            "WHERE m.group_id = #{groupId} " +
            "ORDER BY m.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "groupId", column = "group_id"),
        @Result(property = "senderId", column = "sender_id"),
        @Result(property = "content", column = "content"),
        @Result(property = "messageType", column = "message_type"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "sender.id", column = "sender.id"),
        @Result(property = "sender.username", column = "sender.username"),
        @Result(property = "sender.avatar", column = "sender.avatar")
    })
    List<Message> getMessages(@Param("groupId") Long groupId, 
                              @Param("offset") int offset, 
                              @Param("limit") int limit);
    
    /**
     * 获取群组最后一条消息
     */
    @Select("SELECT m.*, " +
            "u.id as 'sender.id', u.username as 'sender.username', " +
            "u.avatar as 'sender.avatar' " +
            "FROM message m " +
            "LEFT JOIN user u ON m.sender_id = u.id " +
            "WHERE m.group_id = #{groupId} " +
            "ORDER BY m.created_at DESC " +
            "LIMIT 1")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "groupId", column = "group_id"),
        @Result(property = "senderId", column = "sender_id"),
        @Result(property = "content", column = "content"),
        @Result(property = "messageType", column = "message_type"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "sender.id", column = "sender.id"),
        @Result(property = "sender.username", column = "sender.username"),
        @Result(property = "sender.avatar", column = "sender.avatar")
    })
    Message getLastMessage(@Param("groupId") Long groupId);
    
    /**
     * 获取用户在群组的读取位置
     */
    @Select("SELECT * FROM user_read_position WHERE user_id = #{userId} AND group_id = #{groupId}")
    UserReadPosition getReadPosition(@Param("userId") Long userId, @Param("groupId") Long groupId);
    
    /**
     * 更新用户读取位置
     */
    @Insert("INSERT INTO user_read_position (user_id, group_id, last_read_message_id) " +
            "VALUES (#{userId}, #{groupId}, #{lastReadMessageId}) " +
            "ON DUPLICATE KEY UPDATE last_read_message_id = #{lastReadMessageId}, " +
            "last_read_at = CURRENT_TIMESTAMP")
    int updateReadPosition(UserReadPosition position);
    
    /**
     * 获取未读消息数
     */
    @Select("SELECT COUNT(*) FROM message m " +
            "LEFT JOIN user_read_position urp ON m.group_id = urp.group_id AND urp.user_id = #{userId} " +
            "WHERE m.group_id = #{groupId} " +
            "AND (urp.last_read_message_id IS NULL OR m.id > urp.last_read_message_id) " +
            "AND m.sender_id != #{userId}")
    int getUnreadCount(@Param("userId") Long userId, @Param("groupId") Long groupId);
}

