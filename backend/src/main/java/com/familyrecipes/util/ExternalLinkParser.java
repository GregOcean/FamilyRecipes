package com.familyrecipes.util;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 外部链接解析工具类
 * 用于解析各种平台的分享链接，提取URL、标题和来源
 */
public class ExternalLinkParser {
    
    private static final Logger log = LoggerFactory.getLogger(ExternalLinkParser.class);
    
    /**
     * 解析结果
     */
    @Data
    public static class ParseResult {
        private String url;           // 完整的URL
        private String title;         // 标题（从分享文本中提取）
        private String source;        // 来源平台
        private String rawText;       // 原始文本
    }
    
    // 各平台URL模式
    private static final Pattern XIAOHONGSHU_PATTERN = Pattern.compile("(https?://)?xhslink\\.com/[a-zA-Z0-9]+");
    private static final Pattern XIACHUFANG_PATTERN = Pattern.compile("(https?://)?(?:www\\.)?xiachufang\\.com/recipe/\\d+/?");
    private static final Pattern DOUYIN_PATTERN = Pattern.compile("(https?://)?v\\.douyin\\.com/[a-zA-Z0-9]+/?");
    private static final Pattern BILIBILI_PATTERN = Pattern.compile("(https?://)?b23\\.tv/[a-zA-Z0-9]+");
    private static final Pattern BILIBILI_FULL_PATTERN = Pattern.compile("(https?://)?(?:www\\.)?bilibili\\.com/video/[a-zA-Z0-9]+");
    private static final Pattern MEISHIJIE_PATTERN = Pattern.compile("(https?://)?(?:www\\.)?meishij\\.net/");
    private static final Pattern DOUGUO_PATTERN = Pattern.compile("(https?://)?(?:www\\.)?douguo\\.com/");
    
    /**
     * 解析粘贴的链接内容
     * 
     * @param pastedText 用户粘贴的完整文本内容
     * @return 解析结果
     */
    public static ParseResult parse(String pastedText) {
        if (pastedText == null || pastedText.trim().isEmpty()) {
            return null;
        }
        
        pastedText = pastedText.trim();
        ParseResult result = new ParseResult();
        result.setRawText(pastedText);
        
        log.info("开始解析外部链接，原始文本长度: {}", pastedText.length());
        
        // 1. 提取URL
        String url = extractUrl(pastedText);
        if (url == null) {
            log.warn("无法从文本中提取有效URL");
            return null;
        }
        result.setUrl(url);
        log.info("提取到URL: {}", url);
        
        // 2. 识别来源平台
        String source = identifySource(url, pastedText);
        result.setSource(source);
        log.info("识别平台来源: {}", source);
        
        // 3. 提取标题
        String title = extractTitle(pastedText, source);
        result.setTitle(title);
        log.info("提取标题: {}", title);
        
        return result;
    }
    
    /**
     * 从文本中提取URL
     */
    private static String extractUrl(String text) {
        // 按行分割，逐行查找URL
        String[] lines = text.split("\\n");
        
        for (String line : lines) {
            line = line.trim();
            
            // 尝试各种平台的URL模式
            Matcher matcher;
            
            // 小红书短链接
            matcher = XIAOHONGSHU_PATTERN.matcher(line);
            if (matcher.find()) {
                String url = matcher.group();
                return ensureHttps(url);
            }
            
            // 下厨房
            matcher = XIACHUFANG_PATTERN.matcher(line);
            if (matcher.find()) {
                String url = matcher.group();
                return ensureHttps(url);
            }
            
            // 抖音短链接
            matcher = DOUYIN_PATTERN.matcher(line);
            if (matcher.find()) {
                String url = matcher.group();
                return ensureHttps(url);
            }
            
            // B站短链接
            matcher = BILIBILI_PATTERN.matcher(line);
            if (matcher.find()) {
                String url = matcher.group();
                return ensureHttps(url);
            }
            
            // B站完整链接
            matcher = BILIBILI_FULL_PATTERN.matcher(line);
            if (matcher.find()) {
                String url = matcher.group();
                return ensureHttps(url);
            }
            
            // 美食杰
            matcher = MEISHIJIE_PATTERN.matcher(line);
            if (matcher.find()) {
                String url = matcher.group();
                return ensureHttps(url);
            }
            
            // 豆果美食
            matcher = DOUGUO_PATTERN.matcher(line);
            if (matcher.find()) {
                String url = matcher.group();
                return ensureHttps(url);
            }
            
            // 通用URL提取（包含http/https的）
            Pattern genericPattern = Pattern.compile("https?://[^\\s]+");
            matcher = genericPattern.matcher(line);
            if (matcher.find()) {
                String url = matcher.group();
                // 清理可能的尾部标点符号
                url = url.replaceAll("[。，！？、；：）)]+$", "");
                return url;
            }
        }
        
        return null;
    }
    
    /**
     * 识别平台来源
     */
    private static String identifySource(String url, String text) {
        if (url == null) {
            return "未知";
        }
        
        // URL特征匹配
        if (url.contains("xhslink.com") || url.contains("xiaohongshu.com")) {
            return "小红书";
        }
        if (url.contains("xiachufang.com")) {
            return "下厨房";
        }
        if (url.contains("douyin.com")) {
            return "抖音";
        }
        if (url.contains("bilibili.com") || url.contains("b23.tv")) {
            return "哔哩哔哩";
        }
        if (url.contains("meishij.net")) {
            return "美食杰";
        }
        if (url.contains("douguo.com")) {
            return "豆果美食";
        }
        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            return "YouTube";
        }
        
        // 文本特征匹配
        if (text.contains("小红书") || text.contains("【小红书】")) {
            return "小红书";
        }
        if (text.contains("下厨房")) {
            return "下厨房";
        }
        if (text.contains("抖音") || text.contains("【抖音】")) {
            return "抖音";
        }
        if (text.contains("哔哩哔哩") || text.contains("bilibili") || text.contains("-哔哩哔哩】")) {
            return "哔哩哔哩";
        }
        
        return "网络";
    }
    
    /**
     * 从文本中提取标题
     */
    private static String extractTitle(String text, String source) {
        // 按行分割
        String[] lines = text.split("\\n");
        
        // 小红书：第一行通常是标题
        if ("小红书".equals(source)) {
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("http") && !line.contains("复制") && !line.contains("打开")) {
                    // 清理特殊字符
                    line = line.replaceAll("【小红书】", "");
                    line = line.replaceAll("复制后打开.*", "");
                    if (!line.isEmpty()) {
                        return line;
                    }
                }
            }
        }
        
        // 抖音：查看【xxx的作品】
        if ("抖音".equals(source)) {
            Pattern pattern = Pattern.compile("【(.+?)的作品】(.+?)(?:https?://|$)", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String title = matcher.group(2).trim();
                title = title.replaceAll("\\.\\.\\.$", "");
                title = title.replaceAll("[\\r\\n]+", " ");
                if (!title.isEmpty()) {
                    return title;
                }
            }
            
            // 备选：提取包含"作品"的行
            for (String line : lines) {
                if (line.contains("作品】") && !line.startsWith("http")) {
                    String title = line.replaceAll(".*作品】", "").trim();
                    title = title.replaceAll("https?://.*", "").trim();
                    title = title.replaceAll("\\.\\.\\.$", "").trim();
                    if (!title.isEmpty()) {
                        return title;
                    }
                }
            }
        }
        
        // 哔哩哔哩：查找【xxx】格式
        if ("哔哩哔哩".equals(source)) {
            Pattern pattern = Pattern.compile("【([^】]+)-哔哩哔哩】");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        
        // 下厨房：通常是一个简单的链接，标题在网页上
        if ("下厨房".equals(source)) {
            // 第一行如果不是URL，可能是标题
            if (lines.length > 0) {
                String firstLine = lines[0].trim();
                if (!firstLine.startsWith("http") && firstLine.length() < 100) {
                    return firstLine;
                }
            }
        }
        
        // 通用提取：找第一行有意义的文本
        for (String line : lines) {
            line = line.trim();
            // 跳过URL行、复制提示行、过短的行
            if (line.isEmpty() || 
                line.startsWith("http") || 
                line.contains("复制") || 
                line.contains("打开") ||
                line.matches("^[0-9.:\\s]+$") ||  // 纯数字时间
                line.length() < 3) {
                continue;
            }
            
            // 清理特殊标记
            line = line.replaceAll("【[^】]+】", "");
            line = line.trim();
            
            if (!line.isEmpty() && line.length() >= 3) {
                // 限制标题长度
                if (line.length() > 100) {
                    line = line.substring(0, 97) + "...";
                }
                return line;
            }
        }
        
        // 如果没有提取到标题，使用默认值
        return source + "菜谱";
    }
    
    /**
     * 确保URL带有https://前缀
     */
    private static String ensureHttps(String url) {
        if (url == null) {
            return null;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "https://" + url;
        }
        // 将http升级为https
        if (url.startsWith("http://")) {
            return "https://" + url.substring(7);
        }
        return url;
    }
}

