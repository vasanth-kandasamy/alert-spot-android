package com.alertspot.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.alertspot.model.GeofenceLocation
import com.alertspot.ui.theme.Blue
import com.alertspot.ui.theme.Green
import com.alertspot.ui.theme.Red
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AlertRow(
    location: GeofenceLocation,
    distance: String?,
    eta: String?,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val offsetX = remember { Animatable(0f) }
    val rowShape = RoundedCornerShape(26.dp)

    // Half-open position = just enough to reveal the round button
    val halfThresholdPx = with(density) { 78.dp.toPx() }
    // Minimum drag distance to trigger snap-open (below this, snaps back)
    val snapThresholdPx = with(density) { 30.dp.toPx() }

    // Track whether the row is currently sitting at the half-open position
    val currentOffset = offsetX.value
    val isHalfOpenLeft = abs(currentOffset + halfThresholdPx) < 4f
    val isHalfOpenRight = abs(currentOffset - halfThresholdPx) < 4f

    // Button visibility
    val showDeleteButton = currentOffset < -10f
    val showEditButton = currentOffset > 10f

    // Button scale (pop-in)
    val deleteScale by animateFloatAsState(
        targetValue = if (showDeleteButton) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "deleteScale"
    )
    val editScale by animateFloatAsState(
        targetValue = if (showEditButton) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "editScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        // Background: round action buttons
        Box(modifier = Modifier.matchParentSize()) {
            val actionSize = 52.dp
            val actionSidePadding = 14.dp

            // Delete button (right side, revealed on left swipe)
            if (deleteScale > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = actionSidePadding)
                        .scale(deleteScale)
                        .size(actionSize)
                        .clip(CircleShape)
                        .background(Red),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Edit button (left side, revealed on right swipe)
            if (editScale > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = actionSidePadding)
                        .scale(editScale)
                        .size(actionSize)
                        .clip(CircleShape)
                        .background(Blue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Tap overlay — only active when row is half-open
        if (isHalfOpenLeft || isHalfOpenRight) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(isHalfOpenLeft, isHalfOpenRight) {
                        detectTapGestures { offset ->
                            if (isHalfOpenLeft) {
                                val buttonRight = size.width.toFloat()
                                val buttonLeft = buttonRight - with(density) { 80.dp.toPx() }
                                if (offset.x >= buttonLeft) {
                                    // Tapped the delete button
                                    showDeleteConfirm = true
                                } else {
                                    // Tapped elsewhere → close
                                }
                                coroutineScope.launch {
                                    offsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 500f))
                                }
                            } else if (isHalfOpenRight) {
                                val buttonRight = with(density) { 80.dp.toPx() }
                                if (offset.x <= buttonRight) {
                                    // Tapped the edit button
                                    onEdit()
                                }
                                coroutineScope.launch {
                                    offsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 500f))
                                }
                            }
                        }
                    }
            )
        }

        // Foreground card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(x = currentOffset.roundToInt(), y = 0) }
                .pointerInput(Unit) {
                    var startedHalfOpenLeft = false
                    var startedHalfOpenRight = false
                    detectHorizontalDragGestures(
                        onDragStart = {
                            val pos = offsetX.value
                            startedHalfOpenLeft = abs(pos + halfThresholdPx) < 8f
                            startedHalfOpenRight = abs(pos - halfThresholdPx) < 8f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount)
                            }
                        },
                        onDragEnd = {
                            val current = offsetX.value
                            coroutineScope.launch {
                                when {
                                    // Was half-open left and user swiped further left → trigger delete
                                    startedHalfOpenLeft && current < -(halfThresholdPx + snapThresholdPx) -> {
                                        showDeleteConfirm = true
                                        offsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 500f))
                                    }
                                    // Was half-open right and user swiped further right → trigger edit
                                    startedHalfOpenRight && current > (halfThresholdPx + snapThresholdPx) -> {
                                        onEdit()
                                        offsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 500f))
                                    }
                                    // First swipe left past threshold → snap to half-open
                                    current < -snapThresholdPx -> {
                                        offsetX.animateTo(-halfThresholdPx, spring(dampingRatio = 1f, stiffness = 600f))
                                    }
                                    // First swipe right past threshold → snap to half-open
                                    current > snapThresholdPx -> {
                                        offsetX.animateTo(halfThresholdPx, spring(dampingRatio = 1f, stiffness = 600f))
                                    }
                                    // Didn't swipe enough → snap back
                                    else -> {
                                        offsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 500f))
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 500f))
                            }
                        }
                    )
                },
            shape = rowShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 17.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular icon indicator
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (location.isEnabled) Blue.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (location.isEnabled) Icons.Outlined.NotificationsActive
                            else Icons.Outlined.NotificationsOff,
                            contentDescription = null,
                            tint = if (location.isEnabled) Blue
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Text content
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = location.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = location.radiusLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        if (distance != null && location.isEnabled) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = Green
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = distance,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Green,
                                    fontWeight = FontWeight.Medium
                                )
                                if (eta != null) {
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = eta,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Toggle switch
                    Switch(
                        checked = location.isEnabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Green,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }
            }
        }

        // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Red,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("Delete Alert?", fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text(
                    "\"${location.name}\" will be permanently removed.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
