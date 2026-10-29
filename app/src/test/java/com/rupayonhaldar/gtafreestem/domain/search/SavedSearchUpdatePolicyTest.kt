package com.rupayonhaldar.gtafreestem.domain.search

import com.rupayonhaldar.gtafreestem.data.local.SavedSearchEntry
import com.rupayonhaldar.gtafreestem.data.local.SavedSearchSummary
import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSnapshot
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSource
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedSearchUpdatePolicyTest {
    @Test
    fun `new stable ID counts once until viewed and never reappears as new`() {
        val initial = search("age-4", age = 4).copy(
            knownMatchingIds = listOf("A", "B"), hasBaseline = true,
            lastProcessedRevisionEpochMillis = 1_000L,
        )
        val added = summarize(initial, listOf("A", "B", "C"), healthy(2_000L))
        assertEquals(3, added.matchCount)
        assertEquals(1, added.newCount)
        assertEquals(listOf("C"), added.entry.unreadMatchingIds)

        val sameIds = summarize(added.entry, listOf("A", "B", "C"), healthy(3_000L))
        assertEquals(1, sameIds.newCount)
        val viewed = sameIds.entry.copy(unreadMatchingIds = emptyList())
        assertEquals(0, summarize(viewed, listOf("A", "B", "C"), healthy(4_000L)).newCount)

        val removed = summarize(sameIds.entry, listOf("A", "B"), healthy(4_000L))
        assertEquals(0, removed.newCount)
        assertTrue("C" in removed.entry.knownMatchingIds)
        assertTrue(removed.entry.unreadMatchingIds.isEmpty())
        val returned = summarize(removed.entry, listOf("A", "B", "C"), healthy(5_000L))
        assertEquals(0, returned.newCount)
        val next = summarize(returned.entry, listOf("A", "B", "C", "D"), healthy(6_000L))
        assertEquals(1, next.newCount)
        assertEquals(listOf("D"), next.entry.unreadMatchingIds)
    }

    @Test
    fun `pending and edited searches establish quiet baseline on first healthy catalog`() {
        val pending = search("age-16", age = 16)
        val preview = summarize(pending, listOf("A"), partial(1_000L))
        assertFalse(preview.entry.hasBaseline)
        assertEquals(0, preview.newCount)
        val firstHealthy = summarize(preview.entry, listOf("A", "B"), healthy(2_000L))
        assertTrue(firstHealthy.entry.hasBaseline)
        assertEquals(listOf("A", "B"), firstHealthy.entry.knownMatchingIds)
        assertEquals(0, firstHealthy.newCount)
        val edited = firstHealthy.entry.copy(
            filters = OpportunitySearchFilters(age = 10),
            knownMatchingIds = emptyList(), unreadMatchingIds = emptyList(),
            hasBaseline = false, lastProcessedRevisionEpochMillis = null,
        )
        val rebased = summarize(edited, listOf("X"), healthy(3_000L))
        assertEquals(0, rebased.newCount)
        assertEquals(listOf("X"), rebased.entry.knownMatchingIds)
    }

    @Test
    fun `failed empty partial and old revisions keep previous count and unread set`() {
        val entry = search("one", age = 4).copy(
            knownMatchingIds = listOf("A", "B"), unreadMatchingIds = listOf("B"),
            hasBaseline = true, lastProcessedRevisionEpochMillis = 2_000L,
        )
        val previous = SavedSearchSummary(entry, matchCount = 2, newCount = 1)
        val old = SavedSearchUpdatePolicy.summarize(
            listOf(entry), mapOf(entry.id to listOf("A", "B", "C")), healthy(1_000L),
            mapOf(entry.id to previous),
        ).single()
        assertEquals(entry, old.entry)
        assertEquals(2, old.matchCount)
        assertEquals(1, old.newCount)
        assertEquals(null, summarize(entry, listOf("A", "B", "C"), healthy(1_000L)).matchCount)
        val invalid = SavedSearchUpdatePolicy.summarize(
            listOf(entry), mapOf(entry.id to emptyList()), partial(3_000L),
            mapOf(entry.id to previous),
        ).single()
        assertEquals(entry, invalid.entry)
        assertEquals(2, invalid.matchCount)
        assertEquals(1, invalid.newCount)
        val stale = SavedSearchUpdatePolicy.summarize(
            listOf(entry), mapOf(entry.id to emptyList()), healthy(3_000L).copy(isStale = true),
            mapOf(entry.id to previous),
        ).single()
        assertEquals(entry, stale.entry)
        assertEquals(2, stale.matchCount)
        assertEquals(1, stale.newCount)
        val restarted = SavedSearchUpdatePolicy.summarize(
            listOf(entry.copy(lastKnownMatchCount = 2)),
            mapOf(entry.id to emptyList()), partial(3_000L),
        ).single()
        assertEquals(2, restarted.matchCount)
        assertEquals(1, restarted.newCount)
    }

    @Test
    fun `separate saved searches never share unread state`() {
        val first = search("first", age = 4).copy(knownMatchingIds = listOf("A"), hasBaseline = true)
        val second = search("second", age = 10).copy(knownMatchingIds = listOf("B"), hasBaseline = true)
        val third = search("third", age = 16).copy(knownMatchingIds = listOf("C"), hasBaseline = true)
        val summaries = SavedSearchUpdatePolicy.summarize(
            listOf(first, second, third),
            mapOf("first" to listOf("A", "D"), "second" to listOf("B"), "third" to listOf("C", "E")),
            healthy(2_000L),
        )
        assertEquals(listOf(1, 0, 1), summaries.map(SavedSearchSummary::newCount))
    }

    @Test
    fun `language change quietly rebases existing matches before counting later arrivals`() {
        val initial = search("language", age = 10).copy(
            knownMatchingIds = listOf("A"), hasBaseline = true,
            lastProcessedRevisionEpochMillis = 1_000L,
            lastKnownMatchCount = 1,
        )
        val switched = SavedSearchUpdatePolicy.summarize(
            listOf(initial), mapOf(initial.id to listOf("A", "B")), healthy(1_000L),
            quietRebaseline = true,
        ).single()
        assertEquals(2, switched.matchCount)
        assertEquals(0, switched.newCount)
        assertEquals(listOf("A", "B"), switched.entry.knownMatchingIds)

        val unchanged = summarize(switched.entry, listOf("A", "B"), healthy(2_000L))
        assertEquals(0, unchanged.newCount)
        val arrival = summarize(unchanged.entry, listOf("A", "B", "C"), healthy(3_000L))
        assertEquals(1, arrival.newCount)
    }

    private fun search(id: String, age: Int) = SavedSearchEntry(
        id = id, name = id, query = "", filters = OpportunitySearchFilters(age = age),
        knownMatchingIds = emptyList(),
    )

    private fun summarize(entry: SavedSearchEntry, ids: List<String>, snapshot: OpportunityFeedSnapshot) =
        SavedSearchUpdatePolicy.summarize(listOf(entry), mapOf(entry.id to ids), snapshot).single()

    private fun healthy(revision: Long) = OpportunityFeedSnapshot(
        opportunities = listOf(opportunity("fixture")), lastUpdated = Instant.ofEpochMilli(revision),
        source = OpportunityFeedSource.PRIMARY_NETWORK, declaredRecordCount = 1,
        isStale = false, publicationRevision = Instant.ofEpochMilli(revision),
        validatedCatalog = true, acceptedComplete = true,
    )

    private fun partial(revision: Long) = healthy(revision).copy(
        source = OpportunityFeedSource.LAST_GOOD_CACHE,
        validatedCatalog = false, acceptedComplete = false,
        opportunities = emptyList(),
    )

    private fun opportunity(id: String) = Opportunity(
        id = id, title = "Fixture", organization = "Fixture Library",
        description = "Fixture only", category = "Science & Engineering",
        city = "Toronto", region = "Toronto", cost = "Free",
        sourceUrl = "https://example.org/$id",
    )
}
