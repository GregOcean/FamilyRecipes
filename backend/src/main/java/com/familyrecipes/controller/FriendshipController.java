package com.familyrecipes.controller;

import com.familyrecipes.common.Result;
import com.familyrecipes.model.Friendship;
import com.familyrecipes.service.FriendshipService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 好友API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/friends")
public class FriendshipController {
    
    @Autowired
    private FriendshipService friendshipService;
    
    @GetMapping("/search")
    public Result<List<Map<String,Object>>> search(@RequestParam String query,HttpServletRequest r) {
        return Result.success(friendshipService.search((Long)r.getAttribute("userId"),query));
    }
    @GetMapping("/invitations")
    public Result<List<Map<String,Object>>> invitations(HttpServletRequest r) {
        return Result.success(friendshipService.invitations((Long)r.getAttribute("userId")));
    }
    @PostMapping("/invitations/{sender}/respond")
    public Result<Void> respond(@PathVariable Long sender,@RequestBody Map<String,Boolean> body,HttpServletRequest r) {
        if(!body.containsKey("accept") || body.get("accept")==null) throw new com.familyrecipes.common.ApiException(400,"请选择接受或拒绝");
        friendshipService.respond((Long)r.getAttribute("userId"),sender,body.get("accept"));return Result.success();
    }
    /**
     * 添加好友
     */
    @PostMapping("/add")
    public Result<Void> addFriend(@RequestBody Map<String, Long> request, HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            Long friendId = request.get("friendId");
            
            if (friendId == null) {
                return Result.error("好友ID不能为空");
            }
            
            if (userId.equals(friendId)) {
                return Result.error("不能添加自己为好友");
            }
            
            friendshipService.addFriend(userId, friendId);
            return Result.success(null);
        } catch (com.familyrecipes.common.ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("添加好友失败", e);
            throw new com.familyrecipes.common.ApiException(500, "请求处理失败", e);
        }
    }
    
    /**
     * 获取好友列表
     */
    @GetMapping("/list")
    public Result<List<Friendship>> getFriendsList(HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            List<Friendship> friends = friendshipService.getFriendsList(userId);
            return Result.success(friends);
        } catch (com.familyrecipes.common.ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取好友列表失败", e);
            throw new com.familyrecipes.common.ApiException(500, "请求处理失败", e);
        }
    }
    
    /**
     * 删除好友
     */
    @DeleteMapping("/{friendId}")
    public Result<Void> deleteFriend(@PathVariable Long friendId, HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            friendshipService.deleteFriend(userId, friendId);
            return Result.success(null);
        } catch (com.familyrecipes.common.ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除好友失败", e);
            throw new com.familyrecipes.common.ApiException(500, "请求处理失败", e);
        }
    }
    
    /**
     * 更新好友备注
     */
    @PutMapping("/{friendId}/nickname")
    public Result<Void> updateNickname(@PathVariable Long friendId, 
                                       @RequestBody Map<String, String> request,
                                       HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            String nickname = request.get("nickname");
            
            friendshipService.updateNickname(userId, friendId, nickname);
            return Result.success(null);
        } catch (com.familyrecipes.common.ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新好友备注失败", e);
            throw new com.familyrecipes.common.ApiException(500, "请求处理失败", e);
        }
    }
}

