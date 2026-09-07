package com.rupayonhaldar.gtafreestem.domain.search

import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.domain.validation.OpportunityAvailability
import com.rupayonhaldar.gtafreestem.domain.validation.OpportunityCostEligibility
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog
import com.rupayonhaldar.gtafreestem.localization.LocalizedOpportunitySearchIndex
import com.rupayonhaldar.gtafreestem.localization.OpportunityLocalization
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.serialization.Serializable

@Serializable
enum class OpportunitySearchSort {
    SOONEST,
    NEAREST,
    RELEVANCE,
}

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
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distanceKm: Int? = null,
    val includeNewFinds: Boolean = true,
) {
    val hasValidLocation: Boolean
        get() = validCoordinate(latitude, longitude) != null

    val hasActiveFilters: Boolean
        get() = normalized() != OpportunitySearchFilters()

    fun normalized(): OpportunitySearchFilters {
        val coordinate = validCoordinate(latitude, longitude)
        return copy(
            region = region.normalizedSelection(),
            city = city.normalizedSelection(),
            category = category.normalizedSelection(),
            age = age?.takeIf {
                it in 0..OpportunitySearchLimits.MAXIMUM_AGE && !adultsOnly
            },
            language = language.normalizedSelection(),
            latitude = coordinate?.latitude,
            longitude = coordinate?.longitude,
            distanceKm = distanceKm?.takeIf {
                it in OpportunitySearchLimits.MINIMUM_DISTANCE_KM..
                    OpportunitySearchLimits.MAXIMUM_DISTANCE_KM
            },
        )
    }
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
        language: AppLanguage = AppLanguage.ENGLISH,
        catalog: AppStringCatalog? = null,
    ): List<Opportunity> {
        val normalizedFilters = filters.normalized()
        val origin = validCoordinate(
            normalizedFilters.latitude,
            normalizedFilters.longitude,
        )
        val terms = normalize(query.take(OpportunitySearchLimits.MAXIMUM_QUERY_LENGTH))
            .split(' ')
            .filter(String::isNotBlank)
            .distinct()
        val candidates = opportunities.mapIndexed { index, opportunity ->
            IndexedOpportunity(
                opportunity = opportunity,
                originalIndex = index,
                language = language,
                catalog = catalog,
                origin = origin,
            )
        }
        val filtered = candidates.filter { candidate ->
            val opportunity = candidate.opportunity
            OpportunityCostEligibility.isExplicitlyFree(opportunity.cost) &&
                (!normalizedFilters.activeOnly ||
                    OpportunityAvailability.isCurrentlyAvailable(opportunity, now)) &&
                (normalizedFilters.includeNewFinds || opportunity.isNewFind != true) &&
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
                (origin == null || normalizedFilters.distanceKm == null ||
                    candidate.distanceKm?.let { distance ->
                        distance <= normalizedFilters.distanceKm.toDouble()
                    } == true) &&
                terms.all(candidate.searchableText::contains)
        }

        val comparator = when {
            normalizedFilters.sort == OpportunitySearchSort.NEAREST && origin != null ->
                compareBy<IndexedOpportunity> { candidate ->
                    candidate.distanceKm ?: Double.POSITIVE_INFINITY
                }.thenBy { candidate -> candidate.dateValue(now) }
                    .then(STABLE_TIE_ORDER)
            normalizedFilters.sort == OpportunitySearchSort.RELEVANCE && terms.isNotEmpty() ->
                compareByDescending<IndexedOpportunity> { candidate -> candidate.relevance(terms) }
                    .thenBy { candidate -> candidate.dateValue(now) }
                    .then(STABLE_TIE_ORDER)
            else -> compareBy<IndexedOpportunity> { candidate -> candidate.dateValue(now) }
                .then(STABLE_TIE_ORDER)
        }
        return filtered.sortedWith(comparator).map(IndexedOpportunity::result)
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
        val language: AppLanguage,
        val catalog: AppStringCatalog?,
        val origin: SearchCoordinate?,
    ) {
        val searchableText: String by lazy(LazyThreadSafetyMode.NONE) {
            LocalizedOpportunitySearchIndex.normalizedText(opportunity, language, catalog)
        }

        private val weightedFields: List<Pair<String, Int>> by lazy(LazyThreadSafetyMode.NONE) {
            weightedFields(opportunity, language, catalog)
        }

        val distanceKm: Double? by lazy(LazyThreadSafetyMode.NONE) {
            val from = origin ?: return@lazy null
            val to = validCoordinate(opportunity.latitude, opportunity.longitude)
                ?: return@lazy null
            haversineDistanceKm(from, to)
        }

        val result: Opportunity
            get() = if (origin != null) opportunity.copy(distanceKm = distanceKm) else opportunity

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

    private fun weightedFields(
        opportunity: Opportunity,
        language: AppLanguage,
        catalog: AppStringCatalog?,
    ): List<Pair<String, Int>> {
        val localized = OpportunityLocalization.resolve(opportunity, language, catalog)
        return buildList {
            add(localized.title to 8)
            add(opportunity.title to 8)
            add(localized.organization to 5)
            add(opportunity.organization to 5)
            add(localized.category to 4)
            add(opportunity.category to 4)
            add(localized.city to 3)
            add(opportunity.city to 3)
            add(localized.summary to 3)
            opportunity.summary?.let { add(it to 3) }
            add(localized.description to 2)
            add(opportunity.description to 2)
            add(localized.region to 1)
            add(opportunity.region to 1)
            localized.tags.forEach { add(it to 3) }
            opportunity.tags.forEach { add(it to 3) }
        }.uniqueWeightedValues()
    }

    private fun List<Pair<String, Int>>.uniqueWeightedValues(): List<Pair<String, Int>> {
        val seen = mutableSetOf<String>()
        return mapNotNull { field ->
            val normalized = normalize(field.first)
            field.takeIf { normalized.isNotEmpty() && seen.add(normalized) }
        }
    }
}

private data class SearchCoordinate(
    val latitude: Double,
    val longitude: Double,
)

private fun validCoordinate(latitude: Double?, longitude: Double?): SearchCoordinate? {
    if (latitude == null || longitude == null) return null
    if (!latitude.isFinite() || !longitude.isFinite()) return null
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    return SearchCoordinate(latitude = latitude, longitude = longitude)
}

private fun haversineDistanceKm(from: SearchCoordinate, to: SearchCoordinate): Double {
    val fromLatitude = Math.toRadians(from.latitude)
    val toLatitude = Math.toRadians(to.latitude)
    val latitudeDelta = Math.toRadians(to.latitude - from.latitude)
    val longitudeDelta = Math.toRadians(to.longitude - from.longitude)
    val latitudeComponent = sin(latitudeDelta / 2.0)
    val longitudeComponent = sin(longitudeDelta / 2.0)
    val haversine = (
        latitudeComponent * latitudeComponent +
            longitudeComponent * longitudeComponent * cos(fromLatitude) * cos(toLatitude)
        ).coerceIn(0.0, 1.0)
    return EARTH_RADIUS_KM * 2.0 * atan2(sqrt(haversine), sqrt(1.0 - haversine))
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
private const val EARTH_RADIUS_KM = 6_371.0
