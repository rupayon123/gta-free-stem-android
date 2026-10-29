package com.rupayonhaldar.gtafreestem.ui.browse

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.rupayonhaldar.gtafreestem.data.local.SavedSearchEntry
import com.rupayonhaldar.gtafreestem.data.local.SavedSearchSummary
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchFilters
import com.rupayonhaldar.gtafreestem.theme.GTAFreeStemContentTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SavedFilterPickerRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun presetUpdatesTheOpenPanelAndRequiresExplicitSaveForEdits() {
        val entry = SavedSearchEntry(id = "robotics", name = "Robotics", query = "robots",
            filters = OpportunitySearchFilters(city = "Toronto"), knownMatchingIds = emptyList())
        val state = mutableStateOf(BrowseUiState(savedSearches = listOf(SavedSearchSummary(entry, 0, 0))))
        var writes = 0
        compose.setContent {
            GTAFreeStemContentTheme(false) {
                SavedFilterPicker(state.value,
                    onSelect = { state.value = state.value.copy(query = entry.query,
                        filters = entry.filters, activeSavedSearchId = entry.id) },
                    onNew = { state.value = state.value.copy(query = "", filters = OpportunitySearchFilters(),
                        activeSavedSearchId = null) },
                    onQueryChange = { state.value = state.value.copy(query = it) },
                    onSave = { _, update -> if (update) writes++ }, text = { _, fallback -> fallback })
            }
        }
        compose.onNodeWithTag("saved-filter-picker").performClick()
        compose.onNodeWithTag("saved-filter-robotics").performClick()
        compose.onNodeWithTag("filter-query").assertTextContains("robots")
        compose.onNodeWithTag("filter-live-criteria").assertTextContains("Toronto", substring = true)
        compose.onNodeWithTag("filter-live-count").assertTextContains("0 matches")
        compose.onNodeWithTag("filter-query").performTextReplacement("stars")
        compose.onNodeWithTag("filter-modified").assertExists()
        compose.runOnIdle { assertEquals(0, writes) }
        compose.onNodeWithTag("filter-save").performClick()
        compose.onNodeWithTag("filter-save-confirm").performClick()
        compose.runOnIdle { assertEquals(1, writes) }
        compose.onNodeWithTag("filter-new").performClick()
        compose.onNodeWithTag("filter-modified").assertDoesNotExist()
        compose.runOnIdle { assertEquals(null, state.value.activeSavedSearchId) }
    }
}
