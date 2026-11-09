package com.familyrecipes.android.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.familyrecipes.android.R
import com.familyrecipes.android.ui.design.Ui
import org.json.JSONArray

/** Pinned, escaped source content. JavaScript and network subresources are disabled. */
class LearningActivity : AppCompatActivity() {
    private var web: WebView? = null
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val section=intent.getStringExtra("section") ?: "beginner"
        val root=Ui.box(this)
        val top=Ui.row(this)
        top.addView(Ui.back(this) { finish() })
        top.addView(Ui.text(this,getString(if(section=="advanced") R.string.learning_advanced else R.string.learning_beginner),22f,true))
        root.addView(top)
        val chapters=JSONArray(assets.open("learning/index.json").bufferedReader().use{it.readText()})
        val file=intent.getStringExtra("chapter")
        if(file!=null && (0 until chapters.length()).any{chapters.getJSONObject(it).getString("file")==file}) {
            web=WebView(this).apply {
                settings.javaScriptEnabled=false
                settings.allowFileAccess=false
                settings.allowContentAccess=false
                settings.blockNetworkLoads=true
                webViewClient=object:WebViewClient(){
                    override fun shouldOverrideUrlLoading(view:WebView, request:WebResourceRequest):Boolean {
                        val url=request.url
                        if(request.isForMainFrame && request.hasGesture() && url.scheme=="https") {
                            runCatching{startActivity(Intent(Intent.ACTION_VIEW,url))}
                        }
                        return true
                    }
                }
                loadUrl("file:///android_asset/learning/$file")
            }
            root.addView(web,LinearLayout.LayoutParams(-1,0,1f))
        } else {
            val list=Ui.box(this,0)
            for(i in 0 until chapters.length()) {
                val chapter=chapters.getJSONObject(i)
                if(chapter.getString("section")==section) list.addView(Ui.button(this,chapter.getString("title")) {
                    startActivity(Intent(this,LearningActivity::class.java).putExtra("section",section).putExtra("chapter",chapter.getString("file")))
                })
            }
            root.addView(Ui.scroll(this,list),LinearLayout.LayoutParams(-1,0,1f))
        }
        setContentView(root)
    }
    override fun onDestroy(){web?.destroy();super.onDestroy()}
}
