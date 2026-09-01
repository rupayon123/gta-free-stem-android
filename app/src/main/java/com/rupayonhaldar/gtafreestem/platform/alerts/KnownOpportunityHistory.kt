package com.rupayonhaldar.gtafreestem.platform.alerts

data class KnownOpportunityMergeResult(
    val retainedIds: List<String>,
    val newCount: Int,
)

/** Bounded, deterministic counterpart of the Apple app's known-opportunity history policy. */
object KnownOpportunityHistory {
    const val MAXIMUM_COUNT = 2_500
    const val MAXIMUM_ID_LENGTH = 256

    fun merge(
        currentIds: Iterable<String>,
        previousIds: Iterable<String>,
        limit: Int = MAXIMUM_COUNT,
    ): KnownOpportunityMergeResult {
        val normalizedLimit = limit.coerceIn(0, MAXIMUM_COUNT)
        // The retained cap is also the comparison cap. This prevents feeds above the safety
        // bound from re-reporting the same unpersistable tail on every periodic refresh.
        val current = currentIds.normalizedUniqueIds().take(normalizedLimit)
        val previous = previousIds.normalizedUniqueIds()
        val previousSet = previous.toHashSet()
        val newCount = current.count { it !in previousSet }
        if (normalizedLimit == 0) return KnownOpportunityMergeResult(emptyList(), newCount)

        val retained = ArrayList<String>(normalizedLimit)
        val retainedSet = HashSet<String>()
        (current.asSequence() + previous.asSequence()).forEach { id ->
            if (retained.size < normalizedLimit && retainedSet.add(id)) retained += id
        }
        return KnownOpportunityMergeResult(retained, newCount)
    }

    private fun Iterable<String>.normalizedUniqueIds(): List<String> {
        val seen = HashSet<String>()
        return mapNotNull { raw ->
            raw.trim()
                .takeIf { it.isNotEmpty() && it.length <= MAXIMUM_ID_LENGTH }
                ?.takeIf(seen::add)
        }
    }
}
