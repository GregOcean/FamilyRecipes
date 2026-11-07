package com.familyrecipes.service;

import com.familyrecipes.common.ApiException;
import com.familyrecipes.mapper.GroupChatMapper;
import com.familyrecipes.mapper.RecipeMapper;
import com.familyrecipes.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatShareService {
    @Autowired RecipeAccessService access;
    @Autowired RecipeMapper recipes;
    @Autowired GroupChatMapper groups;
    @Autowired MessageService messages;
    @Autowired JdbcTemplate db;

    @Transactional
    public Message shareRecipe(Long id, Long group, Long user, String clientId) {
        if (group == null || groups.lockById(group) == null || !groups.isGroupMember(group, user))
            throw new ApiException(404, "群组不存在或不可访问");
        access.requireRead(id, user);
        var recipe = recipes.findById(id);
        String link = "familyrecipes://recipe/" + id;
        var previous = messages.findSharedMessage(user, clientId, group, link);
        if (previous != null) return previous;
        db.update("INSERT IGNORE INTO recipe_chat_share(recipe_id,group_id,shared_by) VALUES(?,?,?)", id, group, user);
        return messages.sendMessage(group, user, "菜谱：" + recipe.getName() + "\n" + link,
            Message.MessageType.text, clientId);
    }
}
