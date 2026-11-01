package com.familyrecipes.android.worker
/** At most once per local day; this is deliberately not an exact alarm. */
object ReminderPolicy {
    fun due(enabled:Boolean, scheduled:String, minuteOfDay:Int, today:String, lastDay:String?):Boolean {
        val parts=scheduled.split(":")
        val hour=parts.getOrNull(0)?.toIntOrNull() ?: return false
        val minute=parts.getOrNull(1)?.toIntOrNull() ?: return false
        if(hour !in 0..23 || minute !in 0..59 || minuteOfDay !in 0..1439)return false
        return enabled && today!=lastDay && minuteOfDay>=hour*60+minute
    }
}
