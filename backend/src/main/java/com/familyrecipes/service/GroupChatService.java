package com.familyrecipes.service;

import com.familyrecipes.mapper.GroupChatMapper;
import com.familyrecipes.mapper.MessageMapper;
import com.familyrecipes.model.GroupChat;
import com.familyrecipes.model.GroupMember;
import com.familyrecipes.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 群组服务
 */
@Slf4j
@Service
public class GroupChatService {
    
    @Autowired
    private GroupChatMapper groupChatMapper;
    
    @Autowired
    private MessageMapper messageMapper;
    
    @Autowired
    private FriendshipService friendshipService;
    
    /**
     * 创建群组
     */
    @Transactional
    public GroupChat createGroup(Long creatorId, String groupName, List<Long> memberIds) {
        // 检查成员数量（包括创建者，最多5人）
        if (memberIds.size() + 1 > 5) {
            throw new RuntimeException("群组成员数量不能超过5人");
        }
        
        // 创建群组
        GroupChat group = new GroupChat();
        group.setName(groupName);
        group.setCreatorId(creatorId);
        group.setManagerId(creatorId);  // 创建者即为管理员
        group.setMemberCount(memberIds.size() + 1); // 包括创建者
        group.setMaxMembers(5);  // 默认5人，会员可扩展
        groupChatMapper.insertGroup(group);
        
        // 添加创建者为群管理员
        GroupMember manager = new GroupMember();
        manager.setGroupId(group.getId());
        manager.setUserId(creatorId);
        manager.setRole(GroupMember.MemberRole.manager);
        groupChatMapper.insertGroupMember(manager);
        
        // 添加其他成员
        for (Long memberId : memberIds) {
            // 检查是否为好友
            if (!friendshipService.isFriend(creatorId, memberId)) {
                log.warn("用户{}不是创建者{}的好友，跳过", memberId, creatorId);
                continue;
            }
            
            GroupMember member = new GroupMember();
            member.setGroupId(group.getId());
            member.setUserId(memberId);
            member.setRole(GroupMember.MemberRole.member);
            groupChatMapper.insertGroupMember(member);
        }
        
        // 发送系统消息
        Message systemMessage = new Message();
        systemMessage.setGroupId(group.getId());
        systemMessage.setSenderId(creatorId);
        systemMessage.setContent("创建了群聊");
        systemMessage.setMessageType(Message.MessageType.system);
        messageMapper.insertMessage(systemMessage);
        
        log.info("用户{}创建群组: {} (ID: {})", creatorId, groupName, group.getId());
        return group;
    }
    
    /**
     * 获取用户的群组列表
     */
    public List<GroupChat> getUserGroups(Long userId) {
        List<GroupChat> groups = groupChatMapper.getUserGroups(userId);
        
        // 为每个群组填充最后一条消息和未读数
        for (GroupChat group : groups) {
            Message lastMessage = messageMapper.getLastMessage(group.getId());
            group.setLastMessage(lastMessage);
            
            int unreadCount = messageMapper.getUnreadCount(userId, group.getId());
            group.setUnreadCount(unreadCount);
        }
        
        return groups;
    }
    
    /**
     * 获取群组详情
     */
    public GroupChat getGroupDetail(Long groupId, Long userId) {
        // 检查用户是否在群中
        if (!groupChatMapper.isGroupMember(groupId, userId)) {
            throw new RuntimeException("您不在该群组中");
        }
        
        GroupChat group = groupChatMapper.getGroupById(groupId);
        if (group == null) {
            throw new RuntimeException("群组不存在");
        }
        
        // 获取群成员列表
        List<GroupMember> members = groupChatMapper.getGroupMembers(groupId);
        group.setMembers(members);
        
        return group;
    }
    
    /**
     * 添加群成员
     */
    @Transactional
    public void addGroupMember(Long groupId, Long operatorId, List<Long> userIds) {
        // 检查操作者权限（只有管理员可以添加）
        GroupMember operator = groupChatMapper.getGroupMember(groupId, operatorId);
        if (operator == null) {
            throw new RuntimeException("您不在该群组中");
        }
        if (operator.getRole() != GroupMember.MemberRole.manager) {
            throw new RuntimeException("只有群管理员可以添加成员");
        }
        
        // 检查群成员数量
        int currentCount = groupChatMapper.getMemberCount(groupId);
        GroupChat group = groupChatMapper.getGroupById(groupId);
        if (currentCount + userIds.size() > group.getMaxMembers()) {
            throw new RuntimeException("群成员已达上限（" + group.getMaxMembers() + "人）");
        }
        
        // 添加成员
        for (Long userId : userIds) {
            // 检查是否已在群中
            if (groupChatMapper.isGroupMember(groupId, userId)) {
                continue;
            }
            
            // 检查是否为好友
            if (!friendshipService.isFriend(operatorId, userId)) {
                log.warn("用户{}不是操作者{}的好友，跳过", userId, operatorId);
                continue;
            }
            
            GroupMember member = new GroupMember();
            member.setGroupId(groupId);
            member.setUserId(userId);
            member.setRole(GroupMember.MemberRole.member);
            groupChatMapper.insertGroupMember(member);
        }
        
        // 更新群成员数量
        int newCount = groupChatMapper.getMemberCount(groupId);
        groupChatMapper.updateMemberCount(groupId, newCount);
        
        log.info("用户{}向群组{}添加了{}个成员", operatorId, groupId, userIds.size());
    }
    
    /**
     * 移除群成员
     */
    @Transactional
    public void removeGroupMember(Long groupId, Long operatorId, Long userId) {
        // 检查操作者权限（只有管理员可以移除）
        GroupMember operator = groupChatMapper.getGroupMember(groupId, operatorId);
        if (operator == null) {
            throw new RuntimeException("您不在该群组中");
        }
        if (operator.getRole() != GroupMember.MemberRole.manager) {
            throw new RuntimeException("只有群管理员可以移除成员");
        }
        
        // 不能移除管理员自己
        GroupMember target = groupChatMapper.getGroupMember(groupId, userId);
        if (target != null && target.getRole() == GroupMember.MemberRole.manager) {
            throw new RuntimeException("不能移除群管理员");
        }
        
        // 移除成员
        groupChatMapper.deleteGroupMember(groupId, userId);
        
        // 更新群成员数量
        int newCount = groupChatMapper.getMemberCount(groupId);
        groupChatMapper.updateMemberCount(groupId, newCount);
        
        // 发送系统消息
        Message systemMessage = new Message();
        systemMessage.setGroupId(groupId);
        systemMessage.setSenderId(operatorId);
        systemMessage.setContent("移除了一位成员");
        systemMessage.setMessageType(Message.MessageType.system);
        messageMapper.insertMessage(systemMessage);
        
        log.info("用户{}从群组{}移除了用户{}", operatorId, groupId, userId);
    }
    
    /**
     * 退出群组
     */
    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        GroupMember member = groupChatMapper.getGroupMember(groupId, userId);
        if (member == null) {
            throw new RuntimeException("您不在该群组中");
        }
        
        // 如果是群管理员，不能直接退出，需要先转让或解散群组
        if (member.getRole() == GroupMember.MemberRole.manager) {
            throw new RuntimeException("群管理员不能退出群组，请先转让管理员权限或解散群组");
        }
        
        // 退出群组
        groupChatMapper.deleteGroupMember(groupId, userId);
        
        // 更新群成员数量
        int newCount = groupChatMapper.getMemberCount(groupId);
        groupChatMapper.updateMemberCount(groupId, newCount);
        
        log.info("用户{}退出群组{}", userId, groupId);
    }
    
    /**
     * 更新群组信息
     */
    @Transactional
    public void updateGroup(Long groupId, Long operatorId, String name, String description) {
        // 检查操作者权限（只有管理员可以修改）
        GroupMember operator = groupChatMapper.getGroupMember(groupId, operatorId);
        if (operator == null) {
            throw new RuntimeException("您不在该群组中");
        }
        if (operator.getRole() != GroupMember.MemberRole.manager) {
            throw new RuntimeException("只有群管理员可以修改群组信息");
        }
        
        GroupChat group = new GroupChat();
        group.setId(groupId);
        group.setName(name);
        group.setDescription(description);
        groupChatMapper.updateGroup(group);
        
        log.info("用户{}更新了群组{}信息", operatorId, groupId);
    }
    
    /**
     * 解散群组
     */
    @Transactional
    public void dismissGroup(Long groupId, Long operatorId) {
        // 检查操作者权限（只有管理员可以解散）
        GroupMember operator = groupChatMapper.getGroupMember(groupId, operatorId);
        if (operator == null || operator.getRole() != GroupMember.MemberRole.manager) {
            throw new RuntimeException("只有群管理员可以解散群组");
        }
        
        // 删除群组（会级联删除成员和消息）
        groupChatMapper.deleteGroup(groupId);
        
        log.info("用户{}解散了群组{}", operatorId, groupId);
    }
}

