package com.familyrecipes.android.util
import org.junit.Assert.assertEquals
import org.junit.Test
class RecipeTextTest {
    @Test fun removesWikiPresentationButKeepsMeasurements() {
        assertEquals("汤类\n\n1/2 cup · ¼ tsp · 180°C",RecipeText.clean("__notoc__\n| 汤类\n\n\n1/2 cup · ¼ tsp · 180°C"))
        assertEquals("__UNKNOWN__ | 1–2 tbsp",RecipeText.clean("__UNKNOWN__ | 1–2 tbsp"))
    }
}
