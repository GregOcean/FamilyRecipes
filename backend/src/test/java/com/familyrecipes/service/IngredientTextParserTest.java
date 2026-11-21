package com.familyrecipes.service;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
class IngredientTextParserTest {
    private final LocalDate today=LocalDate.of(2026,11,19);
    @Test void preservesCompoundFoodsAndFullEnglishProductNames(){
        assertEquals("胡萝卜",IngredientTextParser.parse("胡萝卜500克",today).name());
        assertEquals("鱼香茄子",IngredientTextParser.parse("鱼香茄子1盒",today).name());
        assertEquals("鸡肉丸",IngredientTextParser.parse("鸡肉丸500克",today).name());
        assertEquals("猪后腿肉",IngredientTextParser.parse("猪后腿肉500克7天后过期",today).name());
        var english=IngredientTextParser.parse("FairPrice Full Cream Milk 2 cartons",today);
        assertEquals("FairPrice Full Cream Milk",english.name());assertEquals("2 cartons",english.amount());
    }
    @Test void datesAreParsedBeforeAmounts(){
        var absolute=IngredientTextParser.parse("牛奶2026年11月20日到期",today);
        assertEquals(LocalDate.of(2026,11,20),absolute.expiryDate());assertNull(absolute.amount());assertEquals("牛奶",absolute.name());
        var relative=IngredientTextParser.parse("牛奶两个月后过期",today);assertEquals(60,relative.days());assertNull(relative.amount());
        assertEquals("2L",IngredientTextParser.parse("牛奶2L",today).amount());
    }
    @Test void unknownExpiryStaysUnknownAndAmbiguousMultipleFoodsAreNotTruncated(){
        assertNull(IngredientTextParser.parse("鸡胸肉300克",today).days());
        assertEquals("鸡蛋2个牛奶1瓶",IngredientTextParser.parse("鸡蛋2个牛奶1瓶",today).name());
        assertEquals("2个",IngredientTextParser.parse("鸡蛋 | 2个",today).amount());
        assertEquals("盐",IngredientTextParser.parse("盐适量",today).name());
        assertEquals("菠菜",IngredientTextParser.parse("菠菜1把",today).name());
        assertEquals("salt",IngredientTextParser.parse("salt to taste",today).name());
        assertEquals("盐",IngredientTextParser.parse("• 盐½茶匙",today).name());
        assertEquals("salt",IngredientTextParser.parse("salt 1 1/2 tsp",today).name());
    }
    @Test void storageAffectsEstimatesAndUnknownsRemainNullable(){
        assertEquals(3,IngredientShelfLife.estimate("鸡胸肉","冰箱冷藏"));
        assertEquals(30,IngredientShelfLife.estimate("鸡胸肉","冷冻"));
        assertNull(IngredientShelfLife.estimate("鸡胸肉","常温"));
        assertNull(IngredientShelfLife.estimate("某品牌不明食品","冷藏"));
        assertEquals("冷藏",IngredientShelfLife.storage("Refrigerated"));
    }
}
