package com.familyrecipes.android.ui.social

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.familyrecipes.android.R
import com.familyrecipes.android.data.local.PreferenceManager
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.design.Ui
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.*
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.familyrecipes.android.data.local.FriendInvitations
import com.familyrecipes.android.data.local.LocalStore
import com.familyrecipes.android.data.model.FriendInvitation
import com.familyrecipes.android.data.model.Friendship

class FriendsListActivity : AppCompatActivity() {
    private lateinit var list:LinearLayout
    private lateinit var results:LinearLayout
    private var displayed:Pair<List<FriendInvitation>,List<Friendship>>?=null
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        if(!PreferenceManager.isLoggedIn() || intent.getStringExtra("invitation_account")?.let { it!=LocalStore.account }==true) {
            android.widget.Toast.makeText(this,getString(R.string.friend_account_mismatch),android.widget.Toast.LENGTH_LONG).show()
            finish();return
        }
        val root=Ui.box(this);val top=Ui.row(this)
        top.addView(Ui.back(this){finish()});top.addView(Ui.text(this,getString(R.string.friends_title),22f,true));root.addView(top)
        val input=Ui.field(this,getString(R.string.friend_search_hint));root.addView(input)
        val search=Ui.button(this,getString(R.string.friend_search)){}
        search.setOnClickListener {
            search.isEnabled=false
            lifecycleScope.launch {
                try {
                    val users=Ui.data(ApiClient.getService().searchFriends(input.text.toString().trim()))
                    results.removeAllViews()
                    if(users.isEmpty())results.addView(Ui.text(this@FriendsListActivity,getString(R.string.friend_not_found)))
                    users.forEach { user->
                        val card=Ui.card(this@FriendsListActivity)
                        card.addView(Ui.text(this@FriendsListActivity,"${user.username} · ID ${user.id}"))
                        val invite=Ui.button(this@FriendsListActivity,getString(R.string.friend_invite)){}
                        invite.setOnClickListener {
                            invite.isEnabled=false
                            lifecycleScope.launch {try{
                                Ui.ok(ApiClient.getService().addFriend(mapOf("friendId" to user.id)))
                                invite.text=getString(R.string.friend_pending);load()
                            }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@FriendsListActivity,e);invite.isEnabled=true}}
                        };card.addView(invite);results.addView(card)
                    }
                }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@FriendsListActivity,e)}finally{search.isEnabled=true}
            }
        }
        root.addView(search)
        val content=Ui.box(this,0);results=Ui.box(this,0);list=Ui.box(this,0);content.addView(results);content.addView(list)
        root.addView(Ui.scroll(this,content),LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while(isActive) { refresh();delay(10_000) }
        }}
    }
    private fun load(){lifecycleScope.launch{refresh()}}
    private suspend fun refresh(){
        val account=LocalStore.account
        try {
            val cached=FriendInvitations.cached(account)
            if(list.childCount==0 && cached.isNotEmpty())render(cached,emptyList())
        } catch(e:CancellationException){throw e}catch(_:Exception){}

        try{
            val api=ApiClient.getService();val requests=FriendInvitations.sync(notify=false)
            if(account!=LocalStore.account)return
            if(displayed?.first!=requests)render(requests,displayed?.second.orEmpty())
            val friends=Ui.data(api.getFriendsList())
            if(account!=LocalStore.account)return
            render(requests,friends)
        }catch(e:CancellationException){throw e}catch(e:Exception){if(list.childCount==0)Ui.toast(this@FriendsListActivity,e)}
    }
    private fun render(requests:List<FriendInvitation>,friends:List<Friendship>) {
            val next=requests to friends
            if(displayed==next)return
            displayed=next
            val api=ApiClient.getService()
            list.removeAllViews()
            requests.forEach { req->
                val card=Ui.card(this@FriendsListActivity);card.addView(Ui.text(this@FriendsListActivity,req.username))
                if(req.recipientId==PreferenceManager.userId) {
                    val row=Ui.row(this@FriendsListActivity)
                    for(accept in listOf(true,false))row.addView(Ui.button(this@FriendsListActivity,getString(if(accept)R.string.friend_accept else R.string.friend_decline)) {
                        for(i in 0 until row.childCount)row.getChildAt(i).isEnabled=false
                        lifecycleScope.launch{try{Ui.ok(api.respondFriend(req.senderId,mapOf("accept" to accept)));load()}catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@FriendsListActivity,e)}finally{for(i in 0 until row.childCount)row.getChildAt(i).isEnabled=true}}
                    },LinearLayout.LayoutParams(0,-2,1f));card.addView(row)
                }else card.addView(Ui.text(this@FriendsListActivity,getString(R.string.friend_pending)))
                list.addView(card)
            }
            friends.forEach{f->list.addView(Ui.text(this@FriendsListActivity,"${f.nickname?:f.friend?.username.orEmpty()} · ID ${f.friendId}"))}
            if(requests.isEmpty() && friends.isEmpty())list.addView(Ui.text(this@FriendsListActivity,getString(R.string.friend_empty)))
    }
}
