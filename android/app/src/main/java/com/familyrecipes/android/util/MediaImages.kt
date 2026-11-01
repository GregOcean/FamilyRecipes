package com.familyrecipes.android.util

import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.familyrecipes.android.data.local.PreferenceManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object MediaImages {
    // Never send our bearer token to a third-party thumbnail host.
    fun model(url: String?): Any? {
        val image = url?.toHttpUrlOrNull() ?: return url
        val base = PreferenceManager.baseUrl.toHttpUrlOrNull() ?: return url
        val token = PreferenceManager.authToken
        return if (!token.isNullOrEmpty() && image.scheme == base.scheme && image.host == base.host &&
            image.port == base.port && image.encodedPath.startsWith("/api/upload/files/")) {
            GlideUrl(url, LazyHeaders.Builder().addHeader("Authorization", "Bearer $token").build())
        } else url
    }
}
