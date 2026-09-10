package com.rupayonhaldar.gtafreestem.ui.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.rupayonhaldar.gtafreestem.theme.GTAFreeStemTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StorybookDarkThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun darkBackgroundAndCardProvideReadableDefaultTextColors() {
        composeRule.setContent {
            GTAFreeStemTheme(darkTheme = true) {
                StorybookBackground {
                    Column(Modifier.padding(16.dp)) {
                        StorySectionTitle("Background heading")
                        StoryCard {
                            Text("Card status")
                        }
                    }
                }
            }
        }

        assertLightText("Background heading")
        assertLightText("Card status")
    }

    private fun assertLightText(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                assertTrue("Expected a text layout result for $text", action(layouts))
            }

        val color = layouts.single().layoutInput.style.color
        assertTrue(
            "Expected readable light text for $text in dark mode, but found $color",
            color.luminance() >= 0.5f,
        )
    }
}
