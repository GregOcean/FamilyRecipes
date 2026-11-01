package com.familyrecipes.android.util

import com.familyrecipes.android.data.model.InventoryCategory

object InventoryCategories {
    fun descendants(id: Long, categories: List<InventoryCategory>): Set<Long> {
        fun flatten(nodes: List<InventoryCategory>): List<InventoryCategory> =
            nodes.flatMap { listOf(it) + flatten(it.children.orEmpty()) }
        val all = flatten(categories)
        val result = mutableSetOf(id)
        var changed: Boolean
        do {
            changed = false
            all.forEach { if (it.parentId in result && result.add(it.id)) changed = true }
        } while (changed)
        return result
    }
}
