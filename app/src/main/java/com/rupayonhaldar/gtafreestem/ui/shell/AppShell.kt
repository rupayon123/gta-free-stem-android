package com.rupayonhaldar.gtafreestem.ui.shell

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rupayonhaldar.gtafreestem.R

enum class PrimaryDestination(
    val catalogKey: String,
    val fallbackLabel: String,
    @param:DrawableRes val iconResource: Int,
) {
    HOME("home", "Home", R.drawable.ic_home),
    OPPORTUNITIES("navOpportunities", "Opportunities", R.drawable.ic_search),
    HIGH_SCHOOL("highSchool", "High School", R.drawable.ic_school),
    SUPPORT("support", "Support", R.drawable.ic_support),
    ACCOUNT("account", "Account", R.drawable.ic_account),
}

@Composable
fun AdaptiveAppShell(
    selectedDestination: PrimaryDestination,
    onDestinationSelected: (PrimaryDestination) -> Unit,
    showNavigation: Boolean,
    modifier: Modifier = Modifier,
    destinationLabel: (PrimaryDestination) -> String = PrimaryDestination::fallbackLabel,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showNavigation && !useNavigationRail) {
                    PrimaryNavigationBar(
                        selectedDestination = selectedDestination,
                        onDestinationSelected = onDestinationSelected,
                        destinationLabel = destinationLabel,
                    )
                }
            },
        ) { innerPadding ->
            if (showNavigation && useNavigationRail) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
                ) {
                    PrimaryNavigationRail(
                        selectedDestination = selectedDestination,
                        onDestinationSelected = onDestinationSelected,
                        destinationLabel = destinationLabel,
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        content()
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun PrimaryNavigationBar(
    selectedDestination: PrimaryDestination,
    onDestinationSelected: (PrimaryDestination) -> Unit,
    destinationLabel: (PrimaryDestination) -> String,
) {
    NavigationBar(
        modifier = Modifier
            .testTag("primary-navigation-bar")
            .fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        windowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
        ),
    ) {
        PrimaryDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selectedDestination == destination,
                onClick = { onDestinationSelected(destination) },
                modifier = Modifier.testTag(destination.testTag),
                icon = {
                    Icon(
                        painter = painterResource(destination.iconResource),
                        contentDescription = null,
                    )
                },
                label = {
                    DestinationLabel(
                        label = destinationLabel(destination),
                        compact = true,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                ),
            )
        }
    }
}

@Composable
private fun PrimaryNavigationRail(
    selectedDestination: PrimaryDestination,
    onDestinationSelected: (PrimaryDestination) -> Unit,
    destinationLabel: (PrimaryDestination) -> String,
) {
    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag("primary-navigation-rail"),
        containerColor = MaterialTheme.colorScheme.surface,
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        PrimaryDestination.entries.forEach { destination ->
            NavigationRailItem(
                selected = selectedDestination == destination,
                onClick = { onDestinationSelected(destination) },
                modifier = Modifier.testTag(destination.testTag),
                icon = {
                    Icon(
                        painter = painterResource(destination.iconResource),
                        contentDescription = null,
                    )
                },
                label = {
                    DestinationLabel(destinationLabel(destination), compact = false)
                },
                alwaysShowLabel = true,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                ),
            )
        }
    }
}

@Composable
private fun DestinationLabel(
    label: String,
    compact: Boolean,
) {
    Text(
        text = label,
        style = if (compact) {
            MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                lineHeight = 14.sp,
            )
        } else {
            MaterialTheme.typography.labelSmall
        },
        maxLines = if (compact) 2 else 1,
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Ellipsis,
    )
}

private val PrimaryDestination.testTag: String
    get() = "primary-navigation-${name.lowercase()}"
