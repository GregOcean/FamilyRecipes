package com.familyrecipes.android.data.model

import com.google.gson.annotations.SerializedName

/**
 * API统一响应格式
 */
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)

/**
 * 分页结果
 */
data class PageResult<T>(
    val list: List<T>,
    val total: Long,
    val pageNum: Int,
    val pageSize: Int,
    val totalPages: Int
)

/**
 * 用户
 */
data class User(
    val id: Long,
    val email: String,
    val username: String,
    val avatar: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

/**
 * 登录响应
 */
data class LoginResponse(
    val token: String,
    val user: User
)

/**
 * 菜谱
 */
data class Recipe(
    val id: Long?,
    val name: String,
    val description: String?,
    @SerializedName("cover_image") val coverImage: String?,
    @SerializedName("cooking_time") val cookingTime: Int?,
    val difficulty: Int?,
    val servings: Int?,
    @SerializedName("creator_id") val creatorId: Long?,
    @SerializedName("view_count") val viewCount: Int?,
    @SerializedName("favorite_count") val favoriteCount: Int?,
    @SerializedName("dislike_count") val dislikeCount: Int?,
    @SerializedName("recently_cooked_count") val recentlyCookedCount: Int?,
    @SerializedName("created_at") val createdAt: String?,
    
    // 权限相关
    val visibility: String? = "group", // public, private, group
    @SerializedName("shared_group_ids") val sharedGroupIds: String? = null, // JSON数组
    @SerializedName("owner_group_id") val ownerGroupId: Long? = null,
    
    // 关联数据
    val creator: User?,
    val tags: List<RecipeTag>?,
    val ingredients: List<RecipeIngredient>?,
    val steps: List<CookingStep>?,
    @SerializedName("external_recipes") val externalRecipes: List<ExternalRecipe>?,
    val cooks: List<User>?,
    @SerializedName("is_favorite") val isFavorite: Boolean?,
    @SerializedName("is_disliked") val isDisliked: Boolean?
)

/**
 * 菜谱标签
 */
data class RecipeTag(
    val id: Long?,
    @SerializedName("recipe_id") val recipeId: Long?,
    @SerializedName("tag_type") val tagType: String,
    @SerializedName("tag_value") val tagValue: String
) {
    companion object {
        const val TYPE_MEAL_TIME = "meal_time"
        const val TYPE_DISH_TYPE = "dish_type"
        const val TYPE_MAIN_INGREDIENT = "main_ingredient"
        const val TYPE_SPECIAL = "special"
    }
}

/**
 * 库存分类
 */
data class InventoryCategory(
    val id: Long,
    val name: String,
    @SerializedName("parent_id") val parentId: Long?,
    val icon: String?,
    @SerializedName("sort_order") val sortOrder: Int,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
    val children: List<InventoryCategory>? = null // 子分类
)

/**
 * 食材/库存物品
 */
data class Ingredient(
    val id: Long?,
    val name: String,
    val category: String?,
    val unit: String? = null,
    @SerializedName("category_id") val categoryId: Long? = null // 库存分类ID
)

/**
 * 菜谱食材
 */
data class RecipeIngredient(
    val id: Long?,
    @SerializedName("recipe_id") val recipeId: Long?,
    @SerializedName("ingredient_id") val ingredientId: Long?,
    val amount: String?,
    @SerializedName("is_main") val isMain: Boolean?,
    val ingredient: Ingredient?
)

/**
 * 烹饪步骤
 */
data class CookingStep(
    val id: Long?,
    @SerializedName("recipe_id") val recipeId: Long?,
    @SerializedName("step_number") val stepNumber: Int,
    val description: String,
    val image: String?,
    val duration: Int?
)

/**
 * 外链食谱
 */
data class ExternalRecipe(
    val id: Long?,
    @SerializedName("recipe_id") val recipeId: Long?,
    val title: String,
    val url: String,
    val source: String?,
    val thumbnail: String?,  // 缩略图URL
    @SerializedName("added_by") val addedBy: Long?,
    @SerializedName("created_at") val createdAt: String?
)

/**
 * 冰箱食材
 */
data class FridgeItem(
    val id: Long?,
    @SerializedName("user_id") val userId: Long?,
    @SerializedName("ingredient_id") val ingredientId: Long?,
    val amount: String?,
    @SerializedName("purchase_date") val purchaseDate: String?,
    @SerializedName("expiry_date") val expiryDate: String,
    @SerializedName("storage_location") val storageLocation: String?,
    val status: String?,
    val notes: String?,
    val ingredient: Ingredient?
) {
    companion object {
        const val STATUS_NORMAL = "normal"
        const val STATUS_EXPIRING = "expiring"
        const val STATUS_EXPIRED = "expired"
        const val STATUS_CONSUMED = "consumed"
    }
}

/**
 * 提醒设置
 */
data class ReminderSetting(
    val id: Long?,
    @SerializedName("user_id") val userId: Long?,
    @SerializedName("days_before_expiry") val daysBeforeExpiry: Int,
    @SerializedName("reminder_time") val reminderTime: String?,
    val enabled: Boolean
)

/**
 * 创建菜谱请求
 */
data class CreateRecipeRequest(
    val recipe: Recipe,
    val tags: List<RecipeTag>?,
    val ingredients: List<RecipeIngredient>?,
    val steps: List<CookingStep>?,
    @SerializedName("cook_user_ids") val cookUserIds: List<Long>?,
    @SerializedName("external_recipes") val externalRecipes: List<ExternalRecipe>?
)

/**
 * 解析外部链接请求
 */
data class ParseLinkRequest(
    @SerializedName("pastedText") val pastedText: String
)

/**
 * 好友关系
 */
data class Friendship(
    val id: Long?,
    @SerializedName("user_id") val userId: Long?,
    @SerializedName("friend_id") val friendId: Long?,
    val nickname: String?,
    val status: String?,
    @SerializedName("created_at") val createdAt: String?,
    val friend: User?
)

/**
 * 群组
 */
data class GroupChat(
    val id: Long?,
    val name: String,
    val avatar: String?,
    @SerializedName("creator_id") val creatorId: Long?,
    @SerializedName("member_count") val memberCount: Int?,
    @SerializedName("max_members") val maxMembers: Int?,
    val description: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
    val members: List<GroupMember>?,
    @SerializedName("last_message") val lastMessage: Message?,
    @SerializedName("unread_count") val unreadCount: Int?
)

/**
 * 创建群组请求
 */
data class CreateGroupRequest(
    val name: String,
    val description: String?,
    @SerializedName("maxMembers") val maxMembers: Int?,
    @SerializedName("memberIds") val memberIds: List<Long>?
)

/**
 * 群组成员
 */
data class GroupMember(
    val id: Long?,
    @SerializedName("group_id") val groupId: Long?,
    @SerializedName("user_id") val userId: Long?,
    val role: String?,
    val nickname: String?,
    @SerializedName("joined_at") val joinedAt: String?,
    val user: User?
) {
    companion object {
        const val ROLE_MANAGER = "manager"  // 群管理员（有且只有1个）
        const val ROLE_MEMBER = "member"    // 普通成员
    }
}

/**
 * 消息
 */
data class Message(
    val id: Long?,
    @SerializedName("group_id") val groupId: Long?,
    @SerializedName("sender_id") val senderId: Long?,
    val content: String,
    @SerializedName("message_type") val messageType: String?,
    @SerializedName("created_at") val createdAt: String?,
    val sender: User?
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
        const val TYPE_SYSTEM = "system"
    }
}

