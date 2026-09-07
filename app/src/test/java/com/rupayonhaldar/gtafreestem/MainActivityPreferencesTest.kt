package com.rupayonhaldar.gtafreestem

import androidx.compose.ui.unit.LayoutDirection
import com.rupayonhaldar.gtafreestem.data.local.AppThemePreference
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog
import com.rupayonhaldar.gtafreestem.localization.TextDirection
import com.rupayonhaldar.gtafreestem.platform.alerts.OpportunityAlertPreferenceUpdate
import com.rupayonhaldar.gtafreestem.ui.preferences.OpportunityAlertFeedback
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
    fun fiveNavigationLabelsUseCatalogWithStableProfileLabel() {
        val french = preferenceState(AppLanguage.FRENCH)
        assertEquals(
            listOf("Accueil", "Occasions", "Secondaire", "Soutien", "Profil"),
            PrimaryDestination.entries.map(french::navigationLabel),
        )

        val english = preferenceState(AppLanguage.ENGLISH)
        assertEquals("Profile", english.navigationLabel(PrimaryDestination.ACCOUNT))
        assertEquals(
            "Fallback copy",
            english.shellText("missingCatalogKey", "Fallback copy"),
        )
    }

    @Test
    fun launchExperienceUsesDedicatedLocalizedCopyForEveryProgressPhase() {
        val french = preferenceState(AppLanguage.FRENCH)

        assertEquals("Préparation de votre recherche…", french.launchExperienceCopy(0.16f).status)
        assertEquals("Ouverture de la bibliothèque locale…", french.launchExperienceCopy(0.38f).status)
        assertEquals("Vérification des détails…", french.launchExperienceCopy(0.90f).status)
        assertEquals("Prêt", french.launchExperienceCopy(1f).status)
        assertEquals(
            "Chargement des occasions",
            french.launchExperienceCopy(0.16f).progressLabel,
        )
    }

    @Test
    fun externalLinkFailureUsesLocalizedPlaceholderPlacement() {
        val french = preferenceState(AppLanguage.FRENCH)

        assertEquals(
            "Impossible d’ouvrir Politique de confidentialité.",
            french.externalLinkUnavailableText("Politique de confidentialité"),
        )
    }

    @Test
    fun permissionDenialAndEnableFailuresMapToSpecificOneShotFeedback() {
        assertEquals(
            OpportunityAlertFeedback.PERMISSION_DENIED,
            alertFeedbackForPermissionResult(granted = false, applied = false),
        )
        assertEquals(
            OpportunityAlertFeedback.UNAVAILABLE,
            alertFeedbackForPermissionResult(granted = true, applied = false),
        )
        assertEquals(
            OpportunityAlertFeedback.UNAVAILABLE,
            alertFeedbackForSynchronousUpdate(
                preferred = true,
                update = OpportunityAlertPreferenceUpdate.FAILED,
            ),
        )
        assertEquals(
            null,
            alertFeedbackForPermissionResult(granted = true, applied = true),
        )
        assertEquals(
            null,
            alertFeedbackForSynchronousUpdate(
                preferred = true,
                update = OpportunityAlertPreferenceUpdate.REQUEST_PERMISSION,
            ),
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
                "account": "Profile",
                "externalLinkUnavailable": "{label} is unavailable.",
                "launchPreparingOpportunityHunt": "Preparing your opportunity hunt…",
                "launchOpeningLocalLibrary": "Opening the local opportunity library…",
                "launchCheckingOpportunityDetails": "Checking verified opportunity details…",
                "launchLoadingOpportunities": "Loading opportunities",
                "readyToHunt": "Ready",
                "preparingOpportunities": "WRONG preparing key",
                "savedAppCache": "WRONG cache key",
                "sourceDetails": "WRONG source key"
              },
              "fr": {
                "home": "Accueil",
                "navOpportunities": "Occasions",
                "highSchool": "Secondaire",
                "support": "Soutien",
                "account": "Profil",
                "externalLinkUnavailable": "Impossible d’ouvrir {label}.",
                "launchPreparingOpportunityHunt": "Préparation de votre recherche…",
                "launchOpeningLocalLibrary": "Ouverture de la bibliothèque locale…",
                "launchCheckingOpportunityDetails": "Vérification des détails…",
                "launchLoadingOpportunities": "Chargement des occasions",
                "readyToHunt": "Prêt",
                "preparingOpportunities": "MAUVAIS libellé de préparation",
                "savedAppCache": "MAUVAIS libellé de cache",
                "sourceDetails": "MAUVAIS libellé de source"
              }
            }
            """.trimIndent(),
        )
    }
}
