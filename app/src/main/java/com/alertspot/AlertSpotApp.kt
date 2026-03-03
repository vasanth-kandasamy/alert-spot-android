package com.alertspot

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.alertspot.manager.AlarmHandler
import kotlinx.coroutines.flow.MutableStateFlow
import org.osmdroid.config.Configuration
import java.io.File

class AlertSpotApp : Application() {

    lateinit var alarmHandler: AlarmHandler
    val isAlarmPlaying = MutableStateFlow(false)
    val alarmLocationName = MutableStateFlow<String?>(null)

    override fun onCreate() {
        super.onCreate()
        instance = this
        alarmHandler = AlarmHandler(this)
        createNotificationChannels()
        configureOsmdroid()
    }

    /** Pre-configure osmdroid tile engine for fast map loading. */
    private fun configureOsmdroid() {
        val config = Configuration.getInstance()
        config.userAgentValue = packageName

        // Use a dedicated tile cache directory
        val tileCache = File(cacheDir, "osmdroid/tiles")
        if (!tileCache.exists()) tileCache.mkdirs()
        config.osmdroidTileCache = tileCache

        // Maximize download concurrency
        config.tileDownloadThreads = 8
        config.tileFileSystemThreads = 6
        config.tileDownloadMaxQueueSize = 40
        config.tileFileSystemMaxQueueSize = 40

        // Large disk cache (600 MB) — tiles are tiny, this covers a wide area
        config.tileFileSystemCacheMaxBytes = 600L * 1024 * 1024
        config.tileFileSystemCacheTrimBytes = 500L * 1024 * 1024

        // In-memory tile cache — keep more tiles in RAM for instant re-render
        config.cacheMapTileCount = 36

        // Shorter expiry override so cached tiles are preferred
        config.expirationOverrideDuration = 30L * 24 * 60 * 60 * 1000 // 30 days

        // Disable the osmdroid default GPS overlay (we draw our own)
        config.isMapViewHardwareAccelerated = true
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Alarm channel — max importance for lock screen / heads-up alerts
        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            getString(R.string.channel_alarm),
            NotificationManager.IMPORTANCE_MAX
        ).apply {
            description = getString(R.string.channel_alarm_desc)
            enableVibration(true)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(alarmChannel)

        // Service channel — minimal importance so it stays hidden
        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            getString(R.string.channel_service),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = getString(R.string.channel_service_desc)
            setShowBadge(false)
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
