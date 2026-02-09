package com.familyrecipes.service;

import com.familyrecipes.entity.FridgeItem;
import com.familyrecipes.entity.Ingredient;
import com.familyrecipes.entity.ReminderSetting;
import com.familyrecipes.mapper.FridgeItemMapper;
import com.familyrecipes.mapper.IngredientMapper;
import com.familyrecipes.mapper.ReminderSettingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 冰箱管理服务
 */
@Service
public class FridgeService {

    @Autowired
    private FridgeItemMapper fridgeItemMapper;

    @Autowired
    private IngredientMapper ingredientMapper;

    @Autowired
    private ReminderSettingMapper reminderSettingMapper;

    /**
     * 添加食材到冰箱
     */
    @Transactional
    public FridgeItem addItem(FridgeItem item) {
        // 确保食材存在
        Ingredient ingredient = ingredientMapper.findById(item.getIngredientId());
        if (ingredient == null) {
            throw new RuntimeException("食材不存在");
        }

        item.setStatus(FridgeItem.STATUS_NORMAL);
        fridgeItemMapper.insert(item);
        
        // 更新过期状态
        updateExpiryStatus(item.getUserId());
        
        return fridgeItemMapper.findById(item.getId());
    }

    /**
     * 更新冰箱食材
     */
    @Transactional
    public FridgeItem updateItem(FridgeItem item) {
        fridgeItemMapper.update(item);
        updateExpiryStatus(item.getUserId());
        return fridgeItemMapper.findById(item.getId());
    }

    /**
     * 标记为已消耗
     */
    public void markAsConsumed(Long itemId) {
        fridgeItemMapper.markAsConsumed(itemId);
    }

    /**
     * 删除食材
     */
    public void deleteItem(Long itemId, Long userId) {
        FridgeItem item = fridgeItemMapper.findById(itemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除");
        }
        fridgeItemMapper.delete(itemId);
    }

    /**
     * 获取用户的冰箱食材列表
     */
    public List<FridgeItem> getUserFridgeItems(Long userId) {
        updateExpiryStatus(userId);
        return fridgeItemMapper.findByUserId(userId);
    }

    /**
     * 获取即将过期的食材
     */
    public List<FridgeItem> getExpiringItems(Long userId) {
        ReminderSetting setting = reminderSettingMapper.findByUserId(userId);
        int daysBeforeExpiry = setting != null ? setting.getDaysBeforeExpiry() : 3;
        
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(daysBeforeExpiry);
        
        return fridgeItemMapper.findExpiring(userId, today, endDate);
    }

    /**
     * 更新过期状态
     */
    private void updateExpiryStatus(Long userId) {
        ReminderSetting setting = reminderSettingMapper.findByUserId(userId);
        int daysBeforeExpiry = setting != null ? setting.getDaysBeforeExpiry() : 3;
        fridgeItemMapper.updateExpiryStatus(userId, daysBeforeExpiry);
    }

    /**
     * 获取/更新提醒设置
     */
    public ReminderSetting getReminderSetting(Long userId) {
        return reminderSettingMapper.findByUserId(userId);
    }

    public void updateReminderSetting(ReminderSetting setting) {
        reminderSettingMapper.update(setting);
    }

    /**
     * 搜索食材
     */
    public List<Ingredient> searchIngredients(String keyword) {
        return ingredientMapper.searchByName(keyword);
    }

    /**
     * 创建食材（如果不存在）
     */
    @Transactional
    public Ingredient getOrCreateIngredient(String name, String category) {
        Ingredient ingredient = ingredientMapper.findByName(name);
        if (ingredient == null) {
            ingredient = new Ingredient();
            ingredient.setName(name);
            ingredient.setCategory(category);
            ingredientMapper.insert(ingredient);
        }
        return ingredient;
    }
}

