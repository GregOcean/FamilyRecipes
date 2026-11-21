package com.familyrecipes.android.ui.design

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.familyrecipes.android.R
import com.familyrecipes.android.ui.MainActivity
import com.familyrecipes.android.ui.design.Ui.dp
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeHomeFiltersTest {
    @Test fun mainHomeUsesHeaderSearchAndFiveHorizontallyReachableFilters() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch<MainActivity>(Intent(instrumentation.targetContext,MainActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                activity.showPage("recipes")
                activity.supportFragmentManager.executePendingTransactions()
            }
            instrumentation.waitForIdleSync()
            var tasteFilter:FilterButton?=null
            scenario.onActivity { activity ->
                val fragment=activity.supportFragmentManager.findFragmentByTag("main:recipes")!!
                val root=fragment.requireView() as ViewGroup
                // The feed now starts with filters, not discovery/mine tabs or an embedded search field.
                assertTrue(root.getChildAt(0) is HorizontalScrollView)
                val strip=root.getChildAt(0) as HorizontalScrollView
                val filters=strip.getChildAt(0) as LinearLayout
                assertEquals(5,filters.childCount)
                tasteFilter=filters.getChildAt(2) as FilterButton
                assertTrue((filters.getChildAt(0) as FilterButton).contentDescription.startsWith(activity.getString(R.string.filter_source)))
                strip.measure(View.MeasureSpec.makeMeasureSpec(activity.dp(280),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
                strip.layout(0,0,activity.dp(280),strip.measuredHeight)
                assertTrue(strip.canScrollHorizontally(1))
                strip.scrollTo(filters.width,0)
                assertFalse(strip.canScrollHorizontally(1))
                val last=filters.getChildAt(4)
                assertTrue(last.right-strip.scrollX<=strip.width)
                val texts=mutableListOf<TextView>()
                fun collect(v:View){if(v is TextView)texts.add(v);if(v is ViewGroup)for(i in 0 until v.childCount)collect(v.getChildAt(i))}
                collect(activity.findViewById(android.R.id.content))
                assertTrue(texts.any{it.text.toString()==activity.getString(R.string.recipe_search_short)})
                assertFalse(texts.any{it.text.toString()==activity.getString(R.string.ui_fc4d75f6cb)})
            }
            fun step(block:(MainActivity)->Unit) {scenario.onActivity(block);instrumentation.waitForIdleSync()}
            fun current(activity:MainActivity)=activity.supportFragmentManager.findFragmentByTag("main:recipes") as com.familyrecipes.android.ui.recipe.RecipeListFragment
            fun choose(activity:MainActivity,index:Int) {
                val list=current(activity).selectionDialog!!.listView
                list.performItemClick(list.getChildAt(index-list.firstVisiblePosition),index,list.adapter.getItemId(index))
            }
            step {tasteFilter!!.performClick()}
            step {current(it).selectionDialog!!.getButton(android.content.DialogInterface.BUTTON_NEUTRAL).performClick()}
            step {tasteFilter!!.performClick()}
            step {choose(it,0);choose(it,3)}
            step {current(it).selectionDialog!!.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()}
            step {
                assertTrue(tasteFilter!!.contentDescription.toString(),tasteFilter!!.contentDescription.contains(UiStrings.domain("酸")))
                assertTrue(tasteFilter!!.contentDescription.contains(UiStrings.domain("辣")))
                tasteFilter!!.performClick()
            }
            step {
                assertTrue(current(it).selectionDialog!!.listView.isItemChecked(0))
                assertTrue(current(it).selectionDialog!!.listView.isItemChecked(3))
                choose(it,1)
            }
            step {current(it).selectionDialog!!.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick()}
            step {assertFalse(tasteFilter!!.contentDescription.contains(UiStrings.domain("甜")));tasteFilter!!.performClick()}
            step {current(it).selectionDialog!!.getButton(android.content.DialogInterface.BUTTON_NEUTRAL).performClick()}
            step { activity ->
                val strip=current(activity).requireView().let{it as ViewGroup}.getChildAt(0) as HorizontalScrollView
                strip.scrollTo(0,0)
                val view=activity.findViewById<View>(android.R.id.content)
                val bitmap=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
                view.draw(android.graphics.Canvas(bitmap))
                java.io.File(activity.filesDir,"recipe-home-compact.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
                bitmap.recycle()
            }
        }
    }
}
