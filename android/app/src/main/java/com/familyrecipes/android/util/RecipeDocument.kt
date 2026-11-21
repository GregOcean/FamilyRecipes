package com.familyrecipes.android.util

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan

/** Displays imported plain text/Markdown without treating recipe content as executable HTML. */
object RecipeDocument {
    fun render(text: String): CharSequence {
        val output=SpannableStringBuilder()
        text.lines().forEachIndexed { index, raw ->
            if(index>0)output.append('\n')
            val line=raw.trimEnd()
            val heading=Regex("^#{1,6}\\s+(.+)$").matchEntire(line)
            val value=heading?.groupValues?.get(1) ?: line.replace(Regex("^\\s*[-*+]\\s+"),"• ")
            val start=output.length
            // Only render the small, unambiguous Markdown subset used in our imported recipes.
            var position=0
            Regex("\\*\\*([^*]+)\\*\\*").findAll(value).forEach { match ->
                output.append(value.substring(position,match.range.first))
                val boldStart=output.length
                output.append(match.groupValues[1])
                output.setSpan(StyleSpan(Typeface.BOLD),boldStart,output.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                position=match.range.last+1
            }
            output.append(value.substring(position))
            if(heading!=null) {
                output.setSpan(StyleSpan(Typeface.BOLD),start,output.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                output.setSpan(RelativeSizeSpan(1.16f),start,output.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return output
    }
}
