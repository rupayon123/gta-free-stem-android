package com.rupayonhaldar.gtafreestem.ui.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rupayonhaldar.gtafreestem.localizedMapStrings
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog
import com.rupayonhaldar.gtafreestem.theme.GTAFreeStemTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class OpportunityMapTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mapAnnouncesVisibleCountAndKeepsEveryMarkerAtLeast48Dp() {
        val pins = listOf(TorontoPin, MississaugaPin)
        setMapContent(pins = pins)

        composeRule.onNodeWithTag(OpportunityMapTestTags.VISIBLE_COUNT)
            .assertIsDisplayed()
        composeRule.onNodeWithText("2 opportunities shown").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("2 opportunities shown", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(OpportunityMapTestTags.marker(TorontoPin.id))
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag(OpportunityMapTestTags.marker(MississaugaPin.id))
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun selectingMarkerShowsSourceReviewCardAndDetailsCallback() {
        var selectedByCallback: MapOpportunityPin? = null
        var openedByCallback: MapOpportunityPin? = null
        composeRule.setContent {
            GTAFreeStemTheme {
                var selectedId by remember { mutableStateOf<String?>(null) }
                Box(modifier = Modifier.width(420.dp)) {
                    OpportunityMap(
                        pins = listOf(TorontoPin),
                        selectedPinId = selectedId,
                        onPinSelected = { pin ->
                            selectedByCallback = pin
                            selectedId = pin.id
                        },
                        onShowDetails = { openedByCallback = it },
                    )
                }
            }
        }

        composeRule.onNodeWithTag(OpportunityMapTestTags.marker(TorontoPin.id))
            .performClick()
        composeRule.runOnIdle { assertEquals(TorontoPin, selectedByCallback) }
        composeRule.onNodeWithTag(OpportunityMapTestTags.marker(TorontoPin.id))
            .assertIsSelected()
        composeRule.onNodeWithTag(OpportunityMapTestTags.SELECTED_CARD)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Review the listing and its source before registering.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(OpportunityMapTestTags.DETAILS_ACTION)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals(TorontoPin, openedByCallback) }
    }

    @Test
    fun markerPositionSelectionAndActionSemanticsUseLocalizedMapStrings() {
        val catalog = AppStringCatalog.decode(
            """
            {
              "languageMeta": {
                "en": {"label":"English","native":"English","dir":"ltr"},
                "fr": {"label":"French","native":"Français","dir":"ltr"}
              },
              "en": {
                "selectedState":"Selected",
                "notSelectedState":"Not selected",
                "mapMarkerPosition":"Marker {index} of {count}",
                "mapMarkerActionLabel":"Select opportunity"
              },
              "fr": {
                "selectedState":"Sélectionné",
                "notSelectedState":"Non sélectionné",
                "mapMarkerPosition":"Repère {index} sur {count}",
                "mapMarkerActionLabel":"Sélectionner l’occasion"
              }
            }
            """.trimIndent(),
        )
        val text: (String, String) -> String = { key, fallback ->
            catalog.text(key, AppLanguage.FRENCH)
                .takeUnless { it.isBlank() || it == key }
                ?: fallback
        }
        val strings = localizedMapStrings(catalog, AppLanguage.FRENCH, text)

        composeRule.setContent {
            GTAFreeStemTheme {
                var selectedId by remember { mutableStateOf<String?>(null) }
                OpportunityMap(
                    pins = listOf(TorontoPin),
                    selectedPinId = selectedId,
                    onPinSelected = { selectedId = it.id },
                    onShowDetails = {},
                    strings = strings,
                )
            }
        }

        val marker = composeRule.onNodeWithTag(OpportunityMapTestTags.marker(TorontoPin.id))
            .assertContentDescriptionEquals(
                "Robotics club. Community Lab. Toronto. Repère 1 sur 1",
            )
        assertEquals(
            "Non sélectionné",
            marker.fetchSemanticsNode().config[SemanticsProperties.StateDescription],
        )
        assertEquals(
            "Sélectionner l’occasion",
            marker.fetchSemanticsNode().config[SemanticsActions.OnClick].label,
        )
        marker
            .performClick()
            .assertIsSelected()
            .assertContentDescriptionEquals(
                "Robotics club. Community Lab. Toronto. Repère 1 sur 1",
            )
        assertEquals(
            "Sélectionné",
            marker.fetchSemanticsNode().config[SemanticsProperties.StateDescription],
        )
    }

    @Test
    fun clusteredMarkerUsesSelectedAppLocaleForItsVisibleNumber() {
        val clusteredPins = listOf(
            TorontoPin,
            TorontoPin.copy(id = "toronto-second", title = "Second club"),
        )
        val strings = OpportunityMapStrings(
            numberLocale = Locale.forLanguageTag("ar"),
        )
        assertEquals("٢", strings.clusterSize(clusteredPins.size))

        composeRule.setContent {
            GTAFreeStemTheme {
                OpportunityMap(
                    pins = clusteredPins,
                    selectedPinId = null,
                    onPinSelected = {},
                    onShowDetails = {},
                    strings = strings,
                )
            }
        }

        composeRule.onNodeWithTag(OpportunityMapTestTags.marker(TorontoPin.id))
            .assertIsDisplayed()
        composeRule.onNodeWithText("٢", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun noValidCoordinatesShowsClearListFallback() {
        setMapContent(
            pins = listOf(
                TorontoPin.copy(
                    id = "invalid",
                    coordinate = MapCoordinate(Double.NaN, -79.3832),
                ),
            ),
        )

        composeRule.onNodeWithTag(OpportunityMapTestTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("No locations to map").assertIsDisplayed()
        composeRule.onNodeWithText(
            "These results do not include valid location coordinates. Use the list to view every opportunity.",
        ).assertIsDisplayed()
    }

    @Test
    fun detailPreviewHasAccessibleLocationInRtlAndAnExplicitFallback() {
        val displayedPin = mutableStateOf<MapOpportunityPin?>(TorontoPin)
        composeRule.setContent {
            GTAFreeStemTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    OpportunityMapPreview(pin = displayedPin.value)
                }
            }
        }

        composeRule.onNodeWithTag(OpportunityMapTestTags.PREVIEW)
            .assertIsDisplayed()
            .assertContentDescriptionEquals(
                "Location preview. Robotics club. Community Lab. Toronto. " +
                    "Offline schematic. Locations are approximate; select a marker to review its source details.",
            )

        composeRule.runOnIdle { displayedPin.value = null }
        composeRule.onNodeWithTag(OpportunityMapTestTags.PREVIEW_EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText(
            "A map preview is unavailable because this opportunity has no valid location coordinates.",
        ).assertIsDisplayed()
    }

    private fun setMapContent(pins: List<MapOpportunityPin>) {
        var selected: MapOpportunityPin? = null
        composeRule.setContent {
            GTAFreeStemTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    OpportunityMap(
                        pins = pins,
                        selectedPinId = null,
                        onPinSelected = { selected = it },
                        onShowDetails = {},
                    )
                }
            }
        }
        composeRule.runOnIdle { assertNull(selected) }
    }

    private companion object {
        val TorontoPin = MapOpportunityPin(
            id = "toronto",
            title = "Robotics club",
            organization = "Community Lab",
            locationLabel = "Toronto",
            coordinate = MapCoordinate(43.6532, -79.3832),
        )
        val MississaugaPin = MapOpportunityPin(
            id = "mississauga",
            title = "Science workshop",
            organization = "Discovery Centre",
            locationLabel = "Mississauga",
            coordinate = MapCoordinate(43.5890, -79.6441),
        )
    }
}
