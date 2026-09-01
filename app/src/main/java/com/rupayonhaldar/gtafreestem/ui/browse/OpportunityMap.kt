package com.rupayonhaldar.gtafreestem.ui.browse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import com.rupayonhaldar.gtafreestem.ui.design.StoryCard
import com.rupayonhaldar.gtafreestem.ui.design.StoryButton
import com.rupayonhaldar.gtafreestem.ui.design.StoryButtonKind
import com.rupayonhaldar.gtafreestem.ui.design.StoryRadius
import com.rupayonhaldar.gtafreestem.ui.design.StorySpacing

/** Localizable copy for [OpportunityMap] and [OpportunityMapPreview]. */
data class OpportunityMapStrings(
    val mapTitle: String = "Opportunity map",
    val singleVisibleFormat: String = "%1\$s opportunity shown",
    val multipleVisibleFormat: String = "%1\$s opportunities shown",
    val mapHint: String = "Offline schematic. Locations are approximate; select a marker to review its source details.",
    val northLabel: String = "N",
    val selectedLabel: String = "Selected",
    val notSelectedLabel: String = "Not selected",
    val markerPositionFormat: String = "Marker %1\$s of %2\$s",
    val markerActionLabel: String = "Select opportunity",
    val selectedOpportunityHeading: String = "Selected opportunity",
    val sourceDetailsHint: String = "Review the listing and its source before registering.",
    val showDetailsAction: String = "View source details",
    val emptyTitle: String = "No locations to map",
    val emptyMessage: String = "These results do not include valid location coordinates. Use the list to view every opportunity.",
    val previewTitle: String = "Location preview",
    val previewAction: String = "Show on opportunity map",
    val previewEmptyMessage: String = "A map preview is unavailable because this opportunity has no valid location coordinates.",
    val numberLocale: Locale = Locale.getDefault(),
) {
    fun visibleCount(count: Int): String = String.format(
        numberLocale,
        if (count == 1) singleVisibleFormat else multipleVisibleFormat,
        localizedInteger(count),
    )

    fun markerPosition(index: Int, count: Int): String = String.format(
        numberLocale,
        markerPositionFormat,
        localizedInteger(index),
        localizedInteger(count),
    )

    fun clusterSize(count: Int): String = localizedInteger(count)

    private fun localizedInteger(value: Int): String {
        val zeroDigit = when (numberLocale.getUnicodeLocaleType("nu")) {
            "latn" -> '0'
            "arab" -> '\u0660'
            "arabext" -> '\u06F0'
            else -> when (numberLocale.language) {
                "ar" -> '\u0660'
                "fa", "ur" -> '\u06F0'
                else -> DecimalFormatSymbols.getInstance(numberLocale).zeroDigit
            }
        }
        return value.toString().map { character ->
            if (character in '0'..'9') {
                (zeroDigit.code + (character - '0')).toChar()
            } else {
                character
            }
        }.joinToString(separator = "")
    }
}

/** Stable tags used by focused accessibility tests and release smoke checks. */
object OpportunityMapTestTags {
    const val MAP = "opportunity_map"
    const val VISIBLE_COUNT = "opportunity_map_visible_count"
    const val EMPTY = "opportunity_map_empty"
    const val SELECTED_CARD = "opportunity_map_selected_card"
    const val DETAILS_ACTION = "opportunity_map_details_action"
    const val PREVIEW = "opportunity_map_preview"
    const val PREVIEW_EMPTY = "opportunity_map_preview_empty"

    fun marker(id: String): String = "opportunity_map_marker_$id"
}

/**
 * A zero-cost, offline presentation of opportunity coordinates.
 *
 * This is intentionally a schematic rather than a navigation map: it uses no map SDK, API key,
 * network tiles, tracking, or background location. Pins are accurately projected inside a
 * viewport derived from the supplied results. Registration and directions remain outside this
 * component.
 */
@Composable
fun OpportunityMap(
    pins: List<MapOpportunityPin>,
    selectedPinId: String?,
    onPinSelected: (MapOpportunityPin) -> Unit,
    onShowDetails: (MapOpportunityPin) -> Unit,
    modifier: Modifier = Modifier,
    strings: OpportunityMapStrings = OpportunityMapStrings(),
) {
    val visiblePins = remember(pins) {
        pins.asSequence()
            .filter { it.coordinate.isValid }
            .distinctBy(MapOpportunityPin::id)
            .toList()
    }
    val selectedPin = visiblePins.firstOrNull { it.id == selectedPinId }
    val visibleCountLabel = strings.visibleCount(visiblePins.size)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(StorySpacing.Medium),
    ) {
        StoryCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = StoryRadius.Card,
            padding = StorySpacing.Standard,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = strings.mapTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = strings.mapHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                ) {
                    Text(
                        text = visibleCountLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .testTag(OpportunityMapTestTags.VISIBLE_COUNT)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }

        if (visiblePins.isEmpty()) {
            OpportunityMapEmptyState(
                strings = strings,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            StoryCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = StoryRadius.Card,
                padding = 0.dp,
            ) {
                OpportunityMapSurface(
                    pins = visiblePins,
                    selectedPinId = selectedPinId,
                    onPinSelected = onPinSelected,
                    strings = strings,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (selectedPin != null) {
            SelectedOpportunityCard(
                pin = selectedPin,
                onShowDetails = onShowDetails,
                strings = strings,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Compact map/fallback suitable for an opportunity detail screen. */
@Composable
fun OpportunityMapPreview(
    pin: MapOpportunityPin?,
    modifier: Modifier = Modifier,
    strings: OpportunityMapStrings = OpportunityMapStrings(),
    onOpenMap: (() -> Unit)? = null,
) {
    val validPin = pin?.takeIf { it.coordinate.isValid }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = strings.previewTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )

        if (validPin == null) {
            StoryCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(OpportunityMapTestTags.PREVIEW_EMPTY),
                cornerRadius = StoryRadius.Card,
                padding = StorySpacing.Medium,
            ) {
                Text(
                    text = strings.previewEmptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        val viewport = remember(validPin) { mapViewportFor(listOf(validPin.coordinate)) }
        val position = remember(validPin, viewport) {
            requireNotNull(projectMapCoordinate(validPin.coordinate, viewport))
        }
        val description = listOf(
            strings.previewTitle,
            validPin.title,
            validPin.organization,
            validPin.locationLabel,
            strings.mapHint,
        ).filter(String::isNotBlank).joinToString(". ")

        StoryCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = StoryRadius.Card,
            padding = StorySpacing.Medium,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .testTag(OpportunityMapTestTags.PREVIEW)
                    .semantics { contentDescription = description },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp,
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val containerWidth = maxWidth
                    val containerHeight = maxHeight
                    GeographicMapLayer {
                        SchematicBaseMap(modifier = Modifier.fillMaxSize())
                        PositionedMapMarker(
                            pin = validPin,
                            position = position,
                            index = 1,
                            count = 1,
                            selected = true,
                            strings = strings,
                            containerWidth = containerWidth,
                            containerHeight = containerHeight,
                            onClick = null,
                        )
                    }
                }
            }
        }

        if (onOpenMap != null) {
            StoryButton(
                onClick = onOpenMap,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                kind = StoryButtonKind.QUIET,
            ) {
                Text(strings.previewAction)
            }
        }
    }
}

@Composable
private fun OpportunityMapSurface(
    pins: List<MapOpportunityPin>,
    selectedPinId: String?,
    onPinSelected: (MapOpportunityPin) -> Unit,
    strings: OpportunityMapStrings,
    modifier: Modifier = Modifier,
) {
    val viewport = remember(pins) { mapViewportFor(pins.map(MapOpportunityPin::coordinate)) }
    val projectedPins = remember(pins, viewport) {
        val basePositions = pins.mapNotNull { pin ->
            projectMapCoordinate(pin.coordinate, viewport)?.let { position -> pin to position }
        }
        val separatedPins = spreadCoincidentPins(basePositions)
        clusterProjectedPins(separatedPins)
    }
    BoxWithConstraints(modifier = modifier) {
        val mapHeight: Dp = when {
            maxWidth < 360.dp -> 280.dp
            maxWidth < 600.dp -> 320.dp
            else -> 400.dp
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(mapHeight)
                .testTag(OpportunityMapTestTags.MAP)
                .semantics {
                    contentDescription = "${strings.visibleCount(pins.size)}. ${strings.mapHint}"
                },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
            tonalElevation = 1.dp,
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val containerWidth = maxWidth
                val containerHeight = maxHeight
                GeographicMapLayer {
                    SchematicBaseMap(modifier = Modifier.fillMaxSize())

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(38.dp)
                            .zIndex(2f),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shadowElevation = 3.dp,
                        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = strings.northLabel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }

                    projectedPins.forEachIndexed { index, cluster ->
                        val selectedClusterPin = cluster.pins
                            .firstOrNull { it.id == selectedPinId }
                        val displayedPin = selectedClusterPin ?: cluster.pins.first()
                        val selectedMemberIndex = selectedClusterPin
                            ?.let { cluster.pins.indexOf(it) }
                            ?: -1
                        val nextPin = if (selectedMemberIndex >= 0 && cluster.pins.size > 1) {
                            cluster.pins[(selectedMemberIndex + 1) % cluster.pins.size]
                        } else {
                            displayedPin
                        }
                        PositionedMapMarker(
                            pin = displayedPin,
                            position = cluster.position,
                            index = index + 1,
                            count = projectedPins.size,
                            clusterSize = cluster.pins.size,
                            selected = selectedClusterPin != null,
                            strings = strings,
                            containerWidth = containerWidth,
                            containerHeight = containerHeight,
                            onClick = { onPinSelected(nextPin) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeographicMapLayer(content: @Composable BoxScope.() -> Unit) {
    // A geographical map must never mirror east/west when the surrounding interface is RTL.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
private fun SchematicBaseMap(modifier: Modifier = Modifier) {
    val landColor = MaterialTheme.colorScheme.surfaceVariant
    val waterColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f)
    val shorelineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
    val majorRoadColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.34f)
    val secondaryRoadColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    val parkColor = MaterialTheme.colorScheme.secondaryContainer

    Canvas(
        modifier = modifier.clip(MaterialTheme.shapes.large),
    ) {
        drawRect(landColor)

        val lake = Path().apply {
            moveTo(0f, size.height * 0.78f)
            cubicTo(
                size.width * 0.22f,
                size.height * 0.72f,
                size.width * 0.42f,
                size.height * 0.84f,
                size.width * 0.62f,
                size.height * 0.78f,
            )
            cubicTo(
                size.width * 0.78f,
                size.height * 0.73f,
                size.width * 0.90f,
                size.height * 0.78f,
                size.width,
                size.height * 0.72f,
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path = lake, color = waterColor)
        drawPath(path = lake, color = shorelineColor, style = Stroke(width = 2.dp.toPx()))

        for (column in 1..5) {
            val x = size.width * column / 6f
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1.dp.toPx(),
            )
        }
        for (row in 1..4) {
            val y = size.height * row / 5f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        drawLine(
            color = majorRoadColor,
            start = Offset(size.width * 0.06f, size.height * 0.64f),
            end = Offset(size.width * 0.94f, size.height * 0.52f),
            strokeWidth = 4.dp.toPx(),
        )
        drawLine(
            color = secondaryRoadColor,
            start = Offset(size.width * 0.20f, size.height * 0.08f),
            end = Offset(size.width * 0.62f, size.height * 0.76f),
            strokeWidth = 3.dp.toPx(),
        )
        drawLine(
            color = secondaryRoadColor,
            start = Offset(size.width * 0.76f, size.height * 0.04f),
            end = Offset(size.width * 0.54f, size.height * 0.78f),
            strokeWidth = 3.dp.toPx(),
        )

        drawOval(
            color = parkColor.copy(alpha = 0.45f),
            topLeft = Offset(size.width * 0.08f, size.height * 0.12f),
            size = Size(size.width * 0.18f, size.height * 0.12f),
        )
        drawOval(
            color = parkColor.copy(alpha = 0.38f),
            topLeft = Offset(size.width * 0.70f, size.height * 0.20f),
            size = Size(size.width * 0.20f, size.height * 0.13f),
        )
    }
}

@Composable
private fun PositionedMapMarker(
    pin: MapOpportunityPin,
    position: ProjectedMapPosition,
    index: Int,
    count: Int,
    clusterSize: Int = 1,
    selected: Boolean,
    strings: OpportunityMapStrings,
    containerWidth: Dp,
    containerHeight: Dp,
    onClick: (() -> Unit)?,
) {
    val density = LocalDensity.current
    val latestOnClick by rememberUpdatedState(onClick)
    val markerTargetSize = 48.dp
    val edgePadding = 30.dp
    val offset = remember(position, containerWidth, containerHeight, density) {
        with(density) {
            val widthPx = containerWidth.toPx()
            val heightPx = containerHeight.toPx()
            val targetPx = markerTargetSize.toPx()
            val paddingPx = edgePadding.toPx()
            val availableWidth = (widthPx - 2f * paddingPx).coerceAtLeast(0f)
            val availableHeight = (heightPx - 2f * paddingPx).coerceAtLeast(0f)
            val centreX = paddingPx + availableWidth * position.x
            val centreY = paddingPx + availableHeight * position.y
            IntOffset(
                x = (centreX - targetPx / 2f).roundToInt(),
                y = (centreY - targetPx / 2f).roundToInt(),
            )
        }
    }
    val markerDescription = listOf(
        strings.visibleCount(clusterSize).takeIf { clusterSize > 1 }.orEmpty(),
        pin.title,
        pin.organization,
        pin.locationLabel,
        strings.markerPosition(index, count),
    ).filter(String::isNotBlank).joinToString(". ")

    val markerModifier = Modifier
        .absoluteOffset { offset }
        .size(markerTargetSize)
        .zIndex(if (selected) 3f else 1f)
        .testTag(OpportunityMapTestTags.marker(pin.id))
        .semantics {
            contentDescription = markerDescription
            this.selected = selected
            stateDescription = if (selected) strings.selectedLabel else strings.notSelectedLabel
        }

    val interactionModifier = if (onClick != null) {
        Modifier.clickable(
            onClickLabel = strings.markerActionLabel,
            role = Role.Button,
            onClick = { latestOnClick?.invoke() },
        )
    } else {
        Modifier
    }
    Surface(
        modifier = markerModifier.then(interactionModifier),
        shape = CircleShape,
        color = Color.Transparent,
    ) {
        MapMarkerGlyph(
            selected = selected,
            clusterSize = clusterSize,
            clusterSizeLabel = strings.clusterSize(clusterSize),
        )
    }
}

@Composable
private fun MapMarkerGlyph(
    selected: Boolean,
    clusterSize: Int,
    clusterSizeLabel: String,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(
                when {
                    selected -> 38.dp
                    clusterSize > 1 -> 34.dp
                    else -> 28.dp
                }
            ),
            shape = CircleShape,
            color = if (selected) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.primary
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onTertiary
            } else {
                MaterialTheme.colorScheme.onPrimary
            },
            border = BorderStroke(3.dp, MaterialTheme.colorScheme.surface),
            shadowElevation = if (selected) 5.dp else 3.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (clusterSize > 1) {
                    Text(
                        text = clusterSizeLabel,
                        fontSize = if (clusterSize >= 10) 10.sp else 12.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(if (selected) 8.dp else 6.dp),
                        shape = CircleShape,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onTertiary
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        },
                    ) {
                        Text("")
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedOpportunityCard(
    pin: MapOpportunityPin,
    onShowDetails: (MapOpportunityPin) -> Unit,
    strings: OpportunityMapStrings,
    modifier: Modifier = Modifier,
) {
    StoryCard(
        modifier = modifier.testTag(OpportunityMapTestTags.SELECTED_CARD),
        cornerRadius = StoryRadius.Card,
        padding = StorySpacing.Medium,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(StorySpacing.Small),
        ) {
            Text(
                text = strings.selectedOpportunityHeading,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = pin.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = listOf(pin.organization, pin.locationLabel)
                    .filter(String::isNotBlank)
                    .joinToString(" • "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = strings.sourceDetailsHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StoryButton(
                onClick = { onShowDetails(pin) },
                kind = StoryButtonKind.PRIMARY,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(OpportunityMapTestTags.DETAILS_ACTION),
            ) {
                Text(strings.showDetailsAction)
            }
        }
    }
}

@Composable
private fun OpportunityMapEmptyState(
    strings: OpportunityMapStrings,
    modifier: Modifier = Modifier,
) {
    StoryCard(
        modifier = modifier
            .heightIn(min = 180.dp)
            .testTag(OpportunityMapTestTags.EMPTY),
        cornerRadius = StoryRadius.Card,
        padding = StorySpacing.Large,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("—", fontWeight = FontWeight.Black)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = strings.emptyTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = strings.emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Separates records that share an exact coordinate so every marker remains discoverable.
 * The result stays close to the accurately projected source position and is deterministic.
 */
internal fun spreadCoincidentPins(
    projectedPins: List<Pair<MapOpportunityPin, ProjectedMapPosition>>,
): List<Pair<MapOpportunityPin, ProjectedMapPosition>> {
    val grouped = projectedPins.groupBy { (pin, _) ->
        CoordinateKey(
            latitude = (pin.coordinate.latitude * COORDINATE_GROUP_PRECISION).roundToInt(),
            longitude = (pin.coordinate.longitude * COORDINATE_GROUP_PRECISION).roundToInt(),
        )
    }

    return projectedPins.map { projectedPin ->
        val (pin, basePosition) = projectedPin
        val group = checkNotNull(
            grouped[
                CoordinateKey(
                    latitude = (pin.coordinate.latitude * COORDINATE_GROUP_PRECISION).roundToInt(),
                    longitude = (pin.coordinate.longitude * COORDINATE_GROUP_PRECISION).roundToInt(),
                ),
            ],
        )
        if (group.size == 1) return@map projectedPin

        val index = group.indexOfFirst { (groupedPin) -> groupedPin.id == pin.id }
            .coerceAtLeast(0)
        val angle = -PI / 2.0 + (2.0 * PI * index / group.size)
        val radius = when {
            group.size <= 2 -> 0.075
            group.size <= 5 -> 0.085
            else -> 0.10
        }
        pin to ProjectedMapPosition(
            x = (basePosition.x + cos(angle).toFloat() * radius.toFloat()).coerceIn(0.04f, 0.96f),
            y = (basePosition.y + sin(angle).toFloat() * radius.toFloat()).coerceIn(0.04f, 0.96f),
        )
    }
}

private data class CoordinateKey(
    val latitude: Int,
    val longitude: Int,
)

private const val COORDINATE_GROUP_PRECISION = 100_000.0
