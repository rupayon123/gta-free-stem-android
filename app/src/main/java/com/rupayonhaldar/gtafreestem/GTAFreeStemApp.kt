package com.rupayonhaldar.gtafreestem

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.rupayonhaldar.gtafreestem.ui.design.GtaPrimaryButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.rupayonhaldar.gtafreestem.ui.design.GtaObject
import com.rupayonhaldar.gtafreestem.ui.design.GtaArtwork
import com.rupayonhaldar.gtafreestem.ui.design.artworkForCategory
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rupayonhaldar.gtafreestem.data.local.AppThemePreference
import com.rupayonhaldar.gtafreestem.data.local.DisplayNameSaveResult
import com.rupayonhaldar.gtafreestem.data.local.LocalAccountDataDeletionCoordinator
import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSnapshot
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSource
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchFilters
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog
import com.rupayonhaldar.gtafreestem.localization.LocalizedOpportunityText
import com.rupayonhaldar.gtafreestem.localization.OpportunityLocalization
import com.rupayonhaldar.gtafreestem.platform.navigation.LocalPlatformNavigationCoordinator
import com.rupayonhaldar.gtafreestem.platform.location.NearbyLocationState
import com.rupayonhaldar.gtafreestem.platform.alerts.OpportunityAlertPlatform
import com.rupayonhaldar.gtafreestem.ui.preferences.OpportunityAlertFeedback
import com.rupayonhaldar.gtafreestem.ui.browse.AppDestination
import com.rupayonhaldar.gtafreestem.ui.browse.OpportunityMap
import com.rupayonhaldar.gtafreestem.ui.browse.OpportunityMapStrings
import com.rupayonhaldar.gtafreestem.ui.browse.toMapOpportunityPin
import com.rupayonhaldar.gtafreestem.platform.calendar.OpportunityCalendar
import com.rupayonhaldar.gtafreestem.ui.browse.BrowseUiState
import com.rupayonhaldar.gtafreestem.ui.browse.BrowseViewModel
import com.rupayonhaldar.gtafreestem.ui.browse.OpportunityMap3D
import com.rupayonhaldar.gtafreestem.ui.browse.toMapOpportunityPin
import com.rupayonhaldar.gtafreestem.ui.browse.OpportunityFilterPanel
import com.rupayonhaldar.gtafreestem.ui.browse.OpportunityFilterPanelLabels
import com.rupayonhaldar.gtafreestem.ui.browse.opportunityFilterPanelActiveCount
import com.rupayonhaldar.gtafreestem.ui.preferences.AccountPreferencesScreen
import com.rupayonhaldar.gtafreestem.ui.preferences.AppPreferencesUiState
import com.rupayonhaldar.gtafreestem.ui.saved.SavedOpportunityLibraryScreen
import com.rupayonhaldar.gtafreestem.ui.design.StoryButton
import com.rupayonhaldar.gtafreestem.ui.design.StoryButtonKind
import com.rupayonhaldar.gtafreestem.ui.design.ScienceLaunchExperience
import com.rupayonhaldar.gtafreestem.theme.GTAFreeStemContentTheme
import com.rupayonhaldar.gtafreestem.theme.TopicCoral
import com.rupayonhaldar.gtafreestem.theme.TopicSky
import com.rupayonhaldar.gtafreestem.theme.TopicTeal
import com.rupayonhaldar.gtafreestem.theme.TopicYellow
import com.rupayonhaldar.gtafreestem.ui.browse.SavedSearchFeedback
import com.rupayonhaldar.gtafreestem.ui.home.LandingHomeScreen
import com.rupayonhaldar.gtafreestem.ui.shell.AdaptiveAppShell
import com.rupayonhaldar.gtafreestem.ui.shell.PrimaryDestination
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import com.rupayonhaldar.gtafreestem.ui.format.OpportunitySchedule
import kotlinx.coroutines.flow.emptyFlow

private val AppScreenMaxWidth = 840.dp
private val AppScreenPadding = 16.dp
private val AppScreenTopPadding = 12.dp
private val AppScreenSectionSpacing = 14.dp
private val AppCardRadius = 18.dp
private val AppCardElevation = 0.dp

data class AppPreferenceActions(
    val saveDisplayName: (String) -> DisplayNameSaveResult,
    val clearProfile: () -> Boolean,
    val selectLanguage: (AppLanguage?) -> Boolean,
    val selectTheme: (AppThemePreference) -> Boolean,
    val setOpportunityAlertsPreferred: (Boolean) -> Boolean,
    val refreshPreferenceState: () -> Unit = {},
    val opportunityAlertFeedback: Flow<OpportunityAlertFeedback> = emptyFlow(),
) {
    companion object {
        val NONE = AppPreferenceActions(
            saveDisplayName = { DisplayNameSaveResult.STORAGE_ERROR },
            clearProfile = { false },
            selectLanguage = { false },
            selectTheme = { false },
            setOpportunityAlertsPreferred = { false },
        )
    }
}

@Composable
fun GTAFreeStemApp(
    preferences: AppPreferencesUiState,
    preferenceActions: AppPreferenceActions = AppPreferenceActions.NONE,
    nearbyLocationState: NearbyLocationState = NearbyLocationState.Idle,
    onUseNearby: () -> Unit = {},
    onClearNearby: () -> Unit = {},
    viewModel: BrowseViewModel = viewModel(
        factory = BrowseViewModel.factory(LocalContext.current),
    ),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selected = state.selectedOpportunity
    LaunchedEffect(preferences.resolvedLanguage) {
        viewModel.setSearchLanguage(preferences.resolvedLanguage)
    }
    LaunchedEffect(nearbyLocationState) {
        (nearbyLocationState as? NearbyLocationState.Located)?.fix?.let { fix ->
            viewModel.setNearbyLocation(fix.latitude, fix.longitude)
        }
    }
    var selectedDestinationName by rememberSaveable {
        mutableStateOf(PrimaryDestination.HOME.name)
    }
    var highSchoolFocusName by rememberSaveable {
        mutableStateOf(HighSchoolFocus.ALL.name)
    }
    var showingSavedOpportunities by rememberSaveable { mutableStateOf(false) }
    var showingFilters by rememberSaveable { mutableStateOf(false) }
    var browseShowsMap by rememberSaveable { mutableStateOf(false) }
    var mapDisplayMode by rememberSaveable { mutableStateOf("3D") }
    var selectedMapPinId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedDestination = PrimaryDestination.valueOf(selectedDestinationName)
    val highSchoolFocus = HighSchoolFocus.valueOf(highSchoolFocusName)
    val privacyPolicyUrl = stringResource(R.string.privacy_policy_url)
    val termsUrl = stringResource(R.string.terms_url)
    val deletionCoordinator = remember(viewModel, preferenceActions) {
        LocalAccountDataDeletionCoordinator(
            deleteProfile = preferenceActions.clearProfile,
            deleteSearchHistory = viewModel::clearSearchHistory,
            deleteSavedOpportunities = viewModel::clearSavedOpportunities,
            deleteNamedSearches = viewModel::clearNamedSavedSearches,
            deleteLocalAlerts = {
                val disabled = preferenceActions.setOpportunityAlertsPreferred(false)
                val cleared = OpportunityAlertPlatform.clearLocalHistory(context)
                disabled && cleared
            },
        )
    }

    fun openPolicy(url: String, label: String) {
        if (!openHttps(context, url)) {
            Toast.makeText(context, preferences.externalLinkUnavailableText(label), Toast.LENGTH_LONG).show()
        }
    }

    fun openPrimaryDestination(
        destination: PrimaryDestination,
        focus: HighSchoolFocus = HighSchoolFocus.ALL,
    ) {
        selectedDestinationName = destination.name
        highSchoolFocusName = focus.name
        showingSavedOpportunities = false
        showingFilters = false
        viewModel.setDestination(AppDestination.EXPLORE)
    }

    val navigationRequest = LocalPlatformNavigationCoordinator.current?.request
        ?.collectAsStateWithLifecycle()?.value
    LaunchedEffect(navigationRequest?.sequence) {
        navigationRequest?.let { openPrimaryDestination(it.destination) }
    }

    BackHandler(
        enabled = selected != null ||
            showingFilters ||
            showingSavedOpportunities ||
            selectedDestination != PrimaryDestination.HOME,
    ) {
        when {
            selected != null -> viewModel.closeOpportunity()
            showingFilters -> showingFilters = false
            showingSavedOpportunities -> {
                showingSavedOpportunities = false
                viewModel.setDestination(AppDestination.EXPLORE)
            }
            else -> openPrimaryDestination(PrimaryDestination.HOME)
        }
    }

    if (state.isLoading && state.snapshot == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            ScienceLaunchExperience(
                status = preferences.shellText("preparingOpportunities", "Preparing opportunities"),
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            )
        }
        return
    }

    GTAFreeStemContentTheme(preferences.theme.resolveDarkTheme(isSystemInDarkTheme())) {
    Box(Modifier.fillMaxSize()) {
    AdaptiveAppShell(
        selectedDestination = selectedDestination,
        onDestinationSelected = { destination -> openPrimaryDestination(destination) },
        showNavigation = selected == null,
        modifier = Modifier.fillMaxSize(),
        destinationLabel = preferences::navigationLabel,
    ) {
        when {
            selected != null -> OpportunityDetailScreen(
                opportunity = selected,
                isFavorite = selected.id in state.favoriteIds,
                onBack = viewModel::closeOpportunity,
                onToggleFavorite = { viewModel.toggleFavorite(selected) },
                language = preferences.resolvedLanguage,
                catalog = preferences.catalog,
                text = preferences::shellText,
            )

            showingFilters -> FilterScreen(
                state = state,
                onBack = { showingFilters = false },
                onFiltersChanged = viewModel::setFilters,
                text = preferences::shellText,
            )

            showingSavedOpportunities -> SavedOpportunityLibraryScreen(
                sections = state.savedSections,
                unresolvedLegacyCount = state.unresolvedSavedCount,
                language = preferences.resolvedLanguage,
                catalog = preferences.catalog,
                onBack = {
                    showingSavedOpportunities = false
                    viewModel.setDestination(AppDestination.EXPLORE)
                },
                onOpenDetail = viewModel::selectOpportunity,
                onRemoveSaved = { opportunity -> viewModel.removeSaved(opportunity.id) },
                onClearAllSaved = viewModel::clearSavedOpportunities,
            )

            else -> AnimatedContent(
                targetState = selectedDestination,
                transitionSpec = {
                    // Fade through paper, so outgoing and incoming words never blend.
                    fadeIn(tween(160, delayMillis = 160)) togetherWith fadeOut(tween(160))
                },
                modifier = Modifier.fillMaxSize(),
                label = "primary-destination",
            ) { destination ->
                Box(Modifier.fillMaxSize().semantics { paneTitle = preferences.navigationLabel(destination) }) {
                when (destination) {
                PrimaryDestination.HOME -> LandingHomeScreen(
                    state = state,
                    onBrowseAll = {
                        viewModel.clearSearchAndFilters()
                        openPrimaryDestination(PrimaryDestination.OPPORTUNITIES)
                    },
                    onTopic = { category ->
                        viewModel.clearSearchAndFilters()
                        viewModel.setCategory(category)
                        openPrimaryDestination(PrimaryDestination.OPPORTUNITIES)
                    },
                    onOpenSavedSearch = { id ->
                        if (viewModel.openSavedSearch(id)) {
                            openPrimaryDestination(PrimaryDestination.OPPORTUNITIES)
                        }
                    },
                    onDeleteSavedSearch = viewModel::deleteSavedSearch,
                    language = preferences.resolvedLanguage,
                    catalog = preferences.catalog,
                    text = preferences::shellText,
                )

                PrimaryDestination.OPPORTUNITIES -> BrowseScreen(
                    state = state,
                    nearbyLocationState = nearbyLocationState,
                    onUseNearby = onUseNearby,
                    onClearNearby = { viewModel.clearNearbyLocation(); onClearNearby() },
                    alertsEnabled = preferences.opportunityAlertsPreferred,
                    onToggleAlerts = { preferenceActions.setOpportunityAlertsPreferred(!preferences.opportunityAlertsPreferred) },
                    mode = BrowseMode.OPPORTUNITIES,
                    highSchoolFocus = HighSchoolFocus.ALL,
                    onHighSchoolFocusChanged = {},
                    onQueryChanged = viewModel::setQuery,
                    onCategoryChanged = viewModel::setCategory,
                    onOpenFilters = { showingFilters = true },
                    showMap = browseShowsMap,
                    onMapViewChanged = { browseShowsMap = it },
                    mapDisplayMode = mapDisplayMode,
                    onMapDisplayModeChanged = { mapDisplayMode = it },
                    selectedMapPinId = selectedMapPinId,
                    onMapPinSelected = { selectedMapPinId = it },
                    onClear = viewModel::clearSearchAndFilters,
                    onRefresh = { viewModel.refresh() },
                    onRetry = viewModel::load,
                    onSelectOpportunity = viewModel::selectOpportunity,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onSaveSearch = viewModel::saveCurrentSearch,
                    language = preferences.resolvedLanguage,
                    catalog = preferences.catalog,
                    shellText = preferences::shellText,
                )

                PrimaryDestination.HIGH_SCHOOL -> BrowseScreen(
                    state = state,
                    nearbyLocationState = nearbyLocationState,
                    onUseNearby = onUseNearby,
                    onClearNearby = { viewModel.clearNearbyLocation(); onClearNearby() },
                    alertsEnabled = preferences.opportunityAlertsPreferred,
                    onToggleAlerts = { preferenceActions.setOpportunityAlertsPreferred(!preferences.opportunityAlertsPreferred) },
                    mode = BrowseMode.HIGH_SCHOOL,
                    highSchoolFocus = highSchoolFocus,
                    onHighSchoolFocusChanged = { highSchoolFocusName = it.name },
                    onQueryChanged = viewModel::setQuery,
                    onCategoryChanged = viewModel::setCategory,
                    onOpenFilters = { showingFilters = true },
                    showMap = browseShowsMap,
                    onMapViewChanged = { browseShowsMap = it },
                    mapDisplayMode = mapDisplayMode,
                    onMapDisplayModeChanged = { mapDisplayMode = it },
                    selectedMapPinId = selectedMapPinId,
                    onMapPinSelected = { selectedMapPinId = it },
                    onClear = {
                        highSchoolFocusName = HighSchoolFocus.ALL.name
                        viewModel.clearSearchAndFilters()
                    },
                    onRefresh = { viewModel.refresh() },
                    onRetry = viewModel::load,
                    onSelectOpportunity = viewModel::selectOpportunity,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onSaveSearch = viewModel::saveCurrentSearch,
                    language = preferences.resolvedLanguage,
                    catalog = preferences.catalog,
                    shellText = preferences::shellText,
                )

                PrimaryDestination.SUPPORT -> SupportScreen(text = preferences::shellText)

                PrimaryDestination.ACCOUNT -> AccountPreferencesScreen(
                    state = preferences,
                    onSaveDisplayName = preferenceActions.saveDisplayName,
                    onClearProfile = preferenceActions.clearProfile,
                    onLanguageSelected = preferenceActions.selectLanguage,
                    onThemeSelected = preferenceActions.selectTheme,
                    onOpportunityAlertsPreferredChanged =
                        preferenceActions.setOpportunityAlertsPreferred,
                    onOpenSavedLibrary = {
                        showingSavedOpportunities = true
                        viewModel.setDestination(AppDestination.SAVED)
                    },
                    onDeleteAllLocalData = {
                        val result = deletionCoordinator.deleteAllLocalAccountData()
                        viewModel.clearNearbyLocation()
                        onClearNearby()
                        preferenceActions.refreshPreferenceState()
                        result
                    },
                    onOpenSupport = {
                        openPrimaryDestination(PrimaryDestination.SUPPORT)
                    },
                    onOpenPrivacyPolicy = {
                        openPolicy(
                            privacyPolicyUrl,
                            preferences.shellText("privacyPolicy", "Privacy policy"),
                        )
                    },
                    onOpenTerms = {
                        openPolicy(
                            termsUrl,
                            preferences.shellText("termsTitle", "Terms of use"),
                        )
                    },
                )
            }
                }
            }
        }
    }
        OpportunityAlertFeedbackHost(
            feedback = preferenceActions.opportunityAlertFeedback,
            languageKey = preferences.resolvedLanguage,
            text = preferences::shellText,
            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).widthIn(max = 600.dp),
        )
    }
    }
}

internal fun AppPreferencesUiState.shellText(key: String, fallback: String): String {
    val localized = text(key)
    return localized.takeUnless { it.isBlank() || it == key } ?: fallback
}

internal fun AppPreferencesUiState.navigationLabel(destination: PrimaryDestination): String {
    return shellText(destination.catalogKey, destination.fallbackLabel)
}

private enum class BrowseMode {
    OPPORTUNITIES,
    HIGH_SCHOOL,
}

private enum class HighSchoolFocus(
    val label: String,
    val catalogKey: String? = null,
) {
    ALL("All high school"),
    VOLUNTEER("Volunteer hours", "volunteerHours"),
    COOP("Co-op / SHSM", "categoryCoOpAndSHSM"),
    MENTORSHIP("Mentorship", "mentorship"),
}

@Composable
private fun CategoryMark(category: String, selected: Boolean = false) {
    Surface(
        modifier = Modifier.size(52.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            GtaObject(artworkForCategory(category), contentDescription = null, modifier = Modifier.size(44.dp))
        }
    }
}

private fun Opportunity.matchesHighSchoolFocus(focus: HighSchoolFocus): Boolean {
    val searchableText = buildList {
        add(title)
        add(description)
        summary?.let(::add)
        add(category)
        addAll(categories)
        addAll(tags)
        addAll(communityFocus)
    }.joinToString(" ").lowercase(Locale.CANADA)
    val overlapsHighSchoolAges = ageMin <= 18 && (ageMax ?: Int.MAX_VALUE) >= 14
    val hasHighSchoolSignal = volunteerHoursEligible ||
        coopEligible ||
        listOf("high school", "secondary school", "teen", "youth", "shsm", "mentor")
            .any(searchableText::contains)

    if (!overlapsHighSchoolAges && !hasHighSchoolSignal) return false

    return when (focus) {
        HighSchoolFocus.ALL -> true
        HighSchoolFocus.VOLUNTEER -> volunteerHoursEligible || "volunteer" in searchableText
        HighSchoolFocus.COOP -> coopEligible ||
            listOf("co-op", "coop", "shsm").any(searchableText::contains)
        HighSchoolFocus.MENTORSHIP ->
            listOf("mentor", "career coaching").any(searchableText::contains)
    }
}

@Composable
private fun FilterScreen(
    state: BrowseUiState,
    onBack: () -> Unit,
    onFiltersChanged: (OpportunitySearchFilters) -> Unit,
    text: (String, String) -> String,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = AppScreenMaxWidth)
                .padding(AppScreenPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = AppCardElevation),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(text("back", "Back"))
                    }
                    Text(
                        text = text("filters", "Filters"),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                }
            }
            OpportunityFilterPanel(
                filters = state.filters,
                options = state.filterOptions,
                onFiltersChange = onFiltersChanged,
                labels = localizedFilterLabels(text),
                onReset = { onFiltersChanged(OpportunitySearchFilters()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 8.dp),
            )
        }
    }
}

internal fun localizedFilterLabels(
    text: (String, String) -> String,
): OpportunityFilterPanelLabels = OpportunityFilterPanelLabels.English.copy(
    title = text("filters", "Filters"),
    reset = text("reset", "Reset filters"),
    selectedState = text("selectedState", "Selected"),
    notSelectedState = text("notSelectedState", "Not selected"),
    programDetailsSection = text("programDetails", "Program details"),
    pathwaysSection = text("pathway", "Pathways"),
    communityFocusSection = text("communityFocus", "Community focus"),
    sortSection = text("sortResults", "Sort results"),
    region = text("region", "Region"),
    city = text("city", "City"),
    category = text("category", "Category"),
    age = text("age", "Age"),
    any = text("any", "Any"),
    volunteerHours = text("volunteerHours", "Volunteer hours"),
    coop = text("coop", "Co-op"),
    mentorship = text("mentorship", "Mentorship"),
    scholarships = text("categoryScholarships", "Scholarships"),
    blackFocused = text("black", "Black-focused"),
    girlsFocused = text("girls", "Girls-focused"),
    indigenousFocused = text("indigenous", "Indigenous-focused"),
    leadership = text("leadership", "Leadership"),
    soonest = text("sortSoonest", "Soonest"),
    relevance = text("sortBestMatch", "Best match"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrowseScreen(
    state: BrowseUiState,
    nearbyLocationState: NearbyLocationState,
    onUseNearby: () -> Unit,
    onClearNearby: () -> Unit,
    alertsEnabled: Boolean,
    onToggleAlerts: () -> Unit,
    mode: BrowseMode,
    highSchoolFocus: HighSchoolFocus,
    onHighSchoolFocusChanged: (HighSchoolFocus) -> Unit,
    onQueryChanged: (String) -> Unit,
    onCategoryChanged: (String?) -> Unit,
    onOpenFilters: () -> Unit,
    showMap: Boolean,
    onMapViewChanged: (Boolean) -> Unit,
    mapDisplayMode: String,
    onMapDisplayModeChanged: (String) -> Unit,
    selectedMapPinId: String?,
    onMapPinSelected: (String) -> Unit,
    onClear: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onSelectOpportunity: (Opportunity) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSaveSearch: (String, Boolean) -> Unit,
    language: AppLanguage,
    catalog: AppStringCatalog,
    shellText: (String, String) -> String,
) {
    val visibleOpportunities = when (mode) {
        BrowseMode.HIGH_SCHOOL -> state.opportunities.filter { opportunity ->
            opportunity.matchesHighSchoolFocus(highSchoolFocus)
        }
        else -> state.opportunities
    }
    val context = LocalContext.current
    var websiteUnavailable by remember { mutableStateOf(false) }
    var saveDialogMode by rememberSaveable { mutableStateOf<String?>(null) }
    var savedSearchName by rememberSaveable { mutableStateOf("") }
    val activeSavedSearch = state.savedSearches.firstOrNull {
        it.entry.id == state.activeSavedSearchId
    }
    val mapPins = remember(visibleOpportunities, language, catalog) {
        visibleOpportunities.mapNotNull { opportunity ->
            val localized = OpportunityLocalization.resolve(opportunity, language, catalog)
            opportunity.toMapOpportunityPin(
                displayTitle = localized.title,
                displayOrganization = localized.organization,
                displayLocation = listOf(localized.city, localized.region)
                    .filter(String::isNotBlank).joinToString(", "),
            )
        }
    }
    val activeFilterCount = opportunityFilterPanelActiveCount(state.filters)
    val hasFilters = state.query.isNotBlank() ||
        state.filters.hasActiveFilters ||
        (mode == BrowseMode.HIGH_SCHOOL && highSchoolFocus != HighSchoolFocus.ALL)

    Box(
        modifier = Modifier.fillMaxHeight(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = AppScreenMaxWidth)
                .padding(horizontal = AppScreenPadding)
                .fillMaxHeight()
                .testTag("browse-results"),
            contentPadding = PaddingValues(
                top = AppScreenTopPadding,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        item {
            BrandHeader(
                mode = mode,
                highSchoolFocus = highSchoolFocus,
                shellText = shellText,
                query = state.query,
            )
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = onQueryChanged,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                        shape = MaterialTheme.shapes.small,
                        label = { Text(shellText("search", "Search opportunities")) },
                        placeholder = {
                            Text(
                                shellText(
                                    "searchPlaceholder",
                                    "Try robotics, coding, or Mississauga",
                                ),
                            )
                        },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = null,
                            )
                        },
                        trailingIcon = {
                            if (state.query.isNotBlank()) {
                                IconButton(
                                    onClick = { onQueryChanged("") },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_clear),
                                        contentDescription = shellText("clearSearch", "Clear search"),
                                    )
                                }
                            }
                        },
                    )

                    if (mode == BrowseMode.OPPORTUNITIES) Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = {
                                savedSearchName = ""
                                saveDialogMode = "new"
                            },
                            enabled = !state.filters.hasValidLocation,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("browse-save-search"),
                        ) { Text(shellText("saveSearch", "Save search")) }
                        if (activeSavedSearch != null) {
                            TextButton(
                                onClick = {
                                    savedSearchName = activeSavedSearch.entry.name
                                    saveDialogMode = "update"
                                },
                                enabled = !state.filters.hasValidLocation,
                                modifier = Modifier.heightIn(min = 48.dp).testTag("browse-update-search"),
                            ) { Text(shellText("updateSavedSearch", "Update saved search")) }
                        }
                    }
                    if (mode == BrowseMode.OPPORTUNITIES && state.filters.hasValidLocation) {
                        Text(
                            shellText("nearbyNotSaved", "Nearby location cannot be saved. Choose a city or clear Nearby first."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (mode == BrowseMode.OPPORTUNITIES) state.savedSearchFeedback?.let { feedback ->
                        val message = when (feedback) {
                            SavedSearchFeedback.SAVED -> shellText("savedSearchSaved", "Search saved on this device.")
                            SavedSearchFeedback.UPDATED -> shellText("savedSearchUpdated", "Saved search updated.")
                            SavedSearchFeedback.DELETED -> shellText("savedSearchDeleted", "Saved search deleted.")
                            SavedSearchFeedback.OPENED -> shellText("savedSearchOpened", "Saved search opened. New matches marked as viewed.")
                            SavedSearchFeedback.INVALID_NAME -> shellText("savedSearchInvalidName", "Enter a name from 1 to 60 characters.")
                            SavedSearchFeedback.LIMIT_REACHED -> shellText("savedSearchLimit", "You can save up to 16 searches on this device.")
                            SavedSearchFeedback.STORAGE_FAILED -> shellText("localSaveFailed", "That change could not be saved on this device.")
                            SavedSearchFeedback.NOT_FOUND -> shellText("savedSearchNotFound", "That saved search is no longer available.")
                            SavedSearchFeedback.NEARBY_UNSUPPORTED -> shellText("nearbyNotSaved", "Nearby location cannot be saved. Choose a city or clear Nearby first.")
                        }
                        Text(message, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("saved-search-feedback"))
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onOpenFilters,
                            modifier = Modifier.widthIn(min = 96.dp).heightIn(min = 48.dp),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(if (activeFilterCount == 0) shellText("filters", "Filters") else "${shellText("filters", "Filters")} ($activeFilterCount)")
                        }
                        FilterChip(
                            selected = !showMap,
                            onClick = { onMapViewChanged(false) },
                            modifier = Modifier.widthIn(min = 80.dp).heightIn(min = 48.dp).testTag("browse-list-view"),
                            label = { Text(shellText("list", "List")) },
                        )
                        FilterChip(
                            selected = showMap,
                            onClick = { onMapViewChanged(true) },
                            modifier = Modifier.widthIn(min = 80.dp).heightIn(min = 48.dp).testTag("browse-map-view"),
                            label = { Text(shellText("map", "Map")) },
                        )
                    }

                    NearbyAndAlertsControls(
                        filters = state.filters,
                        nearbyLocationState = nearbyLocationState,
                        onUseNearby = onUseNearby,
                        onClearNearby = onClearNearby,
                        alertsEnabled = alertsEnabled,
                        onToggleAlerts = onToggleAlerts,
                        text = shellText,
                    )

                    if (state.categories.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = state.selectedCategory == null,
                                onClick = { onCategoryChanged(null) },
                                label = { Text(shellText("all", "All")) },
                                shape = MaterialTheme.shapes.small,
                                colors = browseFilterChipColors(
                                    selected = state.selectedCategory == null,
                                ),
                            )
                            state.categories.forEach { category ->
                                FilterChip(
                                    selected = state.selectedCategory == category,
                                    onClick = { onCategoryChanged(category) },
                                    modifier = Modifier.testTag("browse-category-$category"),
                                    shape = MaterialTheme.shapes.small,
                                    colors = browseFilterChipColors(
                                        selected = state.selectedCategory == category,
                                    ),
                                    label = {
                                        Text(
                                            OpportunityLocalization.categoryName(
                                                category,
                                                language,
                                                catalog,
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }

                    if (mode == BrowseMode.HIGH_SCHOOL) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            HighSchoolFocus.entries.forEach { focus ->
                                val label = focus.catalogKey?.let { catalogKey ->
                                    shellText(catalogKey, focus.label)
                                } ?: shellText("all", focus.label)
                                FilterChip(
                                    selected = highSchoolFocus == focus,
                                    onClick = { onHighSchoolFocusChanged(focus) },
                                    shape = MaterialTheme.shapes.small,
                                    colors = browseFilterChipColors(
                                        selected = highSchoolFocus == focus,
                                    ),
                                    label = { Text(label) },
                                )
                            }
                        }
                    }

                    FeedStatusRow(
                        snapshot = state.snapshot,
                        isRefreshing = state.isRefreshing,
                        resultCount = visibleOpportunities.size,
                        onRefresh = onRefresh,
                        text = shellText,
                        language = language,
                    )
                }
            }
        }

        state.errorMessage?.let { message ->
            item {
                ErrorBanner(
                    message = message,
                    showRetry = true,
                    onRetry = onRetry,
                    text = shellText,
                )
            }
        }

        if (!state.isLoading && !state.isRefreshing && visibleOpportunities.isEmpty() && state.errorMessage != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StoryButton(
                        onClick = { websiteUnavailable = !openHttps(context, OnlineBrowseUrl) },
                        modifier = Modifier.testTag("browse-online"),
                        kind = StoryButtonKind.SECONDARY,
                    ) { Text(shellText("browseOnline", "Browse online")) }
                    Text(shellText("opensWebsite", "Opens website"), style = MaterialTheme.typography.bodySmall)
                    if (websiteUnavailable) {
                        Text(
                            shellText("websiteUnavailable", "The website could not be opened on this device."),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("browse-online-unavailable"),
                        )
                    }
                }
            }
        }

        if ((state.isLoading || state.isRefreshing) && visibleOpportunities.isEmpty()) {
            item { LoadingPanel(text = shellText) }
        } else if (visibleOpportunities.isEmpty()) {
            item {
                EmptyPanel(
                    mode = mode,
                    hasFilters = hasFilters,
                    onClear = onClear,
                    text = shellText,
                )
            }
        } else if (showMap) {
            item {
                OpportunityMap3D(
                    pins = mapPins,
                    selectedPinId = selectedMapPinId,
                    onPinSelected = { onMapPinSelected(it.id) },
                    onShowDetails = { pin ->
                        visibleOpportunities.firstOrNull { it.id == pin.id }?.let(onSelectOpportunity)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    strings = localizedMapStrings(catalog = catalog, language = language, text = shellText),
                    offlineLabel = shellText("offlinePreview", "Offline preview"),
                    displayMode = mapDisplayMode,
                    onDisplayModeChanged = onMapDisplayModeChanged,
                    loadingMessage = shellText("mapLoading", "Loading interactive map…"),
                    unavailableMessage = shellText(
                        "mapUnavailable",
                        "Interactive map unavailable. Showing offline preview. Choose 3D or 2D to retry, or {list} to browse activities.",
                    ).replace("{list}", shellText("list", "List")),
                )
                StoryButton(onClick = { onMapViewChanged(false) }, kind = StoryButtonKind.SECONDARY) {
                    Text(shellText("list", "List"))
                }
            }
        } else {
            items(items = visibleOpportunities, key = Opportunity::id) { opportunity ->
                OpportunityCard(
                    opportunity = opportunity,
                    isFavorite = opportunity.id in state.favoriteIds,
                    onDetails = { onSelectOpportunity(opportunity) },
                    onToggleFavorite = { onToggleFavorite(opportunity.id) },
                    language = language,
                    catalog = catalog,
                    text = shellText,
                )
            }
        }
        }
    }
    saveDialogMode?.let { mode ->
        AlertDialog(
            onDismissRequest = { saveDialogMode = null },
            title = {
                Text(if (mode == "update") shellText("updateSavedSearch", "Update saved search")
                    else shellText("saveSearch", "Save search"))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = savedSearchName,
                        onValueChange = { savedSearchName = it.take(60) },
                        label = { Text(shellText("name", "Name")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("saved-search-name"),
                    )
                    Text(shellText("savedSearchLocal", "Saved on this device. Browse changes only update it when you confirm here."),
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveSearch(savedSearchName, mode == "update")
                        saveDialogMode = null
                    },
                    enabled = savedSearchName.trim().isNotEmpty(),
                    modifier = Modifier.testTag("saved-search-confirm"),
                ) { Text(if (mode == "update") shellText("update", "Update") else shellText("save", "Save")) }
            },
            dismissButton = {
                TextButton(onClick = { saveDialogMode = null }) { Text(shellText("cancel", "Cancel")) }
            },
        )
    }
}

internal fun localizedMapStrings(language: AppLanguage, text: (String, String) -> String) = OpportunityMapStrings(
    mapTitle = text("map", "Map"),
    singleVisibleFormat = "%1\$s ${text("visible", "visible")}",
    multipleVisibleFormat = "%1\$s ${text("visible", "visible")}",
    mapHint = text("offlineMapHint", "Offline schematic. Locations are approximate. Online programs and results without coordinates remain in the list."),
    selectedLabel = text("selectedState", "Selected"),
    notSelectedLabel = text("notSelectedState", "Not selected"),
    markerPositionFormat = text("mapMarkerPosition", "Marker {index} of {count}").replace("{index}", "%1\$s").replace("{count}", "%2\$s"),
    markerActionLabel = text("mapMarkerActionLabel", "Select opportunity"),
    selectedOpportunityHeading = text("details", "Details"),
    sourceDetailsHint = text("sourceDetails", "Confirm details with the provider."),
    showDetailsAction = text("details", "Details"),
    emptyTitle = text("noLocationsToMap", "No locations to map"),
    emptyMessage = text("mapNoCoordinates", "These results have no physical locations to map. Use the list to see every result."),
    numberLocale = Locale.forLanguageTag(language.localeTag),
)

@Composable
private fun BrandHeader(
    mode: BrowseMode,
    highSchoolFocus: HighSchoolFocus,
    shellText: (String, String) -> String,
    query: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (mode == BrowseMode.HIGH_SCHOOL) shellText("highSchool", "High School") else shellText("browse", "Browse"),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f).testTag("browse-screen-title").semantics { heading() },
        )
        if (LocalDensity.current.fontScale < 1.6f) {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.width(88.dp).height(56.dp),
            )
        }
    }
}

@Composable
private fun FeedStatusRow(
    snapshot: OpportunityFeedSnapshot?,
    isRefreshing: Boolean,
    resultCount: Int,
    onRefresh: () -> Unit,
    text: (String, String) -> String,
    language: AppLanguage,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("$resultCount ${text("visible", "available")}", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag("feed-result-count"))
                snapshot?.let {
                    Text(
                        "${text("lastVerified", "Last checked")}: ${formatDate(it.lastUpdated.toString(), language)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("feed-last-verified-date"),
                    )
                }
            }
            TextButton(onClick = onRefresh, enabled = !isRefreshing, modifier = Modifier.heightIn(min = 48.dp)) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(text("refreshResearch", "Refresh"))
                }
            }
        }
        Text(
            if (isRefreshing) text("checkingLiveSources", "Checking live sources") else feedLabel(snapshot, text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun browseFilterChipColors(selected: Boolean) = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

private fun feedLabel(
    snapshot: OpportunityFeedSnapshot?,
    text: (String, String) -> String,
): String = when {
    snapshot == null -> text("preparingOpportunities", "Getting ready")
    snapshot.isStale -> text("offlinePreview", "Offline copy")
    snapshot.source == OpportunityFeedSource.PRIMARY_NETWORK ||
        snapshot.source == OpportunityFeedSource.FALLBACK_NETWORK ->
        text("publicLiveFeed", "Verified live")
    else -> text("previewDatabase", "Offline-ready")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OpportunityCard(
    opportunity: Opportunity,
    isFavorite: Boolean,
    onDetails: () -> Unit,
    onToggleFavorite: () -> Unit,
    language: AppLanguage,
    catalog: AppStringCatalog,
    text: (String, String) -> String,
    modifier: Modifier = Modifier,
) {
    val localized = remember(opportunity, language, catalog) {
        OpportunityLocalization.resolve(opportunity, language, catalog)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("opportunity-card-${opportunity.id}")
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            CategoryMark(opportunity.category)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(localized.category, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text(localized.cost, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(text = localized.title, style = MaterialTheme.typography.titleLarge)
                Text(text = localized.organization, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
        Text(
            text = localized.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        OpportunitySchedule.range(
            opportunity.startDate,
            opportunity.endDate,
            Locale.forLanguageTag(language.localeTag),
        )?.let { schedule ->
            Text(
                text = "${text(if (opportunity.startDate == null) "endDate" else "date", "Date")}: $schedule",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        val age = ageLabel(opportunity, text)
        Text(
            text = listOf(locationLabel(opportunity, localized), age)
                .filter(String::isNotBlank).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onToggleFavorite,
                modifier = Modifier.heightIn(min = 48.dp).semantics {
                    contentDescription = if (isFavorite) {
                        "${text("saved", "Saved")}: ${localized.title}"
                    } else {
                        "${text("save", "Save")}: ${localized.title}"
                    }
                },
            ) {
                if (isFavorite) {
                    GtaObject(GtaArtwork.SAVED, contentDescription = null,
                        modifier = Modifier.size(24.dp), compact = true)
                } else {
                    Icon(painterResource(R.drawable.ic_bookmark_border), contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isFavorite) text("saved", "Saved") else text("save", "Save"))
            }
            GtaPrimaryButton(
                onClick = onDetails,
                modifier = Modifier.heightIn(min = 48.dp).semantics {
                    contentDescription = "${text("details", "View details")}: ${localized.title}"
                },
            ) {
                Text(text("details", "View details"))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun CategoryPill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .widthIn(max = 210.dp)
                .padding(horizontal = 11.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun FreePill(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun LoadingPanel(text: (String, String) -> String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.semantics {
                    contentDescription = text("preparingOpportunities", "Loading opportunities")
                },
            )
            Text(
                text("preparingOpportunities", "Loading verified free opportunities…"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    showRetry: Boolean,
    onRetry: () -> Unit,
    text: (String, String) -> String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            if (showRetry) {
                TextButton(
                    onClick = onRetry,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.testTag("feed-error-retry"),
                ) {
                    Text(text("refreshResearch", "Try again"))
                }
            }
        }
    }
}

@Composable
private fun EmptyPanel(
    mode: BrowseMode,
    hasFilters: Boolean,
    onClear: () -> Unit,
    text: (String, String) -> String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.7.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = when {
                    mode == BrowseMode.HIGH_SCHOOL -> text(
                        "noOpportunities",
                        "No matching high school opportunities",
                    )
                    else -> text("noOpportunities", "No matching opportunities")
                },
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = when {
                    mode == BrowseMode.HIGH_SCHOOL ->
                        "Try another pathway, broaden your search, or clear the category filter."
                    else -> "Try a broader search or clear the category filter."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (hasFilters) {
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(text("reset", "Clear search and filters"))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OpportunityDetailScreen(
    opportunity: Opportunity,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    language: AppLanguage,
    catalog: AppStringCatalog,
    text: (String, String) -> String,
) {
    val context = LocalContext.current
    var calendarUnavailable by rememberSaveable(opportunity.id) { mutableStateOf(false) }
    val localized = remember(opportunity, language, catalog) {
        OpportunityLocalization.resolve(opportunity, language, catalog)
    }
    Box(
        modifier = Modifier.fillMaxHeight(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = AppScreenMaxWidth)
                .padding(horizontal = AppScreenPadding)
                .fillMaxHeight()
                .testTag("opportunity-detail"),
            contentPadding = PaddingValues(
                top = AppScreenTopPadding,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppCardRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = AppCardElevation),
                ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = onBack,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text(text("back", "Back")) }
                        Spacer(Modifier.weight(1f))
                        OutlinedButton(
                            onClick = onToggleFavorite,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .widthIn(max = 160.dp)
                                .heightIn(min = 48.dp)
                                .semantics {
                                    contentDescription = if (isFavorite) {
                                        "${text("saved", "Saved")}: ${localized.title}"
                                    } else {
                                        "${text("save", "Save")}: ${localized.title}"
                                    }
                                },
                        ) {
                            Text(if (isFavorite) text("saved", "Saved") else text("save", "Save"))
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CategoryPill(localized.category)
                        FreePill(localized.cost)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = localized.title,
                            style = MaterialTheme.typography.displaySmall,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = localized.organization,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        item {
            DetailFacts(
                opportunity = opportunity,
                localized = localized,
                language = language,
                text = text,
            )
        }

        item {
            val registrationUrl = opportunity.registrationUrl ?: opportunity.sourceUrl
            GtaPrimaryButton(
                shape = MaterialTheme.shapes.medium,
                onClick = {
                    if (!openHttps(context, registrationUrl)) {
                        Toast.makeText(
                            context,
                            text("registerApply", "This registration link is unavailable."),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text(text(if (registrationUrl.trim() == opportunity.sourceUrl.trim()) "openProgramPage" else "registerApply", "Open program page"))
            }
        }

        item {
            DetailSection(
                title = text("programDetails", "Program details"),
                body = localized.description,
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = AppCardElevation),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = text("support", "Opportunity actions"),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )

                    OutlinedButton(
                        onClick = {
                            calendarUnavailable = !OpportunityCalendar.open(
                                context, opportunity, localized.title,
                                localized.address?.takeIf(String::isNotBlank) ?: locationLabel(opportunity, localized),
                                description = localized.description,
                            )
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("opportunity-calendar"),
                    ) { Text(text("addToCalendar", "Add to calendar")) }
                    if (calendarUnavailable) {
                        Text(
                            text("calendarUnavailable", "No calendar app is available. You can still save this listing and check the provider’s dates."),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("opportunity-calendar-unavailable"),
                        )
                    }

                    if (!opportunity.virtual && (opportunity.address != null ||
                        (opportunity.latitude != null && opportunity.longitude != null)
                    )) {
                        OutlinedButton(
                            onClick = {
                                if (!openDirections(context, opportunity)) {
                                    Toast.makeText(
                                        context,
                                        text("directions", "Directions are unavailable on this device."),
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(text("directions", "Get directions"))
                        }
                    }

                    TextButton(
                        onClick = {
                            if (!openHttps(context, opportunity.sourceUrl)) {
                                Toast.makeText(
                                    context,
                                    text("sourceLink", "The source link is unavailable."),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text(text("sourceLink", "View original source"))
                    }
                    Text(
                        text = text(
                            "sourceDetails",
                            "Details can change. Confirm dates, eligibility, and availability with the provider before registering.",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (opportunity.communityFocus.isNotEmpty()) {
            item {
                DetailSection(
                    title = text("equity", "Who it welcomes"),
                    body = opportunity.communityFocus.joinToString(" • "),
                )
            }
        }

        if (opportunity.accessibility.isNotEmpty()) {
            item {
                DetailSection(
                    title = text("access", "Accessibility"),
                    body = opportunity.accessibility.joinToString(" • "),
                )
            }
        }

        opportunity.commitment?.let { commitment ->
            item {
                DetailSection(
                    title = text("commitment", "Commitment"),
                    body = commitment,
                )
            }
        }
        }
    }
}

@Composable
private fun DetailFacts(
    opportunity: Opportunity,
    localized: LocalizedOpportunityText,
    language: AppLanguage,
    text: (String, String) -> String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            DetailFactRow(text("region", "Location"), locationLabel(opportunity, localized))
            ageRange(opportunity)?.let { age ->
                HorizontalDivider()
                DetailFactRow(text("ageGuide", "Age guide"), age)
            }
            OpportunitySchedule.range(opportunity.startDate, opportunity.endDate, Locale.forLanguageTag(language.localeTag))?.let { schedule ->
                HorizontalDivider()
                DetailFactRow(text(if (opportunity.startDate == null) "endDate" else "date", "Date"), schedule)
            }
            if (OpportunitySchedule.hasDistinctDeadline(opportunity.startDate, opportunity.deadline)) {
                OpportunitySchedule.dateTime(opportunity.deadline, Locale.forLanguageTag(language.localeTag))?.let { deadline ->
                    HorizontalDivider()
                    DetailFactRow(text("deadline", "Apply by"), deadline)
                }
            }
            if (opportunity.volunteerHoursEligible || opportunity.coopEligible) {
                HorizontalDivider()
                DetailFactRow(
                    text("pathway", "Eligible for"),
                    buildList {
                        if (opportunity.volunteerHoursEligible) {
                            add(text("volunteerHours", "Volunteer hours"))
                        }
                        if (opportunity.coopEligible) add(text("coop", "Co-op"))
                    }.joinToString(" • "),
                )
            }
        }
    }
}

@Composable
private fun DetailFactRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(92.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DetailSection(title: String, body: String) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SupportScreen(text: (String, String) -> String) {
    val context = LocalContext.current
    val supportActions = listOf(
        SupportAction(
            label = text("sendFeedback", "Get help and share feedback"),
            summary = "Report issues, suggest features, and help us improve reliability and accuracy.",
            url = stringResource(R.string.support_url),
        ),
        SupportAction(
            label = text("privacyPolicy", "Privacy policy"),
            summary = "Review how local data and account settings are handled.",
            url = stringResource(R.string.privacy_policy_url),
        ),
        SupportAction(
            label = text("termsTitle", "Terms of use"),
            summary = "Understand usage rules and support expectations.",
            url = stringResource(R.string.terms_url),
        ),
    )

    Box(
        modifier = Modifier.fillMaxHeight(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .padding(horizontal = AppScreenPadding)
                .fillMaxHeight()
                .testTag("support-screen"),
            contentPadding = PaddingValues(
                top = AppScreenTopPadding,
                bottom = 24.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SupportHeroCard(text)
            }
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = AppCardElevation),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text("support", "Support and feedback"),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            "Need help, found a listing that changed, or have an idea to make GTA FREE STEM better? Reach us through the support page.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            item {
                DetailSection(
                    title = "Community-built",
                    body = "Feedback helps keep listings accurate, improve language support, and make the app easier for everyone to use.",
                )
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    supportActions.forEach {
                        SupportActionCard(
                            label = it.label,
                            summary = it.summary,
                            url = it.url,
                            context = context,
                        )
                    }
                }
            }
            item {
                Text(
                    "Tip: if something feels off, report it quickly and include your city, device version, and what you were doing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SupportHeroCard(text: (String, String) -> String) {
    Card(
        shape = RoundedCornerShape(AppCardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = AppCardElevation),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(98.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.app_icon),
                        contentDescription = "GTA FREE STEM app icon",
                        modifier = Modifier
                            .size(66.dp)
                            .clip(RoundedCornerShape(20.dp)),
                    )
                }
            }
            Text(
                text("support", "Support"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text("supportHeading", "Built to be easier for students, parents, educators, and community groups."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

private data class SupportAction(
    val label: String,
    val summary: String,
    val url: String,
)

@Composable
private fun SupportActionCard(
    label: String,
    summary: String,
    url: String,
    context: Context,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.9.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable {
                if (!openHttps(context, url)) {
                    Toast.makeText(context, "$label is unavailable.", Toast.LENGTH_LONG).show()
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                ">",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun locationLabel(
    opportunity: Opportunity,
    localized: LocalizedOpportunityText? = null,
): String = buildList {
    if (opportunity.virtual) add("Online")
    val city = localized?.city ?: opportunity.city
    val region = localized?.region ?: opportunity.region
    if (city.isNotBlank() && city != "GTA") add(city)
    if (region.isNotBlank() && region !in listOf("All", city)) {
        add(region)
    }
}.distinct().joinToString(", ").ifBlank { "Across the GTA" }

private fun ageLabel(
    opportunity: Opportunity,
    text: (String, String) -> String = { _, fallback -> fallback },
): String = ageRange(opportunity)?.let { "${text("ageGuide", "Age guide")}: $it" }.orEmpty()

private fun ageRange(opportunity: Opportunity): String? = when {
    opportunity.ageMin <= 0 && opportunity.ageMax == null -> null
    opportunity.ageMin <= 0 -> "0–${opportunity.ageMax}"
    opportunity.ageMax == null -> "${opportunity.ageMin}+"
    opportunity.ageMin == opportunity.ageMax -> "${opportunity.ageMin}"
    else -> "${opportunity.ageMin}–${opportunity.ageMax}"
}

private fun formatDate(raw: String, language: AppLanguage = AppLanguage.ENGLISH): String {
    val formatter = DateTimeFormatter.ofPattern(
        "MMM d, yyyy",
        Locale.forLanguageTag(language.localeTag),
    )
    return try {
        OffsetDateTime.parse(raw).format(formatter)
    } catch (_: DateTimeParseException) {
        try {
            LocalDate.parse(raw).format(formatter)
        } catch (_: DateTimeParseException) {
            raw
        }
    }
}

internal const val OnlineBrowseUrl = "https://gta-free-stem.pages.dev/opportunities/"

private fun openHttps(context: Context, rawUrl: String): Boolean {
    val uri = runCatching { Uri.parse(rawUrl.trim()) }.getOrNull() ?: return false
    if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) return false
    return launchExternal(context, Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
}

internal fun openDirections(context: Context, opportunity: Opportunity): Boolean {
    if (opportunity.virtual) return false
    val label = opportunity.address ?: opportunity.title
    val geoUri = if (opportunity.latitude != null && opportunity.longitude != null) {
        Uri.parse(
            "geo:${opportunity.latitude},${opportunity.longitude}?q=" +
                Uri.encode("${opportunity.latitude},${opportunity.longitude} ($label)"),
        )
    } else {
        Uri.parse("geo:0,0?q=${Uri.encode(label)}")
    }
    return launchExternal(context, Intent(Intent.ACTION_VIEW, geoUri))
}

private fun launchExternal(context: Context, intent: Intent): Boolean = runCatching {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    true
}.getOrDefault(false)
