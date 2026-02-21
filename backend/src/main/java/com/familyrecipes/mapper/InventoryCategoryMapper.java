package com.familyrecipes.mapper;

import com.familyrecipes.model.InventoryCategory;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 库存分类 Mapper
 */
@Mapper
public interface InventoryCategoryMapper {
    
    /**
     * 获取所有一级分类
     */
    @Select("SELECT * FROM inventory_categories WHERE parent_id IS NULL ORDER BY sort_order")
    List<InventoryCategory> getTopLevelCategories();
    
    /**
     * 获取指定父分类的子分类
     */
    @Select("SELECT * FROM inventory_categories WHERE parent_id = #{parentId} ORDER BY sort_order")
    List<InventoryCategory> getChildCategories(@Param("parentId") Long parentId);
    
    /**
     * 获取所有分类（包含子分类）
     */
    @Select("SELECT * FROM inventory_categories ORDER BY parent_id, sort_order")
    List<InventoryCategory> getAllCategories();
    
    /**
     * 根据ID获取分类
     */
    @Select("SELECT * FROM inventory_categories WHERE id = #{id}")
    InventoryCategory getCategoryById(@Param("id") Long id);
    
    /**
     * 创建分类
     */
    @Insert("INSERT INTO inventory_categories (name, parent_id, icon, sort_order) " +
            "VALUES (#{name}, #{parentId}, #{icon}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertCategory(InventoryCategory category);
    
    /**
     * 更新分类
     */
    @Update("UPDATE inventory_categories SET name = #{name}, icon = #{icon}, " +
            "sort_order = #{sortOrder} WHERE id = #{id}")
    void updateCategory(InventoryCategory category);
    
    /**
     * 删除分类
     */
    @Delete("DELETE FROM inventory_categories WHERE id = #{id}")
    void deleteCategory(@Param("id") Long id);
    
    /**
     * 根据名称模糊搜索分类
     */
    @Select("SELECT * FROM inventory_categories WHERE name LIKE CONCAT('%', #{keyword}, '%') ORDER BY sort_order")
    List<InventoryCategory> searchCategoriesByName(@Param("keyword") String keyword);
}

