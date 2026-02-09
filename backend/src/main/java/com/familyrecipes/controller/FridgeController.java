package com.familyrecipes.controller;

import com.familyrecipes.common.Result;
import com.familyrecipes.entity.FridgeItem;
import com.familyrecipes.entity.Ingredient;
import com.familyrecipes.entity.ReminderSetting;
import com.familyrecipes.service.FridgeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 冰箱管理控制器
 */
@RestController
@RequestMapping("/api/fridge")
public class FridgeController {

    @Autowired
    private FridgeService fridgeService;

    /**
     * 添加食材到冰箱
     */
    @PostMapping("/items")
    public Result<FridgeItem> addItem(@RequestBody FridgeItem item, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            item.setUserId(userId);
            FridgeItem created = fridgeService.addItem(item);
            return Result.success(created);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新冰箱食材
     */
    @PutMapping("/items/{id}")
    public Result<FridgeItem> updateItem(@PathVariable Long id,
                                         @RequestBody FridgeItem item,
                                         HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            item.setId(id);
            item.setUserId(userId);
            FridgeItem updated = fridgeService.updateItem(item);
            return Result.success(updated);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 标记为已消耗
     */
    @PostMapping("/items/{id}/consume")
    public Result<Void> markAsConsumed(@PathVariable Long id) {
        try {
            fridgeService.markAsConsumed(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除食材
     */
    @DeleteMapping("/items/{id}")
    public Result<Void> deleteItem(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            fridgeService.deleteItem(id, userId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取冰箱食材列表
     */
    @GetMapping("/items")
    public Result<List<FridgeItem>> getFridgeItems(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<FridgeItem> items = fridgeService.getUserFridgeItems(userId);
        return Result.success(items);
    }

    /**
     * 获取即将过期的食材
     */
    @GetMapping("/items/expiring")
    public Result<List<FridgeItem>> getExpiringItems(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<FridgeItem> items = fridgeService.getExpiringItems(userId);
        return Result.success(items);
    }

    /**
     * 获取提醒设置
     */
    @GetMapping("/reminder-settings")
    public Result<ReminderSetting> getReminderSetting(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        ReminderSetting setting = fridgeService.getReminderSetting(userId);
        return Result.success(setting);
    }

    /**
     * 更新提醒设置
     */
    @PutMapping("/reminder-settings")
    public Result<Void> updateReminderSetting(@RequestBody ReminderSetting setting,
                                               HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            setting.setUserId(userId);
            fridgeService.updateReminderSetting(setting);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 搜索食材
     */
    @GetMapping("/ingredients/search")
    public Result<List<Ingredient>> searchIngredients(@RequestParam String keyword) {
        List<Ingredient> ingredients = fridgeService.searchIngredients(keyword);
        return Result.success(ingredients);
    }

    /**
     * 创建食材
     */
    @PostMapping("/ingredients")
    public Result<Ingredient> createIngredient(@RequestBody Ingredient ingredient) {
        try {
            Ingredient created = fridgeService.getOrCreateIngredient(
                ingredient.getName(),
                ingredient.getCategory()
            );
            return Result.success(created);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

