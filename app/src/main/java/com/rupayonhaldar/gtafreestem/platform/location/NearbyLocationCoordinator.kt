package com.rupayonhaldar.gtafreestem.platform.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Activity-facing coordinator for a one-shot, foreground-only coarse location request.
 *
 * Call [onUseNearbyTapped] only from the user's explicit "Use nearby" action. If it
 * returns [NearbyLocationLaunch.REQUEST_COARSE_PERMISSION], launch Android's permission
 * contract for [Manifest.permission.ACCESS_COARSE_LOCATION], then pass its result to
 * [onCoarsePermissionResult]. The resulting fix exists only in [state]; this class never
 * stores or sends it and registers no background or continuous work.
 *
 * Methods are intended to be called from the main thread.
 */
internal class NearbyLocationCoordinator(
    context: Context,
    timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    private val locationManager: LocationManager =
        requireNotNull(context.getSystemService(LocationManager::class.java)),
    private val handler: Handler = Handler(Looper.getMainLooper()),
    private val callbackExecutor: Executor = ContextCompat.getMainExecutor(context),
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
    private val nowElapsedRealtimeMillis: () -> Long = android.os.SystemClock::elapsedRealtime,
) : NearbyLocationOperations {
    private val applicationContext = context.applicationContext
    private val controller = NearbyLocationController()
    private val boundedTimeoutMillis = timeoutMillis.coerceIn(
        MINIMUM_TIMEOUT_MILLIS,
        MAXIMUM_TIMEOUT_MILLIS,
    )
    private val _state = MutableStateFlow<NearbyLocationState>(controller.state)
    override val state: StateFlow<NearbyLocationState> = _state.asStateFlow()

    private var requestGeneration = 0L
    private var timeoutCallback: Runnable? = null
    private var cancellationSignal: CancellationSignal? = null
    private var legacyListener: LocationListener? = null
    private var lastKnownFallback: NearbyLocationFix? = null

    /** Starts the flow only in response to an explicit user action. */
    override fun onUseNearbyTapped(): NearbyLocationLaunch {
        stopPlatformRequest()
        val command = controller.beginExplicitRequest(
            coarsePermissionGranted = hasCoarsePermission(),
            locationAvailable = isLocationAvailable(),
        )
        publishControllerState()
        return execute(command)
    }

    /** Completes the single coarse-permission request launched by the Activity. */
    override fun onCoarsePermissionResult(granted: Boolean): NearbyLocationLaunch {
        val command = controller.completePermissionRequest(
            granted = granted,
            locationAvailable = granted && isLocationAvailable(),
        )
        publishControllerState()
        return execute(command)
    }

    /** Cancels any active permission/acquisition state and releases provider callbacks. */
    fun cancel() {
        stopPlatformRequest()
        if (controller.cancel()) publishControllerState()
    }

    /** Clears an in-memory fix when the user turns Nearby off. */
    override fun clear() {
        stopPlatformRequest()
        controller.reset()
        publishControllerState()
    }

    override fun close() {
        cancel()
    }

    /** Restores only an outstanding permission wait after saved-state recreation. */
    override fun restoreAwaitingPermission(): Boolean {
        val restored = controller.restoreAwaitingPermission()
        if (restored) publishControllerState()
        return restored
    }

    /** Releases provider callbacks on background, while leaving permission UI pending. */
    override fun cancelActiveAcquisition(): Boolean {
        if (controller.state != NearbyLocationState.Locating) return false
        stopPlatformRequest()
        val cancelled = controller.cancelActiveAcquisition()
        if (cancelled) publishControllerState()
        return cancelled
    }

    private fun execute(command: NearbyLocationCommand): NearbyLocationLaunch = when (command) {
        NearbyLocationCommand.NONE -> NearbyLocationLaunch.NONE
        NearbyLocationCommand.REQUEST_COARSE_PERMISSION ->
            NearbyLocationLaunch.REQUEST_COARSE_PERMISSION

        NearbyLocationCommand.ACQUIRE_SINGLE_FIX -> {
            acquireSingleFix()
            NearbyLocationLaunch.NONE
        }
    }

    @SuppressLint("MissingPermission")
    private fun acquireSingleFix() {
        if (!hasCoarsePermission()) {
            completeFailure()
            return
        }

        val provider = activeCoarseProvider()
        if (provider == null) {
            completeFailure()
            return
        }

        stopPlatformRequest()
        val generation = ++requestGeneration
        lastKnownFallback = bestRecentLastKnownFix()
        scheduleTimeout(generation)

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val signal = CancellationSignal()
                cancellationSignal = signal
                locationManager.getCurrentLocation(
                    provider,
                    signal,
                    callbackExecutor,
                ) { location ->
                    if (generation != requestGeneration) return@getCurrentLocation
                    location?.toFix(NearbyLocationFixSource.CURRENT)?.let(::completeWithFix)
                        ?: completeFailure()
                }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (generation != requestGeneration) return
                        location.toFix(NearbyLocationFixSource.CURRENT)?.let(::completeWithFix)
                            ?: completeFailure()
                    }

                    @Deprecated("Required on API levels below 30")
                    override fun onStatusChanged(
                        provider: String?,
                        status: Int,
                        extras: Bundle?,
                    ) = Unit

                    override fun onProviderEnabled(provider: String) = Unit

                    override fun onProviderDisabled(provider: String) {
                        if (generation == requestGeneration) completeFailure()
                    }
                }
                legacyListener = listener
                @Suppress("DEPRECATION")
                locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            }
        }.onFailure {
            completeFailure()
        }
    }

    private fun scheduleTimeout(generation: Long) {
        val callback = Runnable {
            if (generation != requestGeneration) return@Runnable
            val fallback = lastKnownFallback
            stopPlatformRequest()
            if (controller.completeTimeout(fallback)) publishControllerState()
        }
        timeoutCallback = callback
        handler.postDelayed(callback, boundedTimeoutMillis)
    }

    private fun completeWithFix(fix: NearbyLocationFix) {
        stopPlatformRequest()
        if (controller.completeWithFix(fix)) publishControllerState()
    }

    private fun completeFailure() {
        val fallback = lastKnownFallback
        stopPlatformRequest()
        if (controller.completeFailure(fallback)) publishControllerState()
    }

    private fun stopPlatformRequest() {
        requestGeneration += 1L
        timeoutCallback?.let(handler::removeCallbacks)
        timeoutCallback = null
        cancellationSignal?.cancel()
        cancellationSignal = null
        legacyListener?.let { listener ->
            runCatching { locationManager.removeUpdates(listener) }
        }
        legacyListener = null
        lastKnownFallback = null
    }

    private fun publishControllerState() {
        _state.value = controller.state
    }

    private fun hasCoarsePermission(): Boolean =
        ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    private fun isLocationAvailable(): Boolean = runCatching {
        val globallyEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            (
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)
                )
        }
        globallyEnabled && activeCoarseProvider() != null
    }.getOrDefault(false)

    private fun activeCoarseProvider(): String? = COARSE_PROVIDERS.firstOrNull { provider ->
        runCatching {
            locationManager.allProviders.contains(provider) &&
                locationManager.isProviderEnabled(provider)
        }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    private fun bestRecentLastKnownFix(): NearbyLocationFix? {
        if (!hasCoarsePermission()) return null
        val nowElapsed = nowElapsedRealtimeMillis()
        return COARSE_PROVIDERS.mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }.filter { location ->
            val capturedElapsedMillis = location.elapsedRealtimeNanos / NANOS_PER_MILLISECOND
            val ageMillis = (nowElapsed - capturedElapsedMillis).coerceAtLeast(0L)
            ageMillis <= MAXIMUM_LAST_KNOWN_AGE_MILLIS
        }.sortedWith(
            compareByDescending<Location> { it.elapsedRealtimeNanos }
                .thenBy { if (it.hasAccuracy()) it.accuracy else Float.MAX_VALUE },
        ).firstOrNull()?.toFix(NearbyLocationFixSource.LAST_KNOWN)
    }

    private fun Location.toFix(source: NearbyLocationFixSource): NearbyLocationFix? {
        if (!latitude.isFinite() || latitude !in -90.0..90.0) return null
        if (!longitude.isFinite() || longitude !in -180.0..180.0) return null
        val normalizedAccuracy = accuracy
            .takeIf { hasAccuracy() && it.isFinite() && it >= 0f }
        val timestamp = time.takeIf { it >= 0L } ?: nowEpochMillis()
        return NearbyLocationFix(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = normalizedAccuracy,
            capturedAtEpochMillis = timestamp,
            source = source,
        )
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 8_000L
        const val MINIMUM_TIMEOUT_MILLIS = 1_000L
        const val MAXIMUM_TIMEOUT_MILLIS = 15_000L
        const val MAXIMUM_LAST_KNOWN_AGE_MILLIS = 30L * 60L * 1_000L
        private const val NANOS_PER_MILLISECOND = 1_000_000L
        private val COARSE_PROVIDERS = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
    }
}

/** Result that tells MainActivity whether Android's coarse-permission UI is needed. */
enum class NearbyLocationLaunch {
    NONE,
    REQUEST_COARSE_PERMISSION,
}
