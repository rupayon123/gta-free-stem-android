package com.rupayonhaldar.gtafreestem.platform.location

/**
 * An in-memory, coarse location fix used only to rank opportunities near the user.
 *
 * The app must not persist or transmit this value. [source] makes a time-bounded
 * last-known fallback visible to the UI instead of presenting it as a live fix.
 */
data class NearbyLocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val capturedAtEpochMillis: Long,
    val source: NearbyLocationFixSource,
) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0) {
            "Latitude must be finite and in range."
        }
        require(longitude.isFinite() && longitude in -180.0..180.0) {
            "Longitude must be finite and in range."
        }
        require(accuracyMeters == null || (accuracyMeters.isFinite() && accuracyMeters >= 0f)) {
            "Accuracy must be null or a finite, non-negative value."
        }
        require(capturedAtEpochMillis >= 0L) {
            "Capture time must not be negative."
        }
    }
}

enum class NearbyLocationFixSource {
    CURRENT,
    LAST_KNOWN,
}

sealed interface NearbyLocationState {
    data object Idle : NearbyLocationState

    /** The user explicitly tapped "Use nearby" and Android permission UI is required. */
    data object AwaitingPermission : NearbyLocationState

    /** A single foreground fix is in progress. No continuing updates are registered. */
    data object Locating : NearbyLocationState

    data class Located(val fix: NearbyLocationFix) : NearbyLocationState

    data object PermissionDenied : NearbyLocationState

    data object LocationDisabled : NearbyLocationState

    data object TimedOut : NearbyLocationState

    data object Cancelled : NearbyLocationState

    /** A provider/API failure. Keep user-facing copy generic and offer another attempt. */
    data object Error : NearbyLocationState
}

/** Activity work requested by the pure controller after a state transition. */
enum class NearbyLocationCommand {
    NONE,
    REQUEST_COARSE_PERMISSION,
    ACQUIRE_SINGLE_FIX,
}

/**
 * Pure state machine for the explicit, foreground-only nearby flow.
 *
 * Android APIs live in [NearbyLocationCoordinator]; this class is intentionally
 * deterministic so permission, retry, timeout, fallback, and cancellation logic
 * can be covered by local JVM tests.
 */
class NearbyLocationController {
    var state: NearbyLocationState = NearbyLocationState.Idle
        private set

    fun beginExplicitRequest(
        coarsePermissionGranted: Boolean,
        locationAvailable: Boolean,
    ): NearbyLocationCommand = when {
        !coarsePermissionGranted -> transition(
            NearbyLocationState.AwaitingPermission,
            NearbyLocationCommand.REQUEST_COARSE_PERMISSION,
        )

        !locationAvailable -> transition(
            NearbyLocationState.LocationDisabled,
            NearbyLocationCommand.NONE,
        )

        else -> transition(
            NearbyLocationState.Locating,
            NearbyLocationCommand.ACQUIRE_SINGLE_FIX,
        )
    }

    fun completePermissionRequest(
        granted: Boolean,
        locationAvailable: Boolean,
    ): NearbyLocationCommand {
        if (state != NearbyLocationState.AwaitingPermission) {
            return NearbyLocationCommand.NONE
        }
        return when {
            !granted -> transition(
                NearbyLocationState.PermissionDenied,
                NearbyLocationCommand.NONE,
            )

            !locationAvailable -> transition(
                NearbyLocationState.LocationDisabled,
                NearbyLocationCommand.NONE,
            )

            else -> transition(
                NearbyLocationState.Locating,
                NearbyLocationCommand.ACQUIRE_SINGLE_FIX,
            )
        }
    }

    /**
     * Restores only the non-sensitive fact that an Android permission result is pending.
     * Location coordinates and provider work are never restored.
     */
    fun restoreAwaitingPermission(): Boolean {
        if (state != NearbyLocationState.Idle) return false
        state = NearbyLocationState.AwaitingPermission
        return true
    }

    fun completeWithFix(fix: NearbyLocationFix): Boolean {
        if (state != NearbyLocationState.Locating) return false
        state = NearbyLocationState.Located(fix)
        return true
    }

    fun completeTimeout(lastKnownFix: NearbyLocationFix?): Boolean = finishAcquisition(
        fallback = lastKnownFix?.copy(source = NearbyLocationFixSource.LAST_KNOWN),
        failure = NearbyLocationState.TimedOut,
    )

    fun completeFailure(lastKnownFix: NearbyLocationFix?): Boolean = finishAcquisition(
        fallback = lastKnownFix?.copy(source = NearbyLocationFixSource.LAST_KNOWN),
        failure = NearbyLocationState.Error,
    )

    fun cancel(): Boolean {
        if (state != NearbyLocationState.Locating &&
            state != NearbyLocationState.AwaitingPermission
        ) {
            return false
        }
        state = NearbyLocationState.Cancelled
        return true
    }

    /** Stops provider work when the app backgrounds without consuming a permission wait. */
    fun cancelActiveAcquisition(): Boolean {
        if (state != NearbyLocationState.Locating) return false
        state = NearbyLocationState.Cancelled
        return true
    }

    fun reset() {
        state = NearbyLocationState.Idle
    }

    private fun finishAcquisition(
        fallback: NearbyLocationFix?,
        failure: NearbyLocationState,
    ): Boolean {
        if (state != NearbyLocationState.Locating) return false
        state = fallback?.let(NearbyLocationState::Located) ?: failure
        return true
    }

    private fun transition(
        nextState: NearbyLocationState,
        command: NearbyLocationCommand,
    ): NearbyLocationCommand {
        state = nextState
        return command
    }
}
