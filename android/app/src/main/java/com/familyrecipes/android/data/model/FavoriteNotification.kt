package com.familyrecipes.android.data.model

data class FavoriteNotification(val id:Long,val recipeId:Long,val actorName:String,val recipeName:String,val createdAt:String,val isRead:Boolean)
data class FavoriteNotificationPage(val items:List<FavoriteNotification> = emptyList(),val unread:Int=0)
