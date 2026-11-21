package com.familyrecipes.android.ui.social

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.familyrecipes.android.R
import com.familyrecipes.android.data.local.*
import com.familyrecipes.android.data.model.CreateGroupRequest
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.design.Ui
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class CreateGroupActivity:AppCompatActivity(){
    private lateinit var name:EditText
    private lateinit var about:EditText
    private lateinit var friends:LinearLayout
    private val selected=linkedSetOf<Long>()
    private val account=LocalStore.account
    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        if(!PreferenceManager.isLoggedIn()){finish();return}
        selected.addAll(state?.getLongArray("selected")?.toList().orEmpty())
        val root=Ui.box(this);root.addView(Ui.back(this){finish()});root.addView(Ui.text(this,getString(R.string.social_create_group),24f,true))
        name=Ui.field(this,getString(R.string.social_group_name),state?.getString("name").orEmpty()).apply{filters=arrayOf(android.text.InputFilter.LengthFilter(100))}
        about=Ui.field(this,getString(R.string.social_group_about),state?.getString("about").orEmpty()).apply{filters=arrayOf(android.text.InputFilter.LengthFilter(500))}
        root.addView(name);root.addView(about)
        root.addView(Ui.text(this,getString(R.string.social_choose_friends),18f,true))
        friends=Ui.box(this,0);root.addView(friends)
        val create=Ui.button(this,getString(R.string.social_create_group),true){}
        create.setOnClickListener{
            val title=name.text.toString().trim()
            if(title.isBlank()){name.error=getString(R.string.social_name_invalid);return@setOnClickListener}
            create.isEnabled=false;create.text=getString(R.string.social_creating)
            lifecycleScope.launch{try{
                check(account==LocalStore.account)
                val group=Ui.data(ApiClient.getAccountService(account).createGroup(CreateGroupRequest(title,about.text.toString().trim(),10,selected.toList())))
                if(account!=LocalStore.account){finish();return@launch}
                // The group already exists: a later refresh failure must never offer creation again.
                try {
                    val groups=Ui.data(ApiClient.getAccountService(account).getGroupsList())
                    if(account==LocalStore.account)GroupCache.save(account,groups)
                }catch(e:CancellationException){throw e}catch(_:Exception){}
                if(account!=LocalStore.account){finish();return@launch}
                startActivity(Intent(this@CreateGroupActivity,GroupChatActivity::class.java).putExtra(GroupChatActivity.EXTRA_GROUP_ID,group.id).putExtra(GroupChatActivity.EXTRA_GROUP_NAME,group.name))
                setResult(RESULT_OK);finish()
            }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@CreateGroupActivity,e)}finally{create.isEnabled=true;create.text=getString(R.string.social_create_group)}}
        }
        root.addView(create);setContentView(Ui.scroll(this,root))
    }
    override fun onResume(){super.onResume();if(::friends.isInitialized)loadFriends()}
    private fun loadFriends(){lifecycleScope.launch{try{
        val rows=Ui.data(ApiClient.getAccountService(account).getFriendsList());if(account!=LocalStore.account){finish();return@launch}
        selected.retainAll(rows.mapNotNull{it.friendId}.toSet());friends.removeAllViews()
        if(rows.isEmpty())friends.addView(Ui.text(this@CreateGroupActivity,getString(R.string.social_empty_friends),15f))
        rows.forEach{friend->friend.friendId?.let{id->
            friends.addView(CheckBox(this@CreateGroupActivity).apply{
                text=friend.nickname?.takeIf{it.isNotBlank()}?:friend.friend?.username?:id.toString()
                textSize=17f;setTextColor(Ui.ink);minHeight=dp(48);isChecked=id in selected
                setOnCheckedChangeListener{button,checked->if(checked && selected.size>=9){button.isChecked=false;Toast.makeText(this@CreateGroupActivity,getString(R.string.social_member_limit,9),Toast.LENGTH_SHORT).show()}else if(checked)selected.add(id) else selected.remove(id)}
            })
        }}
        friends.addView(Ui.button(this@CreateGroupActivity,getString(R.string.social_manage_friends)){startActivity(Intent(this@CreateGroupActivity,FriendsListActivity::class.java))})
    }catch(e:CancellationException){throw e}catch(e:Exception){friends.removeAllViews();friends.addView(Ui.button(this@CreateGroupActivity,getString(R.string.social_retry)){loadFriends()})}}}
    override fun onSaveInstanceState(out:Bundle){out.putString("name",name.text.toString());out.putString("about",about.text.toString());out.putLongArray("selected",selected.toLongArray());super.onSaveInstanceState(out)}
    private fun dp(n:Int)=(resources.displayMetrics.density*n).toInt()
}
