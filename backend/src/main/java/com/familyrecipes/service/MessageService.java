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
    
    @Autowired private MediaService media;
    @Autowired private org.springframework.context.ApplicationEventPublisher events;

    /**
     * 发送消息
     */
    @org.springframework.transaction.annotation.Transactional
    public Message sendMessage(Long groupId, Long senderId, String content, Message.MessageType messageType) {
        return sendMessage(groupId,senderId,content,messageType,null);
    }
    @org.springframework.transaction.annotation.Transactional
    public Message sendMessage(Long groupId,Long senderId,String content,Message.MessageType messageType,String clientId) {
        if(groupChatMapper.lockById(groupId)==null) throw new com.familyrecipes.common.ApiException(404,"群组不存在");
        // 检查用户是否在群中
        if (!groupChatMapper.isGroupMember(groupId, senderId)) {
            throw new com.familyrecipes.common.ApiException(404, "群组不存在或不可访问");
        }
        
        if(clientId!=null) {
            if(!clientId.matches("[a-zA-Z0-9-]{16,64}")) throw new com.familyrecipes.common.ApiException(400,"消息编号不合法");
            Long existingId=messageMapper.findClient(senderId,clientId);
            if(existingId!=null) {
                Message previous=messageMapper.findById(existingId);
                if(!groupId.equals(previous.getGroupId()) || !java.util.Objects.equals(content,previous.getContent()) ||
                    previous.getMessageType()!=(messageType==null?Message.MessageType.text:messageType))
                    throw new com.familyrecipes.common.ApiException(409,"消息编号已用于其他内容");
                return previous;
            }
        }
        if (content == null || content.isBlank() || content.length() > 4000)
            throw new com.familyrecipes.common.ApiException(400, "消息长度须为1至4000字符");
        if (messageType != null && messageType != Message.MessageType.text && messageType != Message.MessageType.sticker)
            throw new com.familyrecipes.common.ApiException(400, "仅支持文字、链接和表情包");
        if (messageType == null || messageType == Message.MessageType.text) {
            var mentions = java.util.regex.Pattern.compile("@\\[[^\\]\\r\\n]{1,80}\\]\\(familyrecipes://user/([1-9][0-9]*)\\)").matcher(content);
            while (mentions.find()) {
                Long mentioned;
                try { mentioned = Long.valueOf(mentions.group(1)); }
                catch (NumberFormatException e) { throw new com.familyrecipes.common.ApiException(400, "提及成员不合法"); }
                if (!groupChatMapper.isGroupMember(groupId, mentioned))
                    throw new com.familyrecipes.common.ApiException(400, "被提及的成员已不在群中，请编辑后重发");
            }
        }
        if(messageType==Message.MessageType.sticker) media.requireSticker(content,senderId);
        Message message = new Message();
        message.setClientMessageId(clientId);
        message.setGroupId(groupId);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setMessageType(messageType != null ? messageType : Message.MessageType.text);
        
        messageMapper.insertMessage(message);
        
        log.info("用户{}在群组{}发送了消息", senderId, groupId);
        Message saved=messageMapper.findById(message.getId());
        events.publishEvent(saved);
        return saved;
    }
    
    /** Retry a share without duplicating messages, even if the source was later edited. */
    public Message findSharedMessage(Long user, String clientId, Long group, String link) {
        if (clientId == null) return null;
        Long id = messageMapper.findClient(user, clientId);
        if (id == null) return null;
        Message prior = messageMapper.findById(id);
        if (!group.equals(prior.getGroupId()) || !prior.getContent().endsWith("\n" + link))
            throw new com.familyrecipes.common.ApiException(409, "消息编号已用于其他内容");
        return prior;
    }

    public List<Message> catchUp(Long groupId,Long userId,Long afterId,int limit) {
        if(!groupChatMapper.isGroupMember(groupId,userId)) throw new com.familyrecipes.common.ApiException(404,"群组不存在或不可访问");
        if(afterId<0 || limit<1 || limit>200) throw new com.familyrecipes.common.ApiException(400,"游标参数不合法");
        return messageMapper.after(groupId,afterId,limit);
    }

    /**
     * 获取群组消息列表
     */
    public List<Message> getMessages(Long groupId, Long userId, int pageNum, int pageSize) {
        // 检查用户是否在群中
        if (!groupChatMapper.isGroupMember(groupId, userId)) {
            throw new com.familyrecipes.common.ApiException(404, "群组不存在或不可访问");
        }
        
        if (pageNum < 1 || pageNum > 1000000 || pageSize < 1 || pageSize > 100)
            throw new com.familyrecipes.common.ApiException(400, "分页参数不合法");
        int offset = (pageNum - 1) * pageSize;
        return messageMapper.getMessages(groupId, offset, pageSize);
    }
    
    /**
     * 标记消息已读
     */
    public void markAsRead(Long groupId, Long userId, Long messageId) {
        // 检查用户是否在群中
        if (!groupChatMapper.isGroupMember(groupId, userId)) {
            throw new com.familyrecipes.common.ApiException(404, "群组不存在或不可访问");
        }
        
        Message target = messageMapper.findById(messageId);
        if (target == null || !groupId.equals(target.getGroupId()))
            throw new com.familyrecipes.common.ApiException(400, "已读消息不属于该群组");
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
        if (!groupChatMapper.isGroupMember(groupId, userId))
            throw new com.familyrecipes.common.ApiException(404, "群组不存在或不可访问");
        return messageMapper.getUnreadCount(userId, groupId);
    }
}

