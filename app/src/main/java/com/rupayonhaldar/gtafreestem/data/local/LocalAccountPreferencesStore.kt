package com.rupayonhaldar.gtafreestem.data.local

import java.nio.charset.StandardCharsets
import java.util.Locale

/** Device appearance choice. System follows the current Android setting. */
enum class AppThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    internal val storedValue: String
        get() = name.lowercase(Locale.ROOT)

    internal companion object {
        fun fromStoredValue(value: String?): AppThemePreference = entries.firstOrNull { theme ->
            theme.storedValue == value?.trim()?.lowercase(Locale.ROOT)
        } ?: SYSTEM
    }
}

/** Local-only profile and settings. This is not a signed-in or cloud account. */
data class LocalAccountPreferences(
    val displayName: String? = null,
    val theme: AppThemePreference = AppThemePreference.SYSTEM,
    val opportunityAlertsPreferred: Boolean = false,
) {
    val hasProfile: Boolean
        get() = displayName != null
}

enum class DisplayNameSaveResult {
    SAVED,
    INVALID,
    STORAGE_ERROR,
}

/** Stores the optional local profile and device settings without requesting permissions. */
interface LocalAccountPreferencesStore {
    fun currentPreferences(): LocalAccountPreferences

    /** Saves a non-blank display name after trimming and bounding it. */
    fun saveDisplayName(displayName: String): DisplayNameSaveResult

    fun setTheme(theme: AppThemePreference): Boolean

    /** Records intent only. Notification permission and scheduling belong to a later UI flow. */
    fun setOpportunityAlertsPreferred(preferred: Boolean): Boolean

    /** Removes only the optional display name; device-level settings remain unchanged. */
    fun clearProfile(): Boolean
}

internal interface LocalAccountPreferencesPersistence {
    fun readDisplayName(): String?
    fun readTheme(): String?
    fun readOpportunityAlertsPreferred(): Boolean?
    fun writeDisplayName(displayName: String?): Boolean
    fun writeTheme(theme: String): Boolean
    fun writeOpportunityAlertsPreferred(preferred: Boolean): Boolean
}

/** Pure Kotlin policy layer, kept separate from SharedPreferences for fast JVM tests. */
internal class PersistentLocalAccountPreferencesStore(
    private val persistence: LocalAccountPreferencesPersistence,
) : LocalAccountPreferencesStore {
    private val lock = Any()
    private var state = LocalAccountPreferences(
        displayName = runCatching { persistence.readDisplayName() }
            .getOrNull()
            ?.let(LocalDisplayNamePolicy::normalize),
        theme = AppThemePreference.fromStoredValue(
            runCatching { persistence.readTheme() }.getOrNull(),
        ),
        opportunityAlertsPreferred = runCatching {
            persistence.readOpportunityAlertsPreferred()
        }.getOrNull() ?: false,
    )

    override fun currentPreferences(): LocalAccountPreferences = synchronized(lock) { state }

    override fun saveDisplayName(displayName: String): DisplayNameSaveResult {
        val normalized = LocalDisplayNamePolicy.normalize(displayName)
            ?: return DisplayNameSaveResult.INVALID
        return synchronized(lock) {
            val saved = runCatching { persistence.writeDisplayName(normalized) }
                .getOrDefault(false)
            if (!saved) return@synchronized DisplayNameSaveResult.STORAGE_ERROR
            state = state.copy(displayName = normalized)
            DisplayNameSaveResult.SAVED
        }
    }

    override fun setTheme(theme: AppThemePreference): Boolean = synchronized(lock) {
        val saved = runCatching { persistence.writeTheme(theme.storedValue) }.getOrDefault(false)
        if (saved) state = state.copy(theme = theme)
        saved
    }

    override fun setOpportunityAlertsPreferred(preferred: Boolean): Boolean = synchronized(lock) {
        val saved = runCatching {
            persistence.writeOpportunityAlertsPreferred(preferred)
        }.getOrDefault(false)
        if (saved) state = state.copy(opportunityAlertsPreferred = preferred)
        saved
    }

    override fun clearProfile(): Boolean = synchronized(lock) {
        val cleared = runCatching { persistence.writeDisplayName(null) }.getOrDefault(false)
        if (cleared) state = state.copy(displayName = null)
        cleared
    }
}

internal object LocalDisplayNamePolicy {
    const val MAXIMUM_CODE_POINTS = 80
    const val MAXIMUM_UTF8_BYTES = MAXIMUM_CODE_POINTS * 4

    fun normalize(candidate: String): String? {
        val normalized = collapseWhitespace(candidate) ?: return null
        if (normalized.codePointCount(0, normalized.length) > MAXIMUM_CODE_POINTS) return null
        if (normalized.toByteArray(StandardCharsets.UTF_8).size > MAXIMUM_UTF8_BYTES) return null
        return normalized
    }

    private fun collapseWhitespace(candidate: String): String? {
        val output = StringBuilder(candidate.length.coerceAtMost(MAXIMUM_UTF8_BYTES))
        var pendingSpace = false
        candidate.forEach { character ->
            when {
                character.isWhitespace() -> pendingSpace = output.isNotEmpty()
                character.isISOControl() -> return null
                else -> {
                    if (pendingSpace) output.append(' ')
                    output.append(character)
                    pendingSpace = false
                }
            }
        }
        return output.toString().takeIf(String::isNotEmpty)
    }
}
