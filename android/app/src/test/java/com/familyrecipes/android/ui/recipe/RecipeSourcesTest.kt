package com.familyrecipes.android.ui.recipe

import org.junit.Assert.*
import org.junit.Test

class RecipeSourcesTest {
    @Test fun preservesPersonalEntryPoints() {
        assertEquals("created",RecipeSources.fromLegacy("mine","created",false))
        assertEquals("favorites",RecipeSources.fromLegacy("mine","favorites",false))
        assertEquals("known",RecipeSources.fromLegacy("mine","all",true))
        assertEquals("created,known,favorites,shared",RecipeSources.fromLegacy("mine","all",false))
        assertEquals("",RecipeSources.fromLegacy("discovery","all",false))
    }
}
