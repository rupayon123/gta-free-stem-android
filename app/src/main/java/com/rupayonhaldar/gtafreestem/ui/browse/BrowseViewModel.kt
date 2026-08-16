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
import com.rupayonhaldar.gtafreestem.data.repository.OpportunityRepositories
import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSnapshot
import com.rupayonhaldar.gtafreestem.domain.repository.OpportunityRepository
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearch
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchFilters
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchLimits
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchOptions
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchSort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppDestination {
    EXPLORE,
    SAVED,
    ABOUT,
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
    val filterOptions: OpportunitySearchOptions = OpportunitySearchOptions(),
    val favoriteIds: Set<String> = emptySet(),
    val savedSections: SavedOpportunitySections = SavedOpportunitySections(
        current = emptyList(),
        archive = emptyList(),
    ),
    val unresolvedSavedCount: Int = 0,
    val selectedOpportunity: Opportunity? = null,
    val errorMessage: String? = null,
) {
    /** Compatibility alias retained for the current category-chip UI. */
    val selectedCategory: String?
        get() = filters.category
}

class BrowseViewModel(
    private val repository: OpportunityRepository,
    private val favorites: FavoriteOpportunityStore,
    private val browseStateStore: BrowseSearchStateStore = BrowseSearchStateStore.NONE,
) : ViewModel() {
    private val restoredBrowseState = runCatching(browseStateStore::read)
        .getOrDefault(BrowseSearchState())
    private val restoredSavedLibraryState = readSavedLibraryState()
    private val _uiState = MutableStateFlow(
        BrowseUiState(
            query = restoredBrowseState.query.take(OpportunitySearchLimits.MAXIMUM_QUERY_LENGTH),
            filters = restoredBrowseState.filters.normalized(),
            favoriteIds = restoredSavedLibraryState.ids,
            savedSections = restoredSavedLibraryState.sections,
            unresolvedSavedCount = restoredSavedLibraryState.unresolvedCount,
        ),
    )
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var refreshJob: Job? = null

    init {
        load()
    }

    fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            runCatching { repository.bootstrap() }
                .onSuccess(::applySnapshot)
                .onFailure { error ->
                    if (error is CancellationException) throw error
                }

            _uiState.update { it.copy(isLoading = false) }
            refresh(showProgress = false)
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
        _uiState.update {
            it.copy(
                destination = destination,
                selectedOpportunity = null,
                errorMessage = null,
            )
        }
        recomputeResults()
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
            )
        }
        persistBrowseState()
        recomputeResults()
    }

    /** Deletes persisted browse history and resets the live query/filter/sort state on success. */
    fun clearSearchHistory(): Boolean {
        val cleared = runCatching(browseStateStore::clear).getOrDefault(false)
        if (cleared) {
            _uiState.update {
                it.copy(
                    query = "",
                    filters = OpportunitySearchFilters(),
                )
            }
            recomputeResults()
        }
        return cleared
    }

    /** Replaces the complete filter selection in one state update and one persistence write. */
    fun setFilters(filters: OpportunitySearchFilters) {
        _uiState.update { it.copy(filters = filters.normalized()) }
        persistBrowseState()
        recomputeResults()
    }

    fun setRegion(region: String?) = updateFilters { it.copy(region = region) }

    fun setCity(city: String?) = updateFilters { it.copy(city = city) }

    fun setAge(age: Int?, adultsOnly: Boolean = false) = updateFilters {
        it.copy(age = age, adultsOnly = adultsOnly)
    }

    fun setLanguage(language: String?) = updateFilters { it.copy(language = language) }

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

    private fun applySnapshot(snapshot: OpportunityFeedSnapshot) {
        favorites.reconcile(snapshot)
        val filterOptions = OpportunitySearch.options(snapshot.opportunities)
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
    }

    private fun recomputeResults() {
        val state = _uiState.value
        val searched = repository.search(
            query = state.query,
            filters = state.filters,
        )
        val visible = if (state.destination == AppDestination.SAVED) {
            searched.filter { it.id in state.favoriteIds }
        } else {
            searched
        }
        _uiState.update { it.copy(opportunities = visible) }
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
                filters = state.filters,
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
                    ) as T
                }
            }
        }
    }
}
