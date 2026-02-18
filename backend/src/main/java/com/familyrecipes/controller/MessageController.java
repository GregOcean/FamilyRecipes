package com.familyrecipes.controller;

import com.familyrecipes.common.Result;
import com.familyrecipes.model.Message;
import com.familyrecipes.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 消息API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/messages")
public class MessageController {
    
    @Autowired
    private MessageService messageService;
    
    /**
     * 发送消息
     */
    @PostMapping("/send")
    public Result<Message> sendMessage(@RequestBody SendMessageRequest request, 
                                      HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            
            if (request.getGroupId() == null) {
                return Result.error("群组ID不能为空");
            }
            
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                return Result.error("消息内容不能为空");
            }
            
            Message message = messageService.sendMessage(
                request.getGroupId(), 
                userId, 
                request.getContent(), 
                request.getMessageType()
            );
            return Result.success(message);
        } catch (Exception e) {
            log.error("发送消息失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取群组消息列表
     */
    @GetMapping("/list")
    public Result<List<Message>> getMessages(@RequestParam Long groupId,
                                             @RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "50") int pageSize,
                                             HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            List<Message> messages = messageService.getMessages(groupId, userId, pageNum, pageSize);
            return Result.success(messages);
        } catch (Exception e) {
            log.error("获取消息列表失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 标记消息已读
     */
    @PostMapping("/read")
    public Result<Void> markAsRead(@RequestBody Map<String, Long> request, 
                                   HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            Long groupId = request.get("groupId");
            Long messageId = request.get("messageId");
            
            if (groupId == null || messageId == null) {
                return Result.error("参数错误");
            }
            
            messageService.markAsRead(groupId, userId, messageId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("标记消息已读失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取未读消息数
     */
    @GetMapping("/unread")
    public Result<Integer> getUnreadCount(@RequestParam Long groupId, 
                                          HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            int count = messageService.getUnreadCount(groupId, userId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取未读消息数失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @Data
    static class SendMessageRequest {
        private Long groupId;
        private String content;
        private Message.MessageType messageType;
    }
}

