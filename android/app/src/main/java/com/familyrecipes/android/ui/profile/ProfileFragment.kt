package com.familyrecipes.android.ui.profile

import com.familyrecipes.android.ui.design.UiStrings
import androidx.core.widget.doAfterTextChanged
import com.familyrecipes.android.ui.design.Ui
import com.familyrecipes.android.ui.design.Ui.dp
import com.familyrecipes.android.R
import android.widget.*
import android.content.Intent
import com.familyrecipes.android.ui.recipe.RecipeCollectionActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.familyrecipes.android.data.local.PreferenceManager
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.databinding.FragmentProfileBinding
import kotlinx.coroutines.launch

/**
 * 我的Fragment - 整合登录和个人信息
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    // 登录/注册模式标志
    private var isLoginMode = true
    private var codeRequestPending=false
    private var codeCooldown:kotlinx.coroutines.Job?=null
    private val resendAt=mutableMapOf<String,Long>()
    private fun emailKey()=binding.etEmail.text.toString().trim().lowercase(java.util.Locale.ROOT)
    private fun refreshCodeButton() {
        codeCooldown?.cancel()
        codeCooldown=viewLifecycleOwner.lifecycleScope.launch {
            while(_binding!=null) {
                val seconds=((resendAt[emailKey()] ?: 0L)-System.currentTimeMillis()+999)/1000
                binding.btnSendCode.isEnabled=!codeRequestPending && seconds<=0
                binding.btnSendCode.text=when {
                    codeRequestPending -> UiStrings.s(com.familyrecipes.android.R.string.ui_0881570719)
                    seconds>0 -> UiStrings.f(com.familyrecipes.android.R.string.resend_seconds, seconds)
                    else -> UiStrings.s(com.familyrecipes.android.R.string.ui_d369f43aac)
                }
                if(seconds<=0)break
                kotlinx.coroutines.delay(1000)
            }
        }
    }
    private fun requestRegistrationCode() {
        val email=emailKey()
        if(email.length>100 || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error=UiStrings.s(com.familyrecipes.android.R.string.ui_b45b4ed55a);return
        }
        codeRequestPending=true;refreshCodeButton()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response=ApiClient.getService().sendRegistrationCode(mapOf("email" to email))
                if(response.isSuccessful && response.body()?.code==200) {
                    resendAt[email]=System.currentTimeMillis()+60000
                    if(email==emailKey()) binding.etVerificationCode.setText("")
                    android.widget.Toast.makeText(context,UiStrings.s(com.familyrecipes.android.R.string.ui_a568feaf47),android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context,com.familyrecipes.android.data.remote.ApiErrors.message(response,UiStrings.s(com.familyrecipes.android.R.string.ui_152bbcb262)),android.widget.Toast.LENGTH_LONG).show()
                }
            } catch(e:kotlinx.coroutines.CancellationException) { throw e }
              catch(_:Exception) { android.widget.Toast.makeText(context,UiStrings.s(com.familyrecipes.android.R.string.ui_c08c2f36d2),android.widget.Toast.LENGTH_LONG).show() }
            finally { codeRequestPending=false;if(_binding!=null)refreshCodeButton() }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isLoginMode=savedInstanceState?.getBoolean("login_mode",true) ?: true
        binding.btnSendCode.setOnClickListener { requestRegistrationCode() }
        binding.etEmail.doAfterTextChanged { binding.etVerificationCode.setText("");refreshCodeButton() }
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        if (PreferenceManager.isLoggedIn()) {
            // 已登录，显示个人信息页面
            showProfilePage()
        } else {
            // 未登录只切换当前视图，不能重建 Activity，否则生命周期会无限循环。
            showLoginPage()
        }
    }

    private fun showLoginPage() {
        binding.layoutLogin.visibility = View.VISIBLE
        binding.layoutProfile.visibility = View.GONE
        
        renderLoginMode()
        setupLoginListeners()
    }

    private var quickLinksAdded=false
    private var stats=mutableMapOf<String,TextView>()
    private fun openRecipes(origin:String="all",known:Boolean=false) {
        startActivity(Intent(requireContext(),RecipeCollectionActivity::class.java).putExtra("origin",origin).putExtra("known",known))
    }
    private fun openMeals(tab:String) {
        val host=activity as? com.familyrecipes.android.ui.MainActivity ?: return
        host.showPage("meals");host.supportFragmentManager.executePendingTransactions()
        (host.supportFragmentManager.findFragmentByTag("main:meals") as? com.familyrecipes.android.ui.meal.MealFragment)?.showTab(tab)
    }
    private fun showProfilePage() {
        binding.layoutLogin.visibility=View.GONE
        binding.layoutProfile.visibility=View.VISIBLE
        if(quickLinksAdded)return
        quickLinksAdded=true
        val c=requireContext()
        val root=Ui.box(c)
        binding.layoutProfile.removeAllViews();binding.layoutProfile.addView(root)
        val identity=Ui.row(c)
        identity.addView(com.familyrecipes.android.ui.design.UserAvatar.current(c, viewLifecycleOwner),LinearLayout.LayoutParams(c.dp(76),c.dp(76)))
        val bio=Ui.box(c,0).apply{setPadding(c.dp(16),0,0,0)}
        bio.addView(Ui.text(c,PreferenceManager.userName ?: UiStrings.s(com.familyrecipes.android.R.string.ui_2878420820),25f,true))
        bio.addView(Ui.text(c,PreferenceManager.userEmail.orEmpty(),14f).apply{setTextColor(Ui.muted)})
        identity.addView(bio,LinearLayout.LayoutParams(0,-2,1f));root.addView(identity)
        val numbers=Ui.card(c).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,c.dp(14),0,c.dp(14))}
        listOf("created" to UiStrings.s(com.familyrecipes.android.R.string.ui_2f8ba39ad4),"favorites" to UiStrings.s(com.familyrecipes.android.R.string.ui_975ff67ee1),"known" to UiStrings.s(com.familyrecipes.android.R.string.ui_d3617a614a)).forEach{(key,label)->
            val item=Ui.box(c,0).apply{setBackgroundColor(android.graphics.Color.TRANSPARENT);gravity=android.view.Gravity.CENTER;minimumHeight=c.dp(72);isFocusable=true}
            val count=Ui.text(c,"—",25f,true).apply{gravity=android.view.Gravity.CENTER;setTextColor(Ui.blue);setPadding(0,0,0,0)}
            stats[key]=count;item.addView(count);item.addView(Ui.text(c,label,15f).apply{gravity=android.view.Gravity.CENTER})
            item.setOnClickListener{openRecipes(if(key=="known")"all" else key,key=="known")}
            numbers.addView(item,LinearLayout.LayoutParams(0,-2,1f))
        }
        root.addView(numbers,LinearLayout.LayoutParams(-1,-2).apply{topMargin=c.dp(20);bottomMargin=c.dp(12)})
        val shortcuts=listOf(Triple(UiStrings.s(com.familyrecipes.android.R.string.ui_a690f4d30e),R.drawable.ic_ui_order,"order"),Triple(UiStrings.s(com.familyrecipes.android.R.string.ui_2869f51718),R.drawable.ic_ui_prep,"prep"),Triple(UiStrings.s(com.familyrecipes.android.R.string.ui_ba330b9fe4),R.drawable.ic_ui_draft,"draft"),Triple(UiStrings.s(com.familyrecipes.android.R.string.ui_11d0c6e5bc),R.drawable.ic_ui_users,"shared"))
        shortcuts.chunked(2).forEach{pair->val row=Ui.row(c);pair.forEach{(label,icon,key)->row.addView(Ui.tile(c,label,icon,if(key=="order")Ui.warm else if(key=="draft")Ui.purple else Ui.pale){if(key=="shared")androidx.appcompat.app.AlertDialog.Builder(c).setTitle(UiStrings.s(com.familyrecipes.android.R.string.ui_11d0c6e5bc)).setItems(arrayOf(UiStrings.s(com.familyrecipes.android.R.string.ui_7f649e7630),UiStrings.s(com.familyrecipes.android.R.string.ui_3138b7d701))){_,which->if(which==0)openRecipes("shared") else openMeals("prep")}.show() else if(key=="draft")startActivity(Intent(c,com.familyrecipes.android.ui.recipe.DraftsActivity::class.java)) else openMeals(key)},LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(c.dp(4),c.dp(4),c.dp(4),c.dp(4))})};root.addView(row)}
        root.addView(Ui.text(c,UiStrings.s(com.familyrecipes.android.R.string.ui_eddeaaf2ee),21f,true))
        val kitchens=Ui.box(c,0);root.addView(kitchens)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val rows=Ui.data(ApiClient.getService().getKitchens())
                rows.forEach{k->kitchens.addView(Ui.entry(c,k.name,if(k.id==PreferenceManager.currentKitchenId)UiStrings.s(com.familyrecipes.android.R.string.ui_d2d6ba5b26) else UiStrings.s(com.familyrecipes.android.R.string.ui_c018eeb2d2),R.drawable.ic_ui_kitchen){PreferenceManager.saveCurrentKitchen(k.id,k.name);PreferenceManager.currentGroupId=k.groupId;(activity as? com.familyrecipes.android.ui.MainActivity)?.showPage("stock");requireActivity().recreate()})}
                if(rows.isEmpty())kitchens.addView(Ui.text(c,UiStrings.s(com.familyrecipes.android.R.string.ui_e22e0575cb)))
            }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(_:Exception){kitchens.addView(Ui.button(c,UiStrings.s(com.familyrecipes.android.R.string.ui_867f270c2e)){quickLinksAdded=false;showProfilePage()})}
        }
        root.addView(Ui.text(c,UiStrings.s(com.familyrecipes.android.R.string.ui_4fa105b9e2),20f,true))
        root.addView(Ui.entry(c,UiStrings.s(com.familyrecipes.android.R.string.ui_5ac95e3cdd),UiStrings.s(com.familyrecipes.android.R.string.ui_f3a4f9ea11),R.drawable.ic_clock_filled){startActivity(Intent(c,com.familyrecipes.android.ui.recipe.RecipeHistoryActivity::class.java))})
        root.addView(Ui.entry(c,UiStrings.s(com.familyrecipes.android.R.string.ui_e366ccf155),UiStrings.s(com.familyrecipes.android.R.string.ui_c86435dc69),R.drawable.ic_settings){startActivity(Intent(c,com.familyrecipes.android.ui.settings.SettingsActivity::class.java))})
        root.addView(Ui.entry(c,UiStrings.s(com.familyrecipes.android.R.string.ui_4a86cd505e),UiStrings.s(com.familyrecipes.android.R.string.ui_8b4169e947),R.drawable.ic_qr_code){startActivity(Intent(c,com.familyrecipes.android.ui.social.MyQRCodeActivity::class.java))})
        root.addView(Ui.entry(c,UiStrings.s(com.familyrecipes.android.R.string.ui_c3af9f8f1d),UiStrings.s(com.familyrecipes.android.R.string.ui_a1d036fe76),R.drawable.ic_ui_draft){showTrash()})
        root.addView(Ui.button(c,UiStrings.s(com.familyrecipes.android.R.string.ui_44efd179aa)){
            PreferenceManager.clearUserData()
            startActivity(Intent(c,com.familyrecipes.android.ui.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        })
        loadRecipeCounts()
    }

    private fun setupLoginListeners() {
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(requireContext(),ForgotPasswordActivity::class.java).putExtra("email",binding.etEmail.text.toString().trim()))
        }

        // 登录按钮
        binding.btnLogin.setOnClickListener {
            if (isLoginMode) {
                performLogin()
            } else {
                performRegister()
            }
        }

        // 切换登录/注册
        binding.tvSwitchMode.setOnClickListener {
            switchMode()
        }
    }
    
    /**
     * 切换登录/注册模式
     */
    private fun switchMode() {
        isLoginMode = !isLoginMode
        renderLoginMode()
    }
    private fun renderLoginMode() {
        binding.layoutVerification.visibility=if(isLoginMode) View.GONE else View.VISIBLE
        refreshCodeButton()
        if (isLoginMode) {
            binding.layoutUsername.visibility = View.GONE
            binding.btnLogin.text = UiStrings.s(com.familyrecipes.android.R.string.ui_402d19e50f)
            binding.tvSwitchMode.text = UiStrings.s(com.familyrecipes.android.R.string.ui_d8f9fa703c)
        } else {
            binding.layoutUsername.visibility = View.VISIBLE
            binding.btnLogin.text = UiStrings.s(com.familyrecipes.android.R.string.ui_fe0e8bf768)
            binding.tvSwitchMode.text = UiStrings.s(com.familyrecipes.android.R.string.ui_7c1277e4fd)
        }
    }

    override fun onSaveInstanceState(outState:Bundle) {
        outState.putBoolean("login_mode",isLoginMode)
        super.onSaveInstanceState(outState)
    }

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            android.widget.Toast.makeText(context, UiStrings.s(com.familyrecipes.android.R.string.ui_fe142a5429), android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnLogin.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = mapOf("email" to email, "password" to password)
                val response = ApiClient.getService().login(request)

                if (response.isSuccessful && response.body()?.code == 200) {
                    val loginData = response.body()?.data
                    if (loginData != null) {
                        PreferenceManager.saveLoginInfo(
                            loginData.token,
                            loginData.user.id,
                            loginData.user.email,
                            loginData.user.username
                        )
                        showProfilePage()
                        (activity as? com.familyrecipes.android.ui.MainActivity)?.initializeKitchen()
                        com.familyrecipes.android.util.AuthUtil.completeLogin(requireActivity())
                    }
                } else {
                    android.widget.Toast.makeText(context, com.familyrecipes.android.data.remote.ApiErrors.message(response, UiStrings.s(com.familyrecipes.android.R.string.ui_b6076a055f)), android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, UiStrings.s(com.familyrecipes.android.R.string.ui_0bffc62bb3), android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                _binding?.btnLogin?.isEnabled = true
            }
        }
    }

    private fun performRegister() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val username = binding.etUsername.text.toString().trim().ifEmpty {
            // 生成随机用户名：家肴用户_<5位随机字符>
            getString(R.string.review_default_username,generateRandomString(5))
        }

        binding.etEmail.error = null
        binding.etPassword.error = null
        binding.etUsername.error = null
        if (email.length > 100 || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = UiStrings.s(com.familyrecipes.android.R.string.ui_b45b4ed55a)
            binding.etEmail.requestFocus()
            return
        }
        com.familyrecipes.android.util.PasswordRules.registrationError(password)?.let { error ->
            binding.etPassword.error = error
            binding.etPassword.requestFocus()
            return
        }
        if (username.length > 50) {
            binding.etUsername.error = UiStrings.s(com.familyrecipes.android.R.string.ui_8a56d0cff6)
            binding.etUsername.requestFocus()
            return
        }

        val code=binding.etVerificationCode.text.toString().trim()
        if(!code.matches(Regex("[0-9]{6}"))) {
            binding.etVerificationCode.error=UiStrings.s(com.familyrecipes.android.R.string.ui_8ac9af261b)
            binding.etVerificationCode.requestFocus()
            return
        }
        binding.btnLogin.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = mapOf("email" to email, "password" to password, "username" to username, "code" to code)
                val response = ApiClient.getService().register(request)

                if (response.isSuccessful && response.body()?.code == 200) {
                    android.widget.Toast.makeText(context, UiStrings.s(com.familyrecipes.android.R.string.ui_784c4de2e4), android.widget.Toast.LENGTH_SHORT).show()
                    
                    // 注册成功后自动登录
                    performLogin()
                } else {
                    android.widget.Toast.makeText(context, com.familyrecipes.android.data.remote.ApiErrors.message(response, UiStrings.s(com.familyrecipes.android.R.string.ui_1c56d8c5aa)), android.widget.Toast.LENGTH_SHORT).show()
                    binding.btnLogin.isEnabled = true
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, Ui.errorMessage(requireContext(),e), android.widget.Toast.LENGTH_SHORT).show()
                binding.btnLogin.isEnabled = true
            }
        }
    }

    private fun loadRecipeCounts() {
        stats.forEach{(key,view)->viewLifecycleOwner.lifecycleScope.launch {
            try {
                val page=Ui.data(ApiClient.getService().browseRecipes(PreferenceManager.currentKitchenId,scope="mine",origin=if(key=="known")"all" else key,known=key=="known"))
                view.text=page.total.toString()
            }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(_:Exception){view.text=UiStrings.s(com.familyrecipes.android.R.string.ui_132c5cdcce);view.setOnClickListener{loadRecipeCounts()}}
        }}
    }

    private fun generateRandomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    override fun onResume() {
        super.onResume()
        val hadProfile=quickLinksAdded
        checkLoginStatus()
        if(hadProfile && PreferenceManager.isLoggedIn())loadRecipeCounts()
    }

    override fun onHiddenChanged(hidden:Boolean) {
        super.onHiddenChanged(hidden)
        if(!hidden && _binding!=null){quickLinksAdded=false;stats.clear();checkLoginStatus()}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        quickLinksAdded=false
        stats.clear()
        _binding = null
    }

    private fun showTrash() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response=ApiClient.getService().getMediaTrash()
                check(response.isSuccessful && response.body()?.code==200)
                val files=response.body()?.data.orEmpty()
                androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle(UiStrings.s(com.familyrecipes.android.R.string.ui_3aa47b363c))
                    .setItems(files.map { "${it["category"]} · ${it["filename"]}${if(it["purged_at"]!=null) getString(R.string.review_purged) else ""}" }.toTypedArray()) { _,index ->
                        val file=files[index]
                        if(file["purged_at"]==null) androidx.appcompat.app.AlertDialog.Builder(requireContext()).setMessage(UiStrings.s(com.familyrecipes.android.R.string.ui_34053f01aa))
                            .setPositiveButton(UiStrings.s(com.familyrecipes.android.R.string.ui_c7db6d4f1a)) { _,_ -> viewLifecycleOwner.lifecycleScope.launch {
                                try { val result=ApiClient.getService().restoreMedia(file["filename"].toString())
                                    check(result.isSuccessful && result.body()?.code==200);showTrash()
                                } catch(e:kotlinx.coroutines.CancellationException) { throw e }
                                  catch(_:Exception) { android.widget.Toast.makeText(context,UiStrings.s(com.familyrecipes.android.R.string.ui_2e827d23cb),android.widget.Toast.LENGTH_SHORT).show() }
                            } }.setNegativeButton(UiStrings.s(com.familyrecipes.android.R.string.ui_625fb26b4b),null).show()
                    }.setPositiveButton(UiStrings.s(com.familyrecipes.android.R.string.ui_b15d91274e),null).show()
            } catch(e:kotlinx.coroutines.CancellationException) { throw e }
              catch(_:Exception) { android.widget.Toast.makeText(context,UiStrings.s(com.familyrecipes.android.R.string.ui_227cc473cf),android.widget.Toast.LENGTH_SHORT).show() }
        }
    }
}
