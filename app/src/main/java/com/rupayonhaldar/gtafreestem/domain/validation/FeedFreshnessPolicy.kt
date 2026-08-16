package com.rupayonhaldar.gtafreestem.domain.validation

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

object FeedFreshnessPolicy {
    const val MAXIMUM_REMOTE_AGE_DAYS: Long = 14
    val maximumRemoteAge: Duration = Duration.ofDays(MAXIMUM_REMOTE_AGE_DAYS)

    fun isFresh(updatedAt: Instant, now: Instant): Boolean {
        val age = Duration.between(updatedAt, now)
        return !age.isNegative && age <= maximumRemoteAge
    }

    fun parse(value: String?): Instant? {
        val normalized = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
        return runCatching { Instant.parse(normalized) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(normalized).toInstant() }.getOrNull()
            ?: runCatching {
                LocalDate.parse(normalized).atStartOfDay().toInstant(ZoneOffset.UTC)
            }.getOrNull()
    }
}
