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

    @Autowired private java.time.Clock clock;

    private static final Logger log = LoggerFactory.getLogger(FridgeService.class);

    @Autowired
    private FridgeItemMapper fridgeItemMapper;

    @Autowired
    private IngredientMapper ingredientMapper;

    @Autowired
    private InventoryCategoryService categoryService;
    @Autowired private MediaService media;
    @Autowired private KitchenService kitchens;

    @Autowired
    private ReminderSettingMapper reminderSettingMapper;

    private FridgeItem requireOwner(Long id, Long userId) {
        FridgeItem item = fridgeItemMapper.findById(id);
        if (item == null)
            throw new com.familyrecipes.common.ApiException(404, "食材不存在");
        kitchens.requireMember(item.getKitchenId(), userId);
        return item;
    }

    /**
     * 添加食材到冰箱
     */
    @Transactional
    public FridgeItem addItem(FridgeItem item) {
        validateCategoryOverride(item);
        kitchens.validateReservedFor(kitchens.requireMember(item.getKitchenId(), item.getUserId()), item.getOwnerUserId());
        if (item.getExpiryDate() == null) throw new com.familyrecipes.common.ApiException(400, "过期日期不能为空");
        media.requireOwned(item.getImageUrl(), item.getUserId());
        log.info("用户ID: {}", item.getUserId());
        log.info("食材ID: {}", item.getIngredientId());
        log.info("数量: {}", item.getAmount());
        log.info("过期日期: {}", item.getExpiryDate());
        
        // 确保食材存在
        Ingredient ingredient = ingredientMapper.findById(item.getIngredientId());
        
        if (ingredient == null) {
            log.error("❌ 食材不存在！食材ID: {}", item.getIngredientId());
            log.error("请检查：1. 食材是否已创建 2. ID是否正确传递");
            throw new com.familyrecipes.common.ApiException(400, "食材不存在");
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
        updateExpiryStatus(item.getKitchenId());
        
        FridgeItem result = fridgeItemMapper.findById(item.getId());
        log.info("========== 添加食材完成 ==========");
        
        return result;
    }

    /**
     * 更新冰箱食材
     */
    @Transactional
    public FridgeItem updateItem(FridgeItem item) {
        FridgeItem previous = requireOwner(item.getId(), item.getUserId());
        item.setKitchenId(previous.getKitchenId());
        validateCategoryOverride(item);
        kitchens.validateReservedFor(kitchens.requireMember(item.getKitchenId(), item.getUserId()), item.getOwnerUserId());
        if (!java.util.Objects.equals(previous.getImageUrl(), item.getImageUrl())) media.requireOwned(item.getImageUrl(), item.getUserId());
        if (item.getExpiryDate() == null) throw new com.familyrecipes.common.ApiException(400, "过期日期不能为空");
        if (fridgeItemMapper.update(item) != 1) throw new com.familyrecipes.common.ApiException(404, "食材不存在");
        updateExpiryStatus(item.getKitchenId());
        media.retireIfUnlinked(previous.getImageUrl());
        return fridgeItemMapper.findById(item.getId());
    }

    private void validateCategoryOverride(FridgeItem item) {
        if (item.getCategoryOverrideId() != null && categoryService.getCategoryById(item.getCategoryOverrideId()) == null)
            throw new com.familyrecipes.common.ApiException(400,"请选择有效分类");
    }

    /**
     * 标记为已消耗
     */
    public void markAsConsumed(Long itemId, Long userId) {
        FridgeItem item = requireOwner(itemId, userId);
        fridgeItemMapper.markAsConsumed(itemId, item.getKitchenId());
    }

    @Transactional public void changeStatus(Long id,Long user,String status) {
        if(!java.util.Set.of("normal","consumed","discarded","deleted").contains(status)) throw new com.familyrecipes.common.ApiException(400,"库存状态不合法");
        FridgeItem item=requireOwner(id,user);
        fridgeItemMapper.changeStatus(id,item.getKitchenId(),status);
        if(status.equals("normal")) updateExpiryStatus(item.getKitchenId());
    }
    /**
     * 删除食材
     */
    @Transactional
    public void deleteItem(Long itemId, Long userId) {
        FridgeItem item = requireOwner(itemId, userId);
        fridgeItemMapper.delete(itemId, item.getKitchenId());
        media.retireIfUnlinked(item.getImageUrl());
    }

    /**
     * 获取用户的冰箱食材列表
     */
    public List<FridgeItem> getUserFridgeItems(Long userId, Long kitchenId) {
        kitchens.requireMember(kitchenId, userId);
        updateExpiryStatus(kitchenId);
        return fridgeItemMapper.findByKitchenId(kitchenId);
    }
    
    /**
     * 获取用户已消耗的食材列表
     */
    public List<FridgeItem> getConsumedItems(Long userId, Long kitchenId) {
        kitchens.requireMember(kitchenId,userId);
        return fridgeItemMapper.findConsumedByKitchenId(kitchenId);
    }

    /**
     * 获取即将过期的食材
     */
    public List<FridgeItem> getExpiringItems(Long userId) {
        ReminderSetting setting = reminderSettingMapper.findByUserId(userId);
        int daysBeforeExpiry = setting != null ? setting.getDaysBeforeExpiry() : 3;
        
        LocalDate today = LocalDate.now(clock);
        LocalDate endDate = today.plusDays(daysBeforeExpiry);
        
        return fridgeItemMapper.findExpiring(userId, today, endDate);
    }

    /**
     * 更新过期状态
     */
    private void updateExpiryStatus(Long userId) {
        fridgeItemMapper.updateExpiryStatus(userId, 3, LocalDate.now(clock));
    }

    /**
     * 获取/更新提醒设置
     */
    public ReminderSetting getReminderSetting(Long userId) {
        ReminderSetting result=reminderSettingMapper.findByUserId(userId);
        if(result==null) { result=new ReminderSetting();result.setUserId(userId);result.setDaysBeforeExpiry(3);result.setReminderTime(java.time.LocalTime.of(17,0));result.setEnabled(true); }
        return result;
    }

    public void updateReminderSetting(ReminderSetting setting) {
        if (setting.getDaysBeforeExpiry() == null || setting.getDaysBeforeExpiry() < 0 || setting.getDaysBeforeExpiry() > 365)
            throw new com.familyrecipes.common.ApiException(400, "提醒天数必须在0到365之间");
        if (setting.getReminderTime() == null || setting.getEnabled() == null) throw new com.familyrecipes.common.ApiException(400,"请填写提醒时间和开关");
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
        return getOrCreateIngredient(name, category, null);
    }

    public Ingredient previewIngredient(String rawName) {
        String name = validateIngredientName(rawName);
        Ingredient existing = ingredientMapper.findByName(name);
        if (existing != null) return existing;
        Ingredient draft = new Ingredient(); draft.setName(name);
        draft.setCategoryId(categoryService.smartMatchCategory(name));
        draft.setCategory(categoryService.getCategoryById(draft.getCategoryId()).getName());
        return draft;
    }

    private String validateIngredientName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 50)
            throw new com.familyrecipes.common.ApiException(400,"食材名称须为1至50字");
        return name.trim();
    }

    @Transactional
    public Ingredient getOrCreateIngredient(String name, String category, Long categoryId) {
        name = validateIngredientName(name);
        log.info("--- 开始获取或创建食材: {} ---", name);
        
        Ingredient ingredient = ingredientMapper.findByName(name);
        
        if (ingredient == null) {
            log.info("食材不存在，创建新食材...");
            ingredient = new Ingredient();
            ingredient.setName(name);
            Long selected = categoryId == null ? categoryService.smartMatchCategory(name) : categoryId;
            var classification = categoryService.getCategoryById(selected);
            if (classification == null) throw new com.familyrecipes.common.ApiException(400,"请选择有效分类");
            ingredient.setCategory(classification.getName());
            ingredient.setCategoryId(selected);
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

