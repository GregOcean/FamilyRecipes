package com.familyrecipes.android.ui.meal

internal object MealDraftScope {
    fun matches(draftKind:String?,draftKitchen:Long?,kind:String,kitchen:Long?):Boolean =
        (draftKind ?: "order")==kind && (draftKitchen==null || draftKitchen==kitchen)
}
