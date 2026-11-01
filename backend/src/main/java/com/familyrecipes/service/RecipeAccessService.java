package com.familyrecipes.service;

import com.familyrecipes.common.ApiException;
import com.familyrecipes.entity.Recipe;
import com.familyrecipes.mapper.RecipeMapper;
import com.familyrecipes.mapper.GroupChatMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class RecipeAccessService {
    @Autowired private RecipeMapper recipes;
    @Autowired private GroupChatMapper groups;
    @Autowired private ObjectMapper json;
    @Autowired private KitchenService kitchens;

    public void requireRead(Long id, Long userId) {
        if (!recipes.isVisible(id, userId)) throw new ApiException(404, "菜谱不存在");
    }
    public Recipe requireEdit(Long id, Long userId) {
        Recipe recipe = recipes.lockById(id);
        if (recipe == null || userId == null || !userId.equals(recipe.getCreatorId()))
            throw new ApiException(404, "菜谱不存在或无权修改");
        return recipe;
    }
    public void validateSharing(Recipe recipe) {
        if(recipe.getKitchenId()!=null) recipe.setOwnerGroupId(kitchens.requireMember(recipe.getKitchenId(),recipe.getCreatorId()).getGroupId());
        if (recipe.getName() == null || recipe.getName().isBlank() || recipe.getName().length() > 100)
            throw new ApiException(400, "菜名不能为空且最多100字");
        if (recipe.getVisibility() == null) recipe.setVisibility("private");
        if (!Set.of("public", "private", "group").contains(recipe.getVisibility()))
            throw new ApiException(400, "可见性不合法");
        try {
            List<Long> ids = recipe.getSharedGroupIds() == null ? List.of() :
                json.readValue(recipe.getSharedGroupIds(), new TypeReference<List<Long>>() {});
            if (ids == null || ids.size() > 100) throw new ApiException(400, "共享群组不合法");
            Set<Long> all = new HashSet<>(ids);
            if (recipe.getOwnerGroupId() != null) all.add(recipe.getOwnerGroupId());
            for (Long id : all) {
                if (id == null || !groups.isGroupMember(id, recipe.getCreatorId()))
                    throw new ApiException(403, "不能分享至未加入的群组");
            }
            recipe.setSharedGroupIds(json.writeValueAsString(new LinkedHashSet<>(ids)));
        } catch (ApiException e) { throw e; }
        catch (Exception e) { throw new ApiException(400, "共享群组格式不合法"); }
    }
}
