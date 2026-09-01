package com.rupayonhaldar.gtafreestem.platform.navigation

import android.content.Intent
import androidx.compose.runtime.staticCompositionLocalOf
import com.rupayonhaldar.gtafreestem.ui.shell.PrimaryDestination
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlatformNavigationRequest(
    val sequence: Long,
    val destination: PrimaryDestination,
)

/** Retains the cold-start request and emits a new sequence for every warm-link delivery. */
class PlatformNavigationCoordinator {
    private val nextSequence = AtomicLong(0)
    private val _request = MutableStateFlow<PlatformNavigationRequest?>(null)
    val request: StateFlow<PlatformNavigationRequest?> = _request.asStateFlow()

    fun handle(intent: Intent?): Boolean {
        if (intent?.action != Intent.ACTION_VIEW) return false
        return open(AppDeepLink.parse(intent.dataString) ?: return false)
    }

    fun open(destination: PrimaryDestination): Boolean {
        _request.value = PlatformNavigationRequest(
            sequence = nextSequence.incrementAndGet(),
            destination = destination,
        )
        return true
    }
}

/** MainActivity provides this; previews and isolated Compose tests safely receive null. */
val LocalPlatformNavigationCoordinator =
    staticCompositionLocalOf<PlatformNavigationCoordinator?> { null }
