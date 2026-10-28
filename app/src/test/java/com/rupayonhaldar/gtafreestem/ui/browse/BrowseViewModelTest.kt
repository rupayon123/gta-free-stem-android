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
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {
    @Test
    fun `restored unsupported program language is neutral while display locale and filters remain`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val stateStore = RecordingBrowseSearchStateStore(
                restored = BrowseSearchState(
                    query = "",
                    filters = OpportunitySearchFilters(
                        region = "Toronto",
                        city = "Toronto",
                        category = "Coding & Robotics",
                        age = 12,
                        language = "ja",
                    ),
                ),
            )
            val repository = RecordingRepository(snapshot(listOf(
                opportunity(
                    id = "english-source",
                    category = "Coding & Robotics",
                    region = "Toronto",
                    city = "Toronto",
                    languages = listOf("en"),
                    ageMax = 14,
                ),
            )))
            val viewModel = BrowseViewModel(
                repository = repository,
                favorites = EmptyFavorites(),
                browseStateStore = stateStore,
                initialSearchLanguage = AppLanguage.JAPANESE,
                searchDispatcher = StandardTestDispatcher(testScheduler),
            )

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(AppLanguage.JAPANESE, state.searchLanguage)
            assertEquals(null, state.filters.language)
            assertEquals("Toronto", state.filters.region)
            assertEquals("Toronto", state.filters.city)
            assertEquals("Coding & Robotics", state.filters.category)
            assertEquals(12, state.filters.age)
            assertEquals(listOf("english-source"), state.opportunities.map(Opportunity::id))
            assertEquals(AppLanguage.JAPANESE, repository.searches.last().third)

            viewModel.setQuery("Program")
            advanceUntilIdle()
            assertEquals(null, stateStore.writes.last().filters.language)
            assertEquals("Toronto", stateStore.writes.last().filters.city)
        } finally {
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `rapid query edits publish only the latest search`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = RecordingRepository(snapshot(listOf(
                opportunity(
                    id = "robotics",
                    category = "Coding & Robotics",
                    region = "Toronto",
                    city = "Toronto",
                    languages = listOf("en"),
                    ageMax = 18,
                ),
            )))
            val viewModel = BrowseViewModel(
                repository = repository,
                favorites = EmptyFavorites(),
                searchDispatcher = StandardTestDispatcher(testScheduler),
            )
            advanceUntilIdle()
            repository.searches.clear()

            viewModel.setQuery("rob")
            viewModel.setQuery("robot")
            viewModel.setQuery("robotics")
            advanceUntilIdle()

            assertEquals(listOf("robotics"), repository.searches.map { it.first })
            assertEquals("robotics", viewModel.uiState.value.query)
            assertEquals(listOf("robotics"), viewModel.uiState.value.opportunities.map { it.id })
        } finally {
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

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
                searchDispatcher = StandardTestDispatcher(testScheduler),
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
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.filters.scholarshipsOnly)
            assertTrue(repository.searches.last().second.leadershipOnly)
            assertEquals(viewModel.uiState.value.filters, stateStore.writes.last().filters)

            viewModel.setQuery("q".repeat(OpportunitySearchLimits.MAXIMUM_QUERY_LENGTH + 10))
            assertEquals(
                OpportunitySearchLimits.MAXIMUM_QUERY_LENGTH,
                viewModel.uiState.value.query.length,
            )

        viewModel.clearSearchAndFilters()
        advanceUntilIdle()
        val cleared = viewModel.uiState.value
        assertEquals("", cleared.query)
            assertEquals(OpportunitySearchFilters(), cleared.filters)
            assertEquals(BrowseSearchState(), stateStore.writes.last())
            assertFalse(cleared.filters.hasActiveFilters)
            assertEquals(AppLanguage.ENGLISH, repository.searches.last().third)

            repository.searches.clear()
            viewModel.setSearchLanguage(AppLanguage.SPANISH)
            advanceUntilIdle()
            assertEquals(AppLanguage.SPANISH, repository.searches.last().third)
        } finally {
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `nearby coordinates stay in memory and are stripped from restored and persisted state`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val stateStore = RecordingBrowseSearchStateStore(
                restored = BrowseSearchState(
                    query = "science",
                    filters = OpportunitySearchFilters(
                        latitude = 43.6532,
                        longitude = -79.3832,
                        distanceKm = 50,
                        sort = OpportunitySearchSort.NEAREST,
                    ),
                ),
            )
            val viewModel = BrowseViewModel(
                repository = RecordingRepository(snapshot(emptyList())),
                favorites = EmptyFavorites(),
                browseStateStore = stateStore,
                searchDispatcher = StandardTestDispatcher(testScheduler),
            )

            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.filters.latitude)
            assertEquals(null, viewModel.uiState.value.filters.longitude)
            assertEquals(null, viewModel.uiState.value.filters.distanceKm)
            assertEquals(OpportunitySearchSort.SOONEST, viewModel.uiState.value.filters.sort)

            viewModel.setNearbyLocation(
                latitude = 43.7001,
                longitude = -79.4163,
                radiusKm = 25,
            )

            val liveFilters = viewModel.uiState.value.filters
            assertEquals(43.7001, liveFilters.latitude)
            assertEquals(-79.4163, liveFilters.longitude)
            assertEquals(25, liveFilters.distanceKm)
            assertEquals(OpportunitySearchSort.NEAREST, liveFilters.sort)

            val persistedFilters = stateStore.writes.last().filters
            assertEquals(null, persistedFilters.latitude)
            assertEquals(null, persistedFilters.longitude)
            assertEquals(null, persistedFilters.distanceKm)
            assertEquals(OpportunitySearchSort.SOONEST, persistedFilters.sort)
        } finally {
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `choosing a Home city clears incompatible nearby constraints and preserves the search`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val toronto = opportunity("robotics-toronto", "Coding & Robotics", "Toronto", "Toronto", listOf("en"), 17)
                .copy(latitude = 43.6532, longitude = -79.3832)
            val markham = opportunity("robotics-markham", "Coding & Robotics", "York", "Markham", listOf("en"), 17)
                .copy(latitude = 43.8561, longitude = -79.3370)
            val vm = BrowseViewModel(
                repository = RecordingRepository(snapshot(listOf(toronto, markham))),
                favorites = EmptyFavorites(),
                searchDispatcher = StandardTestDispatcher(testScheduler),
            )
            advanceUntilIdle()
            vm.setQuery("robotics")
            vm.setFilters(OpportunitySearchFilters(
                region = "York", category = "Coding & Robotics", age = 13,
                latitude = 43.8561, longitude = -79.3370, distanceKm = 1,
                sort = OpportunitySearchSort.NEAREST,
            ))
            advanceUntilIdle()
            assertEquals(listOf(markham.id), vm.uiState.value.opportunities.map(Opportunity::id))
            vm.setCity("Toronto")
            advanceUntilIdle()
            assertEquals(listOf(toronto.id), vm.uiState.value.opportunities.map(Opportunity::id))
            assertEquals("robotics", vm.uiState.value.query)
            assertEquals(13, vm.uiState.value.filters.age)
            assertEquals("Coding & Robotics", vm.uiState.value.filters.category)
            assertEquals(null, vm.uiState.value.filters.region)
            assertEquals(null, vm.uiState.value.filters.latitude)
            assertEquals(null, vm.uiState.value.filters.longitude)
            assertEquals(null, vm.uiState.value.filters.distanceKm)
            assertEquals(OpportunitySearchSort.SOONEST, vm.uiState.value.filters.sort)
        } finally {
            advanceUntilIdle()
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
                searchDispatcher = StandardTestDispatcher(testScheduler),
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
            advanceUntilIdle()
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
                searchDispatcher = StandardTestDispatcher(testScheduler),
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
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `expired empty bootstrap stays updating while live data arrives without resetting choices`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val result = CompletableDeferred<OpportunityFeedSnapshot>()
            val retained = opportunity("saved", "STEM", "Toronto", "Toronto", listOf("en"), 18)
            val favorites = RecordingFavorites(current = mutableListOf(savedEntry(retained)))
            val repository = SuspendedRefreshRepository(snapshot(emptyList()).copy(isStale = true), result)
            val viewModel = BrowseViewModel(
                repository = repository,
                favorites = favorites,
                searchDispatcher = StandardTestDispatcher(testScheduler),
            )
            runCurrent()
            assertFalse(viewModel.uiState.value.isLoading)
            assertTrue(viewModel.uiState.value.isRefreshing)
            assertTrue(viewModel.uiState.value.opportunities.isEmpty())

            viewModel.setQuery("robotics")
            viewModel.setFilters(OpportunitySearchFilters(city = "Toronto"))
            val fresh = opportunity("robotics", "STEM", "Toronto", "Toronto", listOf("en"), null)
            result.complete(snapshot(listOf(fresh)).copy(source = OpportunityFeedSource.PRIMARY_NETWORK))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isRefreshing)
            assertEquals(listOf("robotics"), state.opportunities.map(Opportunity::id))
            assertEquals("robotics", state.query)
            assertEquals("Toronto", state.filters.city)
            assertTrue("saved" in state.favoriteIds)
            assertEquals(null, state.errorMessage)
        } finally {
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `failed initial update ends progress and keeps the retry explanation across navigation`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val result = CompletableDeferred<OpportunityFeedSnapshot>()
            val viewModel = BrowseViewModel(
                repository = SuspendedRefreshRepository(snapshot(emptyList()).copy(isStale = true), result),
                favorites = EmptyFavorites(),
                searchDispatcher = StandardTestDispatcher(testScheduler),
            )
            runCurrent()
            assertTrue(viewModel.uiState.value.isRefreshing)
            result.completeExceptionally(java.io.IOException("offline"))
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isRefreshing)
            val explanation = viewModel.uiState.value.errorMessage
            assertFalse(explanation.isNullOrBlank())
            viewModel.setDestination(AppDestination.SAVED)
            assertEquals(explanation, viewModel.uiState.value.errorMessage)
        } finally {
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    private class SuspendedRefreshRepository(
        private var snapshot: OpportunityFeedSnapshot,
        private val result: CompletableDeferred<OpportunityFeedSnapshot>,
    ) : OpportunityRepository {
        override suspend fun bootstrap() = snapshot
        override suspend fun refresh(): OpportunityFeedSnapshot = result.await().also { snapshot = it }
        override fun current() = snapshot
        override fun findById(id: String) = snapshot.opportunities.firstOrNull { it.id == id }
        override fun search(query: String, filters: OpportunitySearchFilters, language: AppLanguage) =
            OpportunitySearch.search(snapshot.opportunities, query, filters, now = Instant.parse("2026-08-16T16:00:00Z"))
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
        val searches = mutableListOf<Triple<String, OpportunitySearchFilters, AppLanguage>>()

        override suspend fun bootstrap(): OpportunityFeedSnapshot = snapshot

        override suspend fun refresh(): OpportunityFeedSnapshot = snapshot

        override fun current(): OpportunityFeedSnapshot = snapshot

        override fun search(
            query: String,
            filters: OpportunitySearchFilters,
            language: AppLanguage,
        ): List<Opportunity> {
            searches += Triple(query, filters, language)
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
