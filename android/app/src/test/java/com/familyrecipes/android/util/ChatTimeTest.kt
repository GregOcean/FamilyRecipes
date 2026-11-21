package com.familyrecipes.android.util

import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class ChatTimeTest {
    private val singapore=ZoneId.of("Asia/Singapore")
    @Test fun serverUtcAndLegacyCachedUtcRenderInDeviceZone(){
        assertEquals("2026-11-19 23:57",ChatTime.label("2026-11-19T15:57:00Z",zone=singapore))
        assertEquals("2026-11-19 23:57",ChatTime.label("2026-11-19T15:57:00",zone=singapore))
        assertEquals("23:57",ChatTime.label("2026-11-19 15:57:00",false,singapore))
    }
    @Test fun explicitOffsetIsNotAppliedTwice(){
        assertEquals("2026-11-19 23:57",ChatTime.label("2026-11-19T23:57:00+08:00",zone=singapore))
        assertEquals("2026-11-19 10:57",ChatTime.label("2026-11-19T15:57:00Z",zone=ZoneId.of("America/New_York")))
    }
    @Test fun separatorsUseLocalCalendarAndElapsedTimeAcrossDaylightSaving(){
        assertTrue(ChatTime.separate("2026-11-19T15:59:00Z","2026-11-19T16:00:00Z",singapore))
        assertFalse(ChatTime.separate("2026-11-19T23:59:00Z","2026-11-20T00:00:00Z",singapore))
        assertTrue(ChatTime.separate("2026-11-01T05:57:00Z","2026-11-01T06:02:00Z",ZoneId.of("America/New_York")))
    }
    @Test fun missingAndMalformedTimeNeverCrashOrDisplayRawProtocolText(){
        assertEquals("",ChatTime.label(null,zone=singapore))
        assertEquals("",ChatTime.label("invalid",zone=singapore))
        assertNull(ChatTime.instant(""))
    }
}
