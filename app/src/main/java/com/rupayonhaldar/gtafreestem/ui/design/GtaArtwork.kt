package com.rupayonhaldar.gtafreestem.ui.design

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import com.rupayonhaldar.gtafreestem.R

/** Approved original objects; preserve the drawing colours instead of tinting them. */
enum class GtaArtwork(@param:DrawableRes val light: Int, @param:DrawableRes val dark: Int,
    @param:DrawableRes val compactLight: Int, @param:DrawableRes val compactDark: Int) {
    HOME(R.drawable.gta_home_illustration_light, R.drawable.gta_home_illustration_dark, R.drawable.gta_home_compact_light, R.drawable.gta_home_compact_dark),
    EXPLORE(R.drawable.gta_explore_illustration_light, R.drawable.gta_explore_illustration_dark, R.drawable.gta_explore_compact_light, R.drawable.gta_explore_compact_dark),
    SAVED(R.drawable.gta_saved_illustration_light, R.drawable.gta_saved_illustration_dark, R.drawable.gta_saved_compact_light, R.drawable.gta_saved_compact_dark),
    MAP(R.drawable.gta_map_illustration_light, R.drawable.gta_map_illustration_dark, R.drawable.gta_map_compact_light, R.drawable.gta_map_compact_dark),
    SCIENCE(R.drawable.gta_science_illustration_light, R.drawable.gta_science_illustration_dark, R.drawable.gta_science_compact_light, R.drawable.gta_science_compact_dark),
    ENGINEERING(R.drawable.gta_engineering_illustration_light, R.drawable.gta_engineering_illustration_dark, R.drawable.gta_engineering_compact_light, R.drawable.gta_engineering_compact_dark),
    IDEAS(R.drawable.gta_ideas_illustration_light, R.drawable.gta_ideas_illustration_dark, R.drawable.gta_ideas_compact_light, R.drawable.gta_ideas_compact_dark),
    LIBRARY(R.drawable.gta_library_illustration_light, R.drawable.gta_library_illustration_dark, R.drawable.gta_library_compact_light, R.drawable.gta_library_compact_dark),
    CALENDAR(R.drawable.gta_calendar_illustration_light, R.drawable.gta_calendar_illustration_dark, R.drawable.gta_calendar_compact_light, R.drawable.gta_calendar_compact_dark),
    FILTERS(R.drawable.gta_filters_illustration_light, R.drawable.gta_filters_illustration_dark, R.drawable.gta_filters_compact_light, R.drawable.gta_filters_compact_dark),
    ABOUT(R.drawable.gta_about_illustration_light, R.drawable.gta_about_illustration_dark, R.drawable.gta_about_compact_light, R.drawable.gta_about_compact_dark),
    ACCESS(R.drawable.gta_access_illustration_light, R.drawable.gta_access_illustration_dark, R.drawable.gta_access_compact_light, R.drawable.gta_access_compact_dark),
    OFFLINE(R.drawable.gta_offline_illustration_light, R.drawable.gta_offline_illustration_dark, R.drawable.gta_offline_compact_light, R.drawable.gta_offline_compact_dark),
    VOLUNTEER(R.drawable.gta_volunteer_illustration_light, R.drawable.gta_volunteer_illustration_dark, R.drawable.gta_volunteer_compact_light, R.drawable.gta_volunteer_compact_dark),
    CODE(R.drawable.gta_code_illustration_light, R.drawable.gta_code_illustration_dark, R.drawable.gta_code_compact_light, R.drawable.gta_code_compact_dark),
    ART(R.drawable.gta_art_illustration_light, R.drawable.gta_art_illustration_dark, R.drawable.gta_art_compact_light, R.drawable.gta_art_compact_dark),
    SCHOOL(R.drawable.gta_school_illustration_light, R.drawable.gta_school_illustration_dark, R.drawable.gta_school_compact_light, R.drawable.gta_school_compact_dark),
    SUPPORT(R.drawable.gta_support_illustration_light, R.drawable.gta_support_illustration_dark, R.drawable.gta_support_compact_light, R.drawable.gta_support_compact_dark),
    PROFILE(R.drawable.gta_profile_illustration_light, R.drawable.gta_profile_illustration_dark, R.drawable.gta_profile_compact_light, R.drawable.gta_profile_compact_dark),
}

@Composable
fun GtaObject(artwork: GtaArtwork, contentDescription: String?, modifier: Modifier = Modifier,
    compact: Boolean = false) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val resource = if (compact) {
        if (dark) artwork.compactDark else artwork.compactLight
    } else {
        if (dark) artwork.dark else artwork.light
    }
    Image(painterResource(resource), contentDescription, modifier)
}

internal fun artworkForCategory(category: String): GtaArtwork {
    val name = category.lowercase(java.util.Locale.ROOT)
    return when {
        "coding" in name || "robotic" in name || "technology" in name -> GtaArtwork.CODE
        "science" in name || "stem" in name -> GtaArtwork.SCIENCE
        "digital" in name -> GtaArtwork.IDEAS
        "engineering" in name || "maker" in name || "fabrication" in name -> GtaArtwork.ENGINEERING
        "library" in name -> GtaArtwork.LIBRARY
        "volunteer" in name || "community" in name -> GtaArtwork.VOLUNTEER
        "art" in name || "design" in name -> GtaArtwork.ART
        "career" in name || "mentor" in name || "co-op" in name -> GtaArtwork.IDEAS
        else -> GtaArtwork.LIBRARY
    }
}
