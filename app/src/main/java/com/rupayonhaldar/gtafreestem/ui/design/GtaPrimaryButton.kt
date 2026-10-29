package com.rupayonhaldar.gtafreestem.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Shared approved contour; callers retain labels, enabled state and native click semantics. */
@Composable
fun GtaPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    content: @Composable RowScope.() -> Unit,
) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled,
        shape = shape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        content = content)
}
