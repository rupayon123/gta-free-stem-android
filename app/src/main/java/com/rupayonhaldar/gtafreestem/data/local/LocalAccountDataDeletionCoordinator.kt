package com.rupayonhaldar.gtafreestem.data.local

/** Per-store deletion status lets the UI report an incomplete local wipe honestly. */
data class LocalAccountDataDeletionResult(
    val profileDeleted: Boolean,
    val searchHistoryDeleted: Boolean,
    val savedOpportunitiesDeleted: Boolean,
) {
    val allLocalAccountDataDeleted: Boolean
        get() = profileDeleted && searchHistoryDeleted && savedOpportunitiesDeleted
}

/**
 * Coordinates best-effort deletion of local personal data. All three actions are attempted even
 * if one fails or throws; there is deliberately no server, sign-in, or cloud-account operation.
 */
class LocalAccountDataDeletionCoordinator(
    private val deleteProfile: () -> Boolean,
    private val deleteSearchHistory: () -> Boolean,
    private val deleteSavedOpportunities: () -> Boolean,
) {
    fun deleteAllLocalAccountData(): LocalAccountDataDeletionResult =
        LocalAccountDataDeletionResult(
            profileDeleted = safelyDelete(deleteProfile),
            searchHistoryDeleted = safelyDelete(deleteSearchHistory),
            savedOpportunitiesDeleted = safelyDelete(deleteSavedOpportunities),
        )

    private fun safelyDelete(delete: () -> Boolean): Boolean =
        runCatching(delete).getOrDefault(false)
}
