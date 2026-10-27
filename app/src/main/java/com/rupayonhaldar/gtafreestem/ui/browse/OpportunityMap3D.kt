package com.rupayonhaldar.gtafreestem.ui.browse

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val SOURCE_ID = "gta-physical-opportunities"
private const val CIRCLE_LAYER_ID = "gta-opportunity-circles"
private const val COUNT_LAYER_ID = "gta-opportunity-counts"
private val TORONTO_CENTRE = LatLng(43.6532, -79.3832)

internal data class NativeMapLocation(
    val id: String,
    val coordinate: MapCoordinate,
    val pins: List<MapOpportunityPin>,
)

internal fun nativeMapLocations(pins: List<MapOpportunityPin>): List<NativeMapLocation> =
    pins.asSequence()
        .filter { it.coordinate.isValid }
        .distinctBy(MapOpportunityPin::id)
        .groupBy { it.coordinate }
        .map { (coordinate, members) ->
            NativeMapLocation(
                id = "${coordinate.latitude},${coordinate.longitude}",
                coordinate = coordinate,
                pins = members,
            )
        }

/** Interactive map with a retained offline schematic and list route when tiles cannot load. */
@Composable
fun OpportunityMap3D(
    pins: List<MapOpportunityPin>,
    selectedPinId: String?,
    onPinSelected: (MapOpportunityPin) -> Unit,
    onShowDetails: (MapOpportunityPin) -> Unit,
    modifier: Modifier = Modifier,
    strings: OpportunityMapStrings = OpportunityMapStrings(),
    offlineLabel: String = "Offline preview",
) {
    val locations = remember(pins) { nativeMapLocations(pins) }
    var showSchematic by rememberSaveable { mutableStateOf(false) }
    var mapFailed by remember { mutableStateOf(false) }
    var cameraMode by rememberSaveable { mutableStateOf("3D") }
    val selectedPin = pins.firstOrNull { it.id == selectedPinId }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = strings.mapTitle,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = strings.mapHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !showSchematic && !mapFailed && cameraMode == "3D",
                onClick = { showSchematic = false; mapFailed = false; cameraMode = "3D" },
                label = { Text("3D") },
                modifier = Modifier.heightIn(min = 48.dp).testTag("map-3d-control"),
            )
            FilterChip(
                selected = !showSchematic && !mapFailed && cameraMode == "2D",
                onClick = { showSchematic = false; mapFailed = false; cameraMode = "2D" },
                label = { Text("2D") },
                modifier = Modifier.heightIn(min = 48.dp).testTag("map-2d-control"),
            )
            FilterChip(
                selected = showSchematic || mapFailed,
                onClick = { showSchematic = true },
                label = { Text(offlineLabel) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("map-offline-control"),
            )
        }

        Text(
            text = strings.visibleCount(pins.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (locations.isEmpty() || showSchematic || mapFailed) {
            OpportunityMap(
                pins = pins,
                selectedPinId = selectedPinId,
                onPinSelected = onPinSelected,
                onShowDetails = onShowDetails,
                strings = strings,
            )
        } else {
            Box(Modifier.fillMaxWidth().height(500.dp).testTag("native-3d-map")) {
                NativeMapSurface(
                    locations = locations,
                    cameraMode = cameraMode,
                    selectedPinId = selectedPinId,
                    onPinSelected = onPinSelected,
                    onLoadFailure = { mapFailed = true },
                )
            }
            selectedPin?.let { pin ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(strings.selectedOpportunityHeading, style = MaterialTheme.typography.labelLarge)
                    locations.firstOrNull { location -> location.pins.any { it.id == pin.id } }
                        ?.let { location ->
                            val position = location.pins.indexOfFirst { it.id == pin.id } + 1
                            Text(
                                strings.markerPosition(position, location.pins.size),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    Text(pin.title, style = MaterialTheme.typography.titleMedium)
                    Text("${pin.organization} · ${pin.locationLabel}", style = MaterialTheme.typography.bodySmall)
                    TextButton(
                        onClick = { onShowDetails(pin) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag(OpportunityMapTestTags.DETAILS_ACTION),
                    ) { Text(strings.showDetailsAction) }
                }
            }
            Text(
                "© OpenStreetMap · © OpenMapTiles · OpenFreeMap",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NativeMapSurface(
    locations: List<NativeMapLocation>,
    cameraMode: String,
    selectedPinId: String?,
    onPinSelected: (MapOpportunityPin) -> Unit,
    onLoadFailure: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentLocations by rememberUpdatedState(locations)
    val currentSelectedPinId by rememberUpdatedState(selectedPinId)
    val currentOnPinSelected by rememberUpdatedState(onPinSelected)
    val currentOnLoadFailure by rememberUpdatedState(onLoadFailure)
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var source by remember { mutableStateOf<GeoJsonSource?>(null) }

    val mapView = remember(context) {
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply { onCreate(null) }
    }
    DisposableEffect(mapView, lifecycle) {
        var started = false
        var resumed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> { mapView.onStart(); started = true }
                Lifecycle.Event.ON_RESUME -> { mapView.onResume(); resumed = true }
                Lifecycle.Event.ON_PAUSE -> { mapView.onPause(); resumed = false }
                Lifecycle.Event.ON_STOP -> { mapView.onStop(); started = false }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)

        mapView.addOnDidFailLoadingMapListener { currentOnLoadFailure() }
        mapView.getMapAsync { readyMap ->
            map = readyMap
            readyMap.uiSettings.isCompassEnabled = true
            readyMap.uiSettings.isAttributionEnabled = true
            readyMap.uiSettings.isRotateGesturesEnabled = true
            readyMap.uiSettings.isTiltGesturesEnabled = true
            readyMap.setStyle(STYLE_URL) { style ->
                val geoSource = GeoJsonSource(SOURCE_ID, locationsGeoJson(currentLocations))
                style.addSource(geoSource)
                style.addLayer(
                    CircleLayer(CIRCLE_LAYER_ID, SOURCE_ID).withProperties(
                        PropertyFactory.circleColor(AndroidColor.rgb(244, 196, 49)),
                        PropertyFactory.circleRadius(16f),
                        PropertyFactory.circleStrokeColor(AndroidColor.rgb(16, 52, 82)),
                        PropertyFactory.circleStrokeWidth(2f),
                    ),
                )
                style.addLayer(
                    SymbolLayer(COUNT_LAYER_ID, SOURCE_ID).withProperties(
                        PropertyFactory.textField("{count}"),
                        PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
                        PropertyFactory.textSize(12f),
                        PropertyFactory.textColor(AndroidColor.rgb(16, 52, 82)),
                        PropertyFactory.textAllowOverlap(true),
                    ),
                )
                source = geoSource
            }
            readyMap.addOnMapClickListener { coordinate ->
                val point = readyMap.projection.toScreenLocation(coordinate)
                val locationID = readyMap.queryRenderedFeatures(point, CIRCLE_LAYER_ID)
                    .firstOrNull()?.getStringProperty("location_id")
                val group = currentLocations.firstOrNull { it.id == locationID }?.pins.orEmpty()
                val selectedIndex = group.indexOfFirst { it.id == currentSelectedPinId }
                val pin = group.getOrNull((selectedIndex + 1) % group.size.coerceAtLeast(1))
                if (pin != null) currentOnPinSelected(pin)
                pin != null
            }
        }
        onDispose {
            lifecycle.removeObserver(observer)
            if (resumed) mapView.onPause()
            if (started) mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(locations, source) {
        source?.setGeoJson(locationsGeoJson(locations))
    }
    LaunchedEffect(map, cameraMode, locations) {
        val focus = locations.minByOrNull { location ->
            val latitudeDelta = location.coordinate.latitude - TORONTO_CENTRE.latitude
            val longitudeDelta = location.coordinate.longitude - TORONTO_CENTRE.longitude
            latitudeDelta * latitudeDelta + longitudeDelta * longitudeDelta
        }?.coordinate
        map?.moveCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(focus?.let { LatLng(it.latitude, it.longitude) } ?: TORONTO_CENTRE)
                    .zoom(14.8)
                    .tilt(if (cameraMode == "3D") 60.0 else 0.0)
                    .bearing(if (cameraMode == "3D") 20.0 else 0.0)
                    .build(),
            ),
        )
    }
    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
}

private fun locationsGeoJson(locations: List<NativeMapLocation>): String {
    val features = JSONArray()
    locations.forEach { location ->
        features.put(
            JSONObject()
                .put("type", "Feature")
                .put("geometry", JSONObject()
                    .put("type", "Point")
                    .put("coordinates", JSONArray()
                        .put(location.coordinate.longitude)
                        .put(location.coordinate.latitude)))
                .put("properties", JSONObject()
                    .put("location_id", location.id)
                    .put("count", location.pins.size.toString())),
        )
    }
    return JSONObject().put("type", "FeatureCollection").put("features", features).toString()
}
