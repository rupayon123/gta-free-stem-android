package com.rupayonhaldar.gtafreestem.ui.preferences

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rupayonhaldar.gtafreestem.data.local.AppThemePreference
import com.rupayonhaldar.gtafreestem.data.local.DisplayNameSaveResult
import com.rupayonhaldar.gtafreestem.data.local.LocalAccountDataDeletionResult
import com.rupayonhaldar.gtafreestem.localization.AndroidAppStringCatalogLoader
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountPreferencesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun localOnlyActionsAndWorkingAlertPreferenceAreVisibleAndCallable() {
        var savedLibraryOpened = false
        val state = state(
            displayName = "Ada",
            opportunityAlertsPreferred = true,
        )

        composeRule.setContent {
            MaterialTheme {
                AccountPreferencesScreen(
                    state = state,
                    onSaveDisplayName = { DisplayNameSaveResult.SAVED },
                    onClearProfile = { true },
                    onLanguageSelected = { true },
                    onThemeSelected = { true },
                    onOpportunityAlertsPreferredChanged = { true },
                    onOpenSavedLibrary = { savedLibraryOpened = true },
                    onDeleteAllLocalData = ::successfulDeletion,
                    onOpenSupport = {},
                    onOpenPrivacyPolicy = {},
                    onOpenTerms = {},
                )
            }
        }

        composeRule.onNodeWithText("Personalize this device without signing in.")
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            "Checks periodically for new matching opportunities and alerts you on this device.",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("New-match alerts")
            .assertIsOn()
        composeRule.onNodeWithTag(AccountPreferencesTestTags.SAVED_LIBRARY)
            .performScrollTo()
            .performClick()
        composeRule.runOnIdle { assertEquals(true, savedLibraryOpened) }
    }

    @Test
    fun displayNameEditorIsModalAndSavesThroughExistingCallback() {
        var savedName: String? = null
        composeRule.setContent {
            MaterialTheme {
                AccountPreferencesScreen(
                    state = state(displayName = "Ada"),
                    onSaveDisplayName = { candidate ->
                        savedName = candidate
                        DisplayNameSaveResult.SAVED
                    },
                    onClearProfile = { true },
                    onLanguageSelected = { true },
                    onThemeSelected = { true },
                    onOpportunityAlertsPreferredChanged = { true },
                    onOpenSavedLibrary = {},
                    onDeleteAllLocalData = ::successfulDeletion,
                    onOpenSupport = {},
                    onOpenPrivacyPolicy = {},
                    onOpenTerms = {},
                )
            }
        }

        composeRule.onNodeWithTag(AccountPreferencesTestTags.EDIT_PROFILE)
            .performClick()
        composeRule.onNodeWithTag(AccountPreferencesTestTags.PROFILE_DIALOG)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(AccountPreferencesTestTags.DISPLAY_NAME)
            .performTextReplacement("Grace Hopper")
        composeRule.onNodeWithTag(AccountPreferencesTestTags.SAVE_PROFILE)
            .performClick()

        composeRule.runOnIdle { assertEquals("Grace Hopper", savedName) }
    }

    @Test
    fun languageDialogOffersSystemAndAllCatalogLanguagesWithSelectionSemantics() {
        composeRule.setContent {
            MaterialTheme {
                AccountPreferencesScreen(
                    state = state(selectedLanguage = null),
                    onSaveDisplayName = { DisplayNameSaveResult.SAVED },
                    onClearProfile = { true },
                    onLanguageSelected = { true },
                    onThemeSelected = { true },
                    onOpportunityAlertsPreferredChanged = { true },
                    onOpenSavedLibrary = {},
                    onDeleteAllLocalData = ::successfulDeletion,
                    onOpenSupport = {},
                    onOpenPrivacyPolicy = {},
                    onOpenTerms = {},
                )
            }
        }

        composeRule.onNodeWithTag(AccountPreferencesTestTags.LANGUAGE_SELECTOR)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(AccountPreferencesTestTags.LANGUAGE_DIALOG)
            .assertIsDisplayed()
        composeRule.onNode(
            hasText("System") and hasAnyAncestor(
                hasTestTag(AccountPreferencesTestTags.LANGUAGE_DIALOG),
            ),
        ).assertIsSelected()
        composeRule.onNodeWithTag(AccountPreferencesTestTags.LANGUAGE_LIST)
            .performScrollToNode(hasText("العربية · Arabic"))
        composeRule.onNodeWithText("العربية · Arabic").assertIsDisplayed()
        composeRule.onNodeWithTag(AccountPreferencesTestTags.LANGUAGE_LIST)
            .performScrollToNode(hasText("Magyar · Hungarian"))
        composeRule.onNodeWithText("Magyar · Hungarian").assertIsDisplayed()
    }

    @Test
    fun alertPreferenceToggleDelegatesToTheSharedPreferenceAction() {
        var requestedPreference: Boolean? = null
        composeRule.setContent {
            MaterialTheme {
                AccountPreferencesScreen(
                    state = state(opportunityAlertsPreferred = false),
                    onSaveDisplayName = { DisplayNameSaveResult.SAVED },
                    onClearProfile = { true },
                    onLanguageSelected = { true },
                    onThemeSelected = { true },
                    onOpportunityAlertsPreferredChanged = { preferred ->
                        requestedPreference = preferred
                    },
                    onOpenSavedLibrary = {},
                    onDeleteAllLocalData = ::successfulDeletion,
                    onOpenSupport = {},
                    onOpenPrivacyPolicy = {},
                    onOpenTerms = {},
                )
            }
        }

        composeRule.onNodeWithText("New-match alerts")
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle { assertEquals(true, requestedPreference) }
    }

    private fun state(
        displayName: String? = null,
        selectedLanguage: AppLanguage? = AppLanguage.ENGLISH,
        opportunityAlertsPreferred: Boolean = false,
    ): AppPreferencesUiState {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val catalog = AndroidAppStringCatalogLoader.load(context)
        val resolvedLanguage = selectedLanguage ?: AppLanguage.ENGLISH
        return AppPreferencesUiState(
            selectedLanguage = selectedLanguage,
            resolvedLanguage = resolvedLanguage,
            languageOptions = AppLanguage.entries.map { language ->
                val metadata = catalog.metadata(language)
                AppLanguageOption(
                    language = language,
                    englishName = metadata.englishName,
                    nativeName = metadata.nativeName,
                    direction = metadata.direction,
                )
            },
            textDirection = catalog.metadata(resolvedLanguage).direction,
            displayName = displayName,
            theme = AppThemePreference.SYSTEM,
            opportunityAlertsPreferred = opportunityAlertsPreferred,
            catalog = catalog,
        )
    }

    private fun successfulDeletion() = LocalAccountDataDeletionResult(
        profileDeleted = true,
        searchHistoryDeleted = true,
        savedOpportunitiesDeleted = true,
    )
}
