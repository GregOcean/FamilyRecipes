package com.familyrecipes.android.util

import org.junit.Assert.*
import org.junit.Test

class RecipeEditorRulesTest {
    @Test fun cuisineRoundTripKeepsIndependentLabels() {
        assertEquals(listOf("家常菜", "东北菜"), RecipeEditorRules.labels("家常菜、东北菜"))
        assertEquals(listOf("Home style", "Thai"), RecipeEditorRules.labels("Home style, Thai; home style\n待确认"))
    }
    @Test fun albumDoesNotRepeatCoverOrStepPhotos() {
        assertEquals(listOf("other"), RecipeEditorRules.galleryUrls("cover", listOf("step", null),
            listOf("cover", "step", "other", "other")))
    }
}
