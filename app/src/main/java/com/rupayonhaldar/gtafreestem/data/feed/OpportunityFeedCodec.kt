package com.rupayonhaldar.gtafreestem.data.feed

import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.domain.model.OpportunitySourceEvidence
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityTranslation
import com.rupayonhaldar.gtafreestem.domain.validation.FeedFreshnessPolicy
import com.rupayonhaldar.gtafreestem.domain.validation.OpportunityCostEligibility
import java.net.URI
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

enum class InvalidOpportunityFeedReason {
    MALFORMED_JSON,
    MISSING_PAYLOAD,
    COUNT_MISMATCH,
    EMPTY_PAYLOAD,
    TOO_MANY_RECORDS,
    DUPLICATE_ID,
    NO_VALID_OPPORTUNITIES,
    NO_EXPLICITLY_FREE_OPPORTUNITIES,
    MISSING_OR_INVALID_FRESHNESS,
    STALE_OR_FUTURE_FEED,
    MISSING_SOURCE_HEALTH,
    UNHEALTHY_SOURCE_METRICS,
}

class InvalidOpportunityFeedException(
    val reason: InvalidOpportunityFeedReason,
    cause: Throwable? = null,
) : IllegalArgumentException(reason.name, cause)

internal data class ValidatedOpportunityFeed(
    val opportunities: List<Opportunity>,
    val lastUpdated: Instant,
    val declaredRecordCount: Int,
    val isStale: Boolean,
)

/** Serialization and validation boundary for the untrusted public JSON feed. */
internal class OpportunityFeedCodec(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = false
        coerceInputValues = false
    },
) {
    private companion object {
        const val MAX_RECORD_COUNT = 5_000
    }

    fun decodeAndValidate(
        jsonText: String,
        now: Instant,
        requireFreshness: Boolean,
        requireSourceHealth: Boolean = false,
    ): ValidatedOpportunityFeed {
        val envelope = try {
            json.decodeFromString<PublicOpportunityFeed>(jsonText)
        } catch (error: SerializationException) {
            throw InvalidOpportunityFeedException(InvalidOpportunityFeedReason.MALFORMED_JSON, error)
        } catch (error: IllegalArgumentException) {
            throw InvalidOpportunityFeedException(InvalidOpportunityFeedReason.MALFORMED_JSON, error)
        }

        val raw = envelope.opportunities ?: envelope.data
            ?: throw InvalidOpportunityFeedException(InvalidOpportunityFeedReason.MISSING_PAYLOAD)

        if (raw.size > MAX_RECORD_COUNT) {
            throw InvalidOpportunityFeedException(InvalidOpportunityFeedReason.TOO_MANY_RECORDS)
        }

        // Counts describe the publisher payload, so they are checked before the app removes
        // invalid or non-free records.
        listOfNotNull(envelope.count, envelope.meta?.activeCount).forEach { declaredCount ->
            if (declaredCount != raw.size) {
                throw InvalidOpportunityFeedException(InvalidOpportunityFeedReason.COUNT_MISMATCH)
            }
        }
        if (raw.isEmpty()) {
            throw InvalidOpportunityFeedException(InvalidOpportunityFeedReason.EMPTY_PAYLOAD)
        }

        val health = envelope.sourceHealth
        if (requireSourceHealth && health == null) {
            throw InvalidOpportunityFeedException(InvalidOpportunityFeedReason.MISSING_SOURCE_HEALTH)
        }
        if (health != null && !health.isHealthy(raw.size, requireComplete = requireSourceHealth)) {
            throw InvalidOpportunityFeedException(InvalidOpportunityFeedReason.UNHEALTHY_SOURCE_METRICS)
        }

        val declaredDate = envelope.lastDataChange.nonBlankOrNull()
            ?: envelope.meta?.lastUpdated.nonBlankOrNull()
        val lastUpdated = FeedFreshnessPolicy.parse(declaredDate)
            ?: throw InvalidOpportunityFeedException(
                InvalidOpportunityFeedReason.MISSING_OR_INVALID_FRESHNESS,
            )
        val isFresh = FeedFreshnessPolicy.isFresh(lastUpdated, now)
        if (requireFreshness && !isFresh) {
            throw InvalidOpportunityFeedException(InvalidOpportunityFeedReason.STALE_OR_FUTURE_FEED)
        }

        val safePayloads = raw.filter(OpportunityPayload::isSafeForMapping)
        if (safePayloads.isEmpty()) {
            throw InvalidOpportunityFeedException(InvalidOpportunityFeedReason.NO_VALID_OPPORTUNITIES)
        }

        val seen = mutableSetOf<String>()
        safePayloads.forEach { payload ->
            if (!seen.add(payload.id.trim())) {
                throw InvalidOpportunityFeedException(InvalidOpportunityFeedReason.DUPLICATE_ID)
            }
        }

        val opportunities = safePayloads
            .filter { OpportunityCostEligibility.isExplicitlyFree(it.cost.asStringOrNull()) }
            .map(OpportunityPayload::toDomain)
        if (opportunities.isEmpty()) {
            throw InvalidOpportunityFeedException(
                InvalidOpportunityFeedReason.NO_EXPLICITLY_FREE_OPPORTUNITIES,
            )
        }

        return ValidatedOpportunityFeed(
            opportunities = opportunities,
            lastUpdated = lastUpdated,
            declaredRecordCount = raw.size,
            isStale = !isFresh,
        )
    }
}

@Serializable
private data class PublicOpportunityFeed(
    val count: Int? = null,
    val lastDataChange: String? = null,
    val meta: FeedMetadata? = null,
    val sourceHealth: SourceHealthPayload? = null,
    val opportunities: List<OpportunityPayload>? = null,
    val data: List<OpportunityPayload>? = null,
)

@Serializable
private data class FeedMetadata(
    val activeCount: Int? = null,
    val lastUpdated: String? = null,
)

@Serializable
private data class OpportunityPayload(
    val id: String = "",
    val title: String = "",
    val organization: String? = null,
    val provider: String? = null,
    val description: String? = null,
    val summary: String? = null,
    val category: String? = null,
    val categories: List<String> = emptyList(),
    val city: String? = null,
    val region: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val virtual: Boolean = false,
    val startDate: String? = null,
    val endDate: String? = null,
    val deadline: String? = null,
    val ageMin: Int? = null,
    val ageMax: Int? = null,
    val ages: AgeRangePayload? = null,
    val language: List<String>? = null,
    val languages: List<String>? = null,
    val cost: JsonElement? = null,
    val sourceUrl: String? = null,
    val registrationUrl: String? = null,
    val status: String? = null,
    val communityFocus: List<String> = emptyList(),
    val accessibility: List<String> = emptyList(),
    val equipment: String? = null,
    val food: String? = null,
    val capacity: String? = null,
    val commitment: String? = null,
    val providerContact: String? = null,
    val freeStatusProof: String? = null,
    val trustedSource: Boolean = false,
    val volunteerHoursEligible: Boolean = false,
    val coopEligible: Boolean = false,
    val tags: List<String> = emptyList(),
    val sources: List<OpportunitySourceEvidence> = emptyList(),
    val translations: Map<String, OpportunityTranslationPayload>? = null,
    val localizations: Map<String, OpportunityTranslationPayload>? = null,
    val localized: Map<String, OpportunityTranslationPayload>? = null,
) {
    fun isSafeForMapping(): Boolean {
        val normalizedId = id.trim()
        val normalizedTitle = title.trim()
        val normalizedProvider = organization.nonBlankOrNull() ?: provider.nonBlankOrNull()
        val normalizedDescription = description.nonBlankOrNull() ?: summary.nonBlankOrNull()
        val normalizedCategories = categories.mapNotNull(String::nonBlankOrNull)
        val normalizedCategory = category.nonBlankOrNull() ?: normalizedCategories.firstOrNull()
        val normalizedAgeMin = ages?.min ?: ageMin
        val normalizedAgeMax = ages?.max ?: ageMax
        val suppliedUrls = listOfNotNull(sourceUrl, registrationUrl)

        return normalizedId.length in 1..MAX_ID_LENGTH &&
            normalizedTitle.length in 1..MAX_TITLE_LENGTH &&
            normalizedProvider != null && normalizedProvider.length in 1..MAX_PROVIDER_LENGTH &&
            organization.isWithinLengthWhenPresent(MAX_PROVIDER_LENGTH) &&
            provider.isWithinLengthWhenPresent(MAX_PROVIDER_LENGTH) &&
            normalizedDescription != null &&
            normalizedDescription.length in 1..MAX_DESCRIPTION_LENGTH &&
            description.isWithinLengthWhenPresent(MAX_DESCRIPTION_LENGTH) &&
            summary.isWithinLengthWhenPresent(MAX_DESCRIPTION_LENGTH) &&
            normalizedCategory != null && normalizedCategory.length in 1..MAX_CATEGORY_LENGTH &&
            category.isWithinLengthWhenPresent(MAX_CATEGORY_LENGTH) &&
            categories.size <= MAX_CATEGORY_COUNT &&
            normalizedCategories.size == categories.size &&
            normalizedCategories.all { it.length <= MAX_CATEGORY_LENGTH } &&
            status.nonBlankOrNull().equals("active", ignoreCase = true) &&
            normalizedAgeMin != null && normalizedAgeMin in MIN_AGE..MAX_AGE &&
            (normalizedAgeMax == null || normalizedAgeMax in normalizedAgeMin..MAX_AGE) &&
            suppliedUrls.isNotEmpty() && suppliedUrls.all(::isSafeHttpsUrl) &&
            sources.size <= MAX_SOURCE_EVIDENCE_COUNT &&
            sources.all { isSafeHttpsUrl(it.url) }
    }

    fun toDomain(): Opportunity {
        val providerName = organization.nonBlankOrNull()
            ?: provider.nonBlankOrNull()
            ?: "Community provider"
        val normalizedTitle = title.trim().ifBlank { "Opportunity" }
        val normalizedSummary = summary.nonBlankOrNull()
        val normalizedDescription = description.nonBlankOrNull()
            ?: normalizedSummary
            ?: normalizedTitle
        val normalizedCategories = categories.mapNotNull(String::nonBlankOrNull)
        val normalizedCategory = category.nonBlankOrNull()
            ?: normalizedCategories.firstOrNull()
            ?: "STEM"
        val normalizedLanguages = (language ?: languages)
            ?.mapNotNull(String::nonBlankOrNull)
            ?.ifEmpty { null }
            ?: listOf("en")
        val normalizedCost = cost.asStringOrNull()?.trim().orEmpty()
        val normalizedRegistrationUrl = registrationUrl.nonBlankOrNull()
        val normalizedSourceUrl = sourceUrl.nonBlankOrNull()
            ?: normalizedRegistrationUrl
            ?: ""
        val coordinatesAreValid = latitude?.isFinite() == true &&
            longitude?.isFinite() == true &&
            latitude in -90.0..90.0 &&
            longitude in -180.0..180.0
        val decodedTranslations = translations ?: localizations ?: localized ?: emptyMap()

        return Opportunity(
            id = id.trim(),
            title = normalizedTitle,
            organization = providerName,
            description = normalizedDescription,
            summary = normalizedSummary,
            category = normalizedCategory,
            categories = normalizedCategories.ifEmpty { listOf(normalizedCategory) },
            city = city.nonBlankOrNull() ?: "GTA",
            region = region.nonBlankOrNull() ?: "All",
            address = address.nonBlankOrNull(),
            latitude = latitude.takeIf { coordinatesAreValid },
            longitude = longitude.takeIf { coordinatesAreValid },
            virtual = virtual,
            startDate = startDate.nonBlankOrNull(),
            endDate = endDate.nonBlankOrNull(),
            deadline = deadline.nonBlankOrNull(),
            ageMin = ages?.min ?: ageMin ?: 0,
            ageMax = ages?.max ?: ageMax,
            languages = normalizedLanguages,
            cost = normalizedCost,
            sourceUrl = normalizedSourceUrl,
            registrationUrl = normalizedRegistrationUrl,
            status = status.nonBlankOrNull() ?: "active",
            communityFocus = communityFocus.mapNotNull(String::nonBlankOrNull),
            accessibility = accessibility.mapNotNull(String::nonBlankOrNull),
            equipment = equipment.nonBlankOrNull(),
            food = food.nonBlankOrNull(),
            capacity = capacity.nonBlankOrNull(),
            commitment = commitment.nonBlankOrNull(),
            providerContact = providerContact.nonBlankOrNull(),
            freeStatusProof = freeStatusProof.nonBlankOrNull(),
            trustedSource = trustedSource,
            volunteerHoursEligible = volunteerHoursEligible,
            coopEligible = coopEligible,
            tags = tags.mapNotNull(String::nonBlankOrNull),
            sources = sources,
            translations = decodedTranslations.mapValues { it.value.toDomain() },
        )
    }
}

@Serializable
private data class AgeRangePayload(
    val min: Int? = null,
    val max: Int? = null,
)

@Serializable
private data class OpportunityTranslationPayload(
    val title: String? = null,
    val organization: String? = null,
    val provider: String? = null,
    val description: String? = null,
    val summary: String? = null,
    val category: String? = null,
    val city: String? = null,
    val region: String? = null,
    val address: String? = null,
    val cost: String? = null,
    val tags: List<String>? = null,
) {
    fun toDomain() = OpportunityTranslation(
        title = title.nonBlankOrNull(),
        organization = organization.nonBlankOrNull() ?: provider.nonBlankOrNull(),
        description = description.nonBlankOrNull(),
        summary = summary.nonBlankOrNull(),
        category = category.nonBlankOrNull(),
        city = city.nonBlankOrNull(),
        region = region.nonBlankOrNull(),
        address = address.nonBlankOrNull(),
        cost = cost.nonBlankOrNull(),
        tags = tags?.mapNotNull(String::nonBlankOrNull),
    )
}

@Serializable
private data class SourceHealthPayload(
    val library: LibraryHealthPayload? = null,
    val discovery: DiscoveryHealthPayload? = null,
) {
    fun isHealthy(publishedCount: Int, requireComplete: Boolean): Boolean {
        if (library == null && discovery == null) return false
        if (requireComplete && (library == null || discovery == null)) return false
        return (library?.isHealthy(publishedCount) != false) &&
            (discovery?.isHealthy() != false)
    }
}

@Serializable
private data class LibraryHealthPayload(
    val status: String? = null,
    val attemptedPages: Int? = null,
    val successfulPages: Int? = null,
    val pageSuccessRatio: Double? = null,
    val minimumPageSuccessRatio: Double? = null,
    val acceptedListings: Int? = null,
    val minimumAcceptedListings: Int? = null,
) {
    fun isHealthy(publishedCount: Int): Boolean {
        val attempted = attemptedPages ?: return false
        val successful = successfulPages ?: return false
        val ratio = pageSuccessRatio ?: return false
        val minimumRatio = minimumPageSuccessRatio ?: return false
        val accepted = acceptedListings ?: return false
        val minimumAccepted = minimumAcceptedListings ?: return false
        return status.equals("healthy", ignoreCase = true) &&
            attempted > 0 &&
            successful in 0..attempted &&
            ratio.isFinite() && minimumRatio.isFinite() &&
            ratio in 0.0..1.0 && minimumRatio in 0.0..1.0 &&
            ratio >= minimumRatio &&
            successful.toDouble() / attempted.toDouble() >= minimumRatio &&
            accepted >= 0 && minimumAccepted >= 0 &&
            accepted >= minimumAccepted &&
            publishedCount >= minimumAccepted
    }
}

@Serializable
private data class DiscoveryHealthPayload(
    val status: String? = null,
    val sourcesChecked: Int? = null,
    val successfulSources: Int? = null,
    val sourceSuccessRatio: Double? = null,
    val minimumSourceSuccessRatio: Double? = null,
) {
    fun isHealthy(): Boolean {
        val checked = sourcesChecked ?: return false
        val successful = successfulSources ?: return false
        val ratio = sourceSuccessRatio ?: return false
        val minimumRatio = minimumSourceSuccessRatio ?: return false
        return status.equals("healthy", ignoreCase = true) &&
            checked > 0 &&
            successful in 0..checked &&
            ratio.isFinite() && minimumRatio.isFinite() &&
            ratio in 0.0..1.0 && minimumRatio in 0.0..1.0 &&
            ratio >= minimumRatio &&
            successful.toDouble() / checked.toDouble() >= minimumRatio
    }
}

private fun String?.nonBlankOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)

private fun String?.isWithinLengthWhenPresent(maxLength: Int): Boolean =
    this == null || trim().length <= maxLength

private fun JsonElement?.asStringOrNull(): String? =
    (this as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content

private fun isSafeHttpsUrl(rawUrl: String): Boolean {
    val normalized = rawUrl.trim()
    if (normalized.length !in 1..MAX_URL_LENGTH) return false
    val uri = runCatching { URI(normalized) }.getOrNull() ?: return false
    return !uri.isOpaque &&
        uri.scheme.equals("https", ignoreCase = true) &&
        !uri.host.isNullOrBlank() &&
        uri.userInfo == null &&
        (uri.port == -1 || uri.port in 1..65_535)
}

private const val MAX_ID_LENGTH = 256
private const val MAX_TITLE_LENGTH = 300
private const val MAX_PROVIDER_LENGTH = 300
private const val MAX_DESCRIPTION_LENGTH = 20_000
private const val MAX_CATEGORY_LENGTH = 300
private const val MAX_CATEGORY_COUNT = 100
private const val MAX_SOURCE_EVIDENCE_COUNT = 20
private const val MAX_URL_LENGTH = 4_096
private const val MIN_AGE = 0
private const val MAX_AGE = 120
