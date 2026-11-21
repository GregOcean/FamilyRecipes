package com.familyrecipes.android.ui.design

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import com.familyrecipes.android.R
import com.familyrecipes.android.ui.design.Ui.dp

/** Separate the filter category from its current selection, with an explicit menu affordance. */
class FilterButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val label = TextView(context).apply { textSize=12f; setTextColor(context.getColor(R.color.text_secondary)) }
    private val value = TextView(context).apply { textSize=14f; setTypeface(null,Typeface.BOLD); setTextColor(context.getColor(R.color.primary)) }
    init {
        orientation=VERTICAL
        minimumHeight=context.dp(52)
        setPadding(context.dp(6),context.dp(5),context.dp(6),context.dp(5))
        val header=LinearLayout(context)
        header.addView(label,LayoutParams(0,-2,1f))
        header.addView(TextView(context).apply { text="⌄";textSize=14f;setTextColor(context.getColor(R.color.primary));importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO })
        addView(header,LayoutParams(-1,-2));addView(value,LayoutParams(-1,-2))
        val shape=GradientDrawable().apply {setColor(context.getColor(R.color.surface));cornerRadius=context.dp(10).toFloat();setStroke(context.dp(1),context.getColor(R.color.outline))}
        background=RippleDrawable(ColorStateList.valueOf(context.getColor(R.color.primary_light)),shape,null)
        isClickable=true;isFocusable=true
        importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
        descendantFocusability=FOCUS_BLOCK_DESCENDANTS
        label.importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
        value.importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    fun setLabel(text:String) {label.text=text;updateDescription()}
    fun setValue(text:String) {value.text=text;updateDescription()}
    private fun updateDescription() {contentDescription="${label.text}: ${value.text}"}
    override fun getAccessibilityClassName():CharSequence = "android.widget.Button"
}
