package com.rupayonhaldar.gtafreestem.domain.validation

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedFreshnessPolicyTest {
    @Test
    fun `parses internet timestamps offsets and date-only values`() {
        assertEquals(
            Instant.parse("2026-08-15T12:30:00Z"),
            FeedFreshnessPolicy.parse("2026-08-15T12:30:00Z"),
        )
        assertEquals(
            Instant.parse("2026-08-15T16:30:00Z"),
            FeedFreshnessPolicy.parse("2026-08-15T12:30:00-04:00"),
        )
        assertEquals(
            Instant.parse("2026-08-15T00:00:00Z"),
            FeedFreshnessPolicy.parse("2026-08-15"),
        )
        assertNull(FeedFreshnessPolicy.parse("not-a-date"))
    }

    @Test
    fun `accepts exact fourteen-day boundary and rejects older or future dates`() {
        val now = Instant.parse("2026-08-16T00:00:00Z")
        assertTrue(FeedFreshnessPolicy.isFresh(Instant.parse("2026-08-02T00:00:00Z"), now))
        assertFalse(FeedFreshnessPolicy.isFresh(Instant.parse("2026-08-01T23:59:59Z"), now))
        assertFalse(FeedFreshnessPolicy.isFresh(Instant.parse("2026-08-16T00:00:01Z"), now))
    }
}
