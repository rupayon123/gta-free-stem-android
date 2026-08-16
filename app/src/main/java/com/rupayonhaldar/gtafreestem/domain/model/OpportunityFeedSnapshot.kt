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
)
