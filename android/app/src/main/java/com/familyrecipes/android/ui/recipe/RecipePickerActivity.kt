package com.familyrecipes.android.ui.recipe

import com.familyrecipes.android.ui.design.UiStrings
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.familyrecipes.android.ui.design.Ui

class RecipePickerActivity : AppCompatActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val box = Ui.box(this, 0)
        box.addView(Ui.back(this) { finish() })
        val frame = FrameLayout(this).apply { id = android.R.id.content + 101 }
        box.addView(frame, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(box)
        if (state == null)
            supportFragmentManager
                .beginTransaction()
                .replace(
                    frame.id,
                    RecipeListFragment().apply {
                        arguments =
                            Bundle().apply {
                                putBoolean("picker", true)
                                putString("scope", "mine")
                            }
                    })
                .commit()
    }
}
