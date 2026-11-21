package com.familyrecipes.android.ui.recipe

internal object RecipeSources {
    val keys=listOf("created","known","favorites","shared")
    fun fromLegacy(scope:String?,origin:String?,known:Boolean):String {
        if(known)return "known"
        if(origin in keys)return origin!!
        return if(scope=="mine")keys.joinToString(",") else ""
    }
}
