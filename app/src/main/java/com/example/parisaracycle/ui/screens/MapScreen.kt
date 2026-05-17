package com.example.parisaracycle.ui.screens

import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.parisaracycle.data.model.DangerZoneType
import com.example.parisaracycle.data.model.MapLayer
import com.example.parisaracycle.data.model.PitStopType
import com.example.parisaracycle.utils.hasLocationPermission
import com.example.parisaracycle.utils.locationPermissions
import com.example.parisaracycle.viewmodel.MapUiState
import com.example.parisaracycle.viewmodel.MapViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.setLocationPermissionGranted(permissions.values.any { it })
    }

    LaunchedEffect(Unit) {
        viewModel.setLocationPermissionGranted(context.hasLocationPermission())
    }

    LaunchedEffect(uiState.message, uiState.errorMessage) {
        if (uiState.message != null || uiState.errorMessage != null) {
            delay(4_000)
            viewModel.clearTransientMessages()
        }
    }

    Box(modifier = modifier) {
        OsmCycleMap(
            uiState = uiState,
            onMapClick = viewModel::onMapClick,
            onMapLongClick = viewModel::onMapLongClick,
            modifier = Modifier.fillMaxSize()
        )

        MapControls(
            uiState = uiState,
            onRequestLocation = { locationPermissionLauncher.launch(locationPermissions) },
            onRefreshLocation = viewModel::refreshCurrentLocation,
            onPickDestination = viewModel::startDestinationPick,
            onFindRoute = viewModel::findRoute,
            onToggleLayer = viewModel::toggleLayer,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp)
        )

        if (uiState.pendingDangerPosition != null) {
            ModalBottomSheet(onDismissRequest = viewModel::dismissDangerReport) {
                DangerReportContent(
                    onSelected = viewModel::reportDangerZone,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
        ) {
            Text(
                text = "Map data: OpenStreetMap contributors",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OsmCycleMap(
    uiState: MapUiState,
    onMapClick: (LatLng) -> Unit,
    onMapLongClick: (LatLng) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val defaultCenter = LatLng(12.9141, 74.8560)
    val center = uiState.currentLocation ?: uiState.destination ?: defaultCenter
    val mapView = remember(context) {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = 3.0
            maxZoomLevel = 20.0
            controller.setZoom(15.0)
            controller.setCenter(center.toGeoPoint())
        }
    }
    var lastCenteredPoint by remember { mutableStateOf<LatLng?>(null) }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            if (lastCenteredPoint != center) {
                view.controller.setCenter(center.toGeoPoint())
                if (view.zoomLevelDouble < 13.0) view.controller.setZoom(15.0)
                lastCenteredPoint = center
            }

            view.overlays.clear()
            view.overlays.add(
                MapEventsOverlay(
                    object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(point: GeoPoint?): Boolean {
                            point?.let { onMapClick(it.toLatLng()) }
                            return uiState.isPickingDestination
                        }

                        override fun longPressHelper(point: GeoPoint?): Boolean {
                            point?.let { onMapLongClick(it.toLatLng()) }
                            return true
                        }
                    }
                )
            )

            if (MapLayer.Route in uiState.enabledLayers && uiState.routePoints.size > 1) {
                view.overlays.add(
                    Polyline().apply {
                        setPoints(uiState.routePoints.map { it.toGeoPoint() })
                        outlinePaint.color = AndroidColor.rgb(27, 94, 32)
                        outlinePaint.strokeWidth = 10f
                    }
                )
            }

            if (MapLayer.PitStops in uiState.enabledLayers) {
                uiState.pitStops.forEach { pitStop ->
                    val color = when (pitStop.type) {
                        PitStopType.Repair -> AndroidColor.rgb(224, 122, 45)
                        PitStopType.Water -> AndroidColor.rgb(0, 119, 182)
                    }
                    view.addCycleMarker(
                        position = pitStop.position,
                        title = pitStop.name,
                        color = color
                    )
                }
            }

            if (MapLayer.Danger in uiState.enabledLayers) {
                uiState.dangerZones.forEach { zone ->
                    view.addCycleMarker(
                        position = zone.position,
                        title = zone.type.label,
                        color = AndroidColor.rgb(198, 40, 40)
                    )
                }
            }

            if (MapLayer.Buddies in uiState.enabledLayers) {
                uiState.buddyLocations.forEach { buddy ->
                    view.addCycleMarker(
                        position = buddy.position,
                        title = "Rider",
                        color = AndroidColor.rgb(123, 31, 162)
                    )
                }
            }

            uiState.destination?.let { destination ->
                view.addCycleMarker(
                    position = destination,
                    title = "Destination",
                    color = AndroidColor.rgb(46, 125, 50),
                    size = 38
                )
            }

            uiState.currentLocation?.let { source ->
                view.addCycleMarker(
                    position = source,
                    title = "You",
                    color = AndroidColor.rgb(21, 101, 192),
                    size = 34
                )
            }

            view.invalidate()
        }
    )
}

@Composable
private fun MapControls(
    uiState: MapUiState,
    onRequestLocation: () -> Unit,
    onRefreshLocation: () -> Unit,
    onPickDestination: () -> Unit,
    onFindRoute: () -> Unit,
    onToggleLayer: (MapLayer) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (uiState.currentLocation == null) "Source: location pending" else "Source: current location",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Destination: ${uiState.destination?.coordinateLabel() ?: "not selected"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onRefreshLocation, enabled = uiState.hasLocationPermission) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh location")
                }
            }

            if (!uiState.hasLocationPermission) {
                Button(
                    onClick = onRequestLocation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Allow location")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPickDestination,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (uiState.isPickingDestination) "Tap map" else "Pick destination")
                }
                Button(
                    onClick = onFindRoute,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isRouting
                ) {
                    if (uiState.isRouting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Route")
                    }
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapLayer.entries.forEach { layer ->
                    FilterChip(
                        selected = layer in uiState.enabledLayers,
                        onClick = { onToggleLayer(layer) },
                        label = { Text(layer.label) },
                        leadingIcon = if (layer in uiState.enabledLayers) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else {
                            null
                        }
                    )
                }
            }

            uiState.routeDistanceKm?.let { distance ->
                Text(
                    text = "Selected route: %.1f km".format(distance),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            uiState.message?.let {
                MessageBanner(text = it, isError = false)
            }
            uiState.errorMessage?.let {
                MessageBanner(text = it, isError = true)
            }
        }
    }
}

@Composable
private fun MessageBanner(text: String, isError: Boolean) {
    val container = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DangerReportContent(
    onSelected: (DangerZoneType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Report danger zone",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        DangerZoneType.entries.forEach { type ->
            ListItem(
                headlineContent = { Text(type.label) },
                leadingContent = {
                    Icon(
                        imageVector = when (type) {
                            DangerZoneType.Pothole -> Icons.Default.Warning
                            DangerZoneType.DangerousIntersection -> Icons.Default.LocationOn
                            DangerZoneType.BlockedPath -> Icons.Default.Build
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.clickable { onSelected(type) }
            )
        }
    }
}

private fun MapView.addCycleMarker(
    position: LatLng,
    title: String,
    color: Int,
    size: Int = 30
) {
    overlays.add(
        Marker(this).apply {
            this.position = position.toGeoPoint()
            this.title = title
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = markerDrawable(color, size)
        }
    )
}

private fun markerDrawable(color: Int, size: Int): GradientDrawable =
    GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke(5, AndroidColor.WHITE)
        setSize(size, size)
    }

private fun LatLng.toGeoPoint(): GeoPoint =
    GeoPoint(latitude, longitude)

private fun GeoPoint.toLatLng(): LatLng =
    LatLng(latitude, longitude)

private fun LatLng.coordinateLabel(): String =
    String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
