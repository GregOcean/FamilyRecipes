package com.familyrecipes.android.ui.settings

import android.content.Intent
import android.os.Bundle
import android.text.util.Linkify
import androidx.appcompat.app.AppCompatActivity
import com.familyrecipes.android.BuildConfig
import com.familyrecipes.android.R
import com.familyrecipes.android.ui.design.Ui
import org.json.JSONArray

class AboutActivity:AppCompatActivity() {
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        val root=Ui.box(this);setContentView(Ui.scroll(this,root))
        root.addView(Ui.back(this){finish()})
        root.addView(Ui.text(this,getString(R.string.about_title),24f,true))
        root.addView(Ui.text(this,"家肴 · ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",16f,true))
        root.addView(Ui.text(this,getString(R.string.about_description),16f))
        root.addView(Ui.text(this,getString(R.string.about_learning_sources),20f,true))
        root.addView(Ui.text(this,getString(R.string.about_learning_note),16f))
        val chapters=JSONArray(assets.open("learning/index.json").bufferedReader().use{it.readText()})
        for(i in 0 until chapters.length()) {
            val c=chapters.getJSONObject(i)
            root.addView(Ui.text(this,"${c.getString("title")}\n${c.getString("source")}\n${c.getString("license")} · ${c.getString("revision")}",14f).apply { Linkify.addLinks(this,Linkify.WEB_URLS) })
        }
        root.addView(Ui.button(this,getString(R.string.content_sources)) {startActivity(Intent(this,ContentSourcesActivity::class.java))})
    }
}
