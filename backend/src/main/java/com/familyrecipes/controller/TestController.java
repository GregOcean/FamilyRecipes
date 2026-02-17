package com.familyrecipes.controller;

import com.familyrecipes.common.Result;
import com.familyrecipes.entity.ExternalRecipe;
import com.familyrecipes.service.ExternalLinkParseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试控制器 - 用于开发调试
 * 生产环境应删除或禁用
 */
@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @Autowired
    private ExternalLinkParseService externalLinkParseService;
    
    /**
     * 测试外部链接解析
     * 访问: http://localhost:8080/api/test/parse-links
     */
    @GetMapping("/parse-links")
    public Result<List<ExternalRecipe>> testParsing() {
        List<ExternalRecipe> results = new ArrayList<>();
        
        // 测试1: 小红书
        String xiaohongshu = "这种包子是泡打粉发的面吗？ 自家做的包子，面皮永远... http://xhslink.com/o/42iB8crbRgI \n复制后打开【小红书】查看笔记！";
        ExternalRecipe result1 = externalLinkParseService.parseExternalLink(xiaohongshu, 1L);
        if (result1 != null) results.add(result1);
        
        // 测试2: 下厨房
        String xiachufang = "https://www.xiachufang.com/recipe/107661996/";
        ExternalRecipe result2 = externalLinkParseService.parseExternalLink(xiachufang, 1L);
        if (result2 != null) results.add(result2);
        
        // 测试3: 抖音
        String douyin = "3.35 复制打开抖音，看看【四全丈夫的作品】保姆级卤牛肉的教程，一步不拉，酱味入心，口感不柴。... https://v.douyin.com/rE4SXmayYJA/ 07/19 EHv:/ O@x.FH";
        ExternalRecipe result3 = externalLinkParseService.parseExternalLink(douyin, 1L);
        if (result3 != null) results.add(result3);
        
        // 测试4: B站
        String bilibili = "【隋卞一做 |嚯！好烂的牛肉！-哔哩哔哩】 https://b23.tv/T12xxOU";
        ExternalRecipe result4 = externalLinkParseService.parseExternalLink(bilibili, 1L);
        if (result4 != null) results.add(result4);
        
        return Result.success(results);
    }
}

