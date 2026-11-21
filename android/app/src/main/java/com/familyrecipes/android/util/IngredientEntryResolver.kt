package com.familyrecipes.android.util
import com.familyrecipes.android.data.model.*
import com.familyrecipes.android.data.remote.ApiClient
import com.familyrecipes.android.ui.design.Ui

object IngredientEntryResolver {
    suspend fun resolveRecipeLines(text:String,existing:List<RecipeIngredient> = emptyList()):List<RecipeIngredient> {
        val account=com.familyrecipes.android.data.local.LocalStore.account
        val lines=text.lines().map{it.trim()}.filter{it.isNotEmpty()}
        if(lines.isEmpty())return emptyList()
        val known=existing.associateBy { "${it.ingredient?.name.orEmpty()} | ${it.amount.orEmpty()}".trim() }
        val entries=lines.map { line ->
            val previous=known[line]
            IngredientEntry(line,previous?.ingredientId,previous?.amount)
        }
        return Ui.data(ApiClient.getAccountService(account).resolveIngredientEntries(IngredientEntriesRequest(entries,true)))
            .map { row->RecipeIngredient(null,null,requireNotNull(row.ingredient?.id),row.amount,true,row.ingredient) }.distinctBy{it.ingredientId}
    }
}
