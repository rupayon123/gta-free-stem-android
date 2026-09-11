package com.rupayonhaldar.gtafreestem.ui.shell

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
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
    val haptic = LocalHapticFeedback.current
    val barShape = RoundedCornerShape(
        topStart = NAV_SHELL_BAR_CORNER.dp,
        topEnd = NAV_SHELL_BAR_CORNER.dp,
    )
    val barSurface = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = NAV_SHELL_SURFACE_DARK_ALPHA)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = NAV_SHELL_SURFACE_LIGHT_ALPHA)
    }
    val barBorder = if (isDark) {
        Color.White.copy(alpha = NAV_SHELL_BORDER_DARK_ALPHA)
    } else {
        Color.Black.copy(alpha = NAV_SHELL_BORDER_LIGHT_ALPHA)
    }
    val barBottomGlow = if (isDark) {
        MaterialTheme.colorScheme.primary.copy(alpha = NAV_SHELL_GLOW_DARK_ALPHA)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = NAV_SHELL_GLOW_LIGHT_ALPHA)
    }
    val barContainer = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = NAV_SHELL_CONTAINER_DARK_ALPHA)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = NAV_SHELL_CONTAINER_LIGHT_ALPHA)
    }

    Surface(
        modifier = Modifier
            .testTag("primary-navigation-bar")
            .fillMaxWidth()
            .padding(
                horizontal = NAV_BOTTOM_NAV_BAR_OUTER_HORIZONTAL_PADDING.dp,
                vertical = NAV_BOTTOM_NAV_BAR_OUTER_VERTICAL_PADDING.dp,
            ),
        shape = barShape,
        color = barSurface,
        tonalElevation = NAV_SHELL_BAR_TONAL_ELEVATION.dp,
        shadowElevation = NAV_SHELL_BAR_SHADOW_ELEVATION.dp,
        border = BorderStroke(NAV_SHELL_BAR_OUTER_BORDER_WIDTH.dp, barBorder),
    ) {
        Box {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(barShape)
                .shadow(
                    elevation = if (isDark) NAV_SHELL_BAR_SHADOW_SURFACE_DARK.dp else NAV_SHELL_BAR_SHADOW_SURFACE_LIGHT.dp,
                    shape = barShape,
                    ambientColor = barBottomGlow,
                    spotColor = barBottomGlow,
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            if (isDark) {
                                Color.White.copy(alpha = NAV_SHELL_SURFACE_TOP_GLOW_DARK_ALPHA)
                            } else {
                                Color.Black.copy(alpha = NAV_SHELL_SURFACE_TOP_GLOW_LIGHT_ALPHA)
                            },
                            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) NAV_SHELL_SURFACE_EDGE_DARK_ALPHA else NAV_SHELL_SURFACE_EDGE_LIGHT_ALPHA),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(
                    horizontal = NAV_SHELL_BAR_CENTER_GLOW_HORIZONTAL_PADDING.dp,
                    vertical = NAV_SHELL_BAR_CENTER_GLOW_VERTICAL_PADDING.dp,
                )
                .clip(barShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isDark) {
                                Color.White.copy(alpha = NAV_SHELL_BAR_CENTER_GLOW_DARK_ALPHA)
                            } else {
                                Color.Black.copy(alpha = NAV_SHELL_BAR_CENTER_GLOW_LIGHT_ALPHA)
                            },
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(NAV_SHELL_TOP_RIM_HEIGHT.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (isDark) {
                                Color.White.copy(alpha = NAV_SHELL_TOP_RIM_DARK_ALPHA)
                            } else {
                                Color.Black.copy(alpha = NAV_SHELL_TOP_RIM_LIGHT_ALPHA)
                            },
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(NAV_SHELL_BAR_SIDE_RIM_WIDTH.dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            if (isDark) {
                                Color.White.copy(alpha = NAV_SHELL_BAR_SIDE_RIM_DARK_ALPHA)
                            } else {
                                Color.Black.copy(alpha = NAV_SHELL_BAR_SIDE_RIM_LIGHT_ALPHA)
                            },
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(NAV_SHELL_BAR_SIDE_RIM_WIDTH.dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            if (isDark) {
                                Color.White.copy(alpha = NAV_SHELL_BAR_SIDE_RIM_DARK_ALPHA)
                            } else {
                                Color.Black.copy(alpha = NAV_SHELL_BAR_SIDE_RIM_LIGHT_ALPHA)
                            },
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(NAV_SHELL_BOTTOM_RIM_HEIGHT.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            if (isDark) {
                                Color.Black.copy(alpha = NAV_SHELL_BOTTOM_RIM_DARK_ALPHA)
                            } else {
                                Color.Black.copy(alpha = NAV_SHELL_BOTTOM_RIM_LIGHT_ALPHA)
                            },
                        ),
                    ),
                ),
        )
        NavigationBar(
            modifier = Modifier
                .padding(
                    start = NAV_BOTTOM_NAV_BAR_INNER_PADDING.dp,
                    end = NAV_BOTTOM_NAV_BAR_INNER_PADDING.dp,
                    top = 0.dp,
                    bottom = 0.dp,
                )
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
                    val isActivePress = isPressed && isSelected
                    val selectedPillColor by animateColorAsState(
                        targetValue = if (isActivePress) {
                            MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_SELECTED_PRESSED_ALPHA)
                        } else if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_SELECTED_ALPHA)
                        } else {
                            Color.Transparent
                        },
                        animationSpec = if (isActivePress) {
                            navPressColorAnimationSpec
                        } else {
                            navSelectedSettleColorAnimationSpec
                        },
                        label = "bottom-nav-pill-color-${destination.name}",
                    )
                    val selectedPillTopColor by animateColorAsState(
                        targetValue = if (isActivePress) {
                            MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_TOP_SELECTED_PRESSED_ALPHA)
                        } else if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_TOP_SELECTED_ALPHA)
                        } else {
                            Color.Transparent
                        },
                        animationSpec = if (isActivePress) {
                            navPressColorAnimationSpec
                        } else {
                            navSelectedSettleColorAnimationSpec
                        },
                        label = "bottom-nav-pill-top-color-${destination.name}",
                    )
                    val selectedPillBorderColor by animateColorAsState(
                        targetValue = if (isActivePress) {
                            MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_BORDER_SELECTED_PRESSED_ALPHA)
                        } else if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_BORDER_SELECTED_ALPHA)
                        } else {
                            Color.Transparent
                        },
                        animationSpec = if (isActivePress) {
                            navPressColorAnimationSpec
                        } else {
                            navSelectedSettleColorAnimationSpec
                        },
                        label = "bottom-nav-pill-border-color-${destination.name}",
                    )
                    val selectedPillElevation by animateDpAsState(
                        targetValue = if (isActivePress) {
                            NAV_BOTTOM_NAV_ITEM_PRESSED_SELECTED_ELEVATION.dp
                        } else if (isSelected) {
                            NAV_BOTTOM_NAV_ITEM_SELECTED_ELEVATION.dp
                        } else {
                            NAV_BOTTOM_NAV_ITEM_UNSELECTED_ELEVATION.dp
                        },
                        animationSpec = if (isActivePress) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectedSettleDpAnimationSpec
                        },
                        label = "bottom-nav-pill-elevation-${destination.name}",
                    )
                    val activeNavItemWidth by animateDpAsState(
                        targetValue = if (isSelected) {
                            NAV_BOTTOM_NAV_ITEM_SELECTED_WIDTH.dp
                        } else {
                            NAV_BOTTOM_NAV_ITEM_UNSELECTED_WIDTH.dp
                        },
                        animationSpec = if (isActivePress) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectedSettleDpAnimationSpec
                        },
                        label = "bottom-nav-item-width-${destination.name}",
                    )
                    val activeItemCorner by animateDpAsState(
                        targetValue = if (isActivePress) {
                            NAV_BOTTOM_NAV_ITEM_PRESSED_SELECTED_CORNER.dp
                        } else if (isSelected) {
                            NAV_BOTTOM_NAV_ITEM_SELECTED_CORNER.dp
                        } else {
                            NAV_BOTTOM_NAV_ITEM_UNPRESSED_CORNER.dp
                        },
                        animationSpec = if (isActivePress) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectedSettleDpAnimationSpec
                        },
                        label = "bottom-nav-item-corner-${destination.name}",
                    )
                    val activeItemShape = RoundedCornerShape(activeItemCorner)
                    val activeItemOffset by animateDpAsState(
                        targetValue = if (isActivePress) {
                            NAV_BOTTOM_NAV_ITEM_SELECTED_OFFSET_PRESSED_DP.dp
                        } else if (isSelected) {
                            NAV_BOTTOM_NAV_ITEM_SELECTED_OFFSET_DP.dp
                        } else if (isPressed) {
                            NAV_BOTTOM_NAV_ITEM_UNSELECTED_OFFSET_PRESSED_DP.dp
                        } else {
                            0.dp
                        },
                        animationSpec = if (isActivePress) {
                            navPressDpAnimationSpec
                            } else if (isSelected) {
                            navSelectedSettleDpAnimationSpec
                            } else {
                            navUnselectedSettleDpAnimationSpec
                            },
                        label = "bottom-nav-item-offset-${destination.name}",
                    )
                    val activeItemHeight by animateDpAsState(
                        targetValue = if (isActivePress) {
                            NAV_BOTTOM_NAV_ITEM_PRESSED_SELECTED_HEIGHT.dp
                        } else if (isSelected) {
                            NAV_BOTTOM_NAV_ITEM_SELECTED_HEIGHT.dp
                        } else {
                            NAV_BOTTOM_NAV_ITEM_UNSELECTED_HEIGHT.dp
                        },
                        animationSpec = if (isActivePress) {
                            navPressDpAnimationSpec
                            } else if (isSelected) {
                            navSelectedSettleDpAnimationSpec
                            } else {
                            navUnselectedSettleDpAnimationSpec
                            },
                        label = "bottom-nav-item-height-${destination.name}",
                    )
                    val activeItemScale by animateFloatAsState(
                        targetValue = if (isActivePress) {
                            NAV_BOTTOM_NAV_ITEM_SELECTED_PRESS_SCALE
                        } else if (isSelected) {
                            NAV_BOTTOM_NAV_ITEM_SELECTED_SCALE
                        } else {
                            1f
                        },
                        animationSpec = if (isActivePress) {
                            navPressFloatAnimationSpec
                        } else {
                            navSelectedSettleFloatAnimationSpec
                        },
                        label = "bottom-nav-item-scale-${destination.name}",
                    )
                    val activeItemBorderWidth by animateDpAsState(
                        targetValue = if (isSelected) {
                            NAV_NAV_ITEM_ACTIVE_BORDER_SELECTED.dp
                        } else {
                            0.dp
                        },
                        animationSpec = if (isActivePress) {
                            navPressDpAnimationSpec
                            } else if (isSelected) {
                            navSelectedSettleDpAnimationSpec
                            } else {
                            navUnselectedSettleDpAnimationSpec
                            },
                        label = "bottom-nav-item-border-width-${destination.name}",
                    )
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDestinationSelected(destination)
                            }
                        },
                        interactionSource = destinationInteractionSource,
                            modifier = Modifier
                            .testTag(destination.testTag)
                            .semantics(mergeDescendants = true) {
                                contentDescription = destinationLabel(destination)
                                selected = isSelected
                                role = Role.Tab
                            }
                            .padding(
                                horizontal = NAV_BOTTOM_NAV_ITEM_PADDING_HORIZONTAL.dp,
                                vertical = NAV_BOTTOM_NAV_ITEM_PADDING_VERTICAL.dp,
                            )
                            .clip(activeItemShape)
                            .shadow(
                                elevation = selectedPillElevation,
                                shape = activeItemShape,
                                ambientColor = selectedPillBorderColor.copy(
                                    alpha = if (isDark) NAV_NAV_ITEM_ACTIVE_SHADOW_DARK_ALPHA else NAV_NAV_ITEM_ACTIVE_SHADOW_LIGHT_ALPHA,
                                ),
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
                                    width = activeItemBorderWidth,
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
                        targetValue = if (isActivePress) {
                            NAV_ICON_LIFT_SELECTED_PRESSED_DP.dp
                        } else if (isSelected) {
                            NAV_ICON_LIFT_SELECTED_DP.dp
                        } else if (isPressed) {
                            NAV_ICON_UNSELECTED_PRESSED_LIFT_DP.dp
                        } else {
                            0.dp
                        },
                        animationSpec = if (isActivePress) {
                            navPressDpAnimationSpec
                            } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navUnselectedSettleDpAnimationSpec
                                },
                            label = "bottom-nav-icon-lift-${destination.name}",
                    )
                        val iconElevation by animateFloatAsState(
                        targetValue = if (isActivePress) {
                            NAV_ICON_SELECTED_PRESSED_ELEVATION
                        } else if (isSelected) {
                            NAV_ICON_SELECTED_ELEVATION
                        } else {
                            0f
                        },
                        animationSpec = if (isActivePress) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navUnselectedSettleFloatAnimationSpec
                                },
                            label = "bottom-nav-icon-elevation-${destination.name}",
                        )
                        val iconContainerScale by animateFloatAsState(
                            targetValue = if (isActivePress) {
                                NAV_ICON_BOTTOM_CONTAINER_SELECTED_PRESS_SCALE
                            } else if (isSelected) {
                                NAV_ICON_BOTTOM_CONTAINER_SELECTED_SCALE
                            } else if (isPressed) {
                                NAV_ICON_CONTAINER_UNSELECTED_PRESSED_SCALE
                            } else {
                                1f
                            },
                            animationSpec = if (isActivePress) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navUnselectedSettleFloatAnimationSpec
                                },
                            label = "bottom-nav-icon-container-scale-${destination.name}",
                        )
                        val iconScale by animateFloatAsState(
                            targetValue = if (isActivePress) {
                                NAV_ICON_BOTTOM_ICON_SCALE_SELECTED_PRESS
                            } else if (isSelected) {
                                NAV_ICON_BOTTOM_ICON_SCALE_SELECTED
                            } else if (isPressed) {
                                NAV_ICON_SCALE_UNSELECTED_PRESSED
                            } else {
                                1f
                            },
                            animationSpec = if (isActivePress) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navUnselectedSettleFloatAnimationSpec
                                },
                            label = "bottom-nav-icon-scale-${destination.name}",
                        )
                        val iconColorAnimationSpec = if (isSelected) {
                            if (isActivePress) navPressColorAnimationSpec else navSelectedSettleColorAnimationSpec
                        } else {
                            if (isPressed) navPressColorAnimationSpec else navUnselectedSettleColorAnimationSpec
                        }
                        val iconGlyphAlpha by animateFloatAsState(
                            targetValue = if (isActivePress) {
                                NAV_ICON_GLYPH_SELECTED_PRESSED_ALPHA
                            } else if (isSelected) {
                                NAV_ICON_GLYPH_SELECTED_ALPHA
                            } else if (isPressed) {
                                NAV_ICON_GLYPH_UNSELECTED_PRESSED_ALPHA
                            } else {
                                NAV_ICON_GLYPH_UNSELECTED_ALPHA
                            },
                            animationSpec = if (isActivePress) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navUnselectedSettleFloatAnimationSpec
                                },
                            label = "bottom-nav-icon-glyph-alpha-${destination.name}",
                        )
                        val iconContainerAlpha by animateFloatAsState(
                            targetValue = if (isActivePress) {
                                NAV_ICON_CONTAINER_PRESSED_SELECTED_ALPHA
                            } else if (isSelected) {
                                NAV_ICON_CONTAINER_SELECTED_ALPHA
                            } else {
                                NAV_ICON_CONTAINER_UNSELECTED_ALPHA
                            },
                            animationSpec = if (isActivePress) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navUnselectedSettleFloatAnimationSpec
                                },
                            label = "bottom-nav-icon-container-alpha-${destination.name}",
                        )
                        val iconGlyphSize by animateDpAsState(
                            targetValue = if (isActivePress) {
                                NAV_ICON_GLYPH_SELECTED_PRESSED_SIZE.dp
                            } else if (isSelected) {
                                NAV_ICON_GLYPH_SELECTED_SIZE.dp
                            } else {
                                NAV_ICON_GLYPH_UNSELECTED_SIZE.dp
                            },
                            animationSpec = if (isActivePress) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navUnselectedSettleDpAnimationSpec
                                },
                            label = "bottom-nav-icon-glyph-size-${destination.name}",
                        )
                        val iconFaceSize by animateDpAsState(
                            targetValue = if (isActivePress) {
                                NAV_ICON_FACE_SELECTED_PRESSED_SIZE.dp
                            } else if (isSelected) {
                                NAV_ICON_FACE_SELECTED_SIZE.dp
                                } else {
                                NAV_ICON_FACE_UNSELECTED_SIZE.dp
                            },
                            animationSpec = if (isActivePress) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navUnselectedSettleDpAnimationSpec
                                },
                            label = "bottom-nav-icon-face-size-${destination.name}",
                        )
                        val iconBackground by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = NAV_ICON_BACKGROUND_SELECTED_ALPHA)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = iconColorAnimationSpec,
                            label = "bottom-nav-icon-background-${destination.name}",
                        )
                        val iconFaceGlow by animateColorAsState(
                            targetValue = if (isActivePress) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_FACE_GLOW_SELECTED_PRESSED_ALPHA)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_FACE_GLOW_SELECTED_ALPHA)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = iconColorAnimationSpec,
                            label = "bottom-nav-icon-face-glow-${destination.name}",
                        )
                        val iconTint by animateColorAsState(
                            targetValue = if (isActivePress) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_TINT_SELECTED_PRESSED_ALPHA)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = iconColorAnimationSpec,
                            label = "bottom-nav-icon-tint-${destination.name}",
                        )
                        val iconHalo by animateColorAsState(
                            targetValue = if (isActivePress) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_SELECTED_PRESSED_ALPHA)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_SELECTED_ALPHA)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = iconColorAnimationSpec,
                            label = "bottom-nav-icon-halo-${destination.name}",
                        )
                        val iconInnerRim by animateColorAsState(
                            targetValue = if (isActivePress) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_INNER_RIM_SELECTED_PRESSED_ALPHA)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_INNER_RIM_SELECTED_ALPHA)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = iconColorAnimationSpec,
                            label = "bottom-nav-icon-inner-rim-${destination.name}",
                        )
                        val iconInnerRimBorderWidth by animateDpAsState(
                            targetValue = when {
                                isSelected -> NAV_ICON_INNER_RIM_BORDER_WIDTH.dp
                                else -> 0.dp
                            },
                            animationSpec = if (isActivePress) {
                                navPressDpAnimationSpec
                            } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                            } else {
                                navUnselectedSettleDpAnimationSpec
                            },
                            label = "bottom-nav-icon-inner-rim-border-width-${destination.name}",
                        )
                        val iconHaloHighlight by animateColorAsState(
                            targetValue = if (isActivePress) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_HIGHLIGHT_PRESSED_ALPHA)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_HIGHLIGHT_SELECTED_ALPHA)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = iconColorAnimationSpec,
                            label = "bottom-nav-icon-halo-highlight-${destination.name}",
                        )
                        val iconHaloSize by animateDpAsState(
                            targetValue = if (isActivePress) {
                                NAV_ICON_HALO_SIZE_SELECTED_PRESSED.dp
                            } else if (isSelected) {
                                NAV_ICON_HALO_SIZE_SELECTED.dp
                            } else {
                                NAV_ICON_HALO_SIZE_UNSELECTED.dp
                            },
                            animationSpec = if (isActivePress) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navUnselectedSettleDpAnimationSpec
                            },
                            label = "bottom-nav-icon-halo-size-${destination.name}",
                        )
                        val iconHaloScale by animateFloatAsState(
                            targetValue = if (isActivePress) {
                                NAV_ICON_HALO_SELECTED_PRESSED_SCALE
                            } else if (isSelected) {
                                NAV_ICON_HALO_SELECTED_SCALE
                            } else {
                                1f
                            },
                            animationSpec = if (isActivePress) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navUnselectedSettleFloatAnimationSpec
                                },
                            label = "bottom-nav-icon-halo-scale-${destination.name}",
                        )
                        val iconHaloBorderWidth by animateDpAsState(
                            targetValue = if (isSelected) {
                                NAV_ICON_HALO_BORDER_SELECTED.dp
                            } else {
                                0.dp
                            },
                            animationSpec = if (isActivePress) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navUnselectedSettleDpAnimationSpec
                                },
                            label = "bottom-nav-icon-halo-border-width-${destination.name}",
                        )
                        val iconShadowColor by animateColorAsState(
                            targetValue = if (isActivePress) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_SHADOW_SELECTED_PRESSED_ALPHA)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_SHADOW_SELECTED_ALPHA)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = iconColorAnimationSpec,
                            label = "bottom-nav-icon-shadow-${destination.name}",
                        )
                        val iconAmbientGlow by animateColorAsState(
                            targetValue = if (isActivePress) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_AMBIENT_GLOW_SELECTED_PRESSED_ALPHA)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_AMBIENT_GLOW_SELECTED_ALPHA)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = iconColorAnimationSpec,
                            label = "bottom-nav-icon-ambient-glow-${destination.name}",
                        )
                        val iconAmbientGlowSize by animateDpAsState(
                            targetValue = if (isActivePress) {
                                NAV_ICON_AMBIENT_GLOW_SIZE_SELECTED_PRESSED.dp
                            } else if (isSelected) {
                                NAV_ICON_AMBIENT_GLOW_SIZE_SELECTED.dp
                            } else {
                                0.dp
                            },
                            animationSpec = if (isActivePress) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navUnselectedSettleDpAnimationSpec
                            },
                            label = "bottom-nav-icon-ambient-glow-size-${destination.name}",
                        )
                        val iconAmbientGlowScale by animateFloatAsState(
                            targetValue = if (isActivePress) {
                                NAV_ICON_AMBIENT_GLOW_SELECTED_PRESSED_SCALE
                            } else if (isSelected) {
                                NAV_ICON_AMBIENT_GLOW_SELECTED_SCALE
                            } else {
                                1f
                            },
                            animationSpec = if (isActivePress) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navUnselectedSettleFloatAnimationSpec
                                },
                            label = "bottom-nav-icon-ambient-glow-scale-${destination.name}",
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
                                            MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_BORDER_SELECTED_ALPHA)
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
                                .scale(iconContainerScale * iconHaloScale),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(iconAmbientGlowSize)
                                            .scale(iconAmbientGlowScale)
                                            .alpha(iconContainerAlpha)
                                            .background(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        iconAmbientGlow,
                                                    Color.Transparent,
                                                ),
                                            ),
                                            shape = CircleShape,
                                        ),
                                )
                            }
                            if (isSelected) {
                                val innerRimSize = (iconHaloSize - NAV_ICON_INNER_RIM_INNER_PADDING.dp).coerceAtLeast(1.dp)
                                Box(
                                    modifier = Modifier
                                        .size(innerRimSize)
                                        .clip(RoundedCornerShape(innerRimSize / 2))
                                        .border(
                                            border = BorderStroke(
                                                width = iconInnerRimBorderWidth,
                                                color = iconInnerRim,
                                            ),
                                            shape = RoundedCornerShape(innerRimSize / 2),
                                        ),
                                )
                            }
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
                            isPressed = isPressed,
                            isSelected = isSelected,
                            isActivePress = isActivePress,
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NAV_UNSELECTED_LABEL_ALPHA),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NAV_UNSELECTED_LABEL_ALPHA),
                    indicatorColor = Color.Transparent,
                ),
                )
            }
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
    val haptic = LocalHapticFeedback.current
    val railShape = RoundedCornerShape(NAV_SHELL_RAIL_CORNER.dp)
    val railSurface = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = NAV_SHELL_SURFACE_DARK_ALPHA)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = NAV_SHELL_SURFACE_LIGHT_ALPHA)
    }
    val railBorder = if (isDark) {
        Color.White.copy(alpha = NAV_SHELL_BORDER_DARK_ALPHA)
    } else {
        Color.Black.copy(alpha = NAV_SHELL_BORDER_LIGHT_ALPHA)
    }
    val railBottomGlow = if (isDark) {
        MaterialTheme.colorScheme.primary.copy(alpha = NAV_SHELL_GLOW_DARK_ALPHA)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = NAV_SHELL_GLOW_LIGHT_ALPHA)
    }
    val railContainer = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = NAV_SHELL_CONTAINER_DARK_ALPHA)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = NAV_SHELL_CONTAINER_LIGHT_ALPHA)
    }

        Surface(
        modifier = Modifier
        .fillMaxHeight()
            .widthIn(min = NAV_RAIL_WIDTH.dp)
            .padding(NAV_RAIL_OUTER_PADDING.dp)
            .testTag("primary-navigation-rail"),
        color = railSurface,
        shape = railShape,
        tonalElevation = NAV_SHELL_RAIL_TONAL_ELEVATION.dp,
        shadowElevation = NAV_SHELL_RAIL_SHADOW_ELEVATION.dp,
        border = BorderStroke(NAV_SHELL_RAIL_OUTER_BORDER_WIDTH.dp, railBorder),
    ) {
        Box {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(railShape)
                .shadow(
                    elevation = if (isDark) NAV_SHELL_RAIL_SHADOW_SURFACE_DARK.dp else NAV_SHELL_RAIL_SHADOW_SURFACE_LIGHT.dp,
                    shape = railShape,
                    ambientColor = railBottomGlow,
                    spotColor = railBottomGlow,
                )
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            if (isDark) {
                                Color.White.copy(alpha = NAV_SHELL_SURFACE_TOP_GLOW_DARK_ALPHA)
                            } else {
                                Color.Black.copy(alpha = NAV_SHELL_SURFACE_TOP_GLOW_LIGHT_ALPHA)
                            },
                            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) NAV_SHELL_SURFACE_EDGE_DARK_ALPHA else NAV_SHELL_SURFACE_EDGE_LIGHT_ALPHA),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(
                    horizontal = NAV_SHELL_RAIL_CENTER_GLOW_HORIZONTAL_PADDING.dp,
                    vertical = NAV_SHELL_RAIL_CENTER_GLOW_VERTICAL_PADDING.dp,
                )
                .clip(railShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isDark) {
                                Color.White.copy(alpha = NAV_SHELL_RAIL_CENTER_GLOW_DARK_ALPHA)
                            } else {
                                Color.Black.copy(alpha = NAV_SHELL_RAIL_CENTER_GLOW_LIGHT_ALPHA)
                            },
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(NAV_SHELL_TOP_RIM_HEIGHT.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (isDark) {
                                Color.White.copy(alpha = NAV_SHELL_TOP_RIM_DARK_ALPHA)
                            } else {
                                Color.Black.copy(alpha = NAV_SHELL_TOP_RIM_LIGHT_ALPHA)
                            },
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(NAV_SHELL_BOTTOM_RIM_HEIGHT.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            if (isDark) {
                                Color.Black.copy(alpha = NAV_SHELL_BOTTOM_RIM_DARK_ALPHA)
                            } else {
                                Color.Black.copy(alpha = NAV_SHELL_BOTTOM_RIM_LIGHT_ALPHA)
                            },
                        ),
                    ),
                ),
        )
        NavigationRail(
            modifier = Modifier
                .fillMaxHeight()
                .padding(
                    start = NAV_RAIL_INNER_PADDING_HORIZONTAL.dp,
                    end = NAV_RAIL_INNER_PADDING_HORIZONTAL.dp,
                    top = NAV_RAIL_INNER_PADDING_VERTICAL.dp,
                    bottom = NAV_RAIL_INNER_PADDING_VERTICAL.dp,
                )
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
                    val isActivePress = isPressed && isSelected
                    val selectedPillColor by animateColorAsState(
                        targetValue = if (isActivePress) {
                            MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_SELECTED_PRESSED_ALPHA)
                        } else if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_SELECTED_ALPHA)
                        } else {
                            Color.Transparent
                        },
                        animationSpec = if (isActivePress) {
                            navPressColorAnimationSpec
                        } else {
                            navSelectedSettleColorAnimationSpec
                        },
                        label = "rail-pill-color-${destination.name}",
                    )
                    val selectedPillTopColor by animateColorAsState(
                        targetValue = if (isActivePress) {
                            MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_TOP_SELECTED_PRESSED_ALPHA)
                        } else if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_TOP_SELECTED_ALPHA)
                        } else {
                            Color.Transparent
                        },
                        animationSpec = if (isActivePress) {
                            navPressColorAnimationSpec
                        } else {
                            navSelectedSettleColorAnimationSpec
                        },
                        label = "rail-pill-top-color-${destination.name}",
                    )
                    val selectedPillBorderColor by animateColorAsState(
                        targetValue = if (isActivePress) {
                            MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_BORDER_SELECTED_PRESSED_ALPHA)
                        } else if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_BORDER_SELECTED_ALPHA)
                        } else {
                            Color.Transparent
                        },
                        animationSpec = if (isActivePress) {
                            navPressColorAnimationSpec
                        } else {
                            navSelectedSettleColorAnimationSpec
                        },
                        label = "rail-pill-border-color-${destination.name}",
                    )
                    val selectedPillElevation by animateDpAsState(
                        targetValue = if (isActivePress) {
                            NAV_RAIL_ITEM_PRESSED_SELECTED_ELEVATION.dp
                        } else if (isSelected) {
                            NAV_RAIL_ITEM_SELECTED_ELEVATION.dp
                        } else {
                            NAV_RAIL_ITEM_UNSELECTED_ELEVATION.dp
                        },
                        animationSpec = if (isActivePress) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectedSettleDpAnimationSpec
                        },
                        label = "rail-pill-elevation-${destination.name}",
                    )
                    val activeNavItemWidth by animateDpAsState(
                        targetValue = if (isSelected) {
                            NAV_RAIL_ITEM_SELECTED_WIDTH.dp
                        } else {
                            NAV_RAIL_ITEM_UNSELECTED_WIDTH.dp
                        },
                        animationSpec = if (isActivePress) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectedSettleDpAnimationSpec
                        },
                        label = "rail-item-width-${destination.name}",
                    )
                    val activeItemCorner by animateDpAsState(
                        targetValue = if (isActivePress) {
                            NAV_RAIL_ITEM_PRESSED_SELECTED_CORNER.dp
                        } else if (isSelected) {
                            NAV_RAIL_ITEM_SELECTED_CORNER.dp
                        } else {
                            NAV_RAIL_ITEM_UNPRESSED_CORNER.dp
                        },
                        animationSpec = if (isActivePress) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectedSettleDpAnimationSpec
                        },
                        label = "rail-item-corner-${destination.name}",
                    )
                    val activeItemShape = RoundedCornerShape(activeItemCorner)
                    val activeItemOffset by animateDpAsState(
                        targetValue = if (isActivePress) {
                            NAV_RAIL_ITEM_SELECTED_OFFSET_PRESSED_DP.dp
                        } else if (isSelected) {
                            NAV_RAIL_ITEM_SELECTED_OFFSET_DP.dp
                        } else if (isPressed) {
                            NAV_RAIL_ITEM_UNSELECTED_OFFSET_PRESSED_DP.dp
                        } else {
                            0.dp
                        },
                        animationSpec = if (isActivePress) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectedSettleDpAnimationSpec
                        },
                        label = "rail-item-offset-${destination.name}",
                    )
                    val activeItemHeight by animateDpAsState(
                        targetValue = if (isActivePress) {
                            NAV_RAIL_ITEM_PRESSED_SELECTED_HEIGHT.dp
                        } else if (isSelected) {
                            NAV_RAIL_ITEM_SELECTED_HEIGHT.dp
                        } else {
                            NAV_RAIL_ITEM_UNSELECTED_HEIGHT.dp
                        },
                        animationSpec = if (isActivePress) {
                            navPressDpAnimationSpec
                        } else {
                            navSelectedSettleDpAnimationSpec
                        },
                        label = "rail-item-height-${destination.name}",
                    )
                    val activeItemScale by animateFloatAsState(
                        targetValue = if (isActivePress) {
                            NAV_RAIL_ITEM_SELECTED_PRESS_SCALE
                        } else if (isSelected) {
                            NAV_RAIL_ITEM_SELECTED_SCALE
                        } else {
                            1f
                        },
                        animationSpec = if (isActivePress) {
                            navPressFloatAnimationSpec
                        } else {
                            navSelectedSettleFloatAnimationSpec
                        },
                        label = "rail-item-scale-${destination.name}",
                    )
                    val activeItemBorderWidth by animateDpAsState(
                        targetValue = if (isSelected) {
                            NAV_NAV_ITEM_ACTIVE_BORDER_SELECTED.dp
                        } else {
                            0.dp
                        },
                        animationSpec = if (isActivePress) {
                            navPressDpAnimationSpec
                            } else if (isSelected) {
                            navSelectedSettleDpAnimationSpec
                            } else {
                            navUnselectedSettleDpAnimationSpec
                            },
                        label = "rail-item-border-width-${destination.name}",
                    )
                    NavigationRailItem(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDestinationSelected(destination)
                            }
                        },
                        interactionSource = destinationInteractionSource,
                        modifier = Modifier
                            .testTag(destination.testTag)
                            .semantics(mergeDescendants = true) {
                                contentDescription = destinationLabel(destination)
                                selected = isSelected
                                role = Role.Tab
                            }
                            .padding(
                                horizontal = NAV_RAIL_ITEM_PADDING_HORIZONTAL.dp,
                                vertical = NAV_RAIL_ITEM_PADDING_VERTICAL.dp,
                            )
                            .clip(activeItemShape)
                            .shadow(
                                elevation = selectedPillElevation,
                                shape = activeItemShape,
                                ambientColor = selectedPillBorderColor.copy(
                                    alpha = if (isDark) NAV_NAV_ITEM_ACTIVE_SHADOW_DARK_ALPHA else NAV_NAV_ITEM_ACTIVE_SHADOW_LIGHT_ALPHA,
                                ),
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
                                    width = activeItemBorderWidth,
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
                        targetValue = if (isActivePress) {
                            NAV_ICON_RAIL_LIFT_SELECTED_PRESSED_DP.dp
                        } else if (isSelected) {
                            NAV_ICON_RAIL_LIFT_SELECTED_DP.dp
                        } else if (isPressed) {
                            NAV_ICON_RAIL_UNSELECTED_PRESSED_LIFT_DP.dp
                        } else {
                            0.dp
                        },
                        animationSpec = if (isActivePress) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navUnselectedSettleDpAnimationSpec
                                },
                            label = "rail-icon-lift-${destination.name}",
                        )
                        val iconElevation by animateFloatAsState(
                        targetValue = if (isActivePress) {
                            NAV_ICON_SELECTED_PRESSED_ELEVATION
                        } else if (isSelected) {
                            NAV_ICON_SELECTED_ELEVATION
                        } else {
                            0f
                        },
                        animationSpec = if (isActivePress) {
                            navPressFloatAnimationSpec
                            } else if (isSelected) {
                            navSelectedSettleFloatAnimationSpec
                            } else {
                            navUnselectedSettleFloatAnimationSpec
                            },
                            label = "rail-icon-elevation-${destination.name}",
                        )
                    val iconContainerScale by animateFloatAsState(
                        targetValue = if (isActivePress) {
                            NAV_ICON_RAIL_CONTAINER_SELECTED_PRESS_SCALE
                        } else if (isSelected) {
                            NAV_ICON_CONTAINER_SELECTED_SCALE
                        } else if (isPressed) {
                            NAV_ICON_CONTAINER_UNSELECTED_PRESSED_SCALE
                        } else {
                            1f
                        },
                        animationSpec = if (isActivePress) {
                            navPressFloatAnimationSpec
                            } else if (isSelected) {
                            navSelectedSettleFloatAnimationSpec
                            } else {
                            navUnselectedSettleFloatAnimationSpec
                            },
                            label = "rail-icon-container-scale-${destination.name}",
                        )
                        val iconScale by animateFloatAsState(
                            targetValue = if (isActivePress) {
                                NAV_ICON_SCALE_SELECTED_PRESS_SCALE
                            } else if (isSelected) {
                                NAV_ICON_SCALE_SELECTED
                            } else if (isPressed) {
                                NAV_ICON_SCALE_UNSELECTED_PRESSED
                            } else {
                                1f
                            },
                        animationSpec = if (isActivePress) {
                            navPressFloatAnimationSpec
                            } else if (isSelected) {
                            navSelectedSettleFloatAnimationSpec
                            } else {
                            navUnselectedSettleFloatAnimationSpec
                            },
                            label = "rail-icon-scale-${destination.name}",
                        )
                        val iconGlyphAlpha by animateFloatAsState(
                            targetValue = if (isActivePress) {
                                NAV_ICON_GLYPH_SELECTED_PRESSED_ALPHA
                            } else if (isSelected) {
                                NAV_ICON_GLYPH_SELECTED_ALPHA
                            } else if (isPressed) {
                                NAV_ICON_GLYPH_UNSELECTED_PRESSED_ALPHA
                            } else {
                                NAV_ICON_GLYPH_UNSELECTED_ALPHA
                            },
                        animationSpec = if (isActivePress) {
                            navPressFloatAnimationSpec
                            } else if (isSelected) {
                            navSelectedSettleFloatAnimationSpec
                            } else {
                            navUnselectedSettleFloatAnimationSpec
                            },
                            label = "rail-icon-glyph-alpha-${destination.name}",
                        )
                        val iconContainerAlpha by animateFloatAsState(
                            targetValue = if (isActivePress) {
                                NAV_ICON_CONTAINER_PRESSED_SELECTED_ALPHA
                            } else if (isSelected) {
                                NAV_ICON_CONTAINER_SELECTED_ALPHA
                            } else {
                                NAV_ICON_CONTAINER_UNSELECTED_ALPHA
                            },
                            animationSpec = if (isActivePress) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navUnselectedSettleFloatAnimationSpec
                                },
                            label = "rail-icon-container-alpha-${destination.name}",
                        )
                        val iconGlyphSize by animateDpAsState(
                        targetValue = if (isActivePress) {
                            NAV_ICON_GLYPH_SELECTED_PRESSED_SIZE.dp
                        } else if (isSelected) {
                            NAV_ICON_GLYPH_SELECTED_SIZE.dp
                            } else {
                            NAV_ICON_GLYPH_UNSELECTED_SIZE.dp
                            },
                            animationSpec = if (isActivePress) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navUnselectedSettleDpAnimationSpec
                                },
                            label = "rail-icon-glyph-size-${destination.name}",
                        )
                        val iconFaceSize by animateDpAsState(
                        targetValue = if (isActivePress) {
                            NAV_ICON_FACE_SELECTED_PRESSED_SIZE.dp
                        } else if (isSelected) {
                            NAV_ICON_FACE_SELECTED_SIZE.dp
                            } else {
                            NAV_ICON_FACE_UNSELECTED_SIZE.dp
                            },
                        animationSpec = if (isActivePress) {
                            navPressDpAnimationSpec
                            } else if (isSelected) {
                            navSelectedSettleDpAnimationSpec
                            } else {
                            navUnselectedSettleDpAnimationSpec
                            },
                            label = "rail-icon-face-size-${destination.name}",
                        )
                        val iconColorAnimationSpec = if (isSelected) {
                            if (isActivePress) navPressColorAnimationSpec else navSelectedSettleColorAnimationSpec
                        } else {
                            if (isPressed) navPressColorAnimationSpec else navUnselectedSettleColorAnimationSpec
                        }
                        val iconBackground by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = NAV_ICON_BACKGROUND_SELECTED_ALPHA)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = if (isActivePress) {
                                navPressColorAnimationSpec
                            } else {
                                iconColorAnimationSpec,
                            },
                            label = "rail-icon-background-${destination.name}",
                        )
                        val iconFaceGlow by animateColorAsState(
                            targetValue = if (isActivePress) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_FACE_GLOW_SELECTED_PRESSED_ALPHA)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_FACE_GLOW_SELECTED_ALPHA)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = if (isActivePress) {
                                navPressColorAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleColorAnimationSpec
                                } else {
                                iconColorAnimationSpec
                                },
                            label = "rail-icon-face-glow-${destination.name}",
                        )
                        val iconTint by animateColorAsState(
                            targetValue = if (isActivePress) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_TINT_SELECTED_PRESSED_ALPHA)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = if (isActivePress) {
                                navPressColorAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleColorAnimationSpec
                                } else {
                                iconColorAnimationSpec
                                },
                            label = "rail-icon-tint-${destination.name}",
                        )
                        val iconHalo by animateColorAsState(
                            targetValue = if (isActivePress) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_SELECTED_PRESSED_ALPHA)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_SELECTED_ALPHA)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = if (isActivePress) {
                                navPressColorAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleColorAnimationSpec
                                } else {
                                iconColorAnimationSpec
                                },
                            label = "rail-icon-halo-${destination.name}",
                        )
                        val iconInnerRim by animateColorAsState(
                            targetValue = if (isActivePress) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_INNER_RIM_SELECTED_PRESSED_ALPHA)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_INNER_RIM_SELECTED_ALPHA)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = iconColorAnimationSpec,
                            label = "rail-icon-inner-rim-${destination.name}",
                        )
                        val iconInnerRimBorderWidth by animateDpAsState(
                            targetValue = when {
                                isSelected -> NAV_ICON_INNER_RIM_BORDER_WIDTH.dp
                                else -> 0.dp
                            },
                            animationSpec = if (isActivePress) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navUnselectedSettleDpAnimationSpec
                            },
                            label = "rail-icon-inner-rim-border-width-${destination.name}",
                        )
                        val iconHaloHighlight by animateColorAsState(
                            targetValue = if (isActivePress) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_HIGHLIGHT_PRESSED_ALPHA)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_HIGHLIGHT_SELECTED_ALPHA)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = if (isActivePress) {
                                navPressColorAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleColorAnimationSpec
                                } else {
                                iconColorAnimationSpec
                                },
                            label = "rail-icon-halo-highlight-${destination.name}",
                        )
                        val iconHaloSize by animateDpAsState(
                            targetValue = if (isActivePress) {
                                NAV_ICON_HALO_SIZE_SELECTED_PRESSED.dp
                            } else if (isSelected) {
                                NAV_ICON_HALO_SIZE_SELECTED.dp
                            } else {
                                NAV_ICON_HALO_SIZE_UNSELECTED.dp
                            },
                        animationSpec = if (isActivePress) {
                            navPressDpAnimationSpec
                            } else if (isSelected) {
                            navSelectedSettleDpAnimationSpec
                            } else {
                            navUnselectedSettleDpAnimationSpec
                            },
                            label = "rail-icon-halo-size-${destination.name}",
                        )
                        val iconHaloScale by animateFloatAsState(
                            targetValue = if (isActivePress) {
                                NAV_ICON_RAIL_HALO_SELECTED_PRESSED_SCALE
                            } else if (isSelected) {
                                NAV_ICON_RAIL_HALO_SELECTED_SCALE
                            } else {
                                1f
                            },
                        animationSpec = if (isActivePress) {
                            navPressFloatAnimationSpec
                            } else if (isSelected) {
                            navSelectedSettleFloatAnimationSpec
                            } else {
                            navUnselectedSettleFloatAnimationSpec
                            },
                            label = "rail-icon-halo-scale-${destination.name}",
                        )
                        val iconHaloBorderWidth by animateDpAsState(
                            targetValue = if (isSelected) {
                                NAV_ICON_HALO_BORDER_SELECTED.dp
                            } else {
                                0.dp
                            },
                            animationSpec = if (isActivePress) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navUnselectedSettleDpAnimationSpec
                                },
                            label = "rail-icon-halo-border-width-${destination.name}",
                        )
                        val iconShadowColor by animateColorAsState(
                            targetValue = if (isActivePress) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_SHADOW_SELECTED_PRESSED_ALPHA)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_SHADOW_SELECTED_ALPHA)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = if (isActivePress) {
                                navPressColorAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleColorAnimationSpec
                                } else {
                                iconColorAnimationSpec
                                },
                            label = "rail-icon-shadow-${destination.name}",
                        )
                        val iconAmbientGlow by animateColorAsState(
                            targetValue = if (isActivePress) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_AMBIENT_GLOW_SELECTED_PRESSED_ALPHA)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_AMBIENT_GLOW_SELECTED_ALPHA)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = iconColorAnimationSpec,
                            label = "rail-icon-ambient-glow-${destination.name}",
                        )
                        val iconAmbientGlowSize by animateDpAsState(
                            targetValue = if (isActivePress) {
                                NAV_ICON_AMBIENT_GLOW_SIZE_SELECTED_PRESSED.dp
                            } else if (isSelected) {
                                NAV_ICON_AMBIENT_GLOW_SIZE_SELECTED.dp
                            } else {
                                0.dp
                            },
                            animationSpec = if (isActivePress) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navUnselectedSettleDpAnimationSpec
                                },
                            label = "rail-icon-ambient-glow-size-${destination.name}",
                        )
                        val iconAmbientGlowScale by animateFloatAsState(
                            targetValue = if (isActivePress) {
                                NAV_ICON_AMBIENT_GLOW_SELECTED_PRESSED_SCALE
                            } else if (isSelected) {
                                NAV_ICON_AMBIENT_GLOW_SELECTED_SCALE
                            } else {
                                1f
                            },
                            animationSpec = if (isActivePress) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navUnselectedSettleFloatAnimationSpec
                                },
                            label = "rail-icon-ambient-glow-scale-${destination.name}",
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
                                            MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_BORDER_SELECTED_ALPHA)
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
                            .scale(iconContainerScale * iconHaloScale),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(iconAmbientGlowSize)
                                            .scale(iconAmbientGlowScale)
                                            .alpha(iconContainerAlpha)
                                            .background(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        iconAmbientGlow,
                                                    Color.Transparent,
                                                ),
                                            ),
                                            shape = CircleShape,
                                        ),
                                )
                            }
                            if (isSelected) {
                                val innerRimSize = (iconHaloSize - NAV_ICON_INNER_RIM_INNER_PADDING.dp).coerceAtLeast(1.dp)
                                Box(
                                    modifier = Modifier
                                        .size(innerRimSize)
                                        .clip(RoundedCornerShape(innerRimSize / 2))
                                        .border(
                                            border = BorderStroke(
                                                width = iconInnerRimBorderWidth,
                                                color = iconInnerRim,
                                            ),
                                            shape = RoundedCornerShape(innerRimSize / 2),
                                        ),
                                )
                            }
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
                            isPressed = isPressed,
                            isSelected = isSelected,
                            isActivePress = isActivePress,
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NAV_UNSELECTED_LABEL_ALPHA),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NAV_UNSELECTED_LABEL_ALPHA),
                    indicatorColor = Color.Transparent,
                ),
                )
            }
        }
        }
    }
}

@Composable
private fun DestinationLabel(
    label: String,
    compact: Boolean,
    isPressed: Boolean = false,
    isSelected: Boolean = false,
    isActivePress: Boolean = false,
) {
    val selectedPressedOffsetDp = if (compact) {
        NAV_LABEL_COMPACT_SELECTED_PRESSED_OFFSET_DP
    } else {
        NAV_LABEL_SELECTED_PRESSED_OFFSET_DP
    }
    val selectedOffsetDp = if (compact) {
        NAV_LABEL_COMPACT_OFFSET_SELECTED_DP
    } else {
        NAV_LABEL_OFFSET_SELECTED_DP
    }
    val selectedScale = if (compact) {
        NAV_LABEL_COMPACT_SELECTED_SCALE
    } else {
        NAV_LABEL_SELECTED_SCALE
    }
    val destinationSelectedScale = if (isActivePress) {
        if (compact) {
            NAV_LABEL_COMPACT_SELECTED_PRESSED_SCALE
        } else {
            NAV_LABEL_SELECTED_PRESSED_SCALE
        }
    } else {
        selectedScale
    }
    val destinationSelectedOffsetDp = if (isActivePress) {
        selectedPressedOffsetDp
    } else {
        selectedOffsetDp
    }
    val selectedFontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
    val unselectedLetterSpacing = if (compact) {
        NAV_LABEL_COMPACT_UNSELECTED_LETTER_SPACING
    } else {
        NAV_LABEL_UNSELECTED_LETTER_SPACING
    }
    val selectedLetterSpacing = if (compact) {
        NAV_LABEL_COMPACT_SELECTED_LETTER_SPACING
    } else {
        NAV_LABEL_SELECTED_LETTER_SPACING
    }
    val resolvedStyle = if (compact) {
        MaterialTheme.typography.labelSmall.copy(
            fontSize = if (isSelected) NAV_LABEL_COMPACT_FONT_SELECTED.sp else NAV_LABEL_COMPACT_FONT_UNSELECTED.sp,
            lineHeight = if (isSelected) NAV_LABEL_COMPACT_LINE_HEIGHT_SELECTED.sp else NAV_LABEL_COMPACT_LINE_HEIGHT_UNSELECTED.sp,
            fontWeight = selectedFontWeight,
            letterSpacing = if (isSelected) selectedLetterSpacing.sp else unselectedLetterSpacing.sp,
        )
    } else {
        MaterialTheme.typography.labelSmall.copy(
            fontSize = if (isSelected) NAV_LABEL_RAIL_FONT_SELECTED.sp else NAV_LABEL_RAIL_FONT_UNSELECTED.sp,
            lineHeight = if (isSelected) NAV_LABEL_RAIL_LINE_HEIGHT_SELECTED.sp else NAV_LABEL_RAIL_LINE_HEIGHT_UNSELECTED.sp,
            fontWeight = selectedFontWeight,
            letterSpacing = if (isSelected) selectedLetterSpacing.sp else unselectedLetterSpacing.sp,
        )
    }
    val labelColor by animateColorAsState(
        targetValue = when {
            isSelected && isActivePress -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_LABEL_SELECTED_ALPHA * NAV_LABEL_SELECTED_PRESSED_ALPHA)
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_LABEL_SELECTED_ALPHA)
            isPressed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NAV_LABEL_UNSELECTED_PRESSED_ALPHA)
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NAV_LABEL_UNSELECTED_ALPHA)
        },
        animationSpec = if (isSelected) {
            if (isActivePress) {
                navPressColorAnimationSpec
            } else {
                navLabelColorAnimationSpec
            }
        } else if (isPressed) {
            navPressColorAnimationSpec
        } else {
            navLabelUnselectedColorAnimationSpec
        },
        label = "destination-label-color",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (isSelected) {
            1f
        } else if (isPressed) {
            NAV_LABEL_UNSELECTED_PRESSED_ALPHA
        } else {
            NAV_LABEL_UNSELECTED_VISIBILITY_ALPHA
        },
        animationSpec = if (isPressed && !isSelected) {
            navPressFloatAnimationSpec
        } else {
            navLabelAlphaAnimationSpec
        },
        label = "destination-label-alpha",
    )
    val labelOffset by animateDpAsState(
        targetValue = if (isSelected) {
            destinationSelectedOffsetDp.dp
        } else if (isPressed) {
            NAV_LABEL_UNSELECTED_PRESSED_OFFSET_DP.dp
        } else {
            0.dp
        },
        animationSpec = if (isSelected) {
            navSelectedSettleDpAnimationSpec
        } else if (isPressed) {
            navPressDpAnimationSpec
        } else {
            navUnselectedSettleDpAnimationSpec
        },
        label = "destination-label-offset",
    )
    val labelScale by animateFloatAsState(
        targetValue = if (isSelected) {
            destinationSelectedScale
        } else if (isPressed) {
            NAV_LABEL_UNSELECTED_PRESSED_SCALE
        } else {
            NAV_LABEL_UNSELECTED_SCALE
        },
        animationSpec = if (isSelected) {
            navLabelScaleAnimationSpec
        } else if (isPressed) {
            navPressFloatAnimationSpec
        } else {
            navLabelScaleAnimationSpec
        },
        label = "destination-label-scale",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset(y = labelOffset)
            .scale(labelScale),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = labelColor.copy(alpha = labelAlpha),
                style = resolvedStyle,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val PrimaryDestination.testTag: String
    get() = "primary-navigation-${name.lowercase()}"

private val navSelectionFloatAnimationSpec = tween<Float>(186, easing = FastOutSlowInEasing)
private val navPressFloatAnimationSpec = tween<Float>(108, easing = FastOutSlowInEasing)
private val navSelectionDpAnimationSpec = tween<Dp>(184, easing = FastOutSlowInEasing)
private val navPressDpAnimationSpec = tween<Dp>(106, easing = FastOutSlowInEasing)
private val navPressColorAnimationSpec = tween<Color>(104, easing = FastOutSlowInEasing)
private val navSelectionColorAnimationSpec = tween<Color>(206, easing = FastOutSlowInEasing)
private val navSelectedSettleColorAnimationSpec = tween<Color>(202, easing = FastOutSlowInEasing)
private val navSelectedSettleFloatAnimationSpec = tween<Float>(202, easing = FastOutSlowInEasing)
private val navSelectedSettleDpAnimationSpec = tween<Dp>(202, easing = FastOutSlowInEasing)
private val navUnselectedSettleColorAnimationSpec = tween<Color>(210, easing = FastOutSlowInEasing)
private val navUnselectedSettleFloatAnimationSpec = tween<Float>(170, easing = FastOutSlowInEasing)
private val navUnselectedSettleDpAnimationSpec = tween<Dp>(170, easing = FastOutSlowInEasing)
private val navLabelColorAnimationSpec = tween<Color>(178, easing = FastOutSlowInEasing)
private val navLabelUnselectedColorAnimationSpec = tween<Color>(162, easing = FastOutSlowInEasing)
private val navLabelAlphaAnimationSpec = tween<Float>(168, easing = FastOutSlowInEasing)
private val navLabelScaleAnimationSpec = tween<Float>(176, easing = FastOutSlowInEasing)

private const val NAV_UNSELECTED_LABEL_ALPHA = 0.64f
private const val NAV_LABEL_UNSELECTED_ALPHA = 0.7f
private const val NAV_LABEL_UNSELECTED_PRESSED_ALPHA = 0.76f
private const val NAV_LABEL_SELECTED_ALPHA = 1f
private const val NAV_LABEL_SELECTED_PRESSED_ALPHA = 0.985f
private const val NAV_LABEL_UNSELECTED_VISIBILITY_ALPHA = 0.72f
private const val NAV_LABEL_UNSELECTED_PRESSED_OFFSET_DP = -0.15f
private const val NAV_LABEL_UNSELECTED_SCALE = 1f
private const val NAV_LABEL_SELECTED_SCALE = 1.03f
private const val NAV_LABEL_SELECTED_PRESSED_SCALE = 1.014f
private const val NAV_LABEL_COMPACT_SELECTED_PRESSED_SCALE = 1.02f
private const val NAV_LABEL_COMPACT_FONT_SELECTED = 12.05f
private const val NAV_LABEL_COMPACT_FONT_UNSELECTED = 11.1f
private const val NAV_LABEL_COMPACT_LINE_HEIGHT_SELECTED = 13.90f
private const val NAV_LABEL_COMPACT_LINE_HEIGHT_UNSELECTED = 13.18f
private const val NAV_LABEL_RAIL_FONT_SELECTED = 11.95f
private const val NAV_LABEL_RAIL_FONT_UNSELECTED = 11.05f
private const val NAV_LABEL_RAIL_LINE_HEIGHT_SELECTED = 13.60f
private const val NAV_LABEL_RAIL_LINE_HEIGHT_UNSELECTED = 12.76f
private const val NAV_LABEL_SELECTED_LETTER_SPACING = 0.0f
private const val NAV_LABEL_UNSELECTED_LETTER_SPACING = 0.008f
private const val NAV_LABEL_COMPACT_SELECTED_LETTER_SPACING = 0.0f
private const val NAV_LABEL_COMPACT_UNSELECTED_LETTER_SPACING = 0.011f
private const val NAV_LABEL_OFFSET_SELECTED_DP = -0.075f
private const val NAV_LABEL_COMPACT_OFFSET_SELECTED_DP = -0.085f
private const val NAV_LABEL_SELECTED_PRESSED_OFFSET_DP = -0.060f
private const val NAV_LABEL_COMPACT_SELECTED_PRESSED_OFFSET_DP = -0.070f
private const val NAV_LABEL_COMPACT_SELECTED_SCALE = 1.027f
private const val NAV_LABEL_UNSELECTED_PRESSED_SCALE = 1.008f
private const val NAV_BOTTOM_NAV_BAR_OUTER_HORIZONTAL_PADDING = 12.5f
private const val NAV_BOTTOM_NAV_BAR_OUTER_VERTICAL_PADDING = 4.2f
private const val NAV_BOTTOM_NAV_BAR_INNER_PADDING = 9.8f
private const val NAV_BOTTOM_NAV_ITEM_SELECTED_WIDTH = 85f
private const val NAV_BOTTOM_NAV_ITEM_UNSELECTED_WIDTH = 75.4f
private const val NAV_BOTTOM_NAV_ITEM_PADDING_HORIZONTAL = 5.2f
private const val NAV_BOTTOM_NAV_ITEM_PADDING_VERTICAL = 3.7f
private const val NAV_RAIL_WIDTH = 92f
private const val NAV_RAIL_OUTER_PADDING = 8.2f
    private const val NAV_RAIL_INNER_PADDING_HORIZONTAL = 7.8f
private const val NAV_RAIL_INNER_PADDING_VERTICAL = 8.2f
private const val NAV_RAIL_ITEM_SELECTED_WIDTH = 85.2f
private const val NAV_RAIL_ITEM_UNSELECTED_WIDTH = 85.2f
private const val NAV_RAIL_ITEM_PADDING_HORIZONTAL = 2.5f
    private const val NAV_RAIL_ITEM_PADDING_VERTICAL = 3f
private const val NAV_BOTTOM_NAV_ITEM_SELECTED_OFFSET_PRESSED_DP = -0.16f
private const val NAV_RAIL_ITEM_SELECTED_OFFSET_PRESSED_DP = -0.15f
private const val NAV_BOTTOM_NAV_ITEM_UNSELECTED_OFFSET_PRESSED_DP = -0.08f
private const val NAV_RAIL_ITEM_UNSELECTED_OFFSET_PRESSED_DP = -0.075f
private const val NAV_BOTTOM_NAV_ITEM_SELECTED_OFFSET_DP = -0.16f
private const val NAV_RAIL_ITEM_SELECTED_OFFSET_DP = -0.15f
private const val NAV_BOTTOM_NAV_ITEM_SELECTED_ELEVATION = 0.7f
private const val NAV_BOTTOM_NAV_ITEM_UNSELECTED_ELEVATION = 0.05f
    private const val NAV_RAIL_ITEM_SELECTED_ELEVATION = 0.6f
    private const val NAV_RAIL_ITEM_UNSELECTED_ELEVATION = 0.06f
private const val NAV_BOTTOM_NAV_ITEM_PRESSED_SELECTED_ELEVATION = 0.34f
private const val NAV_BOTTOM_NAV_ITEM_PRESSED_SELECTED_HEIGHT = 58.4f
private const val NAV_BOTTOM_NAV_ITEM_SELECTED_HEIGHT = 59.8f
private const val NAV_BOTTOM_NAV_ITEM_UNSELECTED_HEIGHT = 56.8f
private const val NAV_BOTTOM_NAV_ITEM_SELECTED_SCALE = 1f
private const val NAV_BOTTOM_NAV_ITEM_SELECTED_CORNER = 20.4f
private const val NAV_BOTTOM_NAV_ITEM_PRESSED_SELECTED_CORNER = 16.8f
private const val NAV_BOTTOM_NAV_ITEM_UNPRESSED_CORNER = 16.5f
private const val NAV_BOTTOM_NAV_ITEM_SELECTED_PRESS_SCALE = 0.9990f
private const val NAV_RAIL_ITEM_PRESSED_SELECTED_ELEVATION = 0.31f
private const val NAV_RAIL_ITEM_PRESSED_SELECTED_HEIGHT = 51.0f
private const val NAV_RAIL_ITEM_SELECTED_HEIGHT = 52.4f
private const val NAV_RAIL_ITEM_UNSELECTED_HEIGHT = 48.6f
private const val NAV_RAIL_ITEM_SELECTED_SCALE = 1f
private const val NAV_RAIL_ITEM_SELECTED_CORNER = 19.45f
private const val NAV_RAIL_ITEM_PRESSED_SELECTED_CORNER = 18.28f
private const val NAV_RAIL_ITEM_UNPRESSED_CORNER = 14.56f
    private const val NAV_RAIL_ITEM_SELECTED_PRESS_SCALE = 0.9972f
private const val NAV_ICON_LIFT_SELECTED_DP = -0.0638f
private const val NAV_ICON_LIFT_SELECTED_PRESSED_DP = -0.064f
private const val NAV_ICON_UNSELECTED_PRESSED_LIFT_DP = -0.053f
private const val NAV_ICON_HALO_SELECTED_SCALE = 1.0035f
private const val NAV_ICON_HALO_SELECTED_PRESSED_SCALE = 1.012f
private const val NAV_ICON_RAIL_LIFT_SELECTED_DP = -0.0682f
private const val NAV_ICON_RAIL_LIFT_SELECTED_PRESSED_DP = -0.07f
private const val NAV_ICON_RAIL_UNSELECTED_PRESSED_LIFT_DP = -0.056f
private const val NAV_ICON_RAIL_HALO_SELECTED_SCALE = 1.006f
private const val NAV_ICON_RAIL_HALO_SELECTED_PRESSED_SCALE = 1.012f
private const val NAV_ICON_GLYPH_SELECTED_ALPHA = 0.9865f
    private const val NAV_ICON_GLYPH_UNSELECTED_ALPHA = 0.82f
private const val NAV_ICON_CONTAINER_SELECTED_ALPHA = 0.9f
private const val NAV_ICON_CONTAINER_PRESSED_SELECTED_ALPHA = 0.847f
private const val NAV_ICON_SELECTED_PRESSED_ELEVATION = 0.873f
private const val NAV_ICON_SELECTED_ELEVATION = 0.9785f
private const val NAV_ICON_TINT_SELECTED_PRESSED_ALPHA = 0.985f
private const val NAV_ICON_RAIL_CONTAINER_SELECTED_PRESS_SCALE = 1.004f
private const val NAV_ICON_CONTAINER_SELECTED_SCALE = 1.004f
private const val NAV_ICON_CONTAINER_UNSELECTED_PRESSED_SCALE = 0.9995f
private const val NAV_ICON_BOTTOM_CONTAINER_SELECTED_PRESS_SCALE = 0.998f
private const val NAV_ICON_BOTTOM_CONTAINER_SELECTED_SCALE = 1.008f
private const val NAV_ICON_SCALE_SELECTED_PRESS_SCALE = 0.996f
private const val NAV_ICON_SCALE_SELECTED = 1.011f
private const val NAV_ICON_SCALE_UNSELECTED_PRESSED = 0.9988f
private const val NAV_ICON_BOTTOM_ICON_SCALE_SELECTED_PRESS = 0.999f
private const val NAV_ICON_BOTTOM_ICON_SCALE_SELECTED = 1.007f
private const val NAV_ICON_GLYPH_SIZE_BASE = 19.85f
private const val NAV_ICON_GLYPH_SELECTED_PRESSED_SIZE = 20.2f
    private const val NAV_ICON_GLYPH_SELECTED_SIZE = 20.88f
private const val NAV_ICON_GLYPH_PRESSED_SIZE = NAV_ICON_GLYPH_SIZE_BASE
private const val NAV_ICON_GLYPH_UNSELECTED_SIZE = NAV_ICON_GLYPH_SIZE_BASE
    private const val NAV_ICON_FACE_SIZE_BASE = 34.56f
private const val NAV_ICON_FACE_SIZE_PRESSED = 33.3f
private const val NAV_ICON_FACE_SELECTED_PRESSED_SIZE = 34.6f
private const val NAV_ICON_FACE_PRESSED_SIZE = NAV_ICON_FACE_SIZE_PRESSED
    private const val NAV_ICON_FACE_SELECTED_SIZE = 35.2f
private const val NAV_ICON_FACE_UNSELECTED_SIZE = NAV_ICON_FACE_SIZE_BASE
private const val NAV_ICON_CONTAINER_UNSELECTED_ALPHA = 0.34f
private const val NAV_ICON_GLYPH_SELECTED_PRESSED_ALPHA = 0.9885f
private const val NAV_ICON_GLYPH_UNSELECTED_PRESSED_ALPHA = 0.87f
private const val NAV_NAV_ITEM_ACTIVE_BORDER = 0.4f
private const val NAV_NAV_ITEM_ACTIVE_BORDER_SELECTED = NAV_NAV_ITEM_ACTIVE_BORDER
private const val NAV_NAV_ITEM_ACTIVE_SHADOW_DARK_ALPHA = 0.082f
private const val NAV_NAV_ITEM_ACTIVE_SHADOW_LIGHT_ALPHA = 0.044f
private const val NAV_ICON_FACE_GLOW_SELECTED_PRESSED_ALPHA = 0.03f
private const val NAV_ICON_FACE_GLOW_SELECTED_ALPHA = 0.055f
private const val NAV_ICON_HALO_SELECTED_PRESSED_ALPHA = 0.028f
private const val NAV_ICON_HALO_SELECTED_ALPHA = 0.04f
private const val NAV_ICON_HALO_HIGHLIGHT_SELECTED_ALPHA = 0.012f
private const val NAV_ICON_AMBIENT_GLOW_SELECTED_ALPHA = 0.05f
private const val NAV_ICON_AMBIENT_GLOW_SELECTED_PRESSED_ALPHA = 0.024f
private const val NAV_ICON_HALO_BORDER_SELECTED_ALPHA = 0.0307f
private const val NAV_ICON_INNER_RIM_SELECTED_ALPHA = 0.0220f
private const val NAV_ICON_INNER_RIM_SELECTED_PRESSED_ALPHA = 0.0180f
private const val NAV_ICON_INNER_RIM_BORDER_WIDTH = 0.313f
private const val NAV_ICON_AMBIENT_GLOW_SIZE_SELECTED = 33.07f
private const val NAV_ICON_AMBIENT_GLOW_SIZE_SELECTED_PRESSED = 32.47f
private const val NAV_ICON_AMBIENT_GLOW_SELECTED_SCALE = 1.012f
private const val NAV_ICON_AMBIENT_GLOW_SELECTED_PRESSED_SCALE = 1.018f
private const val NAV_ICON_INNER_RIM_INNER_PADDING = 1.008f
private const val NAV_ICON_HALO_BORDER_SELECTED = NAV_NAV_ITEM_ACTIVE_BORDER
private const val NAV_ICON_SHADOW_SELECTED_PRESSED_ALPHA = 0.0058f
private const val NAV_ICON_SHADOW_SELECTED_ALPHA = 0.0063f
private const val NAV_ICON_BACKGROUND_SELECTED_ALPHA = 0.17f
private const val NAV_NAV_ITEM_PILL_SELECTED_PRESSED_ALPHA = 0.235f
private const val NAV_NAV_ITEM_PILL_SELECTED_ALPHA = 0.225f
private const val NAV_NAV_ITEM_PILL_TOP_SELECTED_PRESSED_ALPHA = 0.205f
private const val NAV_NAV_ITEM_PILL_TOP_SELECTED_ALPHA = 0.185f
private const val NAV_NAV_ITEM_PILL_BORDER_SELECTED_PRESSED_ALPHA = 0.16f
private const val NAV_NAV_ITEM_PILL_BORDER_SELECTED_ALPHA = 0.205f
private const val NAV_ICON_FACE_BORDER = 0.34f
private const val NAV_ICON_HALO_HIGHLIGHT_PRESSED_ALPHA = 0.023f
private const val NAV_ICON_HALO_SIZE_UNSELECTED = 20.64f
private const val NAV_ICON_HALO_SIZE_SELECTED = 30.67f
private const val NAV_ICON_HALO_SIZE_SELECTED_PRESSED = 29.965f
private const val NAV_SHELL_SURFACE_DARK_ALPHA = 0.955f
private const val NAV_SHELL_SURFACE_LIGHT_ALPHA = 0.976f
private const val NAV_SHELL_BAR_OUTER_BORDER_WIDTH = 0.16f
private const val NAV_SHELL_RAIL_OUTER_BORDER_WIDTH = 0.2f
private const val NAV_SHELL_BAR_TONAL_ELEVATION = 2.8f
private const val NAV_SHELL_BAR_SHADOW_ELEVATION = 3.6f
private const val NAV_SHELL_BAR_SHADOW_SURFACE_DARK = 3.55f
private const val NAV_SHELL_BAR_SHADOW_SURFACE_LIGHT = 3.1f
private const val NAV_SHELL_RAIL_TONAL_ELEVATION = 3.8f
private const val NAV_SHELL_RAIL_SHADOW_ELEVATION = 5.0f
private const val NAV_SHELL_RAIL_SHADOW_SURFACE_DARK = 4.55f
private const val NAV_SHELL_RAIL_SHADOW_SURFACE_LIGHT = 3.95f
private const val NAV_SHELL_BORDER_DARK_ALPHA = 0.054f
private const val NAV_SHELL_BORDER_LIGHT_ALPHA = 0.045f
private const val NAV_SHELL_GLOW_DARK_ALPHA = 0.028f
private const val NAV_SHELL_GLOW_LIGHT_ALPHA = 0.028f
private const val NAV_SHELL_CONTAINER_DARK_ALPHA = 0.93f
private const val NAV_SHELL_CONTAINER_LIGHT_ALPHA = 0.975f
private const val NAV_SHELL_SURFACE_TOP_GLOW_DARK_ALPHA = 0.02f
private const val NAV_SHELL_SURFACE_TOP_GLOW_LIGHT_ALPHA = 0.018f
private const val NAV_SHELL_BAR_CENTER_GLOW_DARK_ALPHA = 0.014f
private const val NAV_SHELL_BAR_CENTER_GLOW_LIGHT_ALPHA = 0.01f
private const val NAV_SHELL_BAR_CENTER_GLOW_HORIZONTAL_PADDING = 5.6f
private const val NAV_SHELL_BAR_CENTER_GLOW_VERTICAL_PADDING = 3.4f
private const val NAV_SHELL_BAR_SIDE_RIM_WIDTH = 1.35f
private const val NAV_SHELL_BAR_SIDE_RIM_DARK_ALPHA = 0.026f
private const val NAV_SHELL_BAR_SIDE_RIM_LIGHT_ALPHA = 0.02f
private const val NAV_SHELL_SURFACE_EDGE_DARK_ALPHA = 0.02f
private const val NAV_SHELL_SURFACE_EDGE_LIGHT_ALPHA = 0.014f
private const val NAV_SHELL_TOP_RIM_HEIGHT = 1.52f
private const val NAV_SHELL_TOP_RIM_DARK_ALPHA = 0.14f
private const val NAV_SHELL_TOP_RIM_LIGHT_ALPHA = 0.11f
private const val NAV_SHELL_BOTTOM_RIM_HEIGHT = 1.0f
private const val NAV_SHELL_BOTTOM_RIM_DARK_ALPHA = 0.068f
private const val NAV_SHELL_BOTTOM_RIM_LIGHT_ALPHA = 0.052f
private const val NAV_SHELL_RAIL_CENTER_GLOW_DARK_ALPHA = 0.016f
private const val NAV_SHELL_RAIL_CENTER_GLOW_LIGHT_ALPHA = 0.011f
private const val NAV_SHELL_RAIL_CENTER_GLOW_HORIZONTAL_PADDING = 4.9f
private const val NAV_SHELL_RAIL_CENTER_GLOW_VERTICAL_PADDING = 4.1f
	private const val NAV_SHELL_BAR_CORNER = 31.0f
	private const val NAV_SHELL_RAIL_CORNER = 30.0f
