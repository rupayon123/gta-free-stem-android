package com.rupayonhaldar.gtafreestem.ui.design

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.rupayonhaldar.gtafreestem.theme.GTAFreeStemTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StorybookLargeTextTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun freeForEveryonePromiseWrapsWithoutLosingTextAtLargeText() {
        val promise = "Everything here is free for everyone."

        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale = 2f),
            ) {
                GTAFreeStemTheme {
                    Box(Modifier.width(280.dp)) {
                        StickerBadge(text = promise)
                    }
                }
            }
        }

        val node = composeRule.onNodeWithText(promise, useUnmergedTree = true)
            .assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            assertTrue("Expected a text layout result for the free-for-everyone promise", action(layouts))
        }
        val layout = layouts.single()

        assertFalse("The promise must not be visually truncated", layout.hasVisualOverflow)
        assertEquals(
            "The promise must preserve every visible character",
            promise.length,
            layout.getLineEnd(layout.lineCount - 1, visibleEnd = true),
        )
        assertTrue("The promise should wrap instead of shrinking or clipping", layout.lineCount > 1)
    }
}
