package com.familyrecipes.service;
import com.familyrecipes.common.ApiException;
import com.familyrecipes.entity.Kitchen;
import com.familyrecipes.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
public class KitchenService {
    @Autowired private KitchenMapper kitchens;
    @Autowired private GroupChatMapper groups;
    @Autowired private UserMapper users;
    @Transactional
    public List<Kitchen> list(Long userId) {
        // Serialize first-use initialization across concurrent requests for the same account.
        if (users.lockId(userId) == null) throw new ApiException(404,"用户不存在");
        List<Kitchen> existing = kitchens.list(userId);
        if (existing.isEmpty()) createPersonalKitchen("我的厨房", userId);
        return kitchens.list(userId);
    }
    private Kitchen createPersonalKitchen(String name, Long userId) {
        var group = new com.familyrecipes.model.GroupChat();
        group.setName("我的家庭"); group.setCreatorId(userId); group.setManagerId(userId);
        group.setMemberCount(1); group.setMaxMembers(10);
        groups.insertGroup(group);
        var member = new com.familyrecipes.model.GroupMember();
        member.setGroupId(group.getId()); member.setUserId(userId);
        member.setRole(com.familyrecipes.model.GroupMember.MemberRole.manager);
        groups.insertGroupMember(member);
        Kitchen kitchen = new Kitchen(); kitchen.setGroupId(group.getId()); kitchen.setName(name.trim());
        kitchens.insert(kitchen);
        return kitchen;
    }
    public Kitchen requireMember(Long id, Long userId) {
        Kitchen k = id == null ? null : kitchens.find(id);
        if (k == null || !groups.isGroupMember(k.getGroupId(),userId))
            throw new ApiException(404,"请先选择可访问的厨房");
        return k;
    }
    private void validate(String name) {
        if(name == null || name.isBlank() || name.trim().length()>100)
            throw new ApiException(400,"厨房名称须为1至100字");
    }
    @Transactional
    public Kitchen create(Long groupId,String name,Long userId) {
        validate(name);
        // An explicit group is optional: a solo user can create their own kitchen directly.
        if (groupId == null) {
            if (users.lockId(userId) == null) throw new ApiException(404,"用户不存在");
            return createPersonalKitchen(name, userId);
        }
        var group=groups.lockById(groupId);
        if(group==null || !userId.equals(group.getManagerId()))
            throw new ApiException(403,"只有群管理员可以管理厨房");
        Kitchen k=new Kitchen();k.setGroupId(groupId);k.setName(name.trim());kitchens.insert(k);return k;
    }
    @Transactional
    public Kitchen rename(Long id,String name,Long userId) {
        validate(name);Kitchen k=requireMember(id,userId);
        var group=groups.lockById(k.getGroupId());
        if(!userId.equals(group.getManagerId())) throw new ApiException(403,"只有群管理员可以管理厨房");
        k.setName(name.trim());kitchens.rename(k);return k;
    }
    public void validateReservedFor(Kitchen k, Long userId) {
        if(userId!=null && !groups.isGroupMember(k.getGroupId(),userId))
            throw new ApiException(400,"预留对象须为本群成员");
    }
}
