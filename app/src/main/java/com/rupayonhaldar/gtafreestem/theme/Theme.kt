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

// Approved portfolio palette applies after bootstrap; launch artwork remains unchanged.
private val LightContentColorScheme = lightColorScheme(
    primary = Color(0xFF2E7184), onPrimary = ColorWhite,
    primaryContainer = Color(0xFFD1E9EC), onPrimaryContainer = Color(0xFF19334B),
    secondary = Color(0xFF396E83), onSecondary = ColorWhite,
    secondaryContainer = Color(0xFFE1EEF5), onSecondaryContainer = Color(0xFF19334B),
    tertiary = Color(0xFF8F432E), onTertiary = ColorWhite,
    tertiaryContainer = Color(0xFFFFE4D8), onTertiaryContainer = Color(0xFF54291B),
    background = Color(0xFFF4F8FD), onBackground = Color(0xFF19334B),
    surface = ColorWhite, onSurface = Color(0xFF19334B),
    surfaceTint = Color(0xFF2E7184),
    surfaceDim = Color(0xFFDCE7F0), surfaceBright = ColorWhite,
    surfaceContainerLowest = ColorWhite, surfaceContainerLow = Color(0xFFF4F8FD),
    surfaceContainer = Color(0xFFEAF1F8), surfaceContainerHigh = Color(0xFFE1EBF3),
    surfaceContainerHighest = Color(0xFFD8E5EE),
    inverseSurface = Color(0xFF23364C), inverseOnSurface = Color(0xFFEFF6FF),
    inversePrimary = Color(0xFFA4D8E2),
    surfaceVariant = Color(0xFFEAF1F8), onSurfaceVariant = Color(0xFF536B80),
    outline = Color(0xFF526F82), outlineVariant = Color(0xFFAAC1D1),
    error = Color(0xFFAD3027), onError = ColorWhite,
)
private val DarkContentColorScheme = darkColorScheme(
    primary = Color(0xFFA4D8E2), onPrimary = Color(0xFF142B3C),
    primaryContainer = Color(0xFF365969), onPrimaryContainer = Color(0xFFE9F8FF),
    secondary = Color(0xFFBDD9DF), onSecondary = Color(0xFF142B3C),
    secondaryContainer = Color(0xFF1D354B), onSecondaryContainer = Color(0xFFEFF6FF),
    tertiary = Color(0xFFEDB39A), onTertiary = Color(0xFF40271C),
    tertiaryContainer = Color(0xFF50372F), onTertiaryContainer = Color(0xFFFFDFD1),
    background = Color(0xFF14253A), onBackground = Color(0xFFEFF6FF),
    surface = Color(0xFF23364C), onSurface = Color(0xFFEFF6FF),
    surfaceTint = Color(0xFFA4D8E2),
    surfaceDim = Color(0xFF14253A), surfaceBright = Color(0xFF354B63),
    surfaceContainerLowest = Color(0xFF0F1E30), surfaceContainerLow = Color(0xFF1B2E44),
    surfaceContainer = Color(0xFF23364C), surfaceContainerHigh = Color(0xFF2B4057),
    surfaceContainerHighest = Color(0xFF354B63),
    inverseSurface = Color(0xFFEAF1F8), inverseOnSurface = Color(0xFF19334B),
    inversePrimary = Color(0xFF2E7184),
    surfaceVariant = Color(0xFF1D354B), onSurfaceVariant = Color(0xFFBCD0E1),
    outline = Color(0xFF8BA3B7), outlineVariant = Color(0xFF4B6D87),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
)
private val ContentShapes = Shapes(
    small = RoundedCornerShape(topStart = 5.dp, topEnd = 8.dp, bottomEnd = 4.dp, bottomStart = 6.dp),
    medium = RoundedCornerShape(topStart = 6.dp, topEnd = 9.dp, bottomEnd = 5.dp, bottomStart = 8.dp),
    large = RoundedCornerShape(16.dp),
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

/** Applied only after bootstrap so the original loader palette and animation stay intact. */
@Composable
fun GTAFreeStemContentTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkContentColorScheme else LightContentColorScheme,
        typography = ContentTypography,
        shapes = ContentShapes,
        content = content,
    )
}
