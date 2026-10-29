package com.rupayonhaldar.gtafreestem.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Preset selection stays inside Filters. Only the explicit save dialog writes a preset. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedFilterPicker(
    state: BrowseUiState,
    onSelect: (String) -> Unit,
    onNew: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSave: (String, Boolean) -> Unit,
    text: (String, String) -> String,
    resultCount: Int = state.opportunities.size,
    scopeLabel: String? = null,
) {
    val active = state.savedSearches.firstOrNull { it.entry.id == state.activeSavedSearchId }?.entry
    val modified = active != null && (state.query != active.query ||
        state.filters.normalized() != active.filters.normalized())
    var expanded by rememberSaveable { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        scopeLabel?.let {
            Text(it, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("filter-current-scope"))
        }
        ExposedDropdownMenuBox(expanded, { expanded = it }, Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = active?.name ?: text("newFilter", "New filter"), onValueChange = {}, readOnly = true,
                label = { Text(text("mySavedSearches", "Saved filters")) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().testTag("saved-filter-picker")
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded, { expanded = false }) {
                DropdownMenuItem(text = { Text(text("newFilter", "New filter")) },
                    onClick = { expanded = false; onNew() }, modifier = Modifier.heightIn(min = 48.dp))
                state.savedSearches.forEach { summary ->
                    DropdownMenuItem(text = { Text(summary.entry.name) },
                        onClick = { expanded = false; onSelect(summary.entry.id) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("saved-filter-${summary.entry.id}"))
                }
            }
        }
        OutlinedTextField(value = state.query, onValueChange = onQueryChange,
            label = { Text(text("search", "Search")) }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("filter-query"))
        val criteria = buildList {
            state.filters.region?.let { add("${text("region", "Region")}: $it") }
            state.filters.city?.let { add("${text("city", "City")}: $it") }
            state.filters.category?.let { add("${text("category", "Category")}: $it") }
            state.filters.age?.let { add("${text("age", "Age")}: $it") }
            if (state.filters.adultsOnly) add("${text("age", "Age")}: 18+")
            if (state.filters.volunteerHoursOnly) add(text("volunteerHours", "Volunteer hours"))
            if (state.filters.coopOnly) add(text("coop", "Co-op"))
            if (state.filters.mentorshipOnly) add(text("mentorship", "Mentorship"))
            if (state.filters.scholarshipsOnly) add(text("categoryScholarships", "Scholarships"))
            if (state.filters.blackFocusedOnly) add(text("black", "Black-focused"))
            if (state.filters.girlsFocusedOnly) add(text("girls", "Girls-focused"))
            if (state.filters.indigenousFocusedOnly) add(text("indigenous", "Indigenous-focused"))
            if (state.filters.leadershipOnly) add(text("leadership", "Leadership"))
            add(text("includeNewFinds", "Include new finds") + ": " +
                if (state.filters.includeNewFinds) text("selectedState", "Selected")
                else text("notSelectedState", "Not selected"))
            state.filters.distanceKm?.let {
                add(text("distanceRadius", "Distance radius: {km} km").replace("{km}", it.toString()))
            }
            val sortLabel = when (state.filters.sort) {
                com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchSort.SOONEST -> text("sortSoonest", "Soonest")
                com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchSort.NEAREST -> text("sortNearest", "Nearest")
                com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchSort.RELEVANCE -> text("sortBestMatch", "Best match")
            }
            add(text("sortResults", "Sort results") + ": " + sortLabel)
        }
        Text(criteria.joinToString(" · ").ifBlank { text("any", "Any") },
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("filter-live-criteria"))
        Text("$resultCount ${text("matches", "matches")}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("filter-live-count").semantics { liveRegion = LiveRegionMode.Polite })
        state.savedSearchFeedback?.let { feedback ->
            val message = when (feedback) {
                SavedSearchFeedback.SAVED -> text("savedSearchSaved", "Filter saved on this device.")
                SavedSearchFeedback.UPDATED -> text("savedSearchUpdated", "Saved filter updated.")
                SavedSearchFeedback.DELETED -> text("savedSearchDeleted", "Saved filter deleted.")
                SavedSearchFeedback.OPENED -> text("savedSearchOpened", "Saved filter applied.")
                SavedSearchFeedback.INVALID_NAME -> text("savedSearchInvalidName", "Enter a name from 1 to 60 characters.")
                SavedSearchFeedback.LIMIT_REACHED -> text("savedSearchLimit", "You can save up to 16 filters on this device.")
                SavedSearchFeedback.NOT_FOUND -> text("savedSearchNotFound", "That saved filter is no longer available.")
                SavedSearchFeedback.STORAGE_FAILED -> text("localSaveFailed", "Could not save on this device. Try again.")
                SavedSearchFeedback.NEARBY_UNSUPPORTED -> text("nearbyNotSaved", "Nearby location cannot be saved. Choose a city or clear Nearby first.")
            }
            Text(message, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("filter-feedback").semantics { liveRegion = LiveRegionMode.Polite })
        }
        if (modified) Text(text("filterModified", "Changed — update to keep these choices"),
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("filter-modified"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { name = active?.name.orEmpty(); dialog = true },
                modifier = Modifier.heightIn(min = 48.dp).testTag("filter-save")) {
                Text(if (active == null) text("saveSearch", "Save filter")
                    else text("updateSavedSearch", "Update filter"))
            }
            TextButton(onClick = onNew, modifier = Modifier.heightIn(min = 48.dp).testTag("filter-new")) {
                Text(text("newFilter", "New filter"))
            }
        }
    }
    if (dialog) AlertDialog(onDismissRequest = { dialog = false },
        title = { Text(if (active == null) text("saveSearch", "Save filter") else text("updateSavedSearch", "Update filter")) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it.take(60) }, singleLine = true,
                label = { Text(text("name", "Name")) }, modifier = Modifier.testTag("filter-name"))
            Text(text("savedSearchLocal", "Saved on this device. Changes are kept only when you confirm."))
        } },
        confirmButton = { TextButton(onClick = { onSave(name, active != null); dialog = false },
            enabled = name.trim().isNotEmpty(), modifier = Modifier.testTag("filter-save-confirm")) {
                Text(text("save", "Save"))
            } },
        dismissButton = { TextButton(onClick = { dialog = false }) { Text(text("cancel", "Cancel")) } })
}
