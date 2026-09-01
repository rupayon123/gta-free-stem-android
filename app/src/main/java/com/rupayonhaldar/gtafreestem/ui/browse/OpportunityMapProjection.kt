package com.rupayonhaldar.gtafreestem.ui.browse

import com.rupayonhaldar.gtafreestem.domain.model.Opportunity
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

/** A finite latitude/longitude pair that can be drawn by the offline opportunity map. */
data class MapCoordinate(
    val latitude: Double,
    val longitude: Double,
) {
    val isValid: Boolean
        get() = latitude.isFinite() &&
            longitude.isFinite() &&
            latitude in -90.0..90.0 &&
            longitude in -180.0..180.0
}

/** Display data for an opportunity marker. The map never fetches or stores location data. */
data class MapOpportunityPin(
    val id: String,
    val title: String,
    val organization: String,
    val locationLabel: String,
    val coordinate: MapCoordinate,
)

/**
 * Creates a map marker only when both coordinates are present and valid.
 *
 * Callers can supply already-localized display values without changing the underlying record.
 */
fun Opportunity.toMapOpportunityPin(
    displayTitle: String = title,
    displayOrganization: String = organization,
    displayLocation: String = listOf(city, region).filter(String::isNotBlank).joinToString(", "),
): MapOpportunityPin? {
    val resolvedLatitude = latitude ?: return null
    val resolvedLongitude = longitude ?: return null
    val coordinate = MapCoordinate(resolvedLatitude, resolvedLongitude)
    if (!coordinate.isValid) return null

    return MapOpportunityPin(
        id = id,
        title = displayTitle,
        organization = displayOrganization,
        locationLabel = displayLocation,
        coordinate = coordinate,
    )
}

/** Geographic viewport used to project pins without a map or tile service. */
data class MapViewportBounds(
    val south: Double,
    val north: Double,
    val west: Double,
    val east: Double,
) {
    init {
        require(south.isFinite() && north.isFinite() && west.isFinite() && east.isFinite())
        require(south < north)
        require(west < east)
        require(south >= -90.0 && north <= 90.0)
        require(west >= -180.0 && east <= 180.0)
    }

    fun contains(coordinate: MapCoordinate): Boolean =
        coordinate.isValid &&
            coordinate.latitude in south..north &&
            coordinate.longitude in west..east
}

/** A normalized map position where (0, 0) is northwest and (1, 1) is southeast. */
data class ProjectedMapPosition(
    val x: Float,
    val y: Float,
)

/** A stable, tappable group of pins occupying the same readable map region. */
data class ProjectedMapCluster(
    val pins: List<MapOpportunityPin>,
    val position: ProjectedMapPosition,
)

/**
 * Groups dense projected results into a fixed geographic grid.
 *
 * Five columns and five rows leave enough room for 48 dp touch targets on a compact phone while
 * preserving every opportunity inside a cluster. Group order and member order follow the input,
 * so repeated taps can deterministically cycle through colocated results.
 */
internal fun clusterProjectedPins(
    projectedPins: List<Pair<MapOpportunityPin, ProjectedMapPosition>>,
    columns: Int = MAP_CLUSTER_COLUMNS,
    rows: Int = MAP_CLUSTER_ROWS,
): List<ProjectedMapCluster> {
    require(columns > 0)
    require(rows > 0)

    return projectedPins
        .groupBy { (_, position) ->
            MapClusterCell(
                column = (position.x.coerceIn(0f, 1f) * columns)
                    .toInt()
                    .coerceAtMost(columns - 1),
                row = (position.y.coerceIn(0f, 1f) * rows)
                    .toInt()
                    .coerceAtMost(rows - 1),
            )
        }
        .map { (cell, members) ->
            ProjectedMapCluster(
                pins = members.map(Pair<MapOpportunityPin, ProjectedMapPosition>::first),
                position = ProjectedMapPosition(
                    x = (cell.column + 0.5f) / columns,
                    y = (cell.row + 0.5f) / rows,
                ),
            )
        }
}

/**
 * A broad Greater Toronto Area viewport used when a result set has no geographic spread.
 * Dynamic result bounds are preferred whenever more than one distinct location is available.
 */
val GreaterTorontoAreaViewport = MapViewportBounds(
    south = 43.35,
    north = 44.25,
    west = -80.20,
    east = -78.75,
)

/**
 * Computes a padded viewport around valid result coordinates.
 *
 * The minimum span keeps a single location legible. Invalid coordinates are ignored. The
 * viewport stays within the ranges accepted by standard latitude/longitude coordinates.
 */
fun mapViewportFor(
    coordinates: List<MapCoordinate>,
    fallback: MapViewportBounds = GreaterTorontoAreaViewport,
): MapViewportBounds {
    val validCoordinates = coordinates.filter(MapCoordinate::isValid)
    if (validCoordinates.isEmpty()) return fallback

    val rawSouth = validCoordinates.minOf(MapCoordinate::latitude)
    val rawNorth = validCoordinates.maxOf(MapCoordinate::latitude)
    val rawWest = validCoordinates.minOf(MapCoordinate::longitude)
    val rawEast = validCoordinates.maxOf(MapCoordinate::longitude)

    val latitudeSpan = max(rawNorth - rawSouth, MINIMUM_LATITUDE_SPAN)
    val longitudeSpan = max(rawEast - rawWest, MINIMUM_LONGITUDE_SPAN)
    val latitudeCentre = (rawNorth + rawSouth) / 2.0
    val longitudeCentre = (rawEast + rawWest) / 2.0
    val paddedLatitudeSpan = latitudeSpan * VIEWPORT_PADDING_FACTOR
    val paddedLongitudeSpan = longitudeSpan * VIEWPORT_PADDING_FACTOR

    return boundedViewport(
        south = latitudeCentre - paddedLatitudeSpan / 2.0,
        north = latitudeCentre + paddedLatitudeSpan / 2.0,
        west = longitudeCentre - paddedLongitudeSpan / 2.0,
        east = longitudeCentre + paddedLongitudeSpan / 2.0,
    )
}

/**
 * Projects a coordinate with a Web Mercator latitude transform and a linear longitude axis.
 * Geographic east/west is intentionally independent of the interface layout direction.
 */
fun projectMapCoordinate(
    coordinate: MapCoordinate,
    viewport: MapViewportBounds,
): ProjectedMapPosition? {
    if (!coordinate.isValid || !viewport.contains(coordinate)) return null

    val longitudeSpan = viewport.east - viewport.west
    val x = ((coordinate.longitude - viewport.west) / longitudeSpan).toFloat()

    val northMercator = mercatorLatitude(viewport.north)
    val southMercator = mercatorLatitude(viewport.south)
    val pointMercator = mercatorLatitude(coordinate.latitude)
    val mercatorSpan = northMercator - southMercator
    val y = ((northMercator - pointMercator) / mercatorSpan).toFloat()

    return ProjectedMapPosition(
        x = x.coerceIn(0f, 1f),
        y = y.coerceIn(0f, 1f),
    )
}

private fun boundedViewport(
    south: Double,
    north: Double,
    west: Double,
    east: Double,
): MapViewportBounds {
    val latitudeSpan = north - south
    val longitudeSpan = east - west

    val boundedSouth = max(-90.0, south)
    val boundedNorth = min(90.0, north)
    val shiftedSouth = if (boundedNorth - boundedSouth < latitudeSpan) {
        max(-90.0, boundedNorth - latitudeSpan)
    } else {
        boundedSouth
    }
    val shiftedNorth = if (boundedNorth - shiftedSouth < latitudeSpan) {
        min(90.0, shiftedSouth + latitudeSpan)
    } else {
        boundedNorth
    }

    val boundedWest = max(-180.0, west)
    val boundedEast = min(180.0, east)
    val shiftedWest = if (boundedEast - boundedWest < longitudeSpan) {
        max(-180.0, boundedEast - longitudeSpan)
    } else {
        boundedWest
    }
    val shiftedEast = if (boundedEast - shiftedWest < longitudeSpan) {
        min(180.0, shiftedWest + longitudeSpan)
    } else {
        boundedEast
    }

    return MapViewportBounds(
        south = shiftedSouth,
        north = shiftedNorth,
        west = shiftedWest,
        east = shiftedEast,
    )
}

private fun mercatorLatitude(latitude: Double): Double {
    val clampedLatitude = latitude.coerceIn(-MERCATOR_LATITUDE_LIMIT, MERCATOR_LATITUDE_LIMIT)
    val radians = clampedLatitude * PI / 180.0
    return ln(tan(PI / 4.0 + radians / 2.0))
}

private const val MINIMUM_LATITUDE_SPAN = 0.12
private const val MINIMUM_LONGITUDE_SPAN = 0.18
private const val VIEWPORT_PADDING_FACTOR = 1.20
private const val MERCATOR_LATITUDE_LIMIT = 85.05112878
private const val MAP_CLUSTER_COLUMNS = 5
private const val MAP_CLUSTER_ROWS = 5

private data class MapClusterCell(
    val column: Int,
    val row: Int,
)
