package com.familyrecipes.android.util

/** Validate without decoding or dropping video IDs, timestamps, playlists or signed parameters. */
object ExternalLinkUrl {
    fun validate(value: String): String {
        val target = value.trim()
        val uri = java.net.URI(target)
        require(uri.scheme?.lowercase() in listOf("http", "https") && !uri.host.isNullOrBlank())
        return target
    }
}
