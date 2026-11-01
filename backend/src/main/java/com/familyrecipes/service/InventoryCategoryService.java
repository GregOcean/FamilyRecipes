package com.familyrecipes.service;

import com.familyrecipes.mapper.InventoryCategoryMapper;
import com.familyrecipes.model.InventoryCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 库存分类服务
 */
@Slf4j
@Service
public class InventoryCategoryService {
    
    @Autowired
    private InventoryCategoryMapper inventoryCategoryMapper;
    
    /**
     * 获取分类树（一级分类及其子分类）
     */
    public List<InventoryCategory> getCategoryTree() {
        // 获取所有一级分类
        List<InventoryCategory> topLevelCategories = inventoryCategoryMapper.getTopLevelCategories();
        
        // 为每个一级分类加载子分类
        for (InventoryCategory category : topLevelCategories) {
            List<InventoryCategory> children = inventoryCategoryMapper.getChildCategories(category.getId());
            category.setChildren(children);
        }
        
        return topLevelCategories;
    }
    
    /**
     * 获取所有分类（扁平列表）
     */
    public List<InventoryCategory> getAllCategories() {
        return inventoryCategoryMapper.getAllCategories();
    }
    
    /**
     * 根据ID获取分类
     */
    public InventoryCategory getCategoryById(Long id) {
        return inventoryCategoryMapper.getCategoryById(id);
    }
    
    /**
     * 创建分类
     */
    public InventoryCategory createCategory(InventoryCategory category) {
        // 如果没有指定排序，自动设置
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        
        inventoryCategoryMapper.insertCategory(category);
        return category;
    }
    
    /**
     * 更新分类
     */
    public void updateCategory(InventoryCategory category) {
        inventoryCategoryMapper.updateCategory(category);
    }
    
    /**
     * 删除分类
     */
    public void deleteCategory(Long id) {
        // 检查是否有子分类
        List<InventoryCategory> children = inventoryCategoryMapper.getChildCategories(id);
        if (!children.isEmpty()) {
            throw new com.familyrecipes.common.ApiException(400, "该分类下有子分类，无法删除");
        }
        
        inventoryCategoryMapper.deleteCategory(id);
    }
    
    /** Conservative suggestions; the caller may choose a category for a new item. */
    public Long smartMatchCategory(String itemName) {
        String category = IngredientCategoryRules.suggest(itemName);
        return inventoryCategoryMapper.getAllCategories().stream()
            .filter(c -> c.getName().equals(category)).map(InventoryCategory::getId)
            .findFirst().orElseThrow(() -> new com.familyrecipes.common.ApiException(500,"库存分类尚未初始化"));
    }
}
