package com.rupayonhaldar.gtafreestem

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchFilters
import com.rupayonhaldar.gtafreestem.localization.AndroidAppStringCatalogLoader
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.platform.location.NearbyLocationState
import com.rupayonhaldar.gtafreestem.ui.preferences.OpportunityAlertFeedback
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpportunityAlertFeedbackTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun deniedPermissionShowsLocalizedPoliteSnackbar() {
        assertFeedback(
            feedback = OpportunityAlertFeedback.PERMISSION_DENIED,
            language = AppLanguage.FRENCH,
        )
    }

    @Test
    fun blockedChannelOrSchedulingFailureShowsLocalizedPoliteSnackbar() {
        assertFeedback(
            feedback = OpportunityAlertFeedback.UNAVAILABLE,
            language = AppLanguage.ARABIC,
        )
    }

    @Test
    fun browseAlertControlDelegatesToTheSharedPreferenceAction() {
        var toggleCount = 0
        composeRule.setContent {
            MaterialTheme {
                NearbyAndAlertsControls(
                    filters = OpportunitySearchFilters(),
                    nearbyLocationState = NearbyLocationState.Idle,
                    onUseNearby = {},
                    onClearNearby = {},
                    alertsEnabled = false,
                    onToggleAlerts = { toggleCount += 1 },
                    text = { _, fallback -> fallback },
                )
            }
        }

        composeRule.onNodeWithTag(BROWSE_ALERTS_TOGGLE_TEST_TAG).performClick()

        composeRule.runOnIdle { assertEquals(1, toggleCount) }
    }

    private fun assertFeedback(
        feedback: OpportunityAlertFeedback,
        language: AppLanguage,
    ) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val catalog = AndroidAppStringCatalogLoader.load(context)
        val events = Channel<OpportunityAlertFeedback>(Channel.BUFFERED)
        val expected = catalog.text(feedback.catalogKey, language)
        assertTrue(expected.isNotBlank() && expected != feedback.catalogKey)

        composeRule.setContent {
            MaterialTheme {
                Box {
                    OpportunityAlertFeedbackHost(
                        feedback = events.receiveAsFlow(),
                        languageKey = language,
                        text = { key, fallback ->
                            catalog.text(key, language).takeUnless {
                                it.isBlank() || it == key
                            } ?: fallback
                        },
                        modifier = Modifier,
                    )
                }
            }
        }
        composeRule.runOnIdle {
            assertTrue(events.trySend(feedback).isSuccess)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(OPPORTUNITY_ALERT_FEEDBACK_TEST_TAG)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNodeWithTag(OPPORTUNITY_ALERT_FEEDBACK_TEST_TAG)
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite,
                ),
            )
        composeRule.onNodeWithText(expected).assertIsDisplayed()
    }
}
