package com.familyrecipes.service;

import com.familyrecipes.mapper.GroupChatMapper;
import com.familyrecipes.mapper.MessageMapper;
import com.familyrecipes.model.Message;
import com.familyrecipes.model.UserReadPosition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息服务
 */
@Slf4j
@Service
public class MessageService {
    
    @Autowired
    private MessageMapper messageMapper;
    
    @Autowired
    private GroupChatMapper groupChatMapper;
    
    /**
     * 发送消息
     */
    public Message sendMessage(Long groupId, Long senderId, String content, Message.MessageType messageType) {
        // 检查用户是否在群中
        if (!groupChatMapper.isGroupMember(groupId, senderId)) {
            throw new RuntimeException("您不在该群组中");
        }
        
        Message message = new Message();
        message.setGroupId(groupId);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setMessageType(messageType != null ? messageType : Message.MessageType.text);
        
        messageMapper.insertMessage(message);
        
        log.info("用户{}在群组{}发送了消息", senderId, groupId);
        return message;
    }
    
    /**
     * 获取群组消息列表
     */
    public List<Message> getMessages(Long groupId, Long userId, int pageNum, int pageSize) {
        // 检查用户是否在群中
        if (!groupChatMapper.isGroupMember(groupId, userId)) {
            throw new RuntimeException("您不在该群组中");
        }
        
        int offset = (pageNum - 1) * pageSize;
        return messageMapper.getMessages(groupId, offset, pageSize);
    }
    
    /**
     * 标记消息已读
     */
    public void markAsRead(Long groupId, Long userId, Long messageId) {
        // 检查用户是否在群中
        if (!groupChatMapper.isGroupMember(groupId, userId)) {
            throw new RuntimeException("您不在该群组中");
        }
        
        UserReadPosition position = new UserReadPosition();
        position.setUserId(userId);
        position.setGroupId(groupId);
        position.setLastReadMessageId(messageId);
        
        messageMapper.updateReadPosition(position);
        
        log.debug("用户{}标记群组{}消息{}为已读", userId, groupId, messageId);
    }
    
    /**
     * 获取未读消息数
     */
    public int getUnreadCount(Long groupId, Long userId) {
        return messageMapper.getUnreadCount(userId, groupId);
    }
}

