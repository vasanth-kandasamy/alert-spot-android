package com.alertspot.model

import java.util.UUID

data class AlertHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Double,
    val triggeredAt: Long = System.currentTimeMillis()  // epoch millis
) {
    /** Human-readable radius string (e.g. "2.0 km" or "500 m"). */
    val radiusLabel: String
        get() = if (radius >= 1000) {
            String.format("%.1f km", radius / 1000)
        } else {
            "${radius.toInt()} m"
        }
}
