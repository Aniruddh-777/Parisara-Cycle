package com.example.parisaracycle.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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
        KeylessCycleMap(
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
    }
}

@Composable
private fun KeylessCycleMap(
    uiState: MapUiState,
    onMapClick: (LatLng) -> Unit,
    onMapLongClick: (LatLng) -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultCenter = LatLng(12.9716, 77.5946)
    val center = uiState.currentLocation ?: uiState.destination ?: defaultCenter
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val labelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0xFF263128).toArgb()
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
    val smallLabelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0xFF435147).toArgb()
            textSize = 22f
        }
    }

    Canvas(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .pointerInput(center, canvasSize, uiState.isPickingDestination) {
                detectTapGestures(
                    onTap = { offset ->
                        if (canvasSize.width > 0 && canvasSize.height > 0) {
                            onMapClick(offsetToLatLng(offset, center, canvasSize))
                        }
                    },
                    onLongPress = { offset ->
                        if (canvasSize.width > 0 && canvasSize.height > 0) {
                            onMapLongClick(offsetToLatLng(offset, center, canvasSize))
                        }
                    }
                )
            }
    ) {
        drawRect(Color(0xFFEAF2E6))

        val roadColor = Color(0xFFD7DECF)
        val cycleColor = Color(0xFF9BCF9C)
        val majorRoadColor = Color(0xFFC4CEBE)

        var x = -80f
        while (x < size.width + 80f) {
            drawLine(
                color = if (((x / 160).toInt() % 2) == 0) majorRoadColor else roadColor,
                start = Offset(x, 0f),
                end = Offset(x + size.height * 0.24f, size.height),
                strokeWidth = if (((x / 160).toInt() % 2) == 0) 6f else 3f
            )
            x += 80f
        }

        var y = -80f
        while (y < size.height + 80f) {
            drawLine(
                color = roadColor,
                start = Offset(0f, y),
                end = Offset(size.width, y - size.width * 0.12f),
                strokeWidth = 3f
            )
            y += 80f
        }

        repeat(4) { index ->
            val pathY = size.height * (0.28f + index * 0.16f)
            drawLine(
                color = cycleColor,
                start = Offset(-40f, pathY),
                end = Offset(size.width + 40f, pathY + if (index % 2 == 0) 55f else -45f),
                strokeWidth = 7f
            )
        }

        if (MapLayer.Route in uiState.enabledLayers && uiState.routePoints.size > 1) {
            uiState.routePoints.zipWithNext().forEach { (from, to) ->
                drawLine(
                    color = Color(0xFF1B5E20),
                    start = latLngToOffset(from, center, canvasSize),
                    end = latLngToOffset(to, center, canvasSize),
                    strokeWidth = 12f
                )
            }
        }

        if (MapLayer.PitStops in uiState.enabledLayers) {
            uiState.pitStops.forEach { pitStop ->
                val color = when (pitStop.type) {
                    PitStopType.Repair -> Color(0xFFE07A2D)
                    PitStopType.Water -> Color(0xFF0077B6)
                }
                drawMarker(
                    position = latLngToOffset(pitStop.position, center, canvasSize),
                    color = color,
                    label = pitStop.name.take(14),
                    paint = smallLabelPaint
                )
            }
        }

        if (MapLayer.Danger in uiState.enabledLayers) {
            uiState.dangerZones.forEach { zone ->
                drawMarker(
                    position = latLngToOffset(zone.position, center, canvasSize),
                    color = Color(0xFFC62828),
                    label = zone.type.label.take(14),
                    paint = smallLabelPaint
                )
            }
        }

        if (MapLayer.Buddies in uiState.enabledLayers) {
            uiState.buddyLocations.forEach { buddy ->
                drawMarker(
                    position = latLngToOffset(buddy.position, center, canvasSize),
                    color = Color(0xFF7B1FA2),
                    label = "Rider",
                    paint = smallLabelPaint
                )
            }
        }

        uiState.destination?.let { destination ->
            drawMarker(
                position = latLngToOffset(destination, center, canvasSize),
                color = Color(0xFF2E7D32),
                label = "Destination",
                paint = labelPaint,
                radius = 15f
            )
        }

        uiState.currentLocation?.let { source ->
            drawCircle(
                color = Color(0xFF1565C0),
                radius = 18f,
                center = latLngToOffset(source, center, canvasSize)
            )
            drawCircle(
                color = Color.White,
                radius = 7f,
                center = latLngToOffset(source, center, canvasSize)
            )
            drawContext.canvas.nativeCanvas.drawText(
                "You",
                latLngToOffset(source, center, canvasSize).x + 20f,
                latLngToOffset(source, center, canvasSize).y - 18f,
                labelPaint
            )
        }

        drawContext.canvas.nativeCanvas.drawText(
            "Keyless local map",
            28f,
            size.height - 36f,
            smallLabelPaint
        )
    }
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMarker(
    position: Offset,
    color: Color,
    label: String,
    paint: Paint,
    radius: Float = 12f
) {
    if (position.x !in -60f..(size.width + 60f) || position.y !in -60f..(size.height + 60f)) return
    drawCircle(color = Color.White, radius = radius + 5f, center = position)
    drawCircle(color = color, radius = radius, center = position)
    drawCircle(color = color.copy(alpha = 0.22f), radius = radius + 18f, center = position, style = Stroke(2f))
    drawContext.canvas.nativeCanvas.drawText(label, position.x + radius + 8f, position.y - radius, paint)
}

private fun latLngToOffset(point: LatLng, center: LatLng, size: IntSize): Offset {
    if (size.width == 0 || size.height == 0) return Offset.Zero
    val latitudeSpan = 0.05
    val longitudeSpan = latitudeSpan * size.width / size.height.coerceAtLeast(1).toDouble()
    val x = size.width / 2f + ((point.longitude - center.longitude) / longitudeSpan * size.width).toFloat()
    val y = size.height / 2f - ((point.latitude - center.latitude) / latitudeSpan * size.height).toFloat()
    return Offset(x, y)
}

private fun offsetToLatLng(offset: Offset, center: LatLng, size: IntSize): LatLng {
    val latitudeSpan = 0.05
    val longitudeSpan = latitudeSpan * size.width / size.height.coerceAtLeast(1).toDouble()
    val longitude = center.longitude + ((offset.x - size.width / 2f) / size.width) * longitudeSpan
    val latitude = center.latitude - ((offset.y - size.height / 2f) / size.height) * latitudeSpan
    return LatLng(latitude, longitude)
}

private fun LatLng.coordinateLabel(): String =
    String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
