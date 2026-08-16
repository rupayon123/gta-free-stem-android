package com.rupayonhaldar.gtafreestem.ui.preferences

import com.rupayonhaldar.gtafreestem.data.local.AppThemePreference
import com.rupayonhaldar.gtafreestem.data.local.DisplayNameSaveResult
import com.rupayonhaldar.gtafreestem.data.local.LocalAccountPreferences
import com.rupayonhaldar.gtafreestem.data.local.LocalAccountPreferencesStore
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog
import com.rupayonhaldar.gtafreestem.localization.LanguagePreferenceStore
import com.rupayonhaldar.gtafreestem.localization.TextDirection
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPreferencesViewModelTest {
    @Test
    fun `initial state loads all catalog languages system locale direction and local account`() {
        val viewModel = AppPreferencesViewModel(
            catalog = bundledCatalog(),
            languagePreferences = FakeLanguageStore(),
            accountPreferences = FakeAccountStore(
                preferences = LocalAccountPreferences(
                    displayName = "Ada",
                    theme = AppThemePreference.DARK,
                    opportunityAlertsPreferred = true,
                ),
            ),
            systemLanguageTags = { listOf("xx", "ar-EG") },
        )

        val state = viewModel.uiState.value

        assertTrue(state.followsSystemLanguage)
        assertEquals(AppLanguage.ARABIC, state.resolvedLanguage)
        assertEquals(TextDirection.RIGHT_TO_LEFT, state.textDirection)
        assertTrue(state.isRightToLeft)
        assertEquals("Ada", state.displayName)
        assertEquals(AppThemePreference.DARK, state.theme)
        assertTrue(state.opportunityAlertsPreferred)
        assertEquals(18, state.languageOptions.size)
        assertEquals(AppLanguage.entries.toSet(), state.languageOptions.map { it.language }.toSet())
        assertTrue(state.languageOptions.all { it.englishName.isNotBlank() && it.nativeName.isNotBlank() })
        assertTrue(state.text("account").isNotBlank())
    }

    @Test
    fun `explicit language persists and system selection re-resolves after locale change`() {
        val systemTags = mutableListOf("fr-CA")
        val languageStore = FakeLanguageStore(selected = AppLanguage.SPANISH)
        val viewModel = viewModel(
            languageStore = languageStore,
            systemLanguageTags = { systemTags },
        )

        assertEquals(AppLanguage.SPANISH, viewModel.uiState.value.resolvedLanguage)
        assertTrue(viewModel.setLanguage(AppLanguage.URDU))
        assertEquals(AppLanguage.URDU, languageStore.selected)
        assertEquals(AppLanguage.URDU, viewModel.uiState.value.selectedLanguage)
        assertTrue(viewModel.uiState.value.isRightToLeft)

        assertTrue(viewModel.useSystemLanguage())
        assertEquals(null, languageStore.selected)
        assertEquals(AppLanguage.FRENCH, viewModel.uiState.value.resolvedLanguage)
        assertFalse(viewModel.uiState.value.isRightToLeft)

        systemTags.clear()
        systemTags += "fa-IR"
        viewModel.refreshSystemLanguage()
        assertEquals(AppLanguage.FARSI, viewModel.uiState.value.resolvedLanguage)
        assertTrue(viewModel.uiState.value.isRightToLeft)
    }

    @Test
    fun `profile theme and alert updates refresh observable state`() {
        val accountStore = FakeAccountStore()
        val viewModel = viewModel(accountStore = accountStore)

        assertEquals(DisplayNameSaveResult.SAVED, viewModel.saveDisplayName("Grace Hopper"))
        assertEquals("Grace Hopper", viewModel.uiState.value.displayName)
        assertTrue(viewModel.setTheme(AppThemePreference.LIGHT))
        assertEquals(AppThemePreference.LIGHT, viewModel.uiState.value.theme)
        assertTrue(viewModel.setOpportunityAlertsPreferred(true))
        assertTrue(viewModel.uiState.value.opportunityAlertsPreferred)
        assertTrue(viewModel.clearProfile())
        assertEquals(null, viewModel.uiState.value.displayName)
        assertEquals(AppThemePreference.LIGHT, viewModel.uiState.value.theme)
        assertTrue(viewModel.uiState.value.opportunityAlertsPreferred)
    }

    @Test
    fun `failed or throwing persistence leaves last confirmed state unchanged`() {
        val languageStore = FakeLanguageStore(selected = AppLanguage.FRENCH, acceptsWrites = false)
        val accountStore = FakeAccountStore(
            preferences = LocalAccountPreferences(displayName = "Ada"),
            acceptsWrites = false,
        )
        val viewModel = viewModel(languageStore = languageStore, accountStore = accountStore)
        val original = viewModel.uiState.value

        assertFalse(viewModel.setLanguage(AppLanguage.ARABIC))
        assertEquals(DisplayNameSaveResult.STORAGE_ERROR, viewModel.saveDisplayName("Grace"))
        assertFalse(viewModel.setTheme(AppThemePreference.DARK))
        assertFalse(viewModel.setOpportunityAlertsPreferred(true))
        assertFalse(viewModel.clearProfile())
        assertEquals(original, viewModel.uiState.value)

        languageStore.throwOnWrite = true
        accountStore.throwOnWrite = true
        assertFalse(viewModel.useSystemLanguage())
        assertEquals(DisplayNameSaveResult.STORAGE_ERROR, viewModel.saveDisplayName("Grace"))
        assertFalse(viewModel.setTheme(AppThemePreference.DARK))
        assertEquals(original, viewModel.uiState.value)
    }

    @Test
    fun `read failures use English and migration-safe local defaults`() {
        val viewModel = AppPreferencesViewModel(
            catalog = bundledCatalog(),
            languagePreferences = FakeLanguageStore(throwOnRead = true),
            accountPreferences = FakeAccountStore(throwOnRead = true),
            systemLanguageTags = { error("locale read failed") },
        )

        val state = viewModel.uiState.value

        assertTrue(state.followsSystemLanguage)
        assertEquals(AppLanguage.ENGLISH, state.resolvedLanguage)
        assertFalse(state.isRightToLeft)
        assertEquals(null, state.displayName)
        assertEquals(AppThemePreference.SYSTEM, state.theme)
        assertFalse(state.opportunityAlertsPreferred)
    }

    private fun viewModel(
        languageStore: FakeLanguageStore = FakeLanguageStore(),
        accountStore: FakeAccountStore = FakeAccountStore(),
        systemLanguageTags: () -> Iterable<String> = { listOf("en-CA") },
    ) = AppPreferencesViewModel(
        catalog = bundledCatalog(),
        languagePreferences = languageStore,
        accountPreferences = accountStore,
        systemLanguageTags = systemLanguageTags,
    )

    private fun bundledCatalog(): AppStringCatalog {
        val file = sequenceOf(
            File("app/src/main/res/raw/app_strings.json"),
            File("src/main/res/raw/app_strings.json"),
        ).firstOrNull(File::isFile)
        checkNotNull(file) { "Bundled app string catalog is missing" }
        return AppStringCatalog.decode(file.readText())
    }

    private class FakeLanguageStore(
        var selected: AppLanguage? = null,
        private val acceptsWrites: Boolean = true,
        private val throwOnRead: Boolean = false,
        var throwOnWrite: Boolean = false,
    ) : LanguagePreferenceStore {
        override fun selectedLanguage(): AppLanguage? {
            if (throwOnRead) error("language read failed")
            return selected
        }

        override fun setSelectedLanguage(language: AppLanguage?): Boolean {
            if (throwOnWrite) error("language write failed")
            if (!acceptsWrites) return false
            selected = language
            return true
        }
    }

    private class FakeAccountStore(
        var preferences: LocalAccountPreferences = LocalAccountPreferences(),
        private val acceptsWrites: Boolean = true,
        private val throwOnRead: Boolean = false,
        var throwOnWrite: Boolean = false,
    ) : LocalAccountPreferencesStore {
        override fun currentPreferences(): LocalAccountPreferences {
            if (throwOnRead) error("account read failed")
            return preferences
        }

        override fun saveDisplayName(displayName: String): DisplayNameSaveResult {
            if (throwOnWrite) error("account write failed")
            if (!acceptsWrites) return DisplayNameSaveResult.STORAGE_ERROR
            preferences = preferences.copy(displayName = displayName.trim())
            return DisplayNameSaveResult.SAVED
        }

        override fun setTheme(theme: AppThemePreference): Boolean = write {
            preferences = preferences.copy(theme = theme)
        }

        override fun setOpportunityAlertsPreferred(preferred: Boolean): Boolean = write {
            preferences = preferences.copy(opportunityAlertsPreferred = preferred)
        }

        override fun clearProfile(): Boolean = write {
            preferences = preferences.copy(displayName = null)
        }

        private fun write(update: () -> Unit): Boolean {
            if (throwOnWrite) error("account write failed")
            if (!acceptsWrites) return false
            update()
            return true
        }
    }
}
