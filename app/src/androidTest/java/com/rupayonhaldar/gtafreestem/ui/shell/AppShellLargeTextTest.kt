package com.rupayonhaldar.gtafreestem.ui.shell

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.rupayonhaldar.gtafreestem.theme.GTAFreeStemTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppShellLargeTextTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compactHeightDrawerPreservesTagalogLabelsAndDestinationsAtLargeText() {
        var selectedDestination = PrimaryDestination.HOME

        composeRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(900.dp, 280.dp)),
            ) {
                val currentDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(currentDensity.density, fontScale = 2f),
                ) {
                    GTAFreeStemTheme {
                        AdaptiveAppShell(
                            selectedDestination = selectedDestination,
                            onDestinationSelected = { selectedDestination = it },
                            showNavigation = true,
                            destinationLabel = tagalogLabels::getValue,
                        ) {}
                    }
                }
            }
        }

        PrimaryDestination.entries.reversed().forEach { destination ->
            composeRule.onNodeWithTag("primary-navigation-drawer-list")
                .performScrollToIndex(destination.ordinal + 2)
            val destinationNode = composeRule
                .onNodeWithTag("primary-navigation-${destination.name.lowercase()}")
            destinationNode
                .assertIsDisplayed()
                .performClick()
            assertCompleteLabel(
                label = tagalogLabels.getValue(destination),
                destinationNode = destinationNode,
            )
            composeRule.runOnIdle {
                assertEquals(destination, selectedDestination)
            }
        }
    }

    @Test
    fun compactHeightRailPreservesTagalogLabelsAndDestinationsAtLargeText() {
        var selectedDestination = PrimaryDestination.HOME

        composeRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(700.dp, 280.dp)),
            ) {
                val currentDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(currentDensity.density, fontScale = 2f),
                ) {
                    GTAFreeStemTheme {
                        AdaptiveAppShell(
                            selectedDestination = selectedDestination,
                            onDestinationSelected = { selectedDestination = it },
                            showNavigation = true,
                            destinationLabel = tagalogLabels::getValue,
                        ) {}
                    }
                }
            }
        }

        PrimaryDestination.entries.reversed().forEach { destination ->
            composeRule.onNodeWithTag("primary-navigation-rail-list")
                .performScrollToIndex(destination.ordinal)
            val destinationNode = composeRule
                .onNodeWithTag("primary-navigation-${destination.name.lowercase()}")
            destinationNode
                .assertIsDisplayed()
                .performClick()
            assertCompleteLabel(
                label = tagalogLabels.getValue(destination),
                destinationNode = destinationNode,
            )
            composeRule.runOnIdle {
                assertEquals(destination, selectedDestination)
            }
        }
    }

    @Test
    fun phoneNavigationPreservesTagalogLabelsAndDestinationsAtLargeText() {
        var selectedDestination = PrimaryDestination.HOME

        composeRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 640.dp)),
            ) {
                val currentDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(currentDensity.density, fontScale = 2f),
                ) {
                    GTAFreeStemTheme {
                        AdaptiveAppShell(
                            selectedDestination = selectedDestination,
                            onDestinationSelected = { selectedDestination = it },
                            showNavigation = true,
                            destinationLabel = tagalogLabels::getValue,
                        ) {}
                    }
                }
            }
        }

        val navigationBounds = composeRule.onNodeWithTag("primary-navigation-bar")
            .getUnclippedBoundsInRoot()
        PrimaryDestination.entries.forEach { destination ->
            val label = tagalogLabels.getValue(destination)
            val destinationNode = composeRule
                .onNodeWithTag("primary-navigation-${destination.name.lowercase()}")
            destinationNode.assertIsDisplayed().performClick()
            val labelNode = assertCompleteLabel(
                label = label,
                destinationNode = destinationNode,
            )
            val labelBounds = labelNode.getUnclippedBoundsInRoot()
            assertTrue("$label started above the navigation bar", labelBounds.top >= navigationBounds.top)
            assertTrue("$label ended below the navigation bar", labelBounds.bottom <= navigationBounds.bottom)
            composeRule.runOnIdle {
                assertEquals(destination, selectedDestination)
            }
        }
    }

    private fun assertCompleteLabel(
        label: String,
        destinationNode: androidx.compose.ui.test.SemanticsNodeInteraction,
    ): androidx.compose.ui.test.SemanticsNodeInteraction {
        val labelNode = composeRule.onNodeWithText(label, useUnmergedTree = true)
        labelNode.assertIsDisplayed()
        val layout = labelNode.textLayoutResult(label)
        val lastVisibleEnd = layout.getLineEnd(layout.lineCount - 1, visibleEnd = true)
        val minimumLineLeft = (0 until layout.lineCount).minOf(layout::getLineLeft)
        val maximumLineRight = (0 until layout.lineCount).maxOf(layout::getLineRight)
        val heightOverflowPx = (layout.multiParagraph.height - layout.size.height).coerceAtLeast(0f)
        val failureContext =
            "$label must preserve every visible character; lines=${layout.lineCount}, " +
                "lastVisibleEnd=$lastVisibleEnd, didOverflowWidth=${layout.didOverflowWidth}, " +
                "didOverflowHeight=${layout.didOverflowHeight}, " +
                "lineBounds=$minimumLineLeft..$maximumLineRight, " +
                "heightOverflowPx=$heightOverflowPx, size=${layout.size}, " +
                "paragraphHeight=${layout.multiParagraph.height}, " +
                "constraints=${layout.layoutInput.constraints}"
        assertEquals(failureContext, label.length, lastVisibleEnd)
        assertTrue(failureContext, minimumLineLeft >= -MAX_LAYOUT_ROUNDING_PX)
        assertTrue(failureContext, maximumLineRight <= layout.size.width + MAX_LAYOUT_ROUNDING_PX)
        assertTrue(failureContext, heightOverflowPx <= MAX_LAYOUT_ROUNDING_PX)

        val labelBounds = labelNode.getUnclippedBoundsInRoot()
        val destinationBounds = destinationNode.getUnclippedBoundsInRoot()
        assertTrue("$label started above its destination", labelBounds.top >= destinationBounds.top)
        assertTrue("$label ended below its destination", labelBounds.bottom <= destinationBounds.bottom)
        assertTrue("$label started before its destination", labelBounds.left >= destinationBounds.left)
        assertTrue("$label ended after its destination", labelBounds.right <= destinationBounds.right)
        return labelNode
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.textLayoutResult(
        label: String,
    ): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            assertTrue("Expected a text layout result for $label", action(layouts))
        }
        return layouts.single()
    }

    private companion object {
        const val MAX_LAYOUT_ROUNDING_PX = 1f

        val tagalogLabels = mapOf(
            PrimaryDestination.HOME to "Tahanan",
            PrimaryDestination.OPPORTUNITIES to "Mga Opportunity",
            PrimaryDestination.HIGH_SCHOOL to "Sekondarya",
            PrimaryDestination.SUPPORT to "Suporta",
            PrimaryDestination.ACCOUNT to "Profile ng device",
        )
    }
}
