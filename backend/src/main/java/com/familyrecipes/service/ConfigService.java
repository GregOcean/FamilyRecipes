package com.familyrecipes.service;

import com.familyrecipes.entity.config.*;
import com.familyrecipes.mapper.config.ConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 配置服务层
 */
@Service
public class ConfigService {
    
    @Autowired
    private ConfigMapper configMapper;
    
    /**
     * 获取所有常见食材（带缓存）
     */
    @Cacheable(value = "config", key = "'ingredients'")
    public List<ConfigIngredient> getAllIngredients() {
        return configMapper.findAllIngredients();
    }
    
    /**
     * 获取所有存储位置（带缓存）
     */
    @Cacheable(value = "config", key = "'storageLocations'")
    public List<ConfigStorageLocation> getAllStorageLocations() {
        return configMapper.findAllStorageLocations();
    }
    
    /**
     * 获取默认存储位置
     */
    @Cacheable(value = "config", key = "'defaultStorageLocation'")
    public ConfigStorageLocation getDefaultStorageLocation() {
        ConfigStorageLocation location = configMapper.findDefaultStorageLocation();
        if (location == null) {
            // 如果没有设置默认值，返回第一个
            List<ConfigStorageLocation> all = getAllStorageLocations();
            return all.isEmpty() ? null : all.get(0);
        }
        return location;
    }
    
    /**
     * 获取所有分类（带缓存）
     */
    @Cacheable(value = "config", key = "'categories'")
    public List<ConfigCategory> getAllCategories() {
        return configMapper.findAllCategories();
    }
    
    /**
     * 获取所有文本配置（带缓存）
     */
    @Cacheable(value = "config", key = "'texts'")
    public Map<String, String> getAllTexts() {
        List<ConfigText> texts = configMapper.findAllTexts();
        return texts.stream()
            .collect(Collectors.toMap(
                ConfigText::getConfigKey,
                ConfigText::getConfigValue,
                (v1, v2) -> v1
            ));
    }
    
    /**
     * 获取完整配置数据（一次性获取所有配置）
     */
    public Map<String, Object> getAllConfigs() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("ingredients", getAllIngredients());
        configs.put("storageLocations", getAllStorageLocations());
        configs.put("categories", getAllCategories());
        configs.put("texts", getAllTexts());
        configs.put("defaultStorageLocation", getDefaultStorageLocation());
        return configs;
    }
}

