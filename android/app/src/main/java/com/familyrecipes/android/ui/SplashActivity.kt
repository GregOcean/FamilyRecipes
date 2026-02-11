package com.familyrecipes.android.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

/**
 * 启动欢迎页面
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 安装splash screen（会让系统splash快速消失）
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // 显示自定义的启动页面（带"家肴"字样）
        setContentView(com.familyrecipes.android.R.layout.activity_splash)

        // 延迟后跳转到主页面
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            // 添加淡入淡出动画
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 500) // 0.5秒
    }
}

