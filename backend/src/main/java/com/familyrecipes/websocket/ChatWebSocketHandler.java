package com.familyrecipes.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.familyrecipes.model.Message;
import com.familyrecipes.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket处理器 - 处理原始WebSocket连接
 */
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private MessageService messageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 群组ID -> WebSocket会话集合
    private final Map<Long, CopyOnWriteArraySet<WebSocketSession>> groupSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket连接建立: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("收到WebSocket消息: {}", payload);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String command = (String) data.get("command");

            if ("SUBSCRIBE".equals(command)) {
                handleSubscribe(session, data);
            } else if ("SEND".equals(command)) {
                handleSendMessage(session, data);
            } else if ("UNSUBSCRIBE".equals(command)) {
                handleUnsubscribe(session, data);
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
            sendError(session, "处理消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理订阅群组
     */
    private void handleSubscribe(WebSocketSession session, Map<String, Object> data) {
        Object destinationObj = data.get("destination");
        if (destinationObj != null) {
            String destination = destinationObj.toString();
            // 提取群组ID: /topic/group/1 -> 1
            String[] parts = destination.split("/");
            if (parts.length >= 3) {
                try {
                    Long groupId = Long.parseLong(parts[parts.length - 1]);
                    
                    // 将会话添加到群组
                    groupSessions.computeIfAbsent(groupId, k -> new CopyOnWriteArraySet<>()).add(session);
                    
                    // 在会话属性中保存群组ID
                    session.getAttributes().put("groupId", groupId);
                    
                    log.info("会话 {} 订阅了群组 {}", session.getId(), groupId);
                    
                    // 发送确认消息
                    sendAck(session, "订阅成功: " + destination);
                } catch (NumberFormatException e) {
                    log.error("无效的群组ID", e);
                    sendError(session, "无效的群组ID");
                }
            }
        }
    }

    /**
     * 处理取消订阅
     */
    private void handleUnsubscribe(WebSocketSession session, Map<String, Object> data) {
        Long groupId = (Long) session.getAttributes().get("groupId");
        if (groupId != null) {
            CopyOnWriteArraySet<WebSocketSession> sessions = groupSessions.get(groupId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    groupSessions.remove(groupId);
                }
            }
            session.getAttributes().remove("groupId");
            log.info("会话 {} 取消订阅群组 {}", session.getId(), groupId);
        }
    }

    /**
     * 处理发送消息
     */
    private void handleSendMessage(WebSocketSession session, Map<String, Object> data) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) data.get("body");
        
        if (body != null) {
            Long groupId = getLongValue(body.get("groupId"));
            Long senderId = getLongValue(body.get("senderId"));
            String content = (String) body.get("content");
            String messageType = (String) body.getOrDefault("messageType", "text");

            // 保存消息到数据库
            Message savedMessage = messageService.sendMessage(
                groupId,
                senderId,
                content,
                Message.MessageType.valueOf(messageType)
            );

            // 广播消息到群组所有成员
            broadcastToGroup(groupId, savedMessage);
            
            log.info("消息已发送并广播到群组 {}", groupId);
        }
    }

    /**
     * 广播消息到群组所有成员
     */
    private void broadcastToGroup(Long groupId, Message message) {
        CopyOnWriteArraySet<WebSocketSession> sessions = groupSessions.get(groupId);
        if (sessions != null) {
            String messageJson;
            try {
                // 构造消息格式
                Map<String, Object> messageData = Map.of(
                    "command", "MESSAGE",
                    "destination", "/topic/group/" + groupId,
                    "body", message
                );
                messageJson = objectMapper.writeValueAsString(messageData);
                
                for (WebSocketSession session : sessions) {
                    if (session.isOpen()) {
                        try {
                            session.sendMessage(new TextMessage(messageJson));
                        } catch (IOException e) {
                            log.error("发送消息到会话 {} 失败", session.getId(), e);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("序列化消息失败", e);
            }
        }
    }

    /**
     * 发送确认消息
     */
    private void sendAck(WebSocketSession session, String message) {
        try {
            Map<String, Object> ack = Map.of(
                "command", "ACK",
                "message", message
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ack)));
        } catch (Exception e) {
            log.error("发送ACK失败", e);
        }
    }

    /**
     * 发送错误消息
     */
    private void sendError(WebSocketSession session, String error) {
        try {
            Map<String, Object> errorMsg = Map.of(
                "command", "ERROR",
                "message", error
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorMsg)));
        } catch (Exception e) {
            log.error("发送错误消息失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket连接关闭: {}, status: {}", session.getId(), status);
        
        // 从所有群组中移除该会话
        Long groupId = (Long) session.getAttributes().get("groupId");
        if (groupId != null) {
            handleUnsubscribe(session, null);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: {}", session.getId(), exception);
    }

    /**
     * 获取Long值 (处理Integer和Long类型)
     */
    private Long getLongValue(Object value) {
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        return null;
    }
}

