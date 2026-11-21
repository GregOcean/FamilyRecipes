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
    private lateinit var sharedClient: OkHttpClient
    private var boundCache: Triple<String,String,ApiService>? = null
    private var boundVisionCache: Triple<String,String,ApiService>? = null

    fun init(context: Context) {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (com.familyrecipes.android.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }

        sharedClient=OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        boundCache=null;boundVisionCache=null
        val okHttpClient=sharedClient.newBuilder().addInterceptor(authentication()).build()

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

    private fun authentication(account:String?=null, capturedToken:String?=null) = Interceptor { chain ->
        if(account!=null && (account!=com.familyrecipes.android.data.local.LocalStore.account ||
            capturedToken.isNullOrEmpty() || capturedToken!=PreferenceManager.authToken))
            throw java.io.IOException("Session changed")
        val token=if(account==null)PreferenceManager.authToken else capturedToken
        val request=chain.request().newBuilder().header("Accept-Language",contentLanguage()).apply {
            if(!token.isNullOrEmpty())header("Authorization","Bearer $token")
        }.build()
        val response=chain.proceed(request)
        if(response.code==401 && !token.isNullOrEmpty() &&
            (!request.url.encodedPath.startsWith("/api/auth/") || request.url.encodedPath=="/api/auth/me"))
            PreferenceManager.invalidateToken(token)
        response
    }

    /** A queued outbox request must never adopt the identity of a later login. */
    @Synchronized fun getAccountService(account:String):ApiService {
        val token=if(account==com.familyrecipes.android.data.local.LocalStore.account)PreferenceManager.authToken.orEmpty() else ""
        boundCache?.let {if(it.first==account && it.second==token)return it.third}
        val service=Retrofit.Builder().baseUrl(PreferenceManager.baseUrl)
            .client(sharedClient.newBuilder().addInterceptor(authentication(account,token)).build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)
        boundCache=Triple(account,token,service)
        return service
    }

    @Synchronized fun getAccountVisionService(account:String):ApiService {
        val token=if(account==com.familyrecipes.android.data.local.LocalStore.account)PreferenceManager.authToken.orEmpty() else ""
        boundVisionCache?.let {if(it.first==account && it.second==token)return it.third}
        val service=Retrofit.Builder().baseUrl(PreferenceManager.baseUrl)
            .client(sharedClient.newBuilder().addInterceptor(authentication(account,token))
                .readTimeout(65,TimeUnit.SECONDS).callTimeout(90,TimeUnit.SECONDS)
                .retryOnConnectionFailure(false).build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)
        boundVisionCache=Triple(account,token,service)
        return service
    }

    fun contentLanguage(): String {
        val selected = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().get(0)
        val locale = selected ?: java.util.Locale.getDefault()
        return if (locale.language == "en") "en" else "zh"
    }

    fun getService(): ApiService = apiService
    fun getVisionService(): ApiService = visionService
}

