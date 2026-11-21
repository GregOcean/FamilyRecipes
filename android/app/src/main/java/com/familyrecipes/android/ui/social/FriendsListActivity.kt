package com.familyrecipes.android.ui.social

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.familyrecipes.android.R
import com.familyrecipes.android.data.local.*
import com.familyrecipes.android.data.model.*
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.design.Ui
import com.familyrecipes.android.ui.design.UserAvatar
import com.google.gson.Gson
import kotlinx.coroutines.*

class FriendsListActivity:AppCompatActivity(){
    private lateinit var list:LinearLayout
    private lateinit var results:LinearLayout
    private lateinit var input:EditText
    private var displayed:Pair<List<FriendInvitation>,List<Friendship>>?=null
    private val account=LocalStore.account
    private var refreshing=false
    private var searchJob:Job?=null
    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        if(!PreferenceManager.isLoggedIn() || intent.getStringExtra("invitation_account")?.let{it!=account}==true){Toast.makeText(this,getString(R.string.social_account_changed),Toast.LENGTH_LONG).show();finish();return}
        val root=Ui.box(this);val top=Ui.row(this)
        top.addView(Ui.back(this){finish()});top.addView(Ui.text(this,getString(R.string.friends_title),22f,true));root.addView(top)
        val searchRow=Ui.row(this);input=Ui.field(this,getString(R.string.friend_search_hint)).apply{imeOptions=android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH}
        searchRow.addView(input,LinearLayout.LayoutParams(0,-2,1f));val search=Ui.button(this,getString(R.string.friend_search)){search()}
        searchRow.addView(search);root.addView(searchRow)
        input.setOnEditorActionListener{_,action,_->if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){search();true}else false}
        val content=Ui.box(this,0);results=Ui.box(this,0);list=Ui.box(this,0);content.addView(results);content.addView(list)
        root.addView(Ui.scroll(this,content),LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
        lifecycleScope.launch{repeatOnLifecycle(Lifecycle.State.RESUMED){while(isActive){refresh();delay(10_000)}}}
    }
    private fun search(){val query=input.text.toString().trim();if(query.isEmpty())return
        searchJob?.cancel()
        searchJob=lifecycleScope.launch{try{
            val users=Ui.data(ApiClient.getAccountService(account).searchFriends(query));if(account!=LocalStore.account){finish();return@launch}
            if(input.text.toString().trim()!=query)return@launch
            results.removeAllViews()
            if(users.isEmpty())results.addView(Ui.text(this@FriendsListActivity,getString(R.string.friend_not_found)))
            users.forEach{user->
                val row=userRow(user.username,user.avatar,"ID ${user.id}")
                val label=when(user.relationship){"accepted"->R.string.social_already_friend;"outgoing"->R.string.friend_pending;"incoming"->R.string.social_accept_invite;else->R.string.friend_invite}
                row.addView(Ui.button(this@FriendsListActivity,getString(label)){
                    operation{if(user.relationship=="incoming")Ui.ok(ApiClient.getAccountService(account).respondFriend(user.id,mapOf("accept" to true))) else Ui.ok(ApiClient.getAccountService(account).addFriend(mapOf("friendId" to user.id)))}
                }.apply{isEnabled=user.relationship !in listOf("accepted","outgoing")})
                results.addView(row)
            }
        }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@FriendsListActivity,e)}}
    }
    private suspend fun refresh(){
        if(refreshing)return
        refreshing=true
        try{
            if(account!=LocalStore.account){finish();return}
            if(displayed==null){
                val friends=LocalStore.instance.records().get(account,"friends","list")?.let{runCatching{Gson().fromJson(it,Array<Friendship>::class.java).toList()}.getOrNull()}.orEmpty()
                render(FriendInvitations.cached(account),friends)
            }
            val requests=FriendInvitations.sync(notify=false)
            val friends=Ui.data(ApiClient.getAccountService(account).getFriendsList())
            if(account!=LocalStore.account){finish();return}
            LocalStore.instance.records().put(LocalRecord(account,"friends","list",Gson().toJson(friends)))
            render(requests,friends)
        }catch(e:CancellationException){throw e}catch(e:Exception){if(list.childCount==0)Ui.toast(this@FriendsListActivity,e)}finally{refreshing=false}
    }
    private fun render(requests:List<FriendInvitation>,friends:List<Friendship>){
        val next=requests to friends;if(displayed==next)return;displayed=next;list.removeAllViews()
        for(incoming in listOf(true,false)){
            val rows=requests.filter{(it.recipientId==PreferenceManager.userId)==incoming}
            if(rows.isNotEmpty())list.addView(Ui.text(this,getString(if(incoming)R.string.social_incoming else R.string.social_outgoing),18f,true))
            rows.forEach{req->
                val card=Ui.card(this);card.addView(userRow(req.username,req.avatar,if(incoming)"ID ${req.senderId}" else getString(R.string.friend_pending)))
                val actions=Ui.row(this)
                if(incoming){
                    actions.addView(Ui.button(this,getString(R.string.friend_accept),true){operation{Ui.ok(ApiClient.getAccountService(account).respondFriend(req.senderId,mapOf("accept" to true)))}},LinearLayout.LayoutParams(0,-2,1f))
                    actions.addView(Ui.button(this,getString(R.string.friend_decline)){operation{Ui.ok(ApiClient.getAccountService(account).respondFriend(req.senderId,mapOf("accept" to false)))}},LinearLayout.LayoutParams(0,-2,1f))
                }else actions.addView(Ui.button(this,getString(R.string.social_cancel_invitation)){
                    confirm(getString(R.string.social_cancel_invitation_confirm)){Ui.ok(ApiClient.getAccountService(account).cancelFriendInvitation(req.recipientId))}
                })
                card.addView(actions);list.addView(card)
            }
        }
        list.addView(Ui.text(this,getString(R.string.social_my_friends)+" · ${friends.size}",18f,true))
        if(friends.isEmpty())list.addView(Ui.text(this,getString(R.string.social_empty_friends),15f))
        friends.forEach{f->val id=f.friendId?:return@forEach
            val label=f.nickname?.takeIf{it.isNotBlank()}?:f.friend?.username?:id.toString()
            val row=userRow(label,f.friend?.avatar,"ID $id")
            row.addView(Ui.button(this,"⋮"){
                AlertDialog.Builder(this).setTitle(label).setItems(arrayOf(getString(R.string.social_edit_note),getString(R.string.social_delete_friend))){_,which->
                    if(which==0){
                        val field=Ui.field(this,getString(R.string.social_friend_note),f.nickname.orEmpty()).apply{filters=arrayOf(android.text.InputFilter.LengthFilter(50))}
                        AlertDialog.Builder(this).setTitle(R.string.social_edit_note).setView(field).setNegativeButton(R.string.social_cancel,null).setPositiveButton(R.string.social_save){_,_->operation{Ui.ok(ApiClient.getAccountService(account).updateFriendNickname(id,mapOf("nickname" to field.text.toString().trim())))}}.show()
                    }else confirm(getString(R.string.social_delete_friend_confirm)){Ui.ok(ApiClient.getAccountService(account).deleteFriend(id))}
                }.show()
            }.apply{contentDescription=getString(R.string.social_message_tools)},LinearLayout.LayoutParams(dp(52),dp(48)))
            list.addView(row)
        }
    }
    private fun userRow(name:String,avatar:String?,subtitle:String):LinearLayout=Ui.row(this).apply{
        minimumHeight=dp(64);setPadding(0,dp(6),0,dp(6))
        addView(UserAvatar.create(this@FriendsListActivity,name,avatar),LinearLayout.LayoutParams(dp(42),dp(42)))
        val text=Ui.box(this@FriendsListActivity,0).apply{setBackgroundColor(android.graphics.Color.TRANSPARENT);setPadding(dp(10),0,dp(8),0)}
        text.addView(Ui.text(this@FriendsListActivity,name,17f,true).apply{setPadding(0,0,0,dp(3))})
        text.addView(Ui.text(this@FriendsListActivity,subtitle,13f).apply{setTextColor(Ui.muted);setPadding(0,0,0,0)})
        addView(text,LinearLayout.LayoutParams(0,-2,1f))
    }
    private var acting=false
    private fun operation(block:suspend()->Unit){if(acting)return;acting=true;lifecycleScope.launch{try{
        check(account==LocalStore.account);block();refresh();if(input.text.isNotBlank())search()
    }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@FriendsListActivity,e)}finally{acting=false}}}
    private fun confirm(text:String,block:suspend()->Unit){AlertDialog.Builder(this).setMessage(text).setNegativeButton(R.string.social_cancel,null).setPositiveButton(R.string.social_confirm){_,_->operation(block)}.show()}
    private fun dp(n:Int)=(resources.displayMetrics.density*n).toInt()
}
