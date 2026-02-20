package com.alertspot

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alertspot.service.LocationForegroundService
import com.alertspot.ui.component.AlarmOverlay
import com.alertspot.ui.navigation.AppNavigation
import com.alertspot.ui.theme.AlertSpotTheme
import com.alertspot.viewmodel.AlertViewModel

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var alertViewModel: AlertViewModel? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        Log.d(TAG, "Location permission result: fineGranted=$fineGranted")
        if (fineGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            startLocationService()
            // Start monitoring now that we have permission
            alertViewModel?.startMonitoring()
        }
    }

    private val backgroundPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d(TAG, "Background location permission: granted=$granted")
        // Re-start monitoring with background permission for geofences
        if (granted) {
            alertViewModel?.startMonitoring()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* notification permission result */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermissions()

        setContent {
            val vm: AlertViewModel = viewModel()
            alertViewModel = vm

            val isDarkMode by vm.isDarkMode.collectAsState()
            val isAlarmPlaying by vm.isAlarmPlaying.collectAsState()
            val alarmLocationName by vm.alarmLocationName.collectAsState()

            // Ensure monitoring starts when Compose is ready
            // (handles case where permissions were already granted)
            LaunchedEffect(Unit) {
                Log.d(TAG, "LaunchedEffect: ensuring monitoring is started")
                vm.startMonitoring()
            }

            AlertSpotTheme(darkTheme = isDarkMode) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(vm)

                    if (isAlarmPlaying) {
                        AlarmOverlay(
                            locationName = alarmLocationName ?: "",
                            onStop = { vm.stopAlarm() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-start monitoring when app comes back to foreground
        alertViewModel?.startMonitoring()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        locationPermissionLauncher.launch(permissions.toTypedArray())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationForegroundService::class.java)
        startForegroundService(intent)
    }
}
