package com.familyrecipes.android.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.familyrecipes.android.data.remote.ApiClient
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
        root.addView(Ui.text(this,getString(R.string.about_sources_summary),16f))
        val count=Ui.text(this,"",16f).apply{visibility=android.view.View.GONE}
        root.addView(count)
        lifecycleScope.launch {
            try {
                val total=Ui.data(ApiClient.getService().catalogSummary())["publicRecipes"]
                if(total!=null){count.text=getString(R.string.about_catalog_count,total);count.visibility=android.view.View.VISIBLE}
            }catch(e:CancellationException){throw e}catch(_:Exception){ /* Never display a guessed count. */ }
        }
        root.addView(Ui.button(this,getString(R.string.content_sources)) {startActivity(Intent(this,ContentSourcesActivity::class.java))})
    }
}
