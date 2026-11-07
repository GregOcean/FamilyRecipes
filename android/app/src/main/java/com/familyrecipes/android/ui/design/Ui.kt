package com.familyrecipes.android.ui.design

import com.familyrecipes.android.ui.design.UiStrings
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.*
import com.familyrecipes.android.data.model.ApiResponse
import com.familyrecipes.android.data.remote.ApiErrors
import com.google.android.material.button.MaterialButton
import retrofit2.Response

object Ui {
    private fun color(id: Int): Int {
        val app = com.familyrecipes.android.FamilyRecipesApp.instance
        val config = android.content.res.Configuration(app.resources.configuration)
        val mode = androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode()
        val night = when(mode) {
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES -> android.content.res.Configuration.UI_MODE_NIGHT_YES
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO -> android.content.res.Configuration.UI_MODE_NIGHT_NO
            else -> config.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        }
        config.uiMode = (config.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or night
        return app.createConfigurationContext(config).getColor(id)
    }
    val blue get() = color(com.familyrecipes.android.R.color.primary)
    val ink get() = color(com.familyrecipes.android.R.color.text_primary)
    val muted get() = color(com.familyrecipes.android.R.color.text_secondary)
    val bg get() = color(com.familyrecipes.android.R.color.background)
    val pale get() = color(com.familyrecipes.android.R.color.primary_light)
    val surface get() = color(com.familyrecipes.android.R.color.surface)
    val outline get() = color(com.familyrecipes.android.R.color.outline)
    val onBlue get() = color(com.familyrecipes.android.R.color.on_primary)
    val warm get() = color(com.familyrecipes.android.R.color.warm_surface)
    val green get() = color(com.familyrecipes.android.R.color.green_surface)
    val purple get() = color(com.familyrecipes.android.R.color.purple_surface)

    fun Context.dp(n: Int) = (resources.displayMetrics.density * n).toInt()

    fun box(c: Context, padding: Int = 16) =
        LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(c.dp(padding), c.dp(padding), c.dp(padding), c.dp(padding))
            setBackgroundColor(bg)
        }

    fun row(c: Context) =
        LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

    fun text(c: Context, value: String, size: Float = 17f, bold: Boolean = false) =
        TextView(c).apply {
            text = value
            textSize = size
            setTextColor(ink)
            if (bold) setTypeface(null, Typeface.BOLD)
            setPadding(0, c.dp(8), 0, c.dp(8))
        }

    fun button(c: Context, label: String, primary: Boolean = false, action: () -> Unit) =
        MaterialButton(c).apply {
            text = label
            textSize = 16f
            isAllCaps = false
            minHeight = c.dp(48)
            cornerRadius = c.dp(14)
            setTextColor(if (primary) onBlue else blue)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(if (primary) blue else pale)
            setOnClickListener { action() }
        }

    /** Compact visual arrow with a full accessible touch target. */
    fun back(c: Context, action: () -> Unit) =
        androidx.appcompat.widget.AppCompatImageButton(c).apply {
            setImageResource(com.familyrecipes.android.R.drawable.ic_arrow_back)
            imageTintList = android.content.res.ColorStateList.valueOf(blue)
            contentDescription = UiStrings.s(com.familyrecipes.android.R.string.ui_5f411223ca)
            setPadding(c.dp(12), c.dp(12), c.dp(12), c.dp(12))
            val value = android.util.TypedValue()
            c.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, value, true)
            setBackgroundResource(value.resourceId)
            layoutParams = LinearLayout.LayoutParams(c.dp(48), c.dp(48)).apply {
                gravity = android.view.Gravity.START
            }
            setOnClickListener { action() }
        }

    fun field(c: Context, label: String, value: String = "", multi: Boolean = false) =
        EditText(c).apply {
            hint = label
            setText(value)
            textSize = 17f
            minHeight = c.dp(54)
            setTextColor(ink)
            setHintTextColor(muted)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            inputType =
                if (multi)
                    android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                else android.text.InputType.TYPE_CLASS_TEXT
            setSingleLine(!multi)
            if (multi) {
                minLines = 3
                gravity = android.view.Gravity.TOP
            }
            setPadding(c.dp(12), c.dp(12), c.dp(12), c.dp(12))
            background =
                GradientDrawable().apply {
                    setColor(surface)
                    cornerRadius = c.dp(12).toFloat()
                    setStroke(c.dp(1), outline)
                }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = c.dp(12) }
        }

    fun select(c: Context, items: List<String>, selected: Int = 0, change: (Int) -> Unit) =
        Spinner(c).apply {
            minimumHeight = c.dp(48)
            adapter =
                object :
                    ArrayAdapter<String>(c, android.R.layout.simple_spinner_dropdown_item, items) {
                    override fun getView(
                        position: Int,
                        convert: View?,
                        parent: android.view.ViewGroup
                    ): View {
                        return (super.getView(position, convert, parent) as TextView).apply {
                            textSize = 14f
                            setTextColor(ink)
                            setPadding(c.dp(4), 0, 0, 0)
                            maxLines = 2
                            ellipsize = null
                        }
                    }
                }
            setSelection(selected)
            onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(p: AdapterView<*>?) {}

                    override fun onItemSelected(p: AdapterView<*>?, v: View?, i: Int, id: Long) {
                        change(i)
                    }
                }
        }

    fun card(c: Context) =
        box(c).apply {
            background =
                GradientDrawable().apply {
                    setColor(surface)
                    cornerRadius = c.dp(18).toFloat()
                }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = c.dp(12) }
        }

    fun scroll(c: Context, box: View) =
        ScrollView(c).apply {
            isFillViewport = true
            addView(box)
        }

    fun tile(c: Context, label: String, icon: Int, tint: Int = pale, action: () -> Unit): LinearLayout =
        card(c).apply {
            setPadding(c.dp(16), c.dp(14), c.dp(16), c.dp(14))
            background = GradientDrawable().apply { setColor(tint); cornerRadius=c.dp(18).toFloat() }
            minimumHeight=c.dp(104)
            val art=ImageView(c).apply { setImageResource(icon); importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO }
            addView(art, LinearLayout.LayoutParams(c.dp(30),c.dp(30)))
            addView(text(c,label,18f,true))
            isFocusable=true; contentDescription=label
            setOnClickListener { action() }
        }

    fun entry(c: Context, title: String, subtitle: String, icon: Int, action: () -> Unit): LinearLayout =
        row(c).apply {
            setPadding(c.dp(14),c.dp(12),c.dp(14),c.dp(12)); minimumHeight=c.dp(80)
            background=GradientDrawable().apply{setColor(surface);cornerRadius=c.dp(18).toFloat()}
            val art=ImageView(c).apply {setImageResource(icon);imageTintList=android.content.res.ColorStateList.valueOf(blue);setPadding(c.dp(10),c.dp(10),c.dp(10),c.dp(10));background=GradientDrawable().apply{setColor(pale);cornerRadius=c.dp(14).toFloat()}}
            addView(art,LinearLayout.LayoutParams(c.dp(48),c.dp(48)))
            val words=box(c,0).apply {setBackgroundColor(Color.TRANSPARENT);setPadding(c.dp(12),0,c.dp(8),0)}
            words.addView(text(c,title,18f,true).apply{setPadding(0,0,0,c.dp(4))})
            words.addView(text(c,subtitle,14f).apply{setTextColor(muted);setPadding(0,0,0,0);maxLines=2;ellipsize=android.text.TextUtils.TruncateAt.END})
            addView(words,LinearLayout.LayoutParams(0,-2,1f));addView(text(c,"›",24f))
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=c.dp(10)}
            isFocusable=true;setOnClickListener{action()}
        }

    fun toast(c: Context, error: Throwable) =
        Toast.makeText(c, error.message ?: UiStrings.s(com.familyrecipes.android.R.string.ui_915593a1d4), Toast.LENGTH_LONG).show()

    fun <T> data(r: Response<ApiResponse<T>>): T {
        check(r.isSuccessful && r.body()?.code == 200) { ApiErrors.message(r, UiStrings.s(com.familyrecipes.android.R.string.ui_915593a1d4)) }
        return requireNotNull(r.body()?.data) { UiStrings.s(com.familyrecipes.android.R.string.ui_eb719e385c) }
    }

    fun ok(r: Response<out ApiResponse<*>>) {
        check(r.isSuccessful && r.body()?.code == 200) { ApiErrors.message(r, UiStrings.s(com.familyrecipes.android.R.string.ui_915593a1d4)) }
    }
}
