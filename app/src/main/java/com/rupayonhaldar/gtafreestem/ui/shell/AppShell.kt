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
    val barShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    val barSurface = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.93f)
    }
    val barBorder = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.065f)
    }
    val barBottomGlow = if (isDark) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.067f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
    }
    val barContainer = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.77f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    }

    Surface(
        modifier = Modifier
            .testTag("primary-navigation-bar")
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.6.dp),
        shape = barShape,
        color = barSurface,
        tonalElevation = 4.88.dp,
        shadowElevation = 6.4.dp,
        border = BorderStroke(0.25.dp, barBorder),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(barShape)
                .shadow(
                    elevation = if (isDark) 6.5.dp else 5.2.dp,
                    shape = barShape,
                    ambientColor = barBottomGlow,
                    spotColor = barBottomGlow,
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            if (isDark) {
                                Color.White.copy(alpha = 0.011f)
                            } else {
                                Color.Black.copy(alpha = 0.007f)
                            },
                            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.034f else 0.016f),
                        ),
                    ),
                ),
            )
        NavigationBar(
            modifier = Modifier
                .padding(horizontal = 7.3.dp, top = 0.dp, bottom = 0.dp)
                .clip(barShape),
            containerColor = barContainer,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets.navigationBars.only(
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
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.085f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.018f)
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
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.072f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.018f)
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
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.065f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
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
                            isPressed && isSelected -> NAV_BOTTOM_NAV_ITEM_PRESSED_SELECTED_ELEVATION.dp
                            isSelected -> 0.55.dp
                            isPressed -> 0.dp
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
                            isPressed -> 80.dp
                            else -> 79.dp
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
                            isPressed && isSelected -> 17.2.dp
                            isSelected -> 20.dp
                            isPressed -> 18.dp
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
                            isPressed && !isSelected -> NAV_BOTTOM_NAV_ITEM_OFFSET_PRESSED_DP.dp
                            isSelected -> (-0.30).dp
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
                            isPressed && isSelected -> NAV_BOTTOM_NAV_ITEM_PRESSED_SELECTED_HEIGHT.dp
                            isSelected -> 58.5.dp
                            isPressed -> 55.5.dp
                            else -> 55.5.dp
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
                            isPressed && isSelected -> NAV_BOTTOM_NAV_ITEM_SELECTED_PRESS_SCALE
                            isSelected -> 1.003f
                            isPressed -> 0.998f
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
                            .padding(horizontal = 4.8.dp, vertical = 2.4.dp)
                            .clip(activeItemShape)
                            .shadow(
                                elevation = selectedPillElevation,
                                shape = activeItemShape,
                                ambientColor = selectedPillBorderColor.copy(alpha = if (isDark) 0.18f else 0.10f),
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
                                    width = if (isSelected || isPressed) NAV_NAV_ITEM_ACTIVE_BORDER.dp else 0.dp,
                                    color = selectedPillBorderColor,
                                ),
                                shape = activeItemShape,
                            )
                            .sizeIn(
                                minWidth = activeNavItemWidth,
                                minHeight = activeItemHeight,
                            )
                            .offset(y = activeItemOffset)
                            .scale(activeItemScale),
                    icon = {
                    val iconLift by animateDpAsState(
                        targetValue = when {
                            isPressed -> NAV_ICON_LIFT_PRESSED_DP.dp
                            isSelected -> NAV_ICON_LIFT_SELECTED_DP.dp
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
                            isPressed -> 0.8f
                            isSelected -> 2.0f
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
                                isPressed -> 1f
                                isSelected -> 1.003f
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
                            isPressed -> 0.999f
                            isSelected -> 1.002f
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
                            isSelected -> NAV_ICON_GLYPH_SELECTED_ALPHA
                            isPressed -> NAV_ICON_GLYPH_PRESSED_ALPHA
                            else -> NAV_ICON_GLYPH_UNSELECTED_ALPHA
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
                            isSelected -> NAV_ICON_CONTAINER_SELECTED_ALPHA
                            isPressed -> NAV_ICON_CONTAINER_PRESSED_ALPHA
                            else -> NAV_ICON_CONTAINER_UNSELECTED_ALPHA
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
                            isPressed -> 19.3.dp
                            isSelected -> 20.2.dp
                            else -> 19.3.dp
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
                            isPressed -> 33.2.dp
                            isSelected -> 34.8.dp
                            else -> 33.8.dp
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
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.20f)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
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
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)
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
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NAV_ICON_TINT_PRESSED_ALPHA)
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
                            targetValue = if (isSelected && isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.032f)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.045f)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.012f)
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
                            targetValue = if (isSelected && isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.018f)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.024f)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.010f)
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
                                35.dp
                            } else if (isPressed) {
                                30.dp
                            } else {
                                NAV_ICON_HALO_SIZE_UNSELECTED.dp
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
                            targetValue = if (isSelected) NAV_NAV_ITEM_ACTIVE_BORDER.dp else if (isPressed) 0.dp else 0.dp,
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
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.028f)
                                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.018f)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.028f)
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
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                                        } else if (isPressed) {
                                            Color.Transparent
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
                                            width = NAV_ICON_FACE_BORDER.dp,
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
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NAV_UNSELECTED_LABEL_ALPHA),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NAV_UNSELECTED_LABEL_ALPHA),
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
    val railShape = RoundedCornerShape(30.dp)
    val railSurface = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.875f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    }
    val railBorder = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.065f)
    }
    val railBottomGlow = if (isDark) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.067f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
    }
    val railContainer = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.77f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    }

    Surface(
        modifier = Modifier
        .fillMaxHeight()
            .widthIn(min = 87.8.dp)
            .padding(8.dp)
            .testTag("primary-navigation-rail"),
        color = railSurface,
        shape = railShape,
        tonalElevation = 4.68.dp,
        shadowElevation = 6.3.dp,
        border = BorderStroke(0.25.dp, railBorder),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(railShape)
                .shadow(
                    elevation = if (isDark) 6.35.dp else 5.0.dp,
                    shape = railShape,
                    ambientColor = railBottomGlow,
                    spotColor = railBottomGlow,
                )
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            if (isDark) {
                                Color.White.copy(alpha = 0.011f)
                            } else {
                                Color.Black.copy(alpha = 0.007f)
                            },
                            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.034f else 0.016f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        NavigationRail(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 6.9.dp, end = 6.9.dp, top = 9.4.dp, bottom = 9.4.dp)
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
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.085f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.018f)
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
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.072f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.018f)
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
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.065f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
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
                            isPressed && isSelected -> NAV_RAIL_ITEM_PRESSED_SELECTED_ELEVATION.dp
                            isSelected -> 0.55.dp
                            isPressed -> 0.dp
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
                            isSelected -> 86.dp
                            isPressed -> 86.dp
                            else -> 85.dp
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
                            isPressed && isSelected -> 17.8.dp
                            isSelected -> 18.5.dp
                            isPressed -> 16.5.dp
                            else -> 14.5.dp
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
                                isPressed && !isSelected -> NAV_RAIL_ITEM_OFFSET_PRESSED_DP.dp
                                isSelected -> (-0.32).dp
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
                            isPressed && isSelected -> NAV_RAIL_ITEM_PRESSED_SELECTED_HEIGHT.dp
                            isSelected -> 50.2.dp
                            isPressed -> 47.8.dp
                            else -> 47.8.dp
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
                            isPressed && isSelected -> NAV_RAIL_ITEM_SELECTED_PRESS_SCALE
                            isSelected -> 1.002f
                            isPressed -> 0.998f
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
                            .padding(horizontal = 2.4.dp, vertical = 2.6.dp)
                            .clip(activeItemShape)
                            .shadow(
                                elevation = selectedPillElevation,
                                shape = activeItemShape,
                                ambientColor = selectedPillBorderColor.copy(alpha = 0.18f),
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
                                    width = if (isSelected || isPressed) NAV_NAV_ITEM_ACTIVE_BORDER.dp else 0.dp,
                                    color = selectedPillBorderColor,
                                ),
                                shape = activeItemShape,
                            )
                            .sizeIn(
                                minWidth = activeNavItemWidth,
                                minHeight = activeItemHeight,
                            )
                            .offset(y = activeItemOffset)
                            .scale(activeItemScale),
                    icon = {
                    val iconLift by animateDpAsState(
                        targetValue = when {
                            isPressed -> NAV_ICON_LIFT_PRESSED_DP.dp
                            isSelected -> NAV_ICON_LIFT_SELECTED_DP.dp
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
                            isPressed -> 0.8f
                            isSelected -> 2.0f
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
                                isPressed -> 1f
                                isSelected -> 1.003f
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
                            isPressed -> 0.999f
                            isSelected -> 1.002f
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
                                isSelected -> NAV_ICON_GLYPH_SELECTED_ALPHA
                            isPressed -> NAV_ICON_GLYPH_PRESSED_ALPHA
                            else -> NAV_ICON_GLYPH_UNSELECTED_ALPHA
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
                                isSelected -> NAV_ICON_CONTAINER_SELECTED_ALPHA
                            isPressed -> NAV_ICON_CONTAINER_PRESSED_ALPHA
                            else -> NAV_ICON_CONTAINER_UNSELECTED_ALPHA
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
                            isPressed -> 19.3.dp
                            isSelected -> 20.2.dp
                            else -> 19.3.dp
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
                            isPressed -> 33.2.dp
                            isSelected -> 34.8.dp
                            else -> 33.8.dp
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
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.20f)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
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
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)
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
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NAV_ICON_TINT_PRESSED_ALPHA)
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
                            targetValue = if (isSelected && isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.032f)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.045f)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.012f)
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
                            targetValue = if (isSelected && isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.018f)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.024f)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.010f)
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
                                35.dp
                            } else if (isPressed) {
                                30.dp
                            } else {
                                NAV_ICON_HALO_SIZE_UNSELECTED.dp
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
                            targetValue = if (isSelected) NAV_NAV_ITEM_ACTIVE_BORDER.dp else if (isPressed) 0.dp else 0.dp,
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
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.028f)
                                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.018f)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.028f)
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
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                                        } else if (isPressed) {
                                            Color.Transparent
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
                                            width = NAV_ICON_FACE_BORDER.dp,
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
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NAV_UNSELECTED_LABEL_ALPHA),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NAV_UNSELECTED_LABEL_ALPHA),
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
            fontSize = if (isSelected) 12.25.sp else 11.8.sp,
            lineHeight = if (isSelected) 14.25.sp else 13.5.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = 0.01.sp,
        )
    } else {
            MaterialTheme.typography.labelSmall.copy(
            fontSize = if (isSelected) 12.4.sp else 11.75.sp,
            lineHeight = if (isSelected) 13.7.sp else 13.0.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = 0.01.sp,
        )
    }
    val labelColor by animateColorAsState(
        targetValue = when {
            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_LABEL_SELECTED_PRESS_ALPHA)
            isPressed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NAV_LABEL_PRESSED_ALPHA)
            isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NAV_LABEL_UNSELECTED_ALPHA)
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
        targetValue = when {
            isPressed && isSelected -> NAV_LABEL_SELECTED_PRESS_ALPHA_VISIBILITY
            isPressed && !isSelected -> NAV_LABEL_PRESS_ALPHA
            isSelected -> 1f
            else -> NAV_LABEL_UNSELECTED_VISIBILITY_ALPHA
        },
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
            isSelected && isPressed -> NAV_LABEL_OFFSET_SELECTED_PRESSED_DP.dp
            isSelected -> NAV_LABEL_OFFSET_SELECTED_DP.dp
            isPressed -> 0.dp
            else -> 0.dp
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
    val labelScale by animateFloatAsState(
        targetValue = when {
            isPressed && isSelected -> NAV_LABEL_SELECTED_PRESS_SCALE
            isPressed && !isSelected -> NAV_LABEL_PRESS_SCALE
            isSelected -> NAV_LABEL_SELECTED_SCALE
            else -> 1f
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
    Text(
        text = label,
        modifier = Modifier
            .offset(y = labelOffset)
                    .scale(labelScale),
        color = labelColor.copy(alpha = labelAlpha),
        style = resolvedStyle,
        maxLines = 1,
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Ellipsis,
    )
}

private val PrimaryDestination.testTag: String
    get() = "primary-navigation-${name.lowercase()}"

private val navSelectionFloatAnimationSpec = tween<Float>(240, easing = FastOutSlowInEasing)
private val navPressFloatAnimationSpec = tween<Float>(130, easing = FastOutSlowInEasing)
private val navSelectionDpAnimationSpec = tween<Dp>(240, easing = FastOutSlowInEasing)
private val navPressDpAnimationSpec = tween<Dp>(130, easing = FastOutSlowInEasing)
private val navPressColorAnimationSpec = tween<Color>(130, easing = FastOutSlowInEasing)
private val navSelectionColorAnimationSpec = tween<Color>(240, easing = FastOutSlowInEasing)
private val navSelectedSettleColorAnimationSpec = tween<Color>(220, easing = FastOutSlowInEasing)
private val navSelectedSettleFloatAnimationSpec = tween<Float>(220, easing = FastOutSlowInEasing)
private val navSelectedSettleDpAnimationSpec = tween<Dp>(220, easing = FastOutSlowInEasing)

private const val NAV_UNSELECTED_LABEL_ALPHA = 0.68f
private const val NAV_LABEL_UNSELECTED_ALPHA = 0.84f
private const val NAV_LABEL_UNSELECTED_VISIBILITY_ALPHA = 0.985f
private const val NAV_LABEL_SELECTED_SCALE = 1.012f
private const val NAV_LABEL_PRESS_SCALE = 0.9975f
private const val NAV_LABEL_PRESS_ALPHA = 0.99f
private const val NAV_LABEL_PRESSED_ALPHA = 0.82f
private const val NAV_LABEL_SELECTED_PRESS_ALPHA = 0.98f
private const val NAV_LABEL_SELECTED_PRESS_ALPHA_VISIBILITY = 0.985f
private const val NAV_LABEL_OFFSET_SELECTED_DP = -0.12f
private const val NAV_LABEL_OFFSET_SELECTED_PRESSED_DP = -0.10f
private const val NAV_LABEL_SELECTED_PRESS_SCALE = 1.0065f
private const val NAV_BOTTOM_NAV_ITEM_OFFSET_PRESSED_DP = -0.16f
private const val NAV_RAIL_ITEM_OFFSET_PRESSED_DP = -0.14f
private const val NAV_BOTTOM_NAV_ITEM_PRESSED_SELECTED_ELEVATION = 0.38f
private const val NAV_BOTTOM_NAV_ITEM_PRESSED_SELECTED_HEIGHT = 57.8f
private const val NAV_BOTTOM_NAV_ITEM_SELECTED_PRESS_SCALE = 0.9992f
private const val NAV_RAIL_ITEM_PRESSED_SELECTED_ELEVATION = 0.32f
private const val NAV_RAIL_ITEM_PRESSED_SELECTED_HEIGHT = 49.9f
private const val NAV_RAIL_ITEM_SELECTED_PRESS_SCALE = 0.9992f
private const val NAV_ICON_LIFT_SELECTED_DP = -0.12f
private const val NAV_ICON_LIFT_PRESSED_DP = -0.26f
private const val NAV_ICON_GLYPH_SELECTED_ALPHA = 1f
private const val NAV_ICON_GLYPH_PRESSED_ALPHA = 0.94f
private const val NAV_ICON_GLYPH_UNSELECTED_ALPHA = 0.86f
private const val NAV_ICON_CONTAINER_SELECTED_ALPHA = 1f
private const val NAV_ICON_CONTAINER_PRESSED_ALPHA = 0.99f
private const val NAV_ICON_CONTAINER_UNSELECTED_ALPHA = 0.86f
private const val NAV_ICON_TINT_PRESSED_ALPHA = 0.9f
private const val NAV_NAV_ITEM_ACTIVE_BORDER = 0.42f
private const val NAV_ICON_FACE_BORDER = 0.35f
private const val NAV_ICON_HALO_SIZE_UNSELECTED = 21.8f
