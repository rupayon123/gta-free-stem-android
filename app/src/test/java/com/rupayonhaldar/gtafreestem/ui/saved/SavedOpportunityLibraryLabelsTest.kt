package com.rupayonhaldar.gtafreestem.ui.saved

import com.rupayonhaldar.gtafreestem.data.local.SavedOpportunityEntry
import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import com.rupayonhaldar.gtafreestem.domain.model.OpportunityTranslation
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog
import java.io.File
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedOpportunityLibraryLabelsTest {
    @Test
    fun `existing labels localize while saved-specific copy falls back to English`() {
        val labels = SavedOpportunityLibraryLabels(AppLanguage.SPANISH, bundledCatalog())

        assertEquals("Guardado", labels.title)
        assertEquals("Archivo", labels.archive)
        assertEquals("Detalles", labels.details)
        assertEquals("Current", labels.current)
        assertTrue(labels.localOnlyExplanation.contains("Nothing here is synced"))
    }

    @Test
    fun `card presentation localizes fields but retains complete source opportunity`() {
        val catalog = bundledCatalog()
        val opportunity = opportunity().copy(
            equipment = "Laptop provided",
            translations = mapOf(
                "es" to OpportunityTranslation(
                    title = "Robótica comunitaria",
                    organization = "Biblioteca Central",
                    summary = "Construye un robot sin costo.",
                    category = "Robótica",
                    city = "Toronto",
                    cost = "Gratis",
                ),
            ),
        )
        val entry = SavedOpportunityEntry(
            opportunity = opportunity,
            savedAt = Instant.parse("2026-08-10T16:00:00Z"),
        )

        val card = savedOpportunityCardText(
            entry = entry,
            language = AppLanguage.SPANISH,
            catalog = catalog,
        )

        assertEquals("Robótica comunitaria", card.title)
        assertEquals("Biblioteca Central", card.organization)
        assertEquals("Construye un robot sin costo.", card.summary)
        assertTrue(card.facts.any { fact -> fact.label == "Costo" && fact.value == "Gratis" })
        assertEquals("Laptop provided", entry.opportunity.equipment)
        assertEquals(opportunity, entry.opportunity)
    }

    @Test
    fun `archive explanation distinguishes removed feed entry from other archive reasons`() {
        val labels = SavedOpportunityLibraryLabels(AppLanguage.ENGLISH, bundledCatalog())

        assertTrue(
            archiveStatusExplanation(opportunity(status = "removed"), labels)
                .contains("no longer in the current verified feed"),
        )
        assertTrue(
            archiveStatusExplanation(opportunity(status = "inactive"), labels)
                .contains("deadline or end date passed"),
        )
        assertTrue(labels.unresolvedExplanation(1).startsWith("1 item"))
        assertTrue(labels.unresolvedExplanation(3).startsWith("3 items"))
        assertEquals("0 saved opportunities", labels.itemCount(-5))
    }

    private fun opportunity(status: String = "active") = Opportunity(
        id = "robotics",
        title = "Community Robotics",
        organization = "Central Library",
        description = "Build a robot with mentors.",
        summary = "A complete free robotics workshop.",
        category = "Coding and Robotics",
        city = "Toronto",
        region = "Ontario",
        startDate = "2026-09-01",
        endDate = "2026-10-01",
        deadline = "2026-08-25",
        ageMin = 12,
        ageMax = 17,
        languages = listOf("en", "fr"),
        cost = "Free",
        sourceUrl = "https://example.org/source",
        status = status,
    )

    private fun bundledCatalog(): AppStringCatalog {
        val file = sequenceOf(
            File("app/src/main/res/raw/app_strings.json"),
            File("src/main/res/raw/app_strings.json"),
        ).firstOrNull(File::isFile)
        checkNotNull(file) { "Bundled app string catalog is missing" }
        return AppStringCatalog.decode(file.readText())
    }
}
