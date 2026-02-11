package com.familyrecipes.service;

import com.familyrecipes.entity.FridgeItem;
import com.familyrecipes.entity.Ingredient;
import com.familyrecipes.entity.ReminderSetting;
import com.familyrecipes.mapper.FridgeItemMapper;
import com.familyrecipes.mapper.IngredientMapper;
import com.familyrecipes.mapper.ReminderSettingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(FridgeService.class);

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
        log.info("========== 开始添加食材到冰箱 ==========");
        log.info("用户ID: {}", item.getUserId());
        log.info("食材ID: {}", item.getIngredientId());
        log.info("数量: {}", item.getAmount());
        log.info("过期日期: {}", item.getExpiryDate());
        
        // 确保食材存在
        Ingredient ingredient = ingredientMapper.findById(item.getIngredientId());
        
        if (ingredient == null) {
            log.error("❌ 食材不存在！食材ID: {}", item.getIngredientId());
            log.error("请检查：1. 食材是否已创建 2. ID是否正确传递");
            throw new RuntimeException("食材不存在");
        }
        
        log.info("✅ 找到食材: {} (ID: {}, 分类: {}, 单位: {})", 
            ingredient.getName(), 
            ingredient.getId(), 
            ingredient.getCategory(),
            ingredient.getUnit());

        item.setStatus(FridgeItem.STATUS_NORMAL);
        fridgeItemMapper.insert(item);
        
        log.info("✅ 食材项已插入，ID: {}", item.getId());
        
        // 更新过期状态
        updateExpiryStatus(item.getUserId());
        
        FridgeItem result = fridgeItemMapper.findById(item.getId());
        log.info("========== 添加食材完成 ==========");
        
        return result;
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
     * 获取用户已消耗的食材列表
     */
    public List<FridgeItem> getConsumedItems(Long userId) {
        return fridgeItemMapper.findConsumedByUserId(userId);
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
        log.info("--- 开始获取或创建食材: {} ---", name);
        
        Ingredient ingredient = ingredientMapper.findByName(name);
        
        if (ingredient == null) {
            log.info("食材不存在，创建新食材...");
            ingredient = new Ingredient();
            ingredient.setName(name);
            ingredient.setCategory(category);
            ingredientMapper.insert(ingredient);
            log.info("✅ 食材已创建，ID: {}, 名称: {}, 分类: {}", 
                ingredient.getId(), 
                ingredient.getName(), 
                ingredient.getCategory());
        } else {
            log.info("✅ 食材已存在，ID: {}, 名称: {}", ingredient.getId(), ingredient.getName());
        }
        
        return ingredient;
    }
}

