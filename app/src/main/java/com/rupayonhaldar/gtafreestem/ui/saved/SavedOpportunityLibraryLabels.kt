package com.rupayonhaldar.gtafreestem.ui.saved

import com.rupayonhaldar.gtafreestem.data.local.SavedOpportunityEntry
import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog
import com.rupayonhaldar.gtafreestem.localization.OpportunityLocalization
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Catalog-backed copy with plain-English fallback for saved-library-specific language. */
internal class SavedOpportunityLibraryLabels(
    private val language: AppLanguage,
    private val catalog: AppStringCatalog,
) {
    val title = text("saved", "Saved opportunities")
    val back = text("back", "Back")
    val current = text("current", "Current")
    val archive = text("archive", "Archive")
    val currentExplanation = text(
        "savedCurrentExplanation",
        "Still active in the latest information available on this device.",
    )
    val archiveExplanation = text(
        "savedArchiveNote",
        "Expired saved listings stay available in your archive so you can remember what you found.",
    )
    val localOnlyExplanation = text(
        "savedLocalOnlyExplanation",
        "Saved opportunities and their full details stay on this device for offline access. Nothing here is synced to a cloud account.",
    )
    val emptyTitle = text("savedEmpty", "Saved opportunities will appear here.")
    val emptyExplanation = text(
        "savedEmptyExplanation",
        "Save an opportunity while browsing and its available details will be kept here for offline access.",
    )
    val unresolvedTitle = text("savedLegacyTitle", "Older saved items")
    val details = text("details", "Details")
    val remove = text("removeSaved", "Remove from saved")
    val clearAll = text("clearAllSaved", "Clear saved library")
    val clearAllTitle = text("clearAllSavedTitle", "Clear all saved opportunities?")
    val clearAllExplanation = text(
        "clearAllSavedExplanation",
        "This removes current, archived, and older unresolved saves from this device. This cannot be undone.",
    )
    val cancel = text("cancel", "Cancel")
    val saveFailed = text(
        "savedChangeFailed",
        "The saved library could not be changed on this device. Try again.",
    )
    val libraryCleared = text("savedLibraryCleared", "Saved library cleared from this device.")
    val location = text("location", "Location")
    val ages = text("ages", "Ages")
    val starts = text("date", "Starts")
    val ends = text("endDate", "Ends")
    val deadline = text("deadline", "Deadline")
    val cost = text("cost", "Cost")
    val languages = text("languages", "Languages")
    val virtual = text("virtual", "Virtual")
    val savedOnLabel = text("savedOn", "Saved")

    fun itemCount(count: Int): String {
        val safeCount = count.coerceAtLeast(0)
        val fallback = if (safeCount == 1) {
            "1 saved opportunity"
        } else {
            "$safeCount saved opportunities"
        }
        return text("savedCount", fallback, mapOf("count" to safeCount.toString()))
    }

    fun unresolvedExplanation(count: Int): String {
        val safeCount = count.coerceAtLeast(0)
        val fallback = if (safeCount == 1) {
            "1 item saved by an older app version is waiting for verified details. It remains saved on this device and will appear after a matching listing is loaded."
        } else {
            "$safeCount items saved by an older app version are waiting for verified details. They remain saved on this device and will appear after matching listings are loaded."
        }
        return text("savedLegacyPending", fallback, mapOf("count" to safeCount.toString()))
    }

    fun removedFromFeedExplanation(): String = text(
        "savedRemovedFromFeed",
        "This listing is no longer in the current verified feed. These are the full details saved on this device for offline reference.",
    )

    fun expiredOrInactiveExplanation(): String = text(
        "savedExpiredOrInactive",
        "This listing is archived because its deadline or end date passed, or its status is no longer active. These are the full details saved on this device for offline reference.",
    )

    fun openDetailsAccessibility(title: String): String = text(
        "openSavedDetailsAccessibility",
        "Open saved details for $title",
        mapOf("title" to title),
    )

    fun removeAccessibility(title: String): String = text(
        "removeSavedAccessibility",
        "Remove $title from saved opportunities",
        mapOf("title" to title),
    )

    fun removedMessage(title: String): String = text(
        "removedSavedMessage",
        "$title was removed from saved opportunities.",
        mapOf("title" to title),
    )

    fun savedOn(entry: SavedOpportunityEntry): String {
        val locale = Locale.forLanguageTag(language.localeTag)
        val formatted = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale)
            .format(entry.savedAt.atZone(ZoneId.systemDefault()).toLocalDate())
        return "$savedOnLabel $formatted"
    }

    fun opportunityDate(raw: String): String {
        val locale = Locale.forLanguageTag(language.localeTag)
        val date = runCatching { OffsetDateTime.parse(raw).toLocalDate() }.getOrNull()
            ?: runCatching {
                Instant.parse(raw).atZone(ZoneId.systemDefault()).toLocalDate()
            }.getOrNull()
            ?: runCatching { LocalDate.parse(raw) }.getOrNull()
            ?: return raw
        return DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale)
            .format(date)
    }

    fun languageName(rawTag: String): String {
        val knownLanguage = AppLanguage.matching(rawTag)
        return knownLanguage?.let(catalog::metadata)?.nativeName
            ?: rawTag.trim().ifEmpty { rawTag }
    }

    private fun text(
        key: String,
        englishFallback: String,
        placeholders: Map<String, String> = emptyMap(),
    ): String = catalog.text(key, language, placeholders)
        .trim()
        .takeUnless { value -> value.isEmpty() || value == key }
        ?: englishFallback
}

internal data class SavedOpportunityCardText(
    val title: String,
    val organization: String,
    val summary: String,
    val category: String,
    val facts: List<SavedOpportunityFact>,
    val savedOn: String,
)

internal data class SavedOpportunityFact(
    val label: String,
    val value: String,
)

internal fun savedOpportunityCardText(
    entry: SavedOpportunityEntry,
    language: AppLanguage,
    catalog: AppStringCatalog,
    labels: SavedOpportunityLibraryLabels = SavedOpportunityLibraryLabels(language, catalog),
): SavedOpportunityCardText {
    val opportunity = entry.opportunity
    val localized = OpportunityLocalization.resolve(opportunity, language, catalog)
    val facts = buildList {
        val location = when {
            opportunity.virtual && localized.city.isBlank() && localized.region.isBlank() -> labels.virtual
            else -> listOf(localized.city, localized.region)
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()
                .joinToString(", ")
                .ifBlank { if (opportunity.virtual) labels.virtual else "" }
        }
        if (location.isNotBlank()) add(SavedOpportunityFact(labels.location, location))
        add(SavedOpportunityFact(labels.ages, ageRange(opportunity)))
        opportunity.startDate?.nonBlank()?.let { date ->
            add(SavedOpportunityFact(labels.starts, labels.opportunityDate(date)))
        }
        opportunity.endDate?.nonBlank()?.let { date ->
            add(SavedOpportunityFact(labels.ends, labels.opportunityDate(date)))
        }
        opportunity.deadline?.nonBlank()?.let { date ->
            add(SavedOpportunityFact(labels.deadline, labels.opportunityDate(date)))
        }
        localized.cost.nonBlank()?.let { cost ->
            add(SavedOpportunityFact(labels.cost, cost))
        }
        opportunity.languages
            .mapNotNull(String::nonBlank)
            .takeIf(List<String>::isNotEmpty)
            ?.let { languageTags ->
                add(
                    SavedOpportunityFact(
                        labels.languages,
                        languageTags.joinToString(", ", transform = labels::languageName),
                    ),
                )
            }
    }
    return SavedOpportunityCardText(
        title = localized.title,
        organization = localized.organization,
        summary = localized.summary,
        category = localized.category,
        facts = facts,
        savedOn = labels.savedOn(entry),
    )
}

internal fun archiveStatusExplanation(
    opportunity: Opportunity,
    labels: SavedOpportunityLibraryLabels,
): String = if (opportunity.status.trim().equals("removed", ignoreCase = true)) {
    labels.removedFromFeedExplanation()
} else {
    labels.expiredOrInactiveExplanation()
}

private fun ageRange(opportunity: Opportunity): String = when {
    opportunity.ageMax == null -> "${opportunity.ageMin}+"
    opportunity.ageMax == opportunity.ageMin -> opportunity.ageMin.toString()
    else -> "${opportunity.ageMin}–${opportunity.ageMax}"
}

private fun String.nonBlank(): String? = trim().takeIf(String::isNotEmpty)
