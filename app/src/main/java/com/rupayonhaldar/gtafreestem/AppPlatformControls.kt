package com.rupayonhaldar.gtafreestem

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rupayonhaldar.gtafreestem.domain.search.OpportunitySearchFilters
import com.rupayonhaldar.gtafreestem.platform.location.NearbyLocationState
import com.rupayonhaldar.gtafreestem.platform.location.NearbyLocationFixSource
import com.rupayonhaldar.gtafreestem.ui.design.StoryButton
import com.rupayonhaldar.gtafreestem.ui.design.StoryButtonKind
import com.rupayonhaldar.gtafreestem.ui.preferences.AppPreferencesUiState
import com.rupayonhaldar.gtafreestem.ui.preferences.OpportunityAlertFeedback
import kotlinx.coroutines.flow.Flow

// Restored from the preserved local implementation; no visual redesign.
/** App-level feedback stays visible whichever alert control initiated the request. */
@Composable
internal fun OpportunityAlertFeedbackHost(
    feedback: Flow<OpportunityAlertFeedback>,
    languageKey: Any?,
    text: (String, String) -> String,
    modifier: Modifier = Modifier,
) {
    val hostState = remember { SnackbarHostState() }
    val latestText by rememberUpdatedState(text)
    var activeFeedbackName by rememberSaveable { mutableStateOf<String?>(null) }
    var activeSequence by rememberSaveable { mutableLongStateOf(0L) }

    LaunchedEffect(feedback) {
        feedback.collect { event ->
            activeFeedbackName = event.name
            activeSequence += 1L
        }
    }
    LaunchedEffect(activeFeedbackName, activeSequence, languageKey) {
        val event = activeFeedbackName
            ?.let { name -> OpportunityAlertFeedback.entries.firstOrNull { it.name == name } }
            ?: return@LaunchedEffect
        val displayedSequence = activeSequence
        hostState.showSnackbar(
            message = latestText(event.catalogKey, event.englishFallback),
            duration = SnackbarDuration.Long,
        )
        if (activeSequence == displayedSequence) activeFeedbackName = null
    }

    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
    ) { data ->
        Snackbar(
            modifier = Modifier
                .testTag(OPPORTUNITY_ALERT_FEEDBACK_TEST_TAG)
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Text(data.visuals.message)
        }
    }
}

internal const val OPPORTUNITY_ALERT_FEEDBACK_TEST_TAG = "opportunity-alert-feedback"


internal data class LaunchExperienceCopy(
    val status: String,
    val progressLabel: String,
)

internal fun AppPreferencesUiState.launchExperienceCopy(progress: Float): LaunchExperienceCopy {
    val status = when {
        progress < 0.38f -> shellText(
            "launchPreparingOpportunityHunt",
            "Preparing your opportunity hunt…",
        )
        progress < 0.90f -> shellText(
            "launchOpeningLocalLibrary",
            "Opening the local opportunity library…",
        )
        progress < 1f -> shellText(
            "launchCheckingOpportunityDetails",
            "Checking verified opportunity details…",
        )
        else -> shellText("readyToHunt", "Ready to hunt")
    }
    return LaunchExperienceCopy(
        status = status,
        progressLabel = shellText(
            "launchLoadingOpportunities",
            "Loading opportunities",
        ),
    )
}

internal fun AppPreferencesUiState.externalLinkUnavailableText(label: String): String {
    val key = "externalLinkUnavailable"
    val localized = text(key, placeholders = mapOf("label" to label))
    return localized.takeUnless { it.isBlank() || it == key } ?: "$label is unavailable."
}


@Composable
internal fun NearbyAndAlertsControls(
    filters: OpportunitySearchFilters,
    nearbyLocationState: NearbyLocationState,
    onUseNearby: () -> Unit,
    onClearNearby: () -> Unit,
    alertsEnabled: Boolean,
    onToggleAlerts: () -> Unit,
    text: (String, String) -> String,
) {
    val isLocating = nearbyLocationState == NearbyLocationState.AwaitingPermission ||
        nearbyLocationState == NearbyLocationState.Locating
    val nearbyEnabled = filters.hasValidLocation
    val usesRecentApproximateLocation = nearbyEnabled &&
        (nearbyLocationState as? NearbyLocationState.Located)?.fix?.source ==
        NearbyLocationFixSource.LAST_KNOWN
    val status = when {
        usesRecentApproximateLocation -> text(
            "nearbyRecentApproximateLocation",
            "Using a recent approximate location.",
        )
        nearbyEnabled -> text("nearbyHuntingOn", "Nearby hunting is on.")
        nearbyLocationState == NearbyLocationState.PermissionDenied ->
            text("locationOffChooseCity", "Location is off. Choose a city instead.")
        nearbyLocationState == NearbyLocationState.LocationDisabled ->
            text("locationUnavailable", "Location is unavailable.")
        nearbyLocationState == NearbyLocationState.TimedOut ->
            text("locationNotFound", "Location was not found.")
        nearbyLocationState == NearbyLocationState.Error ->
            text("locationUnavailable", "Location is unavailable.")
        isLocating -> text("lookingNearby", "Looking nearby…")
        else -> null
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 350.dp || LocalDensity.current.fontScale >= 1.8f
            val nearbyButton: @Composable (Modifier) -> Unit = { modifier ->
                StoryButton(
                    onClick = if (nearbyEnabled) onClearNearby else onUseNearby,
                    enabled = !isLocating,
                    kind = StoryButtonKind.SECONDARY,
                    modifier = modifier,
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        if (nearbyEnabled) {
                            text("clearNearbyLocation", "Clear nearby")
                        } else {
                            text("useNearby", "Use nearby")
                        },
                    )
                }
            }
            val alertsButton: @Composable (Modifier) -> Unit = { modifier ->
                StoryButton(
                    onClick = onToggleAlerts,
                    kind = StoryButtonKind.SECONDARY,
                    modifier = modifier.testTag(BROWSE_ALERTS_TOGGLE_TEST_TAG),
                ) {
                    Text(
                        if (alertsEnabled) {
                            text("alertsOn", "New-match alerts are on.")
                        } else {
                            text("alerts", "Alerts")
                        },
                    )
                }
            }
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    nearbyButton(Modifier.fillMaxWidth())
                    alertsButton(Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    nearbyButton(Modifier.weight(1f))
                    alertsButton(Modifier.weight(1f))
                }
            }
        }
        status?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (
                    nearbyLocationState == NearbyLocationState.PermissionDenied ||
                    nearbyLocationState == NearbyLocationState.Error
                ) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

internal const val BROWSE_ALERTS_TOGGLE_TEST_TAG = "browse-alerts-toggle"
