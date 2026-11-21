package com.familyrecipes.android.ui.social

import android.content.Context
import com.familyrecipes.android.R

/** Localize known system events while leaving member names and user text intact. */
object ChatSystemText {
    fun display(context:Context,content:String):String = when {
        content=="创建了群聊" -> context.getString(R.string.social_event_created)
        content.startsWith("加入群聊：") -> context.getString(R.string.social_event_joined,content.removePrefix("加入群聊："))
        content.startsWith("已移除成员：") -> context.getString(R.string.social_event_removed,content.removePrefix("已移除成员："))
        content.startsWith("已将群管理员转让给 ") -> context.getString(R.string.social_event_transferred,content.removePrefix("已将群管理员转让给 "))
        content.endsWith(" 已退出群聊") -> context.getString(R.string.social_event_left,content.removeSuffix(" 已退出群聊"))
        else -> content
    }
}
