package com.familyrecipes.controller;

import com.familyrecipes.common.Result;
import com.familyrecipes.model.GroupChat;
import com.familyrecipes.model.GroupMember;
import com.familyrecipes.service.GroupChatService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 群组API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/groups")
public class GroupChatController {
    
    @Autowired
    private GroupChatService groupChatService;
    
    /**
     * 创建群组
     */
    @PostMapping("/create")
    public Result<GroupChat> createGroup(@RequestBody CreateGroupRequest request, 
                                        HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return Result.error("群组名称不能为空");
            }
            
            // 支持不选择初始成员（可以创建后再邀请）
            List<Long> memberIds = request.getMemberIds();
            if (memberIds == null) {
                memberIds = new java.util.ArrayList<>();
            }
            
            // 设置默认值
            String description = request.getDescription() != null ? request.getDescription() : "";
            Integer maxMembers = request.getMaxMembers() != null ? request.getMaxMembers() : 20;
            
            GroupChat group = groupChatService.createGroup(userId, request.getName(), description, maxMembers, memberIds);
            return Result.success(group);
        } catch (Exception e) {
            log.error("创建群组失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取用户的群组列表
     */
    @GetMapping("/list")
    public Result<List<GroupChat>> getUserGroups(HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            List<GroupChat> groups = groupChatService.getUserGroups(userId);
            return Result.success(groups);
        } catch (Exception e) {
            log.error("获取群组列表失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取群组详情
     */
    @GetMapping("/{groupId}")
    public Result<GroupChat> getGroupDetail(@PathVariable Long groupId, 
                                           HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            GroupChat group = groupChatService.getGroupDetail(groupId, userId);
            return Result.success(group);
        } catch (Exception e) {
            log.error("获取群组详情失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 添加群成员
     */
    @PostMapping("/{groupId}/members")
    public Result<Void> addGroupMembers(@PathVariable Long groupId,
                                        @RequestBody Map<String, List<Long>> request,
                                        HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            List<Long> userIds = request.get("userIds");
            
            if (userIds == null || userIds.isEmpty()) {
                return Result.error("请选择要添加的成员");
            }
            
            groupChatService.addGroupMember(groupId, userId, userIds);
            return Result.success(null);
        } catch (Exception e) {
            log.error("添加群成员失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 移除群成员
     */
    @DeleteMapping("/{groupId}/members/{memberId}")
    public Result<Void> removeGroupMember(@PathVariable Long groupId,
                                          @PathVariable Long memberId,
                                          HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            groupChatService.removeGroupMember(groupId, userId, memberId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("移除群成员失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 退出群组
     */
    @PostMapping("/{groupId}/leave")
    public Result<Void> leaveGroup(@PathVariable Long groupId, 
                                   HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            groupChatService.leaveGroup(groupId, userId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("退出群组失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 更新群组信息
     */
    @PutMapping("/{groupId}")
    public Result<Void> updateGroup(@PathVariable Long groupId,
                                    @RequestBody UpdateGroupRequest request,
                                    HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            groupChatService.updateGroup(groupId, userId, request.getName(), request.getDescription());
            return Result.success(null);
        } catch (Exception e) {
            log.error("更新群组信息失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 解散群组
     */
    @DeleteMapping("/{groupId}")
    public Result<Void> dismissGroup(@PathVariable Long groupId, 
                                     HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            groupChatService.dismissGroup(groupId, userId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("解散群组失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @Data
    static class CreateGroupRequest {
        private String name;
        private String description;
        private Integer maxMembers;
        private List<Long> memberIds;
    }
    
    @Data
    static class UpdateGroupRequest {
        private String name;
        private String description;
    }
}

