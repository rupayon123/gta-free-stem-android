package com.rupayonhaldar.gtafreestem.data.local

import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSnapshot
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSource
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteOpportunityStoreTest {
    private val savedAt = Instant.parse("2026-07-01T14:00:00Z")
    private val reconciledAt = Instant.parse("2026-08-16T16:00:00Z")

    @Test
    fun `legacy IDs migrate and unmatched IDs survive local feed reconciliation`() {
        val persistence = InMemoryPersistence(legacyIds = setOf(" known ", "missing", " "))
        val store = PersistentFavoriteOpportunityStore(persistence)

        val result = store.reconcile(
            snapshot(
                opportunities = listOf(opportunity("known", title = "Current title")),
                source = OpportunityFeedSource.BUNDLED,
            ),
            reconciledAt = reconciledAt,
        )

        assertEquals(setOf("known", "missing"), store.ids())
        assertEquals("Current title", store.entries().single().opportunity.title)
        assertEquals(reconciledAt, store.entries().single().savedAt)
        assertEquals(setOf("missing"), store.unresolvedIds())
        assertEquals(1, result.hydratedCount)
        assertFalse(result.completeFeedApplied)
        assertTrue(persistence.snapshotJson.orEmpty().contains("\"savedAt\""))
        assertEquals(store.ids(), persistence.legacyIds)
    }

    @Test
    fun `healthy complete live feed hydrates known IDs and discards only unmatched legacy IDs`() {
        val persistence = InMemoryPersistence(legacyIds = setOf("known", "missing"))
        val store = PersistentFavoriteOpportunityStore(persistence)

        val result = store.reconcile(
            snapshot(
                opportunities = listOf(opportunity("known")),
                source = OpportunityFeedSource.PRIMARY_NETWORK,
            ),
            reconciledAt = reconciledAt,
        )

        assertTrue(result.completeFeedApplied)
        assertTrue(result.persisted)
        assertEquals(1, result.hydratedCount)
        assertEquals(1, result.discardedUnresolvedCount)
        assertEquals(setOf("known"), store.ids())
        assertTrue(store.unresolvedIds().isEmpty())
    }

    @Test
    fun `missing full snapshot becomes archived but remains saved after complete live feed`() {
        val persistence = InMemoryPersistence(snapshotJson = validEmptyEnvelope())
        val store = PersistentFavoriteOpportunityStore(persistence)
        assertTrue(store.setFavorite(opportunity("saved"), favorite = true, savedAt = savedAt))

        val result = store.reconcile(
            snapshot(
                opportunities = listOf(opportunity("other")),
                source = OpportunityFeedSource.FALLBACK_NETWORK,
            ),
            reconciledAt = reconciledAt,
        )

        val retained = store.entries().single()
        assertEquals("saved", retained.opportunity.id)
        assertEquals("removed", retained.opportunity.status)
        assertEquals(savedAt, retained.savedAt)
        assertEquals(setOf("saved"), store.ids())
        assertEquals(1, result.markedUnavailableCount)
        assertEquals(listOf("saved"), store.sections(reconciledAt).archive.map { it.opportunity.id })
    }

    @Test
    fun `cached stale and incomplete live feeds never remove unmatched saves`() {
        val persistence = InMemoryPersistence(legacyIds = setOf("legacy"))
        val store = PersistentFavoriteOpportunityStore(persistence)
        assertTrue(store.setFavorite(opportunity("saved"), favorite = true, savedAt = savedAt))

        val cachedResult = store.reconcile(
            snapshot(
                opportunities = listOf(opportunity("other")),
                source = OpportunityFeedSource.LAST_GOOD_CACHE,
            ),
            reconciledAt,
        )
        val incompleteLiveResult = store.reconcile(
            snapshot(
                opportunities = listOf(opportunity("other")),
                source = OpportunityFeedSource.PRIMARY_NETWORK,
                declaredRecordCount = 2,
            ),
            reconciledAt,
        )

        assertFalse(cachedResult.completeFeedApplied)
        assertFalse(incompleteLiveResult.completeFeedApplied)
        assertEquals(setOf("legacy", "saved"), store.ids())
        assertEquals("active", store.entries().single().opportunity.status)
        assertEquals(setOf("legacy"), store.unresolvedIds())
    }

    @Test
    fun `reconciliation refreshes payload without changing savedAt`() {
        val persistence = InMemoryPersistence(snapshotJson = validEmptyEnvelope())
        val store = PersistentFavoriteOpportunityStore(persistence)
        assertTrue(store.setFavorite(opportunity("saved", title = "Old"), true, savedAt))

        val result = store.reconcile(
            snapshot(
                opportunities = listOf(opportunity("saved", title = "Updated")),
                source = OpportunityFeedSource.BUNDLED,
            ),
            reconciledAt,
        )

        assertEquals(1, result.refreshedCount)
        assertEquals("Updated", store.entries().single().opportunity.title)
        assertEquals(savedAt, store.entries().single().savedAt)
    }

    @Test
    fun `full opportunity and savedAt survive a persistence round trip`() {
        val persistence = InMemoryPersistence(snapshotJson = validEmptyEnvelope())
        val firstStore = PersistentFavoriteOpportunityStore(persistence)
        val saved = opportunity(
            id = "round-trip",
            title = "Robotics club",
            deadline = "2026-09-15",
        )
        assertTrue(firstStore.setFavorite(saved, true, savedAt))

        val restoredStore = PersistentFavoriteOpportunityStore(persistence)
        val restored = restoredStore.entries().single()

        assertEquals(saved, restored.opportunity)
        assertEquals(savedAt, restored.savedAt)
        assertEquals(setOf("round-trip"), restoredStore.ids())
        assertTrue(restoredStore.unresolvedIds().isEmpty())
    }

    @Test
    fun `ID-only compatibility methods remain consistent with snapshot methods`() {
        val persistence = InMemoryPersistence(snapshotJson = validEmptyEnvelope())
        val store: FavoriteOpportunityStore = PersistentFavoriteOpportunityStore(persistence)

        assertTrue(store.toggle(" id-only "))
        assertTrue(store.contains("id-only"))
        assertEquals(setOf("id-only"), store.unresolvedIds())

        assertTrue(store.setFavorite(opportunity("id-only"), true, savedAt))
        assertTrue(store.unresolvedIds().isEmpty())
        assertEquals(listOf("id-only"), store.entries().map { it.opportunity.id })
        assertEquals(savedAt, store.entries().single().savedAt)

        assertFalse(store.toggle("id-only"))
        assertTrue(store.ids().isEmpty())
    }

    @Test
    fun `malformed JSON recovers mirrored IDs and rewrites a valid bounded envelope`() {
        val persistence = InMemoryPersistence(
            snapshotJson = "{not-json",
            legacyIds = setOf("recover-me"),
        )

        val store = PersistentFavoriteOpportunityStore(persistence)

        assertEquals(setOf("recover-me"), store.ids())
        assertEquals(setOf("recover-me"), store.unresolvedIds())
        assertTrue(persistence.snapshotJson.orEmpty().startsWith("{"))
        assertTrue(persistence.snapshotJson.orEmpty().contains("\"schemaVersion\":1"))
    }

    @Test
    fun `oversized write is rejected without losing existing IDs`() {
        val persistence = InMemoryPersistence(legacyIds = setOf("existing"))
        val store = PersistentFavoriteOpportunityStore(
            persistence = persistence,
            maximumPayloadBytes = 300,
        )

        val accepted = store.setFavorite(
            opportunity("large", title = "x".repeat(1_000)),
            favorite = true,
            savedAt = savedAt,
        )

        assertFalse(accepted)
        assertEquals(setOf("existing"), store.ids())
        assertEquals(setOf("existing"), persistence.legacyIds)
        assertFalse(store.contains("large"))
    }

    @Test
    fun `remove reports persistence failure and never drops retained snapshot state`() {
        val persistence = InMemoryPersistence(snapshotJson = validEmptyEnvelope())
        val store = PersistentFavoriteOpportunityStore(persistence)
        assertTrue(store.setFavorite(opportunity("snapshot"), true, savedAt))
        store.setFavorite("legacy", true)

        persistence.acceptsWrites = false
        assertFalse(store.remove("snapshot"))
        assertEquals(setOf("snapshot", "legacy"), store.ids())

        persistence.acceptsWrites = true
        assertTrue(store.remove(" snapshot "))
        assertEquals(setOf("legacy"), store.ids())
        assertTrue(store.remove("already-absent"))
        assertFalse(store.remove(" "))
    }

    @Test
    fun `clear all deletes snapshots and unresolved IDs only after storage confirms success`() {
        val persistence = InMemoryPersistence(snapshotJson = validEmptyEnvelope())
        val store = PersistentFavoriteOpportunityStore(persistence)
        assertTrue(store.setFavorite(opportunity("snapshot"), true, savedAt))
        store.setFavorite("legacy", true)

        persistence.acceptsClears = false
        assertFalse(store.clearAll())
        assertEquals(setOf("snapshot", "legacy"), store.ids())
        assertEquals(1, persistence.clearCalls)

        persistence.acceptsClears = true
        assertTrue(store.clearAll())
        assertTrue(store.ids().isEmpty())
        assertTrue(store.entries().isEmpty())
        assertTrue(store.unresolvedIds().isEmpty())
        assertEquals(2, persistence.clearCalls)
        assertEquals(null, persistence.snapshotJson)
        assertTrue(persistence.legacyIds.isEmpty())
    }

    private fun snapshot(
        opportunities: List<Opportunity>,
        source: OpportunityFeedSource,
        declaredRecordCount: Int = opportunities.size,
        isStale: Boolean = false,
    ) = OpportunityFeedSnapshot(
        opportunities = opportunities,
        lastUpdated = reconciledAt,
        source = source,
        declaredRecordCount = declaredRecordCount,
        isStale = isStale,
    )

    private fun opportunity(
        id: String,
        title: String = "STEM program $id",
        status: String = "active",
        startDate: String? = "2026-09-01",
        endDate: String? = "2026-09-30",
        deadline: String? = null,
    ) = Opportunity(
        id = id,
        title = title,
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
        sourceUrl = "https://example.org/$id",
        status = status,
    )

    private fun validEmptyEnvelope() =
        """{"schemaVersion":1,"records":[],"unresolvedIds":[]}"""

    private class InMemoryPersistence(
        var snapshotJson: String? = null,
        legacyIds: Set<String> = emptySet(),
        var acceptsWrites: Boolean = true,
        var acceptsClears: Boolean = true,
    ) : FavoriteOpportunityPersistence {
        var legacyIds: Set<String> = legacyIds
            private set
        var clearCalls: Int = 0
            private set

        override fun readSnapshotJson(): String? = snapshotJson

        override fun readLegacyIds(): Set<String> = legacyIds

        override fun write(snapshotJson: String, ids: Set<String>): Boolean {
            if (!acceptsWrites) return false
            this.snapshotJson = snapshotJson
            legacyIds = ids.toSet()
            return true
        }

        override fun clear(): Boolean {
            clearCalls += 1
            if (!acceptsClears) return false
            snapshotJson = null
            legacyIds = emptySet()
            return true
        }
    }
}
