package com.familyrecipes.android.worker
/** 48 小时内的到期提醒最多每 3 天一次，避免每天打扰。 */
object ReminderPolicy {
    fun due(enabled:Boolean, scheduled:String, minuteOfDay:Int, today:String, lastDay:String?):Boolean {
        val parts=scheduled.split(":")
        val hour=parts.getOrNull(0)?.toIntOrNull() ?: return false
        val minute=parts.getOrNull(1)?.toIntOrNull() ?: return false
        if(hour !in 0..23 || minute !in 0..59 || minuteOfDay !in 0..1439)return false
        if(!enabled || minuteOfDay<hour*60+minute)return false
        if(lastDay==null)return true
        return runCatching {
            java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.parse(lastDay), java.time.LocalDate.parse(today)) >= 3
        }.getOrDefault(true)
    }
}

