package com.familyrecipes.android.ui.social

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.familyrecipes.android.data.model.ChatContent
import com.familyrecipes.android.ui.design.Ui

/** Editing a mention invalidates its identity instead of pinging the wrong person. */
class MentionSpan(val userId: Long, val label: String) : ForegroundColorSpan(Ui.blue)
object ChatMentions {
    fun decode(wire: String): SpannableStringBuilder {
        val text = SpannableStringBuilder()
        var end = 0
        ChatContent.mentionPattern.findAll(wire).forEach { match ->
            text.append(wire.substring(end, match.range.first))
            val label = "@${match.groupValues[1]}"
            val start = text.length
            text.append(label)
            match.groupValues[2].toLongOrNull()?.let { id ->
                text.setSpan(MentionSpan(id, label), start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            end = match.range.last + 1
        }
        text.append(wire.substring(end))
        return text
    }
    fun encode(text: CharSequence): String {
        if(text !is Spanned) return text.toString()
        val result = StringBuilder(text.toString())
        text.getSpans(0, text.length, MentionSpan::class.java).sortedByDescending { text.getSpanStart(it) }.forEach { span ->
            val start = text.getSpanStart(span); val end = text.getSpanEnd(span)
            if(start >= 0 && end > start && text.subSequence(start, end).toString() == span.label)
                result.replace(start, end, ChatContent.mention(span.userId, span.label.removePrefix("@")))
        }
        return result.toString()
    }
}
