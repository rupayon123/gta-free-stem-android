package com.rupayonhaldar.gtafreestem.domain.model

import java.time.Instant

enum class OpportunityFeedSource {
    PRIMARY_NETWORK,
    FALLBACK_NETWORK,
    LAST_GOOD_CACHE,
    BUNDLED,
}

data class OpportunityFeedSnapshot(
    val opportunities: List<Opportunity>,
    val lastUpdated: Instant,
    val source: OpportunityFeedSource,
    val declaredRecordCount: Int,
    /** Local fallbacks remain useful offline even after the remote freshness window. */
    val isStale: Boolean,
    val publicationRevision: Instant? = null,
    /** Set by the repository after the whole payload passes feed validation. */
    val validatedCatalog: Boolean = false,
    /** Set only by the repository after validation of a complete accepted live publication. */
    val acceptedComplete: Boolean = false,
)
