package com.rupayonhaldar.gtafreestem.domain.search

import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityTranslation
import com.rupayonhaldar.gtafreestem.domain.validation.OpportunityAvailability
import com.rupayonhaldar.gtafreestem.domain.validation.OpportunityCostEligibility
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
enum class OpportunitySearchSort {
    SOONEST,
    RELEVANCE,
}

/**
 * Distance and new-find selections are retained for forward-compatible persisted state. They are
 * intentionally not applied until Android has a user-selected location and seen-history source.
 */
@Serializable
data class OpportunitySearchFilters(
    val region: String? = null,
    val city: String? = null,
    val category: String? = null,
    val age: Int? = null,
    val adultsOnly: Boolean = false,
    val language: String? = null,
    val volunteerHoursOnly: Boolean = false,
    val coopOnly: Boolean = false,
    val mentorshipOnly: Boolean = false,
    val scholarshipsOnly: Boolean = false,
    val blackFocusedOnly: Boolean = false,
    val girlsFocusedOnly: Boolean = false,
    val indigenousFocusedOnly: Boolean = false,
    val leadershipOnly: Boolean = false,
    val activeOnly: Boolean = true,
    val sort: OpportunitySearchSort = OpportunitySearchSort.SOONEST,
    val distanceKm: Int? = null,
    val includeNewFinds: Boolean = true,
) {
    val hasActiveFilters: Boolean
        get() = normalized() != OpportunitySearchFilters()

    fun normalized(): OpportunitySearchFilters = copy(
        region = region.normalizedSelection(),
        city = city.normalizedSelection(),
        category = category.normalizedSelection(),
        age = age?.takeIf {
            it in 0..OpportunitySearchLimits.MAXIMUM_AGE && !adultsOnly
        },
        language = language.normalizedSelection(),
        distanceKm = distanceKm?.takeIf {
            it in OpportunitySearchLimits.MINIMUM_DISTANCE_KM..
                OpportunitySearchLimits.MAXIMUM_DISTANCE_KM
        },
    )
}

data class OpportunityAgeOption(
    val id: String,
    val label: String,
    val age: Int? = null,
    val adultsOnly: Boolean = false,
)

data class OpportunitySearchOptions(
    val regions: List<String> = emptyList(),
    val cities: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val ages: List<OpportunityAgeOption> = DEFAULT_AGE_OPTIONS,
) {
    companion object {
        val DEFAULT_AGE_OPTIONS = buildList {
            add(OpportunityAgeOption(id = "any", label = "Any"))
            (0..17).forEach { age ->
                add(OpportunityAgeOption(id = age.toString(), label = age.toString(), age = age))
            }
            add(OpportunityAgeOption(id = "18+", label = "18+", adultsOnly = true))
        }
    }
}

object OpportunitySearchLimits {
    const val MAXIMUM_QUERY_LENGTH = 500
    const val MAXIMUM_SELECTION_LENGTH = 256
    const val MAXIMUM_AGE = 120
    const val MINIMUM_DISTANCE_KM = 5
    const val MAXIMUM_DISTANCE_KM = 100
}

object OpportunitySearch {
    fun search(
        opportunities: List<Opportunity>,
        query: String = "",
        filters: OpportunitySearchFilters = OpportunitySearchFilters(),
        now: Instant = Instant.now(),
    ): List<Opportunity> {
        val normalizedFilters = filters.normalized()
        val terms = normalize(query.take(OpportunitySearchLimits.MAXIMUM_QUERY_LENGTH))
            .split(' ')
            .filter(String::isNotBlank)
            .distinct()
        val candidates = opportunities.mapIndexed { index, opportunity ->
            IndexedOpportunity(opportunity = opportunity, originalIndex = index)
        }
        val filtered = candidates.filter { candidate ->
            val opportunity = candidate.opportunity
            OpportunityCostEligibility.isExplicitlyFree(opportunity.cost) &&
                (!normalizedFilters.activeOnly ||
                    OpportunityAvailability.isCurrentlyAvailable(opportunity, now)) &&
                matches(normalizedFilters.region, opportunity.region) &&
                matches(normalizedFilters.city, opportunity.city) &&
                matchesCategory(normalizedFilters.category, opportunity) &&
                matchesAge(normalizedFilters, opportunity) &&
                matchesLanguage(normalizedFilters.language, opportunity) &&
                (!normalizedFilters.volunteerHoursOnly || candidate.matchesAny(VOLUNTEER_TERMS) ||
                    opportunity.volunteerHoursEligible) &&
                (!normalizedFilters.coopOnly || candidate.matchesAny(COOP_TERMS) ||
                    opportunity.coopEligible) &&
                (!normalizedFilters.mentorshipOnly || candidate.matchesAny(MENTORSHIP_TERMS)) &&
                (!normalizedFilters.scholarshipsOnly || candidate.matchesAny(SCHOLARSHIP_TERMS)) &&
                (!normalizedFilters.blackFocusedOnly || candidate.matchesAny(BLACK_FOCUSED_TERMS)) &&
                (!normalizedFilters.girlsFocusedOnly || candidate.matchesAny(GIRLS_FOCUSED_TERMS)) &&
                (!normalizedFilters.indigenousFocusedOnly ||
                    candidate.matchesAny(INDIGENOUS_FOCUSED_TERMS)) &&
                (!normalizedFilters.leadershipOnly || candidate.matchesAny(LEADERSHIP_TERMS)) &&
                terms.all(candidate.searchableText::contains)
        }

        val comparator = when {
            normalizedFilters.sort == OpportunitySearchSort.RELEVANCE && terms.isNotEmpty() ->
                compareByDescending<IndexedOpportunity> { candidate -> candidate.relevance(terms) }
                    .thenBy { candidate -> candidate.dateValue(now) }
                    .then(STABLE_TIE_ORDER)
            else -> compareBy<IndexedOpportunity> { candidate -> candidate.dateValue(now) }
                .then(STABLE_TIE_ORDER)
        }
        return filtered.sortedWith(comparator).map(IndexedOpportunity::opportunity)
    }

    fun options(opportunities: List<Opportunity>): OpportunitySearchOptions =
        OpportunitySearchOptions(
            regions = distinctSorted(opportunities.map(Opportunity::region)),
            cities = distinctSorted(opportunities.map(Opportunity::city)),
            categories = distinctSorted(
                opportunities.flatMap { opportunity ->
                    listOf(opportunity.category) + opportunity.categories
                },
            ),
            languages = distinctSorted(opportunities.flatMap(Opportunity::languages)),
        )

    private fun matches(expected: String?, actual: String): Boolean =
        expected == null || normalize(expected) == normalize(actual)

    private fun matchesCategory(expected: String?, opportunity: Opportunity): Boolean {
        if (expected == null) return true
        val normalizedExpected = normalize(expected)
        return buildList {
            add(opportunity.category)
            addAll(opportunity.categories)
            opportunity.translations.values.mapNotNullTo(this) { it.category }
        }.any { normalize(it) == normalizedExpected }
    }

    private fun matchesAge(filters: OpportunitySearchFilters, opportunity: Opportunity): Boolean {
        if (filters.adultsOnly) {
            return opportunity.ageMax == null || opportunity.ageMax > ADULT_BOUNDARY_AGE
        }
        val age = filters.age ?: return true
        return age >= opportunity.ageMin && (opportunity.ageMax == null || age <= opportunity.ageMax)
    }

    private fun matchesLanguage(expected: String?, opportunity: Opportunity): Boolean {
        if (expected == null) return true
        val normalizedExpected = normalizeLanguageTag(expected)
        return opportunity.languages.any { actual ->
            val normalizedActual = normalizeLanguageTag(actual)
            normalizedActual == normalizedExpected ||
                normalizedActual.substringBefore('-') == normalizedExpected.substringBefore('-')
        }
    }

    private fun distinctSorted(values: List<String>): List<String> {
        val byNormalizedValue = linkedMapOf<String, String>()
        values.forEach { value ->
            val trimmed = value.trim()
            val normalized = normalize(trimmed)
            if (normalized.isNotEmpty()) byNormalizedValue.putIfAbsent(normalized, trimmed)
        }
        return byNormalizedValue.values.sortedWith(
            compareBy<String> { normalize(it) }.thenBy { it.lowercase(Locale.ROOT) },
        )
    }

    private class IndexedOpportunity(
        val opportunity: Opportunity,
        val originalIndex: Int,
    ) {
        val searchableText: String by lazy(LazyThreadSafetyMode.NONE) {
            normalize(searchableFields(opportunity).joinToString(" "))
        }

        private val weightedFields: List<Pair<String, Int>> by lazy(LazyThreadSafetyMode.NONE) {
            weightedFields(opportunity)
        }

        fun matchesAny(terms: List<String>): Boolean = terms.any(searchableText::contains)

        fun relevance(queryTerms: List<String>): Int = weightedFields.sumOf { (field, weight) ->
            val normalizedField = normalize(field)
            queryTerms.count(normalizedField::contains) * weight
        }

        fun dateValue(now: Instant): Instant {
            val dates = listOf(
                opportunity.startDate,
                opportunity.deadline,
                opportunity.endDate,
            ).mapNotNull(::scheduledInstant)
            return dates.filterNot { it.isBefore(now) }.minOrNull()
                ?: dates.maxOrNull()
                ?: Instant.MAX
        }
    }

    private val STABLE_TIE_ORDER =
        compareBy<IndexedOpportunity> { normalize(it.opportunity.title) }
            .thenBy { normalize(it.opportunity.organization) }
            .thenBy { normalize(it.opportunity.id) }
            .thenBy(IndexedOpportunity::originalIndex)

    private val VOLUNTEER_TERMS = normalizedTerms(
        "volunteer hours",
        "community service",
        "student volunteer",
    )
    private val COOP_TERMS = normalizedTerms(
        "co-op",
        "coop",
        "shsm",
        "specialist high skills major",
        "placement",
    )
    private val MENTORSHIP_TERMS = normalizedTerms(
        "mentor",
        "mentorship",
        "career mentor",
        "role model",
    )
    private val SCHOLARSHIP_TERMS = normalizedTerms(
        "scholarship",
        "bursary",
        "grant",
        "award",
        "financial aid",
    )
    private val BLACK_FOCUSED_TERMS = normalizedTerms("black", "african", "caribbean")
    private val GIRLS_FOCUSED_TERMS = normalizedTerms("girl", "girls", "women", "woman", "female")
    private val INDIGENOUS_FOCUSED_TERMS = normalizedTerms(
        "indigenous",
        "first nations",
        "metis",
        "inuit",
    )
    private val LEADERSHIP_TERMS = normalizedTerms("leadership", "leader", "youth council")

    private const val ADULT_BOUNDARY_AGE = 18
    private val GTA_TIME_ZONE = ZoneId.of("America/Toronto")

    private fun normalizedTerms(vararg values: String): List<String> = values.map(::normalize)

    private fun scheduledInstant(raw: String?): Instant? {
        val value = raw?.trim()?.takeIf(String::isNotEmpty) ?: return null
        runCatching { Instant.parse(value) }.getOrNull()?.let { return it }
        runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()?.let { return it }
        val date = runCatching { LocalDate.parse(value) }.getOrNull() ?: return null
        return runCatching { date.atStartOfDay(GTA_TIME_ZONE).toInstant() }.getOrNull()
    }

    private fun searchableFields(opportunity: Opportunity): List<String> = buildList {
        add(opportunity.title)
        add(opportunity.organization)
        add(opportunity.description)
        opportunity.summary?.let(::add)
        add(opportunity.category)
        addAll(opportunity.categories)
        add(opportunity.city)
        add(opportunity.region)
        opportunity.address?.let(::add)
        addAll(opportunity.communityFocus)
        addAll(opportunity.accessibility)
        opportunity.equipment?.let(::add)
        opportunity.food?.let(::add)
        opportunity.capacity?.let(::add)
        opportunity.commitment?.let(::add)
        opportunity.providerContact?.let(::add)
        addAll(opportunity.tags)
        addAll(opportunity.languages)
        opportunity.translations.entries
            .sortedBy { (language, _) -> normalize(language) }
            .forEach { (_, translation) -> addTranslation(translation) }
    }.uniqueNormalizedValues()

    private fun MutableList<String>.addTranslation(translation: OpportunityTranslation) {
        translation.title?.let(::add)
        translation.organization?.let(::add)
        translation.description?.let(::add)
        translation.summary?.let(::add)
        translation.category?.let(::add)
        translation.city?.let(::add)
        translation.region?.let(::add)
        translation.address?.let(::add)
        translation.cost?.let(::add)
        translation.tags?.let(::addAll)
    }

    private fun weightedFields(opportunity: Opportunity): List<Pair<String, Int>> {
        val translations = opportunity.translations.sortedValues()
        return buildList {
            add(opportunity.title to 8)
            translations.mapNotNullTo(this) { it.title?.let { value -> value to 8 } }
            add(opportunity.organization to 5)
            translations.mapNotNullTo(this) { it.organization?.let { value -> value to 5 } }
            add(opportunity.category to 4)
            translations.mapNotNullTo(this) { it.category?.let { value -> value to 4 } }
            add(opportunity.city to 3)
            translations.mapNotNullTo(this) { it.city?.let { value -> value to 3 } }
            opportunity.summary?.let { add(it to 3) }
            translations.mapNotNullTo(this) { it.summary?.let { value -> value to 3 } }
            add(opportunity.description to 2)
            translations.mapNotNullTo(this) { it.description?.let { value -> value to 2 } }
            add(opportunity.region to 1)
            translations.mapNotNullTo(this) { it.region?.let { value -> value to 1 } }
            opportunity.tags.forEach { add(it to 3) }
            translations.forEach { translation ->
                translation.tags.orEmpty().forEach { add(it to 3) }
            }
        }.uniqueWeightedValues()
    }

    private fun Map<String, OpportunityTranslation>.sortedValues(): List<OpportunityTranslation> =
        entries.sortedBy { (language, _) -> normalize(language) }.map { it.value }

    private fun List<String>.uniqueNormalizedValues(): List<String> {
        val seen = mutableSetOf<String>()
        return mapNotNull { value ->
            val normalized = normalize(value)
            value.trim().takeIf { normalized.isNotEmpty() && seen.add(normalized) }
        }
    }

    private fun List<Pair<String, Int>>.uniqueWeightedValues(): List<Pair<String, Int>> {
        val seen = mutableSetOf<String>()
        return mapNotNull { field ->
            val normalized = normalize(field.first)
            field.takeIf { normalized.isNotEmpty() && seen.add(normalized) }
        }
    }
}

private fun String?.normalizedSelection(): String? = this
    ?.trim()
    ?.takeIf { it.isNotEmpty() && !it.equals("all", ignoreCase = true) }

private fun normalizeLanguageTag(value: String): String = normalize(value)
    .replace('_', '-')
    .replace(" ", "")

private fun normalize(value: String): String = Normalizer
    .normalize(value, Normalizer.Form.NFD)
    .replace(COMBINING_MARKS, "")
    .lowercase(Locale.ROOT)
    .trim()
    .replace(WHITESPACE, " ")

private val COMBINING_MARKS = Regex("\\p{M}+")
private val WHITESPACE = Regex("\\s+")
