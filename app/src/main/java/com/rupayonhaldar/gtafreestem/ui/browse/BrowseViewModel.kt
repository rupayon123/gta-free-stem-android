package com.rupayonhaldar.gtafreestem.ui.browse

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rupayonhaldar.gtafreestem.data.local.BrowseSearchState
import com.rupayonhaldar.gtafreestem.data.local.BrowseSearchStateStore
import com.rupayonhaldar.gtafreestem.data.local.FavoriteOpportunityStore
import com.rupayonhaldar.gtafreestem.data.local.SavedOpportunitySections
import com.rupayonhaldar.gtafreestem.data.local.SharedPreferencesBrowseSearchStateStore
import com.rupayonhaldar.gtafreestem.data.local.SharedPreferencesFavoriteOpportunityStore
import com.rupayonhaldar.gtafreestem.data.local.SavedSearchEntry
import com.rupayonhaldar.gtafreestem.data.local.SavedSearchStore
import com.rupayonhaldar.gtafreestem.data.local.SavedSearchSummary
import com.rupayonhaldar.gtafreestem.data.local.SharedPreferencesSavedSearchStore
import com.rupayonhaldar.gtafreestem.data.repository.OpportunityRepositories
import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSnapshot
import com.rupayonhaldar.gtafreestem.domain.repository.OpportunityRepository
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearch
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchFilters
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchLimits
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchOptions
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchSort
import com.rupayonhaldar.gtafreestem.domain.search.SavedSearchUpdatePolicy
import com.rupayonhaldar.gtafreestem.localization.AndroidAppStringCatalogLoader
import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext

enum class AppDestination {
    EXPLORE,
    SAVED,
    ABOUT,
}

enum class SavedSearchFeedback {
    SAVED, UPDATED, DELETED, OPENED, INVALID_NAME, LIMIT_REACHED, STORAGE_FAILED, NOT_FOUND,
    NEARBY_UNSUPPORTED,
}

data class BrowseUiState(
    val destination: AppDestination = AppDestination.EXPLORE,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val snapshot: OpportunityFeedSnapshot? = null,
    val opportunities: List<Opportunity> = emptyList(),
    val categories: List<String> = emptyList(),
    val query: String = "",
    val filters: OpportunitySearchFilters = OpportunitySearchFilters(),
    val searchLanguage: AppLanguage = AppLanguage.ENGLISH,
    val filterOptions: OpportunitySearchOptions = OpportunitySearchOptions(),
    val favoriteIds: Set<String> = emptySet(),
    val savedSections: SavedOpportunitySections = SavedOpportunitySections(
        current = emptyList(),
        archive = emptyList(),
    ),
    val unresolvedSavedCount: Int = 0,
    val selectedOpportunity: Opportunity? = null,
    val errorMessage: String? = null,
    val savedSearches: List<SavedSearchSummary> = emptyList(),
    val activeSavedSearchId: String? = null,
    val savedSearchFeedback: SavedSearchFeedback? = null,
) {
    /** Compatibility alias retained for the current category-chip UI. */
    val selectedCategory: String?
        get() = filters.category
}

class BrowseViewModel(
    private val repository: OpportunityRepository,
    private val favorites: FavoriteOpportunityStore,
    private val browseStateStore: BrowseSearchStateStore = BrowseSearchStateStore.NONE,
    private val savedSearchStore: SavedSearchStore = SavedSearchStore.NONE,
    private val catalog: AppStringCatalog? = null,
    private val initialSearchLanguage: AppLanguage = AppLanguage.ENGLISH,
    private val searchDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val savedSearchIoDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val restoredBrowseState = runCatching(browseStateStore::read)
        .getOrDefault(BrowseSearchState())
    private val restoredSavedLibraryState = readSavedLibraryState()
    private var savedSearchEntries = runCatching(savedSearchStore::read).getOrDefault(emptyList())
    private val _uiState = MutableStateFlow(
        BrowseUiState(
            query = restoredBrowseState.query.take(OpportunitySearchLimits.MAXIMUM_QUERY_LENGTH),
            filters = restoredBrowseState.filters.withoutTransientLocation().normalized(),
            searchLanguage = initialSearchLanguage,
            favoriteIds = restoredSavedLibraryState.ids,
            savedSections = restoredSavedLibraryState.sections,
            unresolvedSavedCount = restoredSavedLibraryState.unresolvedCount,
            savedSearches = savedSearchEntries.map(::SavedSearchSummary),
        ),
    )
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var refreshJob: Job? = null
    private var searchJob: Job? = null
    private var savedSearchJob: Job? = null
    @Volatile private var savedSearchGeneration = 0L
    @Volatile private var clearingNamedSearches = false
    private var pendingLocaleRebaseline = false
    private val savedSearchWriteMutex = Mutex()

    init {
        load()
    }

    fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                applySnapshot(repository.bootstrap())
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                // The network refresh below can still provide the first usable snapshot.
            }

            _uiState.update { it.copy(isLoading = false, isRefreshing = true) }
            refresh()
        }
    }

    fun refresh(showProgress: Boolean = true) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshing = showProgress,
                    errorMessage = null,
                )
            }
            try {
                applySnapshot(repository.refresh())
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                _uiState.update {
                    it.copy(
                        errorMessage = if (it.snapshot == null) {
                            "We couldn't load opportunities. Check your connection and try again."
                        } else {
                            "Live updates are unavailable. Showing your last verified opportunities."
                        },
                    )
                }
            } finally {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            }
        }
    }

    fun setDestination(destination: AppDestination) {
        val destinationChanged = _uiState.value.destination != destination
        _uiState.update {
            it.copy(
                destination = destination,
                selectedOpportunity = null,
            )
        }
        if (destinationChanged) recomputeResults()
    }

    fun setSearchLanguage(language: AppLanguage) {
        val changed = _uiState.value.searchLanguage != language
        _uiState.update { state ->
            val normalizedLanguage = language
            if (state.searchLanguage == normalizedLanguage) state else {
                state.copy(searchLanguage = normalizedLanguage)
            }
        }
        if (changed && savedSearchEntries.isNotEmpty()) pendingLocaleRebaseline = true
        recomputeResults()
        _uiState.value.snapshot?.let(::reconcileSavedSearches)
    }

    fun setQuery(query: String) {
        _uiState.update {
            it.copy(query = query.take(OpportunitySearchLimits.MAXIMUM_QUERY_LENGTH))
        }
        persistBrowseState()
        recomputeResults()
    }

    fun setCategory(category: String?) {
        updateFilters { current ->
            val selected = category?.trim()?.takeIf(String::isNotEmpty)
            current.copy(
                category = selected?.takeUnless {
                    it.equals(current.category, ignoreCase = true)
                },
            )
        }
    }

    fun clearSearchAndFilters() {
        _uiState.update {
            it.copy(
                query = "",
                filters = OpportunitySearchFilters(),
                activeSavedSearchId = null,
            )
        }
        persistBrowseState()
        recomputeResults()
    }

    /** Deletes only the last Browse search, leaving explicitly named searches intact. */
    fun clearSearchHistory(): Boolean {
        val browseCleared = runCatching(browseStateStore::clear).getOrDefault(false)
        if (browseCleared) {
            _uiState.update {
                it.copy(
                    query = "",
                    filters = OpportunitySearchFilters(),
                    activeSavedSearchId = null,
                )
            }
            recomputeResults()
        }
        return browseCleared
    }

    /** Deletes named searches, serialized with queued writes so a late save cannot restore them. */
    suspend fun clearNamedSavedSearches(): Boolean {
        clearingNamedSearches = true
        invalidateSavedSearchWork()
        return try {
            val cleared = withContext(savedSearchIoDispatcher) {
                savedSearchWriteMutex.withLock {
                    runCatching(savedSearchStore::clear).getOrDefault(false)
                }
            }
            if (cleared) {
                savedSearchEntries = emptyList()
                _uiState.update {
                    it.copy(savedSearches = emptyList(), activeSavedSearchId = null)
                }
            }
            cleared
        } finally {
            clearingNamedSearches = false
        }
    }

    /** Replaces the complete filter selection in one state update and one persistence write. */
    fun setFilters(filters: OpportunitySearchFilters) {
        _uiState.update { it.copy(filters = filters.normalized()) }
        persistBrowseState()
        recomputeResults()
    }

    /** A named search is changed only by this explicit action, never by Browse field edits. */
    fun saveCurrentSearch(name: String, updateExisting: Boolean = false) {
        if (clearingNamedSearches) {
            reportSavedSearchFeedback(SavedSearchFeedback.STORAGE_FAILED)
            return
        }
        val normalizedName = name.trim()
        if (normalizedName.length !in 1..60) {
            reportSavedSearchFeedback(SavedSearchFeedback.INVALID_NAME)
            return
        }
        val state = _uiState.value
        if (state.filters.hasValidLocation) {
            reportSavedSearchFeedback(SavedSearchFeedback.NEARBY_UNSUPPORTED)
            return
        }
        val targetId = state.activeSavedSearchId.takeIf { updateExisting }
        if (updateExisting && (targetId == null || savedSearchEntries.none { it.id == targetId })) {
            reportSavedSearchFeedback(SavedSearchFeedback.NOT_FOUND)
            return
        }
        if (!updateExisting && savedSearchEntries.size >= com.rupayonhaldar.gtafreestem.data.local.PersistentSavedSearchStore.MAX_SEARCHES) {
            reportSavedSearchFeedback(SavedSearchFeedback.LIMIT_REACHED)
            return
        }
        val generation = invalidateSavedSearchWork()
        val snapshot = state.snapshot
        val id = targetId ?: UUID.randomUUID().toString()
        viewModelScope.launch {
            val selectedFilters = state.filters.withoutTransientLocation().normalized()
            val matches = if (snapshot != null) withContext(searchDispatcher) {
                OpportunitySearch.search(
                    snapshot.opportunities, state.query, selectedFilters,
                    language = state.searchLanguage, catalog = catalog,
                ).map(Opportunity::id)
            } else emptyList()
            coroutineContext.ensureActive()
            if (generation != savedSearchGeneration) return@launch
            val canBaseline = snapshot != null && SavedSearchUpdatePolicy.canCountUpdates(snapshot)
            val entry = SavedSearchEntry(
                id = id,
                name = normalizedName,
                query = state.query,
                filters = selectedFilters,
                knownMatchingIds = if (canBaseline) matches.distinct() else emptyList(),
                hasBaseline = canBaseline,
                lastProcessedRevisionEpochMillis = if (canBaseline) snapshot.publicationRevision?.toEpochMilli() else null,
                lastKnownMatchCount = if (canBaseline) matches.size else null,
            )
            val revised = if (updateExisting) {
                savedSearchEntries.map { if (it.id == id) entry else it }
            } else savedSearchEntries + entry
            val prior = savedSearchEntries
            savedSearchEntries = revised
            if (!writeSavedSearches(revised, generation)) {
                if (generation == savedSearchGeneration) {
                    savedSearchEntries = prior
                    reportSavedSearchFeedback(SavedSearchFeedback.STORAGE_FAILED)
                }
                return@launch
            }
            if (generation != savedSearchGeneration) return@launch
            savedSearchEntries = revised
            _uiState.update { current ->
                val summary = SavedSearchSummary(entry)
                val summaries = if (updateExisting) {
                    current.savedSearches.map { if (it.entry.id == id) summary else it }
                } else current.savedSearches + summary
                current.copy(
                    activeSavedSearchId = id,
                    savedSearches = summaries,
                    savedSearchFeedback = if (updateExisting) SavedSearchFeedback.UPDATED else SavedSearchFeedback.SAVED,
                )
            }
        }
    }

    fun openSavedSearch(id: String): Boolean {
        if (clearingNamedSearches) return false
        val entry = savedSearchEntries.firstOrNull { it.id == id } ?: run {
            reportSavedSearchFeedback(SavedSearchFeedback.NOT_FOUND)
            return false
        }
        val generation = invalidateSavedSearchWork()
        val prior = savedSearchEntries
        val priorSummaries = _uiState.value.savedSearches
        val revised = savedSearchEntries.map {
            if (it.id == id) it.copy(unreadMatchingIds = emptyList()) else it
        }
        savedSearchEntries = revised
        _uiState.update { state ->
            state.copy(
                query = entry.query,
                filters = entry.filters.normalized(),
                activeSavedSearchId = id,
                savedSearches = state.savedSearches.map { summary ->
                    if (summary.entry.id == id) summary.copy(entry = revised.first { it.id == id }, newCount = 0)
                    else summary
                },
                savedSearchFeedback = SavedSearchFeedback.OPENED,
            )
        }
        persistBrowseState()
        recomputeResults()
        viewModelScope.launch {
            if (!writeSavedSearches(revised, generation) && generation == savedSearchGeneration) {
                savedSearchEntries = prior
                _uiState.update { state ->
                    state.copy(
                        savedSearches = priorSummaries,
                        savedSearchFeedback = SavedSearchFeedback.STORAGE_FAILED,
                    )
                }
            }
        }
        return true
    }

    fun deleteSavedSearch(id: String): Boolean {
        if (clearingNamedSearches) return false
        if (savedSearchEntries.none { it.id == id }) {
            reportSavedSearchFeedback(SavedSearchFeedback.NOT_FOUND)
            return false
        }
        val generation = invalidateSavedSearchWork()
        val prior = savedSearchEntries
        val priorSummaries = _uiState.value.savedSearches
        val revised = savedSearchEntries.filterNot { it.id == id }
        savedSearchEntries = revised
        _uiState.update { state ->
            state.copy(
                savedSearches = state.savedSearches.filterNot { it.entry.id == id },
                activeSavedSearchId = state.activeSavedSearchId.takeUnless { it == id },
                savedSearchFeedback = SavedSearchFeedback.DELETED,
            )
        }
        viewModelScope.launch {
            if (!writeSavedSearches(revised, generation) && generation == savedSearchGeneration) {
                savedSearchEntries = prior
                _uiState.update {
                    it.copy(savedSearches = priorSummaries,
                        savedSearchFeedback = SavedSearchFeedback.STORAGE_FAILED)
                }
            }
        }
        return true
    }

    private fun reportSavedSearchFeedback(feedback: SavedSearchFeedback) {
        _uiState.update { it.copy(savedSearchFeedback = feedback) }
    }

    private fun invalidateSavedSearchWork(): Long {
        savedSearchJob?.cancel()
        savedSearchGeneration += 1
        return savedSearchGeneration
    }

    private suspend fun writeSavedSearches(entries: List<SavedSearchEntry>, generation: Long): Boolean =
        withContext(savedSearchIoDispatcher) {
            savedSearchWriteMutex.withLock {
                if (generation != savedSearchGeneration) return@withLock false
                savedSearchStore.write(entries)
            }
        }

    private fun reconcileSavedSearches(snapshot: OpportunityFeedSnapshot) {
        if (clearingNamedSearches) return
        val generation = invalidateSavedSearchWork()
        val entries = savedSearchEntries.toList()
        val previous = _uiState.value.savedSearches.associateBy { it.entry.id }
        val language = _uiState.value.searchLanguage
        val quietRebaseline = pendingLocaleRebaseline
        if (entries.isEmpty()) return
        savedSearchJob = viewModelScope.launch {
            val summaries = withContext(searchDispatcher) {
                val matching = entries.associate { entry ->
                    entry.id to OpportunitySearch.search(
                        snapshot.opportunities, entry.query, entry.filters,
                        language = language, catalog = catalog,
                    ).map(Opportunity::id)
                }
                SavedSearchUpdatePolicy.summarize(
                    entries, matching, snapshot, previous,
                    quietRebaseline = quietRebaseline,
                )
            }
            coroutineContext.ensureActive()
            if (generation != savedSearchGeneration) return@launch
            val revised = summaries.map(SavedSearchSummary::entry)
            savedSearchEntries = revised
            if (revised != entries && !writeSavedSearches(revised, generation)) {
                if (generation == savedSearchGeneration) {
                    savedSearchEntries = entries
                    reportSavedSearchFeedback(SavedSearchFeedback.STORAGE_FAILED)
                }
                return@launch
            }
            if (generation != savedSearchGeneration) return@launch
            savedSearchEntries = revised
            _uiState.update { it.copy(savedSearches = summaries) }
            if (quietRebaseline && SavedSearchUpdatePolicy.canCountUpdates(snapshot) &&
                summaries.all { summary ->
                    summary.entry.lastProcessedRevisionEpochMillis ==
                        snapshot.publicationRevision?.toEpochMilli()
                }
            ) pendingLocaleRebaseline = false
        }
    }

    /** Applies an in-memory coarse fix; coordinates are deliberately excluded from persistence. */
    fun setNearbyLocation(
        latitude: Double,
        longitude: Double,
        radiusKm: Int = 25,
    ) = updateFilters { current ->
        current.copy(
            latitude = latitude,
            longitude = longitude,
            distanceKm = radiusKm,
            sort = OpportunitySearchSort.NEAREST,
        )
    }

    fun clearNearbyLocation() = updateFilters { current ->
        current.copy(
            latitude = null,
            longitude = null,
            distanceKm = null,
            sort = if (current.sort == OpportunitySearchSort.NEAREST) {
                OpportunitySearchSort.SOONEST
            } else {
                current.sort
            },
        )
    }

    fun setRegion(region: String?) = updateFilters { it.copy(region = region) }

    fun setCity(city: String?) = updateFilters {
        it.copy(
            city = city,
            region = null,
            latitude = null,
            longitude = null,
            distanceKm = null,
            sort = if (it.sort == OpportunitySearchSort.NEAREST) OpportunitySearchSort.SOONEST else it.sort,
        )
    }

    fun setAge(age: Int?, adultsOnly: Boolean = false) = updateFilters {
        it.copy(age = age, adultsOnly = adultsOnly)
    }


    fun setSort(sort: OpportunitySearchSort) = updateFilters { it.copy(sort = sort) }

    fun setVolunteerHoursOnly(enabled: Boolean) = updateFilters {
        it.copy(volunteerHoursOnly = enabled)
    }

    fun setCoopOnly(enabled: Boolean) = updateFilters { it.copy(coopOnly = enabled) }

    fun setMentorshipOnly(enabled: Boolean) = updateFilters { it.copy(mentorshipOnly = enabled) }

    fun setScholarshipsOnly(enabled: Boolean) = updateFilters {
        it.copy(scholarshipsOnly = enabled)
    }

    fun setBlackFocusedOnly(enabled: Boolean) = updateFilters {
        it.copy(blackFocusedOnly = enabled)
    }

    fun setGirlsFocusedOnly(enabled: Boolean) = updateFilters {
        it.copy(girlsFocusedOnly = enabled)
    }

    fun setIndigenousFocusedOnly(enabled: Boolean) = updateFilters {
        it.copy(indigenousFocusedOnly = enabled)
    }

    fun setLeadershipOnly(enabled: Boolean) = updateFilters {
        it.copy(leadershipOnly = enabled)
    }

    fun selectOpportunity(opportunity: Opportunity) {
        _uiState.update { it.copy(selectedOpportunity = opportunity) }
    }

    fun closeOpportunity() {
        _uiState.update { it.copy(selectedOpportunity = null) }
    }

    fun toggleFavorite(id: String) {
        val opportunity = repository.findById(id)
        if (opportunity == null) {
            favorites.toggle(id)
        } else {
            favorites.toggle(opportunity)
        }
        refreshSavedLibraryState()
        recomputeResults()
    }

    /** Preserves the displayed full snapshot when saving an archived or removed opportunity. */
    fun toggleFavorite(opportunity: Opportunity) {
        favorites.toggle(opportunity)
        refreshSavedLibraryState()
        recomputeResults()
    }

    /** Removes one saved snapshot or unresolved legacy ID and reports persistence success. */
    fun removeSaved(id: String): Boolean {
        val removed = runCatching { favorites.remove(id) }.getOrDefault(false)
        refreshSavedLibraryState()
        recomputeResults()
        return removed
    }

    /** Deletes the complete local saved library and reports persistence success. */
    fun clearSavedOpportunities(): Boolean {
        val cleared = runCatching(favorites::clearAll).getOrDefault(false)
        refreshSavedLibraryState()
        recomputeResults()
        return cleared
    }

    private suspend fun applySnapshot(snapshot: OpportunityFeedSnapshot) {
        val filterOptions = withContext(searchDispatcher) {
            OpportunitySearch.options(snapshot.opportunities)
        }
        favorites.reconcile(snapshot)
        val savedLibraryState = readSavedLibraryState()
        _uiState.update {
            it.copy(
                snapshot = snapshot,
                categories = filterOptions.categories,
                filterOptions = filterOptions,
                favoriteIds = savedLibraryState.ids,
                savedSections = savedLibraryState.sections,
                unresolvedSavedCount = savedLibraryState.unresolvedCount,
                errorMessage = null,
            )
        }
        recomputeResults()
        reconcileSavedSearches(snapshot)
    }

    private fun recomputeResults() {
        val state = _uiState.value
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val visible = withContext(searchDispatcher) {
                val searched = repository.search(
                    query = state.query,
                    filters = state.filters,
                    language = state.searchLanguage,
                )
                if (state.destination == AppDestination.SAVED) {
                    searched.filter { it.id in state.favoriteIds }
                } else {
                    searched
                }
            }
            coroutineContext.ensureActive()
            _uiState.update { it.copy(opportunities = visible) }
        }
    }

    private fun updateFilters(
        transform: (OpportunitySearchFilters) -> OpportunitySearchFilters,
    ) {
        _uiState.update { state ->
            state.copy(filters = transform(state.filters).normalized())
        }
        persistBrowseState()
        recomputeResults()
    }

    private fun persistBrowseState() {
        val state = _uiState.value
        browseStateStore.write(
            BrowseSearchState(
                query = state.query,
                filters = state.filters.withoutTransientLocation(),
            ),
        )
    }

    private fun refreshSavedLibraryState() {
        val savedLibraryState = readSavedLibraryState()
        _uiState.update {
            it.copy(
                favoriteIds = savedLibraryState.ids,
                savedSections = savedLibraryState.sections,
                unresolvedSavedCount = savedLibraryState.unresolvedCount,
            )
        }
    }

    private fun readSavedLibraryState(): SavedLibraryState = runCatching {
        SavedLibraryState(
            ids = favorites.ids(),
            sections = favorites.sections(),
            unresolvedCount = favorites.unresolvedIds().size,
        )
    }.getOrDefault(SavedLibraryState())

    private data class SavedLibraryState(
        val ids: Set<String> = emptySet(),
        val sections: SavedOpportunitySections = SavedOpportunitySections(
            current = emptyList(),
            archive = emptyList(),
        ),
        val unresolvedCount: Int = 0,
    )

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(BrowseViewModel::class.java))
                    return BrowseViewModel(
                        repository = OpportunityRepositories.create(appContext),
                        favorites = SharedPreferencesFavoriteOpportunityStore(appContext),
                        browseStateStore = SharedPreferencesBrowseSearchStateStore(appContext),
                        savedSearchStore = SharedPreferencesSavedSearchStore(appContext),
                        catalog = AndroidAppStringCatalogLoader.load(appContext),
                    ) as T
                }
            }
        }
    }
}

private fun OpportunitySearchFilters.withoutTransientLocation(): OpportunitySearchFilters = copy(
    latitude = null,
    longitude = null,
    distanceKm = null,
    sort = if (sort == OpportunitySearchSort.NEAREST) {
        OpportunitySearchSort.SOONEST
    } else {
        sort
    },
)
