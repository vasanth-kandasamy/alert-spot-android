package com.alertspot.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alertspot.model.AlertHistoryEntry
import com.alertspot.ui.component.HistoryRow
import com.alertspot.viewmodel.AlertViewModel
import java.text.SimpleDateFormat
import java.util.*

/** A named group of history entries for a particular date section. */
private data class HistorySection(
    val title: String,
    val entries: List<AlertHistoryEntry>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: AlertViewModel,
    onBack: () -> Unit
) {
    val history by viewModel.alertHistory.collectAsState()
    var showClearConfirmation by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<AlertHistoryEntry?>(null) }

    val sections = remember(history) { buildSections(history) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alert History") },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirmation = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (history.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No History",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Triggered alerts will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                sections.forEach { section ->
                    item {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                    items(section.entries, key = { it.id }) { entry ->
                        HistoryRow(
                            entry = entry,
                            onDelete = { entryToDelete = entry }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }

    // Clear All confirmation dialog
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear All History?") },
            text = { Text("This will permanently delete all alert history. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Single entry delete confirmation
    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete Entry?") },
            text = { Text("Delete the alert history for \"${entry.locationName}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHistoryEntry(entry)
                        entryToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun buildSections(history: List<AlertHistoryEntry>): List<HistorySection> {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val calendar = Calendar.getInstance()
    val today = Calendar.getInstance()

    val dict = linkedMapOf<String, MutableList<AlertHistoryEntry>>()

    for (entry in history) {
        calendar.timeInMillis = entry.triggeredAt
        val key = when {
            isSameDay(calendar, today) -> "Today"
            isYesterday(calendar, today) -> "Yesterday"
            else -> dateFormat.format(Date(entry.triggeredAt))
        }
        dict.getOrPut(key) { mutableListOf() }.add(entry)
    }

    return dict.map { (title, entries) -> HistorySection(title, entries) }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean =
    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)

private fun isYesterday(cal: Calendar, today: Calendar): Boolean {
    val yesterday = today.clone() as Calendar
    yesterday.add(Calendar.DAY_OF_YEAR, -1)
    return isSameDay(cal, yesterday)
}
