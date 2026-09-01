package com.rupayonhaldar.gtafreestem.ui.browse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rupayonhaldar.gtafreestem.domain.search.OpportunityAgeOption
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchFilters
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchOptions
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchSort
import com.rupayonhaldar.gtafreestem.theme.GTAFreeStemTheme
import java.util.Locale

/**
 * Complete local browse-filter surface. Give [modifier] a finite height when the panel should own
 * scrolling, such as `Modifier.fillMaxSize()` in a screen or bounded sheet.
 *
 * Distance and New Finds are intentionally omitted until Android has location and seen-history
 * sources. The default scope copy discloses that limitation to users.
 */
@Composable
fun OpportunityFilterPanel(
    filters: OpportunitySearchFilters,
    options: OpportunitySearchOptions,
    onFiltersChange: (OpportunitySearchFilters) -> Unit,
    modifier: Modifier = Modifier,
    labels: OpportunityFilterPanelLabels = OpportunityFilterPanelLabels.English,
    onReset: () -> Unit = { onFiltersChange(OpportunitySearchFilters()) },
) {
    val normalizedFilters = filters.normalized()
    val activeFilterCount = opportunityFilterPanelActiveCount(normalizedFilters)
    val updateFilters: (OpportunitySearchFilters) -> Unit = { updated ->
        onFiltersChange(updated.normalized())
    }

    BoxWithConstraints(
        modifier = modifier
            .testTag(OpportunityFilterPanelTestTags.PANEL)
            .semantics { paneTitle = labels.title },
    ) {
        val horizontalPadding = when {
            maxWidth < 360.dp -> 12.dp
            maxWidth < 600.dp -> 16.dp
            else -> 32.dp
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 740.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            FilterPanelHeader(
                activeFilterCount = activeFilterCount,
                canReset = normalizedFilters != OpportunitySearchFilters(),
                labels = labels,
                onReset = onReset,
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = labels.scopeDescription,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FilterSection(title = labels.programDetailsSection) {
                StringFilterDropdown(
                    label = labels.region,
                    selection = normalizedFilters.region,
                    availableValues = options.regions,
                    anyLabel = labels.any,
                    testTag = OpportunityFilterPanelTestTags.REGION,
                    onSelection = { updateFilters(normalizedFilters.copy(region = it)) },
                )
                StringFilterDropdown(
                    label = labels.city,
                    selection = normalizedFilters.city,
                    availableValues = options.cities,
                    anyLabel = labels.any,
                    testTag = OpportunityFilterPanelTestTags.CITY,
                    onSelection = { updateFilters(normalizedFilters.copy(city = it)) },
                )
                StringFilterDropdown(
                    label = labels.category,
                    selection = normalizedFilters.category,
                    availableValues = options.categories,
                    anyLabel = labels.any,
                    testTag = OpportunityFilterPanelTestTags.CATEGORY,
                    onSelection = { updateFilters(normalizedFilters.copy(category = it)) },
                )
                AgeFilterDropdown(
                    filters = normalizedFilters,
                    availableOptions = options.ages,
                    label = labels.age,
                    anyLabel = labels.any,
                    onSelection = { selection ->
                        updateFilters(
                            normalizedFilters.copy(
                                age = selection.age,
                                adultsOnly = selection.adultsOnly,
                            ),
                        )
                    },
                )
                StringFilterDropdown(
                    label = labels.language,
                    selection = normalizedFilters.language,
                    availableValues = options.languages,
                    anyLabel = labels.any,
                    testTag = OpportunityFilterPanelTestTags.LANGUAGE,
                    onSelection = { updateFilters(normalizedFilters.copy(language = it)) },
                )
            }

            HorizontalDivider()

            FilterSection(title = labels.pathwaysSection) {
                FilterToggleFlow(
                    toggles = listOf(
                        PanelToggle(
                            label = labels.volunteerHours,
                            selected = normalizedFilters.volunteerHoursOnly,
                            testTag = OpportunityFilterPanelTestTags.VOLUNTEER,
                            onToggle = {
                                updateFilters(
                                    normalizedFilters.copy(volunteerHoursOnly = it),
                                )
                            },
                        ),
                        PanelToggle(
                            label = labels.coop,
                            selected = normalizedFilters.coopOnly,
                            testTag = OpportunityFilterPanelTestTags.COOP,
                            onToggle = {
                                updateFilters(normalizedFilters.copy(coopOnly = it))
                            },
                        ),
                        PanelToggle(
                            label = labels.mentorship,
                            selected = normalizedFilters.mentorshipOnly,
                            testTag = OpportunityFilterPanelTestTags.MENTORSHIP,
                            onToggle = {
                                updateFilters(normalizedFilters.copy(mentorshipOnly = it))
                            },
                        ),
                        PanelToggle(
                            label = labels.scholarships,
                            selected = normalizedFilters.scholarshipsOnly,
                            testTag = OpportunityFilterPanelTestTags.SCHOLARSHIPS,
                            onToggle = {
                                updateFilters(normalizedFilters.copy(scholarshipsOnly = it))
                            },
                        ),
                    ),
                    labels = labels,
                )
            }

            FilterSection(title = labels.communityFocusSection) {
                FilterToggleFlow(
                    toggles = listOf(
                        PanelToggle(
                            label = labels.blackFocused,
                            selected = normalizedFilters.blackFocusedOnly,
                            testTag = OpportunityFilterPanelTestTags.BLACK_FOCUSED,
                            onToggle = {
                                updateFilters(normalizedFilters.copy(blackFocusedOnly = it))
                            },
                        ),
                        PanelToggle(
                            label = labels.girlsFocused,
                            selected = normalizedFilters.girlsFocusedOnly,
                            testTag = OpportunityFilterPanelTestTags.GIRLS_FOCUSED,
                            onToggle = {
                                updateFilters(normalizedFilters.copy(girlsFocusedOnly = it))
                            },
                        ),
                        PanelToggle(
                            label = labels.indigenousFocused,
                            selected = normalizedFilters.indigenousFocusedOnly,
                            testTag = OpportunityFilterPanelTestTags.INDIGENOUS_FOCUSED,
                            onToggle = {
                                updateFilters(normalizedFilters.copy(indigenousFocusedOnly = it))
                            },
                        ),
                        PanelToggle(
                            label = labels.leadership,
                            selected = normalizedFilters.leadershipOnly,
                            testTag = OpportunityFilterPanelTestTags.LEADERSHIP,
                            onToggle = {
                                updateFilters(normalizedFilters.copy(leadershipOnly = it))
                            },
                        ),
                    ),
                    labels = labels,
                )
            }

            HorizontalDivider()

            FilterSection(title = labels.sortSection) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SortChoice(
                        label = labels.soonest,
                        selected = normalizedFilters.sort == OpportunitySearchSort.SOONEST,
                        testTag = OpportunityFilterPanelTestTags.SORT_SOONEST,
                        onClick = {
                            updateFilters(normalizedFilters.copy(sort = OpportunitySearchSort.SOONEST))
                        },
                    )
                    SortChoice(
                        label = labels.relevance,
                        selected = normalizedFilters.sort == OpportunitySearchSort.RELEVANCE,
                        testTag = OpportunityFilterPanelTestTags.SORT_RELEVANCE,
                        onClick = {
                            updateFilters(
                                normalizedFilters.copy(sort = OpportunitySearchSort.RELEVANCE),
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Number of selections represented by controls visible in [OpportunityFilterPanel]. */
fun opportunityFilterPanelActiveCount(filters: OpportunitySearchFilters): Int {
    val normalized = filters.normalized()
    return listOf(
        normalized.region != null,
        normalized.city != null,
        normalized.category != null,
        normalized.age != null || normalized.adultsOnly,
        normalized.language != null,
        normalized.volunteerHoursOnly,
        normalized.coopOnly,
        normalized.mentorshipOnly,
        normalized.scholarshipsOnly,
        normalized.blackFocusedOnly,
        normalized.girlsFocusedOnly,
        normalized.indigenousFocusedOnly,
        normalized.leadershipOnly,
        normalized.sort != OpportunitySearchSort.SOONEST,
    ).count { it }
}

@Composable
private fun FilterPanelHeader(
    activeFilterCount: Int,
    canReset: Boolean,
    labels: OpportunityFilterPanelLabels,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = labels.title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = labels.activeFilterCount(activeFilterCount),
                modifier = Modifier
                    .testTag(OpportunityFilterPanelTestTags.ACTIVE_COUNT)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        TextButton(
            onClick = onReset,
            enabled = canReset,
            modifier = Modifier
                .heightIn(min = MinimumTouchTarget)
                .testTag(OpportunityFilterPanelTestTags.RESET),
            shape = MaterialTheme.shapes.small,
        ) {
            Text(labels.reset)
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

@Composable
private fun StringFilterDropdown(
    label: String,
    selection: String?,
    availableValues: List<String>,
    anyLabel: String,
    testTag: String,
    onSelection: (String?) -> Unit,
) {
    val choices = remember(selection, availableValues, anyLabel) {
        stringPanelChoices(selection, availableValues, anyLabel)
    }
    FilterDropdown(
        label = label,
        selectedId = selection?.let(::stringChoiceId) ?: AnyChoiceId,
        choices = choices,
        testTag = testTag,
        onSelection = onSelection,
    )
}

@Composable
private fun AgeFilterDropdown(
    filters: OpportunitySearchFilters,
    availableOptions: List<OpportunityAgeOption>,
    label: String,
    anyLabel: String,
    onSelection: (AgeSelection) -> Unit,
) {
    val ageOptions = availableOptions.ifEmpty { OpportunitySearchOptions.DEFAULT_AGE_OPTIONS }
    val selection = when {
        filters.adultsOnly -> AgeSelection(age = null, adultsOnly = true)
        filters.age != null -> AgeSelection(age = filters.age, adultsOnly = false)
        else -> AgeSelection(age = null, adultsOnly = false)
    }
    val choices = remember(selection, ageOptions, anyLabel) {
        agePanelChoices(selection, ageOptions, anyLabel)
    }
    FilterDropdown(
        label = label,
        selectedId = selection.id,
        choices = choices,
        testTag = OpportunityFilterPanelTestTags.AGE,
        onSelection = onSelection,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> FilterDropdown(
    label: String,
    selectedId: String,
    choices: List<PanelChoice<T>>,
    testTag: String,
    onSelection: (T) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = choices.firstOrNull { it.id == selectedId }?.label
        ?: choices.first().label

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .testTag(testTag)
                .semantics { stateDescription = selectedLabel },
            readOnly = true,
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            choices.forEach { choice ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(choice.label) },
                    onClick = {
                        expanded = false
                        onSelection(choice.value)
                    },
                    modifier = Modifier.heightIn(min = MinimumTouchTarget),
                )
            }
        }
    }
}

@Composable
private fun FilterToggleFlow(
    toggles: List<PanelToggle>,
    labels: OpportunityFilterPanelLabels,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        toggles.forEach { toggle ->
            FilterChip(
                selected = toggle.selected,
                onClick = { toggle.onToggle(!toggle.selected) },
                label = { Text(toggle.label) },
                shape = MaterialTheme.shapes.small,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier
                    .heightIn(min = MinimumTouchTarget)
                    .testTag(toggle.testTag)
                    .semantics {
                        stateDescription = if (toggle.selected) {
                            labels.selectedState
                        } else {
                            labels.notSelectedState
                        }
                    },
            )
        }
    }
}

@Composable
private fun SortChoice(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            1.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinimumTouchTarget),
    ) {
        Row(
            modifier = Modifier
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    role = Role.RadioButton,
                )
                .padding(horizontal = 12.dp)
                .testTag(testTag),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RadioButton(selected = selected, onClick = null)
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

internal fun stringPanelChoices(
    current: String?,
    available: List<String>,
    anyLabel: String,
): List<PanelChoice<String?>> {
    val candidates = buildList {
        current?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
        addAll(available)
    }
    val seen = mutableSetOf<String>()
    return buildList {
        add(PanelChoice(id = AnyChoiceId, label = anyLabel, value = null))
        candidates.forEach { value ->
            val trimmed = value.trim()
            val key = trimmed.lowercase(Locale.ROOT)
            if (trimmed.isNotEmpty() && seen.add(key)) {
                add(PanelChoice(id = stringChoiceId(trimmed), label = trimmed, value = trimmed))
            }
        }
    }
}

internal fun agePanelChoices(
    current: AgeSelection,
    available: List<OpportunityAgeOption>,
    anyLabel: String,
): List<PanelChoice<AgeSelection>> {
    val choices = available.map { option ->
        val selection = AgeSelection(age = option.age, adultsOnly = option.adultsOnly)
        PanelChoice(
            id = selection.id,
            label = if (selection.id == AnyChoiceId) anyLabel else option.label,
            value = selection,
        )
    }.toMutableList()
    if (choices.none { it.id == AnyChoiceId }) {
        choices.add(
            index = 0,
            element = PanelChoice(
                id = AnyChoiceId,
                label = anyLabel,
                value = AgeSelection(age = null, adultsOnly = false),
            ),
        )
    }
    if (choices.none { it.id == current.id }) {
        choices.add(
            PanelChoice(
                id = current.id,
                label = current.age?.toString() ?: "18+",
                value = current,
            ),
        )
    }
    return choices.distinctBy(PanelChoice<AgeSelection>::id)
}

internal data class PanelChoice<T>(
    val id: String,
    val label: String,
    val value: T,
)

internal data class AgeSelection(
    val age: Int?,
    val adultsOnly: Boolean,
) {
    val id: String
        get() = when {
            adultsOnly -> AdultChoiceId
            age != null -> age.toString()
            else -> AnyChoiceId
        }
}

private data class PanelToggle(
    val label: String,
    val selected: Boolean,
    val testTag: String,
    val onToggle: (Boolean) -> Unit,
)

internal object OpportunityFilterPanelTestTags {
    const val PANEL = "opportunity-filter-panel"
    const val ACTIVE_COUNT = "opportunity-filter-active-count"
    const val RESET = "opportunity-filter-reset"
    const val REGION = "opportunity-filter-region"
    const val CITY = "opportunity-filter-city"
    const val CATEGORY = "opportunity-filter-category"
    const val AGE = "opportunity-filter-age"
    const val LANGUAGE = "opportunity-filter-language"
    const val VOLUNTEER = "opportunity-filter-volunteer"
    const val COOP = "opportunity-filter-coop"
    const val MENTORSHIP = "opportunity-filter-mentorship"
    const val SCHOLARSHIPS = "opportunity-filter-scholarships"
    const val BLACK_FOCUSED = "opportunity-filter-black-focused"
    const val GIRLS_FOCUSED = "opportunity-filter-girls-focused"
    const val INDIGENOUS_FOCUSED = "opportunity-filter-indigenous-focused"
    const val LEADERSHIP = "opportunity-filter-leadership"
    const val SORT_SOONEST = "opportunity-filter-sort-soonest"
    const val SORT_RELEVANCE = "opportunity-filter-sort-relevance"
}

private const val AnyChoiceId = "any"
private const val AdultChoiceId = "18+"
private val MinimumTouchTarget = 48.dp

private fun stringChoiceId(value: String): String =
    "value:${value.trim().lowercase(Locale.ROOT)}"

@Preview(name = "Narrow phone", widthDp = 320, heightDp = 720, fontScale = 1.3f)
@Composable
private fun OpportunityFilterPanelNarrowPreview() {
    GTAFreeStemTheme {
        OpportunityFilterPanel(
            filters = OpportunitySearchFilters(
                category = "Coding & Robotics",
                volunteerHoursOnly = true,
            ),
            options = PreviewOptions,
            onFiltersChange = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(name = "Wide dark", widthDp = 720, heightDp = 600, uiMode = 0x20)
@Composable
private fun OpportunityFilterPanelWideDarkPreview() {
    GTAFreeStemTheme(darkTheme = true) {
        OpportunityFilterPanel(
            filters = OpportunitySearchFilters(adultsOnly = true),
            options = PreviewOptions,
            onFiltersChange = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private val PreviewOptions = OpportunitySearchOptions(
    regions = listOf("Peel", "Toronto", "York"),
    cities = listOf("Brampton", "Mississauga", "Toronto", "Vaughan"),
    categories = listOf("Coding & Robotics", "Science & Engineering"),
    languages = listOf("en", "fr", "zh-Hans"),
)
