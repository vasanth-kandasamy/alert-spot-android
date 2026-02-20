package com.alertspot.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alertspot.ui.component.AlertRow
import com.alertspot.ui.theme.Blue
import com.alertspot.viewmodel.AlertViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertListScreen(
    viewModel: AlertViewModel,
    onAddAlert: () -> Unit,
    onEditAlert: (String) -> Unit
) {
    val locations by viewModel.locations.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Alerts", fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAlert,
                shape = CircleShape,
                containerColor = Blue,
                contentColor = androidx.compose.ui.graphics.Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alert")
            }
        }
    ) { padding ->
        if (locations.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Icon(
                        Icons.Outlined.NotificationsOff,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "No Alerts Yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Add your first location alert and get notified when you arrive.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp)
            ) {
                // Swipe hint
                item(key = "__hint__") {
                    Text(
                        text = "Swipe left to delete · Swipe right to edit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }

                items(
                    items = locations,
                    key = { it.id }
                ) { location ->
                    AlertRow(
                        location = location,
                        distance = if (location.isEnabled) viewModel.formattedDistance(location) else null,
                        eta = if (location.isEnabled) viewModel.formattedETA(location) else null,
                        onToggle = { viewModel.toggleLocation(location) },
                        onEdit = { onEditAlert(location.id) },
                        onDelete = { viewModel.deleteLocation(location) }
                    )
                }
            }
        }
    }
}
