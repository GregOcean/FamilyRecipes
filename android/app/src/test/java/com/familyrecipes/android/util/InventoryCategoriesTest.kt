package com.familyrecipes.android.util

import com.familyrecipes.android.data.model.InventoryCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryCategoriesTest {
    private fun category(id: Long, parent: Long? = null, children: List<InventoryCategory>? = null) =
        InventoryCategory(id, "test", parent, null, 0, null, null, children)
    @Test fun parentIncludesChildrenAndGrandchildrenWithoutIncludingOtherCategories() {
        val tree = listOf(category(1, children=listOf(category(2, 1, listOf(category(3, 2))))), category(4))
        assertEquals(setOf(1L,2L,3L), InventoryCategories.descendants(1, tree))
        assertEquals(setOf(2L,3L), InventoryCategories.descendants(2, tree))
        assertEquals(setOf(4L), InventoryCategories.descendants(4, tree))
    }
}
