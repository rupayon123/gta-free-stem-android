package com.rupayonhaldar.gtafreestem.domain.validation

import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpportunityAvailabilityTest {
    @Test
    fun `distinct registration deadline archives before a later end date`() {
        val opportunity = opportunity(
            startDate = "2026-07-06T08:00:00-04:00",
            endDate = "2026-08-28T15:00:00-04:00",
            deadline = "2026-07-01T23:59:00-04:00",
        )

        assertTrue(OpportunityAvailability.hasDistinctRegistrationDeadline(opportunity))
        assertEquals(
            Instant.parse("2026-07-02T03:59:00Z"),
            OpportunityAvailability.archiveBoundary(opportunity),
        )
        assertTrue(
            OpportunityAvailability.isArchived(
                opportunity,
                Instant.parse("2026-08-16T16:00:00Z"),
            ),
        )
    }

    @Test
    fun `earlier end date wins over a later deadline`() {
        val opportunity = opportunity(
            startDate = "2026-07-01T12:00:00Z",
            endDate = "2026-07-15T20:00:00Z",
            deadline = "2026-08-01T20:00:00Z",
        )

        assertEquals(
            Instant.parse("2026-07-15T20:00:00Z"),
            OpportunityAvailability.archiveBoundary(opportunity),
        )
    }

    @Test
    fun `mirrored start deadline is ignored for an ongoing program`() {
        val opportunity = opportunity(
            startDate = "2026-06-20T04:00:00Z",
            endDate = "2026-09-01T03:59:59Z",
            deadline = "2026-06-20T04:00:00Z",
        )

        assertFalse(OpportunityAvailability.hasDistinctRegistrationDeadline(opportunity))
        assertEquals(
            Instant.parse("2026-09-01T03:59:59Z"),
            OpportunityAvailability.archiveBoundary(opportunity),
        )
        assertTrue(
            OpportunityAvailability.isCurrentlyAvailable(
                opportunity,
                Instant.parse("2026-08-16T16:00:00Z"),
            ),
        )
    }

    @Test
    fun `start date is the fallback when deadline and end are absent`() {
        val opportunity = opportunity(
            startDate = "2026-08-16",
            endDate = null,
            deadline = null,
        )

        assertFalse(
            OpportunityAvailability.isArchived(
                opportunity,
                Instant.parse("2026-08-17T03:59:59Z"),
            ),
        )
        assertTrue(
            OpportunityAvailability.isArchived(
                opportunity,
                Instant.parse("2026-08-17T04:00:00Z"),
            ),
        )
    }

    @Test
    fun `date-only end remains current through the GTA calendar day`() {
        val opportunity = opportunity(
            startDate = null,
            endDate = "2026-08-16",
            deadline = null,
        )

        assertTrue(
            OpportunityAvailability.isCurrentlyAvailable(
                opportunity,
                Instant.parse("2026-08-17T03:59:59Z"),
            ),
        )
        assertTrue(
            OpportunityAvailability.isArchived(
                opportunity,
                Instant.parse("2026-08-17T04:00:00Z"),
            ),
        )
    }

    @Test
    fun `inactive status archives immediately even without dates`() {
        val opportunity = opportunity(
            startDate = null,
            endDate = null,
            deadline = null,
            status = "cancelled",
        )

        assertEquals(Instant.MIN, OpportunityAvailability.archiveBoundary(opportunity))
        assertTrue(
            OpportunityAvailability.isArchived(
                opportunity,
                Instant.parse("2026-08-16T16:00:00Z"),
            ),
        )
    }

    @Test
    fun `unrepresentable date boundary is ignored without crashing`() {
        val opportunity = opportunity(
            startDate = null,
            endDate = "+999999999-12-31",
            deadline = null,
        )

        assertEquals(null, OpportunityAvailability.archiveBoundary(opportunity))
        assertTrue(
            OpportunityAvailability.isCurrentlyAvailable(
                opportunity,
                Instant.parse("2026-08-16T16:00:00Z"),
            ),
        )
    }

    private fun opportunity(
        startDate: String?,
        endDate: String?,
        deadline: String?,
        status: String = "active",
    ) = Opportunity(
        id = "program",
        title = "STEM program",
        organization = "Community Library",
        description = "A free hands-on program.",
        category = "STEM",
        categories = listOf("STEM"),
        city = "Toronto",
        region = "Toronto",
        startDate = startDate,
        endDate = endDate,
        deadline = deadline,
        ageMin = 8,
        ageMax = 18,
        cost = "Free",
        sourceUrl = "https://example.org/program",
        status = status,
    )
}
