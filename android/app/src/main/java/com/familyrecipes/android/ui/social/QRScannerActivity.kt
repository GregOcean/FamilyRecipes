package com.familyrecipes.android.ui.social

import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageButton
import com.familyrecipes.android.R
import com.familyrecipes.android.ui.design.Ui
import com.familyrecipes.android.ui.design.UiStrings
import com.journeyapps.barcodescanner.CaptureActivity

/** Keep the camera preview portrait and provide a visible, accessible exit. */
class QRScannerActivity : CaptureActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val back = ImageButton(this).apply {
            setImageResource(R.drawable.ic_arrow_back)
            imageTintList = android.content.res.ColorStateList.valueOf(Ui.blue)
            contentDescription = UiStrings.s(R.string.ui_5f411223ca)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Ui.surface)
            }
            setOnClickListener { setResult(RESULT_CANCELED); finish() }
        }
        findViewById<FrameLayout>(android.R.id.content).addView(back,
            FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP or Gravity.START).apply {
                topMargin = dp(16); marginStart = dp(16)
            })
    }
}
