package com.rupayonhaldar.gtafreestem

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.LayoutDirection as ComposeLayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rupayonhaldar.gtafreestem.data.local.AppThemePreference
import com.rupayonhaldar.gtafreestem.localization.TextDirection
import com.rupayonhaldar.gtafreestem.theme.GTAFreeStemTheme
import com.rupayonhaldar.gtafreestem.ui.preferences.AppPreferencesViewModel

class MainActivity : ComponentActivity() {
    internal val preferencesViewModel: AppPreferencesViewModel by viewModels {
        AppPreferencesViewModel.factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        preferencesViewModel.refreshSystemLanguage()
        setContent {
            val preferences by preferencesViewModel.uiState.collectAsStateWithLifecycle()
            val darkTheme = preferences.theme.resolveDarkTheme(isSystemInDarkTheme())

            CompositionLocalProvider(
                LocalLayoutDirection provides preferences.textDirection.toLayoutDirection(),
            ) {
                GTAFreeStemTheme(darkTheme = darkTheme) {
                    GTAFreeStemApp(
                        preferences = preferences,
                        preferenceActions = AppPreferenceActions(
                            saveDisplayName = preferencesViewModel::saveDisplayName,
                            clearProfile = preferencesViewModel::clearProfile,
                            selectLanguage = preferencesViewModel::setLanguage,
                            selectTheme = preferencesViewModel::setTheme,
                            setOpportunityAlertsPreferred =
                                preferencesViewModel::setOpportunityAlertsPreferred,
                        ),
                    )
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        preferencesViewModel.refreshSystemLanguage()
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
