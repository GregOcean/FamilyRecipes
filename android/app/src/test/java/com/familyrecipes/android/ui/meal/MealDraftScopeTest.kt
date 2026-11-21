package com.familyrecipes.android.ui.meal

import org.junit.Assert.*
import org.junit.Test

class MealDraftScopeTest {
    @Test fun separatesKindsAndKitchens() {
        assertTrue(MealDraftScope.matches("order",1,"order",1))
        assertTrue(MealDraftScope.matches("prep",1,"prep",1))
        assertFalse(MealDraftScope.matches("prep",1,"order",1))
        assertFalse(MealDraftScope.matches("order",2,"order",1))
        assertFalse(MealDraftScope.matches("order",1,"order",null))
    }
    @Test fun keepsLegacyDraftsAccessible() {
        assertTrue(MealDraftScope.matches(null,null,"order",1))
        assertFalse(MealDraftScope.matches(null,null,"prep",1))
    }
}
