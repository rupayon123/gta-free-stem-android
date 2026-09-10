package com.rupayonhaldar.gtafreestem

import com.rupayonhaldar.gtafreestem.localization.AppStringCatalog
import com.rupayonhaldar.gtafreestem.localization.AppLanguage
import com.rupayonhaldar.gtafreestem.ui.browse.OpportunityMapStrings
import java.util.Locale

internal fun localizedMapStrings(
    catalog: AppStringCatalog,
    language: AppLanguage,
    text: (String, String) -> String,
): OpportunityMapStrings = OpportunityMapStrings(
    mapTitle = text("map", "Map"),
    singleVisibleFormat = "%1\$s ${text("visible", "visible")}",
    multipleVisibleFormat = "%1\$s ${text("visible", "visible")}",
    mapHint = text("sourceDetails", "Source details stay available for verification."),
    selectedLabel = text("selectedState", "Selected"),
    notSelectedLabel = text("notSelectedState", "Not selected"),
    numberLocale = Locale.forLanguageTag(language.localeTag),
    markerPositionFormat = catalog.text(
        "mapMarkerPosition",
        language,
        placeholders = mapOf(
            "index" to "%1\$s",
            "count" to "%2\$s",
        ),
    ),
    markerActionLabel = text(
        "mapMarkerActionLabel",
        "Select opportunity",
    ),
    selectedOpportunityHeading = text("details", "Details"),
    sourceDetailsHint = text("sourceDetails", "Review the source before registering."),
    showDetailsAction = text("details", "View details"),
    emptyTitle = text("noOpportunities", "No opportunities to map"),
    emptyMessage = text("sourceDetails", "Use the list to view every opportunity."),
    previewTitle = text("map", "Map"),
    previewAction = text("map", "Open map"),
    previewEmptyMessage = text("locationNotFound", "Location unavailable"),
)

internal data class ExternalActionFailureCopy(
    val registration: String,
    val directions: String,
    val source: String,
)

internal fun localizedExternalActionFailureCopy(
    text: (String, String) -> String,
): ExternalActionFailureCopy = ExternalActionFailureCopy(
    registration = text(
        "registrationLinkUnavailable",
        "This registration link is unavailable.",
    ),
    directions = text(
        "directionsUnavailable",
        "Directions are unavailable on this device.",
    ),
    source = text("sourceLinkUnavailable", "The source link is unavailable."),
)
