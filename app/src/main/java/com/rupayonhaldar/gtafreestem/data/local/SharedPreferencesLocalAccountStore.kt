package com.rupayonhaldar.gtafreestem.data.local

import android.content.Context
import android.content.SharedPreferences

/** Android adapter for the local-only account preferences policy. */
class SharedPreferencesLocalAccountStore private constructor(
    private val delegate: LocalAccountPreferencesStore,
) : LocalAccountPreferencesStore by delegate {
    constructor(context: Context) : this(
        PersistentLocalAccountPreferencesStore(
            SharedPreferencesLocalAccountPersistence(
                context.applicationContext.getSharedPreferences(
                    PREFERENCES_NAME,
                    Context.MODE_PRIVATE,
                ),
            ),
        ),
    )

    private companion object {
        const val PREFERENCES_NAME = "gta_free_stem_local_account"
    }
}

internal class SharedPreferencesLocalAccountPersistence(
    private val preferences: SharedPreferences,
) : LocalAccountPreferencesPersistence {
    override fun readDisplayName(): String? = synchronized(preferences) {
        preferences.getString(DISPLAY_NAME_KEY, null)
    }

    override fun readTheme(): String? = synchronized(preferences) {
        preferences.getString(THEME_KEY, null)
    }

    override fun readOpportunityAlertsPreferred(): Boolean? = synchronized(preferences) {
        if (preferences.contains(ALERTS_PREFERRED_KEY)) {
            preferences.getBoolean(ALERTS_PREFERRED_KEY, false)
        } else {
            null
        }
    }

    override fun writeDisplayName(displayName: String?): Boolean = edit { editor ->
        if (displayName == null) {
            editor.remove(DISPLAY_NAME_KEY)
        } else {
            editor.putString(DISPLAY_NAME_KEY, displayName)
        }
    }

    override fun writeTheme(theme: String): Boolean = edit { editor ->
        editor.putString(THEME_KEY, theme)
    }

    override fun writeOpportunityAlertsPreferred(preferred: Boolean): Boolean = edit { editor ->
        editor.putBoolean(ALERTS_PREFERRED_KEY, preferred)
    }

    private fun edit(update: (SharedPreferences.Editor) -> SharedPreferences.Editor): Boolean =
        runCatching {
            synchronized(preferences) {
                update(preferences.edit()).commit()
            }
        }.getOrDefault(false)

    private companion object {
        const val DISPLAY_NAME_KEY = "display_name"
        const val THEME_KEY = "theme"
        const val ALERTS_PREFERRED_KEY = "opportunity_alerts_preferred"
    }
}
