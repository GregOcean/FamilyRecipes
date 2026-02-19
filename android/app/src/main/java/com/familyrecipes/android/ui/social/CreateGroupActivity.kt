package com.familyrecipes.android.ui.social

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.familyrecipes.android.R
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.databinding.ActivityCreateGroupBinding
import kotlinx.coroutines.launch

/**
 * 创建群聊页面
 */
class CreateGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateGroupBinding
    private val maxMembers = 20 // 默认最大成员数

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityCreateGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupTextWatchers()
        setupCreateButton()
        updateMaxMembersHint()
    }

    /**
     * 设置工具栏
     */
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    /**
     * 设置文本输入监听器（字符计数）
     */
    private fun setupTextWatchers() {
        // 群名称字符计数
        binding.etGroupName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                binding.tvNameCount.text = "$length/24"
            }
        })

        // 群介绍字符计数
        binding.etGroupDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                binding.tvDescriptionCount.text = "$length/100"
            }
        })
    }

    /**
     * 更新最大成员数提示
     */
    private fun updateMaxMembersHint() {
        binding.tvMaxMembersHint.text = "当前群聊最多支持 $maxMembers 人"
    }

    /**
     * 设置创建按钮
     */
    private fun setupCreateButton() {
        binding.btnCreate.setOnClickListener {
            val name = binding.etGroupName.text.toString().trim()
            val description = binding.etGroupDescription.text.toString().trim()

            // 表单验证
            when {
                name.isEmpty() -> {
                    Toast.makeText(this, "请输入群名称", Toast.LENGTH_SHORT).show()
                    binding.etGroupName.requestFocus()
                    return@setOnClickListener
                }
                description.isEmpty() -> {
                    Toast.makeText(this, "请输入群介绍", Toast.LENGTH_SHORT).show()
                    binding.etGroupDescription.requestFocus()
                    return@setOnClickListener
                }
            }

            // 创建群聊
            createGroup(name, description)
        }
    }

    /**
     * 创建群聊
     */
    private fun createGroup(name: String, description: String) {
        // 禁用按钮，防止重复点击
        binding.btnCreate.isEnabled = false
        binding.btnCreate.text = "创建中..."

        lifecycleScope.launch {
            try {
                val request = com.familyrecipes.android.data.model.CreateGroupRequest(
                    name = name,
                    description = description,
                    maxMembers = maxMembers,
                    memberIds = emptyList()  // 创建时不添加成员
                )

                val response = ApiClient.getService().createGroup(request)

                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(
                        this@CreateGroupActivity,
                        "群聊创建成功",
                        Toast.LENGTH_SHORT
                    ).show()

                    // 返回结果
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val message = response.body()?.message ?: "创建失败"
                    Toast.makeText(
                        this@CreateGroupActivity,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.btnCreate.isEnabled = true
                    binding.btnCreate.text = "立即创建"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@CreateGroupActivity,
                    "创建失败：${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                binding.btnCreate.isEnabled = true
                binding.btnCreate.text = "立即创建"
            }
        }
    }
}

