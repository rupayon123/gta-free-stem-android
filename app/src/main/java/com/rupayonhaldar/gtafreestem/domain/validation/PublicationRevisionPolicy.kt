package com.rupayonhaldar.gtafreestem.domain.validation

import java.time.Instant
import java.time.format.DateTimeFormatterBuilder

/** Publication order is independent of the event-data freshness date. */
internal object PublicationRevisionPolicy {
    private val format = DateTimeFormatterBuilder().appendInstant(3).toFormatter()
    private val shape = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}Z")

    fun parse(raw: String, now: Instant): Instant? {
        if (!shape.matches(raw)) return null
        val value = runCatching { Instant.parse(raw) }.getOrNull() ?: return null
        if (value <= Instant.EPOCH || value > now.plusSeconds(300)) return null
        return value.takeIf { format.format(it) == raw }
    }

    fun shouldReplace(
        candidateDate: Instant,
        candidateRevision: Instant?,
        retainedDate: Instant,
        retainedRevision: Instant?,
        allowLegacyEqualDate: Boolean = true,
    ): Boolean {
        if (candidateDate < retainedDate) return false
        if (retainedRevision != null) {
            // A revision identifies an immutable publication. Equal revisions
            // retain the accepted bytes, including when another host disagrees.
            return candidateRevision != null && candidateRevision > retainedRevision
        }
        if (candidateRevision != null) return true
        return candidateDate > retainedDate || allowLegacyEqualDate
    }
}
