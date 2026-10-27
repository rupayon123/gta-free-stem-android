package com.rupayonhaldar.gtafreestem.domain.validation

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class PublicationRevisionPolicyTest {
    private val now = Instant.parse("2026-08-16T00:00:00Z")
    private val day = Instant.parse("2026-08-15T00:00:00Z")
    private val old = Instant.parse("2026-08-15T16:26:56.721Z")
    private val fresh = Instant.parse("2026-08-15T20:55:11.491Z")

    @Test fun `ordering keeps older equal missing and conflicting revised publications out`() {
        for (incoming in listOf(null, old, fresh)) {
            assertFalse(PublicationRevisionPolicy.shouldReplace(day, incoming, day, fresh))
        }
        assertTrue(PublicationRevisionPolicy.shouldReplace(day, fresh, day, old))
    }

    @Test fun `legacy migration preserves real same day corrections and stable bootstrap ties`() {
        assertTrue(PublicationRevisionPolicy.shouldReplace(day, null, day, null))
        assertFalse(PublicationRevisionPolicy.shouldReplace(day, null, day, null, false))
        assertTrue(PublicationRevisionPolicy.shouldReplace(day, fresh, day, null))
        assertFalse(PublicationRevisionPolicy.shouldReplace(day, null, day, fresh))
    }

    @Test fun `neither publication order nor data date can bypass the other`() {
        assertFalse(PublicationRevisionPolicy.shouldReplace(day.minusSeconds(86400), fresh, day, old))
        assertFalse(PublicationRevisionPolicy.shouldReplace(day.plusSeconds(86400), old, day, fresh))
        assertFalse(PublicationRevisionPolicy.shouldReplace(day.plusSeconds(86400), null, day, fresh))
        assertTrue(PublicationRevisionPolicy.shouldReplace(day.plusSeconds(86400), fresh, day, old))
    }

    @Test fun `strict UTC revisions reject malformed and future values without poisoning retained data`() {
        assertEquals(fresh, PublicationRevisionPolicy.parse("2026-08-15T20:55:11.491Z", now))
        assertEquals(now.plusSeconds(300), PublicationRevisionPolicy.parse("2026-08-16T00:05:00.000Z", now))
        for (raw in listOf("", "2026-08-15", "2026-08-15T20:55:11Z", "2026-08-15T20:55:11.491+00:00",
            "2026-08-16T00:05:00.001Z", "2026-02-30T00:00:00.000Z", "1970-01-01T00:00:00.000Z")) {
            assertNull(raw, PublicationRevisionPolicy.parse(raw, now))
        }
    }
}
