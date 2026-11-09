package com.familyrecipes.android.ui.settings

import android.os.Bundle
import android.widget.LinearLayout
import android.text.util.Linkify
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.familyrecipes.android.R
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.design.Ui
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Attribution is centralized and indexed by recipe, without cluttering recipe instructions. */
class ContentSourcesActivity : AppCompatActivity() {
    private lateinit var root: LinearLayout
    private var cursor = 0L
    private var loading = false
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        root=Ui.box(this); setContentView(Ui.scroll(this, root))
        root.addView(Ui.back(this) { finish() })
        root.addView(Ui.text(this,getString(R.string.content_sources),24f,true))
        root.addView(Ui.text(this,getString(R.string.sources_intro),16f))
        load()
    }
    private fun load() {
        if (loading) return
        loading=true
        lifecycleScope.launch {
            try {
                val rows=Ui.data(ApiClient.getService().contentSources(cursor))
                rows.forEach { row ->
                    val value=row.get("metadata")
                    val metadata=if(value.isJsonPrimitive) JsonParser().parse(value.asString).asJsonObject else value.asJsonObject
                    fun text(key:String)=metadata.get(key)?.takeUnless { it.isJsonNull }?.asString.orEmpty()
                    val card=Ui.card(this@ContentSourcesActivity)
                    card.addView(Ui.text(this@ContentSourcesActivity,"#${row.get("recipe_id").asLong}  ${row.get("name").asString}",18f,true))
                    val attribution=buildString {
                        append(text("author")).append('\n').append(text("source_url")).append('\n')
                        append(text("license")).append('\n').append(text("license_url")).append('\n')
                        append(text("history_url")).append('\n').append(text("modifications"))
                        metadata.getAsJsonArray("images")?.forEach { image ->
                            val i=image.asJsonObject
                            append("\n\n")
                            listOf("title","author","source_url","attribution_url","license","license_url").forEach { key ->
                                i.get(key)?.takeUnless { it.isJsonNull }?.asString?.let { append(android.text.Html.fromHtml(it,android.text.Html.FROM_HTML_MODE_LEGACY)).append('\n') }
                            }
                        }
                    }
                    card.addView(Ui.text(this@ContentSourcesActivity,attribution,14f).apply { Linkify.addLinks(this,Linkify.WEB_URLS) })
                    root.addView(card)
                    cursor=row.get("recipe_id").asLong
                }
                if(rows.size==100) addLoadButton(R.string.sources_more)
            } catch(e:CancellationException) {throw e}
              catch(_:Exception) {addLoadButton(R.string.sources_retry)}
            finally {loading=false}
        }
    }
    private fun addLoadButton(label:Int) {
        val button=Ui.button(this,getString(label)) {}
        button.setOnClickListener {root.removeView(button);load()};root.addView(button)
    }
}
