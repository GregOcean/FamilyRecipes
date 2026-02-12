package com.familyrecipes.android.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL

/**
 * 网页解析工具类
 * 用于解析外部链接的标题、来源和缩略图
 */
object WebPageParser {
    
    private const val TAG = "WebPageParser"
    
    // 解析结果缓存，避免重复解析同一个URL
    private val parseCache = mutableMapOf<String, ParsedWebPage>()
    
    data class ParsedWebPage(
        val title: String,
        val source: String,
        val thumbnailUrl: String?,
        val originalUrl: String
    )
    
    /**
     * 解析网页链接
     */
    suspend fun parseUrl(url: String): ParsedWebPage? = withContext(Dispatchers.IO) {
        // 检查缓存
        parseCache[url]?.let {
            Log.d(TAG, "使用缓存结果: $url")
            return@withContext it
        }
        
        Log.d(TAG, "开始解析URL（无缓存）: $url")
        try {
            Log.d(TAG, "开始解析URL: $url")
            
            // 使用Jsoup获取网页内容，增强配置以支持重定向和反爬虫
            val doc: Document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Accept-Encoding", "gzip, deflate")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .referrer("https://www.google.com")
                .followRedirects(true)  // 跟随重定向
                .maxBodySize(0)  // 不限制body大小
                .timeout(15000)  // 增加超时时间到15秒
                .ignoreHttpErrors(true)  // 忽略HTTP错误
                .get()
            
            // 提取标题
            val title = extractTitle(doc, url)
            
            // 提取来源
            val source = extractSource(url)
            
            // 提取缩略图
            val thumbnail = extractThumbnail(doc, url)
            
            Log.d(TAG, "解析成功 - 标题: $title, 来源: $source, 缩略图: $thumbnail")
            
            val result = ParsedWebPage(
                title = title,
                source = source,
                thumbnailUrl = thumbnail,
                originalUrl = url
            )
            
            // 保存到缓存
            parseCache[url] = result
            Log.d(TAG, "已缓存解析结果，当前缓存数量: ${parseCache.size}")
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "解析URL失败: $url", e)
            // 返回默认值
            val result = ParsedWebPage(
                title = "外部菜谱",
                source = extractSource(url),
                thumbnailUrl = null,
                originalUrl = url
            )
            
            // 失败的结果也缓存，避免重复尝试
            parseCache[url] = result
            
            result
        }
    }
    
    /**
     * 提取标题
     * 优先级：og:title > twitter:title > title标签
     */
    private fun extractTitle(doc: Document, url: String): String {
        // 尝试获取 Open Graph 标题
        doc.select("meta[property=og:title]").first()?.attr("content")?.let {
            if (it.isNotBlank() && !it.contains("小红书 - 你的生活")) return it.trim()
        }
        
        // 尝试获取 Twitter Card 标题
        doc.select("meta[name=twitter:title]").first()?.attr("content")?.let {
            if (it.isNotBlank() && !it.contains("小红书 - 你的生活")) return it.trim()
        }
        
        // 尝试获取普通 title 标签
        doc.title()?.let {
            if (it.isNotBlank() && !it.contains("小红书 - 你的生活")) {
                // 清理标题，去掉常见的后缀
                val cleaned = it.trim()
                    .replace(Regex(" - 小红书.*$"), "")
                    .replace(Regex(" - 下厨房.*$"), "")
                    .replace(Regex(" - 豆果美食.*$"), "")
                    .trim()
                if (cleaned.isNotBlank()) return cleaned
            }
        }
        
        // 尝试从 description 中提取（某些网站标题不明显）
        doc.select("meta[name=description]").first()?.attr("content")?.let {
            if (it.length > 10 && it.length < 100) {
                return it.trim().take(50) // 限制长度
            }
        }
        
        // 如果都没有，使用来源名称 + "菜谱"
        return "${extractSource(url)}菜谱"
    }
    
    /**
     * 提取来源网站名称
     */
    private fun extractSource(url: String): String {
        try {
            val host = URL(url).host
            
            // 常见网站的中文名称映射
            return when {
                host.contains("xiaohongshu.com") || host.contains("xhslink.com") -> "小红书"
                host.contains("xiachufang.com") -> "下厨房"
                host.contains("douguo.com") -> "豆果美食"
                host.contains("meishichina.com") -> "美食天下"
                host.contains("xinshipu.com") -> "心食谱"
                host.contains("163.com") -> "网易"
                host.contains("sohu.com") -> "搜狐"
                host.contains("bilibili.com") || host.contains("b23.tv") -> "哔哩哔哩"
                host.contains("douyin.com") -> "抖音"
                host.contains("youtube.com") || host.contains("youtu.be") -> "YouTube"
                else -> {
                    // 提取主域名（去掉www等前缀）
                    val domain = host.split(".").let { parts ->
                        if (parts.size >= 2) {
                            parts[parts.size - 2].capitalize()
                        } else {
                            host
                        }
                    }
                    domain
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "提取来源失败", e)
            return "网络"
        }
    }
    
    /**
     * 提取缩略图
     * 优先级：og:image > twitter:image > 第一个大图片 > null
     */
    private fun extractThumbnail(doc: Document, baseUrl: String): String? {
        // 尝试获取 Open Graph 图片
        doc.select("meta[property=og:image]").first()?.attr("content")?.let {
            if (it.isNotBlank() && !it.startsWith("data:")) {
                return resolveUrl(it, baseUrl)
            }
        }
        
        // 尝试获取 Twitter Card 图片
        doc.select("meta[name=twitter:image]").first()?.attr("content")?.let {
            if (it.isNotBlank() && !it.startsWith("data:")) {
                return resolveUrl(it, baseUrl)
            }
        }
        
        // 尝试获取第一个较大的图片（宽度 > 200px）
        doc.select("img[src]").forEach { img ->
            val src = img.attr("src")
            
            // 跳过 base64 图片
            if (src.startsWith("data:")) {
                return@forEach
            }
            
            val width = img.attr("width").toIntOrNull() ?: 0
            
            // 如果图片宽度大于200px，或者没有宽度信息但URL看起来像主图
            if (width > 200 || (width == 0 && isLikelyMainImage(src))) {
                return resolveUrl(src, baseUrl)
            }
        }
        
        // 如果以上都没有，尝试获取第一个图片（排除 base64）
        doc.select("img[src]").first()?.attr("src")?.let {
            if (it.isNotBlank() && !it.contains("logo") && !it.contains("icon") && !it.startsWith("data:")) {
                return resolveUrl(it, baseUrl)
            }
        }
        
        return null
    }
    
    /**
     * 判断URL是否可能是主图
     */
    private fun isLikelyMainImage(src: String): Boolean {
        val lower = src.lowercase()
        return !lower.contains("logo") && 
               !lower.contains("icon") && 
               !lower.contains("avatar") &&
               !lower.contains("button") &&
               (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || 
                lower.endsWith(".png") || lower.endsWith(".webp"))
    }
    
    /**
     * 解析相对URL为绝对URL
     */
    private fun resolveUrl(url: String, baseUrl: String): String {
        return try {
            // 过滤掉 Base64 编码的图片（data:image/...）
            if (url.startsWith("data:")) {
                return ""
            }
            
            if (url.startsWith("http://") || url.startsWith("https://")) {
                url
            } else if (url.startsWith("//")) {
                "https:$url"
            } else {
                val base = URL(baseUrl)
                URL(base, url).toString()
            }
        } catch (e: Exception) {
            url
        }
    }
}

