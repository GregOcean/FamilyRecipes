package com.familyrecipes.service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * 网页抓取服务
 * 用于从URL获取页面标题和缩略图
 */
@Service
public class WebScrapingService {
    
    private static final Logger log = LoggerFactory.getLogger(WebScrapingService.class);
    
    private static final int TIMEOUT = 10000; // 10秒超时
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36";
    
    /**
     * 抓取结果
     */
    public static class ScrapedData {
        private String title;
        private String thumbnailUrl;
        private boolean success;
        private String errorMessage;
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getThumbnailUrl() { return thumbnailUrl; }
        public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    /**
     * 从URL抓取页面信息
     * 
     * @param url 目标URL
     * @return 抓取结果，如果失败返回null
     */
    public ScrapedData scrapeUrl(String url) {
        ScrapedData result = new ScrapedData();
        
        try {
            log.info("开始抓取网页: {}", url);
            
            // 发起HTTP请求
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .execute();
            
            int statusCode = response.statusCode();
            log.info("HTTP响应状态码: {}", statusCode);
            
            if (statusCode >= 400) {
                log.warn("HTTP请求失败，状态码: {}", statusCode);
                result.setSuccess(false);
                result.setErrorMessage("HTTP " + statusCode);
                return result;
            }
            
            // 解析HTML
            Document doc = response.parse();
            
            // 提取标题
            String title = extractTitle(doc);
            if (title != null && !title.isEmpty()) {
                result.setTitle(title);
                log.info("提取到标题: {}", title);
            }
            
            // 提取缩略图
            String thumbnail = extractThumbnail(doc, url);
            if (thumbnail != null && !thumbnail.isEmpty()) {
                result.setThumbnailUrl(thumbnail);
                log.info("提取到缩略图: {}", thumbnail);
            }
            
            result.setSuccess(true);
            log.info("网页抓取成功");
            return result;
            
        } catch (SocketTimeoutException e) {
            log.warn("网页抓取超时: {}, 原因: {}", url, e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage("连接超时");
            return result;
            
        } catch (IOException e) {
            log.warn("网页抓取失败: {}, 原因: {}", url, e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
            
        } catch (Exception e) {
            log.error("网页抓取异常: {}", url, e);
            result.setSuccess(false);
            result.setErrorMessage("解析异常");
            return result;
        }
    }
    
    /**
     * 从文档中提取标题
     */
    private String extractTitle(Document doc) {
        // 优先级1：Open Graph标题
        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null && ogTitle.hasAttr("content")) {
            String title = ogTitle.attr("content").trim();
            if (!title.isEmpty()) {
                return cleanTitle(title);
            }
        }
        
        // 优先级2：Twitter卡片标题
        Element twitterTitle = doc.selectFirst("meta[name=twitter:title]");
        if (twitterTitle != null && twitterTitle.hasAttr("content")) {
            String title = twitterTitle.attr("content").trim();
            if (!title.isEmpty()) {
                return cleanTitle(title);
            }
        }
        
        // 优先级3：页面title标签
        String title = doc.title();
        if (title != null && !title.isEmpty()) {
            return cleanTitle(title);
        }
        
        // 优先级4：h1标签
        Element h1 = doc.selectFirst("h1");
        if (h1 != null) {
            String h1Text = h1.text().trim();
            if (!h1Text.isEmpty()) {
                return cleanTitle(h1Text);
            }
        }
        
        return null;
    }
    
    /**
     * 清理标题
     */
    private String cleanTitle(String title) {
        if (title == null) {
            return null;
        }
        
        // 移除常见的网站后缀
        title = title.replaceAll("\\s*[-_|]\\s*(下厨房|小红书|抖音|哔哩哔哩|bilibili|美食杰|豆果美食).*$", "");
        
        // 限制长度
        if (title.length() > 100) {
            title = title.substring(0, 97) + "...";
        }
        
        return title.trim();
    }
    
    /**
     * 从文档中提取缩略图
     */
    private String extractThumbnail(Document doc, String baseUrl) {
        // 优先级1：Open Graph图片
        Element ogImage = doc.selectFirst("meta[property=og:image]");
        if (ogImage != null && ogImage.hasAttr("content")) {
            String imageUrl = ogImage.attr("content").trim();
            if (!imageUrl.isEmpty()) {
                return resolveUrl(imageUrl, baseUrl);
            }
        }
        
        // 优先级2：Twitter卡片图片
        Element twitterImage = doc.selectFirst("meta[name=twitter:image]");
        if (twitterImage != null && twitterImage.hasAttr("content")) {
            String imageUrl = twitterImage.attr("content").trim();
            if (!imageUrl.isEmpty()) {
                return resolveUrl(imageUrl, baseUrl);
            }
        }
        
        // 优先级3：link rel="image_src"
        Element linkImage = doc.selectFirst("link[rel=image_src]");
        if (linkImage != null && linkImage.hasAttr("href")) {
            String imageUrl = linkImage.attr("href").trim();
            if (!imageUrl.isEmpty()) {
                return resolveUrl(imageUrl, baseUrl);
            }
        }
        
        // 优先级4：查找页面中的第一张大图片
        Elements images = doc.select("img[src]");
        for (Element img : images) {
            // 跳过小图标和logo
            if (img.hasAttr("width")) {
                try {
                    int width = Integer.parseInt(img.attr("width"));
                    if (width < 200) continue;
                } catch (NumberFormatException ignored) {}
            }
            
            if (img.hasAttr("height")) {
                try {
                    int height = Integer.parseInt(img.attr("height"));
                    if (height < 200) continue;
                } catch (NumberFormatException ignored) {}
            }
            
            // 跳过常见的图标和logo
            String src = img.attr("src").toLowerCase();
            if (src.contains("logo") || src.contains("icon") || src.contains("avatar")) {
                continue;
            }
            
            String imageUrl = img.attr("abs:src");
            if (!imageUrl.isEmpty() && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
                return imageUrl;
            }
        }
        
        return null;
    }
    
    /**
     * 解析相对URL为绝对URL
     */
    private String resolveUrl(String url, String baseUrl) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        // 已经是绝对URL
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        
        // 协议相对URL
        if (url.startsWith("//")) {
            return "https:" + url;
        }
        
        // 路径相对URL，拼接base URL
        try {
            if (baseUrl.endsWith("/") && url.startsWith("/")) {
                return baseUrl + url.substring(1);
            } else if (!baseUrl.endsWith("/") && !url.startsWith("/")) {
                return baseUrl + "/" + url;
            } else {
                return baseUrl + url;
            }
        } catch (Exception e) {
            log.warn("URL解析失败: {} + {}", baseUrl, url);
            return url;
        }
    }
}

