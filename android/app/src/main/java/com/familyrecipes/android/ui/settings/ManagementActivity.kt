package com.familyrecipes.android.ui.settings

import com.familyrecipes.android.ui.design.UiStrings
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.familyrecipes.android.data.local.PreferenceManager
import com.familyrecipes.android.data.model.Kitchen
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.design.Ui
import com.familyrecipes.android.ui.social.CreateGroupActivity
import com.familyrecipes.android.ui.social.GroupChatActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ManagementActivity : AppCompatActivity() {
    private lateinit var root: LinearLayout
    private val account=com.familyrecipes.android.data.local.LocalStore.account
    private var request:kotlinx.coroutines.Job?=null
    private var groupsMode=false
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        groupsMode=intent.getBooleanExtra("groups",false)
        root=Ui.box(this);setContentView(Ui.scroll(this,root))
    }
    override fun onResume(){super.onResume();if(!PreferenceManager.isLoggedIn() || account!=com.familyrecipes.android.data.local.LocalStore.account){finish();return};load()}
    private fun load(){
        request?.cancel()
        root.removeAllViews()
        root.addView(Ui.back(this@ManagementActivity){finish()})
        root.addView(Ui.text(this@ManagementActivity,if(groupsMode)UiStrings.s(com.familyrecipes.android.R.string.ui_47fdc41702) else UiStrings.s(com.familyrecipes.android.R.string.ui_d46695d56e),24f,true))
        request=lifecycleScope.launch {
            try {
                val groups=Ui.data(ApiClient.getAccountService(account).getGroupsList())
                if(account!=com.familyrecipes.android.data.local.LocalStore.account){finish();return@launch}
                if(groupsMode){
                    root.addView(Ui.text(this@ManagementActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_117723f7be),15f))
                    root.addView(Ui.button(this@ManagementActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_88fdfcc249),true){startActivity(Intent(this@ManagementActivity,CreateGroupActivity::class.java))})
                    groups.forEach{g->root.addView(Ui.entry(this@ManagementActivity,g.name,resources.getQuantityString(com.familyrecipes.android.R.plurals.social_member_count,g.memberCount?:1,g.memberCount?:1),com.familyrecipes.android.R.drawable.ic_ui_users){startActivity(Intent(this@ManagementActivity,GroupSettingsActivity::class.java).putExtra("group_id",g.id))})}
                } else {
                    root.addView(Ui.text(this@ManagementActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_3392783de7),15f))
                    root.addView(Ui.button(this@ManagementActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_36d00ad97b),true){startActivity(Intent(this@ManagementActivity,KitchenEditorActivity::class.java))})
                    val kitchens=Ui.data(ApiClient.getAccountService(account).getKitchens())
                    if(account!=com.familyrecipes.android.data.local.LocalStore.account){finish();return@launch}
                    kitchens.forEach{k->
                        val card=Ui.card(this@ManagementActivity)
                        card.addView(Ui.text(this@ManagementActivity,k.name,20f,true))
                        card.addView(Ui.text(this@ManagementActivity,getString(com.familyrecipes.android.R.string.kitchen_group_label,k.groupName.orEmpty()),15f))
                        card.addView(Ui.button(this@ManagementActivity,if(k.id==PreferenceManager.currentKitchenId)UiStrings.s(com.familyrecipes.android.R.string.ui_33f8952802) else UiStrings.s(com.familyrecipes.android.R.string.ui_bde123ec96)){
                            if(account!=com.familyrecipes.android.data.local.LocalStore.account){finish();return@button}
                            PreferenceManager.saveCurrentKitchen(k.id,k.name);PreferenceManager.currentGroupId=k.groupId
                            startActivity(Intent(this@ManagementActivity,com.familyrecipes.android.ui.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));finish()
                        })
                        if(groups.any{it.id==k.groupId && it.managerId==PreferenceManager.userId})card.addView(Ui.button(this@ManagementActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_932666cb18)){
                            startActivity(Intent(this@ManagementActivity,KitchenEditorActivity::class.java).putExtra("id",k.id).putExtra("group",k.groupId).putExtra("name",k.name))
                        })
                        root.addView(card)
                    }
                }
            }catch(e:CancellationException){throw e}catch(e:Exception){root.addView(Ui.button(this@ManagementActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_729a79fdf0)){load()})}
        }
    }
}

class KitchenEditorActivity : AppCompatActivity(){
    private val account=com.familyrecipes.android.data.local.LocalStore.account
    override fun onStart(){super.onStart();if(!PreferenceManager.isLoggedIn() || account!=com.familyrecipes.android.data.local.LocalStore.account)finish()}
    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        val root=Ui.box(this);setContentView(Ui.scroll(this,root))
        val id=intent.getLongExtra("id",0).takeIf{it>0}
        root.addView(Ui.back(this){finish()})
        root.addView(Ui.text(this,if(id==null)UiStrings.s(com.familyrecipes.android.R.string.ui_d670379f49) else UiStrings.s(com.familyrecipes.android.R.string.ui_7890ec1dfa),24f,true))
        val name=Ui.field(this,UiStrings.s(com.familyrecipes.android.R.string.ui_280da9de8d),state?.getString("name") ?: intent.getStringExtra("name").orEmpty()).apply{this.id=android.R.id.edit}
        root.addView(name)
        lifecycleScope.launch {try{
            val groups=Ui.data(ApiClient.getAccountService(account).getGroupsList()).filter{it.managerId==PreferenceManager.userId}
            if(account!=com.familyrecipes.android.data.local.LocalStore.account){finish();return@launch}
            if(groups.isEmpty()){root.addView(Ui.text(this@KitchenEditorActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_ea2b020b9e)));root.addView(Ui.button(this@KitchenEditorActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_528c6bb352)){startActivity(Intent(this@KitchenEditorActivity,CreateGroupActivity::class.java));finish()});return@launch}
            root.addView(Ui.text(this@KitchenEditorActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_2c6ff6f1da)))
            var selected=groups.indexOfFirst{it.id==intent.getLongExtra("group",0)}.coerceAtLeast(0)
            root.addView(Ui.select(this@KitchenEditorActivity,groups.map{it.name},selected){selected=it}.apply{isEnabled=id==null})
            val save=Ui.button(this@KitchenEditorActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_be5fbbe34c),true){}
            save.setOnClickListener {
                if(name.text.isNullOrBlank()){name.error=UiStrings.s(com.familyrecipes.android.R.string.ui_464069bf95);return@setOnClickListener}
                save.isEnabled=false
                lifecycleScope.launch{try{
                    val k=Kitchen(id,groups[selected].id,name.text.toString().trim())
                    if(id==null)Ui.data(ApiClient.getAccountService(account).createKitchen(k)) else Ui.data(ApiClient.getAccountService(account).renameKitchen(id,k))
                    if(account!=com.familyrecipes.android.data.local.LocalStore.account){finish();return@launch}
                    if(id==PreferenceManager.currentKitchenId)PreferenceManager.saveCurrentKitchen(id,k.name)
                    finish()
                }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@KitchenEditorActivity,e);save.isEnabled=true}}
            };root.addView(save)
        }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@KitchenEditorActivity,e);root.addView(Ui.button(this@KitchenEditorActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_64ca9bab92)){recreate()})}}
    }
    override fun onSaveInstanceState(out:Bundle){out.putString("name",findViewById<EditText>(android.R.id.edit)?.text.toString());super.onSaveInstanceState(out)}
}

class GroupSettingsActivity:AppCompatActivity(){
    private lateinit var root:LinearLayout
    private val groupId get()=intent.getLongExtra("group_id",0)
    private val account=com.familyrecipes.android.data.local.LocalStore.account
    private var request:kotlinx.coroutines.Job?=null
    private var savedName:String?=null
    private var savedAbout:String?=null
    private var editName:EditText?=null
    private var editAbout:EditText?=null
    override fun onCreate(state:Bundle?){super.onCreate(state);savedName=state?.getString("name");savedAbout=state?.getString("about");root=Ui.box(this);setContentView(Ui.scroll(this,root))}
    override fun onSaveInstanceState(out:Bundle){out.putString("name",editName?.text?.toString());out.putString("about",editAbout?.text?.toString());super.onSaveInstanceState(out)}
    override fun onResume(){super.onResume();load()}
    private fun s(id:Int)=getString(id)
    private fun load(){
        request?.cancel()
        root.removeAllViews();root.addView(Ui.back(this){finish()})
        root.addView(Ui.text(this,s(com.familyrecipes.android.R.string.social_group_settings),23f,true))
        request=lifecycleScope.launch{try{
            if(account!=com.familyrecipes.android.data.local.LocalStore.account){finish();return@launch}
            val group=Ui.data(ApiClient.getAccountService(account).getGroupDetail(groupId))
            if(account!=com.familyrecipes.android.data.local.LocalStore.account){finish();return@launch}
            com.familyrecipes.android.data.local.GroupCache.saveDetail(account,group)
            val manager=group.managerId==PreferenceManager.userId
            val name=Ui.field(this@GroupSettingsActivity,s(com.familyrecipes.android.R.string.social_group_name),savedName?:group.name).apply{isEnabled=manager}
            root.addView(name)
            val about=Ui.field(this@GroupSettingsActivity,s(com.familyrecipes.android.R.string.social_group_about),savedAbout?:group.description.orEmpty()).apply{isEnabled=manager}
            editName=name;editAbout=about;savedName=null;savedAbout=null
            if(manager || !group.description.isNullOrBlank())root.addView(about)
            if(manager)root.addView(Ui.button(this@GroupSettingsActivity,s(com.familyrecipes.android.R.string.social_save),true){
                if(name.text.isNullOrBlank() || name.text.toString().trim().length>100){name.error=s(com.familyrecipes.android.R.string.social_name_invalid);return@button}
                action{Ui.ok(ApiClient.getAccountService(account).updateGroup(groupId,mapOf("name" to name.text.toString().trim(),"description" to about.text.toString().trim())))}
            })
            val heading=Ui.row(this@GroupSettingsActivity)
            heading.addView(Ui.text(this@GroupSettingsActivity,"${s(com.familyrecipes.android.R.string.social_members)} · ${group.members.orEmpty().size}/${group.maxMembers?:10}",20f,true),LinearLayout.LayoutParams(0,-2,1f))
            heading.addView(Ui.button(this@GroupSettingsActivity,s(com.familyrecipes.android.R.string.social_add_members)){addMembers()});root.addView(heading)
            group.members.orEmpty().forEach{m->
                val user=m.userId?:return@forEach
                val label=m.nickname?.takeIf{it.isNotBlank()}?:m.user?.username?:user.toString()
                val row=Ui.row(this@GroupSettingsActivity).apply{minimumHeight=dp(64);setPadding(0,dp(6),0,dp(6))}
                row.addView(com.familyrecipes.android.ui.design.UserAvatar.create(this@GroupSettingsActivity,label,m.user?.avatar),LinearLayout.LayoutParams(dp(44),dp(44)))
                row.addView(Ui.text(this@GroupSettingsActivity,label+(if(user==group.managerId)" · ${s(com.familyrecipes.android.R.string.social_manager)}" else ""),17f).apply{setPadding(dp(12),0,0,0)},LinearLayout.LayoutParams(0,-2,1f))
                if(manager && user!=PreferenceManager.userId)row.addView(Ui.button(this@GroupSettingsActivity,"⋮"){
                    androidx.appcompat.app.AlertDialog.Builder(this@GroupSettingsActivity).setTitle(label)
                        .setItems(arrayOf(s(com.familyrecipes.android.R.string.social_transfer),s(com.familyrecipes.android.R.string.social_remove_member))){_,which->
                            if(which==0)confirm(s(com.familyrecipes.android.R.string.social_transfer),s(com.familyrecipes.android.R.string.social_transfer_confirm)){
                                Ui.ok(ApiClient.getAccountService(account).transferGroup(groupId,mapOf("userId" to user)))}
                            else confirm(s(com.familyrecipes.android.R.string.social_remove_member),s(com.familyrecipes.android.R.string.social_remove_confirm)){
                                Ui.ok(ApiClient.getAccountService(account).removeGroupMember(groupId,user))}
                        }.show()
                }.apply{contentDescription=s(com.familyrecipes.android.R.string.social_message_tools)},LinearLayout.LayoutParams(dp(52),dp(48)))
                root.addView(row)
            }
            root.addView(Ui.text(this@GroupSettingsActivity,s(com.familyrecipes.android.R.string.social_linked_kitchens),20f,true))
            val kitchens=Ui.data(ApiClient.getAccountService(account).getKitchens()).filter{it.groupId==groupId}
            if(account!=com.familyrecipes.android.data.local.LocalStore.account){finish();return@launch}
            kitchens.forEach{k->root.addView(Ui.text(this@GroupSettingsActivity,k.name,17f))}
            root.addView(Ui.text(this@GroupSettingsActivity,getString(com.familyrecipes.android.R.string.group_kitchen_relation),14f))
            root.addView(Ui.button(this@GroupSettingsActivity,s(if(manager)com.familyrecipes.android.R.string.social_dismiss else com.familyrecipes.android.R.string.social_leave)){
                confirm(s(if(manager)com.familyrecipes.android.R.string.social_dismiss else com.familyrecipes.android.R.string.social_leave),
                    s(if(manager)com.familyrecipes.android.R.string.social_dismiss_confirm else com.familyrecipes.android.R.string.social_leave_confirm)+(if(manager)"\n\n"+kitchens.joinToString("、"){it.name} else "")){
                    if(manager)Ui.ok(ApiClient.getAccountService(account).dismissGroup(groupId)) else Ui.ok(ApiClient.getAccountService(account).leaveGroup(groupId))
                    check(account==com.familyrecipes.android.data.local.LocalStore.account)
                    com.familyrecipes.android.data.local.GroupCache.remove(account,groupId)
                    com.familyrecipes.android.data.local.ChatNotifications.clearGroup(account,groupId)
                    if(PreferenceManager.currentGroupId==groupId){PreferenceManager.currentGroupId=null;PreferenceManager.saveCurrentKitchen(null,null)}
                    startActivity(Intent(this@GroupSettingsActivity,com.familyrecipes.android.ui.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));finish()
                }
            })
        }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@GroupSettingsActivity,e);root.addView(Ui.button(this@GroupSettingsActivity,s(com.familyrecipes.android.R.string.social_retry)){load()})}}
    }
    private fun addMembers(){lifecycleScope.launch{try{
        val latest=Ui.data(ApiClient.getAccountService(account).getGroupDetail(groupId))
        val members=latest.members.orEmpty().mapNotNull{it.userId}.toSet()
        val friends=Ui.data(ApiClient.getAccountService(account).getFriendsList()).filter{it.friendId!=null && it.friendId !in members}
        if(account!=com.familyrecipes.android.data.local.LocalStore.account){finish();return@launch}
        val capacity=(latest.maxMembers?:10)-members.size
        if(capacity<=0){Toast.makeText(this@GroupSettingsActivity,getString(com.familyrecipes.android.R.string.group_full),Toast.LENGTH_LONG).show();return@launch}
        if(friends.isEmpty()){
            androidx.appcompat.app.AlertDialog.Builder(this@GroupSettingsActivity).setMessage(s(com.familyrecipes.android.R.string.social_no_friends))
                .setPositiveButton(com.familyrecipes.android.R.string.social_manage_friends){_,_->startActivity(Intent(this@GroupSettingsActivity,com.familyrecipes.android.ui.social.FriendsListActivity::class.java))}
                .setNegativeButton(com.familyrecipes.android.R.string.social_cancel,null).show();return@launch
        }
        val selected=linkedSetOf<Long>()
        val dialog=androidx.appcompat.app.AlertDialog.Builder(this@GroupSettingsActivity)
            .setTitle(getString(com.familyrecipes.android.R.string.social_member_limit,capacity))
            .setMultiChoiceItems(friends.map{it.nickname?.takeIf(String::isNotBlank)?:it.friend?.username?:it.friendId.toString()}.toTypedArray(),BooleanArray(friends.size)){d,i,checked->
                if(checked && selected.size>=capacity)(d as androidx.appcompat.app.AlertDialog).listView.setItemChecked(i,false)
                else if(checked)selected.add(friends[i].friendId!!) else selected.remove(friends[i].friendId!!)
            }.setNegativeButton(com.familyrecipes.android.R.string.social_cancel,null).setPositiveButton(com.familyrecipes.android.R.string.social_add_members,null).create()
        dialog.setOnShowListener{dialog.getButton(-1).setOnClickListener{
            if(selected.isEmpty())return@setOnClickListener
            dialog.getButton(-1).isEnabled=false
            lifecycleScope.launch{try{Ui.ok(ApiClient.getAccountService(account).addGroupMembers(groupId,mapOf("userIds" to selected.toList())));dialog.dismiss();load()}
                catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@GroupSettingsActivity,e);dialog.getButton(-1).isEnabled=true}}
        }};dialog.show()
    }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@GroupSettingsActivity,e)}}}
    private fun action(block:suspend()->Unit){lifecycleScope.launch{try{check(account==com.familyrecipes.android.data.local.LocalStore.account);block();if(!isFinishing)load()}catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@GroupSettingsActivity,e)}}}
    private fun confirm(title:String,message:String,block:suspend()->Unit){
        val dialog=androidx.appcompat.app.AlertDialog.Builder(this).setTitle(title).setMessage(message).setNegativeButton(com.familyrecipes.android.R.string.social_cancel,null).setPositiveButton(com.familyrecipes.android.R.string.social_confirm,null).create()
        dialog.setOnShowListener{dialog.getButton(-1).setOnClickListener{dialog.getButton(-1).isEnabled=false;lifecycleScope.launch{try{check(account==com.familyrecipes.android.data.local.LocalStore.account);block();dialog.dismiss();if(!isFinishing)load()}catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@GroupSettingsActivity,e);dialog.getButton(-1).isEnabled=true}}}};dialog.show()
    }
    private fun dp(n:Int)=(resources.displayMetrics.density*n).toInt()
}
