package com.rupayonhaldar.gtafreestem.ui.shell

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rupayonhaldar.gtafreestem.R

enum class PrimaryDestination(
    val catalogKey: String,
    val fallbackLabel: String,
    @param:DrawableRes val iconResource: Int,
) {
    HOME("home", "Home", R.drawable.ic_home),
    OPPORTUNITIES("navOpportunities", "Discover", R.drawable.ic_search),
    HIGH_SCHOOL("highSchool", "School", R.drawable.ic_school),
    SUPPORT("support", "Help", R.drawable.ic_support),
    ACCOUNT("account", "Profile", R.drawable.ic_account),
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
        val useSideNavigation = maxWidth >= 600.dp
        val useDrawer = maxWidth >= 840.dp
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showNavigation && !useSideNavigation) {
                    PrimaryNavigationBar(selectedDestination, onDestinationSelected, destinationLabel)
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding),
            ) {
                if (showNavigation && useSideNavigation) {
                    PrimarySideNavigation(selectedDestination, onDestinationSelected, destinationLabel, useDrawer)
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clipToBounds(), contentAlignment = Alignment.TopCenter) {
                    content()
                }
            }
        }
    }
}

/** Navigation owns only its measured content height. Decoration never sizes the bar. */
@Composable
private fun PrimaryNavigationBar(
    selectedDestination: PrimaryDestination,
    onDestinationSelected: (PrimaryDestination) -> Unit,
    destinationLabel: (PrimaryDestination) -> String,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.testTag("primary-navigation-bar").navigationBarsPadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                PrimaryDestination.entries.forEach { destination ->
                    val selected = selectedDestination == destination
                    Surface(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(
                            modifier = Modifier.testTag(destination.testTag)
                                .selectable(selected = selected, role = Role.Tab, onClick = { onDestinationSelected(destination) })
                                .heightIn(min = 56.dp).padding(horizontal = 2.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            DestinationIcon(destination, selected)
                            Text(
                                text = destinationLabel(destination),
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    lineBreak = LineBreak.Heading,
                                    hyphens = Hyphens.Auto,
                                ),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A scrollable native side list keeps every translated label reachable in landscape. */
@Composable
private fun PrimarySideNavigation(
    selectedDestination: PrimaryDestination,
    onDestinationSelected: (PrimaryDestination) -> Unit,
    destinationLabel: (PrimaryDestination) -> String,
    useDrawer: Boolean,
) {
    val kind = if (useDrawer) "drawer" else "rail"
    Surface(
        modifier = Modifier.width(if (useDrawer) 240.dp else 136.dp).fillMaxHeight().testTag("primary-navigation-$kind"),
        color = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("primary-navigation-$kind-list").selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        ) {
            if (useDrawer) {
                item { Text("GTA FREE STEM", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(8.dp)) }
                item { HorizontalDivider() }
            }
            items(PrimaryDestination.entries, key = { it.name }) { destination ->
                val selected = selectedDestination == destination
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().testTag(destination.testTag)
                            .selectable(selected = selected, role = Role.Tab, onClick = { onDestinationSelected(destination) })
                            .heightIn(min = 64.dp).padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DestinationIcon(destination, selected)
                        Text(
                            text = destinationLabel(destination),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationIcon(destination: PrimaryDestination, selected: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.10f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "destination-icon-${destination.name}",
    )
    Icon(
        painter = painterResource(destination.iconResource),
        contentDescription = null,
        modifier = Modifier.size(24.dp).graphicsLayer { scaleX = scale; scaleY = scale },
    )
}

private val PrimaryDestination.testTag: String
    get() = "primary-navigation-${name.lowercase()}"
