package com.familyrecipes.android.ui.profile

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
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        if (PreferenceManager.isLoggedIn()) {
            // 已登录，显示个人信息页面
            showProfilePage()
        } else {
            // 未登录，显示登录页面
            showLoginPage()
        }
    }

    private fun showLoginPage() {
        binding.layoutLogin.visibility = View.VISIBLE
        binding.layoutProfile.visibility = View.GONE
        
        setupLoginListeners()
    }

    private fun showProfilePage() {
        binding.layoutLogin.visibility = View.GONE
        binding.layoutProfile.visibility = View.VISIBLE
        
        loadUserProfile()
        setupProfileListeners()
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

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            android.widget.Toast.makeText(context, "请输入邮箱和密码", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
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
                    }
                } else {
                    android.widget.Toast.makeText(context, response.body()?.message ?: "登录失败", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "网络错误", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnLogin.isEnabled = true
            }
        }
    }

    private fun performRegister() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val username = binding.etUsername.text.toString().trim().ifEmpty {
            // 生成随机用户名：家肴用户_<5位随机字符>
            "家肴用户_${generateRandomString(5)}"
        }

        if (email.isEmpty() || password.isEmpty()) {
            android.widget.Toast.makeText(context, "请填写完整信息", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
            try {
                val request = mapOf("email" to email, "password" to password, "username" to username)
                val response = ApiClient.getService().register(request)

                if (response.isSuccessful && response.body()?.code == 200) {
                    android.widget.Toast.makeText(context, "注册成功，正在登录...", android.widget.Toast.LENGTH_SHORT).show()
                    
                    // 注册成功后自动登录
                    performLogin()
                } else {
                    android.widget.Toast.makeText(context, response.body()?.message ?: "注册失败", android.widget.Toast.LENGTH_SHORT).show()
                    binding.btnLogin.isEnabled = true
                }
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
        lifecycleScope.launch {
            try {
                // 加载收藏数
                val favResponse = ApiClient.getService().getMyFavorites(pageNum = 1, pageSize = 1)
                if (favResponse.isSuccessful) {
                    val favCount = favResponse.body()?.data?.total ?: 0
                    binding.tvFavoriteCount.text = favCount.toString()
                }
                
                // 加载创作数
                val createdResponse = ApiClient.getService().getMyCreatedRecipes(pageNum = 1, pageSize = 1)
                if (createdResponse.isSuccessful) {
                    val createdCount = createdResponse.body()?.data?.total ?: 0
                    binding.tvCreatedCount.text = createdCount.toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
            android.widget.Toast.makeText(context, "设置功能即将上线", android.widget.Toast.LENGTH_SHORT).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
