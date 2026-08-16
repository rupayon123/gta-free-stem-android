package com.rupayonhaldar.gtafreestem.data.local

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSnapshot
import java.time.Instant

/** A locally retained opportunity and the time at which the user saved it. */
data class SavedOpportunityEntry(
    val opportunity: Opportunity,
    val savedAt: Instant,
)

/** Current and past saved opportunities, each ordered from most recently saved. */
data class SavedOpportunitySections(
    val current: List<SavedOpportunityEntry>,
    val archive: List<SavedOpportunityEntry>,
)

/** Result details make conservative feed reconciliation observable and testable. */
data class SavedOpportunityReconciliation(
    val refreshedCount: Int,
    val hydratedCount: Int,
    val markedUnavailableCount: Int,
    val discardedUnresolvedCount: Int,
    val completeFeedApplied: Boolean,
    val persisted: Boolean,
)

interface FavoriteOpportunityStore {
    /** Includes both full snapshots and legacy IDs awaiting a matching opportunity. */
    fun ids(): Set<String>

    fun contains(id: String): Boolean

    /**
     * Compatibility API for callers that only know an ID. A saved ID is retained until a
     * matching opportunity can hydrate it or a healthy complete feed proves it no longer exists.
     */
    fun setFavorite(id: String, favorite: Boolean)

    /** Returns the resulting favorite state. Blank or oversized identifiers are ignored. */
    fun toggle(id: String): Boolean

    /** Removes either a full snapshot or an unresolved legacy ID and reports persistence success. */
    fun remove(id: String): Boolean

    /** Atomically deletes every saved snapshot and unresolved legacy ID from local persistence. */
    fun clearAll(): Boolean

    /** Returns only saves that already have an offline-capable opportunity snapshot. */
    fun entries(): List<SavedOpportunityEntry>

    /** IDs migrated from the old store that have not yet matched a validated feed record. */
    fun unresolvedIds(): Set<String>

    fun sections(now: Instant = Instant.now()): SavedOpportunitySections

    /** Returns whether the requested state was persisted. Existing saves keep their savedAt. */
    fun setFavorite(
        opportunity: Opportunity,
        favorite: Boolean,
        savedAt: Instant = Instant.now(),
    ): Boolean

    /** Returns the resulting favorite state. Existing saves keep their original savedAt. */
    fun toggle(
        opportunity: Opportunity,
        savedAt: Instant = Instant.now(),
    ): Boolean

    /**
     * Refreshes saved payloads without changing savedAt. Only a fresh, complete network feed may
     * mark missing snapshots unavailable or discard unmatched legacy IDs.
     */
    fun reconcile(
        snapshot: OpportunityFeedSnapshot,
        reconciledAt: Instant = Instant.now(),
    ): SavedOpportunityReconciliation
}

/** SharedPreferences adapter; persistence and migration policy live in a JVM-testable store. */
class SharedPreferencesFavoriteOpportunityStore private constructor(
    private val delegate: FavoriteOpportunityStore,
) : FavoriteOpportunityStore by delegate {
    constructor(context: Context) : this(
        PersistentFavoriteOpportunityStore(
            persistence = SharedPreferencesFavoriteOpportunityPersistence(
                context.applicationContext.getSharedPreferences(
                    PREFERENCES_NAME,
                    Context.MODE_PRIVATE,
                ),
            ),
        ),
    )

    private companion object {
        const val PREFERENCES_NAME = "gta_free_stem_favorites"
    }
}

internal class SharedPreferencesFavoriteOpportunityPersistence(
    private val preferences: SharedPreferences,
) : FavoriteOpportunityPersistence {
    override fun readSnapshotJson(): String? = synchronized(preferences) {
        preferences.getString(SNAPSHOT_JSON_KEY, null)
    }

    override fun readLegacyIds(): Set<String> = synchronized(preferences) {
        preferences.getStringSet(FAVORITE_IDS_KEY, emptySet()).orEmpty().toSet()
    }

    @SuppressLint("ApplySharedPref")
    override fun write(snapshotJson: String, ids: Set<String>): Boolean =
        runCatching {
            synchronized(preferences) {
                preferences.edit()
                    .putString(SNAPSHOT_JSON_KEY, snapshotJson)
                    // Keep the old key as a recovery mirror and for ID-only compatibility.
                    .putStringSet(FAVORITE_IDS_KEY, ids.toSet())
                    // State is updated only when disk persistence reports success.
                    .commit()
            }
        }.getOrDefault(false)

    @SuppressLint("ApplySharedPref")
    override fun clear(): Boolean = runCatching {
        synchronized(preferences) {
            preferences.edit()
                .remove(SNAPSHOT_JSON_KEY)
                .remove(FAVORITE_IDS_KEY)
                .commit()
        }
    }.getOrDefault(false)

    private companion object {
        const val FAVORITE_IDS_KEY = "favorite_opportunity_ids"
        const val SNAPSHOT_JSON_KEY = "saved_opportunity_snapshot_v1"
    }
}
