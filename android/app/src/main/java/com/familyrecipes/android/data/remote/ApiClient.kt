package com.familyrecipes.android.data.remote

import android.content.Context
import com.familyrecipes.android.data.local.PreferenceManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * API客户端
 */
object ApiClient {
    
    private lateinit var apiService: ApiService
    private lateinit var visionService: ApiService

    fun init(context: Context) {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (com.familyrecipes.android.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }

        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept-Language", contentLanguage())
                .build()
            val token = PreferenceManager.authToken
            
            val newRequest = if (!token.isNullOrEmpty()) {
                request.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                request
            }
            
            chain.proceed(newRequest)
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(PreferenceManager.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
        visionService = retrofit.newBuilder().client(okHttpClient.newBuilder()
            .readTimeout(65, TimeUnit.SECONDS).callTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false).build()).build().create(ApiService::class.java)
    }

    fun contentLanguage(): String {
        val selected = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().get(0)
        val locale = selected ?: java.util.Locale.getDefault()
        return if (locale.language == "en") "en" else "zh"
    }

    fun getService(): ApiService = apiService
    fun getVisionService(): ApiService = visionService
}

