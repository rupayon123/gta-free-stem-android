package com.rupayonhaldar.gtafreestem

import androidx.compose.ui.unit.LayoutDirection
import com.rupayonhaldar.gtafreestem.data.local.AppThemePreference
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog
import com.rupayonhaldar.gtafreestem.localization.TextDirection
import com.rupayonhaldar.gtafreestem.ui.preferences.AppPreferencesUiState
import com.rupayonhaldar.gtafreestem.ui.shell.PrimaryDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityPreferencesTest {
    @Test
    fun themePreferenceResolvesSystemLightAndDark() {
        assertFalse(AppThemePreference.SYSTEM.resolveDarkTheme(systemDarkTheme = false))
        assertTrue(AppThemePreference.SYSTEM.resolveDarkTheme(systemDarkTheme = true))
        assertFalse(AppThemePreference.LIGHT.resolveDarkTheme(systemDarkTheme = true))
        assertTrue(AppThemePreference.DARK.resolveDarkTheme(systemDarkTheme = false))
    }

    @Test
    fun catalogDirectionMapsToComposeLayoutDirection() {
        assertEquals(LayoutDirection.Ltr, TextDirection.LEFT_TO_RIGHT.toLayoutDirection())
        assertEquals(LayoutDirection.Rtl, TextDirection.RIGHT_TO_LEFT.toLayoutDirection())
    }

    @Test
    fun fiveNavigationLabelsUseCatalogWithFallbackAndStableEnglishAccount() {
        val french = preferenceState(AppLanguage.FRENCH)
        assertEquals(
            listOf("Accueil", "Occasions", "Secondaire", "Soutien", "Profil"),
            PrimaryDestination.entries.map(french::navigationLabel),
        )

        val english = preferenceState(AppLanguage.ENGLISH)
        assertEquals("Account", english.navigationLabel(PrimaryDestination.ACCOUNT))
        assertEquals(
            "Fallback copy",
            english.shellText("missingCatalogKey", "Fallback copy"),
        )
    }

    private fun preferenceState(language: AppLanguage): AppPreferencesUiState =
        AppPreferencesUiState(
            selectedLanguage = language,
            resolvedLanguage = language,
            languageOptions = emptyList(),
            textDirection = language.direction,
            displayName = null,
            theme = AppThemePreference.SYSTEM,
            opportunityAlertsPreferred = false,
            catalog = CATALOG,
        )

    private companion object {
        val CATALOG: AppStringCatalog = AppStringCatalog.decode(
            """
            {
              "languageMeta": {
                "en": {"label": "English", "native": "English", "dir": "ltr"},
                "fr": {"label": "French", "native": "Francais", "dir": "ltr"}
              },
              "en": {
                "home": "Home",
                "navOpportunities": "Opportunities",
                "highSchool": "High School",
                "support": "Support",
                "account": "Profile"
              },
              "fr": {
                "home": "Accueil",
                "navOpportunities": "Occasions",
                "highSchool": "Secondaire",
                "support": "Soutien",
                "account": "Profil"
              }
            }
            """.trimIndent(),
        )
    }
}
