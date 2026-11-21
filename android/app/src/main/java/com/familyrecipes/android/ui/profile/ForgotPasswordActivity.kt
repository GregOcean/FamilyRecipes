package com.familyrecipes.android.ui.profile

import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.familyrecipes.android.R
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.design.Ui
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var email: EditText
    private lateinit var code: EditText
    private lateinit var status: TextView
    private var until = 0L
    private var timer: CountDownTimer? = null
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val root=Ui.box(this);setContentView(Ui.scroll(this,root))
        root.addView(Ui.back(this){finish()})
        root.addView(Ui.text(this,getString(R.string.reset_title),24f,true))
        root.addView(Ui.text(this,getString(R.string.reset_intro),16f))
        email=Ui.field(this,getString(R.string.reset_email),state?.getString("email")?:intent.getStringExtra("email").orEmpty()).apply { inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
        root.addView(email)
        val send=Ui.button(this,getString(R.string.reset_send)){}
        root.addView(send)
        code=Ui.field(this,getString(R.string.reset_code),state?.getString("code").orEmpty()).apply {inputType=InputType.TYPE_CLASS_NUMBER;filters=arrayOf(android.text.InputFilter.LengthFilter(6))};root.addView(code)
        val password=Ui.field(this,getString(R.string.reset_password)).apply{inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD;isSaveEnabled=false};root.addView(password)
        val confirm=Ui.field(this,getString(R.string.reset_confirm)).apply{inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD;isSaveEnabled=false};root.addView(confirm)
        val submit=Ui.button(this,getString(R.string.reset_submit)){};root.addView(submit)
        status=Ui.text(this,"",16f);status.accessibilityLiveRegion=android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE;root.addView(status)
        fun countdown() {
            timer?.cancel();val remaining=until-System.currentTimeMillis()
            if(remaining<=0){send.isEnabled=true;send.text=getString(R.string.reset_send);return}
            send.isEnabled=false
            timer=object:CountDownTimer(remaining,1000){override fun onTick(ms:Long){send.text=getString(R.string.reset_seconds,(ms+999)/1000)};override fun onFinish(){send.isEnabled=true;send.text=getString(R.string.reset_send)}}.start()
        }
        until=state?.getLong("until")?:0L;countdown()
        send.setOnClickListener {
            val address=email.text.toString().trim()
            if(!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()){email.error=getString(R.string.reset_email_error);return@setOnClickListener}
            send.isEnabled=false;status.text=""
            lifecycleScope.launch {
                try{Ui.data(ApiClient.getService().sendPasswordResetCode(mapOf("email" to address)));until=System.currentTimeMillis()+60000;status.text=getString(R.string.reset_sent)}
                catch(e:CancellationException){throw e}catch(e:Exception){status.text=e.message?:getString(R.string.reset_failed)}
                finally{countdown()}
            }
        }
        submit.setOnClickListener {
            val value=password.text.toString()
            if(value.length<8 || value.toByteArray(Charsets.UTF_8).size>72){password.error=getString(R.string.reset_password_error);return@setOnClickListener}
            if(value!=confirm.text.toString()){confirm.error=getString(R.string.reset_mismatch);return@setOnClickListener}
            if(!code.text.toString().matches(Regex("[0-9]{6}"))){code.error=getString(R.string.reset_code_error);return@setOnClickListener}
            submit.isEnabled=false;status.text=""
            lifecycleScope.launch {
                try {
                    val response=ApiClient.getService().resetPassword(mapOf("email" to email.text.toString().trim(),"code" to code.text.toString(),"password" to value))
                    if(!response.isSuccessful || response.body()?.code!=200)throw IllegalStateException(com.familyrecipes.android.data.remote.ApiErrors.message(response,getString(R.string.reset_failed)))
                    timer?.cancel();com.familyrecipes.android.data.local.PreferenceManager.clearUserData()
                    password.text.clear();confirm.text.clear();send.isEnabled=false
                    status.text=getString(R.string.reset_success)
                    root.addView(Ui.button(this@ForgotPasswordActivity,getString(R.string.reset_return)){finish()})
                }catch(e:CancellationException){throw e}catch(e:Exception){status.text=e.message?:getString(R.string.reset_failed);submit.isEnabled=true}
            }
        }
    }
    override fun onSaveInstanceState(out:Bundle){out.putString("email",email.text.toString());out.putString("code",code.text.toString());out.putLong("until",until);super.onSaveInstanceState(out)}
    override fun onDestroy(){timer?.cancel();super.onDestroy()}
}
