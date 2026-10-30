package com.rupayonhaldar.gtafreestem.domain.search

import com.rupayonhaldar.gtafreestem.data.local.SavedSearchEntry
import com.rupayonhaldar.gtafreestem.data.local.SavedSearchSummary
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSnapshot
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSource

/** Only a validated, current network publication may establish new matches. */
object SavedSearchUpdatePolicy {
    fun canCountUpdates(snapshot: OpportunityFeedSnapshot): Boolean =
        snapshot.source in setOf(
            OpportunityFeedSource.PRIMARY_NETWORK,
            OpportunityFeedSource.FALLBACK_NETWORK,
        ) && snapshot.validatedCatalog && snapshot.acceptedComplete && !snapshot.isStale &&
            snapshot.publicationRevision != null &&
            snapshot.declaredRecordCount > 0 && snapshot.opportunities.isNotEmpty()

    fun summarize(
        entries: List<SavedSearchEntry>,
        matchingIds: Map<String, List<String>>,
        snapshot: OpportunityFeedSnapshot,
        previous: Map<String, SavedSearchSummary> = emptyMap(),
        quietRebaseline: Boolean = false,
    ): List<SavedSearchSummary> = entries.map { entry ->
        val current = matchingIds[entry.id].orEmpty().distinct()
        val currentSet = current.toSet()
        val revision = snapshot.publicationRevision?.toEpochMilli()
        val canAdvance = canCountUpdates(snapshot) && revision != null &&
            (entry.lastProcessedRevisionEpochMillis == null ||
                revision > entry.lastProcessedRevisionEpochMillis)
        val currentIsAccepted = canCountUpdates(snapshot) && revision != null &&
            (entry.lastProcessedRevisionEpochMillis == null ||
                revision >= entry.lastProcessedRevisionEpochMillis)
        val updated = if (quietRebaseline && currentIsAccepted) {
            entry.copy(
                knownMatchingIds = (entry.knownMatchingIds + current).distinct(),
                unreadMatchingIds = entry.unreadMatchingIds.filter(currentSet::contains),
                hasBaseline = true,
                lastProcessedRevisionEpochMillis = revision,
                lastKnownMatchCount = current.size,
            )
        } else if (canAdvance) {
            val knownSet = entry.knownMatchingIds.toSet()
            val newlySeen = if (entry.hasBaseline) current.filterNot(knownSet::contains) else emptyList()
            entry.copy(
                knownMatchingIds = (entry.knownMatchingIds + current).distinct(),
                unreadMatchingIds = (entry.unreadMatchingIds.filter(currentSet::contains) + newlySeen)
                    .distinct(),
                hasBaseline = true,
                lastProcessedRevisionEpochMillis = revision,
                lastKnownMatchCount = current.size,
            )
        } else if (currentIsAccepted) {
            entry.copy(lastKnownMatchCount = current.size)
        } else {
            entry
        }
        SavedSearchSummary(
            entry = updated,
            matchCount = updated.lastKnownMatchCount ?: previous[entry.id]?.matchCount,
            newCount = if (currentIsAccepted) {
                updated.unreadMatchingIds.count(currentSet::contains)
            } else {
                previous[entry.id]?.newCount ?: entry.unreadMatchingIds.size
            },
        )
    }
}
