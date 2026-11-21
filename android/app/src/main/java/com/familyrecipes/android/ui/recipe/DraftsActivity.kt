package com.familyrecipes.android.ui.recipe

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.familyrecipes.android.R
import com.familyrecipes.android.data.local.*
import com.familyrecipes.android.ui.design.Ui
import com.google.gson.*
import kotlinx.coroutines.*

class DraftsActivity:AppCompatActivity() {
    private var job:Job?=null
    override fun onResume(){super.onResume();draw()}
    private fun draw(){
        job?.cancel()
        val root=Ui.box(this);setContentView(Ui.scroll(this,root))
        val heading=Ui.row(this);heading.addView(Ui.back(this){finish()});heading.addView(Ui.text(this,getString(R.string.recipe_drafts),22f,true));root.addView(heading)
        val account=LocalStore.account
        job=lifecycleScope.launch {try {
            DraftWrites.flush()
            val rows=LocalStore.instance.records().list(account,"draft").filter{it.key.startsWith("recipe:")||it.key.startsWith("cooking:")}
            if(account!=LocalStore.account)return@launch
            if(rows.isEmpty())root.addView(Ui.text(this@DraftsActivity,getString(R.string.ui_0b67983743)))
            rows.forEach { d ->
                val parsed=runCatching{Gson().fromJson(d.json,JsonObject::class.java)}.getOrNull()
                val title=if(d.key.startsWith("cooking:"))getString(R.string.review_record_draft,runCatching{parsed?.get("cookedAt")?.asString?.take(10)}.getOrNull().orEmpty())
                    else runCatching{parsed?.get("title")?.asString ?: parsed?.get("name")?.asString}.getOrNull().orEmpty().ifBlank{getString(R.string.review_new_draft)}
                val card=Ui.card(this@DraftsActivity)
                val row=Ui.row(this@DraftsActivity)
                row.addView(Ui.button(this@DraftsActivity,title){
                    val target=if(d.key.startsWith("cooking:"))CookingRecordActivity::class.java else EditRecipeActivity::class.java
                    val intent=Intent(this@DraftsActivity,target).putExtra("draft_key",d.key)
                    d.key.split(":").getOrNull(1)?.toLongOrNull()?.let{intent.putExtra("recipe_id",it)}
                    startActivity(intent)
                },android.widget.LinearLayout.LayoutParams(0,-2,1f))
                row.addView(Ui.button(this@DraftsActivity,getString(R.string.draft_delete)){
                    androidx.appcompat.app.AlertDialog.Builder(this@DraftsActivity).setMessage(R.string.draft_delete_confirm)
                        .setNegativeButton(android.R.string.cancel,null).setPositiveButton(android.R.string.ok){_,_->lifecycleScope.launch{DraftWrites.clear(account,d.key);if(account==LocalStore.account)draw()}}.show()
                });card.addView(row);root.addView(card)
            }
        }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@DraftsActivity,e)}}
    }
}
