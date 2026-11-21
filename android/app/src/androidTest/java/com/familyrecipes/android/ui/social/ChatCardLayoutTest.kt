package com.familyrecipes.android.ui.social

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.familyrecipes.android.R
import com.familyrecipes.android.data.model.Message
import java.io.File
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android measurement, including MATCH_PARENT descendants and font scaling. No network. */
@RunWith(AndroidJUnit4::class)
class ChatCardLayoutTest {
    @Test fun compactActionsKeepTouchTargetsAndWrapEnglishLabels() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (language in listOf("zh","en")) for (scale in listOf(1f,2f)) {
                val config=Configuration(instrumentation.targetContext.resources.configuration).apply{setLocale(Locale(language));fontScale=scale}
                val c=ContextThemeWrapper(instrumentation.targetContext.createConfigurationContext(config),R.style.Theme_FamilyRecipes)
                val density=c.resources.displayMetrics.density
                val root=com.familyrecipes.android.ui.design.Ui.box(c,12)
                val label=if(language=="en") "Choose a saved recipe" else "从菜谱中选择"
                val button=com.familyrecipes.android.ui.design.Ui.button(c,label){}
                root.addView(button)
                val row=com.familyrecipes.android.ui.design.Ui.row(c)
                listOf(R.string.chat_share_order,R.string.chat_share_prep).forEach { id ->
                    row.addView(com.familyrecipes.android.ui.design.Ui.tile(c,c.getString(id),R.drawable.ic_ui_order){},android.widget.LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=(4*density).toInt()})
                }
                root.addView(row)
                val width=(320*density).toInt()
                root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
                root.layout(0,0,width,root.measuredHeight)
                assertTrue("Touch target smaller than 48dp",button.height>=48*density-1)
                if(scale==1f)assertTrue("Ordinary button still too tall",button.height<=50*density)
                fun check(view:View) {
                    if(view is TextView && view.layout!=null)assertTrue("Clipped action: ${view.text}",view.layout.height<=view.height-view.compoundPaddingTop-view.compoundPaddingBottom)
                    if(view is ViewGroup)for(i in 0 until view.childCount)check(view.getChildAt(i))
                }
                check(root)
                if(scale==1f){
                    val shot=Bitmap.createBitmap(width,root.height,Bitmap.Config.ARGB_8888)
                    root.draw(Canvas(shot))
                    File(instrumentation.targetContext.filesDir,"compact-actions-$language.png").outputStream().use{shot.compress(Bitmap.CompressFormat.PNG,100,it)}
                    shot.recycle()
                }
            }
        }
    }

    @Test fun sharedCardsStayInsideConversationInBothLanguagesAndLargeType() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val originalMode = AppCompatDelegate.getDefaultNightMode()
            try {
                for (night in listOf(false, true)) {
                    AppCompatDelegate.setDefaultNightMode(if(night) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
                    for (language in listOf("zh", "en")) for (scale in listOf(1f, 1.3f, 2f)) {
                        val config = Configuration(instrumentation.targetContext.resources.configuration).apply {
                            setLocale(Locale(language));fontScale=scale
                            uiMode=(uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                                if(night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                        }
                        val context=ContextThemeWrapper(instrumentation.targetContext.createConfigurationContext(config),R.style.Theme_FamilyRecipes)
                        val title=if(language=="en") "Weekend dinner with steamed fish and seasonal vegetables for everyone" else "周末全家一起吃的晚餐：清蒸鲈鱼和新鲜时蔬，还有孩子最爱的糖醋排骨"
                        val dishes=if(language=="en") "Steamed fish with ginger and scallions、Sweet and sour pork ribs、Tomato and egg soup、Seasonal vegetables" else "姜葱清蒸鲈鱼、糖醋排骨配新鲜时蔬、番茄鸡蛋汤、清炒时蔬"
                        val contents=listOf("点菜：$title\n$dishes\nfamilyrecipes://meal/1","备菜：$title\n2026-11-20 · Dinner\n$dishes\nfamilyrecipes://meal/2","菜谱：$title\nfamilyrecipes://recipe/3", "很长的普通聊天消息 $title $dishes https://example.test/long-link")
                        for(widthDp in listOf(320,360,600)) for(sent in listOf(false,true)) for((index,content) in contents.withIndex()) {
                            val message=Message(id=1,groupId=1,senderId=if(sent)1 else 2,content=content,messageType="text",createdAt=null,sender=null)
                            val adapter=MessageAdapter(listOf(message),1){}
                            val holder=adapter.onCreateViewHolder(FrameLayout(context),0)
                            adapter.onBindViewHolder(holder,0)
                            val root=holder.root
                            val width=(widthDp*context.resources.displayMetrics.density).toInt()
                            root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
                            root.layout(0,0,width,root.measuredHeight)
                            fun check(view:View) {
                                if(view.visibility!=View.VISIBLE)return
                                val rect=Rect(0,0,view.width,view.height)
                                if(view!==root)root.offsetDescendantRectToMyCoords(view,rect)
                                val info="$language, font=$scale, night=$night, width=$widthDp, sent=$sent, kind=$index, ${view.javaClass.simpleName} ${if(view is TextView)view.text else ""}: $rect / $width"
                                assertTrue(info,rect.left>=0 && rect.right<=width)
                                if(view is TextView && view.layout!=null)assertTrue("Vertical clipping: $info",view.layout.height<=view.height-view.compoundPaddingTop-view.compoundPaddingBottom)
                                if(view is ViewGroup)for(i in 0 until view.childCount)check(view.getChildAt(i))
                            }
                            check(root)
                            if(widthDp==360 && scale==1f && index==0 && sent) {
                                val bitmap=Bitmap.createBitmap(width,root.height,Bitmap.Config.ARGB_8888)
                                root.draw(Canvas(bitmap))
                                File(instrumentation.targetContext.filesDir,"chat-card-$language-${if(night)"dark" else "light"}.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
                                bitmap.recycle()
                                val screen=android.view.LayoutInflater.from(context).inflate(R.layout.activity_group_chat,null)
                                screen.findViewById<TextView>(R.id.tv_group_name).text=if(language=="en") "Cooking together · 3 members" else "一起下厨 · 3人"
                                val list=screen.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_messages)
                                list.layoutManager=androidx.recyclerview.widget.LinearLayoutManager(context)
                                val preview=listOf(
                                    Message(id=11,groupId=1,senderId=2,content=if(language=="en") "What shall we cook tonight?" else "今晚吃什么？我想吃清淡一点的。",messageType="text",createdAt=null,sender=null),
                                    message.copy(content=if(language=="en") "点菜：Tonight’s dinner\nSteamed fish、Tomato and egg soup、Stir-fried greens\nfamilyrecipes://meal/1" else "点菜：今晚的晚餐\n清蒸鲈鱼、番茄鸡蛋汤、清炒时蔬\nfamilyrecipes://meal/1"),
                                    Message(id=12,groupId=1,senderId=2,content=if(language=="en") "菜谱：Steamed fish with ginger\nfamilyrecipes://recipe/3" else "菜谱：姜葱清蒸鲈鱼\nfamilyrecipes://recipe/3",messageType="text",createdAt=null,sender=null))
                                list.adapter=MessageAdapter(preview,1){}
                                val h=(760*context.resources.displayMetrics.density).toInt()
                                screen.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY))
                                screen.layout(0,0,width,h)
                                val shot=Bitmap.createBitmap(width,h,Bitmap.Config.ARGB_8888)
                                screen.draw(Canvas(shot))
                                File(instrumentation.targetContext.filesDir,"chat-screen-$language-${if(night)"dark" else "light"}.png").outputStream().use{shot.compress(Bitmap.CompressFormat.PNG,100,it)}
                                shot.recycle()
                            }
                        }
                    }
                }
            } finally {AppCompatDelegate.setDefaultNightMode(originalMode)}
        }
    }
}
