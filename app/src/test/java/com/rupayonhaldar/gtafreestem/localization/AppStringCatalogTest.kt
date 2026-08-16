package com.rupayonhaldar.gtafreestem.localization

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStringCatalogTest {
    @Test
    fun `bundled catalog contains complete metadata and strings for all languages`() {
        val catalog = bundledCatalog()

        assertEquals(18, AppLanguage.entries.size)
        assertEquals(AppLanguage.entries.toSet(), catalog.availableLanguages)
        assertEquals(AppLanguage.entries.toSet(), catalog.metadataLanguages)
        assertEquals(184, catalog.sourceKeys.size)

        AppLanguage.entries.forEach { language ->
            val metadata = catalog.metadata(language)
            assertTrue("${language.catalogCode} English label is blank", metadata.englishName.isNotBlank())
            assertTrue("${language.catalogCode} native label is blank", metadata.nativeName.isNotBlank())
            assertEquals(language.direction, metadata.direction)
            assertEquals(catalog.sourceKeys, catalog.keys(language))
            assertTrue("${language.catalogCode} is incomplete", catalog.isComplete(language))
        }
    }

    @Test
    fun `only Arabic Farsi and Urdu are right to left`() {
        assertEquals(
            setOf(AppLanguage.ARABIC, AppLanguage.FARSI, AppLanguage.URDU),
            AppLanguage.entries.filter(AppLanguage::isRightToLeft).toSet(),
        )
    }

    @Test
    fun `missing or blank translation falls back to English then key`() {
        val catalog = AppStringCatalog.decode(
            """
            {
              "languageMeta": {
                "en": {"label":"English","native":"English","dir":"ltr"},
                "fr": {"label":"French","native":"Français","dir":"ltr"}
              },
              "en": {"hello":"Hello {name}","englishOnly":"English fallback"},
              "fr": {"hello":"Bonjour {name}","englishOnly":"   "}
            }
            """.trimIndent(),
        )

        assertEquals(
            "Bonjour Ada",
            catalog.text("hello", AppLanguage.FRENCH, mapOf("name" to "Ada")),
        )
        assertEquals("English fallback", catalog.text("englishOnly", AppLanguage.FRENCH))
        assertEquals("unknownKey", catalog.text("unknownKey", AppLanguage.FRENCH))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTagOrEnglish("not-a-language"))
    }

    @Test
    fun `placeholder substitution is literal single pass and preserves unknown tokens`() {
        val replacement = "${'$'}1\\{city}"

        assertEquals(
            "Value ${'$'}1\\{city}; city Toronto; unknown {missing}",
            SafePlaceholderSubstitution.substitute(
                "Value {value}; city {city}; unknown {missing}",
                mapOf("value" to replacement, "city" to "Toronto", "unused" to "ignored"),
            ),
        )
    }

    @Test
    fun `catalog rejects malformed JSON invalid direction and missing English source`() {
        assertThrows(IllegalArgumentException::class.java) {
            AppStringCatalog.decode("not json")
        }
        assertThrows(IllegalArgumentException::class.java) {
            AppStringCatalog.decode(
                """
                {
                  "languageMeta": {"en":{"label":"English","native":"English","dir":"sideways"}},
                  "en": {"hello":"Hello"}
                }
                """.trimIndent(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            AppStringCatalog.decode("""{"fr":{"hello":"Bonjour"}}""")
        }
    }

    private fun bundledCatalog(): AppStringCatalog {
        val file = sequenceOf(
            File("app/src/main/res/raw/app_strings.json"),
            File("src/main/res/raw/app_strings.json"),
        ).firstOrNull(File::isFile)
        assertTrue("Bundled app string catalog is missing", file != null)
        return AppStringCatalog.decode(requireNotNull(file).readText())
    }
}
