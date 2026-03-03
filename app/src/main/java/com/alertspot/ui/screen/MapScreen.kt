package com.alertspot.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    // Increment this to force map re-center (even to same coords)
    var centerTrigger by remember { mutableIntStateOf(0) }
    var hasCenteredOnUser by remember { mutableStateOf(false) }
    LaunchedEffect(currentLocation) {
        if (!hasCenteredOnUser && currentLocation != null) {
            hasCenteredOnUser = true
            mapCenter = GeoPoint(currentLocation!!.latitude, currentLocation!!.longitude)
            centerTrigger++
        }
    }

    val markers = remember(locations) {
        locations.map { location ->
            OsmMarker(
                position = GeoPoint(location.latitude, location.longitude),
                title = location.name,
                snippet = viewModel.formattedDistance(location) ?: "",
                tintColor = if (location.isEnabled) android.graphics.Color.rgb(52, 199, 89) // Green
                            else android.graphics.Color.rgb(142, 142, 147) // Grey
            )
        }
    }

    // Current user location as GeoPoint for blue dot
    val userGeoPoint = remember(currentLocation) {
        currentLocation?.let { GeoPoint(it.latitude, it.longitude) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMapView(
            modifier = Modifier.fillMaxSize(),
            center = mapCenter,
            zoom = 14.0,
            markers = markers,
            gesturesEnabled = true,
            userLocation = userGeoPoint,
            animateKey = centerTrigger
        )

        // Button column (bottom-end)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // My Location button
            SmallFloatingActionButton(
                onClick = {
                    currentLocation?.let {
                        mapCenter = GeoPoint(it.latitude, it.longitude)
                        centerTrigger++
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Blue,
                shape = CircleShape
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location")
            }

            // Add Alert button
            FloatingActionButton(
                onClick = onAddAlert,
                containerColor = Blue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alert")
            }
        }
    }
}
