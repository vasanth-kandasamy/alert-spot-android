package com.alertspot.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alertspot.ui.theme.Blue
import com.alertspot.viewmodel.AlertViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun MapScreen(
    viewModel: AlertViewModel,
    onAddAlert: () -> Unit
) {
    val locations by viewModel.locations.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()

    // Default: center of India, overridden by user location
    val defaultPosition = LatLng(20.5937, 78.9629)
    val startPosition = currentLocation?.let {
        LatLng(it.latitude, it.longitude)
    } ?: defaultPosition

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startPosition, 12f)
    }

    // Center on user location once available
    var hasCenteredOnUser by remember { mutableStateOf(false) }
    LaunchedEffect(currentLocation) {
        if (!hasCenteredOnUser && currentLocation != null) {
            hasCenteredOnUser = true
            val pos = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pos, 14f))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = true,
                compassEnabled = true,
                mapToolbarEnabled = false
            )
        ) {
            locations.filter { it.isEnabled }.forEach { location ->
                val position = LatLng(location.latitude, location.longitude)

                // Radius circle
                Circle(
                    center = position,
                    radius = location.radius,
                    fillColor = Blue.copy(alpha = 0.10f),
                    strokeColor = Blue.copy(alpha = 0.35f),
                    strokeWidth = 3f
                )

                // Pin marker
                val distance = viewModel.formattedDistance(location) ?: ""
                val snippet = if (distance.isNotEmpty()) "Distance: $distance" else ""

                MarkerInfoWindowContent(
                    state = MarkerState(position = position),
                    title = location.name,
                    snippet = snippet
                ) { marker ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = marker.title ?: "",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (distance.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = distance,
                                style = MaterialTheme.typography.bodySmall,
                                color = Blue,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Floating add button
        FloatingActionButton(
            onClick = onAddAlert,
            containerColor = Blue,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Alert")
        }
    }
}
