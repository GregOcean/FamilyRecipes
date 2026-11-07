package com.familyrecipes.android.data.local

import com.familyrecipes.android.data.model.Recipe
import com.google.gson.Gson

/** Only successful recipe-detail reads, isolated by account and API server. */
object RecipeHistory {
    data class Entry(val id:Long,val name:String,val viewedAt:Long)
    suspend fun record(account:String,recipe:Recipe){
        val id=recipe.id?:return
        val dao=LocalStore.instance.records()
        dao.put(LocalRecord(account,"recipe_history",id.toString(),Gson().toJson(Entry(id,recipe.name,System.currentTimeMillis()))))
        dao.list(account,"recipe_history").dropLast(200).forEach{dao.delete(account,it.kind,it.key)}
    }
    suspend fun list(account:String)=LocalStore.instance.records().list(account,"recipe_history").mapNotNull{runCatching{Gson().fromJson(it.json,Entry::class.java)}.getOrNull()}.sortedByDescending{it.viewedAt}
    suspend fun clear(account:String){LocalStore.instance.records().list(account,"recipe_history").forEach{LocalStore.instance.records().delete(account,it.kind,it.key)}}
}
