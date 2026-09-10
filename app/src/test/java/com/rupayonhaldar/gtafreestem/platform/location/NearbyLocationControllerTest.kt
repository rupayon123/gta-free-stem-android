package com.rupayonhaldar.gtafreestem.platform.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyLocationControllerTest {
    @Test
    fun explicitRequestWithoutPermissionRequestsCoarsePermission() {
        val controller = NearbyLocationController()

        val command = controller.beginExplicitRequest(
            coarsePermissionGranted = false,
            locationAvailable = false,
        )

        assertEquals(NearbyLocationCommand.REQUEST_COARSE_PERMISSION, command)
        assertEquals(NearbyLocationState.AwaitingPermission, controller.state)
    }

    @Test
    fun permissionDenialProducesTerminalDenialState() {
        val controller = NearbyLocationController()
        controller.beginExplicitRequest(
            coarsePermissionGranted = false,
            locationAvailable = true,
        )

        val command = controller.completePermissionRequest(
            granted = false,
            locationAvailable = true,
        )

        assertEquals(NearbyLocationCommand.NONE, command)
        assertEquals(NearbyLocationState.PermissionDenied, controller.state)
    }

    @Test
    fun grantedPermissionStartsSingleFix() {
        val controller = NearbyLocationController()
        controller.beginExplicitRequest(
            coarsePermissionGranted = false,
            locationAvailable = true,
        )

        val command = controller.completePermissionRequest(
            granted = true,
            locationAvailable = true,
        )

        assertEquals(NearbyLocationCommand.ACQUIRE_SINGLE_FIX, command)
        assertEquals(NearbyLocationState.Locating, controller.state)
    }

    @Test
    fun disabledLocationDoesNotStartProviderWork() {
        val controller = NearbyLocationController()

        val command = controller.beginExplicitRequest(
            coarsePermissionGranted = true,
            locationAvailable = false,
        )

        assertEquals(NearbyLocationCommand.NONE, command)
        assertEquals(NearbyLocationState.LocationDisabled, controller.state)
    }

    @Test
    fun currentFixCompletesOnlyAnActiveAcquisition() {
        val controller = NearbyLocationController()
        val fix = fix(source = NearbyLocationFixSource.CURRENT)

        assertFalse(controller.completeWithFix(fix))
        controller.beginExplicitRequest(
            coarsePermissionGranted = true,
            locationAvailable = true,
        )

        assertTrue(controller.completeWithFix(fix))
        assertEquals(NearbyLocationState.Located(fix), controller.state)
        assertFalse(controller.completeWithFix(fix.copy(latitude = 44.0)))
    }

    @Test
    fun timeoutUsesRecentLastKnownFallbackAndMarksItsSource() {
        val controller = locatingController()
        val fallback = fix(source = NearbyLocationFixSource.CURRENT)

        assertTrue(controller.completeTimeout(fallback))

        assertEquals(
            NearbyLocationState.Located(
                fallback.copy(source = NearbyLocationFixSource.LAST_KNOWN),
            ),
            controller.state,
        )
    }

    @Test
    fun timeoutWithoutFallbackIsVisible() {
        val controller = locatingController()

        assertTrue(controller.completeTimeout(lastKnownFix = null))

        assertEquals(NearbyLocationState.TimedOut, controller.state)
    }

    @Test
    fun providerFailureUsesFallbackOrReportsError() {
        val withFallback = locatingController()
        val fallback = fix(source = NearbyLocationFixSource.LAST_KNOWN)
        assertTrue(withFallback.completeFailure(fallback))
        assertEquals(NearbyLocationState.Located(fallback), withFallback.state)

        val withoutFallback = locatingController()
        assertTrue(withoutFallback.completeFailure(lastKnownFix = null))
        assertEquals(NearbyLocationState.Error, withoutFallback.state)
    }

    @Test
    fun activeFlowCanBeCancelledAndRetried() {
        val controller = locatingController()

        assertTrue(controller.cancel())
        assertEquals(NearbyLocationState.Cancelled, controller.state)
        assertFalse(controller.cancel())

        assertEquals(
            NearbyLocationCommand.ACQUIRE_SINGLE_FIX,
            controller.beginExplicitRequest(
                coarsePermissionGranted = true,
                locationAvailable = true,
            ),
        )
        assertEquals(NearbyLocationState.Locating, controller.state)
    }

    @Test
    fun backgroundCancellationStopsOnlyAnActiveLocationAcquisition() {
        val awaitingPermission = NearbyLocationController().apply {
            beginExplicitRequest(
                coarsePermissionGranted = false,
                locationAvailable = true,
            )
        }

        assertFalse(awaitingPermission.cancelActiveAcquisition())
        assertEquals(NearbyLocationState.AwaitingPermission, awaitingPermission.state)

        val locating = locatingController()
        assertTrue(locating.cancelActiveAcquisition())
        assertEquals(NearbyLocationState.Cancelled, locating.state)
    }

    @Test
    fun savedPermissionWaitCanOnlyRestoreAnIdleController() {
        val controller = NearbyLocationController()

        assertTrue(controller.restoreAwaitingPermission())
        assertEquals(NearbyLocationState.AwaitingPermission, controller.state)
        assertFalse(controller.restoreAwaitingPermission())

        controller.reset()
        controller.beginExplicitRequest(
            coarsePermissionGranted = true,
            locationAvailable = false,
        )
        assertFalse(controller.restoreAwaitingPermission())
        assertEquals(NearbyLocationState.LocationDisabled, controller.state)
    }

    @Test
    fun stalePermissionCallbackCannotStartLocation() {
        val controller = NearbyLocationController()

        val command = controller.completePermissionRequest(
            granted = true,
            locationAvailable = true,
        )

        assertEquals(NearbyLocationCommand.NONE, command)
        assertEquals(NearbyLocationState.Idle, controller.state)
    }

    @Test
    fun fixRejectsInvalidCoordinatesAndAccuracy() {
        assertThrows(IllegalArgumentException::class.java) {
            fix(source = NearbyLocationFixSource.CURRENT).copy(latitude = 91.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            fix(source = NearbyLocationFixSource.CURRENT).copy(longitude = Double.NaN)
        }
        assertThrows(IllegalArgumentException::class.java) {
            fix(source = NearbyLocationFixSource.CURRENT).copy(accuracyMeters = -1f)
        }
    }

    private fun locatingController() = NearbyLocationController().apply {
        beginExplicitRequest(
            coarsePermissionGranted = true,
            locationAvailable = true,
        )
    }

    private fun fix(source: NearbyLocationFixSource) = NearbyLocationFix(
        latitude = 43.6532,
        longitude = -79.3832,
        accuracyMeters = 1_000f,
        capturedAtEpochMillis = 1_725_000_000_000L,
        source = source,
    )
}
