package com.familyrecipes.android.data.remote

import com.familyrecipes.android.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * API接口定义
 */
interface ApiService {

    // ========== 认证相关 ==========
    @POST("/api/auth/register")
    suspend fun register(@Body request: Map<String, String>): Response<ApiResponse<User>>

    @POST("/api/auth/login")
    suspend fun login(@Body request: Map<String, String>): Response<ApiResponse<LoginResponse>>

    @GET("/api/auth/me")
    suspend fun getCurrentUser(): Response<ApiResponse<User>>
    
    // ========== 配置相关 ==========
    @GET("/api/config/all")
    suspend fun getAllConfigs(): Response<ApiResponse<AllConfigsResponse>>
    
    @GET("/api/config/ingredients")
    suspend fun getConfigIngredients(): Response<ApiResponse<List<ConfigIngredient>>>
    
    @GET("/api/config/storage-locations")
    suspend fun getConfigStorageLocations(): Response<ApiResponse<List<ConfigStorageLocation>>>
    
    @GET("/api/config/categories")
    suspend fun getConfigCategories(): Response<ApiResponse<List<ConfigCategory>>>
    
    @GET("/api/config/texts")
    suspend fun getConfigTexts(): Response<ApiResponse<Map<String, String>>>

    // ========== 菜谱相关 ==========
    @POST("/api/recipes")
    suspend fun createRecipe(@Body request: CreateRecipeRequest): Response<ApiResponse<Recipe>>

    @PUT("/api/recipes/{id}")
    suspend fun updateRecipe(
        @Path("id") id: Long,
        @Body request: CreateRecipeRequest
    ): Response<ApiResponse<Recipe>>

    @DELETE("/api/recipes/{id}")
    suspend fun deleteRecipe(@Path("id") id: Long): Response<ApiResponse<Unit>>

    @GET("/api/recipes/{id}")
    suspend fun getRecipeDetail(@Path("id") id: Long): Response<ApiResponse<Recipe>>

    @GET("/api/recipes/search")
    suspend fun searchRecipes(
        @Query("keyword") keyword: String? = null,
        @Query("tagType") tagType: String? = null,
        @Query("tagValue") tagValue: String? = null,
        @Query("ingredientIds") ingredientIds: List<Long>? = null,
        @Query("creatorId") creatorId: Long? = null,
        @Query("creatorUsername") creatorUsername: String? = null,
        @Query("orderBy") orderBy: String? = null,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<ApiResponse<PageResult<Recipe>>>

    @GET("/api/recipes/my-created")
    suspend fun getMyCreatedRecipes(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<ApiResponse<PageResult<Recipe>>>

    @GET("/api/recipes/my-favorites")
    suspend fun getMyFavorites(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<ApiResponse<PageResult<Recipe>>>

    @POST("/api/recipes/{id}/favorite")
    suspend fun toggleFavorite(@Path("id") id: Long): Response<ApiResponse<Boolean>>
    
    @POST("/api/recipes/{id}/dislike")
    suspend fun toggleDislike(@Path("id") id: Long): Response<ApiResponse<Boolean>>

    @POST("/api/recipes/{id}/external-links")
    suspend fun addExternalRecipe(
        @Path("id") id: Long,
        @Body externalRecipe: ExternalRecipe
    ): Response<ApiResponse<Unit>>

    @POST("/api/recipes/{id}/cook")
    suspend fun recordCooking(@Path("id") id: Long): Response<ApiResponse<Unit>>

    @GET("/api/recipes/recently-cooked")
    suspend fun getRecentlyCooked(@Query("limit") limit: Int = 10): Response<ApiResponse<List<Recipe>>>

    @GET("/api/recipes/tags/{tagType}")
    suspend fun getTagValues(@Path("tagType") tagType: String): Response<ApiResponse<List<String>>>

    // ========== 冰箱相关 ==========
    @POST("/api/fridge/items")
    suspend fun addFridgeItem(@Body item: FridgeItem): Response<ApiResponse<FridgeItem>>

    @PUT("/api/fridge/items/{id}")
    suspend fun updateFridgeItem(
        @Path("id") id: Long,
        @Body item: FridgeItem
    ): Response<ApiResponse<FridgeItem>>

    @POST("/api/fridge/items/{id}/consume")
    suspend fun markAsConsumed(@Path("id") id: Long): Response<ApiResponse<Unit>>

    @DELETE("/api/fridge/items/{id}")
    suspend fun deleteFridgeItem(@Path("id") id: Long): Response<ApiResponse<Unit>>

    @GET("/api/fridge/items")
    suspend fun getFridgeItems(): Response<ApiResponse<List<FridgeItem>>>
    
    @GET("/api/fridge/items/consumed")
    suspend fun getConsumedItems(): Response<ApiResponse<List<FridgeItem>>>

    @GET("/api/fridge/items/expiring")
    suspend fun getExpiringItems(): Response<ApiResponse<List<FridgeItem>>>

    @GET("/api/fridge/reminder-settings")
    suspend fun getReminderSetting(): Response<ApiResponse<ReminderSetting>>

    @PUT("/api/fridge/reminder-settings")
    suspend fun updateReminderSetting(@Body setting: ReminderSetting): Response<ApiResponse<Unit>>

    // ========== 食材相关 ==========
    @GET("/api/fridge/ingredients/search")
    suspend fun searchIngredients(@Query("keyword") keyword: String): Response<ApiResponse<List<Ingredient>>>

    @POST("/api/fridge/ingredients")
    suspend fun createIngredient(@Body ingredient: Ingredient): Response<ApiResponse<Ingredient>>
    
    // ========== 全局搜索 ==========
    @GET("/api/search/global")
    suspend fun globalSearch(
        @Query("keyword") keyword: String,
        @Query("priorityType") priorityType: String = "relevance",
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<ApiResponse<GlobalSearchResult>>

    // ========== 文件上传 ==========
    @Multipart
    @POST("/api/upload/image")
    suspend fun uploadImage(@Part file: MultipartBody.Part): Response<ApiResponse<String>>

    @Multipart
    @POST("/api/upload/images")
    suspend fun uploadImages(@Part files: List<MultipartBody.Part>): Response<ApiResponse<List<String>>>
}

