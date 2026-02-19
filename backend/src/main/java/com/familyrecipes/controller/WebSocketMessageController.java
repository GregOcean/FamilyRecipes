package com.familyrecipes.controller;

import com.familyrecipes.common.Result;
import com.familyrecipes.model.Message;
import com.familyrecipes.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * WebSocket消息控制器 (STOMP)
 * 已弃用 - 使用 ChatWebSocketHandler 代替
 * 如果需要为Web端启用STOMP，取消注释 @Controller
 */
@Slf4j
// @Controller
public class WebSocketMessageController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageService messageService;

    /**
     * 处理客户端发送的消息
     * 客户端发送到: /app/chat.sendMessage
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Map<String, Object> payload) {
        try {
            Long groupId = Long.valueOf(payload.get("groupId").toString());
            Long senderId = Long.valueOf(payload.get("senderId").toString());
            String content = payload.get("content").toString();
            String messageTypeStr = payload.getOrDefault("messageType", "text").toString();
            
            // 将字符串转换为枚举类型
            Message.MessageType messageType = Message.MessageType.valueOf(messageTypeStr);

            log.info("收到WebSocket消息: groupId={}, senderId={}, content={}", groupId, senderId, content);

            // 保存消息到数据库
            Message message = messageService.sendMessage(groupId, senderId, content, messageType);

            // 广播消息到群组所有成员
            // 订阅地址: /topic/group/{groupId}
            messagingTemplate.convertAndSend("/topic/group/" + groupId, message);

            log.info("消息已广播到群组: {}", groupId);
        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }

    /**
     * 用户加入群组
     * 客户端发送到: /app/chat.joinGroup
     */
    @MessageMapping("/chat.joinGroup")
    public void joinGroup(@Payload Map<String, Object> payload) {
        try {
            Long groupId = Long.valueOf(payload.get("groupId").toString());
            Long userId = Long.valueOf(payload.get("userId").toString());
            String username = payload.getOrDefault("username", "用户").toString();

            log.info("用户加入群组: userId={}, username={}, groupId={}", userId, username, groupId);

            // 发送系统消息通知
            Message systemMessage = new Message();
            systemMessage.setGroupId(groupId);
            systemMessage.setSenderId(userId);
            systemMessage.setContent(username + " 加入了群聊");
            systemMessage.setMessageType(Message.MessageType.system);

            messagingTemplate.convertAndSend("/topic/group/" + groupId, systemMessage);
        } catch (Exception e) {
            log.error("加入群组失败", e);
        }
    }

    /**
     * 用户离开群组
     * 客户端发送到: /app/chat.leaveGroup
     */
    @MessageMapping("/chat.leaveGroup")
    public void leaveGroup(@Payload Map<String, Object> payload) {
        try {
            Long groupId = Long.valueOf(payload.get("groupId").toString());
            Long userId = Long.valueOf(payload.get("userId").toString());
            String username = payload.getOrDefault("username", "用户").toString();

            log.info("用户离开群组: userId={}, username={}, groupId={}", userId, username, groupId);

            // 发送系统消息通知
            Message systemMessage = new Message();
            systemMessage.setGroupId(groupId);
            systemMessage.setSenderId(userId);
            systemMessage.setContent(username + " 离开了群聊");
            systemMessage.setMessageType(Message.MessageType.system);

            messagingTemplate.convertAndSend("/topic/group/" + groupId, systemMessage);
        } catch (Exception e) {
            log.error("离开群组失败", e);
        }
    }
}

