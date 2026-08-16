package com.rupayonhaldar.gtafreestem.localization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguagePreferenceStoreTest {
    @Test
    fun `language matching accepts catalog locale and legacy tags`() {
        assertEquals(AppLanguage.FRENCH, AppLanguage.matching("fr-CA"))
        assertEquals(AppLanguage.MANDARIN, AppLanguage.matching("zh_Hans"))
        assertEquals(AppLanguage.CANTONESE, AppLanguage.matching("zh-yue"))
        assertEquals(AppLanguage.FILIPINO, AppLanguage.matching("fil-PH"))
        assertEquals(AppLanguage.FARSI, AppLanguage.matching("Persian"))
        assertNull(AppLanguage.matching("xx-Unknown"))
    }

    @Test
    fun `stored language wins while corrupt or absent selection follows system then English`() {
        val persistence = FakeLanguagePreferencePersistence("fr-CA")
        val store = PersistentLanguagePreferenceStore(persistence)

        assertEquals(AppLanguage.FRENCH, store.selectedLanguage())
        assertEquals(AppLanguage.FRENCH, store.resolvedLanguage(listOf("ar-SA")))

        persistence.value = "unsupported"
        assertNull(store.selectedLanguage())
        assertEquals(AppLanguage.URDU, store.resolvedLanguage(listOf("xx", "ur-PK")))
        assertEquals(AppLanguage.ENGLISH, store.resolvedLanguage(emptyList()))
    }

    @Test
    fun `selection persists canonical catalog code and null returns to system`() {
        val persistence = FakeLanguagePreferencePersistence(null)
        val store = PersistentLanguagePreferenceStore(persistence)

        assertTrue(store.setSelectedLanguage(AppLanguage.CANTONESE))
        assertEquals("yue", persistence.value)
        assertEquals(AppLanguage.CANTONESE, store.selectedLanguage())

        assertTrue(store.setSelectedLanguage(null))
        assertNull(persistence.value)
        assertEquals(AppLanguage.ARABIC, store.resolvedLanguage(listOf("ar-EG")))
    }

    private class FakeLanguagePreferencePersistence(
        var value: String?,
    ) : LanguagePreferencePersistence {
        override fun readLanguageTag(): String? = value

        override fun writeLanguageTag(languageTag: String?): Boolean {
            value = languageTag
            return true
        }
    }
}
