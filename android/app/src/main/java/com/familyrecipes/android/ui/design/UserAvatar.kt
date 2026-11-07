package com.familyrecipes.android.ui.design

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.familyrecipes.android.data.local.PreferenceManager
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.util.MediaImages
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

object UserAvatar {
    fun current(c: Context, owner: LifecycleOwner): View {
        val frame = FrameLayout(c)
        fun render(name: String, url: String?) {
            frame.removeAllViews()
            frame.addView(create(c, name, url), FrameLayout.LayoutParams(-1, -1))
        }
        render(PreferenceManager.userName.orEmpty(), null)
        val account = PreferenceManager.userId
        if (PreferenceManager.isLoggedIn()) owner.lifecycleScope.launch {
            try {
                val user = Ui.data(ApiClient.getService().getCurrentUser())
                if (PreferenceManager.userId == account) render(user.username.orEmpty(), user.avatar)
            } catch (e: CancellationException) { throw e }
              catch (_: Exception) { /* Keep the local identity while offline. */ }
        }
        return frame
    }

    fun create(c: Context, name: String, url: String?): View {
        fun dp(n:Int)=(c.resources.displayMetrics.density*n).toInt()
        val colors=intArrayOf(Ui.pale,Ui.green,Ui.warm,Ui.purple)
        val frame=FrameLayout(c).apply {
            background=GradientDrawable().apply {setColor(colors[(name.hashCode() and Int.MAX_VALUE)%colors.size]);cornerRadius=dp(16).toFloat()}
            clipToOutline=true
            contentDescription=name
        }
        if (name.isBlank()) {
            frame.addView(ImageView(c).apply {
                setImageResource(com.familyrecipes.android.R.drawable.ic_person_filled)
                imageTintList = android.content.res.ColorStateList.valueOf(Ui.blue)
                setPadding(dp(10), dp(10), dp(10), dp(10))
                scaleType = ImageView.ScaleType.FIT_CENTER
            }, FrameLayout.LayoutParams(-1, -1))
        } else {
            frame.addView(TextView(c).apply {
                text=name.take(1).uppercase();textSize=20f;setTextColor(Ui.ink);gravity=Gravity.CENTER;setTypeface(null,Typeface.BOLD)
            },FrameLayout.LayoutParams(-1,-1))
        }
        if(!url.isNullOrBlank()) {
            val image=ImageView(c)
            frame.addView(image,FrameLayout.LayoutParams(-1,-1))
            Glide.with(image).load(MediaImages.model(url)).signature(ObjectKey(PreferenceManager.userId)).centerCrop().into(image)
        }
        return frame
    }
}
