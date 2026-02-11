package com.familyrecipes.controller;

import com.familyrecipes.common.Result;
import com.familyrecipes.entity.config.*;
import com.familyrecipes.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 配置相关接口
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {
    
    @Autowired
    private ConfigService configService;
    
    /**
     * 获取所有配置（应用启动时调用）
     */
    @GetMapping("/all")
    public Result<Map<String, Object>> getAllConfigs() {
        return Result.success(configService.getAllConfigs());
    }
    
    /**
     * 获取常见食材列表
     */
    @GetMapping("/ingredients")
    public Result<List<ConfigIngredient>> getIngredients() {
        return Result.success(configService.getAllIngredients());
    }
    
    /**
     * 获取存储位置列表
     */
    @GetMapping("/storage-locations")
    public Result<List<ConfigStorageLocation>> getStorageLocations() {
        return Result.success(configService.getAllStorageLocations());
    }
    
    /**
     * 获取分类列表
     */
    @GetMapping("/categories")
    public Result<List<ConfigCategory>> getCategories() {
        return Result.success(configService.getAllCategories());
    }
    
    /**
     * 获取文本配置
     */
    @GetMapping("/texts")
    public Result<Map<String, String>> getTexts() {
        return Result.success(configService.getAllTexts());
    }
}

