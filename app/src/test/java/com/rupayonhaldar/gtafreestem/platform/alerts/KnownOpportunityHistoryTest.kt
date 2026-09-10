package com.rupayonhaldar.gtafreestem.platform.alerts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KnownOpportunityHistoryTest {
    @Test
    fun mergeCountsOnlyNormalizedUniqueNewIds() {
        val result = KnownOpportunityHistory.merge(
            currentIds = listOf(" current-a ", "current-a", "current-b", ""),
            previousIds = listOf("current-a", "historical"),
        )

        assertEquals(1, result.newCount)
        assertEquals(listOf("current-a", "current-b", "historical"), result.retainedIds)
    }

    @Test
    fun currentFeedSurvivesHistoryCapAndRelaunch() {
        val historical = (0 until 2_600).map { "historical-$it" }
        val current = listOf("current-a", "current-b")

        val first = KnownOpportunityHistory.merge(current, historical + current)
        assertEquals(0, first.newCount)
        assertEquals(KnownOpportunityHistory.MAXIMUM_COUNT, first.retainedIds.size)
        assertTrue(first.retainedIds.containsAll(current))

        val relaunched = KnownOpportunityHistory.merge(current.reversed(), first.retainedIds.reversed())
        assertEquals(0, relaunched.newCount)
        assertTrue(relaunched.retainedIds.containsAll(current))
    }

    @Test
    fun overlongUntrustedIdsAreNotPersistedOrCounted() {
        val unsafe = "x".repeat(KnownOpportunityHistory.MAXIMUM_ID_LENGTH + 1)
        val result = KnownOpportunityHistory.merge(listOf(unsafe, "safe"), emptyList())

        assertEquals(1, result.newCount)
        assertEquals(listOf("safe"), result.retainedIds)
    }

    @Test
    fun feedTailBeyondSafetyCapDoesNotRepeatAsNewForever() {
        val oversizedFeed = (0 until 2_600).map { "current-$it" }

        val first = KnownOpportunityHistory.merge(oversizedFeed, emptyList())
        val second = KnownOpportunityHistory.merge(oversizedFeed, first.retainedIds)

        assertEquals(KnownOpportunityHistory.MAXIMUM_COUNT, first.newCount)
        assertEquals(KnownOpportunityHistory.MAXIMUM_COUNT, first.retainedIds.size)
        assertEquals(0, second.newCount)
    }
}
