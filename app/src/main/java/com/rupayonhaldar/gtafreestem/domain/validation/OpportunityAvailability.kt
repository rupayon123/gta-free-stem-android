package com.rupayonhaldar.gtafreestem.domain.validation

import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

/** Shared browse/save availability policy matching the Apple app's archive behavior. */
object OpportunityAvailability {
    private val gtaTimeZone = ZoneId.of("America/Toronto")

    /**
     * A distinct registration deadline closes first, otherwise the declared end closes first.
     * Start is a fallback for one-day records without either of those values. Inactive records use
     * [Instant.MIN] so they are archived immediately and consistently.
     */
    fun archiveBoundary(
        opportunity: Opportunity,
        localZone: ZoneId = gtaTimeZone,
    ): Instant? {
        if (!opportunity.status.trim().equals("active", ignoreCase = true)) return Instant.MIN
        return activeArchiveBoundary(opportunity, localZone)
    }

    fun isCurrentlyAvailable(
        opportunity: Opportunity,
        now: Instant,
        localZone: ZoneId = gtaTimeZone,
    ): Boolean {
        if (!opportunity.status.trim().equals("active", ignoreCase = true)) return false
        val boundary = activeArchiveBoundary(opportunity, localZone) ?: return true
        return !boundary.isBefore(now)
    }

    fun isArchived(
        opportunity: Opportunity,
        now: Instant,
        localZone: ZoneId = gtaTimeZone,
    ): Boolean = !isCurrentlyAvailable(opportunity, now, localZone)

    fun hasDistinctRegistrationDeadline(
        opportunity: Opportunity,
        localZone: ZoneId = gtaTimeZone,
    ): Boolean = registrationDeadline(opportunity, localZone) != null

    /** Rejects listings whose effective archive boundary has passed, independent of status. */
    fun hasNotEnded(
        opportunity: Opportunity,
        now: Instant,
        localZone: ZoneId = gtaTimeZone,
    ): Boolean {
        val boundary = activeArchiveBoundary(opportunity, localZone) ?: return true
        return !boundary.isBefore(now)
    }

    private fun activeArchiveBoundary(opportunity: Opportunity, localZone: ZoneId): Instant? {
        val deadline = registrationDeadline(opportunity, localZone)
        val end = expiryInstant(opportunity.endDate, localZone)
        return listOfNotNull(deadline, end).minOrNull()
            ?: expiryInstant(opportunity.startDate, localZone)
    }

    private fun registrationDeadline(opportunity: Opportunity, localZone: ZoneId): Instant? {
        val deadline = expiryInstant(opportunity.deadline, localZone) ?: return null
        val start = expiryInstant(opportunity.startDate, localZone) ?: return deadline
        return deadline.takeUnless { it == start }
    }

    private fun expiryInstant(raw: String?, localZone: ZoneId): Instant? {
        val value = raw?.trim()?.takeIf(String::isNotEmpty) ?: return null
        runCatching { Instant.parse(value) }.getOrNull()?.let { return it }
        runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()?.let { return it }

        val date = runCatching { LocalDate.parse(value) }.getOrNull() ?: return null
        // Date-only values describe a GTA calendar day and expire at the next local midnight.
        return runCatching {
            date.plusDays(1).atStartOfDay(localZone).toInstant().minusNanos(1)
        }.getOrNull()
    }
}
