package com.alertspot.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alertspot.ui.theme.Blue
import com.alertspot.viewmodel.AlertViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLocationScreen(
    viewModel: AlertViewModel,
    locationId: String,
    onDismiss: () -> Unit
) {
    val locations by viewModel.locations.collectAsState()
    val location = locations.find { it.id == locationId } ?: run {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    var name by remember { mutableStateOf(location.name) }
    var notificationMessage by remember { mutableStateOf(location.notificationMessage ?: "") }
    var radius by remember { mutableFloatStateOf(location.radius.toFloat()) }

    // Camera position adjusts to show radius
    val position = LatLng(location.latitude, location.longitude)
    val zoomForRadius = remember(radius) {
        // Approximate zoom level that shows the radius circle well
        val metersPerDp = radius * 2.5 // show ~2.5x radius on screen
        when {
            metersPerDp < 500   -> 16f
            metersPerDp < 1000  -> 15f
            metersPerDp < 2000  -> 14f
            metersPerDp < 5000  -> 13f
            metersPerDp < 10000 -> 12f
            metersPerDp < 25000 -> 11f
            else -> 10f
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        this.position = CameraPosition.fromLatLngZoom(position, zoomForRadius)
    }

    // Update camera when radius changes
    LaunchedEffect(zoomForRadius) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(position, zoomForRadius)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Alert") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val updated = location.copy(
                                name = name.trim(),
                                notificationMessage = notificationMessage.trim().ifEmpty { null },
                                radius = radius.toDouble()
                            )
                            viewModel.updateLocation(updated)
                            onDismiss()
                        },
                        enabled = name.trim().isNotEmpty()
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Map preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(),
                    uiSettings = MapUiSettings(
                        scrollGesturesEnabled = false,
                        zoomGesturesEnabled = false,
                        tiltGesturesEnabled = false,
                        rotationGesturesEnabled = false,
                        compassEnabled = false,
                        mapToolbarEnabled = false,
                        myLocationButtonEnabled = false,
                        zoomControlsEnabled = false
                    )
                ) {
                    Circle(
                        center = position,
                        radius = radius.toDouble(),
                        fillColor = Blue.copy(alpha = 0.12f),
                        strokeColor = Blue.copy(alpha = 0.4f),
                        strokeWidth = 3f
                    )
                    Marker(
                        state = MarkerState(position = position),
                        title = name
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name
            SectionHeader("Name")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Alert name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Notification message
            SectionHeader("Notification Message")
            OutlinedTextField(
                value = notificationMessage,
                onValueChange = { notificationMessage = it },
                placeholder = { Text("You have arrived at ${location.name}") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Radius
            SectionHeader("Alert Radius")
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Radius", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = if (radius >= 1000) String.format("%.1f km", radius / 1000) else "${radius.toInt()} m",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Blue
                    )
                }

                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 100f..10000f,
                    steps = 98,
                    colors = SliderDefaults.colors(thumbColor = Blue, activeTrackColor = Blue)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("100 m", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text("10 km", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Coordinates (read-only)
            SectionHeader("Location")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Coordinates", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text(
                    String.format("%.5f, %.5f", location.latitude, location.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}
