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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.Shadow
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
        MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    }
    val barBorder = if (isDark) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    val barBottomGlow = if (isDark) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    }
    val barContainer = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.76f)
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
                .padding(horizontal = 4.dp, top = 2.dp, bottom = 2.dp)
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
                    val selectedPillColor by animateColorAsState(
                        targetValue = when {
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
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
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
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
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
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
                            isSelected -> 2.0.dp
                            isPressed -> 1.1.dp
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
                            isSelected -> 79.dp
                            isPressed -> 73.dp
                            else -> 70.dp
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
                    val activeItemOffset by animateDpAsState(
                        targetValue = when {
                            isSelected -> (-0.8).dp
                            isPressed -> 0.5.dp
                            else -> 0.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectionDpAnimationSpec
                        },
                        label = "bottom-nav-item-offset-${destination.name}",
                    )
                    val activeItemHeight by animateDpAsState(
                        targetValue = when {
                            isSelected -> 60.dp
                            isPressed -> 57.dp
                            else -> 56.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectionDpAnimationSpec
                        },
                        label = "bottom-nav-item-height-${destination.name}",
                    )
                    val activeItemScale by animateFloatAsState(
                        targetValue = when {
                            isSelected -> 1.002f
                            isPressed -> 0.988f
                            else -> 1f
                        },
                        animationSpec = if (isPressed) {
                            navPressFloatAnimationSpec
                        } else {
                            navSelectionFloatAnimationSpec
                        },
                        label = "bottom-nav-item-scale-${destination.name}",
                    )
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onDestinationSelected(destination) },
                        interactionSource = destinationInteractionSource,
                        modifier = Modifier
                            .testTag(destination.testTag)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
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
                                minHeight = activeItemHeight,
                            ),
                            .offset(y = activeItemOffset)
                            .scale(activeItemScale),
                    icon = {
                        val iconLift by animateDpAsState(
                            targetValue = when {
                                isPressed -> 0.dp
                                isSelected -> (-1.1).dp
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
                                isSelected -> 4.1f
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
                                isPressed -> 1.015f
                                isSelected -> 1.01f
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
                                isPressed -> 0.99f
                                isSelected -> 1.01f
                                else -> 1f
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                            } else {
                                navSelectionFloatAnimationSpec
                            },
                            label = "bottom-nav-icon-scale-${destination.name}",
                        )
                        val iconGlyphAlpha by animateFloatAsState(
                            targetValue = when {
                                isSelected -> 1f
                                isPressed -> 0.88f
                                else -> 0.78f
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                            } else {
                                navSelectionFloatAnimationSpec
                            },
                            label = "bottom-nav-icon-glyph-alpha-${destination.name}",
                        )
                        val iconContainerAlpha by animateFloatAsState(
                            targetValue = when {
                                isSelected -> 1f
                                isPressed -> 0.9f
                                else -> 0.84f
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                            } else {
                                navSelectionFloatAnimationSpec
                            },
                            label = "bottom-nav-icon-container-alpha-${destination.name}",
                        )
                        val iconGlyphSize by animateDpAsState(
                            targetValue = when {
                                isPressed -> 19.dp
                                isSelected -> 20.3.dp
                                else -> 19.5.dp
                            },
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                            } else {
                                navSelectionDpAnimationSpec
                            },
                            label = "bottom-nav-icon-glyph-size-${destination.name}",
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
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.36f)
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
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
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
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
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
                        val iconHaloHighlight by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                            } else {
                                navSelectionColorAnimationSpec
                            },
                            label = "bottom-nav-icon-halo-highlight-${destination.name}",
                        )
                        val iconHaloSize by animateDpAsState(
                            targetValue = if (isSelected) {
                                38.dp
                            } else if (isPressed) {
                                29.dp
                            } else {
                                23.dp
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
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
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
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            iconHaloHighlight,
                                            iconHalo,
                                        ),
                                    ),
                                )
                                .border(
                                    border = BorderStroke(
                                        width = iconHaloBorderWidth,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                                        } else if (isPressed) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
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
                                .alpha(iconContainerAlpha)
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
                                        .size(iconGlyphSize)
                                        .scale(iconScale)
                                        .alpha(iconGlyphAlpha),
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
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                        indicatorColor = Color.Transparent,
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
        MaterialTheme.colorScheme.surface.copy(alpha = 0.93f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    }
    val railBorder = if (isDark) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    val railTint = if (isDark) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.17f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    }
    val railContainer = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.70f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
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
                .padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 6.dp)
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
                    val selectedPillColor by animateColorAsState(
                        targetValue = when {
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
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
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
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
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
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
                            isSelected -> 1.7.dp
                            isPressed -> 1.0.dp
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
                            isSelected -> 87.dp
                            isPressed -> 78.dp
                            else -> 76.dp
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
                    val activeItemOffset by animateDpAsState(
                        targetValue = when {
                                isSelected -> (-1.1).dp
                                isPressed -> 0.4.dp
                                else -> 0.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectionDpAnimationSpec
                        },
                        label = "rail-item-offset-${destination.name}",
                    )
                    val activeItemHeight by animateDpAsState(
                        targetValue = when {
                            isSelected -> 51.dp
                            isPressed -> 49.5.dp
                            else -> 48.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectionDpAnimationSpec
                        },
                        label = "rail-item-height-${destination.name}",
                    )
                    val activeItemScale by animateFloatAsState(
                        targetValue = when {
                            isSelected -> 1.004f
                            isPressed -> 0.988f
                            else -> 1f
                        },
                        animationSpec = if (isPressed) {
                            navPressFloatAnimationSpec
                        } else {
                            navSelectionFloatAnimationSpec
                        },
                        label = "rail-item-scale-${destination.name}",
                    )
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
                                minHeight = activeItemHeight,
                            ),
                            .offset(y = activeItemOffset)
                            .scale(activeItemScale),
                    icon = {
                        val iconLift by animateDpAsState(
                            targetValue = when {
                                isPressed -> 0.dp
                                isSelected -> (-1.1).dp
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
                                isSelected -> 4.2f
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
                                isPressed -> 1.015f
                                isSelected -> 1.01f
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
                                isPressed -> 0.995f
                                isSelected -> 1.01f
                                else -> 1f
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                            } else {
                                navSelectionFloatAnimationSpec
                            },
                            label = "rail-icon-scale-${destination.name}",
                        )
                        val iconGlyphAlpha by animateFloatAsState(
                            targetValue = when {
                                isSelected -> 1f
                                isPressed -> 0.88f
                                else -> 0.78f
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                            } else {
                                navSelectionFloatAnimationSpec
                            },
                            label = "rail-icon-glyph-alpha-${destination.name}",
                        )
                        val iconContainerAlpha by animateFloatAsState(
                            targetValue = when {
                                isSelected -> 1f
                                isPressed -> 0.9f
                                else -> 0.84f
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                            } else {
                                navSelectionFloatAnimationSpec
                            },
                            label = "rail-icon-container-alpha-${destination.name}",
                        )
                        val iconGlyphSize by animateDpAsState(
                            targetValue = when {
                                isPressed -> 19.dp
                                isSelected -> 20.3.dp
                                else -> 19.5.dp
                            },
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                            } else {
                                navSelectionDpAnimationSpec
                            },
                            label = "rail-icon-glyph-size-${destination.name}",
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
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.24f)
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
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
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
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
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
                        val iconHaloHighlight by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                            } else {
                                navSelectionColorAnimationSpec
                            },
                            label = "rail-icon-halo-highlight-${destination.name}",
                        )
                        val iconHaloSize by animateDpAsState(
                            targetValue = if (isSelected) {
                                38.dp
                            } else if (isPressed) {
                                30.dp
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
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
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
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            iconHaloHighlight,
                                            iconHalo,
                                        ),
                                    ),
                                )
                                .border(
                                    border = BorderStroke(
                                        width = iconHaloBorderWidth,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                                        } else if (isPressed) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
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
                                .alpha(iconContainerAlpha)
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
                                        .size(iconGlyphSize)
                                        .scale(iconScale)
                                        .alpha(iconGlyphAlpha),
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
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                    indicatorColor = Color.Transparent,
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
            fontSize = if (isSelected) 12.2.sp else if (isPressed) 11.6.sp else 11.8.sp,
            lineHeight = if (isPressed) 13.0.sp else 14.1.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else if (isPressed) FontWeight.Medium else FontWeight.Normal,
            letterSpacing = 0.06.sp,
        )
    } else {
        MaterialTheme.typography.labelSmall.copy(
            fontSize = if (isSelected) 11.6.sp else if (isPressed) 11.3.sp else 11.1.sp,
            lineHeight = if (isPressed) 12.4.sp else 12.8.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else if (isPressed) FontWeight.Medium else FontWeight.Normal,
            letterSpacing = 0.02.sp,
        )
    }
    val labelGlowColor by animateColorAsState(
        targetValue = when {
            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.96f)
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.90f)
            isPressed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
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
            isPressed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.64f)
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
        targetValue = if (isPressed && !isSelected) 0.93f else if (isSelected) 1f else 0.94f,
        animationSpec = if (isPressed) {
            navPressFloatAnimationSpec
        } else {
            navSelectionFloatAnimationSpec
        },
        label = "destination-label-alpha",
    )
    val labelOffset by animateDpAsState(
        targetValue = when {
            isPressed && !isSelected -> 0.5.dp
            isSelected -> 0.dp
            else -> if (compact) 1.1.dp else 0.7.dp
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
            isPressed && !isSelected -> 0.2.dp
            isPressed && isSelected -> 0.dp
            isSelected -> (-0.1).dp
            else -> if (compact) 1.0.dp else 0.7.dp
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
            isPressed && !isSelected -> 0.99f
            isSelected -> 1f
            else -> 0.985f
        },
        animationSpec = if (isPressed) {
            navPressFloatAnimationSpec
        } else {
            navSelectionFloatAnimationSpec
        },
        label = "destination-label-scale",
    )
    val labelShadowColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = if (isPressed) 0.18f else 0.14f)
            isPressed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f)
            else -> Color.Transparent
        },
        animationSpec = if (isPressed) {
            navPressColorAnimationSpec
        } else {
            navSelectionColorAnimationSpec
        },
        label = "destination-label-shadow-color",
    )
    val labelShadowRadius by animateFloatAsState(
        targetValue = when {
            isSelected -> 1f
            isPressed -> 0.2f
            else -> 0f
        },
        animationSpec = if (isPressed) {
            navPressFloatAnimationSpec
        } else {
            navSelectionFloatAnimationSpec
        },
        label = "destination-label-shadow-radius",
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
        shadow = Shadow(
            color = labelShadowColor,
            offset = Offset(0f, 1f),
            blurRadius = labelShadowRadius,
        ),
    )
}

private val PrimaryDestination.testTag: String
    get() = "primary-navigation-${name.lowercase()}"

private val navSelectionFloatAnimationSpec = tween<Float>(240, easing = FastOutSlowInEasing)
private val navPressFloatAnimationSpec = tween<Float>(140, easing = FastOutSlowInEasing)
private val navSelectionDpAnimationSpec = tween<Dp>(240, easing = FastOutSlowInEasing)
private val navPressDpAnimationSpec = tween<Dp>(140, easing = FastOutSlowInEasing)
private val navPressColorAnimationSpec = tween<Color>(140, easing = FastOutSlowInEasing)
private val navSelectionColorAnimationSpec = tween<Color>(240, easing = FastOutSlowInEasing)
