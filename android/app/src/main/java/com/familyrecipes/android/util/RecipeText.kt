package com.familyrecipes.android.util

/** Known MediaWiki presentation directives; preserve recipe punctuation and quantities. */
object RecipeText {
    fun clean(text:String):String = text
        .replace(Regex("__(?:NOTOC|TOC|FORCETOC|NOEDITSECTION|NEWSECTIONLINK|NONEWSECTIONLINK|NOGALLERY|HIDDENCAT|INDEX|NOINDEX)__",RegexOption.IGNORE_CASE),"")
        .replace(Regex("(?m)^\\s*\\|\\s+(?=\\S)"),"")
        .replace(Regex("\\n[ \\t]*\\n(?:[ \\t]*\\n)+"),"\n\n").trim()
}
