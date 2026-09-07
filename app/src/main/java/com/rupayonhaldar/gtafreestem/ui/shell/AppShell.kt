package com.rupayonhaldar.gtafreestem.ui.shell

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val barShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val barSurface = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    }
    val barBorder = if (isDark) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    val barBottomGlow = if (isDark) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    }
    val barContainer = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.70f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
    }

    Surface(
        modifier = Modifier
            .testTag("primary-navigation-bar")
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = barShape,
        color = barSurface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(0.5.dp, barBorder),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(barShape)
                .shadow(
                    elevation = if (isDark) 8.dp else 6.dp,
                    shape = barShape,
                    ambientColor = barBottomGlow,
                    spotColor = barBottomGlow,
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.08f else 0.04f),
                        ),
                    ),
                ),
        )
        NavigationBar(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .clip(barShape),
            containerColor = barContainer,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
            ),
        ) {
            PrimaryDestination.entries.forEach { destination ->
                    val destinationInteractionSource = remember {
                        MutableInteractionSource()
                    }
                    val isSelected = selectedDestination == destination
                    val isPressed by destinationInteractionSource.collectIsPressedAsState()
                    val selectedIndicatorColor by animateColorAsState(
                        targetValue = when {
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            else -> Color.Transparent
                        },
                        animationSpec = if (isPressed) {
                            navPressColorAnimationSpec
                        } else {
                            navSelectionColorAnimationSpec
                        },
                        label = "bottom-nav-indicator-color-${destination.name}",
                    )
                    val selectedPillColor by animateColorAsState(
                        targetValue = when {
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else -> Color.Transparent
                        },
                        animationSpec = if (isPressed) {
                            navPressColorAnimationSpec
                        } else {
                            navSelectionColorAnimationSpec
                        },
                        label = "bottom-nav-pill-color-${destination.name}",
                    )
                    val selectedPillTopColor by animateColorAsState(
                        targetValue = when {
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            else -> Color.Transparent
                        },
                        animationSpec = if (isPressed) {
                            navPressColorAnimationSpec
                        } else {
                            navSelectionColorAnimationSpec
                        },
                        label = "bottom-nav-pill-top-color-${destination.name}",
                    )
                    val selectedPillBorderColor by animateColorAsState(
                        targetValue = when {
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else -> Color.Transparent
                        },
                        animationSpec = if (isPressed) {
                            navPressColorAnimationSpec
                        } else {
                            navSelectionColorAnimationSpec
                        },
                        label = "bottom-nav-pill-border-color-${destination.name}",
                    )
                    val selectedPillElevation by animateDpAsState(
                        targetValue = when {
                            isSelected -> 2.6.dp
                            isPressed -> 1.4.dp
                            else -> 0.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectionDpAnimationSpec
                        },
                        label = "bottom-nav-pill-elevation-${destination.name}",
                    )
                    val activeNavItemWidth by animateDpAsState(
                        targetValue = when {
                            isSelected -> 74.dp
                            isPressed -> 64.dp
                            else -> 56.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectionDpAnimationSpec
                        },
                        label = "bottom-nav-item-width-${destination.name}",
                    )
                    val activeItemCorner by animateDpAsState(
                        targetValue = when {
                            isSelected -> 22.dp
                            isPressed -> 20.dp
                            else -> 18.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectionDpAnimationSpec
                        },
                        label = "bottom-nav-item-corner-${destination.name}",
                    )
                    val activeItemShape = RoundedCornerShape(activeItemCorner)
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onDestinationSelected(destination) },
                        interactionSource = destinationInteractionSource,
                        modifier = Modifier
                            .testTag(destination.testTag)
                            .padding(horizontal = 4.dp, top = 2.dp)
                            .clip(activeItemShape)
                            .shadow(
                                elevation = selectedPillElevation,
                                shape = activeItemShape,
                                ambientColor = selectedPillBorderColor.copy(alpha = if (isDark) 0.26f else 0.16f),
                                spotColor = selectedPillColor,
                            )
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        selectedPillTopColor,
                                        selectedPillColor,
                                    ),
                                ),
                                shape = activeItemShape,
                            )
                            .border(
                                border = BorderStroke(
                                    width = if (isSelected || isPressed) 0.9.dp else 0.dp,
                                    color = selectedPillBorderColor,
                                ),
                                shape = activeItemShape,
                            )
                            .sizeIn(
                                minWidth = activeNavItemWidth,
                                minHeight = 56.dp,
                            ),
                    icon = {
                        val iconLift by animateDpAsState(
                            targetValue = when {
                                isPressed -> (-1).dp
                                isSelected -> (-2).dp
                                else -> 0.dp
                            },
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                            } else {
                                navSelectionDpAnimationSpec
                            },
                            label = "bottom-nav-icon-lift-${destination.name}",
                        )
                        val iconElevation by animateFloatAsState(
                            targetValue = when {
                                isPressed -> 2f
                                isSelected -> 5f
                                else -> 0f
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                            } else {
                                navSelectionFloatAnimationSpec
                            },
                            label = "bottom-nav-icon-elevation-${destination.name}",
                        )
                        val iconContainerScale by animateFloatAsState(
                            targetValue = when {
                                isPressed -> 1.03f
                                isSelected -> 1.05f
                                else -> 1f
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                            } else {
                                navSelectionFloatAnimationSpec
                            },
                            label = "bottom-nav-icon-container-scale-${destination.name}",
                        )
                        val iconScale by animateFloatAsState(
                            targetValue = when {
                                isPressed -> 0.98f
                                isSelected -> 1.04f
                                else -> 1f
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                            } else {
                                navSelectionFloatAnimationSpec
                            },
                            label = "bottom-nav-icon-scale-${destination.name}",
                        )
                        val iconFaceSize by animateDpAsState(
                            targetValue = when {
                                isPressed -> 33.dp
                                isSelected -> 35.dp
                                else -> 34.dp
                            },
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                            } else {
                                navSelectionDpAnimationSpec
                            },
                            label = "bottom-nav-icon-face-size-${destination.name}",
                        )
                        val iconBackground by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                            } else {
                                navSelectionColorAnimationSpec
                            },
                            label = "bottom-nav-icon-background-${destination.name}",
                        )
                        val iconFaceGlow by animateColorAsState(
                            targetValue = when {
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)
                                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                else -> Color.Transparent
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                            } else {
                                navSelectionColorAnimationSpec
                            },
                            label = "bottom-nav-icon-face-glow-${destination.name}",
                        )
                        val iconTint by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                            } else {
                                navSelectionColorAnimationSpec
                            },
                            label = "bottom-nav-icon-tint-${destination.name}",
                        )
                        val iconHalo by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                            } else {
                                navSelectionColorAnimationSpec
                            },
                            label = "bottom-nav-icon-halo-${destination.name}",
                        )
                        val iconHaloSize by animateDpAsState(
                            targetValue = if (isSelected) {
                                40.dp
                            } else if (isPressed) {
                                30.dp
                            } else {
                                24.dp
                            },
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                            } else {
                                navSelectionDpAnimationSpec
                            },
                            label = "bottom-nav-icon-halo-size-${destination.name}",
                        )
                        val iconHaloBorderWidth by animateDpAsState(
                            targetValue = if (isSelected || isPressed) 0.8.dp else 0.dp,
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                            } else {
                                navSelectionDpAnimationSpec
                            },
                            label = "bottom-nav-icon-halo-border-width-${destination.name}",
                        )
                        val iconShadowColor by animateColorAsState(
                            targetValue = when {
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                else -> Color.Transparent
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                            } else {
                                navSelectionColorAnimationSpec
                            },
                            label = "bottom-nav-icon-shadow-${destination.name}",
                        )

                        Box(
                            modifier = Modifier
                                .size(iconHaloSize)
                                .clip(RoundedCornerShape(iconHaloSize / 2))
                                .background(iconHalo)
                                .border(
                                    border = BorderStroke(
                                        width = iconHaloBorderWidth,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.52f)
                                        } else if (isPressed) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                                        } else {
                                            Color.Transparent
                                        },
                                    ),
                                    shape = RoundedCornerShape(iconHaloSize / 2),
                                )
                                .shadow(
                                    elevation = iconElevation.dp,
                                    shape = RoundedCornerShape(iconHaloSize / 2),
                                    ambientColor = iconShadowColor,
                                    spotColor = iconShadowColor,
                                )
                                .offset(y = iconLift)
                                .scale(iconContainerScale),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(iconFaceSize)
                                    .clip(RoundedCornerShape(iconFaceSize / 2))
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                iconFaceGlow,
                                                iconBackground,
                                            ),
                                        ),
                                    ),
                                    .border(
                                        border = BorderStroke(
                                            width = 0.5.dp,
                                            color = iconFaceGlow,
                                        ),
                                        shape = RoundedCornerShape(iconFaceSize / 2),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(destination.iconResource),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .scale(iconScale),
                                    tint = iconTint,
                                )
                            }
                        }
                    },
                    label = {
                        DestinationLabel(
                            label = destinationLabel(destination),
                            compact = true,
                            isSelected = isSelected,
                            isPressed = isPressed,
                        )
                    },
                    alwaysShowLabel = false,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        indicatorColor = selectedIndicatorColor,
                        disabledIndicatorColor = Color.Transparent,
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val railShape = RoundedCornerShape(20.dp)
    val railSurface = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    }
    val railBorder = if (isDark) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    val railTint = if (isDark) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    }
    val railContainer = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
    }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(min = 84.dp)
            .padding(8.dp)
            .testTag("primary-navigation-rail"),
        color = railSurface,
        shape = railShape,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(0.5.dp, railBorder),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(railShape)
                .shadow(
                    elevation = if (isDark) 8.dp else 6.dp,
                    shape = railShape,
                    ambientColor = railTint,
                    spotColor = railTint,
                )
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.09f else 0.05f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        NavigationRail(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 4.dp, top = 8.dp, bottom = 8.dp)
                .clip(railShape),
            containerColor = railContainer,
            windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            PrimaryDestination.entries.forEach { destination ->
                    val destinationInteractionSource = remember {
                        MutableInteractionSource()
                    }
                    val isSelected = selectedDestination == destination
                    val isPressed by destinationInteractionSource.collectIsPressedAsState()
                    val selectedIndicatorColor by animateColorAsState(
                        targetValue = when {
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            else -> Color.Transparent
                        },
                        animationSpec = if (isPressed) {
                            navPressColorAnimationSpec
                        } else {
                            navSelectionColorAnimationSpec
                        },
                        label = "rail-indicator-color-${destination.name}",
                    )
                    val selectedPillColor by animateColorAsState(
                        targetValue = when {
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                            else -> Color.Transparent
                        },
                        animationSpec = if (isPressed) {
                            navPressColorAnimationSpec
                        } else {
                            navSelectionColorAnimationSpec
                        },
                        label = "rail-pill-color-${destination.name}",
                    )
                    val selectedPillTopColor by animateColorAsState(
                        targetValue = when {
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            else -> Color.Transparent
                        },
                        animationSpec = if (isPressed) {
                            navPressColorAnimationSpec
                        } else {
                            navSelectionColorAnimationSpec
                        },
                        label = "rail-pill-top-color-${destination.name}",
                    )
                    val selectedPillBorderColor by animateColorAsState(
                        targetValue = when {
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            else -> Color.Transparent
                        },
                        animationSpec = if (isPressed) {
                            navPressColorAnimationSpec
                        } else {
                            navSelectionColorAnimationSpec
                        },
                        label = "rail-pill-border-color-${destination.name}",
                    )
                    val selectedPillElevation by animateDpAsState(
                        targetValue = when {
                            isSelected -> 2.2.dp
                            isPressed -> 1.2.dp
                            else -> 0.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectionDpAnimationSpec
                        },
                        label = "rail-pill-elevation-${destination.name}",
                    )
                    val activeNavItemWidth by animateDpAsState(
                        targetValue = when {
                            isSelected -> 86.dp
                            isPressed -> 76.dp
                            else -> 72.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectionDpAnimationSpec
                        },
                        label = "rail-item-width-${destination.name}",
                    )
                    val activeItemCorner by animateDpAsState(
                        targetValue = when {
                            isSelected -> 18.dp
                            isPressed -> 16.dp
                            else -> 14.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectionDpAnimationSpec
                        },
                        label = "rail-item-corner-${destination.name}",
                    )
                    val activeItemShape = RoundedCornerShape(activeItemCorner)
                    NavigationRailItem(
                        selected = isSelected,
                        onClick = { onDestinationSelected(destination) },
                        interactionSource = destinationInteractionSource,
                        modifier = Modifier
                            .testTag(destination.testTag)
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                            .clip(activeItemShape)
                            .shadow(
                                elevation = selectedPillElevation,
                                shape = activeItemShape,
                                ambientColor = selectedPillBorderColor.copy(alpha = 0.24f),
                                spotColor = selectedPillColor,
                            )
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        selectedPillTopColor,
                                        selectedPillColor,
                                    ),
                                ),
                                shape = activeItemShape,
                            )
                            .border(
                                border = BorderStroke(
                                    width = if (isSelected || isPressed) 0.8.dp else 0.dp,
                                    color = selectedPillBorderColor,
                                ),
                                shape = activeItemShape,
                            )
                            .sizeIn(
                                minWidth = activeNavItemWidth,
                                minHeight = 48.dp,
                            ),
                    icon = {
                        val iconLift by animateDpAsState(
                            targetValue = when {
                                isPressed -> (-1).dp
                                isSelected -> (-2).dp
                                else -> 0.dp
                            },
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                            } else {
                                navSelectionDpAnimationSpec
                            },
                            label = "rail-icon-lift-${destination.name}",
                        )
                        val iconElevation by animateFloatAsState(
                            targetValue = when {
                                isPressed -> 2f
                                isSelected -> 5f
                                else -> 0f
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                            } else {
                                navSelectionFloatAnimationSpec
                            },
                            label = "rail-icon-elevation-${destination.name}",
                        )
                        val iconContainerScale by animateFloatAsState(
                            targetValue = when {
                                isPressed -> 1.03f
                                isSelected -> 1.05f
                                else -> 1f
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                            } else {
                                navSelectionFloatAnimationSpec
                            },
                            label = "rail-icon-container-scale-${destination.name}",
                        )
                        val iconScale by animateFloatAsState(
                            targetValue = when {
                                isPressed -> 0.98f
                                isSelected -> 1.05f
                                else -> 1f
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                            } else {
                                navSelectionFloatAnimationSpec
                            },
                            label = "rail-icon-scale-${destination.name}",
                        )
                        val iconFaceSize by animateDpAsState(
                            targetValue = when {
                                isPressed -> 33.dp
                                isSelected -> 35.dp
                                else -> 34.dp
                            },
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                            } else {
                                navSelectionDpAnimationSpec
                            },
                            label = "rail-icon-face-size-${destination.name}",
                        )
                        val iconBackground by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                            } else {
                                navSelectionColorAnimationSpec
                            },
                            label = "rail-icon-background-${destination.name}",
                        )
                        val iconFaceGlow by animateColorAsState(
                            targetValue = when {
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else -> Color.Transparent
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                            } else {
                                navSelectionColorAnimationSpec
                            },
                            label = "rail-icon-face-glow-${destination.name}",
                        )
                        val iconTint by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                            } else {
                                navSelectionColorAnimationSpec
                            },
                            label = "rail-icon-tint-${destination.name}",
                        )
                        val iconHalo by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                            } else {
                                navSelectionColorAnimationSpec
                            },
                            label = "rail-icon-halo-${destination.name}",
                        )
                        val iconHaloSize by animateDpAsState(
                            targetValue = if (isSelected) {
                                40.dp
                            } else if (isPressed) {
                                32.dp
                            } else {
                                26.dp
                            },
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                            } else {
                                navSelectionDpAnimationSpec
                            },
                            label = "rail-icon-halo-size-${destination.name}",
                        )
                        val iconHaloBorderWidth by animateDpAsState(
                            targetValue = if (isSelected || isPressed) 0.8.dp else 0.dp,
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                            } else {
                                navSelectionDpAnimationSpec
                            },
                            label = "rail-icon-halo-border-width-${destination.name}",
                        )
                        val iconShadowColor by animateColorAsState(
                            targetValue = when {
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else -> Color.Transparent
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                            } else {
                                navSelectionColorAnimationSpec
                            },
                            label = "rail-icon-shadow-${destination.name}",
                        )
                        Box(
                            modifier = Modifier
                                .size(iconHaloSize)
                                .clip(RoundedCornerShape(iconHaloSize / 2))
                                .background(iconHalo)
                                .border(
                                    border = BorderStroke(
                                        width = iconHaloBorderWidth,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
                                        } else if (isPressed) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        } else {
                                            Color.Transparent
                                        },
                                    ),
                                    shape = RoundedCornerShape(iconHaloSize / 2),
                                )
                                .shadow(
                                    elevation = iconElevation.dp,
                                    shape = RoundedCornerShape(iconHaloSize / 2),
                                    ambientColor = iconShadowColor,
                                    spotColor = iconShadowColor,
                                )
                                .offset(y = iconLift)
                                .scale(iconContainerScale),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(iconFaceSize)
                                    .clip(RoundedCornerShape(iconFaceSize / 2))
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                iconFaceGlow,
                                                iconBackground,
                                            ),
                                        ),
                                    )
                                    .border(
                                        border = BorderStroke(
                                            width = 0.45.dp,
                                            color = iconFaceGlow,
                                        ),
                                        shape = RoundedCornerShape(iconFaceSize / 2),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(destination.iconResource),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .scale(iconScale),
                                    tint = iconTint,
                                )
                            }
                        }
                    },
                    label = {
                        DestinationLabel(
                            destinationLabel(destination),
                            compact = false,
                            isSelected = isSelected,
                            isPressed = isPressed,
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        indicatorColor = selectedIndicatorColor,
                        disabledIndicatorColor = Color.Transparent,
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
    isPressed: Boolean = false,
) {
    val resolvedStyle = if (compact) {
        MaterialTheme.typography.labelSmall.copy(
            fontSize = if (isPressed) 11.sp else 12.sp,
            lineHeight = if (isPressed) 13.sp else 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else if (isPressed) FontWeight.Medium else FontWeight.Normal,
            letterSpacing = 0.08.sp,
        )
    } else {
        MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.5.sp,
            lineHeight = 13.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            letterSpacing = 0.02.sp,
        )
    }
    val labelGlowColor by animateColorAsState(
        targetValue = when {
            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.96f)
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.90f)
            isPressed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
        },
        animationSpec = if (isPressed) {
            navPressColorAnimationSpec
        } else {
            navSelectionColorAnimationSpec
        },
        label = "destination-label-glow-color",
    )
    val labelColor by animateColorAsState(
        targetValue = when {
            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
            isPressed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
            isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
        },
        animationSpec = if (isPressed) {
            navPressColorAnimationSpec
        } else {
            navSelectionColorAnimationSpec
        },
        label = "destination-label-color",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (isPressed && !isSelected) 0.72f else if (isSelected) 1f else 0.88f,
        animationSpec = if (isPressed) {
            navPressFloatAnimationSpec
        } else {
            navSelectionFloatAnimationSpec
        },
        label = "destination-label-alpha",
    )
    val labelOffset by animateDpAsState(
        targetValue = when {
            isPressed && !isSelected -> 3.dp
            isSelected -> 0.dp
            else -> if (compact) 2.dp else 1.dp
        },
        animationSpec = if (isPressed) {
            navPressDpAnimationSpec
        } else {
            navSelectionDpAnimationSpec
        },
        label = "destination-label-offset",
    )
    val labelLift by animateDpAsState(
        targetValue = when {
            isPressed && !isSelected -> 1.5.dp
            isPressed && isSelected -> 0.dp
            isSelected -> 0.dp
            else -> if (compact) 2.dp else 1.5.dp
        },
        animationSpec = if (isPressed) {
            navPressDpAnimationSpec
        } else {
            navSelectionDpAnimationSpec
        },
        label = "destination-label-lift",
    )
    val labelScale by animateFloatAsState(
        targetValue = when {
            isPressed && !isSelected -> 0.96f
            isSelected -> 1f
            else -> 0.97f
        },
        animationSpec = if (isPressed) {
            navPressFloatAnimationSpec
        } else {
            navSelectionFloatAnimationSpec
        },
        label = "destination-label-scale",
    )
    Text(
        text = label,
        modifier = Modifier
            .offset(y = labelOffset + labelLift)
            .scale(labelScale),
        color = if (isSelected) {
            labelColor.copy(alpha = labelAlpha)
        } else {
            labelGlowColor.copy(alpha = labelAlpha)
        },
        style = resolvedStyle,
        maxLines = 1,
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Ellipsis,
    )
}

private val PrimaryDestination.testTag: String
    get() = "primary-navigation-${name.lowercase()}"

private val navSelectionFloatAnimationSpec = tween<Float>(220, easing = FastOutSlowInEasing)
private val navPressFloatAnimationSpec = tween<Float>(120, easing = FastOutSlowInEasing)
private val navSelectionDpAnimationSpec = tween<Dp>(220, easing = FastOutSlowInEasing)
private val navPressDpAnimationSpec = tween<Dp>(120, easing = FastOutSlowInEasing)
private val navPressColorAnimationSpec = tween<Color>(120, easing = FastOutSlowInEasing)
private val navSelectionColorAnimationSpec = tween<Color>(220, easing = FastOutSlowInEasing)
