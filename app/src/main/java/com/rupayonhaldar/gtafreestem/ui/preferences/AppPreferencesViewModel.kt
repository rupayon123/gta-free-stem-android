package com.rupayonhaldar.gtafreestem.ui.preferences

import android.content.Context
import android.os.LocaleList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rupayonhaldar.gtafreestem.data.local.AppThemePreference
import com.rupayonhaldar.gtafreestem.data.local.DisplayNameSaveResult
import com.rupayonhaldar.gtafreestem.data.local.LocalAccountPreferences
import com.rupayonhaldar.gtafreestem.data.local.LocalAccountPreferencesStore
import com.rupayonhaldar.gtafreestem.data.local.SharedPreferencesLocalAccountStore
import com.rupayonhaldar.gtafreestem.localization.AndroidAppStringCatalogLoader
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog
import com.rupayonhaldar.gtafreestem.localization.LanguagePreferenceStore
import com.rupayonhaldar.gtafreestem.localization.SharedPreferencesLanguagePreferenceStore
import com.rupayonhaldar.gtafreestem.localization.TextDirection
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

data class AppLanguageOption(
    val language: AppLanguage,
    val englishName: String,
    val nativeName: String,
    val direction: TextDirection,
)

/** Failure feedback emitted after a user explicitly tries to enable local opportunity alerts. */
enum class OpportunityAlertFeedback(
    val catalogKey: String,
    val englishFallback: String,
) {
    PERMISSION_DENIED(
        catalogKey = "alertsPermissionDenied",
        englishFallback =
            "Alerts stay off because notification permission wasn't allowed. You can try again anytime.",
    ),
    UNAVAILABLE(
        catalogKey = "alertsUnavailable",
        englishFallback =
            "Alerts couldn't be turned on. Check notification settings and try again.",
    ),
    SAVE_FAILED(
        catalogKey = "localSaveFailed",
        englishFallback = "That change could not be saved on this device. Try again.",
    ),
}

/** Immutable preferences state for a later lifecycle-aware Compose collector. */
data class AppPreferencesUiState(
    val selectedLanguage: AppLanguage?,
    val resolvedLanguage: AppLanguage,
    val languageOptions: List<AppLanguageOption>,
    val textDirection: TextDirection,
    val displayName: String?,
    val theme: AppThemePreference,
    val opportunityAlertsPreferred: Boolean,
    val catalog: AppStringCatalog,
) {
    val followsSystemLanguage: Boolean
        get() = selectedLanguage == null

    val isRightToLeft: Boolean
        get() = textDirection == TextDirection.RIGHT_TO_LEFT

    fun text(
        key: String,
        placeholders: Map<String, String> = emptyMap(),
    ): String = catalog.text(key, resolvedLanguage, placeholders)
}

/**
 * Owns local language, profile, and appearance state across configuration changes.
 * It performs no sign-in, network, location, notification-permission, or scheduling work; the
 * Activity coordinates Android permission and WorkManager around this observable preference.
 */
class AppPreferencesViewModel internal constructor(
    private val catalog: AppStringCatalog,
    private val languagePreferences: LanguagePreferenceStore,
    private val accountPreferences: LocalAccountPreferencesStore,
    private val systemLanguageTags: () -> Iterable<String>,
) : ViewModel() {
    private val updateLock = Any()
    private val languageOptions = AppLanguage.entries.map { language ->
        val metadata = catalog.metadata(language)
        AppLanguageOption(
            language = language,
            englishName = metadata.englishName,
            nativeName = metadata.nativeName,
            direction = metadata.direction,
        )
    }
    private val _uiState = MutableStateFlow(readState())
    val uiState: StateFlow<AppPreferencesUiState> = _uiState.asStateFlow()
    private val alertFeedbackChannel = Channel<OpportunityAlertFeedback>(Channel.BUFFERED)

    /**
     * A buffered, single-consumption stream. A permission callback can enqueue feedback while an
     * Activity is recreating, but a message that the UI already consumed is never replayed.
     */
    val opportunityAlertFeedback: Flow<OpportunityAlertFeedback> =
        alertFeedbackChannel.receiveAsFlow()

    /** Reloads local preferences and re-resolves a system-selected language. */
    fun reload() {
        synchronized(updateLock) {
            _uiState.value = readState()
        }
    }

    /** Pass null to follow the device language instead of forcing a language. */
    fun setLanguage(language: AppLanguage?): Boolean = synchronized(updateLock) {
        val saved = runCatching { languagePreferences.setSelectedLanguage(language) }
            .getOrDefault(false)
        if (saved) _uiState.value = readState()
        saved
    }

    fun useSystemLanguage(): Boolean = setLanguage(null)

    /** Call after an Android locale/configuration change while System is selected. */
    fun refreshSystemLanguage() = reload()

    fun saveDisplayName(displayName: String): DisplayNameSaveResult = synchronized(updateLock) {
        val result = runCatching { accountPreferences.saveDisplayName(displayName) }
            .getOrDefault(DisplayNameSaveResult.STORAGE_ERROR)
        if (result == DisplayNameSaveResult.SAVED) _uiState.value = readState()
        result
    }

    fun clearProfile(): Boolean = updateAccountPreference(accountPreferences::clearProfile)

    fun setTheme(theme: AppThemePreference): Boolean =
        updateAccountPreference { accountPreferences.setTheme(theme) }

    /** Persists the state after the Activity has applied permission and scheduling policy. */
    fun setOpportunityAlertsPreferred(preferred: Boolean): Boolean =
        updateAccountPreference {
            accountPreferences.setOpportunityAlertsPreferred(preferred)
        }

    internal fun reportOpportunityAlertFeedback(feedback: OpportunityAlertFeedback) {
        alertFeedbackChannel.trySend(feedback)
    }

    private fun updateAccountPreference(update: () -> Boolean): Boolean = synchronized(updateLock) {
        val saved = runCatching(update).getOrDefault(false)
        if (saved) _uiState.value = readState()
        saved
    }

    private fun readState(): AppPreferencesUiState {
        val selectedLanguage = runCatching(languagePreferences::selectedLanguage).getOrNull()
        val preferredSystemTags = runCatching(systemLanguageTags).getOrDefault(emptyList())
        val resolvedLanguage = selectedLanguage
            ?: AppLanguage.bestMatchOrEnglish(preferredSystemTags)
        val account = runCatching(accountPreferences::currentPreferences)
            .getOrDefault(LocalAccountPreferences())

        return AppPreferencesUiState(
            selectedLanguage = selectedLanguage,
            resolvedLanguage = resolvedLanguage,
            languageOptions = languageOptions,
            textDirection = catalog.metadata(resolvedLanguage).direction,
            displayName = account.displayName,
            theme = account.theme,
            opportunityAlertsPreferred = account.opportunityAlertsPreferred,
            catalog = catalog,
        )
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(AppPreferencesViewModel::class.java))
                    return AppPreferencesViewModel(
                        catalog = AndroidAppStringCatalogLoader.load(appContext),
                        languagePreferences = SharedPreferencesLanguagePreferenceStore(appContext),
                        accountPreferences = SharedPreferencesLocalAccountStore(appContext),
                        systemLanguageTags = {
                            LocaleList.getDefault()
                                .toLanguageTags()
                                .split(',')
                                .filter(String::isNotBlank)
                        },
                    ) as T
                }
            }
        }
    }
}
