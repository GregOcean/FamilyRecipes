package com.familyrecipes.mapper.config;

import com.familyrecipes.entity.config.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 配置相关数据访问层
 */
@Mapper
public interface ConfigMapper {
    
    /**
     * 获取所有启用的常见食材
     */
    @Select("SELECT * FROM config_ingredients WHERE is_enabled = 1 ORDER BY sort_order, id")
    List<ConfigIngredient> findAllIngredients();
    
    /**
     * 根据分类获取食材
     */
    @Select("SELECT * FROM config_ingredients WHERE category = #{category} AND is_enabled = 1 ORDER BY sort_order, id")
    List<ConfigIngredient> findIngredientsByCategory(String category);
    
    /**
     * 获取所有启用的存储位置
     */
    @Select("SELECT * FROM config_storage_locations WHERE is_enabled = 1 ORDER BY sort_order, id")
    List<ConfigStorageLocation> findAllStorageLocations();
    
    /**
     * 获取默认存储位置
     */
    @Select("SELECT * FROM config_storage_locations WHERE is_default = 1 AND is_enabled = 1 LIMIT 1")
    ConfigStorageLocation findDefaultStorageLocation();
    
    /**
     * 获取所有启用的分类
     */
    @Select("SELECT * FROM config_categories WHERE is_enabled = 1 ORDER BY sort_order, id")
    List<ConfigCategory> findAllCategories();
    
    /**
     * 获取所有启用的文本配置
     */
    @Select("SELECT * FROM config_texts WHERE is_enabled = 1")
    List<ConfigText> findAllTexts();
    
    /**
     * 根据分类获取文本配置
     */
    @Select("SELECT * FROM config_texts WHERE category = #{category} AND is_enabled = 1")
    List<ConfigText> findTextsByCategory(String category);
    
    /**
     * 根据key获取文本配置
     */
    @Select("SELECT * FROM config_texts WHERE config_key = #{key} AND is_enabled = 1")
    ConfigText findTextByKey(String key);
}

