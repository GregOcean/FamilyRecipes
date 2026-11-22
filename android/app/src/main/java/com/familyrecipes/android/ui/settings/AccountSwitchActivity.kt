package com.familyrecipes.android.ui.settings

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.familyrecipes.android.R
import com.familyrecipes.android.ui.design.Ui
import com.familyrecipes.android.ui.profile.ProfileFragment

/**
 * 切换账号独立页面：在登录成功前保留当前账号会话，返回即可取消切换。
 */
class AccountSwitchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this).apply {
            id = R.id.fragment_container
            setBackgroundColor(Ui.bg)
            fitsSystemWindows = true
        }
        setContentView(root)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment().apply {
                    arguments = Bundle().apply { putBoolean("switch_account", true) }
                }, "switch_account")
                .commit()
        }
    }
}
