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
import com.alertspot.ui.component.OsmCircle
import com.alertspot.ui.component.OsmMapView
import com.alertspot.ui.component.OsmMarker
import com.alertspot.ui.theme.Blue
import com.alertspot.viewmodel.AlertViewModel
import org.osmdroid.util.GeoPoint

@Composable
fun MapScreen(
    viewModel: AlertViewModel,
    onAddAlert: () -> Unit
) {
    val locations by viewModel.locations.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()

    val defaultPosition = GeoPoint(20.5937, 78.9629)
    val startPosition = currentLocation?.let {
        GeoPoint(it.latitude, it.longitude)
    } ?: defaultPosition

    var mapCenter by remember { mutableStateOf(startPosition) }
    var hasCenteredOnUser by remember { mutableStateOf(false) }
    LaunchedEffect(currentLocation) {
        if (!hasCenteredOnUser && currentLocation != null) {
            hasCenteredOnUser = true
            mapCenter = GeoPoint(currentLocation!!.latitude, currentLocation!!.longitude)
        }
    }

    val markers = remember(locations) {
        locations.filter { it.isEnabled }.map { location ->
            OsmMarker(
                position = GeoPoint(location.latitude, location.longitude),
                title = location.name,
                snippet = viewModel.formattedDistance(location) ?: ""
            )
        }
    }

    val circles = remember(locations) {
        locations.filter { it.isEnabled }.map { location ->
            OsmCircle(
                center = GeoPoint(location.latitude, location.longitude),
                radiusMeters = location.radius,
                fillColor = Blue.copy(alpha = 0.10f),
                strokeColor = Blue.copy(alpha = 0.35f),
                strokeWidth = 3f
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMapView(
            modifier = Modifier.fillMaxSize(),
            center = mapCenter,
            zoom = 14.0,
            markers = markers,
            circles = circles,
            gesturesEnabled = true
        )

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
