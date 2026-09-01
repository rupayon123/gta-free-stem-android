package com.rupayonhaldar.gtafreestem.platform.location

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.io.Closeable
import kotlinx.coroutines.flow.StateFlow

/** Narrow seam that keeps saved-state policy testable without Android location services. */
internal interface NearbyLocationOperations : Closeable {
    val state: StateFlow<NearbyLocationState>

    fun onUseNearbyTapped(): NearbyLocationLaunch

    fun onCoarsePermissionResult(granted: Boolean): NearbyLocationLaunch

    fun restoreAwaitingPermission(): Boolean

    fun cancelActiveAcquisition(): Boolean

    fun clear()
}

/**
 * Owns the foreground-only location session across Activity recreation.
 *
 * The saved state contains only a permission-request flag. Coordinates remain exclusively in the
 * coordinator's in-memory [state] and are never written to a Bundle, disk, or a network service.
 */
class NearbyLocationViewModel internal constructor(
    private val savedStateHandle: SavedStateHandle,
    private val coordinator: NearbyLocationOperations,
) : ViewModel() {
    val state: StateFlow<NearbyLocationState> = coordinator.state
    private var isPermissionRequestPending: Boolean
        get() = savedStateHandle[PERMISSION_REQUEST_PENDING_KEY] ?: false
        set(value) {
            savedStateHandle[PERMISSION_REQUEST_PENDING_KEY] = value
        }

    init {
        if (isPermissionRequestPending && !coordinator.restoreAwaitingPermission()) {
            isPermissionRequestPending = false
        }
    }

    /** Starts at most one Android permission request for the current explicit user action. */
    fun onUseNearbyTapped(): NearbyLocationLaunch {
        if (isPermissionRequestPending) return NearbyLocationLaunch.NONE
        return coordinator.onUseNearbyTapped().also { launch ->
            isPermissionRequestPending =
                launch == NearbyLocationLaunch.REQUEST_COARSE_PERMISSION
        }
    }

    /** Consumes exactly one expected result; duplicate or stale callbacks are ignored. */
    fun onCoarsePermissionResult(granted: Boolean): NearbyLocationLaunch {
        if (!isPermissionRequestPending) return NearbyLocationLaunch.NONE
        isPermissionRequestPending = false
        return coordinator.onCoarsePermissionResult(granted)
    }

    /** A true background transition stops only active provider work, not permission UI. */
    fun onAppBackgrounded() {
        coordinator.cancelActiveAcquisition()
    }

    fun clear() {
        isPermissionRequestPending = false
        coordinator.clear()
    }

    override fun onCleared() {
        isPermissionRequestPending = false
        coordinator.close()
        super.onCleared()
    }

    companion object {
        internal const val PERMISSION_REQUEST_PENDING_KEY =
            "nearby_location_permission_request_pending"

        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer {
                    NearbyLocationViewModel(
                        savedStateHandle = createSavedStateHandle(),
                        coordinator = NearbyLocationCoordinator(appContext),
                    )
                }
            }
        }
    }
}
