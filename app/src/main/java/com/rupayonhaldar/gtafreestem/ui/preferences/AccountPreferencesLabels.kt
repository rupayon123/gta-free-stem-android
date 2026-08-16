package com.rupayonhaldar.gtafreestem.ui.preferences

import com.rupayonhaldar.gtafreestem.data.local.AppThemePreference
import com.rupayonhaldar.gtafreestem.data.local.LocalAccountDataDeletionResult
import com.rupayonhaldar.gtafreestem.localization.AppLanguage

/**
 * Catalog-backed copy for the local preferences screen.
 *
 * The shared Apple/Android catalog remains the source of truth for existing product language.
 * New explanatory copy falls back to plain English until community translations are added.
 */
internal class AccountPreferencesLabels(
    private val state: AppPreferencesUiState,
) {
    val screenTitle = text("settings", "Settings")
    val screenSummary = text(
        "preferencesLocalSummary",
        "Personalize this device without signing in.",
    )

    val profileTitle = text("account", "Profile")
    val profileOnDevice = text(
        "profileOnDevice",
        "This profile and its saved opportunities stay on this device.",
    )
    val guest = text("guest", "Guest")
    val displayName = text("name", "Name")
    val displayNameHelp = text(
        "displayNameHelp",
        "Optional. Saved only on this device; no sign-in or cloud account.",
    )
    val displayNameInvalid = text(
        "displayNameInvalid",
        "Enter a name from 1 to 80 characters without control characters.",
    )
    val save = text("save", "Save")
    val profileSaved = text("profileSaved", "Profile saved on this device.")
    val clearProfile = text("signOut", "Clear profile")
    val clearProfileTitle = text("clearProfileTitle", "Clear profile?")
    val clearProfileExplanation = text(
        "clearProfileExplanation",
        "This removes only the optional display name. Saved opportunities and settings stay on this device.",
    )
    val profileCleared = text("profileCleared", "Profile cleared from this device.")

    val savedTitle = text("saved", "Saved")
    val savedExplanation = text(
        "savedArchiveNote",
        "Expired saved listings stay available in your archive so you can remember what you found.",
    )
    val openSavedLibrary = text("openSavedLibrary", "Open saved opportunities")

    val languageTitle = text("siteLanguage", "App language")
    val chooseLanguage = text("chooseLanguage", "Choose language")
    val system = text("system", "System")
    val followsDeviceLanguage = text(
        "followsDeviceLanguage",
        "Follows your device language",
    )
    val done = text("done", "Done")

    val themeTitle = text("theme", "Theme")
    val systemTheme = text("system", "System")
    val lightTheme = text("light", "Light")
    val darkTheme = text("dark", "Dark")

    val alertsTitle = text("alerts", "Alerts")
    val alertsPreference = text(
        "alertsPreference",
        "Remember that I want opportunity alerts",
    )
    val alertsNotActive = text(
        "alertsNotActive",
        "Notifications are not active yet. This only remembers your preference on this device.",
    )

    val helpAndLegalTitle = text("helpAndLegal", "Help and legal")
    val support = text("support", "Support")
    val privacyPolicy = text("privacyPolicy", "Privacy policy")
    val terms = text("termsTitle", "Terms of Service and Privacy Notice")
    val legalExplanation = text(
        "termsBody",
        "Listings link to public provider pages. No ads, paid ranking, direct messaging, or tutoring marketplace features are included.",
    )

    val localDataTitle = text("localDataTitle", "Local data")
    val localDataExplanation = text(
        "localDataExplanation",
        "Your profile, search history, saved opportunities, language, theme, and alerts preference stay on this device. This action removes the profile, search history, and saved opportunities; app preferences remain.",
    )
    val deleteAllLocalData = text("deleteLocalData", "Delete all local data")
    val deleteProfileAndLocalData = text(
        "deleteAccount",
        "Delete profile and saved opportunities",
    )
    val deleteConfirmationTitle = text(
        "deleteAccountConfirmation",
        "Delete profile, saved opportunities, and search history?",
    )
    val deleteConfirmationExplanation = text(
        "deleteConfirmationExplanation",
        "This permanently removes the optional profile, search history, and saved opportunities from this device. This cannot be undone.",
    )

    val saveFailed = text(
        "localSaveFailed",
        "That change could not be saved on this device. Try again.",
    )
    val cancel = text("cancel", "Cancel")

    fun theme(theme: AppThemePreference): String = when (theme) {
        AppThemePreference.SYSTEM -> systemTheme
        AppThemePreference.LIGHT -> lightTheme
        AppThemePreference.DARK -> darkTheme
    }

    fun language(option: AppLanguageOption): String = when {
        option.nativeName.equals(option.englishName, ignoreCase = true) -> option.nativeName
        else -> "${option.nativeName} · ${option.englishName}"
    }

    fun deletionSuccess(hadProfile: Boolean): String = if (hadProfile) {
        text(
            "accountDeleted",
            "Profile, saved opportunities, and search history deleted.",
        )
    } else {
        text(
            "localDataDeleted",
            "Saved opportunities and search history deleted.",
        )
    }

    fun deletionFailure(result: LocalAccountDataDeletionResult): String {
        val remaining = buildList {
            if (!result.profileDeleted) add("profile")
            if (!result.searchHistoryDeleted) add("search history")
            if (!result.savedOpportunitiesDeleted) add("saved opportunities")
        }
        return text(
            "localDeleteIncomplete",
            "Some local data could not be deleted (${remaining.joinToString()}). Try again.",
        )
    }

    private fun text(
        key: String,
        englishFallback: String,
        placeholders: Map<String, String> = emptyMap(),
    ): String = state.text(key, placeholders)
        .trim()
        .takeUnless { value -> value.isEmpty() || value == key }
        ?: englishFallback
}

internal data class AccountLanguageChoice(
    val language: AppLanguage?,
    val title: String,
    val supportingText: String? = null,
)

internal fun accountLanguageChoices(
    state: AppPreferencesUiState,
    labels: AccountPreferencesLabels = AccountPreferencesLabels(state),
): List<AccountLanguageChoice> = buildList {
    val resolvedOption = state.languageOptions.firstOrNull { option ->
        option.language == state.resolvedLanguage
    }
    add(
        AccountLanguageChoice(
            language = null,
            title = labels.system,
            supportingText = resolvedOption?.let { option ->
                "${labels.followsDeviceLanguage}: ${labels.language(option)}"
            } ?: labels.followsDeviceLanguage,
        ),
    )
    state.languageOptions.forEach { option ->
        add(
            AccountLanguageChoice(
                language = option.language,
                title = labels.language(option),
            ),
        )
    }
}
