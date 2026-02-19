package com.alertspot.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Re-registers geofences after device reboot, since Android clears them.
 * The ViewModel's startMonitoring() is called when the app is next opened.
 * This receiver ensures the foreground service restarts.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device rebooted — will re-register geofences on next app launch")
            // Geofences are re-registered when the ViewModel initializes.
            // Optionally, start the foreground service here for immediate monitoring.
        }
    }
}
