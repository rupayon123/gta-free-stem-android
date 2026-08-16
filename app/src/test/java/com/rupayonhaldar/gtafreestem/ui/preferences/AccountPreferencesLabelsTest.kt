package com.rupayonhaldar.gtafreestem.ui.preferences

import com.rupayonhaldar.gtafreestem.data.local.AppThemePreference
import com.rupayonhaldar.gtafreestem.data.local.LocalAccountDataDeletionResult
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog
import com.rupayonhaldar.gtafreestem.localization.TextDirection
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountPreferencesLabelsTest {
    @Test
    fun `catalog labels localize and new copy safely falls back to English`() {
        val labels = AccountPreferencesLabels(state(AppLanguage.SPANISH))

        assertEquals("Ajustes", labels.screenTitle)
        assertEquals("Perfil", labels.profileTitle)
        assertEquals("Tema", labels.themeTitle)
        assertEquals("Oscuro", labels.darkTheme)
        assertEquals("Soporte", labels.support)
        assertEquals(
            "Notifications are not active yet. This only remembers your preference on this device.",
            labels.alertsNotActive,
        )
    }

    @Test
    fun `language choices contain System followed by every supported catalog language`() {
        val state = state(AppLanguage.ARABIC, selectedLanguage = null)
        val choices = accountLanguageChoices(state)

        assertEquals(19, choices.size)
        assertEquals(null, choices.first().language)
        assertEquals("النظام", choices.first().title)
        assertTrue(choices.first().supportingText.orEmpty().contains("العربية"))
        assertEquals(AppLanguage.entries.toList(), choices.drop(1).map { it.language })
        assertTrue(choices.any { choice -> choice.title.contains("Français") })
    }

    @Test
    fun `theme labels and partial deletion feedback are explicit`() {
        val labels = AccountPreferencesLabels(state(AppLanguage.ENGLISH))

        assertEquals("System", labels.theme(AppThemePreference.SYSTEM))
        assertEquals("Light", labels.theme(AppThemePreference.LIGHT))
        assertEquals("Dark", labels.theme(AppThemePreference.DARK))
        assertEquals(
            "Some local data could not be deleted (search history, saved opportunities). Try again.",
            labels.deletionFailure(
                LocalAccountDataDeletionResult(
                    profileDeleted = true,
                    searchHistoryDeleted = false,
                    savedOpportunitiesDeleted = false,
                ),
            ),
        )
    }

    private fun state(
        language: AppLanguage,
        selectedLanguage: AppLanguage? = language,
    ): AppPreferencesUiState {
        val catalog = bundledCatalog()
        return AppPreferencesUiState(
            selectedLanguage = selectedLanguage,
            resolvedLanguage = language,
            languageOptions = AppLanguage.entries.map { option ->
                val metadata = catalog.metadata(option)
                AppLanguageOption(
                    language = option,
                    englishName = metadata.englishName,
                    nativeName = metadata.nativeName,
                    direction = metadata.direction,
                )
            },
            textDirection = catalog.metadata(language).direction,
            displayName = null,
            theme = AppThemePreference.SYSTEM,
            opportunityAlertsPreferred = false,
            catalog = catalog,
        )
    }

    private fun bundledCatalog(): AppStringCatalog {
        val file = sequenceOf(
            File("app/src/main/res/raw/app_strings.json"),
            File("src/main/res/raw/app_strings.json"),
        ).firstOrNull(File::isFile)
        checkNotNull(file) { "Bundled app string catalog is missing" }
        return AppStringCatalog.decode(file.readText())
    }
}
