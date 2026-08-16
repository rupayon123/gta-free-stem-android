package com.rupayonhaldar.gtafreestem

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rupayonhaldar.gtafreestem.data.local.AppThemePreference
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.ui.shell.PrimaryDestination
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun useDeterministicPreferences() {
        composeRule.runOnIdle {
            composeRule.activity.preferencesViewModel.setLanguage(AppLanguage.ENGLISH)
            composeRule.activity.preferencesViewModel.setTheme(AppThemePreference.SYSTEM)
        }
    }

    @After
    fun restoreSystemPreferences() {
        composeRule.runOnIdle {
            composeRule.activity.preferencesViewModel.useSystemLanguage()
            composeRule.activity.preferencesViewModel.setTheme(AppThemePreference.SYSTEM)
        }
    }

    @Test
    fun primaryNavigationHasExactlyFiveDestinationsAndNavigates() {
        val destinations = listOf(
            "home" to "Home",
            "opportunities" to "Opportunities",
            "high_school" to "High School",
            "support" to "Support",
            "account" to "Account",
        )

        destinations.forEach { (tag, label) ->
            composeRule.onNodeWithTag("primary-navigation-$tag")
                .assertIsDisplayed()
                .assertTextContains(label)
        }

        composeRule.onNodeWithText("Everything here is free for everyone.").assertIsDisplayed()

        composeRule.onNodeWithTag("primary-navigation-opportunities").performClick()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("")
        composeRule.onNodeWithTag("browse-screen-title")
            .assertTextContains("Opportunities")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Filters").performClick()
        composeRule.onNodeWithTag("opportunity-filter-panel").assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithTag("browse-screen-title")
            .assertTextContains("Opportunities")
            .assertIsDisplayed()

        composeRule.onNodeWithTag("primary-navigation-high_school").performClick()
        composeRule.onNodeWithText("Programs for teens", substring = true).assertIsDisplayed()

        composeRule.onNodeWithTag("primary-navigation-support").performClick()
        composeRule.onNodeWithText("Community-built").assertIsDisplayed()
        composeRule.onNodeWithTag("support-screen").performScrollToIndex(4)
        composeRule.onNodeWithText("Send feedback").assertIsDisplayed()
        composeRule.onNodeWithText("Privacy policy").assertIsDisplayed()
        composeRule.onNodeWithText("Terms of Service and Privacy Notice").assertIsDisplayed()

        composeRule.onNodeWithTag("primary-navigation-account").performClick()
        composeRule.onNodeWithText("Profile").assertIsDisplayed()
        composeRule.onNodeWithText(
            "This profile and its saved opportunities stay on this device.",
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("account-preferences-saved-library")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("saved-library-screen").assertIsDisplayed()
        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithText("Profile").assertIsDisplayed()
    }

    @Test
    fun selectedLanguageLocalizesFiveDestinationsAndMirrorsRtlNavigation() {
        composeRule.runOnIdle {
            assertTrue(
                composeRule.activity.preferencesViewModel.setLanguage(AppLanguage.FRENCH),
            )
        }
        composeRule.waitUntil {
            composeRule.activity.preferencesViewModel.uiState.value.resolvedLanguage ==
                AppLanguage.FRENCH
        }

        val frenchState = composeRule.activity.preferencesViewModel.uiState.value
        PrimaryDestination.entries.forEach { destination ->
            composeRule.onNodeWithTag("primary-navigation-${destination.name.lowercase()}")
                .assertTextContains(frenchState.text(destination.catalogKey))
        }
        composeRule.onNodeWithText(frenchState.text("mission")).assertIsDisplayed()
        composeRule.onNodeWithTag("primary-navigation-opportunities").performClick()
        // The browse query is intentionally persisted on-device. Clear any value left by a
        // previous test/run so this assertion verifies the localized empty-state placeholder.
        composeRule.onNode(hasSetTextAction()).performTextReplacement("")
        composeRule.onNodeWithTag("browse-screen-title")
            .assertTextContains(frenchState.text("navOpportunities"))
        composeRule.onNodeWithText(frenchState.text("searchPlaceholder")).assertIsDisplayed()

        composeRule.runOnIdle {
            assertTrue(
                composeRule.activity.preferencesViewModel.setLanguage(AppLanguage.ARABIC),
            )
        }
        composeRule.waitUntil {
            composeRule.activity.preferencesViewModel.uiState.value.resolvedLanguage ==
                AppLanguage.ARABIC
        }

        val arabicState = composeRule.activity.preferencesViewModel.uiState.value
        PrimaryDestination.entries.forEach { destination ->
            composeRule.onNodeWithTag("primary-navigation-${destination.name.lowercase()}")
                .assertTextContains(arabicState.text(destination.catalogKey))
        }
        composeRule.onNodeWithTag("browse-screen-title")
            .assertTextContains(arabicState.text("navOpportunities"))
        composeRule.onNodeWithText(arabicState.text("searchPlaceholder")).assertIsDisplayed()

        val rootBounds = composeRule.onRoot().getUnclippedBoundsInRoot()
        if (rootBounds.right - rootBounds.left < 600.dp) {
            val homeBounds = composeRule.onNodeWithTag("primary-navigation-home")
                .getUnclippedBoundsInRoot()
            val accountBounds = composeRule.onNodeWithTag("primary-navigation-account")
                .getUnclippedBoundsInRoot()
            assertTrue(
                "RTL should place Home on the logical start (right) edge",
                homeBounds.left > accountBounds.left,
            )
        } else {
            val railBounds = composeRule.onNodeWithTag("primary-navigation-rail")
                .getUnclippedBoundsInRoot()
            assertTrue(
                "RTL should place the navigation rail on the logical start (right) edge",
                railBounds.left > rootBounds.left + (rootBounds.right - rootBounds.left) / 2,
            )
        }
    }

    @Test
    fun searchStateSurvivesPrimaryNavigation() {
        composeRule.onNodeWithTag("primary-navigation-opportunities").performClick()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("Conservation Youth Corps")

        composeRule.onNodeWithTag("primary-navigation-home").performClick()
        composeRule.onNodeWithTag("primary-navigation-opportunities").performClick()
        composeRule.onNode(hasSetTextAction()).assertTextContains(
            "Conservation Youth Corps",
            false,
            true,
        )
    }

    @Test
    fun primaryNavigationDoesNotOverlapSystemNavigation() {
        composeRule.waitForIdle()

        val rootBounds = composeRule.onRoot().getUnclippedBoundsInRoot()
        val homeBounds = composeRule.onNodeWithTag("primary-navigation-home")
            .getUnclippedBoundsInRoot()
        val accountBounds = composeRule.onNodeWithTag("primary-navigation-account")
            .getUnclippedBoundsInRoot()
        val navigationBarInsetsPx = ViewCompat
            .getRootWindowInsets(composeRule.activity.window.decorView)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())
            ?: androidx.core.graphics.Insets.NONE
        val navigationBarInsets = with(composeRule.density) {
            listOf(
                navigationBarInsetsPx.left.toDp(),
                navigationBarInsetsPx.top.toDp(),
                navigationBarInsetsPx.right.toDp(),
                navigationBarInsetsPx.bottom.toDp(),
            )
        }
        val safeLeft = rootBounds.left + navigationBarInsets[0]
        val safeTop = rootBounds.top + navigationBarInsets[1]
        val safeRight = rootBounds.right - navigationBarInsets[2]
        val safeBottom = rootBounds.bottom - navigationBarInsets[3]

        assertTrue(
            "Expected a visible system navigation inset for this regression test",
            navigationBarInsetsPx != androidx.core.graphics.Insets.NONE,
        )
        assertTrue(
            "Primary navigation started outside the safe area at $safeLeft, $safeTop",
            homeBounds.left >= safeLeft && homeBounds.top >= safeTop,
        )
        assertTrue(
            "Primary navigation ended outside the safe area at $safeRight, $safeBottom",
            accountBounds.right <= safeRight && accountBounds.bottom <= safeBottom,
        )

        val expectedContainer = if (rootBounds.right - rootBounds.left >= 600.dp) {
            "primary-navigation-rail"
        } else {
            "primary-navigation-bar"
        }
        composeRule.onNodeWithTag(expectedContainer).assertIsDisplayed()
    }
}
