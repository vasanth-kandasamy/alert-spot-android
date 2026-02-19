package com.alertspot.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alertspot.model.GeofenceLocation
import com.alertspot.ui.theme.Blue
import com.alertspot.viewmodel.AlertViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLocationScreen(
    viewModel: AlertViewModel,
    onDismiss: () -> Unit
) {
    val currentLocation by viewModel.currentLocation.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var searchText by remember { mutableStateOf("") }
    var selectedName by remember { mutableStateOf("") }
    var showSearchResults by remember { mutableStateOf(false) }
    var radius by remember { mutableFloatStateOf(2000f) }

    val defaultPosition = currentLocation?.let {
        LatLng(it.latitude, it.longitude)
    } ?: LatLng(20.5937, 78.9629)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 14f)
    }

    // Track map center as selected coordinate
    val selectedCoordinate by remember {
        derivedStateOf { cameraPositionState.position.target }
    }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Alert") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Map layer
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = true,
                    compassEnabled = true,
                    mapToolbarEnabled = false,
                    zoomControlsEnabled = false
                )
            ) {
                // Radius circle follows map center
                Circle(
                    center = selectedCoordinate,
                    radius = radius.toDouble(),
                    fillColor = Blue.copy(alpha = 0.12f),
                    strokeColor = Blue.copy(alpha = 0.4f),
                    strokeWidth = 3f
                )
            }

            // Center pin (fixed in screen center)
            Icon(
                Icons.Default.LocationOn,
                contentDescription = "Pin",
                tint = Color.Red,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
                    .offset(y = (-24).dp) // anchor at bottom of icon
            )

            // Overlays
            Column(modifier = Modifier.fillMaxSize()) {
                // Search bar
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { value ->
                        searchText = value
                        viewModel.search(value)
                        showSearchResults = value.isNotEmpty()
                    },
                    placeholder = { Text("Search for a place") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = {
                                searchText = ""
                                showSearchResults = false
                                viewModel.clearSearchResults()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                // Search results
                if (showSearchResults && searchResults.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        items(searchResults) { result ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedName = result.title
                                        searchText = result.title
                                        showSearchResults = false
                                        viewModel.clearSearchResults()
                                        scope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(result.latitude, result.longitude),
                                                    15f
                                                )
                                            )
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(result.title, style = MaterialTheme.typography.bodyMedium)
                                if (result.subtitle.isNotEmpty() && result.subtitle != result.title) {
                                    Text(
                                        result.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(start = 14.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Drag indicator
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(2.5.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Coordinates
                        Text(
                            text = String.format("%.5f, %.5f", selectedCoordinate.latitude, selectedCoordinate.longitude),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Radius slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Alert Radius", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = if (radius >= 1000) String.format("%.1f km", radius / 1000) else "${radius.toInt()} m",
                                style = MaterialTheme.typography.labelLarge,
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Save button
                        Button(
                            onClick = {
                                scope.launch {
                                    val coord = selectedCoordinate
                                    val geocodedName = viewModel.reverseGeocode(coord.latitude, coord.longitude)
                                    val name = geocodedName ?: selectedName.ifBlank { "Alert" }
                                    val alert = GeofenceLocation(
                                        name = name,
                                        latitude = coord.latitude,
                                        longitude = coord.longitude,
                                        radius = radius.toDouble(),
                                        isEnabled = true
                                    )
                                    viewModel.addLocation(alert)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue)
                        ) {
                            Text("Save Alert", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
