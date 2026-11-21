package com.familyrecipes.android.data.local

/** Stable order and retry classification are independent of SQLite row IDs and display text. */
object DeliveryPolicy {
    fun ordered(items:List<PendingMessage>)=items.sortedBy{it.sequence}
    fun failureKind(status: Int): String = when(status) {
        401 -> "auth"
        403,404 -> "unavailable"
        408,429 -> "temporary"
        in 500..599 -> "temporary"
        else -> "invalid"
    }
    fun retryDelay(attempt: Int): Long = (5_000L shl (attempt.coerceIn(1,7)-1)).coerceAtMost(300_000)
    fun canAutoSend(state: String?, nextAttemptAt: Long, now: Long): Boolean =
        state !in setOf("failed","auth","unavailable") && nextAttemptAt <= now
}
