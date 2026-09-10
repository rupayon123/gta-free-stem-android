package com.rupayonhaldar.gtafreestem

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rupayonhaldar.gtafreestem.data.local.AppThemePreference
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenTransitionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun waitForDeterministicHomeScreen() {
        composeRule.runOnIdle {
            composeRule.activity.preferencesViewModel.setLanguage(AppLanguage.ENGLISH)
            composeRule.activity.preferencesViewModel.setTheme(AppThemePreference.SYSTEM)
        }
        // A pristine API 36 AVD may spend tens of seconds verifying the app and decoding the
        // bundled feed before the launch experience yields to primary navigation. Keep the
        // behavioral transition assertion strict while allowing that one-time device work.
        composeRule.waitUntil(timeoutMillis = 60_000) {
            composeRule.onAllNodesWithTag("primary-navigation-home")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @After
    fun restorePreferencesAndClock() {
        composeRule.mainClock.autoAdvance = true
        composeRule.runOnIdle {
            composeRule.activity.preferencesViewModel.useSystemLanguage()
            composeRule.activity.preferencesViewModel.setTheme(AppThemePreference.SYSTEM)
        }
    }

    @Test
    fun transitionKeepsOneOutgoingAndOneIncomingPane() {
        val homePane = SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Home")
        val opportunitiesPane = SemanticsMatcher.expectValue(
            SemanticsProperties.PaneTitle,
            "Opportunities",
        )

        composeRule.onAllNodes(homePane, useUnmergedTree = true).assertCountEquals(1)
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("primary-navigation-opportunities").performClick()
        composeRule.mainClock.advanceTimeBy(80)

        composeRule.onAllNodes(homePane, useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodes(opportunitiesPane, useUnmergedTree = true).assertCountEquals(1)
    }
}
