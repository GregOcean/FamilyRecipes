package com.familyrecipes.service;

import com.familyrecipes.entity.User;
import com.familyrecipes.mapper.FriendshipMapper;
import com.familyrecipes.mapper.UserMapper;
import com.familyrecipes.model.Friendship;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 好友服务
 */
@Slf4j
@Service
public class FriendshipService {
    
    @Autowired
    private FriendshipMapper friendshipMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 添加好友（双向关系）
     */
    @Transactional
    public void addFriend(Long userId, Long friendId) {
        // 检查是否已经是好友
        if (friendshipMapper.isFriend(userId, friendId)) {
            throw new RuntimeException("已经是好友关系");
        }
        
        // 检查好友是否存在
        User friend = userMapper.findById(friendId);
        if (friend == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 创建双向好友关系
        Friendship friendship1 = new Friendship();
        friendship1.setUserId(userId);
        friendship1.setFriendId(friendId);
        friendship1.setStatus(Friendship.FriendshipStatus.accepted);
        friendshipMapper.insertFriendship(friendship1);
        
        Friendship friendship2 = new Friendship();
        friendship2.setUserId(friendId);
        friendship2.setFriendId(userId);
        friendship2.setStatus(Friendship.FriendshipStatus.accepted);
        friendshipMapper.insertFriendship(friendship2);
        
        log.info("用户{}成功添加好友{}", userId, friendId);
    }
    
    /**
     * 获取好友列表
     */
    public List<Friendship> getFriendsList(Long userId) {
        return friendshipMapper.getFriendsList(userId);
    }
    
    /**
     * 删除好友（双向删除）
     */
    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        friendshipMapper.deleteFriendship(userId, friendId);
        friendshipMapper.deleteFriendship(friendId, userId);
        log.info("用户{}删除好友{}", userId, friendId);
    }
    
    /**
     * 更新好友备注
     */
    public void updateNickname(Long userId, Long friendId, String nickname) {
        friendshipMapper.updateNickname(userId, friendId, nickname);
        log.info("用户{}更新好友{}备注为: {}", userId, friendId, nickname);
    }
    
    /**
     * 检查是否为好友
     */
    public boolean isFriend(Long userId, Long friendId) {
        return friendshipMapper.isFriend(userId, friendId);
    }
}

