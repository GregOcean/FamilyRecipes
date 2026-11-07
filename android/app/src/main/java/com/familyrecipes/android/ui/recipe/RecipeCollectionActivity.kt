package com.familyrecipes.android.ui.recipe

import com.familyrecipes.android.ui.design.UiStrings
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.familyrecipes.android.ui.design.Ui

class RecipeCollectionActivity : AppCompatActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val box = Ui.box(this, 0)
        box.addView(Ui.button(this, UiStrings.s(com.familyrecipes.android.R.string.ui_7bb3e48630)) { finish() })
        val frame = FrameLayout(this).apply { id = android.R.id.content + 102 }
        box.addView(frame, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(box)
        if (b == null)
            supportFragmentManager
                .beginTransaction()
                .replace(
                    frame.id,
                    RecipeListFragment().apply {
                        arguments =
                            Bundle().apply {
                                putString("scope", "mine")
                                putString("origin", intent.getStringExtra("origin") ?: "all")
                                putBoolean("known", intent.getBooleanExtra("known", false))
                            }
                    })
                .commit()
    }
}
