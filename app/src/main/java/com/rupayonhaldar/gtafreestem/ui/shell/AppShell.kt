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
    val barShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val barSurface = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    }
    val barBorder = if (isDark) {
        Color.White.copy(alpha = 0.11f)
    } else {
        Color.Black.copy(alpha = 0.055f)
    }
    val barBottomGlow = if (isDark) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.045f)
    }
    val barContainer = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.71f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
    }

    Surface(
        modifier = Modifier
            .testTag("primary-navigation-bar")
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = barShape,
        color = barSurface,
        tonalElevation = 6.dp,
        shadowElevation = 9.dp,
        border = BorderStroke(0.42.dp, barBorder),
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
                            if (isDark) {
                                Color.White.copy(alpha = 0.026f)
                            } else {
                                Color.Black.copy(alpha = 0.012f)
                            },
                            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.048f else 0.03f),
                        ),
                    ),
                ),
            )
        NavigationBar(
            modifier = Modifier
                .padding(horizontal = 5.dp, top = 0.dp, bottom = 0.dp)
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
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.055f)
                            else -> Color.Transparent
                        },
                        animationSpec = if (isPressed) {
                            navPressColorAnimationSpec
                        } else {
                            navSelectedSettleColorAnimationSpec
                        },
                        label = "bottom-nav-pill-color-${destination.name}",
                    )
                    val selectedPillTopColor by animateColorAsState(
                        targetValue = when {
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.21f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.17f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.065f)
                            else -> Color.Transparent
                        },
                        animationSpec = if (isPressed) {
                            navPressColorAnimationSpec
                        } else {
                            navSelectedSettleColorAnimationSpec
                        },
                        label = "bottom-nav-pill-top-color-${destination.name}",
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
                            navSelectedSettleColorAnimationSpec
                        },
                        label = "bottom-nav-pill-border-color-${destination.name}",
                    )
                    val selectedPillElevation by animateDpAsState(
                        targetValue = when {
                            isSelected -> 1.70.dp
                            isPressed -> 0.95.dp
                            else -> 0.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectedSettleDpAnimationSpec
                        },
                        label = "bottom-nav-pill-elevation-${destination.name}",
                    )
                    val activeNavItemWidth by animateDpAsState(
                        targetValue = when {
                            isSelected -> 80.dp
                            isPressed -> 73.dp
                            else -> 70.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectedSettleDpAnimationSpec
                        },
                        label = "bottom-nav-item-width-${destination.name}",
                    )
                    val activeItemCorner by animateDpAsState(
                        targetValue = when {
                            isSelected -> 21.5.dp
                            isPressed -> 18.5.dp
                            else -> 16.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectedSettleDpAnimationSpec
                        },
                        label = "bottom-nav-item-corner-${destination.name}",
                    )
                    val activeItemShape = RoundedCornerShape(activeItemCorner)
                    val activeItemOffset by animateDpAsState(
                        targetValue = when {
                            isSelected -> (-0.65).dp
                            isPressed -> 0.46.dp
                            else -> 0.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                            } else if (isSelected) {
                            navSelectedSettleDpAnimationSpec
                            } else {
                            navSelectionDpAnimationSpec
                            },
                        label = "bottom-nav-item-offset-${destination.name}",
                    )
                    val activeItemHeight by animateDpAsState(
                        targetValue = when {
                            isSelected -> 59.4.dp
                            isPressed -> 55.2.dp
                            else -> 54.4.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                            } else if (isSelected) {
                            navSelectedSettleDpAnimationSpec
                            } else {
                            navSelectionDpAnimationSpec
                            },
                        label = "bottom-nav-item-height-${destination.name}",
                    )
                    val activeItemScale by animateFloatAsState(
                        targetValue = when {
                            isSelected -> 1.003f
                            isPressed -> 0.989f
                            else -> 1f
                        },
                        animationSpec = if (isPressed) {
                            navPressFloatAnimationSpec
                        } else {
                            navSelectedSettleFloatAnimationSpec
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
                                ambientColor = selectedPillBorderColor.copy(alpha = if (isDark) 0.23f else 0.13f),
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
                                    width = if (isSelected || isPressed) 0.86.dp else 0.dp,
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
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navSelectionDpAnimationSpec
                                },
                            label = "bottom-nav-icon-lift-${destination.name}",
                        )
                        val iconElevation by animateFloatAsState(
                            targetValue = when {
                                isPressed -> 1.9f
                                isSelected -> 4.0f
                                else -> 0f
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navSelectionFloatAnimationSpec
                                },
                            label = "bottom-nav-icon-elevation-${destination.name}",
                        )
                    val iconContainerScale by animateFloatAsState(
                        targetValue = when {
                            isPressed -> 1.015f
                            isSelected -> 1.013f
                            else -> 1f
                        },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navSelectionFloatAnimationSpec
                                },
                            label = "bottom-nav-icon-container-scale-${destination.name}",
                        )
                        val iconScale by animateFloatAsState(
                        targetValue = when {
                            isPressed -> 0.99f
                            isSelected -> 1.018f
                            else -> 1f
                        },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navSelectionFloatAnimationSpec
                                },
                            label = "bottom-nav-icon-scale-${destination.name}",
                        )
                        val iconGlyphAlpha by animateFloatAsState(
                            targetValue = when {
                            isSelected -> 1f
                            isPressed -> 0.875f
                            else -> 0.73f
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navSelectionFloatAnimationSpec
                                },
                            label = "bottom-nav-icon-glyph-alpha-${destination.name}",
                        )
                        val iconContainerAlpha by animateFloatAsState(
                        targetValue = when {
                            isSelected -> 1f
                            isPressed -> 0.89f
                            else -> 0.76f
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navSelectionFloatAnimationSpec
                                },
                            label = "bottom-nav-icon-container-alpha-${destination.name}",
                        )
                        val iconGlyphSize by animateDpAsState(
                            targetValue = when {
                            isPressed -> 19.dp
                            isSelected -> 20.7.dp
                            else -> 19.6.dp
                            },
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navSelectionDpAnimationSpec
                                },
                            label = "bottom-nav-icon-glyph-size-${destination.name}",
                        )
                        val iconFaceSize by animateDpAsState(
                            targetValue = when {
                            isPressed -> 33.dp
                            isSelected -> 35.6.dp
                            else -> 34.1.dp
                            },
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navSelectionDpAnimationSpec
                                },
                            label = "bottom-nav-icon-face-size-${destination.name}",
                        )
                        val iconBackground by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleColorAnimationSpec
                                } else {
                                navSelectionColorAnimationSpec
                                },
                            label = "bottom-nav-icon-background-${destination.name}",
                        )
                        val iconFaceGlow by animateColorAsState(
                            targetValue = when {
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
                                else -> Color.Transparent
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleColorAnimationSpec
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
                                } else if (isSelected) {
                                navSelectedSettleColorAnimationSpec
                                } else {
                                navSelectionColorAnimationSpec
                                },
                            label = "bottom-nav-icon-tint-${destination.name}",
                        )
                        val iconHalo by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleColorAnimationSpec
                                } else {
                                navSelectionColorAnimationSpec
                                },
                            label = "bottom-nav-icon-halo-${destination.name}",
                        )
                        val iconHaloHighlight by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleColorAnimationSpec
                                } else {
                                navSelectionColorAnimationSpec
                                },
                            label = "bottom-nav-icon-halo-highlight-${destination.name}",
                        )
                        val iconHaloSize by animateDpAsState(
                            targetValue = if (isSelected) {
                                38.4.dp
                            } else if (isPressed) {
                                27.dp
                            } else {
                                22.dp
                            },
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navSelectionDpAnimationSpec
                                },
                            label = "bottom-nav-icon-halo-size-${destination.name}",
                        )
                        val iconHaloBorderWidth by animateDpAsState(
                            targetValue = if (isSelected || isPressed) 0.76.dp else 0.dp,
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navSelectionDpAnimationSpec
                                },
                            label = "bottom-nav-icon-halo-border-width-${destination.name}",
                        )
                        val iconShadowColor by animateColorAsState(
                            targetValue = when {
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)
                                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.055f)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.082f)
                                else -> Color.Transparent
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleColorAnimationSpec
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
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                        } else if (isPressed) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
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
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.59f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.59f),
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
    val railShape = RoundedCornerShape(24.dp)
    val railSurface = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    }
    val railBorder = if (isDark) {
        Color.White.copy(alpha = 0.11f)
    } else {
        Color.Black.copy(alpha = 0.055f)
    }
    val railBottomGlow = if (isDark) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.045f)
    }
    val railContainer = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
    }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(min = 86.dp)
            .padding(8.dp)
            .testTag("primary-navigation-rail"),
        color = railSurface,
        shape = railShape,
        tonalElevation = 6.dp,
        shadowElevation = 9.dp,
        border = BorderStroke(0.42.dp, railBorder),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(railShape)
                .shadow(
                    elevation = if (isDark) 8.dp else 6.dp,
                    shape = railShape,
                    ambientColor = railBottomGlow,
                    spotColor = railBottomGlow,
                )
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            if (isDark) {
                                Color.White.copy(alpha = 0.024f)
                            } else {
                                Color.Black.copy(alpha = 0.012f)
                            },
                            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.048f else 0.03f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        NavigationRail(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 6.dp, end = 6.dp, top = 9.dp, bottom = 9.dp)
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
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.17f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.055f)
                            else -> Color.Transparent
                        },
                        animationSpec = if (isPressed) {
                            navPressColorAnimationSpec
                        } else {
                            navSelectedSettleColorAnimationSpec
                        },
                        label = "rail-pill-color-${destination.name}",
                    )
                    val selectedPillTopColor by animateColorAsState(
                        targetValue = when {
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.19f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.17f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                            else -> Color.Transparent
                        },
                        animationSpec = if (isPressed) {
                            navPressColorAnimationSpec
                        } else {
                            navSelectedSettleColorAnimationSpec
                        },
                        label = "rail-pill-top-color-${destination.name}",
                    )
                    val selectedPillBorderColor by animateColorAsState(
                        targetValue = when {
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.19f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            else -> Color.Transparent
                        },
                        animationSpec = if (isPressed) {
                            navPressColorAnimationSpec
                        } else {
                            navSelectedSettleColorAnimationSpec
                        },
                        label = "rail-pill-border-color-${destination.name}",
                    )
                    val selectedPillElevation by animateDpAsState(
                        targetValue = when {
                            isSelected -> 1.50.dp
                            isPressed -> 0.85.dp
                            else -> 0.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectedSettleDpAnimationSpec
                        },
                        label = "rail-pill-elevation-${destination.name}",
                    )
                    val activeNavItemWidth by animateDpAsState(
                        targetValue = when {
                            isSelected -> 88.dp
                            isPressed -> 79.dp
                            else -> 76.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectedSettleDpAnimationSpec
                        },
                        label = "rail-item-width-${destination.name}",
                    )
                    val activeItemCorner by animateDpAsState(
                        targetValue = when {
                            isSelected -> 19.dp
                            isPressed -> 16.5.dp
                            else -> 14.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectedSettleDpAnimationSpec
                        },
                        label = "rail-item-corner-${destination.name}",
                    )
                    val activeItemShape = RoundedCornerShape(activeItemCorner)
                    val activeItemOffset by animateDpAsState(
                        targetValue = when {
                                isSelected -> (-0.93).dp
                                isPressed -> 0.31.dp
                                else -> 0.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectedSettleDpAnimationSpec
                        },
                        label = "rail-item-offset-${destination.name}",
                    )
                    val activeItemHeight by animateDpAsState(
                        targetValue = when {
                            isSelected -> 50.7.dp
                            isPressed -> 48.8.dp
                            else -> 48.2.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectedSettleDpAnimationSpec
                        },
                        label = "rail-item-height-${destination.name}",
                    )
                    val activeItemScale by animateFloatAsState(
                        targetValue = when {
                            isSelected -> 1.004f
                            isPressed -> 0.992f
                            else -> 1f
                        },
                        animationSpec = if (isPressed) {
                            navPressFloatAnimationSpec
                        } else {
                            navSelectedSettleFloatAnimationSpec
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
                                ambientColor = selectedPillBorderColor.copy(alpha = 0.21f),
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
                                    width = if (isSelected || isPressed) 0.76.dp else 0.dp,
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
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navSelectionDpAnimationSpec
                                },
                            label = "rail-icon-lift-${destination.name}",
                        )
                        val iconElevation by animateFloatAsState(
                            targetValue = when {
                                isPressed -> 1.9f
                                isSelected -> 4.0f
                                else -> 0f
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navSelectionFloatAnimationSpec
                                },
                            label = "rail-icon-elevation-${destination.name}",
                        )
                    val iconContainerScale by animateFloatAsState(
                        targetValue = when {
                            isPressed -> 1.015f
                            isSelected -> 1.013f
                            else -> 1f
                        },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navSelectionFloatAnimationSpec
                                },
                            label = "rail-icon-container-scale-${destination.name}",
                        )
                        val iconScale by animateFloatAsState(
                        targetValue = when {
                            isPressed -> 0.994f
                            isSelected -> 1.018f
                            else -> 1f
                        },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navSelectionFloatAnimationSpec
                                },
                            label = "rail-icon-scale-${destination.name}",
                        )
                        val iconGlyphAlpha by animateFloatAsState(
                            targetValue = when {
                            isSelected -> 1f
                            isPressed -> 0.875f
                            else -> 0.73f
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navSelectionFloatAnimationSpec
                                },
                            label = "rail-icon-glyph-alpha-${destination.name}",
                        )
                        val iconContainerAlpha by animateFloatAsState(
                        targetValue = when {
                            isSelected -> 1f
                            isPressed -> 0.89f
                            else -> 0.76f
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navSelectionFloatAnimationSpec
                                },
                            label = "rail-icon-container-alpha-${destination.name}",
                        )
                        val iconGlyphSize by animateDpAsState(
                        targetValue = when {
                            isPressed -> 19.dp
                            isSelected -> 20.7.dp
                            else -> 19.6.dp
                            },
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navSelectionDpAnimationSpec
                                },
                            label = "rail-icon-glyph-size-${destination.name}",
                        )
                        val iconFaceSize by animateDpAsState(
                        targetValue = when {
                            isPressed -> 33.dp
                            isSelected -> 35.6.dp
                            else -> 34.1.dp
                            },
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navSelectionDpAnimationSpec
                                },
                            label = "rail-icon-face-size-${destination.name}",
                        )
                        val iconBackground by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleColorAnimationSpec
                                } else {
                                navSelectionColorAnimationSpec
                                },
                            label = "rail-icon-background-${destination.name}",
                        )
                        val iconFaceGlow by animateColorAsState(
                            targetValue = when {
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
                                else -> Color.Transparent
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleColorAnimationSpec
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
                                } else if (isSelected) {
                                navSelectedSettleColorAnimationSpec
                                } else {
                                navSelectionColorAnimationSpec
                                },
                            label = "rail-icon-tint-${destination.name}",
                        )
                        val iconHalo by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleColorAnimationSpec
                                } else {
                                navSelectionColorAnimationSpec
                                },
                            label = "rail-icon-halo-${destination.name}",
                        )
                        val iconHaloHighlight by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleColorAnimationSpec
                                } else {
                                navSelectionColorAnimationSpec
                                },
                            label = "rail-icon-halo-highlight-${destination.name}",
                        )
                        val iconHaloSize by animateDpAsState(
                            targetValue = if (isSelected) {
                                36.4.dp
                            } else if (isPressed) {
                                27.dp
                            } else {
                                23.dp
                            },
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navSelectionDpAnimationSpec
                                },
                            label = "rail-icon-halo-size-${destination.name}",
                        )
                        val iconHaloBorderWidth by animateDpAsState(
                            targetValue = if (isSelected || isPressed) 0.76.dp else 0.dp,
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navSelectionDpAnimationSpec
                                },
                            label = "rail-icon-halo-border-width-${destination.name}",
                        )
                        val iconShadowColor by animateColorAsState(
                            targetValue = when {
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else -> Color.Transparent
                            },
                            animationSpec = if (isPressed) {
                                navPressColorAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleColorAnimationSpec
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
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                        } else if (isPressed) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
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
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.59f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.59f),
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
            fontSize = if (isSelected) 12.50.sp else if (isPressed) 11.72.sp else 12.03.sp,
            lineHeight = if (isPressed) 13.16.sp else 14.02.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else if (isPressed) FontWeight.Medium else FontWeight.Normal,
            letterSpacing = 0.01.sp,
        )
    } else {
        MaterialTheme.typography.labelSmall.copy(
            fontSize = if (isSelected) 12.08.sp else if (isPressed) 11.57.sp else 11.36.sp,
            lineHeight = if (isPressed) 12.64.sp else 12.72.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else if (isPressed) FontWeight.Medium else FontWeight.Normal,
            letterSpacing = 0.00.sp,
        )
    }
    val labelGlowColor by animateColorAsState(
        targetValue = when {
            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.74f)
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            isPressed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.44f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f)
        },
        animationSpec = if (isPressed) {
            navPressColorAnimationSpec
        } else if (isSelected) {
            navSelectedSettleColorAnimationSpec
        } else {
            navSelectionColorAnimationSpec
        },
        label = "destination-label-glow-color",
    )
    val labelColor by animateColorAsState(
        targetValue = when {
            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.86f)
            isPressed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.57f)
            isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        },
        animationSpec = if (isPressed) {
            navPressColorAnimationSpec
        } else if (isSelected) {
            navSelectedSettleColorAnimationSpec
        } else {
            navSelectionColorAnimationSpec
        },
        label = "destination-label-color",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (isPressed && !isSelected) 0.97f else if (isSelected) 1f else 0.98f,
        animationSpec = if (isPressed) {
            navPressFloatAnimationSpec
        } else if (isSelected) {
            navSelectedSettleFloatAnimationSpec
        } else {
            navSelectionFloatAnimationSpec
        },
        label = "destination-label-alpha",
    )
    val labelOffset by animateDpAsState(
        targetValue = when {
            isPressed && !isSelected -> 0.29.dp
            isSelected -> 0.dp
            else -> if (compact) 0.54.dp else 0.44.dp
        },
        animationSpec = if (isPressed) {
            navPressDpAnimationSpec
        } else if (isSelected) {
            navSelectedSettleDpAnimationSpec
        } else {
            navSelectionDpAnimationSpec
        },
        label = "destination-label-offset",
    )
    val labelLift by animateDpAsState(
        targetValue = when {
            isPressed && !isSelected -> 0.09.dp
            isPressed && isSelected -> 0.dp
            isSelected -> (-0.14).dp
            else -> if (compact) 0.56.dp else 0.40.dp
        },
        animationSpec = if (isPressed) {
            navPressDpAnimationSpec
        } else if (isSelected) {
            navSelectedSettleDpAnimationSpec
        } else {
            navSelectionDpAnimationSpec
        },
        label = "destination-label-lift",
    )
    val labelScale by animateFloatAsState(
        targetValue = when {
            isPressed && !isSelected -> 0.99f
            isSelected -> 1.004f
            else -> 0.998f
        },
        animationSpec = if (isPressed) {
            navPressFloatAnimationSpec
        } else if (isSelected) {
            navSelectedSettleFloatAnimationSpec
        } else {
            navSelectionFloatAnimationSpec
        },
        label = "destination-label-scale",
    )
    val labelShadowColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = if (isPressed) 0.06f else 0.07f)
            isPressed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.03f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.01f)
        },
        animationSpec = if (isPressed) {
            navPressColorAnimationSpec
        } else if (isSelected) {
            navSelectedSettleColorAnimationSpec
        } else {
            navSelectionColorAnimationSpec
        },
        label = "destination-label-shadow-color",
    )
    val labelShadowRadius by animateFloatAsState(
        targetValue = when {
            isSelected -> 1.0f
            isPressed -> 0.1f
            else -> 0.02f
        },
        animationSpec = if (isPressed) {
            navPressFloatAnimationSpec
        } else if (isSelected) {
            navSelectedSettleFloatAnimationSpec
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
private val navPressFloatAnimationSpec = tween<Float>(145, easing = FastOutSlowInEasing)
private val navSelectionDpAnimationSpec = tween<Dp>(240, easing = FastOutSlowInEasing)
private val navPressDpAnimationSpec = tween<Dp>(145, easing = FastOutSlowInEasing)
private val navPressColorAnimationSpec = tween<Color>(145, easing = FastOutSlowInEasing)
private val navSelectionColorAnimationSpec = tween<Color>(240, easing = FastOutSlowInEasing)
private val navSelectedSettleColorAnimationSpec = tween<Color>(290, easing = FastOutSlowInEasing)
private val navSelectedSettleFloatAnimationSpec = tween<Float>(290, easing = FastOutSlowInEasing)
private val navSelectedSettleDpAnimationSpec = tween<Dp>(290, easing = FastOutSlowInEasing)
