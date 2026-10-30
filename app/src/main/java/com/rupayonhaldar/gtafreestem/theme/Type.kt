package com.rupayonhaldar.gtafreestem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
import com.rupayonhaldar.gtafreestem.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 31.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)

/** Exact approved variable font. Its default axis is ExtraLight, so pin every weight. */
@OptIn(ExperimentalTextApi::class)
private val ContentFont = FontFamily(
    Font(R.font.nunito, weight = FontWeight(400), variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.nunito, weight = FontWeight(500), variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.nunito, weight = FontWeight(600), variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.nunito, weight = FontWeight(700), variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.nunito, weight = FontWeight(800), variationSettings = FontVariation.Settings(FontVariation.weight(800))),
    Font(R.font.nunito, weight = FontWeight(900), variationSettings = FontVariation.Settings(FontVariation.weight(900))),
)

internal val ContentTypography = AppTypography.let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = ContentFont),
        displayMedium = base.displayMedium.copy(fontFamily = ContentFont),
        displaySmall = base.displaySmall.copy(fontFamily = ContentFont, fontWeight = FontWeight.ExtraBold),
        headlineLarge = base.headlineLarge.copy(fontFamily = ContentFont, fontWeight = FontWeight.ExtraBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = ContentFont, fontWeight = FontWeight.ExtraBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = ContentFont, fontWeight = FontWeight.ExtraBold),
        titleLarge = base.titleLarge.copy(fontFamily = ContentFont, fontWeight = FontWeight.ExtraBold),
        titleMedium = base.titleMedium.copy(fontFamily = ContentFont),
        titleSmall = base.titleSmall.copy(fontFamily = ContentFont),
        bodyLarge = base.bodyLarge.copy(fontFamily = ContentFont),
        bodyMedium = base.bodyMedium.copy(fontFamily = ContentFont),
        bodySmall = base.bodySmall.copy(fontFamily = ContentFont),
        labelLarge = base.labelLarge.copy(fontFamily = ContentFont),
        labelMedium = base.labelMedium.copy(fontFamily = ContentFont),
        labelSmall = base.labelSmall.copy(fontFamily = ContentFont),
    )
}
