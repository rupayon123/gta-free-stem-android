package com.rupayonhaldar.gtafreestem.ui.browse

import androidx.compose.runtime.Immutable

/**
 * Copy supplied by the host screen, keeping this reusable panel independent of any localization
 * implementation. [English] is a complete fallback for previews and incremental integration.
 */
@Immutable
data class OpportunityFilterPanelLabels(
    val title: String,
    val reset: String,
    val noActiveFilters: String,
    val oneActiveFilter: String,
    val multipleActiveFiltersTemplate: String,
    val scopeDescription: String,
    val programDetailsSection: String,
    val pathwaysSection: String,
    val communityFocusSection: String,
    val sortSection: String,
    val region: String,
    val city: String,
    val category: String,
    val age: String,
    val language: String,
    val any: String,
    val volunteerHours: String,
    val coop: String,
    val mentorship: String,
    val scholarships: String,
    val blackFocused: String,
    val girlsFocused: String,
    val indigenousFocused: String,
    val leadership: String,
    val soonest: String,
    val relevance: String,
    val selectedState: String,
    val notSelectedState: String,
) {
    fun activeFilterCount(count: Int): String = when (count) {
        0 -> noActiveFilters
        1 -> oneActiveFilter
        else -> multipleActiveFiltersTemplate.replace("{count}", count.toString())
    }

    companion object {
        val English = OpportunityFilterPanelLabels(
            title = "Filters",
            reset = "Reset filters",
            noActiveFilters = "No filters active",
            oneActiveFilter = "1 filter active",
            multipleActiveFiltersTemplate = "{count} filters active",
            scopeDescription =
                "These filters cover current free programs, pathways, and community focus. " +
                    "Distance and New Finds filters are not available in this Android version yet.",
            programDetailsSection = "Program details",
            pathwaysSection = "Pathways",
            communityFocusSection = "Community focus",
            sortSection = "Sort results",
            region = "Region",
            city = "City",
            category = "Category",
            age = "Age",
            language = "Language",
            any = "Any",
            volunteerHours = "Volunteer hours",
            coop = "Co-op",
            mentorship = "Mentorship",
            scholarships = "Scholarships",
            blackFocused = "Black-focused",
            girlsFocused = "Girls-focused",
            indigenousFocused = "Indigenous-focused",
            leadership = "Leadership",
            soonest = "Soonest",
            relevance = "Best match",
            selectedState = "Selected",
            notSelectedState = "Not selected",
        )
    }
}
