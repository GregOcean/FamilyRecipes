package com.familyrecipes.android.util

/** Classify the URL host, never arbitrary share text, paths or redirect parameters. */
enum class LinkPlatform(val label:String, val mark:String, val background:Long, vararg val hosts:String) {
    XIAOHONGSHU("小红书","小红书",0xFFDF2846,"xiaohongshu.com","xhslink.com","xhslink.cn"),
    BILIBILI("bilibili","bilibili",0xFFCA4978,"bilibili.com","b23.tv"),
    YOUTUBE("YouTube","▶",0xFFCC2027,"youtube.com","youtu.be","youtube-nocookie.com"),
    DOUYIN("抖音","抖音",0xFF242331,"douyin.com","iesdouyin.com"),
    TIKTOK("TikTok","♪",0xFF242331,"tiktok.com"),
    CHANNELS("微信视频号","视频号",0xFFD57620,"channels.weixin.qq.com","finder.video.qq.com"),
    WECHAT("微信","微信",0xFF278643,"mp.weixin.qq.com","weixin.qq.com"),
    XIACHUFANG("下厨房","下厨房",0xFFB64C32,"xiachufang.com"),
    INSTAGRAM("Instagram","IG",0xFFAD367B,"instagram.com","instagr.am"),
    FACEBOOK("Facebook","f",0xFF2563B7,"facebook.com","fb.watch","fb.com"),
    DOUGUO("豆果美食","豆果",0xFF63813B,"douguo.com"),
    MEISHIJIE("美食杰","美食杰",0xFFB64C32,"meishij.net"),
    WEB("Web","↗",0xFF70665D);

    companion object {
        fun fromUrl(value:String):LinkPlatform {
            val uri=runCatching { java.net.URI(value.trim()) }.getOrNull() ?: return WEB
            if(uri.scheme?.lowercase() !in listOf("http","https") || uri.userInfo!=null) return WEB
            val host=uri.host?.lowercase(java.util.Locale.ROOT)?.trimEnd('.') ?: return WEB
            return values().firstOrNull { platform -> platform.hosts.any { host==it || host.endsWith(".$it") } } ?: WEB
        }
    }
}
