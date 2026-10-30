package com.rupayonhaldar.gtafreestem.data.local

import kotlinx.coroutines.CancellationException

/** Per-store deletion status lets the UI report an incomplete local wipe honestly. */
data class LocalAccountDataDeletionResult(
    val profileDeleted: Boolean,
    val searchHistoryDeleted: Boolean,
    val savedOpportunitiesDeleted: Boolean,
    val localAlertsDeleted: Boolean = true,
    val namedSearchesDeleted: Boolean = true,
) {
    val allLocalAccountDataDeleted: Boolean
        get() = profileDeleted &&
            searchHistoryDeleted &&
            savedOpportunitiesDeleted &&
            localAlertsDeleted && namedSearchesDeleted
}

/**
 * Coordinates best-effort deletion of local personal data. Every action is attempted even
 * if one fails or throws; there is deliberately no server, sign-in, or cloud-account operation.
 */
class LocalAccountDataDeletionCoordinator(
    private val deleteProfile: () -> Boolean,
    private val deleteSearchHistory: () -> Boolean,
    private val deleteSavedOpportunities: () -> Boolean,
    private val deleteLocalAlerts: () -> Boolean = { true },
    private val deleteNamedSearches: suspend () -> Boolean = { true },
) {
    suspend fun deleteAllLocalAccountData(): LocalAccountDataDeletionResult {
        // Revoke alert work first so a refresh cannot notify while the remaining local stores are
        // being erased. Every deletion is still attempted even when this first step fails.
        val localAlertsDeleted = safelyDelete(deleteLocalAlerts)
        return LocalAccountDataDeletionResult(
            profileDeleted = safelyDelete(deleteProfile),
            searchHistoryDeleted = safelyDelete(deleteSearchHistory),
            savedOpportunitiesDeleted = safelyDelete(deleteSavedOpportunities),
            localAlertsDeleted = localAlertsDeleted,
            namedSearchesDeleted = safelyDelete(deleteNamedSearches),
        )
    }

    private suspend fun safelyDelete(delete: suspend () -> Boolean): Boolean = try {
        delete()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Throwable) {
        false
    }
}
