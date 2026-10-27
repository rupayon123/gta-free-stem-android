package com.rupayonhaldar.gtafreestem.data.feed

import com.rupayonhaldar.gtafreestem.domain.validation.FeedFreshnessPolicy
import java.net.URI
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import kotlinx.serialization.Serializable

/** Binary-reviewed source authority; publisher metadata cannot approve a new provider. */
internal object OpportunityContentRightsPolicy {
    private val approved = mapOf(
        ("Markham Public Library" to "markham.bibliocommons.com") to
            "sha256:451e63466c801166653f47336e236d05f9a50a3df45a917fa603e92bcd11c8a0",
        ("Toronto Public Library" to "tpl.bibliocommons.com") to
            "sha256:c9b14cbd531afa09533e844274bb9f07f8042883e12b5a3990be3100ca5d4b43",
    )
    private val conditions = listOf(
        "Automated access uses the provider's RSS/XML event feed.",
        "Every displayed extract links to its original BiblioCommons event page.",
        "GTA FREE STEM is free and does not monetize provider content.",
    )
    private val ownerScope = listOf("display-event-listings", "translate-event-listings")

    fun isApproved(
        rights: ContentRightsPayload,
        extracts: List<PublishedRightsExtract>,
        dataUpdated: Instant,
        now: Instant,
        requireCurrentEvidence: Boolean,
    ): Boolean {
        val verified = FeedFreshnessPolicy.parse(rights.termsVerifiedAt) ?: return false
        if (verified.isAfter(now) ||
            (requireCurrentEvidence && !FeedFreshnessPolicy.isFresh(verified, now))
        ) return false
        val verifiedDay = verified.atOffset(ZoneOffset.UTC).toLocalDate()
        val dataDay = dataUpdated.atOffset(ZoneOffset.UTC).toLocalDate()
        if (verifiedDay < dataDay) return false
        val restricted = rights.restrictedOpportunityIds?.map(String::trim) ?: return false
        val restrictedSet = restricted.toSet()
        if (restricted.any(String::isEmpty) || restrictedSet.size != restricted.size) return false
        val ids = extracts.map { it.id.trim() }
        if (ids.any(String::isEmpty) || ids.toSet().size != ids.size || ids.any { it in restrictedSet }) return false
        val sources = rights.sources?.takeIf { it.isNotEmpty() } ?: return false
        val declared = mutableMapOf<Pair<String, String>, Int>()
        for (source in sources) {
            val organization = source.organization?.trim() ?: return false
            val host = source.sourceHost?.trim()?.lowercase(Locale.ROOT) ?: return false
            val key = organization to host
            val digest = approved[key] ?: return false
            val records = source.records?.takeIf { it > 0 } ?: return false
            if (declared.put(key, records) != null) return false
            when (source.status) {
                "owner-confirmed-permission" -> {
                    val permission = source.ownerPermission ?: return false
                    // This records the owner's confirmation, not a written agreement
                    // or a permission expiry date. Manifest freshness is separate.
                    if (permission.confirmationID != "gta-library-content-2026-10-25" ||
                        permission.confirmedAt != "2026-10-25" || permission.scope != ownerScope
                    ) return false
                }
                "terms-permitted" -> {
                    val sourceVerified = FeedFreshnessPolicy.parse(source.termsVerifiedAt) ?: return false
                    if (sourceVerified.atOffset(ZoneOffset.UTC).toLocalDate() != verifiedDay ||
                        source.basisUrl != "https://$host/info/terms/" ||
                        source.termsVersion != "2018-09-19" ||
                        source.termsDigestVersion != "bibliocommons-relevant-sections-v1" ||
                        source.termsSectionDigest != digest || source.conditions != conditions
                    ) return false
                }
                else -> return false
            }
        }
        val observed = mutableMapOf<Pair<String, String>, Int>()
        for (extract in extracts) {
            val source = extract.sourceUrl ?: return false
            val uri = runCatching { URI(source) }.getOrNull() ?: return false
            val host = uri.host?.lowercase(Locale.ROOT) ?: return false
            if (!uri.scheme.equals("https", ignoreCase = true) || uri.userInfo != null ||
                uri.port != -1 || uri.rawPath?.startsWith("/events/") != true ||
                extract.registrationUrl != source || source !in extract.sourceEvidenceUrls
            ) return false
            val organization = extract.organization?.trim() ?: return false
            val key = organization to host
            if (key !in declared) return false
            observed[key] = (observed[key] ?: 0) + 1
        }
        return observed == declared
    }
}

internal data class PublishedRightsExtract(
    val id: String,
    val organization: String?,
    val sourceUrl: String?,
    val registrationUrl: String?,
    val sourceEvidenceUrls: List<String>,
)

@Serializable
internal data class ContentRightsPayload(
    val termsVerifiedAt: String? = null,
    val restrictedOpportunityIds: List<String>? = null,
    val sources: List<SourceRightsPayload>? = null,
)

@Serializable
internal data class SourceRightsPayload(
    val organization: String? = null,
    val sourceHost: String? = null,
    val records: Int? = null,
    val status: String? = null,
    val ownerPermission: OwnerPermissionPayload? = null,
    val basisUrl: String? = null,
    val conditions: List<String>? = null,
    val termsVerifiedAt: String? = null,
    val termsVersion: String? = null,
    val termsDigestVersion: String? = null,
    val termsSectionDigest: String? = null,
)

@Serializable
internal data class OwnerPermissionPayload(
    val confirmationID: String? = null,
    val confirmedAt: String? = null,
    val scope: List<String>? = null,
)
