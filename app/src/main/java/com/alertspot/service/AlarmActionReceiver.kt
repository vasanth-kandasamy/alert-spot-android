package com.alertspot.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.alertspot.AlertSpotApp

/**
 * Handles notification actions (e.g., "Stop Alarm") so the user
 * can dismiss the alarm directly from the lock screen or notification shade.
 */
class AlarmActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_STOP_ALARM = "com.alertspot.ACTION_STOP_ALARM"
        private const val TAG = "AlarmActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_STOP_ALARM) {
            Log.d(TAG, "Stop alarm action received from notification")
            val app = context.applicationContext as? AlertSpotApp ?: return
            app.alarmHandler.stopAlarm()
        }
    }
}
