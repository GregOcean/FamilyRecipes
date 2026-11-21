package com.familyrecipes.android.ui.social

import android.content.Context
import android.widget.LinearLayout

/** Bound the entire bubble, including MATCH_PARENT dividers, to its conversation column. */
internal class ChatBubbleLayout(context: Context) : LinearLayout(context) {
    init { orientation = VERTICAL }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val cap = (320 * resources.displayMetrics.density).toInt()
        val mode = MeasureSpec.getMode(widthMeasureSpec)
        val available = if (mode == MeasureSpec.UNSPECIFIED) cap else MeasureSpec.getSize(widthMeasureSpec)
        super.onMeasure(MeasureSpec.makeMeasureSpec(minOf(cap, available),
            if (mode == MeasureSpec.EXACTLY) MeasureSpec.EXACTLY else MeasureSpec.AT_MOST), heightMeasureSpec)
    }
}
