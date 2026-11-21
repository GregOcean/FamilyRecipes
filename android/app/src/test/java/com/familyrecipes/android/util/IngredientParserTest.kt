package com.familyrecipes.android.util
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
class IngredientParserTest {
    private val today=LocalDate.of(2026,11,19)
    @Test fun preservesNamesAndQuantities(){
        for((raw,name) in mapOf("猪后腿肉500克7天后过期" to "猪后腿肉","胡萝卜500克" to "胡萝卜","鱼香茄子1盒" to "鱼香茄子","鸡肉丸500克" to "鸡肉丸","FairPrice Full Cream Milk 2 cartons" to "FairPrice Full Cream Milk"))assertEquals(name,IngredientParser.parse(raw,today).ingredientName)
        assertEquals("2L",IngredientParser.parse("牛奶2L",today).amount)
    }
    @Test fun datesDoNotBecomeQuantities(){
        assertEquals("2026-11-20",IngredientParser.parse("牛奶2026年11月20日到期",today).expiryDate)
        val parsed=IngredientParser.parse("牛奶两个月后过期",today)
        assertEquals(60,parsed.expiryDays);assertNull(parsed.amount)
    }
    @Test fun noMadeUpExpiryOrDroppedSecondFood(){
        assertEquals(0,IngredientParser.parse("鸡胸肉300克",today).expiryDays)
        assertEquals("鸡蛋2个牛奶1瓶",IngredientParser.parse("鸡蛋2个牛奶1瓶",today).ingredientName)
    }
}
