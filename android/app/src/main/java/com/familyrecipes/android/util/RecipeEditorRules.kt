package com.familyrecipes.android.util

/** Shared, UI-independent rules for editable recipe metadata. */
object RecipeEditorRules {
    fun labels(text: String): List<String> = text.split(Regex("[、，,;；\\n]+"))
        .map { it.trim().removePrefix("#").trim() }
        .filter { it.isNotEmpty() && it != "待确认" }
        .distinctBy { it.lowercase(java.util.Locale.ROOT) }

    fun galleryUrls(cover: String?, steps: List<String?>, photos: List<String>): List<String> {
        val used = (steps.filterNotNull() + listOfNotNull(cover)).toSet()
        return photos.filter { it.isNotBlank() && it !in used }.distinct()
    }
}
