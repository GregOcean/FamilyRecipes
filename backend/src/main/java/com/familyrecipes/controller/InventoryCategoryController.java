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
        } catch (com.familyrecipes.common.ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取分类树失败", e);
            throw new com.familyrecipes.common.ApiException(500, "请求处理失败", e);
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
        } catch (com.familyrecipes.common.ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取所有分类失败", e);
            throw new com.familyrecipes.common.ApiException(500, "请求处理失败", e);
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
        } catch (com.familyrecipes.common.ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取分类详情失败", e);
            throw new com.familyrecipes.common.ApiException(500, "请求处理失败", e);
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
        } catch (com.familyrecipes.common.ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("智能匹配分类失败", e);
            throw new com.familyrecipes.common.ApiException(500, "请求处理失败", e);
        }
    }
    
}
