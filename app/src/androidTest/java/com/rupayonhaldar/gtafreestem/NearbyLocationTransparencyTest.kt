package com.rupayonhaldar.gtafreestem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchFilters
import com.rupayonhaldar.gtafreestem.localization.AndroidAppStringCatalogLoader
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.platform.location.NearbyLocationFix
import com.rupayonhaldar.gtafreestem.platform.location.NearbyLocationFixSource
import com.rupayonhaldar.gtafreestem.platform.location.NearbyLocationState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NearbyLocationTransparencyTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun currentFixChangesToLocalizedRecentApproximateDisclosureAndStaysPolite() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val catalog = AndroidAppStringCatalogLoader.load(context)
        val language = AppLanguage.FRENCH
        val currentStatus = catalog.text("nearbyHuntingOn", language)
        val recentStatus = catalog.text("nearbyRecentApproximateLocation", language)
        var locationState by mutableStateOf(located(NearbyLocationFixSource.CURRENT))

        composeRule.setContent {
            MaterialTheme {
                NearbyAndAlertsControls(
                    filters = FiltersWithLocation,
                    nearbyLocationState = locationState,
                    onUseNearby = {},
                    onClearNearby = {},
                    alertsEnabled = false,
                    onToggleAlerts = {},
                    text = { key, fallback ->
                        catalog.text(key, language).takeUnless {
                            it.isBlank() || it == key
                        } ?: fallback
                    },
                )
            }
        }

        assertPoliteStatus(currentStatus)
        composeRule.onNodeWithText(recentStatus).assertDoesNotExist()

        composeRule.runOnIdle {
            locationState = located(NearbyLocationFixSource.LAST_KNOWN)
        }

        composeRule.onNodeWithText(currentStatus).assertDoesNotExist()
        assertPoliteStatus(recentStatus)
        RawFixFields.forEach { rawValue ->
            composeRule.onNodeWithText(rawValue, substring = true).assertDoesNotExist()
        }
    }

    private fun assertPoliteStatus(status: String) {
        composeRule.onNodeWithText(status)
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite,
                ),
            )
    }

    private fun located(source: NearbyLocationFixSource) = NearbyLocationState.Located(
        NearbyLocationFix(
            latitude = Latitude,
            longitude = Longitude,
            accuracyMeters = 1_000f,
            capturedAtEpochMillis = CapturedAtEpochMillis,
            source = source,
        ),
    )

    private companion object {
        const val Latitude = 43.6532
        const val Longitude = -79.3832
        const val CapturedAtEpochMillis = 1_725_000_000_000L
        val FiltersWithLocation = OpportunitySearchFilters(
            latitude = Latitude,
            longitude = Longitude,
            distanceKm = 25,
        )
        val RawFixFields = listOf(
            Latitude.toString(),
            Longitude.toString(),
            CapturedAtEpochMillis.toString(),
        )
    }
}
