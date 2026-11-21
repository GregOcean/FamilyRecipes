package com.familyrecipes.android.data.model
import com.google.gson.annotations.SerializedName

data class IngredientEntry(val text:String,@SerializedName("ingredient_id") val ingredientId:Long?=null,val amount:String?=null)
data class IngredientEntriesRequest(val entries:List<IngredientEntry>,@SerializedName("create_unmatched") val createUnmatched:Boolean=false,val storage:String?=null)
data class IngredientResolution(val ingredient:Ingredient?,val name:String,val amount:String?,val storage:String?,@SerializedName("estimated_days") val estimatedDays:Int?,@SerializedName("expiry_date") val expiryDate:String?,val matched:Boolean,@SerializedName("match_kind") val matchKind:String,val candidates:List<Ingredient> = emptyList())
data class IngredientEstimateRequest(@SerializedName("ingredient_id")val ingredientId:Long?=null,val name:String?=null,val storage:String?=null)
data class IngredientEstimate(@SerializedName("estimated_days")val estimatedDays:Int?,val storage:String?)
