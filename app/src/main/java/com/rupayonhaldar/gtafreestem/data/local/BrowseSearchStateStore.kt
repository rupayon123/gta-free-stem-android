package com.rupayonhaldar.gtafreestem.data.local

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchFilters
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchLimits
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

data class BrowseSearchState(
    val query: String = "",
    val filters: OpportunitySearchFilters = OpportunitySearchFilters(),
)

interface BrowseSearchStateStore {
    fun read(): BrowseSearchState

    /** Returns false without replacing the previous value when state exceeds a safety bound. */
    fun write(state: BrowseSearchState): Boolean

    /** Deletes the persisted query, filters, and sort selection. */
    fun clear(): Boolean

    companion object {
        val NONE: BrowseSearchStateStore = object : BrowseSearchStateStore {
            override fun read() = BrowseSearchState()
            override fun write(state: BrowseSearchState) = true
            override fun clear() = true
        }
    }
}

internal interface BrowseSearchStatePersistence {
    fun readStateJson(): String?
    fun writeStateJson(json: String): Boolean
    fun clearState(): Boolean
}

/** Pure Kotlin policy behind the Android adapter for focused corruption and round-trip tests. */
internal class PersistentBrowseSearchStateStore(
    private val persistence: BrowseSearchStatePersistence,
    private val json: Json = DEFAULT_JSON,
    private val maximumPayloadBytes: Int = MAXIMUM_PAYLOAD_BYTES,
) : BrowseSearchStateStore {
    init {
        require(maximumPayloadBytes > 0)
    }

    override fun read(): BrowseSearchState {
        val raw = runCatching { persistence.readStateJson() }.getOrNull()
            ?: return BrowseSearchState()
        if (!raw.isSafeBoundedJson(maximumPayloadBytes)) return BrowseSearchState()
        val envelope = try {
            json.decodeFromString<BrowseSearchStateEnvelope>(raw)
        } catch (_: SerializationException) {
            return BrowseSearchState()
        } catch (_: IllegalArgumentException) {
            return BrowseSearchState()
        }
        if (envelope.schemaVersion != SCHEMA_VERSION) return BrowseSearchState()
        val decodedState = BrowseSearchState(
            query = envelope.query,
            filters = envelope.filters,
        )
        if (!isValid(decodedState)) return BrowseSearchState()
        return decodedState.copy(filters = decodedState.filters.normalized())
    }

    override fun write(state: BrowseSearchState): Boolean {
        if (!isValid(state)) return false
        val normalized = state.copy(filters = state.filters.normalized())
        val encoded = runCatching {
            json.encodeToString(
                BrowseSearchStateEnvelope(
                    query = normalized.query,
                    filters = normalized.filters,
                ),
            )
        }.getOrNull() ?: return false
        if (!encoded.isSafeBoundedJson(maximumPayloadBytes)) return false
        return runCatching { persistence.writeStateJson(encoded) }.getOrDefault(false)
    }

    override fun clear(): Boolean =
        runCatching(persistence::clearState).getOrDefault(false)

    private fun isValid(state: BrowseSearchState): Boolean {
        val filters = state.filters
        val selections = listOfNotNull(
            filters.region,
            filters.city,
            filters.category,
            filters.language,
        )
        return state.query.length <= OpportunitySearchLimits.MAXIMUM_QUERY_LENGTH &&
            selections.all { it.length <= OpportunitySearchLimits.MAXIMUM_SELECTION_LENGTH } &&
            (filters.age == null || filters.age in 0..OpportunitySearchLimits.MAXIMUM_AGE) &&
            (filters.distanceKm == null || filters.distanceKm in DISTANCE_RANGE)
    }

    private companion object {
        const val SCHEMA_VERSION = 1
        const val MAXIMUM_PAYLOAD_BYTES = 32 * 1024
        val DISTANCE_RANGE = OpportunitySearchLimits.MINIMUM_DISTANCE_KM..
            OpportunitySearchLimits.MAXIMUM_DISTANCE_KM

        val DEFAULT_JSON = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            isLenient = false
            coerceInputValues = false
            encodeDefaults = true
        }
    }
}

class SharedPreferencesBrowseSearchStateStore private constructor(
    private val delegate: BrowseSearchStateStore,
) : BrowseSearchStateStore by delegate {
    constructor(context: Context) : this(
        PersistentBrowseSearchStateStore(
            SharedPreferencesBrowseSearchStatePersistence(
                context.applicationContext.getSharedPreferences(
                    PREFERENCES_NAME,
                    Context.MODE_PRIVATE,
                ),
            ),
        ),
    )

    private companion object {
        const val PREFERENCES_NAME = "gta_free_stem_browse_state"
    }
}

internal class SharedPreferencesBrowseSearchStatePersistence(
    private val preferences: SharedPreferences,
) : BrowseSearchStatePersistence {
    override fun readStateJson(): String? = synchronized(preferences) {
        preferences.getString(STATE_JSON_KEY, null)
    }

    override fun writeStateJson(json: String): Boolean = runCatching {
        synchronized(preferences) {
            preferences.edit().putString(STATE_JSON_KEY, json).apply()
        }
    }.isSuccess

    @SuppressLint("ApplySharedPref")
    override fun clearState(): Boolean = runCatching {
        synchronized(preferences) {
            preferences.edit().remove(STATE_JSON_KEY).commit()
        }
    }.getOrDefault(false)

    private companion object {
        const val STATE_JSON_KEY = "browse_search_state_v1"
    }
}

@Serializable
private data class BrowseSearchStateEnvelope(
    val schemaVersion: Int = 1,
    val query: String = "",
    val filters: OpportunitySearchFilters = OpportunitySearchFilters(),
)
