package com.familyrecipes.android.ui.meal
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class MealDefaultsTest {
    @Test fun keepsSameNameDifferentRecipesAndDoesNotDuplicateRenamedRecipe() {
        val first=com.familyrecipes.android.data.model.MealDish(1,"汤")
        val second=com.familyrecipes.android.data.model.MealDish(2,"汤")
        val merged=MealDefaults.mergeDishes(listOf(first.copy(name="我的汤")),listOf(first,second))
        assertEquals(listOf(first.copy(name="我的汤"),second),merged)
    }
    @Test fun selectingRecipeLinksExistingManualDish() {
        val recipe=com.familyrecipes.android.data.model.MealDish(8,"米饭")
        assertEquals(listOf(recipe),MealDefaults.mergeDishes(listOf(com.familyrecipes.android.data.model.MealDish(name="米饭")),listOf(recipe)))
    }

    @Test fun picksUpcomingMealAtBoundaries() {
        for((time,meal) in listOf("06:59" to "早餐","07:00" to "午餐","11:59" to "午餐","12:00" to "晚餐","17:59" to "晚餐")) {
            val next=MealDefaults.next(LocalDateTime.parse("2026-11-08T$time"))
            assertEquals(meal,next.meal);assertEquals("2026-11-08",next.date.toString())
        }
    }
    @Test fun rollsOverMonthAndYearForNextBreakfast() {
        val next=MealDefaults.next(LocalDateTime.parse("2026-12-31T18:00"))
        assertEquals("早餐",next.meal);assertEquals("2027-01-01",next.date.toString())
    }
}
