package com.familyrecipes.android.ui.meal

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object MealDefaults {
    fun mergeDishes(existing:List<com.familyrecipes.android.data.model.MealDish>,added:List<com.familyrecipes.android.data.model.MealDish>):List<com.familyrecipes.android.data.model.MealDish> {
        val merged=existing.toMutableList()
        added.forEach { item ->
            if(item.recipeId!=null) {
                if(merged.none { it.recipeId==item.recipeId }) {
                    val manual=merged.indexOfFirst { it.recipeId==null && it.name==item.name }
                    if(manual>=0)merged[manual]=item else merged.add(item)
                }
            } else if(merged.none { it.name==item.name }) merged.add(item)
        }
        return merged
    }

    data class Next(val date: LocalDate,val meal:String)
    fun next(now:LocalDateTime):Next {
        val time=now.toLocalTime()
        return when {
            time<LocalTime.of(7,0)->Next(now.toLocalDate(),"早餐")
            time<LocalTime.of(12,0)->Next(now.toLocalDate(),"午餐")
            time<LocalTime.of(18,0)->Next(now.toLocalDate(),"晚餐")
            else->Next(now.toLocalDate().plusDays(1),"早餐")
        }
    }
}
