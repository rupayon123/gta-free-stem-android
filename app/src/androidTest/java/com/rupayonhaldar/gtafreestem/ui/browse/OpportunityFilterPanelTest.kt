package com.rupayonhaldar.gtafreestem.ui.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchFilters
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchOptions
import com.rupayonhaldar.gtafreestem.theme.GTAFreeStemTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpportunityFilterPanelTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun narrowPanelScrollsToEverySectionAndKeepsInteractiveTargetsAtLeast48Dp() {
        var emitted: OpportunitySearchFilters? = null
        setPanelContent(
            filters = OpportunitySearchFilters(region = "Toronto"),
            onFiltersChange = { emitted = it },
        )

        composeRule.onNodeWithTag(OpportunityFilterPanelTestTags.ACTIVE_COUNT)
            .assertTextEquals("1 filter active")
        composeRule.onNodeWithTag(OpportunityFilterPanelTestTags.REGION)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithText("Peel").performClick()
        composeRule.runOnIdle { assertEquals("Peel", emitted?.region) }

        composeRule.onNodeWithTag(OpportunityFilterPanelTestTags.LEADERSHIP)
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.runOnIdle { assertTrue(emitted?.leadershipOnly == true) }

        composeRule.onNodeWithTag(OpportunityFilterPanelTestTags.SORT_RELEVANCE)
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun resetIsOneAtomicCallbackAndDoesNotEmitIndividualFilterChanges() {
        var resetCalls = 0
        var filterChangeCalls = 0
        setPanelContent(
            filters = OpportunitySearchFilters(
                adultsOnly = true,
                volunteerHoursOnly = true,
            ),
            onFiltersChange = { filterChangeCalls += 1 },
            onReset = { resetCalls += 1 },
        )

        composeRule.onNodeWithTag(OpportunityFilterPanelTestTags.ACTIVE_COUNT)
            .assertTextEquals("2 filters active")
        composeRule.onNodeWithTag(OpportunityFilterPanelTestTags.RESET)
            .assertIsEnabled()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, resetCalls)
            assertEquals(0, filterChangeCalls)
        }
    }

    private fun setPanelContent(
        filters: OpportunitySearchFilters,
        onFiltersChange: (OpportunitySearchFilters) -> Unit,
        onReset: () -> Unit = {},
    ) {
        composeRule.setContent {
            GTAFreeStemTheme {
                Box(modifier = Modifier.width(320.dp).height(640.dp)) {
                    OpportunityFilterPanel(
                        filters = filters,
                        options = Options,
                        onFiltersChange = onFiltersChange,
                        onReset = onReset,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    private companion object {
        val Options = OpportunitySearchOptions(
            regions = listOf("Peel", "Toronto", "York"),
            cities = listOf("Brampton", "Mississauga", "Toronto"),
            categories = listOf("Coding & Robotics", "Science & Engineering"),
            languages = listOf("en", "fr"),
        )
    }
}
