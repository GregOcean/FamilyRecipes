package com.familyrecipes.controller;

import com.familyrecipes.common.Result;
import com.familyrecipes.model.InventoryCategory;
import com.familyrecipes.service.InventoryCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库存分类控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/inventory/categories")
public class InventoryCategoryController {
    
    @Autowired
    private InventoryCategoryService categoryService;
    
    /**
     * 获取分类树（一级分类及其子分类）
     */
    @GetMapping("/tree")
    public Result<List<InventoryCategory>> getCategoryTree() {
        try {
            List<InventoryCategory> tree = categoryService.getCategoryTree();
            return Result.success(tree);
        } catch (Exception e) {
            log.error("获取分类树失败", e);
            return Result.error("获取分类树失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有分类（扁平列表）
     */
    @GetMapping("/all")
    public Result<List<InventoryCategory>> getAllCategories() {
        try {
            List<InventoryCategory> categories = categoryService.getAllCategories();
            return Result.success(categories);
        } catch (Exception e) {
            log.error("获取所有分类失败", e);
            return Result.error("获取所有分类失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取分类
     */
    @GetMapping("/{id}")
    public Result<InventoryCategory> getCategoryById(@PathVariable Long id) {
        try {
            InventoryCategory category = categoryService.getCategoryById(id);
            if (category == null) {
                return Result.error("分类不存在");
            }
            return Result.success(category);
        } catch (Exception e) {
            log.error("获取分类详情失败", e);
            return Result.error("获取分类详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 智能匹配分类
     */
    @PostMapping("/smart-match")
    public Result<Long> smartMatchCategory(@RequestBody String itemName) {
        try {
            Long categoryId = categoryService.smartMatchCategory(itemName);
            return Result.success(categoryId);
        } catch (Exception e) {
            log.error("智能匹配分类失败", e);
            return Result.error("智能匹配分类失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建分类（仅管理员）
     */
    @PostMapping
    public Result<InventoryCategory> createCategory(@RequestBody InventoryCategory category) {
        try {
            InventoryCategory created = categoryService.createCategory(category);
            return Result.success(created);
        } catch (Exception e) {
            log.error("创建分类失败", e);
            return Result.error("创建分类失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新分类（仅管理员）
     */
    @PutMapping("/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody InventoryCategory category) {
        try {
            category.setId(id);
            categoryService.updateCategory(category);
            return Result.success();
        } catch (Exception e) {
            log.error("更新分类失败", e);
            return Result.error("更新分类失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除分类（仅管理员）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除分类失败", e);
            return Result.error("删除分类失败: " + e.getMessage());
        }
    }
}

