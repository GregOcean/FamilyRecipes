package com.familyrecipes.android.worker
import org.junit.Assert.*
import org.junit.Test
class ReminderPolicyTest {
    @Test fun respectsConfiguredTimeAndDailyCap() {
        assertFalse(ReminderPolicy.due(true,"17:00:00",1019,"2026-11-01",null))
        assertTrue(ReminderPolicy.due(true,"17:00:00",1020,"2026-11-01",null))
        assertTrue(ReminderPolicy.due(true,"17:00:00",1300,"2026-11-01",null))
        assertFalse(ReminderPolicy.due(true,"17:00:00",1300,"2026-11-01","2026-11-01"))
        assertTrue(ReminderPolicy.due(true,"17:00:00",1300,"2026-11-02","2026-11-01"))
    }
    @Test fun disabledAndInvalidSettingsNeverNotify() {
        assertFalse(ReminderPolicy.due(false,"17:00:00",1300,"today",null))
        assertFalse(ReminderPolicy.due(true,"24:00:00",1300,"today",null))
        assertFalse(ReminderPolicy.due(true,"broken",1300,"today",null))
    }
}
