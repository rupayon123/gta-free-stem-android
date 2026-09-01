package com.rupayonhaldar.gtafreestem

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rupayonhaldar.gtafreestem.data.local.AppThemePreference
import com.rupayonhaldar.gtafreestem.data.local.DisplayNameSaveResult
import com.rupayonhaldar.gtafreestem.data.local.LocalAccountDataDeletionCoordinator
import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSnapshot
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSource
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearch
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchFilters
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog
import com.rupayonhaldar.gtafreestem.localization.LocalizedOpportunityText
import com.rupayonhaldar.gtafreestem.localization.OpportunityLocalization
import com.rupayonhaldar.gtafreestem.ui.browse.AppDestination
import com.rupayonhaldar.gtafreestem.ui.browse.BrowseUiState
import com.rupayonhaldar.gtafreestem.ui.browse.BrowseViewModel
import com.rupayonhaldar.gtafreestem.ui.browse.OpportunityFilterPanel
import com.rupayonhaldar.gtafreestem.ui.browse.OpportunityFilterPanelLabels
import com.rupayonhaldar.gtafreestem.ui.browse.opportunityFilterPanelActiveCount
import com.rupayonhaldar.gtafreestem.ui.preferences.AccountPreferencesScreen
import com.rupayonhaldar.gtafreestem.ui.preferences.AppPreferencesUiState
import com.rupayonhaldar.gtafreestem.ui.saved.SavedOpportunityLibraryScreen
import com.rupayonhaldar.gtafreestem.ui.shell.AdaptiveAppShell
import com.rupayonhaldar.gtafreestem.ui.shell.PrimaryDestination
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val AppScreenMaxWidth = 840.dp
private val AppScreenPadding = 16.dp
private val AppScreenTopPadding = 12.dp
private val AppScreenSectionSpacing = 14.dp
private val AppCardRadius = 18.dp
private val AppCardElevation = 1.5.dp

data class AppPreferenceActions(
    val saveDisplayName: (String) -> DisplayNameSaveResult,
    val clearProfile: () -> Boolean,
    val selectLanguage: (AppLanguage?) -> Boolean,
    val selectTheme: (AppThemePreference) -> Boolean,
    val setOpportunityAlertsPreferred: (Boolean) -> Boolean,
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
    viewModel: BrowseViewModel = viewModel(
        factory = BrowseViewModel.factory(LocalContext.current),
    ),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selected = state.selectedOpportunity
    var selectedDestinationName by rememberSaveable {
        mutableStateOf(PrimaryDestination.HOME.name)
    }
    var highSchoolFocusName by rememberSaveable {
        mutableStateOf(HighSchoolFocus.ALL.name)
    }
    var showingSavedOpportunities by rememberSaveable { mutableStateOf(false) }
    var showingFilters by rememberSaveable { mutableStateOf(false) }
    val selectedDestination = PrimaryDestination.valueOf(selectedDestinationName)
    val highSchoolFocus = HighSchoolFocus.valueOf(highSchoolFocusName)
    val privacyPolicyUrl = stringResource(R.string.privacy_policy_url)
    val termsUrl = stringResource(R.string.terms_url)
    val deletionCoordinator = remember(viewModel, preferenceActions) {
        LocalAccountDataDeletionCoordinator(
            deleteProfile = preferenceActions.clearProfile,
            deleteSearchHistory = viewModel::clearSearchHistory,
            deleteSavedOpportunities = viewModel::clearSavedOpportunities,
        )
    }

    fun openPolicy(url: String, label: String) {
        if (!openHttps(context, url)) {
            Toast.makeText(context, "$label is unavailable.", Toast.LENGTH_LONG).show()
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

            else -> when (selectedDestination) {
                PrimaryDestination.HOME -> HomeScreen(
                    state = state,
                    onSelectOpportunity = viewModel::selectOpportunity,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onSearch = { openPrimaryDestination(PrimaryDestination.OPPORTUNITIES) },
                    onHighSchool = { openPrimaryDestination(PrimaryDestination.HIGH_SCHOOL) },
                    onQuickAction = { focus ->
                        openPrimaryDestination(PrimaryDestination.HIGH_SCHOOL, focus)
                    },
                    onFeedback = { openPrimaryDestination(PrimaryDestination.SUPPORT) },
                    language = preferences.resolvedLanguage,
                    catalog = preferences.catalog,
                    text = preferences::shellText,
                )

                PrimaryDestination.OPPORTUNITIES -> BrowseScreen(
                    state = state,
                    mode = BrowseMode.OPPORTUNITIES,
                    highSchoolFocus = HighSchoolFocus.ALL,
                    onHighSchoolFocusChanged = {},
                    onQueryChanged = viewModel::setQuery,
                    onCategoryChanged = viewModel::setCategory,
                    onOpenFilters = { showingFilters = true },
                    onClear = viewModel::clearSearchAndFilters,
                    onRefresh = { viewModel.refresh() },
                    onRetry = viewModel::load,
                    onSelectOpportunity = viewModel::selectOpportunity,
                    onToggleFavorite = viewModel::toggleFavorite,
                    language = preferences.resolvedLanguage,
                    catalog = preferences.catalog,
                    shellText = preferences::shellText,
                )

                PrimaryDestination.HIGH_SCHOOL -> BrowseScreen(
                    state = state,
                    mode = BrowseMode.HIGH_SCHOOL,
                    highSchoolFocus = highSchoolFocus,
                    onHighSchoolFocusChanged = { highSchoolFocusName = it.name },
                    onQueryChanged = viewModel::setQuery,
                    onCategoryChanged = viewModel::setCategory,
                    onOpenFilters = { showingFilters = true },
                    onClear = {
                        highSchoolFocusName = HighSchoolFocus.ALL.name
                        viewModel.clearSearchAndFilters()
                    },
                    onRefresh = { viewModel.refresh() },
                    onRetry = viewModel::load,
                    onSelectOpportunity = viewModel::selectOpportunity,
                    onToggleFavorite = viewModel::toggleFavorite,
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
                        deletionCoordinator.deleteAllLocalAccountData()
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

internal fun AppPreferencesUiState.shellText(key: String, fallback: String): String {
    val localized = text(key)
    return localized.takeUnless { it.isBlank() || it == key } ?: fallback
}

internal fun AppPreferencesUiState.navigationLabel(destination: PrimaryDestination): String {
    if (destination == PrimaryDestination.ACCOUNT && resolvedLanguage == AppLanguage.ENGLISH) {
        return destination.fallbackLabel
    }
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
private fun HomeScreen(
    state: BrowseUiState,
    onSelectOpportunity: (Opportunity) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSearch: () -> Unit,
    onHighSchool: () -> Unit,
    onQuickAction: (HighSchoolFocus) -> Unit,
    onFeedback: () -> Unit,
    language: AppLanguage,
    catalog: AppStringCatalog,
    text: (String, String) -> String,
) {
    val featuredOpportunities = remember(state.snapshot) {
        OpportunitySearch.search(
            opportunities = state.snapshot?.opportunities.orEmpty(),
            query = "",
            filters = OpportunitySearchFilters(),
        ).take(3)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = AppScreenMaxWidth),
        contentPadding = PaddingValues(
            start = AppScreenPadding,
            end = AppScreenPadding,
            top = AppScreenTopPadding,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(AppScreenSectionSpacing),
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.app_logo),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(18.dp)),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                text = "GTA FREE STEM",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.semantics { heading() },
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary,
                                shape = CircleShape,
                            ) {
                                Text(
                                    text = text(
                                        "freeOnly",
                                        "Everything here is free for everyone.",
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = text(
                            "mission",
                            "Find real free STEM programs, library events, volunteer hours, co-op, SHSM, and youth opportunities across the GTA.",
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = onSearch,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
                        ) { Text(text("search", "Search")) }
                        OutlinedButton(
                            onClick = onHighSchool,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
                        ) { Text(text("highSchool", "High School")) }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = text("pathway", "Choose your path"),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HomePathButton(
                        label = text("volunteerHours", "Volunteer Hours"),
                        modifier = Modifier.weight(1f),
                    ) { onQuickAction(HighSchoolFocus.VOLUNTEER) }
                    HomePathButton(
                        label = text("categoryCoOpAndSHSM", "Co-op / SHSM"),
                        modifier = Modifier.weight(1f),
                    ) { onQuickAction(HighSchoolFocus.COOP) }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HomePathButton(
                        label = text("mentorship", "Mentorship"),
                        modifier = Modifier.weight(1f),
                    ) { onQuickAction(HighSchoolFocus.MENTORSHIP) }
                    HomePathButton(
                        label = text("feedback", "Feedback"),
                        modifier = Modifier.weight(1f),
                        onClick = onFeedback,
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = text("opportunities", "Featured opportunities"),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = text(
                        "sourceDetails",
                        "A few source-backed programs to get you started.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when {
            state.isLoading && state.snapshot == null -> item { LoadingPanel(text) }
            featuredOpportunities.isEmpty() -> item {
                Text(
                    text = text(
                        "noOpportunities",
                        "Open Opportunities to search all verified listings.",
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> items(
                items = featuredOpportunities,
                key = Opportunity::id,
            ) { opportunity ->
                OpportunityCard(
                    opportunity = opportunity,
                    isFavorite = opportunity.id in state.favoriteIds,
                    onDetails = { onSelectOpportunity(opportunity) },
                    onToggleFavorite = { onToggleFavorite(opportunity.id) },
                    language = language,
                    catalog = catalog,
                    text = text,
                )
            }
        }
    }
}

@Composable
private fun HomePathButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 52.dp)
            .padding(top = 0.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(label)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppScreenPadding),
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

private fun localizedFilterLabels(
    text: (String, String) -> String,
): OpportunityFilterPanelLabels = OpportunityFilterPanelLabels.English.copy(
    title = text("filters", "Filters"),
    reset = text("reset", "Reset filters"),
    programDetailsSection = text("programDetails", "Program details"),
    pathwaysSection = text("pathway", "Pathways"),
    communityFocusSection = text("communityFocus", "Community focus"),
    sortSection = text("sortResults", "Sort results"),
    region = text("region", "Region"),
    city = text("city", "City"),
    category = text("category", "Category"),
    age = text("age", "Age"),
    language = text("programLanguage", "Language"),
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

@Composable
private fun BrowseScreen(
    state: BrowseUiState,
    mode: BrowseMode,
    highSchoolFocus: HighSchoolFocus,
    onHighSchoolFocusChanged: (HighSchoolFocus) -> Unit,
    onQueryChanged: (String) -> Unit,
    onCategoryChanged: (String?) -> Unit,
    onOpenFilters: () -> Unit,
    onClear: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onSelectOpportunity: (Opportunity) -> Unit,
    onToggleFavorite: (String) -> Unit,
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
    val activeFilterCount = opportunityFilterPanelActiveCount(state.filters)
    val hasFilters = state.query.isNotBlank() ||
        state.filters.hasActiveFilters ||
        (mode == BrowseMode.HIGH_SCHOOL && highSchoolFocus != HighSchoolFocus.ALL)

    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = AppScreenMaxWidth),
        contentPadding = PaddingValues(
            start = AppScreenPadding,
            top = AppScreenTopPadding,
            end = AppScreenPadding,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            BrandHeader(
                mode = mode,
                highSchoolFocus = highSchoolFocus,
                shellText = shellText,
            )
        }

        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = AppCardElevation),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = onQueryChanged,
                        modifier = Modifier.fillMaxWidth(),
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
                        trailingIcon = {
                            if (state.query.isNotBlank()) {
                                TextButton(
                                    onClick = { onQueryChanged("") },
                                    modifier = Modifier.heightIn(min = 40.dp),
                                ) {
                                    Text(shellText("reset", "Clear"))
                                }
                            }
                        },
                    )

                    OutlinedButton(
                        onClick = onOpenFilters,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            text = if (activeFilterCount == 0) {
                                shellText("filters", "Filters")
                            } else {
                                "${shellText("filters", "Filters")} ($activeFilterCount)"
                            },
                        )
                    }

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
                    )
                }
            }
        }

        state.errorMessage?.let { message ->
            item {
                ErrorBanner(
                    message = message,
                    showRetry = state.snapshot == null,
                    onRetry = onRetry,
                    text = shellText,
                )
            }
        }

        if (state.isLoading && state.snapshot == null) {
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
        } else {
            items(
                items = visibleOpportunities,
                key = Opportunity::id,
            ) { opportunity ->
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

@Composable
private fun BrandHeader(
    mode: BrowseMode,
    highSchoolFocus: HighSchoolFocus,
    shellText: (String, String) -> String,
) {
    val title = when (mode) {
        BrowseMode.OPPORTUNITIES -> shellText("navOpportunities", "Opportunities")
        BrowseMode.HIGH_SCHOOL -> shellText("highSchool", "High School")
    }
    val subtitle = when (mode) {
        BrowseMode.OPPORTUNITIES -> shellText(
            "mission",
            stringResource(R.string.browse_subtitle),
        )
        BrowseMode.HIGH_SCHOOL -> when (highSchoolFocus) {
            HighSchoolFocus.ALL -> "Programs for teens, volunteer hours, co-op, SHSM, and mentorship."
            HighSchoolFocus.VOLUNTEER -> "Free opportunities that can help with volunteer hours."
            HighSchoolFocus.COOP -> "Free co-op and SHSM-connected opportunities for students."
            HighSchoolFocus.MENTORSHIP -> "Free mentorship and career-connected opportunities."
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(AppCardRadius),
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp)),
            )
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .testTag("browse-screen-title")
                        .semantics { heading() },
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
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
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                color = when (snapshot?.source) {
                    OpportunityFeedSource.PRIMARY_NETWORK,
                    OpportunityFeedSource.FALLBACK_NETWORK,
                    -> MaterialTheme.colorScheme.secondaryContainer

                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = CircleShape,
            ) {
                Text(
                    text = feedLabel(snapshot, text),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
            Text(
                text = resultSummary(snapshot, resultCount, text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onRefresh,
                enabled = !isRefreshing,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.small,
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .semantics {
                                contentDescription = text(
                                    "checkingLiveSources",
                                    "Refreshing opportunities",
                                )
                            },
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (isRefreshing) {
                        text("checkingLiveSources", "Refreshing")
                    } else {
                        text("refreshResearch", "Refresh")
                    },
                )
            }
        }
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

private fun resultSummary(
    snapshot: OpportunityFeedSnapshot?,
    resultCount: Int,
    text: (String, String) -> String,
): String {
    val visible = "$resultCount ${text("visible", "available")}".trim()
    val declared = snapshot?.declaredRecordCount ?: return visible
    return if (declared == resultCount) {
        visible
    } else {
        "$visible • $declared ${text("lastVerified", "verified")}".trim()
    }
}

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
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppCardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = AppCardElevation),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CategoryPill(localized.category)
                Spacer(Modifier.weight(1f))
                FreePill(text("freeShort", "FREE"))
            }
            Text(
                text = localized.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = localized.organization,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = localized.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOf(locationLabel(opportunity, localized), ageLabel(opportunity, text))
                    .filter(String::isNotBlank)
                    .joinToString("  •  "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .weight(1f)
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
                Button(
                    onClick = onDetails,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = "${text("details", "View details")}: ${localized.title}"
                        },
                ) {
                    Text(text("details", "View details"))
                }
            }
        }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
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
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            if (showRetry) {
                TextButton(
                    onClick = onRetry,
                    shape = MaterialTheme.shapes.small,
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
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

@Composable
private fun OpportunityDetailScreen(
    opportunity: Opportunity,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    language: AppLanguage,
    catalog: AppStringCatalog,
    text: (String, String) -> String,
) {
    val context = LocalContext.current
    val localized = remember(opportunity, language, catalog) {
        OpportunityLocalization.resolve(opportunity, language, catalog)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = AppScreenMaxWidth)
            .testTag("opportunity-detail"),
        contentPadding = PaddingValues(
            start = AppScreenPadding,
            top = AppScreenTopPadding,
            end = AppScreenPadding,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppCardRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CategoryPill(localized.category)
                        FreePill(text("freeShort", "FREE"))
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
                        Text(
                            text = localized.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                catalog = catalog,
                text = text,
            )
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

                    val registrationUrl = opportunity.registrationUrl ?: opportunity.sourceUrl
                    Button(
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                    ) {
                        Text(text("registerApply", "Open registration"))
                    }

                    if (opportunity.address != null ||
                        (opportunity.latitude != null && opportunity.longitude != null)
                    ) {
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
    }
}

@Composable
private fun DetailFacts(
    opportunity: Opportunity,
    localized: LocalizedOpportunityText,
    language: AppLanguage,
    catalog: AppStringCatalog,
    text: (String, String) -> String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            DetailFactRow(text("region", "Location"), locationLabel(opportunity, localized))
            HorizontalDivider()
            DetailFactRow(text("ages", "Ages"), ageLabel(opportunity, text))
            opportunity.startDate?.let { date ->
                HorizontalDivider()
                DetailFactRow(text("date", "Starts"), formatDate(date, language))
            }
            opportunity.endDate?.let { date ->
                HorizontalDivider()
                DetailFactRow(text("date", "Ends"), formatDate(date, language))
            }
            opportunity.deadline?.let { date ->
                HorizontalDivider()
                DetailFactRow(text("deadline", "Apply by"), formatDate(date, language))
            }
            if (opportunity.languages.isNotEmpty()) {
                HorizontalDivider()
                DetailFactRow(
                    text("languages", "Languages"),
                    opportunity.languages.joinToString(", ") { code ->
                        languageName(code, catalog)
                    },
                )
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

    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 760.dp)
            .testTag("support-screen"),
        contentPadding = PaddingValues(
            start = AppScreenPadding,
            top = AppScreenTopPadding,
            end = AppScreenPadding,
            bottom = 24.dp,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SupportHeroCard()
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

@Composable
private fun SupportHeroCard() {
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
                "GTA FREE STEM support",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Need us? We're here to help fix issues and improve the app together.",
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
): String = when {
    opportunity.ageMin <= 0 && opportunity.ageMax == null -> "All ages"
    opportunity.ageMin <= 0 -> "Up to age ${opportunity.ageMax}"
    opportunity.ageMax == null -> "${text("ages", "Ages")} ${opportunity.ageMin}+"
    opportunity.ageMin == opportunity.ageMax -> "Age ${opportunity.ageMin}"
    else -> "${text("ages", "Ages")} ${opportunity.ageMin}–${opportunity.ageMax}"
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

private fun languageName(code: String, catalog: AppStringCatalog): String =
    AppLanguage.matching(code)?.let(catalog::metadata)?.nativeName
        ?: code.uppercase(Locale.ROOT)

private fun openHttps(context: Context, rawUrl: String): Boolean {
    val uri = runCatching { Uri.parse(rawUrl.trim()) }.getOrNull() ?: return false
    if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) return false
    return launchExternal(context, Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
}

private fun openDirections(context: Context, opportunity: Opportunity): Boolean {
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
