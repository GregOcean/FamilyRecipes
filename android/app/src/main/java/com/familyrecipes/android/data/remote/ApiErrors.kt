package com.familyrecipes.android.data.remote

import com.familyrecipes.android.data.model.ApiResponse
import com.google.gson.Gson
import retrofit2.Response

object ApiErrors {
    fun message(response: Response<out ApiResponse<*>>, fallback: String): String {
        response.body()?.message?.takeIf { it.isNotBlank() }?.let { return it }
        // Retrofit puts HTTP 4xx/5xx JSON in errorBody, not body.
        return runCatching {
            Gson().fromJson(response.errorBody()?.string(), ApiResponse::class.java)
                ?.message?.takeIf { it.isNotBlank() }
        }.getOrNull() ?: fallback
    }
}
