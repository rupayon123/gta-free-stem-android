package com.rupayonhaldar.gtafreestem.data.local

import android.content.Context
import android.content.SharedPreferences
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchFilters
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchLimits
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchSort
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A named search with the last verified total and stable-ID update baseline. */
@Serializable
data class SavedSearchEntry(
    val id: String,
    val name: String,
    val query: String,
    val filters: OpportunitySearchFilters,
    val knownMatchingIds: List<String>,
    val unreadMatchingIds: List<String> = emptyList(),
    val hasBaseline: Boolean = false,
    val lastProcessedRevisionEpochMillis: Long? = null,
    /** Last total from an accepted complete publication; retained through offline failures. */
    val lastKnownMatchCount: Int? = null,
)

data class SavedSearchSummary(
    val entry: SavedSearchEntry,
    val matchCount: Int? = entry.lastKnownMatchCount,
    val newCount: Int = 0,
)

interface SavedSearchStore {
    fun read(): List<SavedSearchEntry>
    fun write(entries: List<SavedSearchEntry>): Boolean
    fun clear(): Boolean

    companion object {
        val NONE = object : SavedSearchStore {
            override fun read() = emptyList<SavedSearchEntry>()
            override fun write(entries: List<SavedSearchEntry>) = true
            override fun clear() = true
        }
    }
}

internal interface SavedSearchPersistence {
    fun readJson(): String?
    fun writeJson(value: String): Boolean
    fun clear(): Boolean
}

internal class PersistentSavedSearchStore(
    private val persistence: SavedSearchPersistence,
) : SavedSearchStore {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    override fun read(): List<SavedSearchEntry> = runCatching {
        val raw = persistence.readJson() ?: return emptyList()
        if (raw.toByteArray(Charsets.UTF_8).size > MAX_BYTES) return emptyList()
        val envelope = json.decodeFromString<SavedSearchEnvelope>(raw)
        if (envelope.schemaVersion != SCHEMA_VERSION) return emptyList()
        envelope.entries.takeIf(::valid)?.map { entry ->
            entry.copy(filters = entry.filters.normalized())
        } ?: emptyList()
    }.getOrDefault(emptyList())

    override fun write(entries: List<SavedSearchEntry>): Boolean {
        if (!valid(entries)) return false
        val normalized = entries.map { it.copy(filters = it.filters.normalized()) }
        val encoded = runCatching {
            json.encodeToString(SavedSearchEnvelope(entries = normalized))
        }.getOrNull() ?: return false
        if (encoded.toByteArray(Charsets.UTF_8).size > MAX_BYTES) return false
        return runCatching { persistence.writeJson(encoded) }.getOrDefault(false)
    }

    override fun clear(): Boolean = runCatching(persistence::clear).getOrDefault(false)

    private fun valid(entries: List<SavedSearchEntry>): Boolean =
        entries.size <= MAX_SEARCHES &&
            entries.map(SavedSearchEntry::id).distinct().size == entries.size &&
            entries.all { entry ->
                entry.id.length in 1..80 && entry.name.trim().length in 1..60 &&
                    entry.query.length <= OpportunitySearchLimits.MAXIMUM_QUERY_LENGTH &&
                    entry.filters.latitude == null && entry.filters.longitude == null &&
                    entry.filters.distanceKm == null && entry.filters.sort != OpportunitySearchSort.NEAREST &&
                    entry.filters.age?.let { it in 0..OpportunitySearchLimits.MAXIMUM_AGE } != false &&
                    listOfNotNull(entry.filters.region, entry.filters.city, entry.filters.category)
                        .all { it.length <= OpportunitySearchLimits.MAXIMUM_SELECTION_LENGTH } &&
                    entry.knownMatchingIds.validIds() && entry.unreadMatchingIds.validIds() &&
                    entry.unreadMatchingIds.all(entry.knownMatchingIds.toSet()::contains) &&
                    entry.lastKnownMatchCount?.let { it in 0..MAX_MATCH_IDS } != false
            }

    private fun List<String>.validIds() = size <= MAX_MATCH_IDS &&
        distinct().size == size && all { it.isNotBlank() && it.length <= 256 }

    companion object {
        const val MAX_SEARCHES = 16
        private const val MAX_MATCH_IDS = 20_000
        private const val MAX_BYTES = 4 * 1024 * 1024
        private const val SCHEMA_VERSION = 1
    }
}

class SharedPreferencesSavedSearchStore(context: Context) : SavedSearchStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        "gta_free_stem_saved_searches", Context.MODE_PRIVATE,
    )
    private val delegate = PersistentSavedSearchStore(object : SavedSearchPersistence {
        override fun readJson(): String? = preferences.getString("saved_searches_v1", null)
        override fun writeJson(value: String): Boolean =
            preferences.edit().putString("saved_searches_v1", value).commit()
        override fun clear(): Boolean = preferences.edit().remove("saved_searches_v1").commit()
    })

    override fun read() = delegate.read()
    override fun write(entries: List<SavedSearchEntry>) = delegate.write(entries)
    override fun clear() = delegate.clear()
}

@Serializable
private data class SavedSearchEnvelope(
    val schemaVersion: Int = 1,
    val entries: List<SavedSearchEntry> = emptyList(),
)
