package com.rupayonhaldar.gtafreestem.ui.browse

import com.rupayonhaldar.gtafreestem.data.local.BrowseSearchState
import com.rupayonhaldar.gtafreestem.data.local.BrowseSearchStateStore
import com.rupayonhaldar.gtafreestem.data.local.FavoriteOpportunityStore
import com.rupayonhaldar.gtafreestem.data.local.SavedOpportunityEntry
import com.rupayonhaldar.gtafreestem.data.local.SavedOpportunityReconciliation
import com.rupayonhaldar.gtafreestem.data.local.SavedOpportunitySections
import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSnapshot
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSource
import com.rupayonhaldar.gtafreestem.domain.repository.OpportunityRepository
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearch
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchFilters
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchLimits
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchSort
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {
    @Test
    fun `restores persists applies options and atomically clears complete browse state`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val restoredFilters = OpportunitySearchFilters(
                region = "Toronto",
                category = "Coding & Robotics",
                adultsOnly = true,
                volunteerHoursOnly = true,
                sort = OpportunitySearchSort.RELEVANCE,
            )
            val stateStore = RecordingBrowseSearchStateStore(
                restored = BrowseSearchState(
                    query = "robotics",
                    filters = restoredFilters,
                ),
            )
            val repository = RecordingRepository(
                snapshot(
                    listOf(
                        opportunity(
                            id = "match",
                            category = "Coding & Robotics",
                            region = "Toronto",
                            city = "Toronto",
                            languages = listOf("en", "fr"),
                            ageMax = null,
                            tags = listOf("Volunteer hours robotics"),
                        ),
                        opportunity(
                            id = "other",
                            category = "Science & Engineering",
                            region = "Peel",
                            city = "Brampton",
                            languages = listOf("en"),
                            ageMax = 14,
                        ),
                    ),
                ),
            )
            val viewModel = BrowseViewModel(
                repository = repository,
                favorites = EmptyFavorites(),
                browseStateStore = stateStore,
            )

            advanceUntilIdle()

            val restored = viewModel.uiState.value
            assertEquals("robotics", restored.query)
            assertEquals(restoredFilters, restored.filters)
            assertEquals("Coding & Robotics", restored.selectedCategory)
            assertEquals(listOf("match"), restored.opportunities.map(Opportunity::id))
            assertEquals(listOf("Peel", "Toronto"), restored.filterOptions.regions)
            assertEquals(listOf("Brampton", "Toronto"), restored.filterOptions.cities)
            assertEquals(
                listOf("Coding & Robotics", "Science & Engineering"),
                restored.categories,
            )
            assertEquals(listOf("en", "fr"), restored.filterOptions.languages)
            assertEquals("18+", restored.filterOptions.ages.last().id)

            viewModel.setCategory("Coding & Robotics")
            assertEquals(null, viewModel.uiState.value.selectedCategory)
            assertEquals(null, stateStore.writes.last().filters.category)

            viewModel.setFilters(
                OpportunitySearchFilters(
                    city = "Toronto",
                    scholarshipsOnly = true,
                    leadershipOnly = true,
                    sort = OpportunitySearchSort.RELEVANCE,
                ),
            )
            assertTrue(viewModel.uiState.value.filters.scholarshipsOnly)
            assertTrue(repository.searches.last().second.leadershipOnly)
            assertEquals(viewModel.uiState.value.filters, stateStore.writes.last().filters)

            viewModel.setQuery("q".repeat(OpportunitySearchLimits.MAXIMUM_QUERY_LENGTH + 10))
            assertEquals(
                OpportunitySearchLimits.MAXIMUM_QUERY_LENGTH,
                viewModel.uiState.value.query.length,
            )

            viewModel.clearSearchAndFilters()
            val cleared = viewModel.uiState.value
            assertEquals("", cleared.query)
            assertEquals(OpportunitySearchFilters(), cleared.filters)
            assertEquals(BrowseSearchState(), stateStore.writes.last())
            assertFalse(cleared.filters.hasActiveFilters)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `saved library state refreshes after reconcile toggle remove and clear`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val existing = opportunity(
                id = "existing",
                category = "STEM",
                region = "Toronto",
                city = "Toronto",
                languages = listOf("en"),
                ageMax = 18,
            )
            val hydrated = opportunity(
                id = "legacy",
                category = "STEM",
                region = "Toronto",
                city = "Toronto",
                languages = listOf("en"),
                ageMax = 18,
            )
            val fresh = opportunity(
                id = "fresh",
                category = "STEM",
                region = "Toronto",
                city = "Toronto",
                languages = listOf("en"),
                ageMax = 18,
            )
            val favorites = RecordingFavorites(
                current = mutableListOf(savedEntry(existing)),
                unresolved = mutableSetOf("legacy"),
            )
            val viewModel = BrowseViewModel(
                repository = RecordingRepository(snapshot(listOf(existing, hydrated, fresh))),
                favorites = favorites,
            )

            assertEquals(setOf("existing", "legacy"), viewModel.uiState.value.favoriteIds)
            assertEquals(listOf("existing"), currentSavedIds(viewModel))
            assertEquals(1, viewModel.uiState.value.unresolvedSavedCount)

            advanceUntilIdle()

            assertEquals(setOf("existing", "legacy"), viewModel.uiState.value.favoriteIds)
            assertEquals(listOf("existing", "legacy"), currentSavedIds(viewModel))
            assertEquals(0, viewModel.uiState.value.unresolvedSavedCount)

            viewModel.toggleFavorite("fresh")
            assertEquals(setOf("existing", "legacy", "fresh"), viewModel.uiState.value.favoriteIds)
            assertEquals(listOf("existing", "legacy", "fresh"), currentSavedIds(viewModel))

            assertTrue(viewModel.removeSaved("fresh"))
            assertEquals(setOf("existing", "legacy"), viewModel.uiState.value.favoriteIds)
            assertEquals(listOf("existing", "legacy"), currentSavedIds(viewModel))

            val offlineOnly = opportunity(
                id = "offline-snapshot",
                category = "STEM",
                region = "Toronto",
                city = "Toronto",
                languages = listOf("en"),
                ageMax = 18,
            ).copy(status = "removed")
            viewModel.toggleFavorite(offlineOnly)
            assertEquals(
                offlineOnly,
                viewModel.uiState.value.savedSections.current
                    .first { it.opportunity.id == offlineOnly.id }
                    .opportunity,
            )
            assertEquals(0, viewModel.uiState.value.unresolvedSavedCount)
            viewModel.toggleFavorite(offlineOnly)
            assertFalse(offlineOnly.id in viewModel.uiState.value.favoriteIds)

            favorites.removeResult = false
            assertFalse(viewModel.removeSaved("existing"))
            assertTrue("existing" in viewModel.uiState.value.favoriteIds)

            favorites.clearResult = false
            assertFalse(viewModel.clearSavedOpportunities())
            assertEquals(2, viewModel.uiState.value.savedSections.current.size)

            favorites.clearResult = true
            assertTrue(viewModel.clearSavedOpportunities())
            assertTrue(viewModel.uiState.value.favoriteIds.isEmpty())
            assertTrue(viewModel.uiState.value.savedSections.current.isEmpty())
            assertTrue(viewModel.uiState.value.savedSections.archive.isEmpty())
            assertEquals(0, viewModel.uiState.value.unresolvedSavedCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `clear search history deletes persistence and resets live state only on success`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val restored = BrowseSearchState(
                query = "robotics",
                filters = OpportunitySearchFilters(
                    region = "Toronto",
                    leadershipOnly = true,
                    sort = OpportunitySearchSort.RELEVANCE,
                ),
            )
            val stateStore = RecordingBrowseSearchStateStore(
                restored = restored,
                clearResult = false,
            )
            val viewModel = BrowseViewModel(
                repository = RecordingRepository(snapshot(emptyList())),
                favorites = EmptyFavorites(),
                browseStateStore = stateStore,
            )
            advanceUntilIdle()

            assertFalse(viewModel.clearSearchHistory())
            assertEquals(restored.query, viewModel.uiState.value.query)
            assertEquals(restored.filters, viewModel.uiState.value.filters)

            stateStore.clearResult = true
            val writeCountBeforeDeletion = stateStore.writes.size
            assertTrue(viewModel.clearSearchHistory())
            assertEquals("", viewModel.uiState.value.query)
            assertEquals(OpportunitySearchFilters(), viewModel.uiState.value.filters)
            assertEquals(writeCountBeforeDeletion, stateStore.writes.size)
            assertEquals(2, stateStore.clearCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private data class RecordingBrowseSearchStateStore(
        val restored: BrowseSearchState,
        val writes: MutableList<BrowseSearchState> = mutableListOf(),
        var clearResult: Boolean = true,
        var clearCalls: Int = 0,
    ) : BrowseSearchStateStore {
        override fun read(): BrowseSearchState = restored

        override fun write(state: BrowseSearchState): Boolean {
            writes += state
            return true
        }

        override fun clear(): Boolean {
            clearCalls += 1
            return clearResult
        }
    }

    private class RecordingRepository(
        private val snapshot: OpportunityFeedSnapshot,
    ) : OpportunityRepository {
        val searches = mutableListOf<Pair<String, OpportunitySearchFilters>>()

        override suspend fun bootstrap(): OpportunityFeedSnapshot = snapshot

        override suspend fun refresh(): OpportunityFeedSnapshot = snapshot

        override fun current(): OpportunityFeedSnapshot = snapshot

        override fun search(query: String, filters: OpportunitySearchFilters): List<Opportunity> {
            searches += query to filters
            return OpportunitySearch.search(
                opportunities = snapshot.opportunities,
                query = query,
                filters = filters,
                now = Instant.parse("2026-08-16T16:00:00Z"),
            )
        }

        override fun findById(id: String): Opportunity? =
            snapshot.opportunities.firstOrNull { it.id == id }
    }

    private class EmptyFavorites : FavoriteOpportunityStore {
        override fun ids(): Set<String> = emptySet()
        override fun contains(id: String) = false
        override fun setFavorite(id: String, favorite: Boolean) = Unit
        override fun toggle(id: String) = false
        override fun remove(id: String) = true
        override fun clearAll() = true
        override fun entries(): List<SavedOpportunityEntry> = emptyList()
        override fun unresolvedIds(): Set<String> = emptySet()
        override fun sections(now: Instant) = SavedOpportunitySections(emptyList(), emptyList())

        override fun setFavorite(
            opportunity: Opportunity,
            favorite: Boolean,
            savedAt: Instant,
        ) = false

        override fun toggle(opportunity: Opportunity, savedAt: Instant) = false

        override fun reconcile(
            snapshot: OpportunityFeedSnapshot,
            reconciledAt: Instant,
        ) = SavedOpportunityReconciliation(
            refreshedCount = 0,
            hydratedCount = 0,
            markedUnavailableCount = 0,
            discardedUnresolvedCount = 0,
            completeFeedApplied = false,
            persisted = true,
        )
    }

    private class RecordingFavorites(
        private val current: MutableList<SavedOpportunityEntry> = mutableListOf(),
        private val archive: MutableList<SavedOpportunityEntry> = mutableListOf(),
        private val unresolved: MutableSet<String> = mutableSetOf(),
        var removeResult: Boolean = true,
        var clearResult: Boolean = true,
    ) : FavoriteOpportunityStore {
        override fun ids(): Set<String> =
            (current + archive).mapTo(mutableSetOf()) { it.opportunity.id } + unresolved

        override fun contains(id: String): Boolean = id.trim() in ids()

        override fun setFavorite(id: String, favorite: Boolean) {
            if (favorite) unresolved += id.trim() else remove(id)
        }

        override fun toggle(id: String): Boolean {
            val normalized = id.trim()
            if (contains(normalized)) {
                remove(normalized)
                return false
            }
            unresolved += normalized
            return true
        }

        override fun remove(id: String): Boolean {
            if (!removeResult) return false
            val normalized = id.trim()
            current.removeAll { it.opportunity.id == normalized }
            archive.removeAll { it.opportunity.id == normalized }
            unresolved.remove(normalized)
            return true
        }

        override fun clearAll(): Boolean {
            if (!clearResult) return false
            current.clear()
            archive.clear()
            unresolved.clear()
            return true
        }

        override fun entries(): List<SavedOpportunityEntry> = current + archive

        override fun unresolvedIds(): Set<String> = unresolved.toSet()

        override fun sections(now: Instant): SavedOpportunitySections = SavedOpportunitySections(
            current = current.toList(),
            archive = archive.toList(),
        )

        override fun setFavorite(
            opportunity: Opportunity,
            favorite: Boolean,
            savedAt: Instant,
        ): Boolean = if (favorite) {
            remove(opportunity.id)
            current += SavedOpportunityEntry(opportunity, savedAt)
            true
        } else {
            remove(opportunity.id)
        }

        override fun toggle(opportunity: Opportunity, savedAt: Instant): Boolean {
            if (contains(opportunity.id)) {
                remove(opportunity.id)
                return false
            }
            current += SavedOpportunityEntry(opportunity, savedAt)
            unresolved.remove(opportunity.id)
            return true
        }

        override fun reconcile(
            snapshot: OpportunityFeedSnapshot,
            reconciledAt: Instant,
        ): SavedOpportunityReconciliation {
            var hydrated = 0
            snapshot.opportunities.forEach { opportunity ->
                if (opportunity.id in unresolved) {
                    unresolved.remove(opportunity.id)
                    current += SavedOpportunityEntry(opportunity, reconciledAt)
                    hydrated += 1
                }
            }
            return SavedOpportunityReconciliation(
                refreshedCount = 0,
                hydratedCount = hydrated,
                markedUnavailableCount = 0,
                discardedUnresolvedCount = 0,
                completeFeedApplied = false,
                persisted = true,
            )
        }
    }

    private fun snapshot(opportunities: List<Opportunity>) = OpportunityFeedSnapshot(
        opportunities = opportunities,
        lastUpdated = Instant.parse("2026-08-16T12:00:00Z"),
        source = OpportunityFeedSource.BUNDLED,
        declaredRecordCount = opportunities.size,
        isStale = false,
    )

    private fun opportunity(
        id: String,
        category: String,
        region: String,
        city: String,
        languages: List<String>,
        ageMax: Int?,
        tags: List<String> = emptyList(),
    ) = Opportunity(
        id = id,
        title = "Program $id",
        organization = "Community Library",
        description = "A hands-on program.",
        category = category,
        categories = listOf(category),
        city = city,
        region = region,
        endDate = "2027-12-31",
        ageMin = 8,
        ageMax = ageMax,
        languages = languages,
        cost = "Free",
        sourceUrl = "https://example.org/$id",
        tags = tags,
    )

    private fun savedEntry(opportunity: Opportunity) = SavedOpportunityEntry(
        opportunity = opportunity,
        savedAt = Instant.parse("2026-08-16T12:00:00Z"),
    )

    private fun currentSavedIds(viewModel: BrowseViewModel): List<String> =
        viewModel.uiState.value.savedSections.current.map { it.opportunity.id }
}
