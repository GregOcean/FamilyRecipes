package com.familyrecipes.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IngredientCategoryRulesTest {
    @Test void compoundProductsDoNotBecomeTheirRawIngredients() {
        assertEquals("调味料", IngredientCategoryRules.suggest("番茄酱"));
        assertEquals("零食坚果", IngredientCategoryRules.suggest("牛奶巧克力"));
        assertEquals("米面主食", IngredientCategoryRules.suggest("牛奶面包"));
        assertEquals("米面主食", IngredientCategoryRules.suggest("手工饺子皮"));
        assertEquals("熟食速食", IngredientCategoryRules.suggest("牛肉水饺"));
        assertEquals("蛋奶", IngredientCategoryRules.suggest("有机鸡蛋"));
        assertEquals("蛋奶", IngredientCategoryRules.suggest("低脂牛奶"));
        assertEquals("待分类", IngredientCategoryRules.suggest("神秘礼盒"));
    }
}
