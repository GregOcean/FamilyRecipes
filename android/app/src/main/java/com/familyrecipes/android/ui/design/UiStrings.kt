package com.familyrecipes.android.ui.design

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.familyrecipes.android.FamilyRecipesApp

/** Uses the selected app locale without translating user-created content. */
object UiStrings {
    fun f(id:Int,vararg args:Any?):String = String.format(java.util.Locale.getDefault(),s(id),*args)
    fun domain(value:String):String = when(value) {
        "甜" -> s(com.familyrecipes.android.R.string.taste_sweet)
        "麻" -> s(com.familyrecipes.android.R.string.taste_numbing)
        "咸" -> s(com.familyrecipes.android.R.string.taste_salty)
        "酸" -> s(com.familyrecipes.android.R.string.enum_778b84eb)
        "辣" -> s(com.familyrecipes.android.R.string.enum_2134a402)
        "酸辣" -> s(com.familyrecipes.android.R.string.enum_32525fa1)
        "清淡" -> s(com.familyrecipes.android.R.string.enum_ee489f03)
        "鲜香" -> s(com.familyrecipes.android.R.string.enum_8d8d8a5a)
        "早餐" -> s(com.familyrecipes.android.R.string.enum_edb33656)
        "午餐" -> s(com.familyrecipes.android.R.string.enum_3cc4b779)
        "晚餐" -> s(com.familyrecipes.android.R.string.enum_96a6ae14)
        "加餐" -> s(com.familyrecipes.android.R.string.enum_601c4270)
        "简单" -> s(com.familyrecipes.android.R.string.enum_d517acb6)
        "较易" -> s(com.familyrecipes.android.R.string.enum_bee8c357)
        "适中" -> s(com.familyrecipes.android.R.string.enum_24ebacb2)
        "较难" -> s(com.familyrecipes.android.R.string.enum_de4d8ec4)
        "困难" -> s(com.familyrecipes.android.R.string.enum_d6f39fd5)
        "封面" -> s(com.familyrecipes.android.R.string.enum_41231061)
        "步骤图" -> s(com.familyrecipes.android.R.string.enum_0d5635de)
        "待分类" -> s(com.familyrecipes.android.R.string.enum_a9ad622c)
        else -> value
    }
    fun s(id:Int):String {
        val app=FamilyRecipesApp.instance
        val locales=AppCompatDelegate.getApplicationLocales()
        if(locales.isEmpty)return app.getString(id)
        val config=Configuration(app.resources.configuration)
        config.setLocale(locales[0])
        return app.createConfigurationContext(config).getString(id)
    }
}
