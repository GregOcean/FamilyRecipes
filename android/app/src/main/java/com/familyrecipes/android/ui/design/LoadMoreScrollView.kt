package com.familyrecipes.android.ui.design

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.ScrollView

/** One request per upward gesture, including a swipe when already at the bottom. */
class LoadMoreScrollView(context:Context):ScrollView(context) {
    var onLoadMore:(()->Boolean)?=null
    private var startY=0f
    private var armed=false
    private var used=false
    private val slop=ViewConfiguration.get(context).scaledTouchSlop
    override fun dispatchTouchEvent(event:MotionEvent):Boolean {
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {startY=event.y;armed=false;used=false}
            MotionEvent.ACTION_MOVE -> {if(startY-event.y>slop && !used)armed=true;if(event.y-startY>slop)armed=false}
            MotionEvent.ACTION_CANCEL -> armed=false
        }
        val handled=super.dispatchTouchEvent(event)
        if(event.actionMasked==MotionEvent.ACTION_MOVE || event.actionMasked==MotionEvent.ACTION_UP)maybeLoad()
        return handled
    }
    override fun onScrollChanged(l:Int,t:Int,oldl:Int,oldt:Int) {
        super.onScrollChanged(l,t,oldl,oldt)
        if(t>oldt)maybeLoad()
    }
    private fun maybeLoad() {
        if(armed && !used && !canScrollVertically(1) && onLoadMore?.invoke()==true) {used=true;armed=false}
    }
}
