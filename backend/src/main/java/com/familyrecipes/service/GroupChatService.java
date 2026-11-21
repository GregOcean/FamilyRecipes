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
    @Autowired private KitchenService kitchens;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate db;
    @Autowired private org.springframework.context.ApplicationEventPublisher events;

    private void validateInfo(String name, String description) {
        if(name==null || name.isBlank() || name.trim().length()>100)
            throw new com.familyrecipes.common.ApiException(400,"群名称须为1至100字符");
        if(description!=null && description.length()>500)
            throw new com.familyrecipes.common.ApiException(400,"群介绍最多500字符");
    }
    private void systemMessage(Long group,Long sender,String text) {
        Message message=new Message(); message.setGroupId(group); message.setSenderId(sender);
        message.setContent(text); message.setMessageType(Message.MessageType.system);
        messageMapper.insertMessage(message); events.publishEvent(messageMapper.findById(message.getId()));
    }
    private String memberName(Long user) {
        return db.queryForObject("SELECT username FROM user WHERE id=?",String.class,user);
    }
    @Transactional
    public void transferManager(Long groupId,Long operatorId,Long targetId) {
        lockGroup(groupId);
        GroupMember operator=groupChatMapper.getGroupMember(groupId,operatorId);
        if(operator==null || operator.getRole()!=GroupMember.MemberRole.manager)
            throw new com.familyrecipes.common.ApiException(403,"只有群管理员可以转让管理权");
        if(targetId==null || targetId.equals(operatorId) || !groupChatMapper.isGroupMember(groupId,targetId))
            throw new com.familyrecipes.common.ApiException(400,"请选择群内其他成员");
        groupChatMapper.updateMemberRole(groupId,operatorId,"member");
        groupChatMapper.updateMemberRole(groupId,targetId,"manager");
        db.update("UPDATE group_chat SET manager_id=? WHERE id=?",targetId,groupId);
        systemMessage(groupId,operatorId,"已将群管理员转让给 "+memberName(targetId));
    }

    private void lockGroup(Long groupId) {
        if (groupChatMapper.lockById(groupId) == null)
            throw new com.familyrecipes.common.ApiException(404, "群组不存在");
    }
    
    /**
     * 创建群组
     */
    @Transactional
    public GroupChat createGroup(Long creatorId, String groupName, String description, 
                                  Integer maxMembers, List<Long> memberIds) {
        validateInfo(groupName,description);
        groupName=groupName.trim();
        maxMembers = 10; // Single-instance personal edition: capacity is a server rule.
        memberIds = memberIds == null ? List.of() : memberIds.stream().filter(java.util.Objects::nonNull)
            .filter(id -> !id.equals(creatorId)).distinct().toList();
        for (Long id : memberIds) if (!friendshipService.isFriend(creatorId, id))
            throw new com.familyrecipes.common.ApiException(400, "只能邀请好友入群");
        // 检查成员数量（包括创建者）
        int totalMembers = memberIds.size() + 1;
        if (totalMembers > maxMembers) {
            throw new com.familyrecipes.common.ApiException(400, "群组成员数量不能超过" + maxMembers + "人");
        }
        
        // 创建群组
        GroupChat group = new GroupChat();
        group.setName(groupName);
        group.setDescription(description);
        group.setCreatorId(creatorId);
        group.setManagerId(creatorId);  // 创建者即为管理员
        group.setMemberCount(totalMembers); // 包括创建者
        group.setMaxMembers(maxMembers);
        groupChatMapper.insertGroup(group);
        
        // 添加创建者为群管理员
        GroupMember manager = new GroupMember();
        manager.setGroupId(group.getId());
        manager.setUserId(creatorId);
        manager.setRole(GroupMember.MemberRole.manager);
        groupChatMapper.insertGroupMember(manager);
        
        // 添加其他成员
        for (Long memberId : memberIds) {
            GroupMember member = new GroupMember();
            member.setGroupId(group.getId());
            member.setUserId(memberId);
            member.setRole(GroupMember.MemberRole.member);
            groupChatMapper.insertGroupMember(member);
        }
        
        kitchens.create(group.getId(), KitchenService.defaultName("家里的厨房","Home kitchen"), creatorId);

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
            var position=messageMapper.getReadPosition(userId,group.getId());
            group.setLastReadMessageId(position==null?0L:position.getLastReadMessageId());
        }
        
        groups.sort(java.util.Comparator.comparing((GroupChat g) -> g.getLastMessage()==null ? 0L : g.getLastMessage().getId()).reversed());
        return groups;
    }
    
    /**
     * 获取群组详情
     */
    public GroupChat getGroupDetail(Long groupId, Long userId) {
        // 检查用户是否在群中
        if (!groupChatMapper.isGroupMember(groupId, userId)) {
            throw new com.familyrecipes.common.ApiException(400, "您不在该群组中");
        }
        
        GroupChat group = groupChatMapper.getGroupById(groupId);
        if (group == null) {
            throw new com.familyrecipes.common.ApiException(400, "群组不存在");
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
        lockGroup(groupId);
        // 所有群成员均可添加自己的已确认好友
        GroupMember operator = groupChatMapper.getGroupMember(groupId, operatorId);
        if (operator == null) {
            throw new com.familyrecipes.common.ApiException(400, "您不在该群组中");
        }
        
        userIds = userIds == null ? List.of() : userIds.stream().filter(java.util.Objects::nonNull)
            .distinct().filter(id -> !groupChatMapper.isGroupMember(groupId, id)).toList();
        for (Long id : userIds) if (!friendshipService.isFriend(operatorId, id))
            throw new com.familyrecipes.common.ApiException(400, "只能邀请好友入群");
        // 检查群成员数量
        int currentCount = groupChatMapper.getMemberCount(groupId);
        GroupChat group = groupChatMapper.getGroupById(groupId);
        if (currentCount + userIds.size() > group.getMaxMembers()) {
            throw new com.familyrecipes.common.ApiException(400, "群成员已达上限（" + group.getMaxMembers() + "人）");
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
            // Joining grants history access, but old history must not become new unread messages.
            db.update("INSERT INTO user_read_position(user_id,group_id,last_read_message_id) " +
                "SELECT ?,?,COALESCE(MAX(id),0) FROM message WHERE group_id=? " +
                "ON DUPLICATE KEY UPDATE last_read_message_id=VALUES(last_read_message_id)",userId,groupId,groupId);
        }
        if(!userIds.isEmpty()) systemMessage(groupId,operatorId,"加入群聊："+
            userIds.stream().map(this::memberName).collect(java.util.stream.Collectors.joining("、")));
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
        lockGroup(groupId);
        // 检查操作者权限（只有管理员可以移除）
        GroupMember operator = groupChatMapper.getGroupMember(groupId, operatorId);
        if (operator == null) {
            throw new com.familyrecipes.common.ApiException(400, "您不在该群组中");
        }
        if (operator.getRole() != GroupMember.MemberRole.manager) {
            throw new com.familyrecipes.common.ApiException(400, "只有群管理员可以移除成员");
        }
        
        // 不能移除管理员自己
        GroupMember target = groupChatMapper.getGroupMember(groupId, userId);
        if (target != null && target.getRole() == GroupMember.MemberRole.manager) {
            throw new com.familyrecipes.common.ApiException(400, "不能移除群管理员");
        }
        
        if(target==null) throw new com.familyrecipes.common.ApiException(404,"该成员已不在群中");
        // 移除成员
        groupChatMapper.deleteGroupMember(groupId, userId);
        
        // 更新群成员数量
        int newCount = groupChatMapper.getMemberCount(groupId);
        groupChatMapper.updateMemberCount(groupId, newCount);
        


        // 发送系统消息
        Message systemMessage = new Message();
        systemMessage.setGroupId(groupId);
        systemMessage.setSenderId(operatorId);
        systemMessage.setContent("已移除成员："+memberName(userId));
        systemMessage.setMessageType(Message.MessageType.system);
        messageMapper.insertMessage(systemMessage);
        events.publishEvent(messageMapper.findById(systemMessage.getId()));
        log.info("用户{}从群组{}移除了用户{}", operatorId, groupId, userId);
    }
    
    /**
     * 退出群组
     */
    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        lockGroup(groupId);
        GroupMember member = groupChatMapper.getGroupMember(groupId, userId);
        if (member == null) {
            throw new com.familyrecipes.common.ApiException(400, "您不在该群组中");
        }
        
        // 如果是群管理员，不能直接退出，需要先转让或解散群组
        if (member.getRole() == GroupMember.MemberRole.manager) {
            throw new com.familyrecipes.common.ApiException(400, "群管理员不能退出群组，请先转让管理员权限或解散群组");
        }
        
        systemMessage(groupId,userId,memberName(userId)+" 已退出群聊");
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
        lockGroup(groupId);
        // 检查操作者权限（只有管理员可以修改）
        GroupMember operator = groupChatMapper.getGroupMember(groupId, operatorId);
        if (operator == null) {
            throw new com.familyrecipes.common.ApiException(400, "您不在该群组中");
        }
        if (operator.getRole() != GroupMember.MemberRole.manager) {
            throw new com.familyrecipes.common.ApiException(400, "只有群管理员可以修改群组信息");
        }
        
        validateInfo(name,description);
        GroupChat group = groupChatMapper.getGroupById(groupId);
        group.setName(name.trim());
        group.setDescription(description);
        groupChatMapper.updateGroup(group);
        
        log.info("用户{}更新了群组{}信息", operatorId, groupId);
    }
    
    /**
     * 解散群组
     */
    @Transactional
    public void dismissGroup(Long groupId, Long operatorId) {
        lockGroup(groupId);
        // 检查操作者权限（只有管理员可以解散）
        GroupMember operator = groupChatMapper.getGroupMember(groupId, operatorId);
        if (operator == null || operator.getRole() != GroupMember.MemberRole.manager) {
            throw new com.familyrecipes.common.ApiException(400, "只有群管理员可以解散群组");
        }
        
        // 删除群组（会级联删除成员和消息）
        groupChatMapper.deleteGroup(groupId);
        
        log.info("用户{}解散了群组{}", operatorId, groupId);
    }
}

