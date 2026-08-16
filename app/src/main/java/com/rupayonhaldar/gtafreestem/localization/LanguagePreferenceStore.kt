package com.rupayonhaldar.gtafreestem.localization

import android.content.Context
import android.content.SharedPreferences
import android.os.LocaleList

/** Local-only language selection. A null selection means follow the device language. */
interface LanguagePreferenceStore {
    fun selectedLanguage(): AppLanguage?

    fun resolvedLanguage(preferredSystemTags: Iterable<String>): AppLanguage =
        selectedLanguage() ?: AppLanguage.bestMatchOrEnglish(preferredSystemTags)

    /** Pass null to return to the system-language setting. */
    fun setSelectedLanguage(language: AppLanguage?): Boolean
}

internal interface LanguagePreferencePersistence {
    fun readLanguageTag(): String?
    fun writeLanguageTag(languageTag: String?): Boolean
}

internal class PersistentLanguagePreferenceStore(
    private val persistence: LanguagePreferencePersistence,
) : LanguagePreferenceStore {
    override fun selectedLanguage(): AppLanguage? =
        AppLanguage.matching(persistence.readLanguageTag())

    override fun setSelectedLanguage(language: AppLanguage?): Boolean =
        persistence.writeLanguageTag(language?.catalogCode)
}

/** Android adapter around the JVM-testable preference policy. */
class SharedPreferencesLanguagePreferenceStore private constructor(
    private val delegate: LanguagePreferenceStore,
) : LanguagePreferenceStore by delegate {
    constructor(context: Context) : this(
        PersistentLanguagePreferenceStore(
            SharedPreferencesLanguagePreferencePersistence(
                context.applicationContext.getSharedPreferences(
                    PREFERENCES_NAME,
                    Context.MODE_PRIVATE,
                ),
            ),
        ),
    )

    fun resolvedLanguageForDevice(): AppLanguage = resolvedLanguage(
        LocaleList.getDefault().toLanguageTags().split(',').filter(String::isNotBlank),
    )

    private companion object {
        const val PREFERENCES_NAME = "gta_free_stem_language"
    }
}

internal class SharedPreferencesLanguagePreferencePersistence(
    private val preferences: SharedPreferences,
) : LanguagePreferencePersistence {
    override fun readLanguageTag(): String? = synchronized(preferences) {
        preferences.getString(SELECTED_LANGUAGE_KEY, null)
    }

    override fun writeLanguageTag(languageTag: String?): Boolean = runCatching {
        synchronized(preferences) {
            preferences.edit().let { editor ->
                if (languageTag == null) {
                    editor.remove(SELECTED_LANGUAGE_KEY)
                } else {
                    editor.putString(SELECTED_LANGUAGE_KEY, languageTag)
                }.commit()
            }
        }
    }.getOrDefault(false)

    private companion object {
        const val SELECTED_LANGUAGE_KEY = "selected_language_code"
    }
}
