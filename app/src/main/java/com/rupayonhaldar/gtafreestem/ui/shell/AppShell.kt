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
import androidx.compose.ui.graphics.Shadow
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
                    val selectedPillColor by animateColorAsState(
                        targetValue = when {
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_SELECTED_PRESSED_ALPHA)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_SELECTED_ALPHA)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PRESSED_ALPHA)
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
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_TOP_SELECTED_PRESSED_ALPHA)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_TOP_SELECTED_ALPHA)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PRESSED_ALPHA)
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
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_BORDER_SELECTED_PRESSED_ALPHA)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_BORDER_SELECTED_ALPHA)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_BORDER_PRESSED_ALPHA)
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
                            isSelected -> NAV_BOTTOM_NAV_ITEM_SELECTED_ELEVATION.dp
                            isPressed -> NAV_BOTTOM_NAV_ITEM_PRESSED_UNSELECTED_ELEVATION.dp
                            else -> NAV_BOTTOM_NAV_ITEM_UNSELECTED_ELEVATION.dp
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
                            isSelected -> NAV_BOTTOM_NAV_ITEM_SELECTED_WIDTH.dp
                            isPressed -> NAV_BOTTOM_NAV_ITEM_SELECTED_WIDTH.dp
                            else -> NAV_BOTTOM_NAV_ITEM_UNSELECTED_WIDTH.dp
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
                            isPressed && isSelected -> NAV_BOTTOM_NAV_ITEM_PRESSED_SELECTED_CORNER.dp
                            isSelected -> NAV_BOTTOM_NAV_ITEM_SELECTED_CORNER.dp
                            isPressed -> NAV_BOTTOM_NAV_ITEM_PRESSED_CORNER.dp
                            else -> NAV_BOTTOM_NAV_ITEM_UNPRESSED_CORNER.dp
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
                            isPressed && isSelected -> NAV_BOTTOM_NAV_ITEM_SELECTED_OFFSET_PRESSED_DP.dp
                            isPressed && !isSelected -> NAV_BOTTOM_NAV_ITEM_OFFSET_PRESSED_DP.dp
                            isSelected -> NAV_BOTTOM_NAV_ITEM_SELECTED_OFFSET_DP.dp
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
                            isSelected -> NAV_BOTTOM_NAV_ITEM_SELECTED_HEIGHT.dp
                            isPressed -> NAV_BOTTOM_NAV_ITEM_UNSELECTED_HEIGHT.dp
                            else -> NAV_BOTTOM_NAV_ITEM_UNSELECTED_HEIGHT.dp
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
                            isSelected -> NAV_BOTTOM_NAV_ITEM_SELECTED_SCALE
                            isPressed -> NAV_NAV_ITEM_PRESSED_SCALE
                            else -> 1f
                        },
                        animationSpec = if (isPressed) {
                            navPressFloatAnimationSpec
                        } else {
                            navSelectedSettleFloatAnimationSpec
                        },
                        label = "bottom-nav-item-scale-${destination.name}",
                    )
                    val activeItemBorderWidth by animateDpAsState(
                        targetValue = if (isSelected) {
                            NAV_NAV_ITEM_ACTIVE_BORDER_SELECTED.dp
                        } else if (isPressed) {
                            NAV_NAV_ITEM_ACTIVE_BORDER_PRESSED.dp
                        } else {
                            NAV_NAV_ITEM_ACTIVE_BORDER_UNSELECTED.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                            } else if (isSelected) {
                            navSelectedSettleDpAnimationSpec
                            } else {
                            navSelectionDpAnimationSpec
                            },
                        label = "bottom-nav-item-border-width-${destination.name}",
                    )
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                        targetValue = when {
                            isPressed && isSelected -> NAV_ICON_LIFT_SELECTED_PRESSED_DP.dp
                            isPressed -> NAV_ICON_LIFT_BOTTOM_PRESSED_DP.dp
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
                            isPressed && isSelected -> NAV_ICON_SELECTED_PRESSED_ELEVATION
                            isPressed -> NAV_ICON_ELEVATION_PRESSED
                            isSelected -> NAV_ICON_SELECTED_ELEVATION
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
                                isSelected && isPressed -> NAV_ICON_BOTTOM_CONTAINER_SELECTED_PRESS_SCALE
                                isPressed -> NAV_ICON_BOTTOM_CONTAINER_PRESSED_SCALE
                                isSelected -> NAV_ICON_BOTTOM_CONTAINER_SELECTED_SCALE
                                else -> 1f
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navUnselectedSettleFloatAnimationSpec
                                },
                            label = "bottom-nav-icon-container-scale-${destination.name}",
                        )
                        val iconScale by animateFloatAsState(
                            targetValue = when {
                                isSelected && isPressed -> NAV_ICON_BOTTOM_ICON_SCALE_SELECTED_PRESS
                                isPressed -> NAV_ICON_BOTTOM_ICON_SCALE_PRESSED
                                isSelected -> NAV_ICON_BOTTOM_ICON_SCALE_SELECTED
                                else -> 1f
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navUnselectedSettleFloatAnimationSpec
                                },
                            label = "bottom-nav-icon-scale-${destination.name}",
                        )
                        val iconColorAnimationSpec = if (isSelected) {
                            if (isPressed) navPressColorAnimationSpec else navSelectedSettleColorAnimationSpec
                        } else {
                            if (isPressed) navPressColorAnimationSpec else navUnselectedSettleColorAnimationSpec
                        }
                        val iconGlyphAlpha by animateFloatAsState(
                            targetValue = when {
                            isSelected && isPressed -> NAV_ICON_GLYPH_SELECTED_PRESSED_ALPHA
                            isSelected -> NAV_ICON_GLYPH_SELECTED_ALPHA
                            isPressed -> NAV_ICON_GLYPH_PRESSED_ALPHA
                            else -> NAV_ICON_GLYPH_UNSELECTED_ALPHA
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navUnselectedSettleFloatAnimationSpec
                                },
                            label = "bottom-nav-icon-glyph-alpha-${destination.name}",
                        )
                        val iconContainerAlpha by animateFloatAsState(
                            targetValue = when {
                            isSelected && isPressed -> NAV_ICON_CONTAINER_PRESSED_SELECTED_ALPHA
                            isSelected -> NAV_ICON_CONTAINER_SELECTED_ALPHA
                            isPressed -> NAV_ICON_CONTAINER_PRESSED_ALPHA
                            else -> NAV_ICON_CONTAINER_UNSELECTED_ALPHA
                            },
                            animationSpec = if (isPressed) {
                                navPressFloatAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleFloatAnimationSpec
                                } else {
                                navUnselectedSettleFloatAnimationSpec
                                },
                            label = "bottom-nav-icon-container-alpha-${destination.name}",
                        )
                        val iconGlyphSize by animateDpAsState(
                            targetValue = when {
                            isSelected && isPressed -> NAV_ICON_GLYPH_SELECTED_PRESSED_SIZE.dp
                            isPressed -> NAV_ICON_GLYPH_PRESSED_SIZE.dp
                            isSelected -> NAV_ICON_GLYPH_SELECTED_SIZE.dp
                            else -> NAV_ICON_GLYPH_UNSELECTED_SIZE.dp
                            },
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navUnselectedSettleDpAnimationSpec
                                },
                            label = "bottom-nav-icon-glyph-size-${destination.name}",
                        )
                        val iconFaceSize by animateDpAsState(
                            targetValue = when {
                            isSelected && isPressed -> NAV_ICON_FACE_SELECTED_PRESSED_SIZE.dp
                            isPressed -> NAV_ICON_FACE_PRESSED_SIZE.dp
                            isSelected -> NAV_ICON_FACE_SELECTED_SIZE.dp
                            else -> NAV_ICON_FACE_UNSELECTED_SIZE.dp
                            },
                            animationSpec = if (isPressed) {
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
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_BACKGROUND_PRESSED_ALPHA)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = iconColorAnimationSpec,
                            label = "bottom-nav-icon-background-${destination.name}",
                        )
                        val iconFaceGlow by animateColorAsState(
                            targetValue = when {
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_FACE_GLOW_SELECTED_PRESSED_ALPHA)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_FACE_GLOW_SELECTED_ALPHA)
                                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_FACE_GLOW_PRESSED_ALPHA)
                                else -> Color.Transparent
                            },
                            animationSpec = iconColorAnimationSpec,
                            label = "bottom-nav-icon-face-glow-${destination.name}",
                        )
                        val iconTint by animateColorAsState(
                            targetValue = when {
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_TINT_SELECTED_PRESSED_ALPHA)
                                isSelected -> MaterialTheme.colorScheme.primary
                                isPressed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NAV_ICON_TINT_PRESSED_ALPHA)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = iconColorAnimationSpec,
                            label = "bottom-nav-icon-tint-${destination.name}",
                        )
                        val iconHalo by animateColorAsState(
                            targetValue = if (isSelected && isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_SELECTED_PRESSED_ALPHA)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_SELECTED_ALPHA)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_PRESSED_ALPHA)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = iconColorAnimationSpec,
                            label = "bottom-nav-icon-halo-${destination.name}",
                        )
                        val iconHaloHighlight by animateColorAsState(
                            targetValue = if (isSelected && isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_HIGHLIGHT_PRESSED_ALPHA)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_HIGHLIGHT_SELECTED_ALPHA)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_HIGHLIGHT_PRESSED_ONLY_ALPHA)
                            } else {
                                Color.Transparent
                            },
                            animationSpec = iconColorAnimationSpec,
                            label = "bottom-nav-icon-halo-highlight-${destination.name}",
                        )
                        val iconHaloSize by animateDpAsState(
                            targetValue = if (isSelected && isPressed) {
                                NAV_ICON_HALO_SIZE_SELECTED_PRESSED.dp
                            } else if (isSelected) {
                                NAV_ICON_HALO_SIZE_SELECTED.dp
                            } else if (isPressed) {
                                NAV_ICON_HALO_SIZE_PRESSED.dp
                            } else {
                                NAV_ICON_HALO_SIZE_UNSELECTED.dp
                            },
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navUnselectedSettleDpAnimationSpec
                                },
                            label = "bottom-nav-icon-halo-size-${destination.name}",
                        )
                        val iconHaloBorderWidth by animateDpAsState(
                            targetValue = if (isSelected) NAV_ICON_HALO_BORDER_SELECTED.dp else if (isPressed) NAV_ICON_HALO_BORDER_PRESSED.dp else NAV_ICON_HALO_BORDER_UNSELECTED.dp,
                            animationSpec = if (isPressed) {
                                navPressDpAnimationSpec
                                } else if (isSelected) {
                                navSelectedSettleDpAnimationSpec
                                } else {
                                navUnselectedSettleDpAnimationSpec
                                },
                            label = "bottom-nav-icon-halo-border-width-${destination.name}",
                        )
                        val iconShadowColor by animateColorAsState(
                            targetValue = when {
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_SHADOW_SELECTED_PRESSED_ALPHA)
                                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_ICON_SHADOW_PRESSED_ALPHA)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_SHADOW_SELECTED_ALPHA)
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
                                            MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_BORDER_SELECTED_ALPHA)
                                        } else if (isPressed) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_BORDER_PRESSED_ALPHA)
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
                    val selectedPillColor by animateColorAsState(
                        targetValue = when {
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_SELECTED_PRESSED_ALPHA)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_SELECTED_ALPHA)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PRESSED_ALPHA)
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
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_TOP_SELECTED_PRESSED_ALPHA)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_TOP_SELECTED_ALPHA)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PRESSED_ALPHA)
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
                            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_BORDER_SELECTED_PRESSED_ALPHA)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_BORDER_SELECTED_ALPHA)
                            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_PILL_BORDER_PRESSED_ALPHA)
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
                            isSelected -> NAV_RAIL_ITEM_SELECTED_ELEVATION.dp
                            isPressed -> NAV_RAIL_ITEM_PRESSED_UNSELECTED_ELEVATION.dp
                            else -> NAV_RAIL_ITEM_UNSELECTED_ELEVATION.dp
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
                            isSelected -> NAV_RAIL_ITEM_SELECTED_WIDTH.dp
                            isPressed -> NAV_RAIL_ITEM_SELECTED_WIDTH.dp
                            else -> NAV_RAIL_ITEM_UNSELECTED_WIDTH.dp
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
                            isPressed && isSelected -> NAV_RAIL_ITEM_PRESSED_SELECTED_CORNER.dp
                            isSelected -> NAV_RAIL_ITEM_SELECTED_CORNER.dp
                            isPressed -> NAV_RAIL_ITEM_PRESSED_CORNER.dp
                            else -> NAV_RAIL_ITEM_UNPRESSED_CORNER.dp
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
                                isPressed && isSelected -> NAV_RAIL_ITEM_SELECTED_OFFSET_PRESSED_DP.dp
                                isSelected -> NAV_RAIL_ITEM_SELECTED_OFFSET_DP.dp
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
                            isSelected -> NAV_RAIL_ITEM_SELECTED_HEIGHT.dp
                            isPressed -> NAV_RAIL_ITEM_UNSELECTED_HEIGHT.dp
                            else -> NAV_RAIL_ITEM_UNSELECTED_HEIGHT.dp
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
                            isSelected -> NAV_RAIL_ITEM_SELECTED_SCALE
                            isPressed -> NAV_NAV_ITEM_PRESSED_SCALE
                            else -> 1f
                        },
                        animationSpec = if (isPressed) {
                            navPressFloatAnimationSpec
                        } else {
                            navSelectedSettleFloatAnimationSpec
                        },
                        label = "rail-item-scale-${destination.name}",
                    )
                    val activeItemBorderWidth by animateDpAsState(
                        targetValue = if (isSelected) {
                            NAV_NAV_ITEM_ACTIVE_BORDER_SELECTED.dp
                        } else if (isPressed) {
                            NAV_NAV_ITEM_ACTIVE_BORDER_PRESSED.dp
                        } else {
                            NAV_NAV_ITEM_ACTIVE_BORDER_UNSELECTED.dp
                        },
                        animationSpec = if (isPressed) {
                            navPressDpAnimationSpec
                            } else if (isSelected) {
                            navSelectedSettleDpAnimationSpec
                            } else {
                            navSelectionDpAnimationSpec
                            },
                        label = "rail-item-border-width-${destination.name}",
                    )
                    NavigationRailItem(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                        targetValue = when {
                            isPressed && isSelected -> NAV_ICON_RAIL_LIFT_SELECTED_PRESSED_DP.dp
                            isPressed -> NAV_ICON_RAIL_LIFT_PRESSED_DP.dp
                            isSelected -> NAV_ICON_RAIL_LIFT_SELECTED_DP.dp
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
                            isPressed && isSelected -> NAV_ICON_SELECTED_PRESSED_ELEVATION
                            isPressed -> NAV_ICON_ELEVATION_PRESSED
                            isSelected -> NAV_ICON_SELECTED_ELEVATION
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
                            isSelected && isPressed -> NAV_ICON_RAIL_CONTAINER_SELECTED_PRESS_SCALE
                            isPressed -> NAV_ICON_RAIL_CONTAINER_PRESSED_SCALE
                            isSelected -> NAV_ICON_CONTAINER_SELECTED_SCALE
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
                                isSelected && isPressed -> NAV_ICON_SCALE_SELECTED_PRESS_SCALE
                                isPressed -> NAV_ICON_SCALE_PRESSED
                                isSelected -> NAV_ICON_SCALE_SELECTED
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
                            isSelected && isPressed -> NAV_ICON_GLYPH_SELECTED_PRESSED_ALPHA
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
                                isSelected && isPressed -> NAV_ICON_CONTAINER_PRESSED_SELECTED_ALPHA
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
                            isSelected && isPressed -> NAV_ICON_GLYPH_SELECTED_PRESSED_SIZE.dp
                            isPressed -> NAV_ICON_GLYPH_PRESSED_SIZE.dp
                            isSelected -> NAV_ICON_GLYPH_SELECTED_SIZE.dp
                            else -> NAV_ICON_GLYPH_UNSELECTED_SIZE.dp
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
                            isSelected && isPressed -> NAV_ICON_FACE_SELECTED_PRESSED_SIZE.dp
                            isPressed -> NAV_ICON_FACE_PRESSED_SIZE.dp
                            isSelected -> NAV_ICON_FACE_SELECTED_SIZE.dp
                            else -> NAV_ICON_FACE_UNSELECTED_SIZE.dp
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
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = NAV_ICON_BACKGROUND_SELECTED_ALPHA)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_BACKGROUND_PRESSED_ALPHA)
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
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_FACE_GLOW_SELECTED_PRESSED_ALPHA)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_FACE_GLOW_SELECTED_ALPHA)
                                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_FACE_GLOW_PRESSED_ALPHA)
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
                            targetValue = when {
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_TINT_SELECTED_PRESSED_ALPHA)
                                isSelected -> MaterialTheme.colorScheme.primary
                                isPressed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NAV_ICON_TINT_PRESSED_ALPHA)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
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
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_SELECTED_PRESSED_ALPHA)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_SELECTED_ALPHA)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_PRESSED_ALPHA)
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
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_HIGHLIGHT_PRESSED_ALPHA)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_HIGHLIGHT_SELECTED_ALPHA)
                            } else if (isPressed) {
                                MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_HIGHLIGHT_PRESSED_ONLY_ALPHA)
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
                            targetValue = if (isSelected && isPressed) {
                                NAV_ICON_HALO_SIZE_SELECTED_PRESSED.dp
                            } else if (isSelected) {
                                NAV_ICON_HALO_SIZE_SELECTED.dp
                            } else if (isPressed) {
                                NAV_ICON_HALO_SIZE_PRESSED.dp
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
                            targetValue = if (isSelected) NAV_ICON_HALO_BORDER_SELECTED.dp else if (isPressed) NAV_ICON_HALO_BORDER_PRESSED.dp else NAV_ICON_HALO_BORDER_UNSELECTED.dp,
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
                                isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_SHADOW_SELECTED_PRESSED_ALPHA)
                                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_NAV_ITEM_ICON_SHADOW_PRESSED_ALPHA)
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_SHADOW_SELECTED_ALPHA)
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
                                            MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_BORDER_SELECTED_ALPHA)
                                        } else if (isPressed) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = NAV_ICON_HALO_BORDER_PRESSED_ALPHA)
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
    isSelected: Boolean = false,
    isPressed: Boolean = false,
) {
    val selectedPressOffsetDp = if (compact) {
        NAV_LABEL_COMPACT_OFFSET_SELECTED_PRESSED_DP
    } else {
        NAV_LABEL_OFFSET_SELECTED_PRESSED_DP
    }
    val selectedOffsetDp = if (compact) {
        NAV_LABEL_COMPACT_OFFSET_SELECTED_DP
    } else {
        NAV_LABEL_OFFSET_SELECTED_DP
    }
    val unselectedPressOffsetDp = if (compact) {
        NAV_LABEL_COMPACT_OFFSET_UNSELECTED_PRESSED_DP
    } else {
        NAV_LABEL_OFFSET_UNSELECTED_PRESSED_DP
    }
    val selectedScale = if (compact) {
        NAV_LABEL_COMPACT_SELECTED_SCALE
    } else {
        NAV_LABEL_SELECTED_SCALE
    }
    val selectedPressedScale = if (compact) {
        NAV_LABEL_COMPACT_SELECTED_PRESS_SCALE
    } else {
        NAV_LABEL_SELECTED_PRESS_SCALE
    }
    val pressedScale = if (compact) {
        NAV_LABEL_COMPACT_PRESS_SCALE
    } else {
        NAV_LABEL_PRESS_SCALE
    }
    val resolvedStyle = if (compact) {
        MaterialTheme.typography.labelSmall.copy(
            fontSize = if (isSelected) NAV_LABEL_COMPACT_FONT_SELECTED.sp else NAV_LABEL_COMPACT_FONT_UNSELECTED.sp,
            lineHeight = if (isSelected) NAV_LABEL_COMPACT_LINE_HEIGHT_SELECTED.sp else NAV_LABEL_COMPACT_LINE_HEIGHT_UNSELECTED.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = NAV_LABEL_LETTER_SPACING.sp,
        )
    } else {
            MaterialTheme.typography.labelSmall.copy(
            fontSize = if (isSelected) NAV_LABEL_RAIL_FONT_SELECTED.sp else NAV_LABEL_RAIL_FONT_UNSELECTED.sp,
            lineHeight = if (isSelected) NAV_LABEL_RAIL_LINE_HEIGHT_SELECTED.sp else NAV_LABEL_RAIL_LINE_HEIGHT_UNSELECTED.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = NAV_LABEL_LETTER_SPACING.sp,
        )
    }
    val labelColor by animateColorAsState(
        targetValue = when {
            isSelected && isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_LABEL_SELECTED_PRESS_ALPHA)
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = NAV_LABEL_SELECTED_ALPHA)
            isPressed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NAV_LABEL_PRESSED_ALPHA)
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NAV_LABEL_UNSELECTED_ALPHA)
        },
        animationSpec = if (isPressed) {
            navPressColorAnimationSpec
        } else if (isSelected) {
            navSelectedSettleColorAnimationSpec
        } else {
            navUnselectedSettleColorAnimationSpec
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
            navUnselectedSettleFloatAnimationSpec
        },
        label = "destination-label-alpha",
    )
    val labelOffset by animateDpAsState(
        targetValue = when {
            isSelected && isPressed -> selectedPressOffsetDp.dp
            isPressed && !isSelected -> unselectedPressOffsetDp.dp
            isSelected -> selectedOffsetDp.dp
            else -> 0.dp
        },
        animationSpec = if (isPressed) {
            navPressDpAnimationSpec
        } else if (isSelected) {
            navSelectedSettleDpAnimationSpec
        } else {
            navUnselectedSettleDpAnimationSpec
        },
        label = "destination-label-offset",
    )
    val labelScale by animateFloatAsState(
        targetValue = when {
            isPressed && isSelected -> selectedPressedScale
            isPressed && !isSelected -> pressedScale
            isSelected -> selectedScale
            else -> NAV_LABEL_UNSELECTED_SCALE
        },
        animationSpec = if (isPressed) {
            navPressFloatAnimationSpec
        } else if (isSelected) {
            navSelectedSettleFloatAnimationSpec
        } else {
            navUnselectedSettleFloatAnimationSpec
        },
        label = "destination-label-scale",
    )
    val labelShadowProgress by animateFloatAsState(
        targetValue = when {
            isSelected && !isPressed -> 1f
            isSelected && isPressed -> 0.95f
            else -> 0f
        },
        animationSpec = if (isPressed) navPressFloatAnimationSpec else navSelectedSettleFloatAnimationSpec,
        label = "destination-label-shadow",
    )
    val labelShadow = Shadow(
        color = MaterialTheme.colorScheme.primary.copy(alpha = NAV_LABEL_SELECTED_SHADOW_ALPHA * labelShadowProgress),
        offset = Offset(0f, NAV_LABEL_SELECTED_SHADOW_Y_OFFSET * labelShadowProgress),
        blurRadius = NAV_LABEL_SELECTED_SHADOW_BLUR * labelShadowProgress,
    )
    val selectedLabelStyle = resolvedStyle.copy(shadow = labelShadow)
    val textStyle = if (labelShadowProgress > 0.001f) {
        selectedLabelStyle
    } else {
        resolvedStyle
    }
    Text(
        text = label,
        modifier = Modifier
            .offset(y = labelOffset)
                    .scale(labelScale),
        color = labelColor.copy(alpha = labelAlpha),
        style = textStyle,
        maxLines = 1,
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Ellipsis,
    )
}

private val PrimaryDestination.testTag: String
    get() = "primary-navigation-${name.lowercase()}"

private val navSelectionFloatAnimationSpec = tween<Float>(236, easing = FastOutSlowInEasing)
private val navPressFloatAnimationSpec = tween<Float>(110, easing = FastOutSlowInEasing)
private val navSelectionDpAnimationSpec = tween<Dp>(240, easing = FastOutSlowInEasing)
private val navPressDpAnimationSpec = tween<Dp>(116, easing = FastOutSlowInEasing)
private val navPressColorAnimationSpec = tween<Color>(138, easing = FastOutSlowInEasing)
private val navSelectionColorAnimationSpec = tween<Color>(244, easing = FastOutSlowInEasing)
private val navSelectedSettleColorAnimationSpec = tween<Color>(292, easing = FastOutSlowInEasing)
private val navSelectedSettleFloatAnimationSpec = tween<Float>(304, easing = FastOutSlowInEasing)
private val navSelectedSettleDpAnimationSpec = tween<Dp>(298, easing = FastOutSlowInEasing)
private val navUnselectedSettleColorAnimationSpec = tween<Color>(276, easing = FastOutSlowInEasing)
private val navUnselectedSettleFloatAnimationSpec = tween<Float>(284, easing = FastOutSlowInEasing)
private val navUnselectedSettleDpAnimationSpec = tween<Dp>(276, easing = FastOutSlowInEasing)

private const val NAV_UNSELECTED_LABEL_ALPHA = 0.80f
    private const val NAV_LABEL_UNSELECTED_ALPHA = 0.92f
private const val NAV_LABEL_SELECTED_ALPHA = 1f
private const val NAV_LABEL_UNSELECTED_VISIBILITY_ALPHA = 0.93f
private const val NAV_LABEL_UNSELECTED_SCALE = 1f
private const val NAV_LABEL_SELECTED_SCALE = 1.0106f
private const val NAV_LABEL_PRESS_SCALE = 0.9984f
private const val NAV_LABEL_COMPACT_PRESS_SCALE = 0.9991f
private const val NAV_LABEL_COMPACT_FONT_SELECTED = 12.26f
private const val NAV_LABEL_COMPACT_FONT_UNSELECTED = 11.75f
private const val NAV_LABEL_COMPACT_LINE_HEIGHT_SELECTED = 14.00f
private const val NAV_LABEL_COMPACT_LINE_HEIGHT_UNSELECTED = 13.28f
private const val NAV_LABEL_RAIL_FONT_SELECTED = 12.46f
private const val NAV_LABEL_RAIL_FONT_UNSELECTED = 11.74f
private const val NAV_LABEL_RAIL_LINE_HEIGHT_SELECTED = 13.64f
private const val NAV_LABEL_RAIL_LINE_HEIGHT_UNSELECTED = 12.84f
private const val NAV_LABEL_LETTER_SPACING = 0.0060f
private const val NAV_LABEL_PRESS_ALPHA = 0.9736f
private const val NAV_LABEL_PRESSED_ALPHA = 0.9728f
private const val NAV_LABEL_SELECTED_PRESS_ALPHA = 0.9866f
private const val NAV_LABEL_SELECTED_PRESS_ALPHA_VISIBILITY = 0.9908f
private const val NAV_LABEL_SELECTED_SHADOW_ALPHA = 0.0330f
private const val NAV_LABEL_SELECTED_SHADOW_BLUR = 0.548f
private const val NAV_LABEL_SELECTED_SHADOW_Y_OFFSET = 0.038f
private const val NAV_NAV_ITEM_PRESSED_SCALE = 0.9992f
private const val NAV_LABEL_OFFSET_PRESSED_DP = -0.040f
private const val NAV_LABEL_OFFSET_UNSELECTED_PRESSED_DP = -0.032f
private const val NAV_LABEL_OFFSET_SELECTED_DP = -0.086f
private const val NAV_LABEL_OFFSET_SELECTED_PRESSED_DP = -0.072f
private const val NAV_LABEL_COMPACT_OFFSET_UNSELECTED_PRESSED_DP = -0.010f
private const val NAV_LABEL_COMPACT_OFFSET_SELECTED_DP = -0.036f
private const val NAV_LABEL_COMPACT_OFFSET_SELECTED_PRESSED_DP = -0.068f
private const val NAV_LABEL_SELECTED_PRESS_SCALE = 1.0076f
private const val NAV_LABEL_COMPACT_SELECTED_SCALE = 1.0060f
private const val NAV_LABEL_COMPACT_SELECTED_PRESS_SCALE = 1.0048f
private const val NAV_BOTTOM_NAV_BAR_OUTER_HORIZONTAL_PADDING = 12.35f
private const val NAV_BOTTOM_NAV_BAR_OUTER_VERTICAL_PADDING = 4.25f
private const val NAV_BOTTOM_NAV_BAR_INNER_PADDING = 8.2f
private const val NAV_BOTTOM_NAV_ITEM_SELECTED_WIDTH = 82.90f
private const val NAV_BOTTOM_NAV_ITEM_UNSELECTED_WIDTH = 79.60f
private const val NAV_BOTTOM_NAV_ITEM_PADDING_HORIZONTAL = 5.3f
private const val NAV_BOTTOM_NAV_ITEM_PADDING_VERTICAL = 2.65f
private const val NAV_RAIL_WIDTH = 89.95f
private const val NAV_RAIL_OUTER_PADDING = 8.00f
    private const val NAV_RAIL_INNER_PADDING_HORIZONTAL = 7.25f
private const val NAV_RAIL_INNER_PADDING_VERTICAL = 8.0f
private const val NAV_RAIL_ITEM_SELECTED_WIDTH = 89.20f
private const val NAV_RAIL_ITEM_UNSELECTED_WIDTH = 85.18f
private const val NAV_RAIL_ITEM_PADDING_HORIZONTAL = 2.52f
    private const val NAV_RAIL_ITEM_PADDING_VERTICAL = 3.10f
private const val NAV_BOTTOM_NAV_ITEM_OFFSET_PRESSED_DP = -0.108f
private const val NAV_RAIL_ITEM_OFFSET_PRESSED_DP = -0.082f
private const val NAV_BOTTOM_NAV_ITEM_SELECTED_OFFSET_PRESSED_DP = -0.19f
private const val NAV_RAIL_ITEM_SELECTED_OFFSET_PRESSED_DP = -0.20f
private const val NAV_BOTTOM_NAV_ITEM_SELECTED_OFFSET_DP = -0.21f
private const val NAV_RAIL_ITEM_SELECTED_OFFSET_DP = -0.20f
private const val NAV_BOTTOM_NAV_ITEM_SELECTED_ELEVATION = 0.545f
private const val NAV_BOTTOM_NAV_ITEM_UNSELECTED_ELEVATION = 0.046f
private const val NAV_BOTTOM_NAV_ITEM_PRESSED_UNSELECTED_ELEVATION = 0.020f
    private const val NAV_RAIL_ITEM_SELECTED_ELEVATION = 0.628f
    private const val NAV_RAIL_ITEM_UNSELECTED_ELEVATION = 0.074f
    private const val NAV_RAIL_ITEM_PRESSED_UNSELECTED_ELEVATION = 0.032f
private const val NAV_BOTTOM_NAV_ITEM_PRESSED_SELECTED_ELEVATION = 0.37f
private const val NAV_BOTTOM_NAV_ITEM_PRESSED_SELECTED_HEIGHT = 58.5f
private const val NAV_BOTTOM_NAV_ITEM_SELECTED_HEIGHT = 60.55f
private const val NAV_BOTTOM_NAV_ITEM_UNSELECTED_HEIGHT = 55.95f
private const val NAV_BOTTOM_NAV_ITEM_SELECTED_SCALE = 1.0034f
private const val NAV_BOTTOM_NAV_ITEM_SELECTED_CORNER = 20.6f
private const val NAV_BOTTOM_NAV_ITEM_PRESSED_SELECTED_CORNER = 17.3f
private const val NAV_BOTTOM_NAV_ITEM_PRESSED_CORNER = 18.3f
private const val NAV_BOTTOM_NAV_ITEM_UNPRESSED_CORNER = 16.2f
private const val NAV_BOTTOM_NAV_ITEM_SELECTED_PRESS_SCALE = 0.9989f
private const val NAV_RAIL_ITEM_PRESSED_SELECTED_ELEVATION = 0.334f
private const val NAV_RAIL_ITEM_PRESSED_SELECTED_HEIGHT = 51.22f
private const val NAV_RAIL_ITEM_SELECTED_HEIGHT = 52.75f
private const val NAV_RAIL_ITEM_UNSELECTED_HEIGHT = 48.75f
private const val NAV_RAIL_ITEM_SELECTED_SCALE = 1.0042f
private const val NAV_RAIL_ITEM_SELECTED_CORNER = 19.4f
private const val NAV_RAIL_ITEM_PRESSED_SELECTED_CORNER = 18.28f
private const val NAV_RAIL_ITEM_PRESSED_CORNER = 16.78f
private const val NAV_RAIL_ITEM_UNPRESSED_CORNER = 14.56f
    private const val NAV_RAIL_ITEM_SELECTED_PRESS_SCALE = 0.9970f
private const val NAV_ICON_LIFT_SELECTED_DP = -0.063f
private const val NAV_ICON_LIFT_PRESSED_DP = -0.069f
private const val NAV_ICON_LIFT_SELECTED_PRESSED_DP = -0.083f
private const val NAV_ICON_LIFT_BOTTOM_PRESSED_DP = -0.064f
private const val NAV_ICON_RAIL_LIFT_SELECTED_DP = -0.060f
private const val NAV_ICON_RAIL_LIFT_PRESSED_DP = -0.059f
private const val NAV_ICON_RAIL_LIFT_SELECTED_PRESSED_DP = -0.080f
private const val NAV_ICON_GLYPH_SELECTED_ALPHA = 1f
    private const val NAV_ICON_GLYPH_PRESSED_ALPHA = 0.9822f
    private const val NAV_ICON_GLYPH_UNSELECTED_ALPHA = 0.90f
private const val NAV_ICON_CONTAINER_SELECTED_ALPHA = 0.999f
private const val NAV_ICON_CONTAINER_PRESSED_SELECTED_ALPHA = 0.968f
private const val NAV_ICON_CONTAINER_PRESSED_ALPHA = 0.978f
private const val NAV_ICON_SELECTED_PRESSED_ELEVATION = 1.36f
private const val NAV_ICON_SELECTED_ELEVATION = 1.62f
private const val NAV_ICON_ELEVATION_PRESSED = 0.66f
private const val NAV_ICON_SCALE_PRESSED = 0.9985f
private const val NAV_ICON_TINT_SELECTED_PRESSED_ALPHA = 0.971f
private const val NAV_ICON_CONTAINER_SELECTED_PRESS_SCALE = 0.9983f
private const val NAV_ICON_RAIL_CONTAINER_SELECTED_PRESS_SCALE = 0.9976f
private const val NAV_ICON_RAIL_CONTAINER_PRESSED_SCALE = 0.9989f
private const val NAV_ICON_CONTAINER_SELECTED_SCALE = 1.0065f
private const val NAV_ICON_BOTTOM_CONTAINER_SELECTED_PRESS_SCALE = 0.9978f
private const val NAV_ICON_BOTTOM_CONTAINER_PRESSED_SCALE = 0.9990f
private const val NAV_ICON_BOTTOM_CONTAINER_SELECTED_SCALE = 1.0052f
private const val NAV_ICON_SCALE_SELECTED_PRESS_SCALE = 0.9990f
private const val NAV_ICON_SCALE_SELECTED = 1.0048f
private const val NAV_ICON_BOTTOM_ICON_SCALE_SELECTED_PRESS = 0.9992f
private const val NAV_ICON_BOTTOM_ICON_SCALE_PRESSED = 0.9996f
private const val NAV_ICON_BOTTOM_ICON_SCALE_SELECTED = 1.0055f
private const val NAV_ICON_GLYPH_SIZE_BASE = 19.85f
private const val NAV_ICON_GLYPH_SELECTED_PRESSED_SIZE = 20.55f
    private const val NAV_ICON_GLYPH_SELECTED_SIZE = 20.88f
private const val NAV_ICON_GLYPH_PRESSED_SIZE = NAV_ICON_GLYPH_SIZE_BASE
private const val NAV_ICON_GLYPH_UNSELECTED_SIZE = NAV_ICON_GLYPH_SIZE_BASE
    private const val NAV_ICON_FACE_SIZE_BASE = 34.56f
private const val NAV_ICON_FACE_SIZE_PRESSED = 33.3f
private const val NAV_ICON_FACE_SELECTED_PRESSED_SIZE = 34.6f
private const val NAV_ICON_FACE_PRESSED_SIZE = NAV_ICON_FACE_SIZE_PRESSED
    private const val NAV_ICON_FACE_SELECTED_SIZE = 35.8f
private const val NAV_ICON_FACE_UNSELECTED_SIZE = NAV_ICON_FACE_SIZE_BASE
    private const val NAV_ICON_CONTAINER_UNSELECTED_ALPHA = 0.61f
    private const val NAV_ICON_TINT_PRESSED_ALPHA = 0.993f
private const val NAV_ICON_GLYPH_SELECTED_PRESSED_ALPHA = 0.994f
private const val NAV_NAV_ITEM_ACTIVE_BORDER = 0.4f
private const val NAV_NAV_ITEM_ACTIVE_BORDER_SELECTED = NAV_NAV_ITEM_ACTIVE_BORDER
private const val NAV_NAV_ITEM_ACTIVE_BORDER_PRESSED = NAV_NAV_ITEM_ACTIVE_BORDER
private const val NAV_NAV_ITEM_ACTIVE_BORDER_UNSELECTED = 0f
private const val NAV_NAV_ITEM_ACTIVE_SHADOW_DARK_ALPHA = 0.132f
private const val NAV_NAV_ITEM_ACTIVE_SHADOW_LIGHT_ALPHA = 0.070f
private const val NAV_ICON_FACE_GLOW_SELECTED_PRESSED_ALPHA = 0.054f
private const val NAV_ICON_FACE_GLOW_SELECTED_ALPHA = 0.073f
private const val NAV_ICON_FACE_GLOW_PRESSED_ALPHA = 0.0128f
private const val NAV_ICON_HALO_SELECTED_PRESSED_ALPHA = 0.030f
private const val NAV_ICON_HALO_SELECTED_ALPHA = 0.075f
    private const val NAV_ICON_HALO_PRESSED_ALPHA = 0.0079f
private const val NAV_ICON_HALO_HIGHLIGHT_SELECTED_ALPHA = 0.0224f
private const val NAV_ICON_HALO_HIGHLIGHT_PRESSED_ONLY_ALPHA = 0.0088f
private const val NAV_ICON_HALO_BORDER_SELECTED_ALPHA = 0.056f
private const val NAV_ICON_HALO_BORDER_PRESSED_ALPHA = 0.021f
private const val NAV_ICON_HALO_BORDER_SELECTED = NAV_NAV_ITEM_ACTIVE_BORDER
private const val NAV_ICON_HALO_BORDER_PRESSED = 0.13f
private const val NAV_ICON_HALO_BORDER_UNSELECTED = 0f
private const val NAV_ICON_SHADOW_SELECTED_PRESSED_ALPHA = 0.018f
private const val NAV_ICON_SHADOW_SELECTED_ALPHA = 0.0188f
private const val NAV_ICON_BACKGROUND_SELECTED_ALPHA = 0.204f
private const val NAV_ICON_BACKGROUND_PRESSED_ALPHA = 0.053f
private const val NAV_NAV_ITEM_PILL_SELECTED_PRESSED_ALPHA = 0.187f
private const val NAV_NAV_ITEM_PILL_SELECTED_ALPHA = 0.175f
private const val NAV_NAV_ITEM_PILL_TOP_SELECTED_PRESSED_ALPHA = 0.161f
private const val NAV_NAV_ITEM_PILL_TOP_SELECTED_ALPHA = 0.149f
private const val NAV_NAV_ITEM_PILL_BORDER_SELECTED_PRESSED_ALPHA = 0.124f
private const val NAV_NAV_ITEM_PILL_BORDER_SELECTED_ALPHA = 0.155f
private const val NAV_NAV_ITEM_PILL_BORDER_PRESSED_ALPHA = 0.038f
private const val NAV_ICON_FACE_BORDER = 0.34f
    private const val NAV_NAV_ITEM_PRESSED_ALPHA = 0.033f
private const val NAV_ICON_HALO_HIGHLIGHT_PRESSED_ALPHA = 0.0218f
private const val NAV_NAV_ITEM_ICON_SHADOW_PRESSED_ALPHA = 0.0200f
private const val NAV_ICON_HALO_SIZE_UNSELECTED = 20.9f
private const val NAV_ICON_HALO_SIZE_PRESSED = 28.4f
private const val NAV_ICON_HALO_SIZE_SELECTED = 33.0f
private const val NAV_ICON_HALO_SIZE_SELECTED_PRESSED = 31.6f
private const val NAV_SHELL_SURFACE_DARK_ALPHA = 0.942f
private const val NAV_SHELL_SURFACE_LIGHT_ALPHA = 0.970f
private const val NAV_SHELL_BAR_OUTER_BORDER_WIDTH = 0.173f
private const val NAV_SHELL_RAIL_OUTER_BORDER_WIDTH = 0.235f
private const val NAV_SHELL_BAR_TONAL_ELEVATION = 4.10f
private const val NAV_SHELL_BAR_SHADOW_ELEVATION = 4.98f
private const val NAV_SHELL_BAR_SHADOW_SURFACE_DARK = 5.22f
private const val NAV_SHELL_BAR_SHADOW_SURFACE_LIGHT = 4.63f
private const val NAV_SHELL_RAIL_TONAL_ELEVATION = 5.06f
private const val NAV_SHELL_RAIL_SHADOW_ELEVATION = 5.96f
private const val NAV_SHELL_RAIL_SHADOW_SURFACE_DARK = 5.84f
private const val NAV_SHELL_RAIL_SHADOW_SURFACE_LIGHT = 4.74f
private const val NAV_SHELL_BORDER_DARK_ALPHA = 0.119f
private const val NAV_SHELL_BORDER_LIGHT_ALPHA = 0.086f
private const val NAV_SHELL_GLOW_DARK_ALPHA = 0.085f
private const val NAV_SHELL_GLOW_LIGHT_ALPHA = 0.077f
private const val NAV_SHELL_CONTAINER_DARK_ALPHA = 0.862f
private const val NAV_SHELL_CONTAINER_LIGHT_ALPHA = 0.956f
private const val NAV_SHELL_SURFACE_TOP_GLOW_DARK_ALPHA = 0.0262f
private const val NAV_SHELL_SURFACE_TOP_GLOW_LIGHT_ALPHA = 0.0246f
private const val NAV_SHELL_SURFACE_EDGE_DARK_ALPHA = 0.0318f
private const val NAV_SHELL_SURFACE_EDGE_LIGHT_ALPHA = 0.0148f
    private const val NAV_SHELL_TOP_RIM_HEIGHT = 1.55f
    private const val NAV_SHELL_TOP_RIM_DARK_ALPHA = 0.196f
    private const val NAV_SHELL_TOP_RIM_LIGHT_ALPHA = 0.150f
	private const val NAV_SHELL_BAR_CORNER = 31.0f
	private const val NAV_SHELL_RAIL_CORNER = 30.0f
