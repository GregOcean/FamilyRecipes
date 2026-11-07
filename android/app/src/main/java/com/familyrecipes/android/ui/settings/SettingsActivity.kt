package com.familyrecipes.android.ui.settings

import com.familyrecipes.android.ui.design.UiStrings
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.familyrecipes.android.data.model.ReminderSetting
import com.familyrecipes.android.data.local.PreferenceManager
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.design.Ui
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class SettingsActivity: AppCompatActivity(){
    private var reminderHour=17
    private var reminderMinute=0
    private var daysField:EditText?=null
    private var enabledField:CheckBox?=null
    override fun onSaveInstanceState(out:Bundle){
        daysField?.let{out.putString("days",it.text.toString())}
        enabledField?.let{out.putBoolean("enabled",it.isChecked)}
        out.putInt("hour",reminderHour);out.putInt("minute",reminderMinute)
        super.onSaveInstanceState(out)
    }
    private val permission=registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()){}
    override fun onCreate(state:Bundle?){super.onCreate(state)
        val root=Ui.box(this);setContentView(Ui.scroll(this,root))
        root.addView(Ui.back(this){finish()})
        root.addView(Ui.text(this,UiStrings.s(com.familyrecipes.android.R.string.ui_e366ccf155),24f,true))
        root.addView(Ui.text(this,getString(com.familyrecipes.android.R.string.appearance_title),20f,true))
        val appearancePrefs=getSharedPreferences("appearance",MODE_PRIVATE)
        val modes=listOf(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,AppCompatDelegate.MODE_NIGHT_NO,AppCompatDelegate.MODE_NIGHT_YES)
        val selectedMode=appearancePrefs.getInt("mode",AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        root.addView(Ui.select(this,resources.getStringArray(com.familyrecipes.android.R.array.appearance_modes).toList(),modes.indexOf(selectedMode).coerceAtLeast(0)){index ->
            if(modes[index]!=appearancePrefs.getInt("mode",AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)) {
                appearancePrefs.edit().putInt("mode",modes[index]).apply()
                AppCompatDelegate.setDefaultNightMode(modes[index])
            }
        })
        root.addView(Ui.text(this,UiStrings.s(com.familyrecipes.android.R.string.ui_688e2fa33f),20f,true))
        val languages=listOf("简体中文","English")
        val current=if(resources.configuration.locales[0].language=="en")1 else 0
        root.addView(Ui.select(this,languages,current){index->if(index!=current)AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(if(index==1)"en" else "zh-CN"))})
        root.addView(Ui.text(this,UiStrings.s(com.familyrecipes.android.R.string.ui_47267ab387),14f))
        root.addView(Ui.text(this,UiStrings.s(com.familyrecipes.android.R.string.ui_974d3e30d5),20f,true))
        root.addView(Ui.button(this,UiStrings.s(com.familyrecipes.android.R.string.ui_3d1fffd959)){startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))})
        root.addView(Ui.text(this,UiStrings.s(com.familyrecipes.android.R.string.ui_0054ea0793),14f))
        root.addView(Ui.text(this,UiStrings.s(com.familyrecipes.android.R.string.ui_c9fb762fc5),20f,true))
        root.addView(Ui.button(this,UiStrings.s(com.familyrecipes.android.R.string.ui_0439b015f3)){
            startActivity(if(android.os.Build.VERSION.SDK_INT>=26)Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName) else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:$packageName")))
        })
        val form=Ui.card(this);root.addView(form)
        fun load(){form.removeAllViews();form.addView(Ui.text(this,UiStrings.s(com.familyrecipes.android.R.string.ui_fb4ca1cf1b)));lifecycleScope.launch{try{
            val setting=Ui.data(ApiClient.getService().getReminderSetting())
            form.removeAllViews()
            val enabled=CheckBox(this@SettingsActivity).apply{text=UiStrings.s(com.familyrecipes.android.R.string.ui_12f0a5812e);isChecked=state?.getBoolean("enabled")?:setting.enabled;minHeight=(48*resources.displayMetrics.density).toInt()}
            val days=Ui.field(this@SettingsActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_229e171f87),state?.getString("days")?:setting.daysBeforeExpiry.toString()).apply{inputType=android.text.InputType.TYPE_CLASS_NUMBER}
            daysField=days;enabledField=enabled
            reminderHour=state?.getInt("hour")?:setting.reminderTime?.take(2)?.toIntOrNull()?:17
            reminderMinute=state?.getInt("minute")?:setting.reminderTime?.substring(3,5)?.toIntOrNull()?:0
            fun timeLabel()=String.format(java.util.Locale.ROOT,"%02d:%02d",reminderHour,reminderMinute)
            val time=Ui.button(this@SettingsActivity,timeLabel()){}
            time.setOnClickListener{android.app.TimePickerDialog(this@SettingsActivity,{_,h,m->reminderHour=h;reminderMinute=m;time.text=timeLabel()},reminderHour,reminderMinute,true).show()}

            form.addView(enabled);form.addView(Ui.text(this@SettingsActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_229e171f87)));form.addView(days);form.addView(time)
            form.addView(Ui.text(this@SettingsActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_9e20cc1715),14f))
            val save=Ui.button(this@SettingsActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_bfa9c9ba82),true){}
            save.setOnClickListener{
                val count=days.text.toString().toIntOrNull();if(count==null||count !in 0..365){days.error=UiStrings.s(com.familyrecipes.android.R.string.ui_6983955277);return@setOnClickListener}
                save.isEnabled=false
                lifecycleScope.launch{try{
                    Ui.ok(ApiClient.getService().updateReminderSetting(setting.copy(daysBeforeExpiry=count,enabled=enabled.isChecked,reminderTime=String.format(java.util.Locale.ROOT,"%02d:%02d:00",reminderHour,reminderMinute))))
                    if(enabled.isChecked && android.os.Build.VERSION.SDK_INT>=33)permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    Toast.makeText(this@SettingsActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_669e3a20e8),Toast.LENGTH_SHORT).show()
                }catch(e:CancellationException){throw e}catch(e:Exception){Ui.toast(this@SettingsActivity,e)}finally{save.isEnabled=true}}
            };form.addView(save)
        }catch(e:CancellationException){throw e}catch(e:Exception){form.removeAllViews();form.addView(Ui.button(this@SettingsActivity,UiStrings.s(com.familyrecipes.android.R.string.ui_729a79fdf0)){load()})}}}
        load()
    }
}
