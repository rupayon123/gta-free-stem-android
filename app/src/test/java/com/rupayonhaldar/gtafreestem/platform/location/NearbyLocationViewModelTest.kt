package com.rupayonhaldar.gtafreestem.platform.location

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyLocationViewModelTest {
    @Test
    fun permissionWaitIsSavedAndDuplicateCallbacksCannotStartLocation() {
        val savedState = SavedStateHandle()
        val coordinator = FakeNearbyLocationOperations()
        val viewModel = NearbyLocationViewModel(savedState, coordinator)

        assertEquals(
            NearbyLocationLaunch.REQUEST_COARSE_PERMISSION,
            viewModel.onUseNearbyTapped(),
        )
        assertTrue(savedState[NearbyLocationViewModel.PERMISSION_REQUEST_PENDING_KEY] ?: false)

        assertEquals(NearbyLocationLaunch.NONE, viewModel.onCoarsePermissionResult(granted = true))
        assertEquals(1, coordinator.permissionResultCalls)
        assertFalse(savedState[NearbyLocationViewModel.PERMISSION_REQUEST_PENDING_KEY] ?: true)

        assertEquals(NearbyLocationLaunch.NONE, viewModel.onCoarsePermissionResult(granted = true))
        assertEquals("A stale duplicate callback must be ignored", 1, coordinator.permissionResultCalls)
    }

    @Test
    fun savedPermissionWaitRestoresWithoutRelaunchingThePermissionDialog() {
        val savedState = SavedStateHandle(
            mapOf(NearbyLocationViewModel.PERMISSION_REQUEST_PENDING_KEY to true),
        )
        val coordinator = FakeNearbyLocationOperations()
        val viewModel = NearbyLocationViewModel(savedState, coordinator)

        assertEquals(1, coordinator.restoreCalls)
        assertEquals(NearbyLocationState.AwaitingPermission, viewModel.state.value)
        assertEquals(NearbyLocationLaunch.NONE, viewModel.onUseNearbyTapped())
        assertEquals("The restored request must not launch a second dialog", 0, coordinator.tapCalls)
    }

    @Test
    fun invalidSavedWaitIsClearedSoFutureCallbacksStayStale() {
        val savedState = SavedStateHandle(
            mapOf(NearbyLocationViewModel.PERMISSION_REQUEST_PENDING_KEY to true),
        )
        val coordinator = FakeNearbyLocationOperations(allowRestore = false)
        val viewModel = NearbyLocationViewModel(savedState, coordinator)

        assertFalse(savedState[NearbyLocationViewModel.PERMISSION_REQUEST_PENDING_KEY] ?: true)
        assertEquals(NearbyLocationLaunch.NONE, viewModel.onCoarsePermissionResult(granted = true))
        assertEquals(0, coordinator.permissionResultCalls)
    }

    @Test
    fun backgroundCancellationPreservesPermissionWaitButStopsLocating() {
        val savedState = SavedStateHandle()
        val coordinator = FakeNearbyLocationOperations()
        val viewModel = NearbyLocationViewModel(savedState, coordinator)
        viewModel.onUseNearbyTapped()

        viewModel.onAppBackgrounded()

        assertEquals(NearbyLocationState.AwaitingPermission, viewModel.state.value)
        assertTrue(savedState[NearbyLocationViewModel.PERMISSION_REQUEST_PENDING_KEY] ?: false)
        assertEquals(1, coordinator.backgroundCancellationCalls)

        viewModel.onCoarsePermissionResult(granted = true)
        assertEquals(NearbyLocationState.Locating, viewModel.state.value)

        viewModel.onAppBackgrounded()

        assertEquals(NearbyLocationState.Cancelled, viewModel.state.value)
        assertEquals(2, coordinator.backgroundCancellationCalls)
    }

    @Test
    fun clearingTheLifecycleStoreClosesTheCoordinator() {
        val coordinator = FakeNearbyLocationOperations()
        val viewModel = NearbyLocationViewModel(SavedStateHandle(), coordinator)
        val owner = object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = viewModel as T
        }
        ViewModelProvider(owner, factory)[NearbyLocationViewModel::class.java]

        owner.viewModelStore.clear()

        assertTrue(coordinator.closed)
    }

    private class FakeNearbyLocationOperations(
        private val allowRestore: Boolean = true,
    ) : NearbyLocationOperations {
        private val mutableState = MutableStateFlow<NearbyLocationState>(NearbyLocationState.Idle)
        override val state: StateFlow<NearbyLocationState> = mutableState
        var tapCalls = 0
        var permissionResultCalls = 0
        var restoreCalls = 0
        var backgroundCancellationCalls = 0
        var closed = false

        override fun onUseNearbyTapped(): NearbyLocationLaunch {
            tapCalls += 1
            mutableState.value = NearbyLocationState.AwaitingPermission
            return NearbyLocationLaunch.REQUEST_COARSE_PERMISSION
        }

        override fun onCoarsePermissionResult(granted: Boolean): NearbyLocationLaunch {
            permissionResultCalls += 1
            mutableState.value = if (granted) {
                NearbyLocationState.Locating
            } else {
                NearbyLocationState.PermissionDenied
            }
            return NearbyLocationLaunch.NONE
        }

        override fun restoreAwaitingPermission(): Boolean {
            restoreCalls += 1
            if (!allowRestore) return false
            mutableState.value = NearbyLocationState.AwaitingPermission
            return true
        }

        override fun cancelActiveAcquisition(): Boolean {
            backgroundCancellationCalls += 1
            if (mutableState.value != NearbyLocationState.Locating) return false
            mutableState.value = NearbyLocationState.Cancelled
            return true
        }

        override fun clear() {
            mutableState.value = NearbyLocationState.Idle
        }

        override fun close() {
            closed = true
        }
    }
}
