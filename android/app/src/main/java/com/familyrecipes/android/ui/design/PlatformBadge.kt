package com.familyrecipes.android.ui.design

import android.graphics.*
import android.graphics.drawable.Drawable
import com.familyrecipes.android.util.LinkPlatform

/** Local platform wordmarks: no remote thumbnail request or dependency on metadata scraping. */
class PlatformBadge(private val platform:LinkPlatform):Drawable() {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(canvas:Canvas) {
        val rect=RectF(bounds)
        paint.color=platform.background.toInt()
        canvas.drawRoundRect(rect,rect.width()*.24f,rect.height()*.24f,paint)
        paint.color=Color.WHITE
        paint.typeface=Typeface.create("sans-serif",Typeface.BOLD)
        paint.textAlign=Paint.Align.CENTER
        paint.textSize=rect.width()*when {
            platform.mark=="bilibili" -> .22f
            platform.mark.length>=3 -> .25f
            platform.mark.length==2 -> .37f
            else -> .58f
        }
        val baseline=rect.centerY()-(paint.ascent()+paint.descent())/2
        canvas.drawText(platform.mark,rect.centerX(),baseline,paint)
    }
    override fun setAlpha(alpha:Int) { paint.alpha=alpha }
    override fun setColorFilter(filter:ColorFilter?) { paint.colorFilter=filter }
    @Deprecated("Deprecated in Java") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
