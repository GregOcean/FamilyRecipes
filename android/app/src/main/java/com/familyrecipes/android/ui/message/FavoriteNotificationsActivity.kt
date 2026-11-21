package com.familyrecipes.android.ui.message

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.familyrecipes.android.R
import com.familyrecipes.android.data.local.*
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.design.Ui
import com.familyrecipes.android.ui.recipe.RecipeDetailActivity
import kotlinx.coroutines.*

class FavoriteNotificationsActivity:AppCompatActivity() {
    private lateinit var list:android.widget.LinearLayout
    private val account=LocalStore.account
    private var before=Long.MAX_VALUE
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        if(intent.getStringExtra("notification_account")?.let {it!=account}==true){finish();return}
        val root=Ui.box(this);setContentView(Ui.scroll(this,root))
        root.addView(Ui.back(this){finish()})
        root.addView(Ui.text(this,getString(R.string.favorite_alerts),24f,true))
        list=Ui.box(this,0);root.addView(list);load(false)
    }
    override fun onResume(){super.onResume();if(account!=LocalStore.account)finish()}
    private fun load(append:Boolean) { lifecycleScope.launch {
        try {
            val page=Ui.data(ApiClient.getService().favoriteNotifications(if(append)before else Long.MAX_VALUE))
            if(account!=LocalStore.account){finish();return@launch}
            if(!append) list.removeAllViews()
            page.items.take(50).forEach { n -> list.addView(Ui.entry(this@FavoriteNotificationsActivity,
                getString(R.string.favorite_alert_text,n.actorName,n.recipeName),n.createdAt,R.drawable.ic_book_outline) {
                startActivity(Intent(this@FavoriteNotificationsActivity,RecipeDetailActivity::class.java).putExtra("recipe_id",n.recipeId))
            }) }
            before=page.items.take(50).lastOrNull()?.id ?: before
            if(page.items.size>50) {
                val more=Ui.button(this@FavoriteNotificationsActivity,getString(R.string.chat_search_more)) {}
                more.setOnClickListener {list.removeView(more);load(true)};list.addView(more)
            }
            if(list.childCount==0)list.addView(Ui.text(this@FavoriteNotificationsActivity,getString(R.string.favorite_alert_empty)))
            if(page.items.isNotEmpty()) {
                try {
                    Ui.data(ApiClient.getService().readFavoriteNotifications(mapOf("throughId" to page.items.first().id,"fromId" to page.items.take(50).last().id)))
                    FavoriteNotifications.sync()
                }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@FavoriteNotificationsActivity,e)}
            }
        } catch(e:CancellationException){throw e} catch(e:Exception){
            if(account!=LocalStore.account)return@launch
            if(!append && list.childCount==0) FavoriteNotifications.cached(account).items.forEach { n ->
                list.addView(Ui.text(this@FavoriteNotificationsActivity,getString(R.string.favorite_alert_text,n.actorName,n.recipeName)))
            }
            list.addView(Ui.button(this@FavoriteNotificationsActivity,getString(R.string.chat_search_retry)){load(append)})
        }
    } }
}
