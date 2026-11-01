package com.familyrecipes.android.ui.design

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
    const val blue = 0xff007ca9.toInt()
    const val ink = 0xff153344.toInt()
    const val muted = 0xff516b7b.toInt()
    const val bg = 0xfff3f8fb.toInt()
    const val pale = 0xffe2f5fc.toInt()

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
            setTextColor(if (primary) Color.WHITE else blue)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(if (primary) blue else pale)
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
                    setColor(Color.WHITE)
                    cornerRadius = c.dp(12).toFloat()
                    setStroke(c.dp(1), 0xffdfebf1.toInt())
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
                    setColor(Color.WHITE)
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
            background=GradientDrawable().apply{setColor(Color.WHITE);cornerRadius=c.dp(18).toFloat()}
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
        Toast.makeText(c, error.message ?: "操作失败，请重试", Toast.LENGTH_LONG).show()

    fun <T> data(r: Response<ApiResponse<T>>): T {
        check(r.isSuccessful && r.body()?.code == 200) { ApiErrors.message(r, "操作失败，请重试") }
        return requireNotNull(r.body()?.data) { "服务器未返回数据" }
    }

    fun ok(r: Response<out ApiResponse<*>>) {
        check(r.isSuccessful && r.body()?.code == 200) { ApiErrors.message(r, "操作失败，请重试") }
    }
}
