package com.rupayonhaldar.gtafreestem.platform.alerts

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

internal data class OpportunityAlertHistoryState(
    val knownIds: List<String> = emptyList(),
    val hasBaseline: Boolean = false,
    val lastNotificationEpochMillis: Long? = null,
)

/**
 * An opaque, single-run capability. Replacing or removing the persisted token revokes every
 * already-running refresh that captured the previous value.
 */
internal data class OpportunityAlertRunLease(
    val token: String,
)

internal sealed interface OpportunityAlertLeaseAccess<out T> {
    data class Granted<T>(val value: T) : OpportunityAlertLeaseAccess<T>
    data object Stale : OpportunityAlertLeaseAccess<Nothing>
    data object Failed : OpportunityAlertLeaseAccess<Nothing>
}

internal interface OpportunityAlertLeaseStore {
    fun currentLease(): OpportunityAlertRunLease?

    /** Rotates and returns the token that must be bound into the newly scheduled work request. */
    fun replaceActiveLease(): OpportunityAlertRunLease?

    /** Revokes the token before preference changes or WorkManager cancellation are attempted. */
    fun invalidateLease(): Boolean

    /** Serializes the final policy check and local notification side effect with revocation. */
    fun <T> withActiveLease(
        lease: OpportunityAlertRunLease,
        action: () -> T,
    ): OpportunityAlertLeaseAccess<T>
}

internal interface OpportunityAlertHistoryStore : OpportunityAlertLeaseStore {
    fun read(): OpportunityAlertHistoryState

    fun saveKnownIds(
        lease: OpportunityAlertRunLease,
        ids: List<String>,
        hasBaseline: Boolean,
    ): OpportunityAlertLeaseAccess<Unit>

    fun recordNotificationAt(
        lease: OpportunityAlertRunLease,
        epochMillis: Long,
    ): OpportunityAlertLeaseAccess<Unit>

    /** Atomically revokes active work and clears all alert history. */
    fun clear(): Boolean
}

internal class SharedPreferencesOpportunityAlertHistoryStore(
    context: Context,
    private val newLeaseToken: () -> String = { UUID.randomUUID().toString() },
) : OpportunityAlertHistoryStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun currentLease(): OpportunityAlertRunLease? = synchronized(PROCESS_LOCK) {
        currentLeaseLocked()
    }

    override fun replaceActiveLease(): OpportunityAlertRunLease? = synchronized(PROCESS_LOCK) {
        val token = runCatching(newLeaseToken)
            .getOrNull()
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: return@synchronized null
        val lease = OpportunityAlertRunLease(token)
        val committed = runCatching {
            preferences.edit().putString(ACTIVE_LEASE_KEY, token).commit()
        }.getOrDefault(false)
        lease.takeIf { committed }
    }

    override fun invalidateLease(): Boolean = synchronized(PROCESS_LOCK) {
        if (!preferences.contains(ACTIVE_LEASE_KEY)) return@synchronized true
        runCatching {
            preferences.edit().remove(ACTIVE_LEASE_KEY).commit()
        }.getOrDefault(false)
    }

    override fun <T> withActiveLease(
        lease: OpportunityAlertRunLease,
        action: () -> T,
    ): OpportunityAlertLeaseAccess<T> = synchronized(PROCESS_LOCK) {
        if (currentLeaseLocked() != lease) {
            return@synchronized OpportunityAlertLeaseAccess.Stale
        }
        runCatching(action).fold(
            onSuccess = { OpportunityAlertLeaseAccess.Granted(it) },
            onFailure = { OpportunityAlertLeaseAccess.Failed },
        )
    }

    override fun read(): OpportunityAlertHistoryState = synchronized(PROCESS_LOCK) {
        val knownIds = runCatching {
            preferences.getStringSet(KNOWN_IDS_KEY, emptySet()).orEmpty().toList()
        }.getOrDefault(emptyList())
        val normalized = KnownOpportunityHistory.merge(
            currentIds = emptyList(),
            previousIds = knownIds,
        ).retainedIds
        val lastNotification = preferences
            .takeIf { it.contains(LAST_NOTIFICATION_KEY) }
            ?.getLong(LAST_NOTIFICATION_KEY, 0L)
            ?.takeIf { it > 0L }
        OpportunityAlertHistoryState(
            knownIds = normalized,
            hasBaseline = preferences.getBoolean(HAS_BASELINE_KEY, false),
            lastNotificationEpochMillis = lastNotification,
        )
    }

    override fun saveKnownIds(
        lease: OpportunityAlertRunLease,
        ids: List<String>,
        hasBaseline: Boolean,
    ): OpportunityAlertLeaseAccess<Unit> = guardedEdit(lease) { editor ->
        val normalized = KnownOpportunityHistory.merge(
            currentIds = ids,
            previousIds = emptyList(),
        ).retainedIds
        editor
            .putStringSet(KNOWN_IDS_KEY, normalized.toSet())
            .putBoolean(HAS_BASELINE_KEY, hasBaseline)
    }

    override fun recordNotificationAt(
        lease: OpportunityAlertRunLease,
        epochMillis: Long,
    ): OpportunityAlertLeaseAccess<Unit> {
        if (epochMillis <= 0L) return OpportunityAlertLeaseAccess.Failed
        return guardedEdit(lease) { it.putLong(LAST_NOTIFICATION_KEY, epochMillis) }
    }

    override fun clear(): Boolean = synchronized(PROCESS_LOCK) {
        runCatching {
            preferences.edit()
                .remove(ACTIVE_LEASE_KEY)
                .remove(KNOWN_IDS_KEY)
                .remove(HAS_BASELINE_KEY)
                .remove(LAST_NOTIFICATION_KEY)
                .commit()
        }.getOrDefault(false)
    }

    private fun guardedEdit(
        lease: OpportunityAlertRunLease,
        update: (SharedPreferences.Editor) -> SharedPreferences.Editor,
    ): OpportunityAlertLeaseAccess<Unit> = synchronized(PROCESS_LOCK) {
        if (currentLeaseLocked() != lease) {
            return@synchronized OpportunityAlertLeaseAccess.Stale
        }
        val committed = runCatching {
            update(preferences.edit()).commit()
        }.getOrDefault(false)
        if (committed) {
            OpportunityAlertLeaseAccess.Granted(Unit)
        } else {
            OpportunityAlertLeaseAccess.Failed
        }
    }

    private fun currentLeaseLocked(): OpportunityAlertRunLease? = preferences
        .getString(ACTIVE_LEASE_KEY, null)
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.let(::OpportunityAlertRunLease)

    private companion object {
        /** WorkManager uses the app process, so all adapters share this linearization boundary. */
        val PROCESS_LOCK = Any()
        const val PREFERENCES_NAME = "gta_free_stem_opportunity_alert_history"
        const val ACTIVE_LEASE_KEY = "active_refresh_lease"
        const val KNOWN_IDS_KEY = "known_matching_opportunity_ids"
        const val HAS_BASELINE_KEY = "has_known_opportunity_baseline"
        const val LAST_NOTIFICATION_KEY = "last_new_opportunity_notification_at"
    }
}
