package com.alertspot.ui.screen

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alertspot.viewmodel.AlertViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AlertViewModel,
    onNavigateToHistory: () -> Unit
) {
    val locations by viewModel.locations.collectAsState()
    val alertHistory by viewModel.alertHistory.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isAlarmPlaying by viewModel.isAlarmPlaying.collectAsState()
    val context = LocalContext.current

    val activeCount = locations.count { it.isEnabled }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // SECTION: Appearance
            SettingsSectionHeader("Appearance")
            ListItem(
                headlineContent = { Text("Dark Mode") },
                leadingContent = {
                    Icon(
                        if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) }
                    )
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // SECTION: Permissions
            SettingsSectionHeader("Permissions")
            ListItem(
                headlineContent = { Text("Location") },
                trailingContent = {
                    Text(
                        "Granted",
                        color = Color(0xFF34C759),
                        fontWeight = FontWeight.Medium
                    )
                }
            )
            ListItem(
                headlineContent = { Text("Open App Settings") },
                leadingContent = {
                    Icon(Icons.Default.Settings, contentDescription = null)
                },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Open")
                },
                modifier = Modifier.clickable {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // SECTION: Monitoring
            SettingsSectionHeader("Monitoring")
            ListItem(
                headlineContent = { Text("Active Alerts") },
                trailingContent = {
                    Text("$activeCount", fontWeight = FontWeight.Medium)
                }
            )
            ListItem(
                headlineContent = { Text("Total Alerts") },
                trailingContent = {
                    Text("${locations.size}", fontWeight = FontWeight.Medium)
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // SECTION: History
            SettingsSectionHeader("History")
            ListItem(
                headlineContent = { Text("Alert History") },
                leadingContent = {
                    Icon(Icons.Default.History, contentDescription = null)
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (alertHistory.isNotEmpty()) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Text("${alertHistory.size}")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                    }
                },
                modifier = Modifier.clickable { onNavigateToHistory() }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // SECTION: Debug
            SettingsSectionHeader("Debug")
            ListItem(
                headlineContent = {
                    TextButton(onClick = { viewModel.playAlarmSound() }) {
                        Text("Test Alarm")
                    }
                }
            )
            if (isAlarmPlaying) {
                ListItem(
                    headlineContent = {
                        TextButton(
                            onClick = { viewModel.stopAlarm() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Stop Alarm")
                        }
                    }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // SECTION: About
            SettingsSectionHeader("About")
            ListItem(
                headlineContent = { Text("Version") },
                trailingContent = {
                    Text("1.0.0", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
