package com.rupayonhaldar.gtafreestem.data.local

import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSnapshot
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityFeedSource
import com.rupayonhaldar.gtafreestem.domain.validation.OpportunityAvailability
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal interface FavoriteOpportunityPersistence {
    fun readSnapshotJson(): String?
    fun readLegacyIds(): Set<String>
    fun write(snapshotJson: String, ids: Set<String>): Boolean
    fun clear(): Boolean
}

/**
 * Pure Kotlin implementation so migration, corruption handling, and reconciliation stay covered
 * by fast JVM tests rather than depending on Android framework behavior.
 */
internal class PersistentFavoriteOpportunityStore(
    private val persistence: FavoriteOpportunityPersistence,
    private val json: Json = DEFAULT_JSON,
    private val maximumPayloadBytes: Int = MAXIMUM_PAYLOAD_BYTES,
) : FavoriteOpportunityStore {
    private val lock = Any()
    private var state: SavedState

    init {
        require(maximumPayloadBytes > 0)
        val loaded = loadState()
        state = loaded.state
        if (loaded.shouldRewrite) {
            persist(state)
        }
    }

    override fun ids(): Set<String> = synchronized(lock) { state.ids() }

    override fun contains(id: String): Boolean {
        val normalized = normalizeId(id) ?: return false
        return synchronized(lock) { state.contains(normalized) }
    }

    override fun setFavorite(id: String, favorite: Boolean) {
        val normalized = normalizeId(id) ?: return
        synchronized(lock) {
            val updated = if (favorite) {
                if (state.contains(normalized)) return@synchronized
                state.copy(unresolvedIds = state.unresolvedIds + normalized)
            } else {
                if (!state.contains(normalized)) return@synchronized
                state.copy(
                    entriesById = state.entriesById - normalized,
                    unresolvedIds = state.unresolvedIds - normalized,
                )
            }
            commit(updated)
        }
    }

    override fun toggle(id: String): Boolean {
        val normalized = normalizeId(id) ?: return false
        return synchronized(lock) {
            val shouldSave = !state.contains(normalized)
            val updated = if (shouldSave) {
                state.copy(unresolvedIds = state.unresolvedIds + normalized)
            } else {
                state.copy(
                    entriesById = state.entriesById - normalized,
                    unresolvedIds = state.unresolvedIds - normalized,
                )
            }
            commit(updated)
            state.contains(normalized)
        }
    }

    override fun remove(id: String): Boolean {
        val normalized = normalizeId(id) ?: return false
        return synchronized(lock) {
            if (!state.contains(normalized)) return@synchronized true
            commit(
                state.copy(
                    entriesById = state.entriesById - normalized,
                    unresolvedIds = state.unresolvedIds - normalized,
                ),
            )
        }
    }

    override fun clearAll(): Boolean = synchronized(lock) {
        val cleared = runCatching(persistence::clear).getOrDefault(false)
        if (cleared) state = SavedState()
        cleared
    }

    override fun entries(): List<SavedOpportunityEntry> = synchronized(lock) {
        state.entriesById.values.sortedWith(ENTRY_ORDER)
    }

    override fun unresolvedIds(): Set<String> = synchronized(lock) { state.unresolvedIds.toSet() }

    override fun sections(now: Instant): SavedOpportunitySections = synchronized(lock) {
        val ordered = state.entriesById.values.sortedWith(ENTRY_ORDER)
        val (archive, current) = ordered.partition { entry ->
            OpportunityAvailability.isArchived(entry.opportunity, now)
        }
        SavedOpportunitySections(current = current, archive = archive)
    }

    override fun setFavorite(
        opportunity: Opportunity,
        favorite: Boolean,
        savedAt: Instant,
    ): Boolean {
        val normalized = normalizedOpportunity(opportunity) ?: return false
        return synchronized(lock) {
            val id = normalized.id
            val updated = if (favorite) {
                val entry = SavedOpportunityEntry(
                    opportunity = normalized,
                    savedAt = state.entriesById[id]?.savedAt ?: savedAt,
                )
                state.copy(
                    entriesById = state.entriesById + (id to entry),
                    unresolvedIds = state.unresolvedIds - id,
                )
            } else {
                state.copy(
                    entriesById = state.entriesById - id,
                    unresolvedIds = state.unresolvedIds - id,
                )
            }
            if (updated == state || commit(updated)) {
                state.contains(id) == favorite
            } else {
                false
            }
        }
    }

    override fun toggle(opportunity: Opportunity, savedAt: Instant): Boolean {
        val normalized = normalizedOpportunity(opportunity) ?: return false
        return synchronized(lock) {
            val id = normalized.id
            val shouldSave = !state.contains(id)
            val updated = if (shouldSave) {
                val entry = SavedOpportunityEntry(
                    opportunity = normalized,
                    savedAt = savedAt,
                )
                state.copy(
                    entriesById = state.entriesById + (id to entry),
                    unresolvedIds = state.unresolvedIds - id,
                )
            } else {
                state.copy(
                    entriesById = state.entriesById - id,
                    unresolvedIds = state.unresolvedIds - id,
                )
            }
            commit(updated)
            state.contains(id)
        }
    }

    override fun reconcile(
        snapshot: OpportunityFeedSnapshot,
        reconciledAt: Instant,
    ): SavedOpportunityReconciliation = synchronized(lock) {
        val latestById = buildMap {
            snapshot.opportunities.forEach { opportunity ->
                normalizedOpportunity(opportunity)?.let { normalized ->
                    put(normalized.id, normalized)
                }
            }
        }
        val completeFeedApplied = snapshot.permitsDestructiveSavedReconciliation()
        var refreshedCount = 0
        var hydratedCount = 0
        var markedUnavailableCount = 0
        var discardedUnresolvedCount = 0
        val reconciledEntries = state.entriesById.toMutableMap()
        val reconciledUnresolved = state.unresolvedIds.toMutableSet()

        state.entriesById.forEach { (id, savedEntry) ->
            val current = latestById[id]
            val replacement = when {
                current != null -> current
                completeFeedApplied && savedEntry.opportunity.status.isActiveStatus() -> {
                    markedUnavailableCount += 1
                    savedEntry.opportunity.copy(status = REMOVED_STATUS)
                }
                else -> null
            }
            if (replacement != null && replacement != savedEntry.opportunity) {
                reconciledEntries[id] = savedEntry.copy(opportunity = replacement)
                refreshedCount += 1
            }
        }

        state.unresolvedIds.forEach { id ->
            val current = latestById[id]
            when {
                current != null -> {
                    reconciledEntries[id] = SavedOpportunityEntry(current, reconciledAt)
                    reconciledUnresolved.remove(id)
                    hydratedCount += 1
                }
                completeFeedApplied -> {
                    reconciledUnresolved.remove(id)
                    discardedUnresolvedCount += 1
                }
            }
        }

        val updated = SavedState(
            entriesById = reconciledEntries,
            unresolvedIds = reconciledUnresolved,
        )
        val changed = updated != state
        val persisted = !changed || commit(updated)
        SavedOpportunityReconciliation(
            refreshedCount = refreshedCount,
            hydratedCount = hydratedCount,
            markedUnavailableCount = markedUnavailableCount,
            discardedUnresolvedCount = discardedUnresolvedCount,
            completeFeedApplied = completeFeedApplied,
            persisted = persisted,
        )
    }

    private fun loadState(): LoadedState {
        val legacyIds = runCatching { persistence.readLegacyIds() }
            .getOrDefault(emptySet())
            .asSequence()
            .mapNotNull(::normalizeId)
            .distinct()
            .sorted()
            .take(MAXIMUM_SAVED_ITEMS)
            .toSet()
        val rawSnapshot = runCatching { persistence.readSnapshotJson() }.getOrNull()
        val decoded = rawSnapshot?.let(::decodeEnvelope)
        val decodedState = decoded?.toState() ?: SavedState()
        val missingLegacyIds = legacyIds - decodedState.ids()
        return LoadedState(
            state = decodedState.copy(
                unresolvedIds = (decodedState.unresolvedIds + missingLegacyIds)
                    .take(MAXIMUM_SAVED_ITEMS - decodedState.entriesById.size)
                    .toSet(),
            ),
            shouldRewrite = rawSnapshot == null || decoded == null || missingLegacyIds.isNotEmpty(),
        )
    }

    private fun decodeEnvelope(raw: String): SavedOpportunityEnvelope? {
        if (!raw.isSafeBoundedJson(maximumPayloadBytes)) return null
        val envelope = try {
            json.decodeFromString<SavedOpportunityEnvelope>(raw)
        } catch (_: SerializationException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
        return envelope.takeIf { candidate ->
            candidate.schemaVersion == SCHEMA_VERSION &&
                candidate.records.size <= MAXIMUM_SAVED_ITEMS &&
                candidate.unresolvedIds.size <= MAXIMUM_SAVED_ITEMS
        }
    }

    private fun SavedOpportunityEnvelope.toState(): SavedState {
        val entries = linkedMapOf<String, SavedOpportunityEntry>()
        records.forEach { record ->
            if (entries.size >= MAXIMUM_SAVED_ITEMS) return@forEach
            val opportunity = normalizedOpportunity(record.opportunity) ?: return@forEach
            val savedAt = runCatching { Instant.parse(record.savedAt) }.getOrNull()
                ?: return@forEach
            entries.putIfAbsent(
                opportunity.id,
                SavedOpportunityEntry(opportunity = opportunity, savedAt = savedAt),
            )
        }
        val unresolved = unresolvedIds.asSequence()
            .mapNotNull(::normalizeId)
            .filterNot(entries::containsKey)
            .distinct()
            .take(MAXIMUM_SAVED_ITEMS - entries.size)
            .toSet()
        return SavedState(entriesById = entries, unresolvedIds = unresolved)
    }

    private fun commit(updated: SavedState): Boolean {
        if (updated == state || updated.ids().size > MAXIMUM_SAVED_ITEMS) return updated == state
        if (!persist(updated)) return false
        state = updated
        return true
    }

    private fun persist(candidate: SavedState): Boolean {
        val envelope = SavedOpportunityEnvelope(
            records = candidate.entriesById.values
                .sortedWith(ENTRY_ORDER)
                .map { entry ->
                    SavedOpportunityPayload(
                        opportunity = entry.opportunity,
                        savedAt = entry.savedAt.toString(),
                    )
                },
            unresolvedIds = candidate.unresolvedIds.sorted(),
        )
        val encoded = runCatching { json.encodeToString(envelope) }.getOrNull() ?: return false
        if (!encoded.isSafeBoundedJson(maximumPayloadBytes)) return false
        return runCatching { persistence.write(encoded, candidate.ids()) }.getOrDefault(false)
    }

    private fun normalizedOpportunity(opportunity: Opportunity): Opportunity? {
        val id = normalizeId(opportunity.id) ?: return null
        return if (id == opportunity.id) opportunity else opportunity.copy(id = id)
    }

    private fun OpportunityFeedSnapshot.permitsDestructiveSavedReconciliation(): Boolean {
        if (source != OpportunityFeedSource.PRIMARY_NETWORK &&
            source != OpportunityFeedSource.FALLBACK_NETWORK
        ) {
            return false
        }
        if (isStale || declaredRecordCount <= 0 || declaredRecordCount != opportunities.size) {
            return false
        }
        val normalizedIds = opportunities.mapNotNull { normalizeId(it.id) }
        return normalizedIds.size == opportunities.size &&
            normalizedIds.distinct().size == opportunities.size
    }

    private data class SavedState(
        val entriesById: Map<String, SavedOpportunityEntry> = emptyMap(),
        val unresolvedIds: Set<String> = emptySet(),
    ) {
        fun ids(): Set<String> = entriesById.keys + unresolvedIds
        fun contains(id: String): Boolean = id in entriesById || id in unresolvedIds
    }

    private data class LoadedState(
        val state: SavedState,
        val shouldRewrite: Boolean,
    )

    private companion object {
        const val SCHEMA_VERSION = 1
        const val MAXIMUM_ID_LENGTH = 256
        const val MAXIMUM_SAVED_ITEMS = 5_000
        const val MAXIMUM_PAYLOAD_BYTES = 4 * 1024 * 1024
        const val REMOVED_STATUS = "removed"

        val DEFAULT_JSON = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            isLenient = false
            coerceInputValues = false
            encodeDefaults = true
        }

        val ENTRY_ORDER = compareByDescending<SavedOpportunityEntry> { it.savedAt }
            .thenBy { it.opportunity.id }

        fun normalizeId(raw: String): String? = raw.trim()
            .takeIf { it.length in 1..MAXIMUM_ID_LENGTH }

        fun String.isActiveStatus(): Boolean = trim().equals("active", ignoreCase = true)

    }
}

@Serializable
private data class SavedOpportunityEnvelope(
    val schemaVersion: Int = 1,
    val records: List<SavedOpportunityPayload> = emptyList(),
    val unresolvedIds: List<String> = emptyList(),
)

@Serializable
private data class SavedOpportunityPayload(
    val opportunity: Opportunity,
    val savedAt: String,
)
