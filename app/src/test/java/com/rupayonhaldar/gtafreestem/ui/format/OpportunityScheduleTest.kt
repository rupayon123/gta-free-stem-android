package com.rupayonhaldar.gtafreestem.ui.format

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class OpportunityScheduleTest {
    private val locale = Locale.US
    @Test fun novemberUtcRangeUsesTorontoStandardTime() {
        val value = OpportunitySchedule.range("2026-11-01T19:00:00Z", "2026-11-01T20:00:00Z", locale)!!
        assertTrue(value, value.contains("2:00 PM–3:00 PM EST"))
        assertTrue(value, value.contains("Nov 1, 2026"))
    }
    @Test fun dstRepeatedHourRetainsBothOffsets() {
        val value = OpportunitySchedule.range("2026-11-01T05:30:00Z", "2026-11-01T06:30:00Z", locale)!!
        assertTrue(value, value.contains("EDT"))
        assertTrue(value, value.contains("EST"))
    }
    @Test fun dateOnlyAndMissingSchedulesNeverGainTimes() {
        assertEquals("Nov 1, 2026", OpportunitySchedule.range("2026-11-01", "2026-11-01", locale))
        assertNull(OpportunitySchedule.range(null, null, locale))
        assertEquals("Nov 2, 2026", OpportunitySchedule.range(null, "2026-11-02", locale))
    }
    @Test fun mirroredDeadlineUsesInstantEqualityRatherThanRawText() {
        assertFalse(OpportunitySchedule.hasDistinctDeadline("2026-11-01T19:00:00Z", "2026-11-01T14:00:00-05:00"))
        assertFalse(OpportunitySchedule.hasDistinctDeadline("2026-11-01", "2026-11-01"))
        assertTrue(OpportunitySchedule.hasDistinctDeadline("2026-11-01T19:00:00Z", "2026-10-31T19:00:00Z"))
        assertFalse(OpportunitySchedule.hasDistinctDeadline(null, null))
    }
}
