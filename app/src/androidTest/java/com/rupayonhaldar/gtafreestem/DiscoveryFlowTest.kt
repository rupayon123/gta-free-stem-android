package com.rupayonhaldar.gtafreestem

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.graphics.asAndroidBitmap
import kotlinx.coroutines.CompletableDeferred
import java.io.File
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.rupayonhaldar.gtafreestem.data.local.AppThemePreference
import com.rupayonhaldar.gtafreestem.data.local.FavoriteOpportunityPersistence
import com.rupayonhaldar.gtafreestem.data.local.PersistentFavoriteOpportunityStore
import com.rupayonhaldar.gtafreestem.domain.model.*
import com.rupayonhaldar.gtafreestem.domain.repository.OpportunityRepository
import com.rupayonhaldar.gtafreestem.domain.search.*
import com.rupayonhaldar.gtafreestem.localization.*
import com.rupayonhaldar.gtafreestem.platform.calendar.OpportunityCalendar
import com.rupayonhaldar.gtafreestem.ui.browse.*
import com.rupayonhaldar.gtafreestem.ui.preferences.AppPreferencesUiState
import com.rupayonhaldar.gtafreestem.theme.GTAFreeStemTheme
import java.time.Instant
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Deterministic fixture integration. These records never enter production storage. */
class DiscoveryFlowTest {
    @get:Rule val rule = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val catalog by lazy { AndroidAppStringCatalogLoader.load(context) }
    private fun text(key: String, fallback: String) = catalog.text(key, AppLanguage.ENGLISH).takeUnless { it == key } ?: fallback
    private fun program(id: String = "toronto") = Opportunity(
        id = id, title = "Toronto Lab", organization = "Fixture Library", description = "A test-only science workshop.",
        category = "STEM", city = "Toronto", region = "Toronto", latitude = 43.6532, longitude = -79.3832,
        address = "1 Fixture Lane, Toronto",
        ageMin = 0, ageMax = null, cost = "Free", sourceUrl = "https://example.org/fixture",
        startDate = "2099-10-26", endDate = "2099-10-26",
    )

    @Test fun brandedLoadingHandsOffOnlyWhenTheRealBootstrapCompletes() {
        val initial = CompletableDeferred<OpportunityFeedSnapshot>()
        val repository = PendingBootstrapRepository(initial)
        lateinit var vm: BrowseViewModel
        rule.setContent {
            vm = remember { BrowseViewModel(repository, PersistentFavoriteOpportunityStore(MemoryFavorites())) }
            GTAFreeStemTheme { GTAFreeStemApp(preferences = preferences(), viewModel = vm) }
        }
        rule.onNodeWithTag("science-launch-experience").assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate))
        rule.onNodeWithTag("science-launch-status", useUnmergedTree = true)
            .assertTextEquals(text("preparingOpportunities", "Preparing opportunities"))
        rule.onNodeWithTag("primary-navigation-home").assertDoesNotExist()
        // Optional real rendered fixture export for visual review, never source data.
        if (InstrumentationRegistry.getArguments().getString("gtaCaptureFixtures") == "true") {
            File(context.cacheDir, "gta-science-loader-fixture.png").outputStream().use { stream ->
                check(rule.onNodeWithTag("science-launch-experience").captureToImage().asAndroidBitmap()
                    .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream))
            }
        }
        rule.runOnIdle {
            initial.complete(OpportunityFeedSnapshot(listOf(program()), Instant.parse("2099-10-25T00:00:00Z"), OpportunityFeedSource.BUNDLED, 1, false))
        }
        rule.waitUntil(10_000) { !vm.uiState.value.isLoading && vm.uiState.value.snapshot != null }
        rule.onNodeWithTag("science-launch-experience").assertDoesNotExist()
        rule.onNodeWithTag("primary-navigation-home").assertIsDisplayed()
    }

    @Test fun failedBootstrapLeavesTheLoaderAndKeepsNavigationAndRetryUsable() {
        val initial = CompletableDeferred<OpportunityFeedSnapshot>()
        val repository = PendingBootstrapRepository(initial)
        lateinit var vm: BrowseViewModel
        rule.setContent {
            vm = remember { BrowseViewModel(repository, PersistentFavoriteOpportunityStore(MemoryFavorites())) }
            GTAFreeStemTheme { GTAFreeStemApp(preferences = preferences(), viewModel = vm) }
        }
        rule.onNodeWithTag("science-launch-experience").assertIsDisplayed()
        rule.runOnIdle { initial.completeExceptionally(java.io.IOException("Fixture bootstrap unavailable")) }
        rule.waitUntil(10_000) { !vm.uiState.value.isLoading && !vm.uiState.value.isRefreshing }
        rule.onNodeWithTag("science-launch-experience").assertDoesNotExist()
        rule.onNodeWithTag("primary-navigation-opportunities").assertIsDisplayed().performClick()
        rule.onNodeWithTag("browse-results").performScrollToNode(hasTestTag("feed-error-retry"))
        rule.onNodeWithTag("feed-error-retry").assertIsDisplayed().performClick()
        rule.waitUntil(10_000) { !vm.uiState.value.isRefreshing }
        rule.onNodeWithTag("science-launch-experience").assertDoesNotExist()
        rule.onNodeWithTag("primary-navigation-home").assertIsDisplayed()
    }

    private class PendingBootstrapRepository(
        private val initial: CompletableDeferred<OpportunityFeedSnapshot>,
    ) : OpportunityRepository {
        @Volatile private var loaded: OpportunityFeedSnapshot? = null
        override suspend fun bootstrap() = initial.await().also { loaded = it }
        override suspend fun refresh() = loaded ?: throw java.io.IOException("Fixture refresh unavailable")
        override fun current() = loaded
        override fun findById(id: String) = loaded?.opportunities?.firstOrNull { it.id == id }
        override fun search(query: String, filters: OpportunitySearchFilters, language: AppLanguage) =
            OpportunitySearch.search(loaded?.opportunities.orEmpty(), query, filters)
    }

    @Test fun filteredMapOpensSameDetailsAndRetainsSelectionAndSaveOnReturn() {
        val programs = listOf(program(), program("online").copy(title = "Online science", virtual = true))
        val persistence = MemoryFavorites()
        lateinit var viewModel: BrowseViewModel
        rule.setContent {
            viewModel = remember { BrowseViewModel(FixtureRepository(programs), PersistentFavoriteOpportunityStore(persistence)) }
            GTAFreeStemTheme {
                GTAFreeStemApp(preferences = preferences(), viewModel = viewModel)
            }
        }
        rule.onNodeWithTag("primary-navigation-opportunities").performClick()
        rule.onNode(hasSetTextAction()).performTextReplacement("Toronto Lab")
        rule.onNodeWithTag("browse-map-view").performScrollTo().performClick()
        // Deterministic fallback-route coverage; live MapLibre gestures are
        // checked separately on the installed candidate against real venues.
        rule.onNodeWithTag("browse-results").performScrollToNode(hasTestTag("map-offline-control"))
        rule.onNodeWithTag("map-offline-control").performClick()
        rule.onNodeWithTag("browse-results").performScrollToNode(hasTestTag(OpportunityMapTestTags.MAP))
        rule.onNodeWithTag(OpportunityMapTestTags.MAP).assertIsDisplayed()
        rule.onNodeWithTag(OpportunityMapTestTags.marker("toronto")).performClick()
        rule.onNodeWithTag(OpportunityMapTestTags.DETAILS_ACTION).performScrollTo().performClick()
        rule.onNodeWithTag("opportunity-detail").assertIsDisplayed()
        rule.onNodeWithText("Save", substring = false).performScrollTo().performClick()
        rule.runOnIdle { assertTrue("toronto" in viewModel.uiState.value.favoriteIds) }
        rule.onNodeWithTag("opportunity-calendar").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Back").performScrollTo().performClick()
        rule.onNodeWithTag("browse-map-view").performScrollTo().assertIsSelected()
        rule.onNodeWithTag("browse-results").performScrollToNode(hasTestTag("map-offline-control"))
        rule.onNodeWithTag("map-offline-control").assertIsSelected()
        rule.onNodeWithTag("browse-results").performScrollToNode(hasTestTag(OpportunityMapTestTags.marker("toronto")))
        rule.onNodeWithTag(OpportunityMapTestTags.marker("toronto")).assertIsSelected()
        rule.onNodeWithTag("browse-list-view").performScrollTo().performClick()
        rule.onNodeWithTag("browse-results").performScrollToNode(hasText("Toronto Lab") and !hasSetTextAction())
        rule.onNode(hasText("Toronto Lab") and !hasSetTextAction()).assertIsDisplayed()
        rule.runOnIdle { assertEquals("Toronto Lab", viewModel.uiState.value.query) }
        // Rehydrate the same persistence contract as a relaunch, without touching real preferences.
        assertTrue(PersistentFavoriteOpportunityStore(persistence).contains("toronto"))
    }

    @Test fun onlineResultsRemainReachableInListWhenMapHasNoPhysicalLocations() {
        rule.setContent {
            val vm = remember { BrowseViewModel(FixtureRepository(listOf(program("online").copy(virtual = true))), PersistentFavoriteOpportunityStore(MemoryFavorites())) }
            GTAFreeStemTheme { GTAFreeStemApp(preferences = preferences(), viewModel = vm) }
        }
        rule.onNodeWithTag("primary-navigation-opportunities").performClick()
        rule.onNodeWithTag("browse-map-view").performScrollTo().performClick()
        // Deterministic fallback-route coverage; live MapLibre gestures are
        // checked separately on the installed candidate against real venues.
        rule.onNodeWithTag("browse-results").performScrollToNode(hasTestTag("map-offline-control"))
        rule.onNodeWithTag("map-offline-control").performClick()
        rule.onNodeWithTag("browse-results").performScrollToNode(hasTestTag(OpportunityMapTestTags.EMPTY))
        rule.onNodeWithTag(OpportunityMapTestTags.EMPTY).assertIsDisplayed()
        rule.onNodeWithTag("browse-list-view").performScrollTo().performClick()
        rule.onNodeWithTag("browse-results").performScrollToNode(hasText("Toronto Lab") and !hasSetTextAction())
        rule.onNode(hasText("Toronto Lab") and !hasSetTextAction()).assertIsDisplayed()
    }

    @Test fun calendarIntentPreservesDateOnlyInclusiveEndAndSuppliedTimes() {
        val allDay = OpportunityCalendar.intent(program(), "Localized title", "Toronto")
        assertEquals(Intent.ACTION_INSERT, allDay.action)
        assertEquals(CalendarContract.Events.CONTENT_URI, allDay.data)
        assertEquals("Localized title", allDay.getStringExtra(CalendarContract.Events.TITLE))
        assertTrue(allDay.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false))
        assertEquals(Instant.parse("2099-10-26T00:00:00Z").toEpochMilli(), allDay.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, -1))
        assertEquals(Instant.parse("2099-10-27T00:00:00Z").toEpochMilli(), allDay.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, -1))
        assertTrue(allDay.getStringExtra(CalendarContract.Events.DESCRIPTION)!!.contains(program().sourceUrl))
        val timed = OpportunityCalendar.intent(program().copy(startDate = "2099-10-26T14:00:00-04:00", endDate = "2099-10-26T15:30:00-04:00"), "Title", "Toronto")
        assertFalse(timed.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true))
        assertEquals(Instant.parse("2099-10-26T18:00:00Z").toEpochMilli(), timed.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, -1))
        assertEquals(Instant.parse("2099-10-26T19:30:00Z").toEpochMilli(), timed.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, -1))
    }

    @Test fun calendarNeverInventsMissingDatesOrRequestsCalendarPermissions() {
        val intent = OpportunityCalendar.intent(program().copy(startDate = null, endDate = null), "Title", "Toronto")
        assertFalse(intent.hasExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME))
        assertFalse(intent.hasExtra(CalendarContract.EXTRA_EVENT_END_TIME))
        val reversed = OpportunityCalendar.intent(program().copy(endDate = "2099-10-25"), "Title", "Toronto")
        assertFalse(reversed.hasExtra(CalendarContract.EXTRA_EVENT_END_TIME))
        @Suppress("DEPRECATION")
        val permissions = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertFalse("android.permission.READ_CALENDAR" in permissions)
        assertFalse("android.permission.WRITE_CALENDAR" in permissions)
    }

    @Test fun visibleCalendarActionOpensUserFormAndMissingHandlerShowsExplanation() {
        var captured: Intent? = null
        var missing = false
        val handler = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) {
                if (missing) throw ActivityNotFoundException("No calendar fixture handler")
                captured = intent
            }
        }
        rule.setContent {
            CompositionLocalProvider(LocalContext provides handler) {
                MaterialTheme {
                    OpportunityDetailScreen(program(), false, {}, {}, AppLanguage.ENGLISH, catalog, ::text)
                }
            }
        }
        rule.onNodeWithTag("opportunity-calendar").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(Intent.ACTION_INSERT, captured?.action)
            assertEquals(CalendarContract.Events.CONTENT_URI, captured?.data)
            missing = true
        }
        rule.onNodeWithTag("opportunity-calendar").performClick()
        rule.onNodeWithTag("opportunity-calendar-unavailable").performScrollTo().assertIsDisplayed()
    }

    @Test fun largeTextLandscapeKeepsMapControlsAndCalendarFallbackReachable() {
        val missingCalendar = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) { throw ActivityNotFoundException("Missing fixture handler") }
        }
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(700.dp, 280.dp))) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 2f), LocalContext provides missingCalendar) {
                    val vm = remember { BrowseViewModel(FixtureRepository(listOf(program())), PersistentFavoriteOpportunityStore(MemoryFavorites())) }
                    GTAFreeStemTheme { GTAFreeStemApp(preferences = preferences(), viewModel = vm) }
                }
            }
        }
        rule.onNodeWithTag("primary-navigation-rail-list").performScrollToIndex(1)
        rule.onNodeWithTag("primary-navigation-opportunities").performClick()
        rule.onNodeWithTag("browse-map-view").performScrollTo().assertIsDisplayed()
        val listBounds = rule.onNodeWithTag("browse-list-view").getUnclippedBoundsInRoot()
        val mapBounds = rule.onNodeWithTag("browse-map-view").getUnclippedBoundsInRoot()
        assertTrue(listBounds.right <= mapBounds.left)
        assertTrue((listBounds.bottom - listBounds.top) >= 48.dp && (mapBounds.bottom - mapBounds.top) >= 48.dp)
        rule.onNodeWithTag("browse-map-view").performClick()
        rule.onNodeWithTag("browse-results").performScrollToNode(hasTestTag("map-offline-control"))
        rule.onNodeWithTag("map-offline-control").performClick()
        rule.onNodeWithTag("browse-results").performScrollToNode(hasTestTag(OpportunityMapTestTags.marker("toronto")))
        rule.onNodeWithTag(OpportunityMapTestTags.marker("toronto")).performClick()
        rule.onNodeWithTag(OpportunityMapTestTags.DETAILS_ACTION).performScrollTo().performClick()
        rule.onNodeWithTag("opportunity-detail").performScrollToNode(hasTestTag("opportunity-calendar"))
        rule.onNodeWithTag("opportunity-calendar").assertIsDisplayed().performClick()
        rule.onNodeWithTag("opportunity-detail").performScrollToNode(hasTestTag("opportunity-calendar-unavailable"))
        rule.onNodeWithTag("opportunity-calendar-unavailable").assertIsDisplayed()
    }

    @Test fun firstResultIsVisibleWithoutScrollingAtNormalPhoneSize() {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 800.dp))) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
                    val vm = remember { BrowseViewModel(FixtureRepository(listOf(program())), PersistentFavoriteOpportunityStore(MemoryFavorites())) }
                    GTAFreeStemTheme { GTAFreeStemApp(preferences = preferences(), viewModel = vm) }
                }
            }
        }
        rule.onNodeWithTag("primary-navigation-opportunities").performClick()
        rule.onNode(hasSetTextAction()).assertIsDisplayed()
        rule.onNodeWithTag("browse-list-view").assertIsDisplayed()
        rule.onNodeWithTag("browse-map-view").assertIsDisplayed()
        rule.onNode(hasText("Toronto Lab") and !hasSetTextAction()).assertIsDisplayed()
    }

    @Test fun largeTextPhonePreservesListingCategoryAndFreePromise() {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 640.dp))) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                    val vm = remember { BrowseViewModel(FixtureRepository(listOf(program().copy(category = "Science & Engineering"))), PersistentFavoriteOpportunityStore(MemoryFavorites())) }
                    GTAFreeStemTheme { GTAFreeStemApp(preferences = preferences(), viewModel = vm) }
                }
            }
        }
        rule.onNodeWithTag("primary-navigation-opportunities").performClick()
        rule.onNodeWithTag("browse-results").performScrollToNode(hasTestTag("opportunity-card-toronto"))
        val category = rule.onNode(hasText("Science & Engineering") and hasAnyAncestor(hasTestTag("opportunity-card-toronto")))
        val price = rule.onNode(hasText("Free for everyone") and hasAnyAncestor(hasTestTag("opportunity-card-toronto")))
        category.performScrollTo().assertIsDisplayed()
        price.performScrollTo().assertIsDisplayed()
        val c = category.getUnclippedBoundsInRoot()
        val f = price.getUnclippedBoundsInRoot()
        assertTrue("Category and free promise must not overlap", c.bottom <= f.top || c.right <= f.left)
    }

    @Test fun failedEmptyFeedOffersExactWebsiteAndMissingHandlerFeedback() {
        var captured: Intent? = null
        var missing = false
        val browser = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) {
                if (missing) throw ActivityNotFoundException("No browser fixture handler")
                captured = intent
            }
        }
        rule.setContent {
            CompositionLocalProvider(LocalContext provides browser) {
                val vm = remember { BrowseViewModel(FixtureRepository(emptyList(), failRefresh = true), PersistentFavoriteOpportunityStore(MemoryFavorites())) }
                GTAFreeStemTheme { GTAFreeStemApp(preferences = preferences(), viewModel = vm) }
            }
        }
        rule.onNodeWithTag("primary-navigation-opportunities").performClick()
        rule.onNodeWithTag("browse-results").performScrollToNode(hasTestTag("browse-online"))
        rule.onNodeWithTag("browse-online").assertIsDisplayed().performClick()
        rule.runOnIdle {
            assertEquals(Intent.ACTION_VIEW, captured?.action)
            assertEquals("https://gta-free-stem.pages.dev/opportunities/", captured?.data.toString())
            assertTrue(captured?.categories?.contains(Intent.CATEGORY_BROWSABLE) == true)
            missing = true
        }
        rule.onNodeWithTag("browse-online").performClick()
        rule.onNodeWithTag("browse-results").performScrollToNode(hasTestTag("browse-online-unavailable"))
        rule.onNodeWithTag("browse-online-unavailable").assertIsDisplayed()
        rule.onAllNodesWithText("Refresh").onLast().performScrollTo().assertIsDisplayed()
    }

    @Test fun successfulEmptyQueryDoesNotOfferFailureWebsiteShortcut() {
        rule.setContent {
            val vm = remember { BrowseViewModel(FixtureRepository(listOf(program())), PersistentFavoriteOpportunityStore(MemoryFavorites())) }
            GTAFreeStemTheme { GTAFreeStemApp(preferences = preferences(), viewModel = vm) }
        }
        rule.onNodeWithTag("primary-navigation-opportunities").performClick()
        rule.onNode(hasSetTextAction()).performTextReplacement("No matching listing")
        rule.onNodeWithTag("browse-online").assertDoesNotExist()
    }

    private fun preferences() = AppPreferencesUiState(AppLanguage.ENGLISH, AppLanguage.ENGLISH, emptyList(), TextDirection.LEFT_TO_RIGHT, null, AppThemePreference.SYSTEM, false, catalog)
    private class MemoryFavorites : FavoriteOpportunityPersistence {
        private var snapshot: String? = null
        override fun readSnapshotJson() = snapshot
        override fun readLegacyIds() = emptySet<String>()
        override fun write(snapshotJson: String, ids: Set<String>): Boolean { snapshot = snapshotJson; return true }
        override fun clear(): Boolean { snapshot = null; return true }
    }
    private class FixtureRepository(programs: List<Opportunity>, private val failRefresh: Boolean = false) : OpportunityRepository {
        private val snapshot = OpportunityFeedSnapshot(programs, Instant.parse("2099-10-25T00:00:00Z"), OpportunityFeedSource.BUNDLED, programs.size, false)
        override suspend fun bootstrap() = snapshot
        override suspend fun refresh(): OpportunityFeedSnapshot {
            if (failRefresh) error("Fixture network unavailable")
            return snapshot
        }
        override fun current() = snapshot
        override fun findById(id: String) = snapshot.opportunities.firstOrNull { it.id == id }
        override fun search(query: String, filters: OpportunitySearchFilters, language: AppLanguage) = OpportunitySearch.search(snapshot.opportunities, query, filters)
    }
}
