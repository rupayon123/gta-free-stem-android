package com.rupayonhaldar.gtafreestem.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.rupayonhaldar.gtafreestem.ui.design.GtaObject
import com.rupayonhaldar.gtafreestem.ui.design.artworkForCategory
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rupayonhaldar.gtafreestem.R
import com.rupayonhaldar.gtafreestem.data.local.SavedSearchSummary
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog
import com.rupayonhaldar.gtafreestem.localization.OpportunityLocalization
import com.rupayonhaldar.gtafreestem.ui.browse.BrowseUiState
import com.rupayonhaldar.gtafreestem.ui.browse.SavedSearchFeedback

private data class Topic(val category: String)

private val topics = listOf(
    Topic("Science & Engineering"),
    Topic("Coding & Robotics"),
    Topic("AI & Digital Media"),
    Topic("Makerspace & Fabrication"),
)

@Composable
fun LandingHomeScreen(
    state: BrowseUiState,
    language: AppLanguage,
    catalog: AppStringCatalog,
    text: (String, String) -> String,
    onBrowseAll: () -> Unit,
    onTopic: (String) -> Unit,
    onOpenSavedSearch: (String) -> Unit,
    onDeleteSavedSearch: (String) -> Unit,
) {
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val availableTopics = topics.filter { topic ->
        topic.category in state.filterOptions.categories
    }
    val saved = state.savedSearches.asReversed()

    LazyColumn(
        modifier = Modifier.fillMaxHeight().testTag("landing-home"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.app_logo),
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                    )
                    Text("GTA FREE STEM", style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.semantics { heading() })
                }
                Text(
                    text("homeWelcome", "Free things to learn, make and explore"),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("home-welcome"),
                )
                Text(
                    text("mission", "Free activities across the GTA for every age."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (saved.isNotEmpty()) {
            item { SectionTitle(text("mySavedSearches", "My saved searches")) }
            items(saved, key = { it.entry.id }) { summary ->
                SavedSearchRow(summary, language, catalog, text,
                    onOpen = { onOpenSavedSearch(summary.entry.id) },
                    onDelete = { pendingDeleteId = summary.entry.id })
            }
            item { BrowseAllButton(text, onBrowseAll) }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(text("exploreTopics", "Explore a topic"))
                if (availableTopics.isEmpty()) {
                    Text(
                        text("topicsUnavailable", "Topics will appear when activities finish loading."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else BoxWithConstraints {
                    val stack = maxWidth < 320.dp || LocalDensity.current.fontScale >= 1.8f
                    if (stack) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            availableTopics.forEach { topic ->
                                TopicTile(topic, language, catalog, Modifier.fillMaxWidth(), onTopic)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            availableTopics.chunked(2).forEach { pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    pair.forEach { topic ->
                                        TopicTile(topic, language, catalog, Modifier.weight(1f), onTopic)
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (saved.isEmpty()) item { BrowseAllButton(text, onBrowseAll) }

        if (saved.isEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider()
                    SectionTitle(text("mySavedSearches", "My saved searches"))
                    Text(
                        text("savedSearchEmpty", "Save a search in Browse to keep your interests here and see new matches after a verified update."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (state.savedSearchFeedback == SavedSearchFeedback.DELETED ||
            state.savedSearchFeedback == SavedSearchFeedback.STORAGE_FAILED
        ) {
            item {
                Text(
                    if (state.savedSearchFeedback == SavedSearchFeedback.DELETED)
                        text("savedSearchDeleted", "Saved search deleted.")
                    else text("localSaveFailed", "That change could not be saved on this device."),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    pendingDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(text("deleteSavedSearchTitle", "Delete saved search?")) },
            text = { Text(text("deleteSavedSearchExplanation", "This removes only this saved search from this device.")) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSavedSearch(id)
                    pendingDeleteId = null
                }) { Text(text("delete", "Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text(text("cancel", "Cancel")) }
            },
        )
    }
}

@Composable
private fun BrowseAllButton(text: (String, String) -> String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("home-browse-all"),
    ) { Text(text("browseAll", "Browse all free activities")) }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.semantics { heading() })
}

@Composable
private fun TopicTile(
    topic: Topic,
    language: AppLanguage,
    catalog: AppStringCatalog,
    modifier: Modifier,
    onTopic: (String) -> Unit,
) {
    val label = OpportunityLocalization.categoryName(topic.category, language, catalog)
    Surface(
        onClick = { onTopic(topic.category) },
        modifier = modifier.heightIn(min = 112.dp).testTag("home-topic-${topic.category}"),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GtaObject(artworkForCategory(topic.category), contentDescription = null, modifier = Modifier.size(48.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SavedSearchRow(
    summary: SavedSearchSummary,
    language: AppLanguage,
    catalog: AppStringCatalog,
    text: (String, String) -> String,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val entry = summary.entry
    val criteria = buildList {
        entry.query.takeIf(String::isNotBlank)?.let(::add)
        entry.filters.region?.let(::add)
        entry.filters.city?.let(::add)
        entry.filters.category?.let { add(OpportunityLocalization.categoryName(it, language, catalog)) }
        entry.filters.age?.let { add("${text("ageGuide", "Age guide")} $it") }
        if (entry.filters.adultsOnly) add("18+")
        if (entry.filters.volunteerHoursOnly) add(text("volunteerHours", "Volunteer hours"))
        if (entry.filters.coopOnly) add(text("coop", "Co-op"))
        if (entry.filters.mentorshipOnly) add(text("mentorship", "Mentorship"))
        if (entry.filters.scholarshipsOnly) add(text("categoryScholarships", "Scholarships"))
        if (entry.filters.blackFocusedOnly) add(text("black", "Black-focused"))
        if (entry.filters.girlsFocusedOnly) add(text("girls", "Girls-focused"))
        if (entry.filters.indigenousFocusedOnly) add(text("indigenous", "Indigenous-focused"))
        if (entry.filters.leadershipOnly) add(text("leadership", "Leadership"))
        if (entry.filters.sort == com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchSort.RELEVANCE) {
            add(text("sortBestMatch", "Best match"))
        }
    }.joinToString(" · ").ifBlank { text("all", "All topics") }
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("saved-search-${entry.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f).clickable(onClick = onOpen).heightIn(min = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(entry.name, style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Text(criteria, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
                TextButton(onClick = onDelete, modifier = Modifier.testTag("saved-search-delete-${entry.id}")) {
                    Text(text("delete", "Delete"))
                }
            }
            Text(
                summary.matchCount?.let { "$it ${text("matches", "matches")}" }
                    ?: text("savedSearchChecking", "Checking saved matches"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (summary.newCount > 0 && entry.hasBaseline) {
                Text(
                    "${summary.newCount} ${text("newSinceViewed", "new since you last viewed")}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("saved-search-new-${entry.id}"),
                )
            } else if (!entry.hasBaseline) {
                Text(text("savedSearchPending", "Updates begin after a verified refresh"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
