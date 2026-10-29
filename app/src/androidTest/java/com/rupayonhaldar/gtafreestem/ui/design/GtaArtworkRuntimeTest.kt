package com.rupayonhaldar.gtafreestem.ui.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.rupayonhaldar.gtafreestem.theme.GTAFreeStemContentTheme
import org.junit.Rule
import org.junit.Test

/** Exercise every packaged image decoder and appearance, including compact navigation art. */
class GtaArtworkRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun everyApprovedObjectRendersInBothAppearances() {
        val dark = mutableStateOf(false)
        compose.setContent {
            GTAFreeStemContentTheme(dark.value) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    GtaArtwork.entries.forEach { art ->
                        Row {
                            GtaObject(art, "large-${art.name}", Modifier.size(56.dp))
                            GtaObject(art, "small-${art.name}", Modifier.size(28.dp), compact = true)
                            Text(art.name)
                        }
                    }
                }
            }
        }
        listOf(false, true).forEach { appearance ->
            compose.runOnIdle { dark.value = appearance }
            GtaArtwork.entries.forEach { art ->
                compose.onNodeWithContentDescription("large-${art.name}").performScrollTo().assertIsDisplayed()
                compose.onNodeWithContentDescription("small-${art.name}").performScrollTo().assertIsDisplayed()
            }
        }
    }
}
