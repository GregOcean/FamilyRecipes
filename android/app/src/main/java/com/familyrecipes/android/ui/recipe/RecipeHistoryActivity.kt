package com.familyrecipes.android.ui.recipe

import com.familyrecipes.android.ui.design.UiStrings
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.familyrecipes.android.data.local.*
import com.familyrecipes.android.ui.design.Ui
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class RecipeHistoryActivity:AppCompatActivity(){
    override fun onCreate(state:Bundle?){super.onCreate(state)}
    override fun onResume(){super.onResume();draw()}
    private fun draw(){
        val root=Ui.box(this);setContentView(Ui.scroll(this,root))
        root.addView(Ui.back(this){finish()})
        root.addView(Ui.text(this,UiStrings.s(com.familyrecipes.android.R.string.ui_5ac95e3cdd),24f,true))
        root.addView(Ui.text(this,UiStrings.s(com.familyrecipes.android.R.string.ui_8637840d3d),14f))
        val account=LocalStore.account
        lifecycleScope.launch{try{
            val rows=RecipeHistory.list(account)
            if(rows.isEmpty())root.addView(Ui.text(this@RecipeHistoryActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_cfcddb772c)))
            else root.addView(Ui.button(this@RecipeHistoryActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_e44745f241)){
                androidx.appcompat.app.AlertDialog.Builder(this@RecipeHistoryActivity).setMessage(UiStrings.s(com.familyrecipes.android.R.string.ui_8cc3c5dbab))
                    .setPositiveButton(UiStrings.s(com.familyrecipes.android.R.string.ui_288f0c404c)){_,_->lifecycleScope.launch{RecipeHistory.clear(account);draw()}}.setNegativeButton(UiStrings.s(com.familyrecipes.android.R.string.ui_625fb26b4b),null).show()
            })
            rows.forEach{r->root.addView(Ui.entry(this@RecipeHistoryActivity,r.name,java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(java.util.Date(r.viewedAt)),com.familyrecipes.android.R.drawable.ic_book_outline){startActivity(Intent(this@RecipeHistoryActivity,RecipeDetailActivity::class.java).putExtra("recipe_id",r.id))})}
        }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@RecipeHistoryActivity,e)}}
    }
}
