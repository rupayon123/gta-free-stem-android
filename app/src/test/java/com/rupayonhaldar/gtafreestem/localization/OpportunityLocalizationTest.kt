package com.rupayonhaldar.gtafreestem.localization

import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityTranslation
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpportunityLocalizationTest {
    @Test
    fun `resolver uses translated fields and falls back field by field`() {
        val opportunity = opportunity(
            translations = mapOf(
                "fr" to OpportunityTranslation(
                    title = "  Club de robotique  ",
                    organization = "   ",
                    summary = "Construisez des robots.",
                    category = "Codage et robotique",
                    city = "Toronto",
                    address = " ",
                    cost = "Gratuit",
                    tags = listOf(" robotique ", " "),
                ),
            ),
        )

        val localized = OpportunityLocalization.resolve(opportunity, AppLanguage.FRENCH)

        assertTrue(localized.hasDirectTranslation)
        assertEquals("Club de robotique", localized.title)
        assertEquals("Community Library", localized.organization)
        assertEquals("A hands-on robotics program.", localized.description)
        assertEquals("Construisez des robots.", localized.summary)
        assertEquals("Codage et robotique", localized.category)
        assertEquals("Toronto", localized.city)
        assertEquals("Toronto", localized.region)
        assertEquals("100 Queen St W", localized.address)
        assertEquals("Gratuit", localized.cost)
        assertEquals(listOf("robotique"), localized.tags)
    }

    @Test
    fun `resolver accepts locale aliases and ignores empty translation objects`() {
        val translated = opportunity(
            translations = mapOf(
                "zh_Hans" to OpportunityTranslation(title = "机器人俱乐部"),
                "es" to OpportunityTranslation(title = " ", tags = listOf(" ")),
            ),
        )

        assertEquals(
            "机器人俱乐部",
            OpportunityLocalization.resolve(translated, AppLanguage.MANDARIN).title,
        )
        assertNull(OpportunityLocalization.translation(translated, AppLanguage.SPANISH))
        assertFalse(
            OpportunityLocalization.resolve(translated, AppLanguage.SPANISH).hasDirectTranslation,
        )
    }

    @Test
    fun `missing direct translation uses localized summary template and preserves source summary`() {
        val opportunity = opportunity(translations = emptyMap())
        val localized = OpportunityLocalization.resolve(
            opportunity,
            AppLanguage.SPANISH,
            bundledCatalog(),
        )

        assertFalse(localized.hasDirectTranslation)
        assertTrue(localized.summary.contains("Oportunidad gratis"))
        assertTrue(localized.summary.contains("A hands-on robotics program."))
        assertEquals("Programacion y robotica", localized.category)
    }

    @Test
    fun `localized search index finds selected language and English source terms`() {
        val opportunity = opportunity(
            translations = mapOf(
                "fr" to OpportunityTranslation(
                    title = "Club de robotique",
                    description = "Construisez un robot.",
                    category = "Codage et robotique",
                    tags = listOf("programmation"),
                ),
            ),
        )

        assertTrue(
            LocalizedOpportunitySearchIndex.matches(
                opportunity,
                query = "robotique programmation",
                language = AppLanguage.FRENCH,
            ),
        )
        assertTrue(
            LocalizedOpportunitySearchIndex.matches(
                opportunity,
                query = "robotics hands-on",
                language = AppLanguage.FRENCH,
            ),
        )
        assertFalse(
            LocalizedOpportunitySearchIndex.matches(
                opportunity,
                query = "astronomie",
                language = AppLanguage.FRENCH,
            ),
        )
    }

    private fun opportunity(
        translations: Map<String, OpportunityTranslation>,
    ) = Opportunity(
        id = "robotics",
        title = "Robotics Club",
        organization = "Community Library",
        description = "A hands-on robotics program.",
        summary = "A hands-on robotics program.",
        category = "Coding & Robotics",
        categories = listOf("Coding & Robotics"),
        city = "Toronto",
        region = "Toronto",
        address = "100 Queen St W",
        ageMin = 10,
        ageMax = 14,
        languages = listOf("en", "fr"),
        cost = "Free",
        sourceUrl = "https://example.org/robotics",
        tags = listOf("robots"),
        translations = translations,
    )

    private fun bundledCatalog(): AppStringCatalog {
        val file = sequenceOf(
            File("app/src/main/res/raw/app_strings.json"),
            File("src/main/res/raw/app_strings.json"),
        ).firstOrNull(File::isFile)
        assertTrue("Bundled app string catalog is missing", file != null)
        return AppStringCatalog.decode(requireNotNull(file).readText())
    }
}
