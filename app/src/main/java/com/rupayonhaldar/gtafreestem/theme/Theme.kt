package com.rupayonhaldar.gtafreestem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ColorWhite = Color(0xFFFFFFFF)

private val LightColorScheme = lightColorScheme(
    primary = Lake,
    onPrimary = ColorWhite,
    primaryContainer = MintFoam,
    onPrimaryContainer = Navy,
    secondary = Moss,
    onSecondary = DeepOcean,
    secondaryContainer = Color(0xFFE1F0DC),
    onSecondaryContainer = Color(0xFF17361E),
    tertiary = Sun,
    onTertiary = DeepOcean,
    background = Canvas,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Cream,
    onSurfaceVariant = Color(0xFF405052),
    outline = LightOutline,
    error = Coral,
    onError = ColorWhite,
)

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = DeepOcean,
    primaryContainer = Color(0xFF124D56),
    onPrimaryContainer = Ice,
    secondary = Color(0xFF9AD5A3),
    onSecondary = Color(0xFF102B16),
    secondaryContainer = Color(0xFF274A2D),
    onSecondaryContainer = Color(0xFFDDF5DF),
    tertiary = Color(0xFFFFC857),
    onTertiary = DeepOcean,
    background = Night,
    onBackground = Ice,
    surface = NightCard,
    onSurface = Ice,
    surfaceVariant = Color(0xFF173039),
    onSurfaceVariant = Color(0xFFB7C9C9),
    outline = DarkOutline,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun GTAFreeStemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
