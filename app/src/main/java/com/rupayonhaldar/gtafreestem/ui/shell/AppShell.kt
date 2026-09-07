package com.rupayonhaldar.gtafreestem.ui.shell

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
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
    Surface(
        modifier = Modifier
            .testTag("primary-navigation-bar")
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.surfaceVariant),
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            tonalElevation = 0.dp,
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
                            isSelected = selectedDestination == destination,
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
}

@Composable
private fun PrimaryNavigationRail(
    selectedDestination: PrimaryDestination,
    onDestinationSelected: (PrimaryDestination) -> Unit,
    destinationLabel: (PrimaryDestination) -> String,
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(min = 84.dp)
            .padding(8.dp)
            .testTag("primary-navigation-rail"),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 6.dp,
        shadowElevation = 7.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.surfaceVariant),
    ) {
        NavigationRail(
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 8.dp, bottom = 8.dp)
                .clip(RoundedCornerShape(20.dp)),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
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
                    DestinationLabel(
                        destinationLabel(destination),
                        compact = false,
                        isSelected = selectedDestination == destination,
                    )
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
}

@Composable
private fun DestinationLabel(
    label: String,
    compact: Boolean,
    isSelected: Boolean = false,
) {
    val resolvedStyle = if (compact) {
        MaterialTheme.typography.labelSmall.copy(
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    } else {
        MaterialTheme.typography.labelSmall.copy(
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
        )
    }
    Text(
        text = label,
        style = resolvedStyle,
        maxLines = if (compact) 2 else 1,
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Ellipsis,
    )
}

private val PrimaryDestination.testTag: String
    get() = "primary-navigation-${name.lowercase()}"
