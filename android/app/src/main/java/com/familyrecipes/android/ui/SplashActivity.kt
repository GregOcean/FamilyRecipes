package com.familyrecipes.android.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.familyrecipes.android.R

/**
 * 启动欢迎页面
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 1秒后跳转到主页面
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            // 添加淡入淡出动画
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 1000)
    }
}

