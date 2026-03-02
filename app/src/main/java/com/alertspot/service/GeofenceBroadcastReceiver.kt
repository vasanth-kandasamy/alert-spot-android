package com.alertspot.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.alertspot.AlertSpotApp
import com.alertspot.manager.PreferencesManager
import com.alertspot.model.AlertHistoryEntry
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent)
        if (event == null || event.hasError()) {
            Log.e(TAG, "Geofencing event error: ${event?.errorCode}")
            return
        }

        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val app = context.applicationContext as? AlertSpotApp ?: return
        val preferencesManager = PreferencesManager(context)
        val locations = preferencesManager.loadLocations()

        for (geofence in event.triggeringGeofences.orEmpty()) {
            val location = locations.find { it.id == geofence.requestId } ?: continue

            Log.d(TAG, "📍 Entered geofence: ${location.name}")

            // Auto-disable this alert so it doesn't trigger again
            val updatedLocations = locations.map {
                if (it.id == location.id) it.copy(isEnabled = false) else it
            }
            preferencesManager.saveLocations(updatedLocations)

            // Record history
            val history = preferencesManager.loadHistory().toMutableList()
            history.add(
                0,
                AlertHistoryEntry(
                    locationName = location.name,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    radius = location.radius
                )
            )
            preferencesManager.saveHistory(history)

            // Set alarm state
            app.alarmLocationName.value = location.name
            app.isAlarmPlaying.value = true

            // Play alarm + notification
            app.alarmHandler.sendNotification(location.name, location.notificationMessage)
            app.alarmHandler.playAlarm()
            app.alarmHandler.triggerVibration()
        }
    }
}
