package com.familyrecipes.android.ui.design

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.view.*
import android.widget.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.familyrecipes.android.R
import com.familyrecipes.android.data.model.*
import com.familyrecipes.android.ui.adapter.FridgeAdapter
import com.familyrecipes.android.ui.design.Ui.dp
import java.io.File
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompactFlowsTest {
    @Test fun filtersAndStockRowsFitSmallScreensAndLargeType() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for(language in listOf("zh","en")) for(scale in listOf(1f,2f)) for(night in listOf(false,true)) {
                val config=Configuration(instrumentation.targetContext.resources.configuration).apply {
                    setLocale(Locale(language));fontScale=scale
                    uiMode=(uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or if(night)Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
                val c=ContextThemeWrapper(instrumentation.targetContext.createConfigurationContext(config),R.style.Theme_FamilyRecipes)
                val root=LinearLayout(c).apply {orientation=LinearLayout.VERTICAL;setPadding(c.dp(12),c.dp(8),c.dp(12),0);setBackgroundColor(c.getColor(R.color.background))}
                val row=LinearLayout(c)
                val labels=listOf(R.string.filter_cuisine,R.string.filter_taste,R.string.filter_time,R.string.filter_stock)
                labels.forEach { id ->
                    val filter=FilterButton(c).apply {setLabel(c.getString(id));setValue(c.getString(if(id==R.string.filter_stock)R.string.stock_any_short else R.string.filter_any))}
                    row.addView(filter,LinearLayout.LayoutParams(0,-1,1f).apply{marginEnd=c.dp(4)})
                }
                root.addView(row)
                val inventory=LayoutInflater.from(c).inflate(R.layout.fragment_fridge,root,false)
                inventory.findViewById<FilterButton>(R.id.btn_categories).apply {setLabel(c.getString(R.string.inventory_category_filter));setValue(c.getString(R.string.ui_1a75030502))}
                inventory.findViewById<FilterButton>(R.id.btn_status).apply {setLabel(c.getString(R.string.inventory_expiry_filter));setValue(c.getString(R.string.filter_any))}
                // Move the actual inventory header into the fixture; rows below use the production adapter.
                val header=inventory as LinearLayout
                repeat(2){val v=header.getChildAt(0);header.removeView(v);root.addView(v)}
                val sample=FridgeItem(id=1,userId=1,ingredientId=1,amount="500 g",purchaseDate=null,expiryDate="2026-11-23",storageLocation=if(language=="en")"Refrigerated" else "冷藏",status="expiring",notes=null,
                    ingredient=Ingredient(1,if(language=="en")"Fresh whole milk" else "鲜牛奶",if(language=="en")"Dairy" else "乳制品"))
                val adapter=FridgeAdapter(listOf(sample),{},{})
                repeat(3) {
                    val holder=adapter.onCreateViewHolder(root,0);adapter.onBindViewHolder(holder,0)
                    root.addView(holder.itemView)
                }
                val width=c.dp(320)
                root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
                root.layout(0,0,width,root.measuredHeight)
                fun check(v:View) {
                    if(v.visibility!=View.VISIBLE)return
                    val r=Rect(0,0,v.width,v.height)
                    if(v!==root)root.offsetDescendantRectToMyCoords(v,r)
                    assertTrue("$language scale=$scale ${v.javaClass.simpleName}: $r",r.left>=0 && r.right<=width)
                    if(v is TextView && v.layout!=null)assertTrue("Clipped: ${v.text}",v.layout.height<=v.height-v.compoundPaddingTop-v.compoundPaddingBottom)
                    if(v is FilterButton)assertTrue(v.height>=c.dp(48))
                    if(v is ViewGroup)for(index in 0 until v.childCount)check(v.getChildAt(index))
                }
                check(root)
                if(scale==1f) {
                    val item=root.getChildAt(root.childCount-1)
                    assertTrue("Inventory row too tall: ${item.height/c.resources.displayMetrics.density}",item.height<=c.dp(105))
                    val shot=Bitmap.createBitmap(width,root.height,Bitmap.Config.ARGB_8888);root.draw(Canvas(shot))
                    File(instrumentation.targetContext.filesDir,"compact-flows-$language-${if(night)"dark" else "light"}.png").outputStream().use {shot.compress(Bitmap.CompressFormat.PNG,100,it)};shot.recycle()
                }
            }
        }
    }

    @Test fun bottomGestureRequestsOnePageAndCanRetryOnNextSwipe() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val c=instrumentation.targetContext
            val scroll=LoadMoreScrollView(c)
            val content=LinearLayout(c).apply {minimumHeight=c.dp(1000)}
            scroll.addView(content,FrameLayout.LayoutParams(-1,c.dp(1000)))
            scroll.measure(View.MeasureSpec.makeMeasureSpec(c.dp(320),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(c.dp(300),View.MeasureSpec.EXACTLY))
            scroll.layout(0,0,c.dp(320),c.dp(300))
            var calls=0
            var busy=false
            scroll.onLoadMore={if(busy)false else {calls++;true}}
            fun event(action:Int,y:Int) {
                val now=android.os.SystemClock.uptimeMillis()
                val e=MotionEvent.obtain(now,now,action,40f,c.dp(y).toFloat(),0)
                scroll.dispatchTouchEvent(e);e.recycle()
            }
            // Merely restoring a scroll position must never fetch a page.
            scroll.scrollTo(0,c.dp(1000));assertEquals(0,calls)
            event(MotionEvent.ACTION_DOWN,240);event(MotionEvent.ACTION_MOVE,170);event(MotionEvent.ACTION_MOVE,100);event(MotionEvent.ACTION_UP,100)
            assertEquals(1,calls)
            busy=true
            event(MotionEvent.ACTION_DOWN,240);event(MotionEvent.ACTION_MOVE,120);event(MotionEvent.ACTION_UP,120)
            assertEquals(1,calls)
            busy=false
            event(MotionEvent.ACTION_DOWN,240);event(MotionEvent.ACTION_MOVE,120);event(MotionEvent.ACTION_UP,120)
            assertEquals(2,calls)
            event(MotionEvent.ACTION_DOWN,80);event(MotionEvent.ACTION_MOVE,160);event(MotionEvent.ACTION_UP,160)
            assertEquals(2,calls)
        }
    }
}
