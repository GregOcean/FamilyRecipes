package com.familyrecipes.android.ui.profile

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
                    codeRequestPending -> "发送中…"
                    seconds>0 -> "${seconds}秒后重发"
                    else -> "获取验证码"
                }
                if(seconds<=0)break
                kotlinx.coroutines.delay(1000)
            }
        }
    }
    private fun requestRegistrationCode() {
        val email=emailKey()
        if(email.length>100 || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error="请输入有效的邮箱地址";return
        }
        codeRequestPending=true;refreshCodeButton()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response=ApiClient.getService().sendRegistrationCode(mapOf("email" to email))
                if(response.isSuccessful && response.body()?.code==200) {
                    resendAt[email]=System.currentTimeMillis()+60000
                    if(email==emailKey()) binding.etVerificationCode.setText("")
                    android.widget.Toast.makeText(context,"验证码已发送，10分钟内有效",android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context,com.familyrecipes.android.data.remote.ApiErrors.message(response,"验证码发送失败"),android.widget.Toast.LENGTH_LONG).show()
                }
            } catch(e:kotlinx.coroutines.CancellationException) { throw e }
              catch(_:Exception) { android.widget.Toast.makeText(context,"验证码发送失败，请检查网络后重试",android.widget.Toast.LENGTH_LONG).show() }
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
            // 未登录，显示登录页面
            requireActivity().recreate()
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
        identity.addView(Ui.text(c,"🌻",36f).apply{gravity=android.view.Gravity.CENTER;background=android.graphics.drawable.GradientDrawable().apply{setColor(0xfffff1bf.toInt());cornerRadius=c.dp(24).toFloat()}},LinearLayout.LayoutParams(c.dp(76),c.dp(76)))
        val bio=Ui.box(c,0).apply{setPadding(c.dp(16),0,0,0)}
        bio.addView(Ui.text(c,PreferenceManager.userName ?: "家肴用户",25f,true))
        bio.addView(Ui.text(c,PreferenceManager.userEmail.orEmpty(),14f).apply{setTextColor(Ui.muted)})
        identity.addView(bio,LinearLayout.LayoutParams(0,-2,1f));root.addView(identity)
        val numbers=Ui.card(c).apply{orientation=LinearLayout.HORIZONTAL;setPadding(0,c.dp(14),0,c.dp(14))}
        listOf("created" to "我的菜谱","favorites" to "我的收藏","known" to "我会做").forEach{(key,label)->
            val item=Ui.box(c,0).apply{setBackgroundColor(android.graphics.Color.TRANSPARENT);gravity=android.view.Gravity.CENTER;minimumHeight=c.dp(72);isFocusable=true}
            val count=Ui.text(c,"—",25f,true).apply{gravity=android.view.Gravity.CENTER;setTextColor(Ui.blue);setPadding(0,0,0,0)}
            stats[key]=count;item.addView(count);item.addView(Ui.text(c,label,15f).apply{gravity=android.view.Gravity.CENTER})
            item.setOnClickListener{openRecipes(if(key=="known")"all" else key,key=="known")}
            numbers.addView(item,LinearLayout.LayoutParams(0,-2,1f))
        }
        root.addView(numbers,LinearLayout.LayoutParams(-1,-2).apply{topMargin=c.dp(20);bottomMargin=c.dp(12)})
        val shortcuts=listOf(Triple("我的点菜",R.drawable.ic_ui_order,"order"),Triple("我的备菜",R.drawable.ic_ui_prep,"prep"),Triple("草稿箱",R.drawable.ic_ui_draft,"draft"),Triple("分享给我的",R.drawable.ic_ui_users,"shared"))
        shortcuts.chunked(2).forEach{pair->val row=Ui.row(c);pair.forEach{(label,icon,key)->row.addView(Ui.tile(c,label,icon,if(key=="order")0xfffff1cf.toInt() else if(key=="draft")0xffeeeafb.toInt() else Ui.pale){if(key=="shared")androidx.appcompat.app.AlertDialog.Builder(c).setTitle("分享给我的").setItems(arrayOf("共享菜谱","点菜与备菜")){_,which->if(which==0)openRecipes("shared") else openMeals("prep")}.show() else openMeals(key)},LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(c.dp(4),c.dp(4),c.dp(4),c.dp(4))})};root.addView(row)}
        root.addView(Ui.text(c,"我有权限的厨房",21f,true))
        val kitchens=Ui.box(c,0);root.addView(kitchens)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val rows=Ui.data(ApiClient.getService().getKitchens())
                rows.forEach{k->kitchens.addView(Ui.entry(c,k.name,if(k.id==PreferenceManager.currentKitchenId)"当前厨房 · 查看库存" else "共同使用 · 切换厨房",R.drawable.ic_ui_kitchen){PreferenceManager.saveCurrentKitchen(k.id,k.name);PreferenceManager.currentGroupId=k.groupId;(activity as? com.familyrecipes.android.ui.MainActivity)?.showPage("stock");requireActivity().recreate()})}
                if(rows.isEmpty())kitchens.addView(Ui.text(c,"暂无可访问的厨房"))
            }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(_:Exception){kitchens.addView(Ui.button(c,"厨房加载失败 · 重试"){quickLinksAdded=false;showProfilePage()})}
        }
        root.addView(Ui.text(c,"账户与工具",20f,true))
        root.addView(Ui.entry(c,"通知与提醒","设置每天提醒时间",R.drawable.ic_clock_filled){showReminderSettings()})
        root.addView(Ui.entry(c,"我的二维码","扫码加入群聊",R.drawable.ic_qr_code){startActivity(Intent(c,com.familyrecipes.android.ui.social.MyQRCodeActivity::class.java))})
        root.addView(Ui.entry(c,"图片垃圾箱","查看和恢复已删除图片",R.drawable.ic_ui_draft){showTrash()})
        root.addView(Ui.button(c,"退出登录"){
            PreferenceManager.clearUserData()
            startActivity(Intent(c,com.familyrecipes.android.ui.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        })
        loadRecipeCounts()
    }

    private fun setupLoginListeners() {
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
            binding.btnLogin.text = "登录"
            binding.tvSwitchMode.text = "还没有账号？立即注册"
        } else {
            binding.layoutUsername.visibility = View.VISIBLE
            binding.btnLogin.text = "注册"
            binding.tvSwitchMode.text = "已有账号？立即登录"
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
            android.widget.Toast.makeText(context, "请输入邮箱和密码", android.widget.Toast.LENGTH_SHORT).show()
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
                    }
                } else {
                    android.widget.Toast.makeText(context, com.familyrecipes.android.data.remote.ApiErrors.message(response, "登录失败"), android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "网络错误", android.widget.Toast.LENGTH_SHORT).show()
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
            "家肴用户_${generateRandomString(5)}"
        }

        binding.etEmail.error = null
        binding.etPassword.error = null
        binding.etUsername.error = null
        if (email.length > 100 || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "请输入有效的邮箱地址"
            binding.etEmail.requestFocus()
            return
        }
        com.familyrecipes.android.util.PasswordRules.registrationError(password)?.let { error ->
            binding.etPassword.error = error
            binding.etPassword.requestFocus()
            return
        }
        if (username.length > 50) {
            binding.etUsername.error = "用户名最多50个字符"
            binding.etUsername.requestFocus()
            return
        }

        val code=binding.etVerificationCode.text.toString().trim()
        if(!code.matches(Regex("[0-9]{6}"))) {
            binding.etVerificationCode.error="请输入邮件中的6位验证码"
            binding.etVerificationCode.requestFocus()
            return
        }
        binding.btnLogin.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = mapOf("email" to email, "password" to password, "username" to username, "code" to code)
                val response = ApiClient.getService().register(request)

                if (response.isSuccessful && response.body()?.code == 200) {
                    android.widget.Toast.makeText(context, "注册成功，正在登录...", android.widget.Toast.LENGTH_SHORT).show()
                    
                    // 注册成功后自动登录
                    performLogin()
                } else {
                    android.widget.Toast.makeText(context, com.familyrecipes.android.data.remote.ApiErrors.message(response, "注册失败"), android.widget.Toast.LENGTH_SHORT).show()
                    binding.btnLogin.isEnabled = true
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "网络错误: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                binding.btnLogin.isEnabled = true
            }
        }
    }

    private fun loadUserProfile() {
        binding.tvUsername.text = PreferenceManager.userName ?: "家肴用户"
        binding.tvEmail.text = PreferenceManager.userEmail ?: ""
        
        // 加载我喜欢的菜谱数量和我上传的菜谱数量
        loadRecipeCounts()
    }

    private fun loadRecipeCounts() {
        stats.forEach{(key,view)->viewLifecycleOwner.lifecycleScope.launch {
            try {
                val page=Ui.data(ApiClient.getService().browseRecipes(PreferenceManager.currentKitchenId,scope="mine",origin=if(key=="known")"all" else key,known=key=="known"))
                view.text=page.total.toString()
            }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(_:Exception){view.text="重试";view.setOnClickListener{loadRecipeCounts()}}
        }}
    }

    private fun setupProfileListeners() {
        // 退出登录
        binding.btnLogout.setOnClickListener {
            PreferenceManager.clearUserData()
            showLoginPage()
        }
        
        // 点击收藏的菜谱
        binding.layoutFavorites.setOnClickListener {
            val intent = android.content.Intent(requireContext(), FavoriteRecipesActivity::class.java)
            startActivity(intent)
        }
        
        // 点击我的作品
        binding.layoutCreated.setOnClickListener {
            val intent = android.content.Intent(requireContext(), MyCreatedRecipesActivity::class.java)
            startActivity(intent)
        }
        
        // 顶部工具栏按钮
        // 扫一扫
        binding.ivScanQr.setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.familyrecipes.android.ui.social.ScanQRActivity::class.java)
            startActivity(intent)
        }
        
        // 编辑资料
        binding.ivEditProfile.setOnClickListener {
            android.widget.Toast.makeText(context, "编辑资料功能即将上线", android.widget.Toast.LENGTH_SHORT).show()
            // TODO: 跳转到编辑资料页面
        }
        
        // 设置
        binding.ivSettings.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle("设置").setItems(arrayOf("通知与提醒","字体与显示")){_,index->when(index){0->showReminderSettings();1->startActivity(android.content.Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS))}}.show()
            // TODO: 跳转到设置页面
        }
        
        // 分享
        binding.ivShare.setOnClickListener {
            shareApp()
        }
        
        // 我的名片码
        binding.layoutMyQr.setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.familyrecipes.android.ui.social.MyQRCodeActivity::class.java)
            startActivity(intent)
        }
        
        // 我的好友
        binding.layoutFriends.setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.familyrecipes.android.ui.social.FriendsListActivity::class.java)
            startActivity(intent)
        }
        
        // 我加入的群
        binding.layoutJoinedGroups.setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.familyrecipes.android.ui.social.JoinedGroupsActivity::class.java)
            startActivity(intent)
        }
        
        // 我创建的群
        binding.layoutCreatedGroups.setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.familyrecipes.android.ui.social.CreatedGroupsActivity::class.java)
            startActivity(intent)
        }
    }
    
    /**
     * 分享App
     */
    private fun shareApp() {
        val shareText = "推荐你使用「家肴」App，记录美味生活！"
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(android.content.Intent.createChooser(shareIntent, "分享到"))
    }

    private fun generateRandomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    override fun onResume() {
        super.onResume()
        // 每次回到这个页面都检查登录状态
        checkLoginStatus()
    }

    override fun onHiddenChanged(hidden:Boolean) {
        super.onHiddenChanged(hidden)
        if(!hidden && _binding!=null){quickLinksAdded=false;stats.clear();showProfilePage()}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        quickLinksAdded=false
        stats.clear()
        _binding = null
    }

    private val notificationPermission = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }
    private fun showReminderSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response=ApiClient.getService().getReminderSetting()
                check(response.isSuccessful && response.body()?.code==200)
                val setting=response.body()?.data ?: com.familyrecipes.android.data.model.ReminderSetting(null,PreferenceManager.userId,3,"17:00:00",true)
                val box=android.widget.LinearLayout(requireContext()).apply { orientation=android.widget.LinearLayout.VERTICAL;setPadding(32,16,32,16) }
                val enabled=android.widget.CheckBox(requireContext()).apply { text="启用提醒，每天最多一次";isChecked=setting.enabled }
                val days=android.widget.EditText(requireContext()).apply { hint="提前几天（0–365）";inputType=android.text.InputType.TYPE_CLASS_NUMBER;setText(setting.daysBeforeExpiry.toString()) }
                val time=android.widget.TimePicker(requireContext()).apply {
                    setIs24HourView(true);hour=setting.reminderTime?.take(2)?.toIntOrNull() ?: 17
                    minute=setting.reminderTime?.substring(3,5)?.toIntOrNull() ?: 0
                }
                val info=android.widget.TextView(requireContext()).apply { text="默认提醒未来3天到期的食材。时间按本机时区；系统省电可能延后提醒。" }
                box.addView(enabled);box.addView(days);box.addView(time);box.addView(info)
                val dialog=androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle("食材提醒")
                    .setView(box).setPositiveButton("保存",null).setNegativeButton("取消",null).create()
                dialog.show()
                dialog.getButton(-1).setOnClickListener {
                    val count=days.text.toString().toIntOrNull()
                    if(count==null || count !in 0..365) { days.error="请输入0–365";return@setOnClickListener }
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val updated=setting.copy(daysBeforeExpiry=count,enabled=enabled.isChecked,
                                reminderTime=String.format(java.util.Locale.ROOT,"%02d:%02d:00",time.hour,time.minute))
                            val result=ApiClient.getService().updateReminderSetting(updated)
                            check(result.isSuccessful && result.body()?.code==200)
                            if(enabled.isChecked && android.os.Build.VERSION.SDK_INT>=33) notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            dialog.dismiss()
                        } catch(e:kotlinx.coroutines.CancellationException) { throw e }
                          catch(_:Exception) { days.error="保存失败，内容已保留，请重试" }
                    }
                }
            } catch(e:kotlinx.coroutines.CancellationException) { throw e }
              catch(_:Exception) { android.widget.Toast.makeText(context,"设置加载失败",android.widget.Toast.LENGTH_SHORT).show() }
        }
    }
    private fun showTrash() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response=ApiClient.getService().getMediaTrash()
                check(response.isSuccessful && response.body()?.code==200)
                val files=response.body()?.data.orEmpty()
                androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle("图片垃圾箱（30天后清理）")
                    .setItems(files.map { "${it["category"]} · ${it["filename"]}${if(it["purged_at"]!=null) "（已清理）" else ""}" }.toTypedArray()) { _,index ->
                        val file=files[index]
                        if(file["purged_at"]==null) androidx.appcompat.app.AlertDialog.Builder(requireContext()).setMessage("恢复这张图片？恢复后可用原图片地址重新关联。")
                            .setPositiveButton("恢复") { _,_ -> viewLifecycleOwner.lifecycleScope.launch {
                                try { val result=ApiClient.getService().restoreMedia(file["filename"].toString())
                                    check(result.isSuccessful && result.body()?.code==200);showTrash()
                                } catch(e:kotlinx.coroutines.CancellationException) { throw e }
                                  catch(_:Exception) { android.widget.Toast.makeText(context,"恢复失败",android.widget.Toast.LENGTH_SHORT).show() }
                            } }.setNegativeButton("取消",null).show()
                    }.setPositiveButton("关闭",null).show()
            } catch(e:kotlinx.coroutines.CancellationException) { throw e }
              catch(_:Exception) { android.widget.Toast.makeText(context,"垃圾箱加载失败",android.widget.Toast.LENGTH_SHORT).show() }
        }
    }
}
