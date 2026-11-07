package com.familyrecipes.android.data.model

/** Stable IDs travel with mentions; names are only display labels. No HTML is interpreted. */
object ChatContent {
    val mentionPattern = Regex("@\\[([^\\]\\r\\n]{1,80})\\]\\(familyrecipes://user/([1-9][0-9]*)\\)")
    val linkPattern = Regex("familyrecipes://(meal|recipe)/([1-9][0-9]*)(?![0-9])")
    fun mention(id: Long, name: String): String =
        "@[${name.replace(Regex("[\\[\\]\\r\\n]"), " ").take(80).ifBlank { id.toString() }}](familyrecipes://user/$id)"
    fun plain(text: String) = mentionPattern.replace(text) { "@${it.groupValues[1]}" }
    fun mentions(text: String, id: Long) = mentionPattern.findAll(text).any { it.groupValues[2].toLongOrNull() == id }
    data class Link(val kind: String, val id: Long)
    fun link(text: String): Link? {
        val match = linkPattern.find(text) ?: return null
        return match.groupValues[2].toLongOrNull()?.let { Link(match.groupValues[1], it) }
    }
}

data class SendMessageRequest(
    @com.google.gson.annotations.SerializedName("group_id") val groupId: Long,
    val content: String,
    @com.google.gson.annotations.SerializedName("message_type") val messageType: String,
    @com.google.gson.annotations.SerializedName("client_message_id") val clientMessageId: String)
data class ChatShareRequest(val groupId: Long, val clientMessageId: String? = null)
