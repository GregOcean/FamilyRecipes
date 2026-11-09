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
    @Autowired private org.springframework.jdbc.core.JdbcTemplate db;
    private void lockPair(Long a,Long b) {
        db.queryForList("SELECT id FROM user WHERE id IN (?,?) ORDER BY id FOR UPDATE",Math.min(a,b),Math.max(a,b));
    }
    public List<java.util.Map<String,Object>> search(Long user,String query) {
        String q=query==null?"":query.trim();
        if(q.isBlank() || q.length()>50) throw new com.familyrecipes.common.ApiException(400,"请输入完整用户名或用户ID");
        Long id=null;try{id=Long.valueOf(q);}catch(NumberFormatException ignored){}
        return db.queryForList("SELECT id,username,avatar FROM user WHERE id<>? AND (id=? OR username=?) LIMIT 20",user,id,q);
    }
    public List<java.util.Map<String,Object>> invitations(Long user) {
        return db.queryForList("SELECT f.id,f.user_id AS sender_id,f.friend_id AS recipient_id,u.username,u.avatar FROM friendship f JOIN user u ON u.id=CASE WHEN f.user_id=? THEN f.friend_id ELSE f.user_id END WHERE (f.friend_id=? OR f.user_id=?) AND f.status='pending' ORDER BY f.id DESC LIMIT 100",user,user,user);
    }
    @Transactional
    public void respond(Long user,Long sender,boolean accept) {
        lockPair(user,sender);
        int changed=db.update("DELETE FROM friendship WHERE user_id=? AND friend_id=? AND status='pending'",sender,user);
        if(changed!=1) throw new com.familyrecipes.common.ApiException(404,"邀请不存在或已处理");
        if(accept) {
            db.update("INSERT INTO friendship(user_id,friend_id,status) VALUES(?,?,'accepted'),(?,?,'accepted')",sender,user,user,sender);
        }
    }

    
    /**
     * 添加好友（双向关系）
     */
    @Transactional
    public void addFriend(Long userId, Long friendId) {
        if(userId.equals(friendId) || userMapper.findById(friendId)==null)
            throw new com.familyrecipes.common.ApiException(400,"请选择其他有效用户");
        lockPair(userId,friendId);
        if (friendshipMapper.getFriendship(userId,friendId)!=null || friendshipMapper.getFriendship(friendId,userId)!=null)
            throw new com.familyrecipes.common.ApiException(409,"已是好友或有待处理邀请");
        db.update("INSERT INTO friendship(user_id,friend_id,status) VALUES(?,?,'pending')",userId,friendId);

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

