package com.rupayonhaldar.gtafreestem

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.LayoutDirection as ComposeLayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rupayonhaldar.gtafreestem.data.local.AppThemePreference
import com.rupayonhaldar.gtafreestem.localization.TextDirection
import com.rupayonhaldar.gtafreestem.platform.alerts.OpportunityAlertPreferenceController
import com.rupayonhaldar.gtafreestem.platform.alerts.OpportunityAlertPreferenceUpdate
import com.rupayonhaldar.gtafreestem.platform.alerts.OpportunityNotificationPublisher
import com.rupayonhaldar.gtafreestem.platform.alerts.WorkManagerOpportunityAlertSchedule
import com.rupayonhaldar.gtafreestem.platform.navigation.AppShortcutPublisher
import com.rupayonhaldar.gtafreestem.platform.navigation.LocalPlatformNavigationCoordinator
import com.rupayonhaldar.gtafreestem.platform.navigation.PlatformNavigationCoordinator
import com.rupayonhaldar.gtafreestem.platform.location.NearbyLocationLaunch
import com.rupayonhaldar.gtafreestem.platform.location.NearbyLocationViewModel
import com.rupayonhaldar.gtafreestem.theme.GTAFreeStemTheme
import com.rupayonhaldar.gtafreestem.ui.preferences.AppPreferencesViewModel
import com.rupayonhaldar.gtafreestem.ui.preferences.OpportunityAlertFeedback

class MainActivity : ComponentActivity() {
    internal val platformNavigationCoordinator = PlatformNavigationCoordinator()
    internal val nearbyLocationViewModel: NearbyLocationViewModel by viewModels {
        NearbyLocationViewModel.factory(applicationContext)
    }
    internal val preferencesViewModel: AppPreferencesViewModel by viewModels {
        AppPreferencesViewModel.factory(applicationContext)
    }
    private val opportunityAlertController by lazy {
        OpportunityAlertPreferenceController(
            readPreferred = {
                preferencesViewModel.uiState.value.opportunityAlertsPreferred
            },
            writePreferred = preferencesViewModel::setOpportunityAlertsPreferred,
            hasNotificationPermission = {
                OpportunityNotificationPublisher.hasRuntimePermission(applicationContext)
            },
            canPostNotifications = {
                OpportunityNotificationPublisher.canPost(applicationContext)
            },
            workSchedule = WorkManagerOpportunityAlertSchedule(applicationContext),
        )
    }
    private var notificationPermissionRequestInFlight = false
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationPermissionRequestInFlight = false
        val applied = opportunityAlertController.completePermissionRequest(granted)
        alertFeedbackForPermissionResult(granted, applied)?.let(
            preferencesViewModel::reportOpportunityAlertFeedback,
        )
        preferencesViewModel.reload()
    }
    private val coarseLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        nearbyLocationViewModel.onCoarsePermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            AppShortcutPublisher.reportUsageIfShortcut(applicationContext, intent)
            platformNavigationCoordinator.handle(intent)
        }
        OpportunityNotificationPublisher.ensureChannel(applicationContext)
        opportunityAlertController.reconcile()
        preferencesViewModel.refreshSystemLanguage()
        setContent {
            val preferences by preferencesViewModel.uiState.collectAsStateWithLifecycle()
            val nearbyLocationState by nearbyLocationViewModel.state.collectAsStateWithLifecycle()
            val darkTheme = preferences.theme.resolveDarkTheme(isSystemInDarkTheme())
            LaunchedEffect(preferences.resolvedLanguage) {
                OpportunityNotificationPublisher.ensureChannel(
                    context = applicationContext,
                    localizedName = preferences.text("alerts"),
                )
                AppShortcutPublisher.publish(
                    context = applicationContext,
                    catalog = preferences.catalog,
                    language = preferences.resolvedLanguage,
                )
            }

            CompositionLocalProvider(
                LocalLayoutDirection provides preferences.textDirection.toLayoutDirection(),
                LocalPlatformNavigationCoordinator provides platformNavigationCoordinator,
            ) {
                GTAFreeStemTheme(darkTheme = darkTheme) {
                    GTAFreeStemApp(
                        preferences = preferences,
                        nearbyLocationState = nearbyLocationState,
                        onUseNearby = ::useNearby,
                        onClearNearby = nearbyLocationViewModel::clear,
                        preferenceActions = AppPreferenceActions(
                            saveDisplayName = preferencesViewModel::saveDisplayName,
                            clearProfile = preferencesViewModel::clearProfile,
                            selectLanguage = preferencesViewModel::setLanguage,
                            selectTheme = preferencesViewModel::setTheme,
                            setOpportunityAlertsPreferred =
                                ::setOpportunityAlertsPreferred,
                            refreshPreferenceState = preferencesViewModel::reload,
                            opportunityAlertFeedback =
                                preferencesViewModel.opportunityAlertFeedback,
                        ),
                    )
                }
            }
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AppShortcutPublisher.reportUsageIfShortcut(applicationContext, intent)
        platformNavigationCoordinator.handle(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!notificationPermissionRequestInFlight) {
            opportunityAlertController.reconcile()
            preferencesViewModel.reload()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        preferencesViewModel.refreshSystemLanguage()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            nearbyLocationViewModel.onAppBackgrounded()
        }
    }

    private fun useNearby() {
        if (nearbyLocationViewModel.onUseNearbyTapped() !=
            NearbyLocationLaunch.REQUEST_COARSE_PERMISSION
        ) {
            return
        }
        runCatching {
            coarseLocationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }.onFailure {
            nearbyLocationViewModel.onCoarsePermissionResult(granted = false)
        }
    }

    private fun setOpportunityAlertsPreferred(preferred: Boolean): Boolean =
        when (opportunityAlertController.update(preferred)) {
            OpportunityAlertPreferenceUpdate.APPLIED -> {
                preferencesViewModel.reload()
                true
            }

            OpportunityAlertPreferenceUpdate.REQUEST_PERMISSION -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    opportunityAlertController.completePermissionRequest(granted = false)
                    preferencesViewModel.reportOpportunityAlertFeedback(
                        OpportunityAlertFeedback.UNAVAILABLE,
                    )
                    preferencesViewModel.reload()
                    false
                } else if (notificationPermissionRequestInFlight) {
                    true
                } else {
                    notificationPermissionRequestInFlight = true
                    runCatching {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }.fold(
                        onSuccess = { true },
                        onFailure = {
                            notificationPermissionRequestInFlight = false
                            opportunityAlertController.completePermissionRequest(granted = false)
                            preferencesViewModel.reportOpportunityAlertFeedback(
                                OpportunityAlertFeedback.UNAVAILABLE,
                            )
                            preferencesViewModel.reload()
                            false
                        },
                    )
                }
            }

            OpportunityAlertPreferenceUpdate.FAILED -> {
                alertFeedbackForSynchronousUpdate(preferred, OpportunityAlertPreferenceUpdate.FAILED)
                    ?.let(preferencesViewModel::reportOpportunityAlertFeedback)
                preferencesViewModel.reload()
                false
            }
        }
}

internal fun AppThemePreference.resolveDarkTheme(systemDarkTheme: Boolean): Boolean = when (this) {
    AppThemePreference.SYSTEM -> systemDarkTheme
    AppThemePreference.LIGHT -> false
    AppThemePreference.DARK -> true
}

internal fun TextDirection.toLayoutDirection(): ComposeLayoutDirection = when (this) {
    TextDirection.LEFT_TO_RIGHT -> ComposeLayoutDirection.Ltr
    TextDirection.RIGHT_TO_LEFT -> ComposeLayoutDirection.Rtl
}

internal fun alertFeedbackForPermissionResult(
    granted: Boolean,
    applied: Boolean,
): OpportunityAlertFeedback? = when {
    !granted -> OpportunityAlertFeedback.PERMISSION_DENIED
    !applied -> OpportunityAlertFeedback.UNAVAILABLE
    else -> null
}

internal fun alertFeedbackForSynchronousUpdate(
    preferred: Boolean,
    update: OpportunityAlertPreferenceUpdate,
): OpportunityAlertFeedback? = when {
    update != OpportunityAlertPreferenceUpdate.FAILED -> null
    preferred -> OpportunityAlertFeedback.UNAVAILABLE
    else -> OpportunityAlertFeedback.SAVE_FAILED
}
