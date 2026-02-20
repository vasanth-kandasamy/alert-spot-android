package com.alertspot.ui.screen

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alertspot.ui.theme.Blue
import com.alertspot.ui.theme.Green
import com.alertspot.ui.theme.Orange
import com.alertspot.ui.theme.Red
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
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── GENERAL ──
            SectionLabel("GENERAL")
            IosCard {
                IosRow(
                    icon = if (isDarkMode) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                    iconBg = if (isDarkMode) Color(0xFF5856D6) else Orange,
                    title = "Dark Mode",
                    trailing = {
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.setDarkMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Green,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                )
            }

            // ── MONITORING ──
            SectionLabel("MONITORING")
            IosCard {
                IosRow(
                    icon = Icons.Outlined.NotificationsActive,
                    iconBg = Blue,
                    title = "Active Alerts",
                    trailing = {
                        IosDetailBadge("$activeCount", Green)
                    }
                )
                IosRowDivider()
                IosRow(
                    icon = Icons.Outlined.LocationOn,
                    iconBg = Color(0xFF5856D6),
                    title = "Total Alerts",
                    trailing = {
                        IosDetailBadge("${locations.size}", Blue)
                    }
                )
                IosRowDivider()
                IosRow(
                    icon = Icons.Outlined.History,
                    iconBg = Orange,
                    title = "Alert History",
                    onClick = onNavigateToHistory,
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (alertHistory.isNotEmpty()) {
                                Text(
                                    "${alertHistory.size}",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }

            // ── PERMISSIONS ──
            SectionLabel("PERMISSIONS")
            IosCard {
                IosRow(
                    icon = Icons.Outlined.MyLocation,
                    iconBg = Green,
                    title = "Location Access",
                    trailing = {
                        Text(
                            "Granted",
                            color = Green,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                )
                IosRowDivider()
                IosRow(
                    icon = Icons.Outlined.Settings,
                    iconBg = Color(0xFF8E8E93),
                    title = "App Settings",
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }

            // ── DEBUG ──
            SectionLabel("DEBUG")
            IosCard {
                IosRow(
                    icon = Icons.AutoMirrored.Outlined.VolumeUp,
                    iconBg = Red,
                    title = if (isAlarmPlaying) "Stop Alarm" else "Test Alarm",
                    onClick = {
                        if (isAlarmPlaying) viewModel.stopAlarm()
                        else viewModel.playAlarmSound()
                    },
                    trailing = {
                        if (isAlarmPlaying) {
                            Text(
                                "Playing",
                                color = Red,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                )
            }

            // ── ABOUT ──
            SectionLabel("ABOUT")
            IosCard {
                IosRow(
                    icon = Icons.Outlined.Info,
                    iconBg = Blue,
                    title = "Version",
                    trailing = {
                        Text(
                            "1.0.0",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/* ─── iOS-style building blocks ────────────────────────────────────────── */

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 0.5.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun IosCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun IosRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Coloured rounded-square icon — the iOS look
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        trailing()
    }
}

@Composable
private fun IosRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 60.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    )
}

@Composable
private fun IosDetailBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
