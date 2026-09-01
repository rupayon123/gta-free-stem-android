package com.rupayonhaldar.gtafreestem.ui.saved

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rupayonhaldar.gtafreestem.data.local.SavedOpportunityEntry
import com.rupayonhaldar.gtafreestem.data.local.SavedOpportunitySections
import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog

private val SavedLibraryMaxWidth = 840.dp
private val SavedLibraryPadding = 16.dp
private val SavedLibraryTopPadding = 12.dp
private val SavedLibrarySpacing = 14.dp
private val SavedLibraryElevation = 1.5.dp

/**
 * Displays the exact full opportunity snapshots held by the caller's local saved store.
 * This composable performs no persistence, network request, sign-in, or cloud synchronization.
 */
@Composable
fun SavedOpportunityLibraryScreen(
    sections: SavedOpportunitySections,
    unresolvedLegacyCount: Int,
    language: AppLanguage,
    catalog: AppStringCatalog,
    onBack: () -> Unit,
    onOpenDetail: (Opportunity) -> Unit,
    onRemoveSaved: (Opportunity) -> Boolean,
    onClearAllSaved: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val labels = remember(language, catalog) {
        SavedOpportunityLibraryLabels(language, catalog)
    }
    val safeUnresolvedCount = unresolvedLegacyCount.coerceAtLeast(0)
    val visibleCount = sections.current.size + sections.archive.size
    val totalCount = visibleCount + safeUnresolvedCount
    var clearConfirmationVisible by rememberSaveable { mutableStateOf(false) }
    var statusMessage by rememberSaveable(language) { mutableStateOf<String?>(null) }
    val layoutDirection = if (language.isRightToLeft) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            BoxWithConstraints(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.TopCenter,
            ) {
                val listWidth = maxWidth.coerceAtMost(SavedLibraryMaxWidth)
                val horizontalPadding = if (maxWidth >= 600.dp) 24.dp else SavedLibraryPadding

            LazyColumn(
                modifier = Modifier
                    .width(listWidth)
                    .fillMaxHeight()
                    .testTag(SavedOpportunityLibraryTestTags.SCREEN),
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    top = SavedLibraryTopPadding,
                    end = horizontalPadding,
                    bottom = 40.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(SavedLibrarySpacing),
            ) {
                item(key = "header", contentType = "header") {
                    LibraryHeader(
                        labels = labels,
                        count = totalCount,
                        showClearAll = totalCount > 0,
                        onBack = onBack,
                        onClearAll = { clearConfirmationVisible = true },
                    )
                }

                statusMessage?.let { message ->
                    item(key = "status", contentType = "status") {
                        StatusMessage(message)
                    }
                }

                if (safeUnresolvedCount > 0) {
                    item(key = "legacy", contentType = "notice") {
                        LegacySavedNotice(
                            labels = labels,
                            count = safeUnresolvedCount,
                        )
                    }
                }

                if (visibleCount == 0 && safeUnresolvedCount == 0) {
                    item(key = "empty", contentType = "empty") {
                        EmptySavedLibrary(labels)
                    }
                }

                if (visibleCount == 0 && safeUnresolvedCount > 0) {
                    item(key = "awaiting-details", contentType = "empty") {
                        EmptySavedDetails(labels)
                    }
                }

                if (sections.current.isNotEmpty()) {
                    item(key = "current-header", contentType = "section-header") {
                        SavedSectionHeader(
                            title = labels.current,
                            count = sections.current.size,
                            explanation = labels.currentExplanation,
                        )
                    }
                    items(
                        items = sections.current,
                        key = { entry -> "current-${entry.opportunity.id}" },
                        contentType = { "current-opportunity" },
                    ) { entry ->
                        SavedOpportunityCard(
                            entry = entry,
                            archived = false,
                            language = language,
                            catalog = catalog,
                            labels = labels,
                            onOpenDetail = { onOpenDetail(entry.opportunity) },
                            onRemove = {
                                val removed = runCatching {
                                    onRemoveSaved(entry.opportunity)
                                }.getOrDefault(false)
                                statusMessage = if (removed) {
                                    labels.removedMessage(
                                        savedOpportunityCardText(
                                            entry,
                                            language,
                                            catalog,
                                            labels,
                                        ).title,
                                    )
                                } else {
                                    labels.saveFailed
                                }
                            },
                        )
                    }
                }

                if (sections.archive.isNotEmpty()) {
                    item(key = "archive-header", contentType = "section-header") {
                        SavedSectionHeader(
                            title = labels.archive,
                            count = sections.archive.size,
                            explanation = labels.archiveExplanation,
                        )
                    }
                    items(
                        items = sections.archive,
                        key = { entry -> "archive-${entry.opportunity.id}" },
                        contentType = { "archive-opportunity" },
                    ) { entry ->
                        SavedOpportunityCard(
                            entry = entry,
                            archived = true,
                            language = language,
                            catalog = catalog,
                            labels = labels,
                            onOpenDetail = { onOpenDetail(entry.opportunity) },
                            onRemove = {
                                val removed = runCatching {
                                    onRemoveSaved(entry.opportunity)
                                }.getOrDefault(false)
                                statusMessage = if (removed) {
                                    labels.removedMessage(
                                        savedOpportunityCardText(
                                            entry,
                                            language,
                                            catalog,
                                            labels,
                                        ).title,
                                    )
                                } else {
                                    labels.saveFailed
                                }
                            },
                        )
                    }
                }
            }
        }

        if (clearConfirmationVisible) {
            AlertDialog(
                onDismissRequest = { clearConfirmationVisible = false },
                title = {
                    Text(
                        text = labels.clearAllTitle,
                        modifier = Modifier.semantics { heading() },
                    )
                },
                text = { Text(labels.clearAllExplanation) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            clearConfirmationVisible = false
                            statusMessage = if (
                                runCatching(onClearAllSaved).getOrDefault(false)
                            ) {
                                labels.libraryCleared
                            } else {
                                labels.saveFailed
                            }
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag(SavedOpportunityLibraryTestTags.CONFIRM_CLEAR_ALL),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text(labels.clearAll)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { clearConfirmationVisible = false },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(labels.cancel)
                    }
                },
                modifier = Modifier.testTag(SavedOpportunityLibraryTestTags.CLEAR_ALL_DIALOG),
            )
        }
    }
}

@Composable
private fun LibraryHeader(
    labels: SavedOpportunityLibraryLabels,
    count: Int,
    showClearAll: Boolean,
    onBack: () -> Unit,
    onClearAll: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = SavedLibraryElevation),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(SavedOpportunityLibraryTestTags.BACK),
            ) {
                Text(labels.back)
            }
            Text(
                text = labels.title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = labels.localOnlyExplanation,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = labels.itemCount(count),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (showClearAll) {
                OutlinedButton(
                    onClick = onClearAll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag(SavedOpportunityLibraryTestTags.CLEAR_ALL),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(labels.clearAll, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag(SavedOpportunityLibraryTestTags.STATUS),
        shape = MaterialTheme.shapes.small,
        elevation = CardDefaults.cardElevation(defaultElevation = SavedLibraryElevation),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun LegacySavedNotice(
    labels: SavedOpportunityLibraryLabels,
    count: Int,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SavedOpportunityLibraryTestTags.LEGACY_NOTICE),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = SavedLibraryElevation),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = labels.unresolvedTitle,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = labels.unresolvedExplanation(count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun EmptySavedLibrary(labels: SavedOpportunityLibraryLabels) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SavedOpportunityLibraryTestTags.EMPTY),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = SavedLibraryElevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = labels.emptyTitle,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = labels.emptyExplanation,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EmptySavedDetails(labels: SavedOpportunityLibraryLabels) {
    Text(
        text = labels.emptyTitle,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SavedSectionHeader(
    title: String,
    count: Int,
    explanation: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "$title ($count)",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = explanation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SavedOpportunityCard(
    entry: SavedOpportunityEntry,
    archived: Boolean,
    language: AppLanguage,
    catalog: AppStringCatalog,
    labels: SavedOpportunityLibraryLabels,
    onOpenDetail: () -> Unit,
    onRemove: () -> Unit,
) {
    val opportunity = entry.opportunity
    val text = remember(entry, language, catalog) {
        savedOpportunityCardText(entry, language, catalog, labels)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SavedOpportunityLibraryTestTags.card(opportunity.id)),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (archived) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        border = BorderStroke(
            1.dp,
            if (archived) MaterialTheme.colorScheme.outline.copy(alpha = 0.75f) else MaterialTheme.colorScheme.outlineVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = SavedLibraryElevation),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (archived) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = archiveStatusExplanation(opportunity, labels),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Text(
                text = text.category,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = text.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = text.organization,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = text.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            text.facts.forEach { fact ->
                SavedOpportunityFactRow(fact)
            }
            Text(
                text = text.savedOn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onOpenDetail,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = labels.openDetailsAccessibility(text.title)
                        }
                        .testTag(SavedOpportunityLibraryTestTags.details(opportunity.id)),
                ) {
                    Text(labels.details, textAlign = TextAlign.Center)
                }
                OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = labels.removeAccessibility(text.title)
                        }
                        .testTag(SavedOpportunityLibraryTestTags.remove(opportunity.id)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                ) {
                    Text(labels.remove, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun SavedOpportunityFactRow(fact: SavedOpportunityFact) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = fact.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = fact.value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

internal object SavedOpportunityLibraryTestTags {
    const val SCREEN = "saved-library-screen"
    const val BACK = "saved-library-back"
    const val STATUS = "saved-library-status"
    const val EMPTY = "saved-library-empty"
    const val LEGACY_NOTICE = "saved-library-legacy-notice"
    const val CLEAR_ALL = "saved-library-clear-all"
    const val CLEAR_ALL_DIALOG = "saved-library-clear-all-dialog"
    const val CONFIRM_CLEAR_ALL = "saved-library-confirm-clear-all"

    fun card(id: String) = "saved-library-card-$id"
    fun details(id: String) = "saved-library-details-$id"
    fun remove(id: String) = "saved-library-remove-$id"
}
