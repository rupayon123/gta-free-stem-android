package com.rupayonhaldar.gtafreestem.ui.saved

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rupayonhaldar.gtafreestem.data.local.SavedOpportunityEntry
import com.rupayonhaldar.gtafreestem.data.local.SavedOpportunitySections
import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.localization.AndroidAppStringCatalogLoader
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavedOpportunityLibraryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun currentArchiveLegacyAndExactOfflineDetailCallbackAreExposed() {
        val current = entry(opportunity("current", status = "active"))
        val archived = entry(
            opportunity("archived", status = "removed").copy(
                equipment = "Equipment provided",
                accessibility = listOf("Wheelchair accessible"),
            ),
        )
        var opened: Opportunity? = null

        setScreen(
            sections = SavedOpportunitySections(
                current = listOf(current),
                archive = listOf(archived),
            ),
            unresolvedLegacyCount = 2,
            onOpenDetail = { opened = it },
        )

        composeRule.onNodeWithTag(SavedOpportunityLibraryTestTags.SCREEN)
            .performScrollToKey("legacy")
        composeRule.onNodeWithText("2 items", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag(SavedOpportunityLibraryTestTags.SCREEN)
            .performScrollToKey("current-header")
        composeRule.onNodeWithText("Current (1)").assertIsDisplayed()
        composeRule.onNodeWithTag(SavedOpportunityLibraryTestTags.SCREEN)
            .performScrollToKey("archive-header")
        composeRule.onNodeWithText("Archive (1)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(SavedOpportunityLibraryTestTags.SCREEN)
            .performScrollToKey("archive-archived")
        composeRule.onNodeWithTag(SavedOpportunityLibraryTestTags.details("archived"))
            .performScrollTo()
            .performClick()
        composeRule.runOnIdle { assertEquals(archived.opportunity, opened) }
        assertEquals("Equipment provided", opened?.equipment)
        assertEquals(listOf("Wheelchair accessible"), opened?.accessibility)
    }

    @Test
    fun clearAllRequiresConfirmationAndRemoveUsesRequestedOpportunity() {
        val saved = entry(opportunity("saved"))
        var clearCalled = false
        var removed: Opportunity? = null

        setScreen(
            sections = SavedOpportunitySections(current = listOf(saved), archive = emptyList()),
            onRemoveSaved = {
                removed = it
                true
            },
            onClearAllSaved = {
                clearCalled = true
                true
            },
        )

        composeRule.onNodeWithTag(SavedOpportunityLibraryTestTags.CLEAR_ALL).performClick()
        composeRule.runOnIdle { assertFalse(clearCalled) }
        composeRule.onNodeWithTag(SavedOpportunityLibraryTestTags.CLEAR_ALL_DIALOG)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(SavedOpportunityLibraryTestTags.CONFIRM_CLEAR_ALL)
            .performClick()
        composeRule.runOnIdle { assertTrue(clearCalled) }

        composeRule.onNodeWithTag(SavedOpportunityLibraryTestTags.SCREEN)
            .performScrollToKey("current-saved")
        composeRule.onNodeWithTag(SavedOpportunityLibraryTestTags.remove("saved"))
            .performScrollTo()
            .performClick()
        composeRule.runOnIdle { assertEquals(saved.opportunity, removed) }
    }

    @Test
    fun narrowLargeTextLayoutScrollsToActionsWithoutHorizontalActionRows() {
        val saved = entry(opportunity("large-text"))
        var backCalled = false

        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                MaterialTheme {
                    Box(Modifier.width(320.dp).height(640.dp)) {
                        SavedOpportunityLibraryScreen(
                            sections = SavedOpportunitySections(
                                current = listOf(saved),
                                archive = emptyList(),
                            ),
                            unresolvedLegacyCount = 0,
                            language = AppLanguage.ENGLISH,
                            catalog = catalog(),
                            onBack = { backCalled = true },
                            onOpenDetail = {},
                            onRemoveSaved = { true },
                            onClearAllSaved = { true },
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag(SavedOpportunityLibraryTestTags.BACK)
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle { assertTrue(backCalled) }
        composeRule.onNodeWithTag(SavedOpportunityLibraryTestTags.SCREEN)
            .performScrollToKey("current-large-text")
        composeRule.onNodeWithTag(SavedOpportunityLibraryTestTags.details("large-text"))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(SavedOpportunityLibraryTestTags.remove("large-text"))
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun setScreen(
        sections: SavedOpportunitySections,
        unresolvedLegacyCount: Int = 0,
        onOpenDetail: (Opportunity) -> Unit = {},
        onRemoveSaved: (Opportunity) -> Boolean = { true },
        onClearAllSaved: () -> Boolean = { true },
    ) {
        composeRule.setContent {
            MaterialTheme {
                SavedOpportunityLibraryScreen(
                    sections = sections,
                    unresolvedLegacyCount = unresolvedLegacyCount,
                    language = AppLanguage.ENGLISH,
                    catalog = catalog(),
                    onBack = {},
                    onOpenDetail = onOpenDetail,
                    onRemoveSaved = onRemoveSaved,
                    onClearAllSaved = onClearAllSaved,
                )
            }
        }
    }

    private fun catalog() = AndroidAppStringCatalogLoader.load(
        InstrumentationRegistry.getInstrumentation().targetContext,
    )

    private fun entry(opportunity: Opportunity) = SavedOpportunityEntry(
        opportunity = opportunity,
        savedAt = Instant.parse("2026-08-10T16:00:00Z"),
    )

    private fun opportunity(id: String, status: String = "active") = Opportunity(
        id = id,
        title = "Community Robotics $id",
        organization = "Central Library",
        description = "Build a robot with mentors and keep the complete details offline.",
        summary = "A complete free robotics workshop.",
        category = "Coding and Robotics",
        city = "Toronto",
        region = "Ontario",
        startDate = "2026-09-01",
        deadline = "2026-08-25",
        ageMin = 12,
        ageMax = 17,
        languages = listOf("en", "fr"),
        cost = "Free",
        sourceUrl = "https://example.org/source",
        status = status,
    )
}
