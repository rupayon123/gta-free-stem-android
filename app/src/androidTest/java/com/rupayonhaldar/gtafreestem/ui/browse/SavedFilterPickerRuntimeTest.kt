package com.rupayonhaldar.gtafreestem.ui.browse

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.rupayonhaldar.gtafreestem.data.local.SavedSearchEntry
import com.rupayonhaldar.gtafreestem.data.local.SavedSearchSummary
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchFilters
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchOptions
import com.rupayonhaldar.gtafreestem.theme.GTAFreeStemContentTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SavedFilterPickerRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun sectionOnlyFocusKeepsPanelResetEnabledUntilCleared() {
        val focused = mutableStateOf(true)
        compose.setContent {
            GTAFreeStemContentTheme(false) {
                OpportunityFilterPanel(filters = OpportunitySearchFilters(),
                    options = OpportunitySearchOptions(), onFiltersChange = {},
                    canResetOverride = focused.value,
                    onReset = { focused.value = false },
                    presetContent = {
                        androidx.compose.material3.Text(
                            if (focused.value) "High School · Volunteer hours · 3 matches"
                            else "High School · 5 matches",
                            modifier = androidx.compose.ui.Modifier.testTag("scope-preview"))
                    })
            }
        }
        compose.onNodeWithTag(OpportunityFilterPanelTestTags.RESET).assertIsEnabled().performClick()
        compose.onNodeWithTag(OpportunityFilterPanelTestTags.RESET).assertIsNotEnabled()
        compose.onNodeWithTag("scope-preview").assertTextContains("High School · 5 matches")
    }

    @Test
    fun presetUpdatesTheOpenPanelAndRequiresExplicitSaveForEdits() {
        val entry = SavedSearchEntry(id = "robotics", name = "Robotics", query = "robots",
            filters = OpportunitySearchFilters(city = "Toronto"), knownMatchingIds = emptyList())
        val state = mutableStateOf(BrowseUiState(savedSearches = listOf(SavedSearchSummary(entry, 0, 0))))
        var writes = 0
        val scope = mutableStateOf("High School · Volunteer hours")
        val scopedCount = mutableStateOf(3)
        compose.setContent {
            GTAFreeStemContentTheme(false) {
                SavedFilterPicker(state.value,
                    onSelect = { state.value = state.value.copy(query = entry.query,
                        filters = entry.filters, activeSavedSearchId = entry.id) },
                    onNew = {
                        state.value = state.value.copy(query = "", filters = OpportunitySearchFilters(),
                            activeSavedSearchId = null)
                        scope.value = "High School"
                        scopedCount.value = 5
                    },
                    onQueryChange = { state.value = state.value.copy(query = it) },
                    onSave = { _, update -> if (update) writes++ }, text = { _, fallback -> fallback },
                    resultCount = scopedCount.value, scopeLabel = scope.value)
            }
        }
        compose.onNodeWithTag("saved-filter-picker").performClick()
        compose.onNodeWithTag("saved-filter-robotics").performClick()
        compose.onNodeWithTag("filter-query").assertTextContains("robots")
        compose.onNodeWithTag("filter-live-criteria").assertTextContains("Toronto", substring = true)
        compose.onNodeWithTag("filter-live-count").assertTextContains("3 matches")
        compose.onNodeWithTag("filter-current-scope").assertTextContains("High School · Volunteer hours")
        compose.onNodeWithTag("filter-query").performTextReplacement("stars")
        compose.onNodeWithTag("filter-modified").assertExists()
        compose.runOnIdle { assertEquals(0, writes) }
        compose.onNodeWithTag("filter-save").performClick()
        compose.onNodeWithTag("filter-save-confirm").performClick()
        compose.runOnIdle { assertEquals(1, writes) }
        compose.onNodeWithTag("filter-new").performClick()
        compose.onNodeWithTag("filter-modified").assertDoesNotExist()
        compose.onNodeWithTag("filter-live-count").assertTextContains("5 matches")
        compose.onNodeWithTag("filter-current-scope").assertTextContains("High School")
        compose.runOnIdle { assertEquals(null, state.value.activeSavedSearchId) }
    }
}
