package com.familyrecipes.android.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.familyrecipes.android.BuildConfig
import com.familyrecipes.android.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private var systemSplashRemovedAt=0L
    private var openedMain=false
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash=installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        findViewById<TextView>(R.id.splash_version).text =
            "${getString(R.string.app_version)} ${BuildConfig.VERSION_NAME}"
        // The exit callback is optional on warm/recreated launches. It only adjusts
        // the visible interval; it must never be the sole trigger for navigation.
        splash.setOnExitAnimationListener { provider ->
            provider.remove()
            systemSplashRemovedAt=SystemClock.elapsedRealtime()
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val resumedAt=SystemClock.elapsedRealtime()
                while(!openedMain) {
                    val remaining=maxOf(resumedAt,systemSplashRemovedAt)+1000L-SystemClock.elapsedRealtime()
                    if(remaining>0)delay(remaining) else break
                }
                // repeatOnLifecycle cancels the delay when the app leaves the
                // foreground. A later resume starts a fresh visible interval.
                if(!openedMain && !isFinishing && !isDestroyed) {
                    openedMain=true
                    startActivity(Intent(this@SplashActivity,MainActivity::class.java))
                    finish()
                    overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out)
                }
            }
        }
    }
}
