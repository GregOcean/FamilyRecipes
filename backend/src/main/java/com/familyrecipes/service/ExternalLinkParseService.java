package com.familyrecipes.service;

import com.familyrecipes.entity.ExternalRecipe;
import com.familyrecipes.util.ExternalLinkParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 外部链接解析服务
 * 整合URL解析和网页抓取功能
 */
@Service
public class ExternalLinkParseService {
    
    private static final Logger log = LoggerFactory.getLogger(ExternalLinkParseService.class);
    
    @Autowired
    private WebScrapingService webScrapingService;
    
    /**
     * 解析粘贴的外部链接内容
     * 
     * @param pastedText 用户粘贴的完整文本内容
     * @param addedBy 添加者用户ID
     * @return 解析后的ExternalRecipe对象
     */
    public ExternalRecipe parseExternalLink(String pastedText, Long addedBy) {
        log.info("====== 开始解析外部链接 ======");
        log.info("原始输入文本:\n{}", pastedText);
        
        // 第1步：使用ExternalLinkParser解析基本信息
        ExternalLinkParser.ParseResult parseResult = ExternalLinkParser.parse(pastedText);
        
        if (parseResult == null || parseResult.getUrl() == null) {
            log.warn("无法从输入文本中提取有效URL");
            return null;
        }
        
        log.info("解析结果 - URL: {}", parseResult.getUrl());
        log.info("解析结果 - 标题: {}", parseResult.getTitle());
        log.info("解析结果 - 来源: {}", parseResult.getSource());
        
        // 创建ExternalRecipe对象
        ExternalRecipe externalRecipe = new ExternalRecipe();
        externalRecipe.setUrl(parseResult.getUrl());
        externalRecipe.setTitle(parseResult.getTitle());
        externalRecipe.setSource(parseResult.getSource());
        externalRecipe.setAddedBy(addedBy);
        
        // 第2步：尝试抓取网页内容（异步静默处理）
        // 注意：这个操作可能失败（反爬、视频平台等），但不影响整体流程
        try {
            log.info("尝试抓取网页内容...");
            WebScrapingService.ScrapedData scrapedData = webScrapingService.scrapeUrl(parseResult.getUrl());
            
            if (scrapedData != null && scrapedData.isSuccess()) {
                log.info("网页抓取成功");
                
                // 如果网页抓取到了标题，且比解析的标题更详细，则使用抓取的标题
                if (scrapedData.getTitle() != null && !scrapedData.getTitle().isEmpty()) {
                    // 如果解析的标题是默认值（如"抖音菜谱"），则替换
                    if (parseResult.getTitle().endsWith("菜谱")) {
                        externalRecipe.setTitle(scrapedData.getTitle());
                        log.info("使用抓取的标题替换默认标题: {}", scrapedData.getTitle());
                    } else if (scrapedData.getTitle().length() > parseResult.getTitle().length()) {
                        // 或者如果抓取的标题更长（可能更完整），也考虑替换
                        externalRecipe.setTitle(scrapedData.getTitle());
                        log.info("使用更完整的抓取标题: {}", scrapedData.getTitle());
                    }
                }
                
                // 设置缩略图
                if (scrapedData.getThumbnailUrl() != null && !scrapedData.getThumbnailUrl().isEmpty()) {
                    externalRecipe.setThumbnail(scrapedData.getThumbnailUrl());
                    log.info("设置缩略图: {}", scrapedData.getThumbnailUrl());
                }
            } else {
                // 抓取失败，记录警告但不影响功能
                String errorMsg = scrapedData != null ? scrapedData.getErrorMessage() : "未知错误";
                log.warn("网页抓取失败: {} - {}", parseResult.getUrl(), errorMsg);
                log.warn("原因可能是：反爬虫限制、视频平台、网络超时、图片下载失败等");
            }
        } catch (Exception e) {
            // 任何抓取异常都静默处理，只记录警告日志
            log.warn("网页抓取过程中发生异常: {}", e.getMessage());
            log.warn("将使用基础解析信息继续处理");
        }
        
        log.info("====== 外部链接解析完成 ======");
        log.info("最终标题: {}", externalRecipe.getTitle());
        log.info("最终URL: {}", externalRecipe.getUrl());
        log.info("最终来源: {}", externalRecipe.getSource());
        log.info("最终缩略图: {}", externalRecipe.getThumbnail());
        
        return externalRecipe;
    }
    
    /**
     * 批量解析外部链接
     * 
     * @param pastedTexts 多个粘贴文本
     * @param addedBy 添加者用户ID
     * @return 解析后的ExternalRecipe列表
     */
    public java.util.List<ExternalRecipe> parseExternalLinks(java.util.List<String> pastedTexts, Long addedBy) {
        java.util.List<ExternalRecipe> results = new java.util.ArrayList<>();
        
        for (String text : pastedTexts) {
            ExternalRecipe recipe = parseExternalLink(text, addedBy);
            if (recipe != null) {
                results.add(recipe);
            }
        }
        
        return results;
    }
}

