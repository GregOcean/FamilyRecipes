package com.familyrecipes.android.ui.design

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * 允许页面内容横向滑动切换相邻标签，同时不干扰子控件的点击和纵向滚动。
 */
class SwipeTabLayout(context: Context) : FrameLayout(context) {
    var onSwipeLeft: (() -> Unit)? = null
    var onSwipeRight: (() -> Unit)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var swiping = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                swiping = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - downX
                val dy = ev.y - downY
                if (!swiping && abs(dx) > touchSlop * 2 && abs(dx) > abs(dy) * 1.4f) {
                    swiping = true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> swiping = false
        }
        return swiping
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_UP -> {
                if (swiping) {
                    val dx = ev.x - downX
                    if (dx <= -touchSlop * 3) onSwipeLeft?.invoke()
                    else if (dx >= touchSlop * 3) onSwipeRight?.invoke()
                }
                swiping = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> swiping = false
        }
        return true
    }
}
