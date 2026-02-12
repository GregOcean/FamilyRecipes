package com.familyrecipes.controller;

import com.familyrecipes.common.Result;
import com.familyrecipes.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 全局搜索控制器
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    /**
     * 全局搜索（包括菜谱和食材）
     * @param keyword 搜索关键词
     * @param priorityType 优先类型：recipe（菜谱优先）、ingredient（食材优先）、relevance（相关性）
     * @param pageNum 页码
     * @param pageSize 每页数量
     */
    @GetMapping("/global")
    public Result<Map<String, Object>> globalSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "relevance") String priorityType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        
        Map<String, Object> result = searchService.globalSearch(
            keyword, priorityType, pageNum, pageSize
        );
        return Result.success(result);
    }
}

