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
    private var groupsMode=false
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        groupsMode=intent.getBooleanExtra("groups",false)
        root=Ui.box(this);setContentView(Ui.scroll(this,root))
    }
    override fun onResume(){super.onResume();load()}
    private fun load(){
        root.removeAllViews()
        root.addView(Ui.back(this@ManagementActivity){finish()})
        root.addView(Ui.text(this@ManagementActivity,if(groupsMode)UiStrings.s(com.familyrecipes.android.R.string.ui_47fdc41702) else UiStrings.s(com.familyrecipes.android.R.string.ui_d46695d56e),24f,true))
        lifecycleScope.launch {
            try {
                val groups=Ui.data(ApiClient.getService().getGroupsList())
                if(groupsMode){
                    root.addView(Ui.text(this@ManagementActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_117723f7be),15f))
                    root.addView(Ui.button(this@ManagementActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_88fdfcc249),true){startActivity(Intent(this@ManagementActivity,CreateGroupActivity::class.java))})
                    groups.forEach{g->root.addView(Ui.entry(this@ManagementActivity,g.name,UiStrings.f(com.familyrecipes.android.R.string.member_count, g.memberCount?:1),com.familyrecipes.android.R.drawable.ic_ui_users){startActivity(Intent(this@ManagementActivity,GroupSettingsActivity::class.java).putExtra("group_id",g.id))})}
                } else {
                    root.addView(Ui.text(this@ManagementActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_3392783de7),15f))
                    root.addView(Ui.button(this@ManagementActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_36d00ad97b),true){startActivity(Intent(this@ManagementActivity,KitchenEditorActivity::class.java))})
                    Ui.data(ApiClient.getService().getKitchens()).forEach{k->
                        val card=Ui.card(this@ManagementActivity)
                        card.addView(Ui.text(this@ManagementActivity,k.name,20f,true))
                        card.addView(Ui.text(this@ManagementActivity,k.groupName.orEmpty(),15f))
                        card.addView(Ui.button(this@ManagementActivity,if(k.id==PreferenceManager.currentKitchenId)UiStrings.s(com.familyrecipes.android.R.string.ui_33f8952802) else UiStrings.s(com.familyrecipes.android.R.string.ui_bde123ec96)){
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
    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        val root=Ui.box(this);setContentView(Ui.scroll(this,root))
        val id=intent.getLongExtra("id",0).takeIf{it>0}
        root.addView(Ui.back(this){finish()})
        root.addView(Ui.text(this,if(id==null)UiStrings.s(com.familyrecipes.android.R.string.ui_d670379f49) else UiStrings.s(com.familyrecipes.android.R.string.ui_7890ec1dfa),24f,true))
        val name=Ui.field(this,UiStrings.s(com.familyrecipes.android.R.string.ui_280da9de8d),state?.getString("name") ?: intent.getStringExtra("name").orEmpty()).apply{this.id=android.R.id.edit}
        root.addView(name)
        lifecycleScope.launch {try{
            val groups=Ui.data(ApiClient.getService().getGroupsList()).filter{it.managerId==PreferenceManager.userId}
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
                    if(id==null)Ui.data(ApiClient.getService().createKitchen(k)) else Ui.data(ApiClient.getService().renameKitchen(id,k))
                    if(id==PreferenceManager.currentKitchenId)PreferenceManager.saveCurrentKitchen(id,k.name)
                    finish()
                }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@KitchenEditorActivity,e);save.isEnabled=true}}
            };root.addView(save)
        }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@KitchenEditorActivity,e);root.addView(Ui.button(this@KitchenEditorActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_64ca9bab92)){recreate()})}}
    }
    override fun onSaveInstanceState(out:Bundle){out.putString("name",findViewById<EditText>(android.R.id.edit)?.text.toString());super.onSaveInstanceState(out)}
}

class GroupSettingsActivity:AppCompatActivity(){
    override fun onCreate(state:Bundle?){super.onCreate(state);val root=Ui.box(this);setContentView(Ui.scroll(this,root));root.addView(Ui.back(this){finish()})
        lifecycleScope.launch{try{
            val group=Ui.data(ApiClient.getService().getGroupDetail(intent.getLongExtra("group_id",0)))
            val id=group.id?:return@launch
            root.addView(Ui.text(this@GroupSettingsActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_47fdc41702),24f,true))
            val name=Ui.field(this@GroupSettingsActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_acb4c1a177),state?.getString("name")?:group.name).apply{this.id=android.R.id.edit;isEnabled=group.managerId==PreferenceManager.userId};root.addView(name)
            if(name.isEnabled)root.addView(Ui.button(this@GroupSettingsActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_160f1630cb),true){lifecycleScope.launch{try{Ui.ok(ApiClient.getService().updateGroup(id,mapOf("name" to name.text.toString().trim(),"description" to group.description.orEmpty())));finish()}catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@GroupSettingsActivity,e)}}})
            root.addView(Ui.button(this@GroupSettingsActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_270de4e4d2)){startActivity(Intent(this@GroupSettingsActivity,GroupChatActivity::class.java).putExtra("group_id",id).putExtra("group_name",group.name).putExtra("member_count",group.memberCount?:1))})
            root.addView(Ui.text(this@GroupSettingsActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_ab5dea2979),21f,true))
            group.members.orEmpty().forEach{m->root.addView(Ui.text(this@GroupSettingsActivity,(m.user?.username?:m.nickname?:m.userId.toString())+if(m.userId==group.managerId)UiStrings.s(com.familyrecipes.android.R.string.ui_a804fa0231) else ""))}
            root.addView(Ui.text(this@GroupSettingsActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_cf4324dcbc),21f,true))
            Ui.data(ApiClient.getService().getKitchens()).filter{it.groupId==id}.forEach{k->root.addView(Ui.text(this@GroupSettingsActivity,k.name))}
        }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@GroupSettingsActivity,e);root.addView(Ui.button(this@GroupSettingsActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_64ca9bab92)){recreate()})}}
    }
    override fun onSaveInstanceState(out:Bundle){out.putString("name",findViewById<EditText>(android.R.id.edit)?.text?.toString().orEmpty());super.onSaveInstanceState(out)}
}
