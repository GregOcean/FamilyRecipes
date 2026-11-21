package com.familyrecipes.android.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LinkPlatformTest {
    @Test fun recognizesShortAndFullLinksWithoutStoredSource() {
        mapOf(
            "https://xhslink.cn/a/123" to LinkPlatform.XIAOHONGSHU,
            "https://xhslink.com/o/123" to LinkPlatform.XIAOHONGSHU,
            "https://www.xiaohongshu.com/explore/123?xsec_token=abc" to LinkPlatform.XIAOHONGSHU,
            "https://b23.tv/123" to LinkPlatform.BILIBILI,
            "https://m.bilibili.com/video/BV123" to LinkPlatform.BILIBILI,
            "HTTPS://WWW.YOUTUBE.COM/watch?v=123" to LinkPlatform.YOUTUBE,
            "https://youtu.be/123" to LinkPlatform.YOUTUBE,
            "https://channels.weixin.qq.com/123" to LinkPlatform.CHANNELS,
            "https://v.douyin.com/123/" to LinkPlatform.DOUYIN,
            "https://www.xiachufang.com/recipe/123" to LinkPlatform.XIACHUFANG,
            "https://instagram.com/reel/123" to LinkPlatform.INSTAGRAM,
            "https://fb.watch/123" to LinkPlatform.FACEBOOK
        ).forEach { (url,platform) -> assertEquals(url,platform,LinkPlatform.fromUrl(url)) }
    }
    @Test fun rejectsMisleadingDomainsAndNonWebSchemes() {
        listOf("https://xhslink.cn.example.com/a","https://example.com/?url=b23.tv",
            "https://xiaohongshu.com@example.com/a","javascript:youtube.com","not a url")
            .forEach { assertEquals(LinkPlatform.WEB,LinkPlatform.fromUrl(it)) }
    }
}
