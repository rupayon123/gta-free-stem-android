package com.rupayonhaldar.gtafreestem

import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog
import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizedAccessibilityCopyTest {
    @Test
    fun `filter and map semantics use the selected app language`() {
        val catalog = bundledCatalog()

        AppLanguage.entries.forEach { language ->
            val text = localizedText(catalog, language)
            val filterLabels = localizedFilterLabels(text)
            val mapStrings = localizedMapStrings(catalog, language, text)

            assertEquals(catalog.text("selectedState", language), filterLabels.selectedState)
            assertEquals(catalog.text("notSelectedState", language), filterLabels.notSelectedState)
            assertEquals(catalog.text("selectedState", language), mapStrings.selectedLabel)
            assertEquals(catalog.text("notSelectedState", language), mapStrings.notSelectedLabel)
            assertEquals(
                catalog.text("mapMarkerActionLabel", language),
                mapStrings.markerActionLabel,
            )
            val localizedIndex = mapStrings.clusterSize(3)
            val localizedCount = mapStrings.clusterSize(7)
            assertEquals(
                catalog.text(
                    "mapMarkerPosition",
                    language,
                    mapOf("index" to localizedIndex, "count" to localizedCount),
                ),
                mapStrings.markerPosition(index = 3, count = 7),
            )
            assertEquals(
                "$localizedIndex ${catalog.text("visible", language)}",
                mapStrings.visibleCount(count = 3),
            )
        }
    }

    @Test
    fun `map numbers use selected app locale instead of process default`() {
        val catalog = bundledCatalog()
        val previousDefault = Locale.getDefault()

        try {
            Locale.setDefault(Locale.US)
            mapOf(
                AppLanguage.ARABIC to "٣",
                AppLanguage.FARSI to "۳",
                AppLanguage.URDU to "۳",
            ).forEach { (language, expectedNumber) ->
                val strings = localizedMapStrings(
                    catalog,
                    language,
                    localizedText(catalog, language),
                )
                assertEquals(expectedNumber, strings.clusterSize(3))
                assertEquals(
                    "$expectedNumber ${catalog.text("visible", language)}",
                    strings.visibleCount(3),
                )
            }

            val language = AppLanguage.ARABIC
            val mapStrings = localizedMapStrings(catalog, language, localizedText(catalog, language))
            assertEquals("٣ ${catalog.text("visible", language)}", mapStrings.visibleCount(3))
            assertEquals(
                catalog.text(
                    "mapMarkerPosition",
                    language,
                    mapOf("index" to "٣", "count" to "٧"),
                ),
                mapStrings.markerPosition(index = 3, count = 7),
            )
        } finally {
            Locale.setDefault(previousDefault)
        }
    }

    @Test
    fun `external action failures use dedicated localized copy`() {
        val catalog = bundledCatalog()

        AppLanguage.entries.forEach { language ->
            val text = localizedText(catalog, language)
            val failureCopy = localizedExternalActionFailureCopy(text)

            assertEquals(
                catalog.text("registrationLinkUnavailable", language),
                failureCopy.registration,
            )
            assertEquals(catalog.text("directionsUnavailable", language), failureCopy.directions)
            assertEquals(catalog.text("sourceLinkUnavailable", language), failureCopy.source)
            assertNotEquals(catalog.text("registerApply", language), failureCopy.registration)
            assertNotEquals(catalog.text("directions", language), failureCopy.directions)
            assertNotEquals(catalog.text("sourceLink", language), failureCopy.source)
        }
    }

    private fun localizedText(
        catalog: AppStringCatalog,
        language: AppLanguage,
    ): (String, String) -> String = { key, fallback ->
        catalog.text(key, language).takeUnless { it.isBlank() || it == key } ?: fallback
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
