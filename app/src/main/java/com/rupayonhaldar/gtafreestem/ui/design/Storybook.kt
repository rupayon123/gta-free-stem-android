package com.rupayonhaldar.gtafreestem.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object StorySpacing {
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Standard = 16.dp
    val Large = 24.dp
    val ExtraLarge = 32.dp
}

object StoryRadius {
    val Control = 14.dp
    val Card = 20.dp
    val Feature = 24.dp
}

val StoryLavender = Color(0xFF8C80C7)
val StoryOrange = Color(0xFFE87A33)

private val LightGradient = listOf(
    Color(0xFFEEF9F4),
    Color(0xFFFBF9EC),
    Color(0xFFF6F8F4),
)
private val DarkGradient = listOf(
    Color(0xFF071116),
    Color(0xFF091F26),
    Color(0xFF0B1A21),
)

@Composable
fun StorybookBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors = if (isDark) DarkGradient else LightGradient
    val primaryGlow = MaterialTheme.colorScheme.primary.copy(
        alpha = if (isDark) 0.07f else 0.10f,
    )
    val tertiaryGlow = MaterialTheme.colorScheme.tertiary.copy(
        alpha = if (isDark) 0.035f else 0.07f,
    )
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .semantics { },
        ) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = colors,
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                ),
            )
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow,
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.92f, size.height * 0.05f),
                    radius = size.minDimension * 0.55f,
                ),
                topLeft = Offset(size.width * 0.50f, -size.height * 0.20f),
                size = Size(size.width * 0.75f, size.height * 0.65f),
            )
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tertiaryGlow,
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.08f, size.height * 0.92f),
                    radius = size.minDimension * 0.50f,
                ),
                topLeft = Offset(-size.width * 0.45f, size.height * 0.62f),
                size = Size(size.width * 0.90f, size.height * 0.55f),
            )
        }
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
        ) {
            content()
        }
    }
}

@Composable
fun StoryCard(
    modifier: Modifier = Modifier,
    padding: Dp = StorySpacing.Standard,
    cornerRadius: Dp = StoryRadius.Card,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.985f else 1f,
        animationSpec = tween(160),
        label = "story-card-press",
    )
    val shape = RoundedCornerShape(cornerRadius)
    val containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    }
    val elevation = if (isDark) 5.dp else 7.dp
    val surfaceModifier = modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }

    if (onClick == null) {
        Surface(
            modifier = surfaceModifier,
            shape = shape,
            color = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(0.75.dp, borderColor),
            shadowElevation = elevation,
        ) {
            Box(modifier = Modifier.padding(padding), content = content)
        }
    } else {
        Surface(
            onClick = onClick,
            modifier = surfaceModifier,
            shape = shape,
            color = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(0.75.dp, borderColor),
            shadowElevation = elevation,
            interactionSource = interactionSource,
        ) {
            Box(modifier = Modifier.padding(padding), content = content)
        }
    }
}

@Composable
fun StickerBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.tertiary,
    icon: Painter? = null,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Surface(
        modifier = modifier,
        color = color.copy(alpha = if (isDark) 0.22f else 0.13f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = CircleShape,
        border = BorderStroke(
            0.75.dp,
            color.copy(alpha = if (isDark) 0.42f else 0.28f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            icon?.let {
                Icon(
                    painter = it,
                    contentDescription = null,
                    modifier = Modifier.defaultMinSize(minWidth = 14.dp, minHeight = 14.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
fun StorySectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
) {
    Row(
        modifier = modifier.semantics { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon?.let {
            Icon(
                painter = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

enum class StoryButtonKind {
    PRIMARY,
    SECONDARY,
    QUIET,
}

@Composable
fun StoryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: StoryButtonKind = StoryButtonKind.PRIMARY,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(160),
        label = "story-button-press",
    )
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val shape: Shape = RoundedCornerShape(StoryRadius.Control)
    val (container, foreground) = when (kind) {
        StoryButtonKind.PRIMARY -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        StoryButtonKind.SECONDARY -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurface
        StoryButtonKind.QUIET -> Color.Transparent to MaterialTheme.colorScheme.primary
    }
    val border = when (kind) {
        StoryButtonKind.PRIMARY -> null
        StoryButtonKind.SECONDARY -> BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.70f else 0.55f),
        )
        StoryButtonKind.QUIET -> BorderStroke(
            0.75.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
        )
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.48f
            },
        enabled = enabled,
        shape = shape,
        color = container,
        contentColor = foreground,
        border = border,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
