package com.alertspot

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.alertspot.manager.AlarmHandler
import kotlinx.coroutines.flow.MutableStateFlow

class AlertSpotApp : Application() {

    lateinit var alarmHandler: AlarmHandler
    val isAlarmPlaying = MutableStateFlow(false)
    val alarmLocationName = MutableStateFlow<String?>(null)

    override fun onCreate() {
        super.onCreate()
        instance = this
        alarmHandler = AlarmHandler(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Alarm channel — high importance for loud alerts
        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            getString(R.string.channel_alarm),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.channel_alarm_desc)
            enableVibration(true)
            setBypassDnd(true)
        }
        notificationManager.createNotificationChannel(alarmChannel)

        // Service channel — low importance for foreground service
        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            getString(R.string.channel_service),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_service_desc)
        }
        notificationManager.createNotificationChannel(serviceChannel)
    }

    companion object {
        lateinit var instance: AlertSpotApp
            private set

        const val CHANNEL_ALARM = "alarm_channel"
        const val CHANNEL_SERVICE = "service_channel"
    }
}
