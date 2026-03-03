package com.alertspot.viewmodel

import android.Manifest
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alertspot.AlertSpotApp
import com.alertspot.manager.PreferencesManager
import com.alertspot.model.AlertHistoryEntry
import com.alertspot.model.GeofenceLocation
import com.alertspot.model.SearchResult
import com.alertspot.service.GeofenceBroadcastReceiver
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

class AlertViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AlertViewModel"
    }

    // MARK: - Published State

    private val _locations = MutableStateFlow<List<GeofenceLocation>>(emptyList())
    val locations: StateFlow<List<GeofenceLocation>> = _locations.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _alertHistory = MutableStateFlow<List<AlertHistoryEntry>>(emptyList())
    val alertHistory: StateFlow<List<AlertHistoryEntry>> = _alertHistory.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    val isAlarmPlaying: StateFlow<Boolean> =
        (application as AlertSpotApp).isAlarmPlaying.asStateFlow()

    val alarmLocationName: StateFlow<String?> =
        (application as AlertSpotApp).alarmLocationName.asStateFlow()

    // MARK: - Private

    private val preferencesManager = PreferencesManager(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private val geofencingClient = LocationServices.getGeofencingClient(application)
    private val app = application as AlertSpotApp
    private val triggeredAlerts = mutableSetOf<String>()
    private var currentProximityTier = ProximityTier.FAR
    private var geofenceCheckJob: Job? = null
    private var searchJob: Job? = null
    private var locationCallback: LocationCallback? = null
    private var isMonitoring = false

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(getApplication(), GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            getApplication(), 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    // MARK: - Proximity Tiers (mirrors iOS)

    enum class ProximityTier(
        val priority: Int,
        val checkIntervalMs: Long,
        val distanceFilterMeters: Float
    ) {
        CLOSE(Priority.PRIORITY_HIGH_ACCURACY, 5_000L, 10f),
        MEDIUM(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15_000L, 100f),
        FAR(Priority.PRIORITY_LOW_POWER, 60_000L, 500f)
    }

    // MARK: - Init

    init {
        _locations.value = preferencesManager.loadLocations()
        _alertHistory.value = preferencesManager.loadHistory()
        _isDarkMode.value = preferencesManager.isDarkMode()
        startMonitoring()
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }

    // MARK: - Monitoring

    fun startMonitoring() {
        if (!hasLocationPermission()) {
            Log.d(TAG, "⚠️ Location permission not granted — monitoring deferred")
            return
        }

        if (isMonitoring) {
            Log.d(TAG, "ℹ️ Already monitoring — refreshing geofences")
            refreshMonitoredGeofences()
            return
        }

        isMonitoring = true

        Log.d(TAG, "▶️ Starting location monitoring (tier: $currentProximityTier)")

        // Seed location immediately from last known position
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(TAG, "📍 Last known location: ${location.latitude}, ${location.longitude}")
                    _currentLocation.value = location
                    // Immediately check geofences with this location
                    checkGeofenceBoundaries()
                } else {
                    Log.d(TAG, "⚠️ No last known location available")
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting last location", e)
        }

        // Request continuous location updates — start with high accuracy
        // to get a fast initial fix, then adapt via proximity tiers
        val request = LocationRequest.Builder(5_000L)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMinUpdateDistanceMeters(0f) // get every update initially
            .setMaxUpdateDelayMillis(10_000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    Log.d(TAG, "📍 Location update: ${location.latitude}, ${location.longitude} (accuracy: ${location.accuracy}m)")
                    _currentLocation.value = location
                    // Check geofences on every location update
                    checkGeofenceBoundaries()
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                request, locationCallback!!, Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission missing", e)
        }

        refreshMonitoredGeofences()
        startGeofenceCheckLoop()
    }

    fun stopMonitoring() {
        isMonitoring = false
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
        geofenceCheckJob?.cancel()
        geofenceCheckJob = null
        removeAllGeofences()
    }

    private fun startGeofenceCheckLoop() {
        geofenceCheckJob?.cancel()
        geofenceCheckJob = viewModelScope.launch {
            while (isActive) {
                delay(currentProximityTier.checkIntervalMs)
                checkGeofenceBoundaries()
            }
        }
    }

    private fun checkGeofenceBoundaries() {
        val userLocation = _currentLocation.value
        if (userLocation == null) {
            Log.d(TAG, "⚠️ checkGeofenceBoundaries: no current location yet")
            return
        }
        var nearestDistance = Double.MAX_VALUE

        for (location in _locations.value) {
            if (!location.isEnabled) continue

            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                location.latitude, location.longitude,
                results
            )
            val distance = results[0].toDouble()

            Log.d(TAG, "📏 ${location.name}: distance=${distance.toInt()}m, radius=${location.radius.toInt()}m, triggered=${location.id in triggeredAlerts}")

            val distanceToEdge = distance - location.radius
            if (distanceToEdge < nearestDistance) {
                nearestDistance = distanceToEdge
            }

            // User entered geofence
            if (distance <= location.radius && location.id !in triggeredAlerts) {
                Log.d(TAG, "🔔 Entered geofence: ${location.name} (distance: ${distance.toInt()}m, radius: ${location.radius.toInt()}m)")
                triggeredAlerts.add(location.id)
                triggerAlarm(location)
            }
            // User exited geofence — reset
            else if (distance > location.radius) {
                triggeredAlerts.remove(location.id)
            }
        }

        updateProximityTier(nearestDistance)
    }

    private fun updateProximityTier(nearestDistance: Double) {
        val newTier = when {
            nearestDistance < 1_000 -> ProximityTier.CLOSE
            nearestDistance < 5_000 -> ProximityTier.MEDIUM
            else -> ProximityTier.FAR
        }

        if (newTier == currentProximityTier) return

        val oldTier = currentProximityTier
        currentProximityTier = newTier

        // Restart location updates with new accuracy
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        val request = LocationRequest.Builder(newTier.checkIntervalMs)
            .setPriority(newTier.priority)
            .setMinUpdateDistanceMeters(newTier.distanceFilterMeters)
            .build()

        try {
            locationCallback?.let {
                fusedLocationClient.requestLocationUpdates(request, it, Looper.getMainLooper())
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission missing", e)
        }

        // Restart geofence check loop
        startGeofenceCheckLoop()

        Log.d(TAG, "📡 Proximity tier: $oldTier → $newTier")
    }

    // MARK: - Geofence Registration

    private fun refreshMonitoredGeofences() {
        removeAllGeofences()

        val enabledLocations = _locations.value.filter { it.isEnabled }
        if (enabledLocations.isEmpty()) {
            Log.d(TAG, "No enabled locations — skipping geofence registration")
            return
        }

        val geofences = enabledLocations.map { location ->
            Geofence.Builder()
                .setRequestId(location.id)
                .setCircularRegion(location.latitude, location.longitude, location.radius.toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setLoiteringDelay(0)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
            .addGeofences(geofences)
            .build()

        try {
            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener { Log.d(TAG, "✅ Registered ${geofences.size} geofences") }
                .addOnFailureListener { e -> Log.e(TAG, "❌ Failed to register geofences: ${e.message}", e) }
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission missing for geofencing", e)
        }
    }

    private fun removeAllGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent)
    }

    // MARK: - Alarm

    private fun triggerAlarm(location: GeofenceLocation) {
        recordHistory(location)

        // Auto-disable the alert so it fires only once
        val disabled = location.copy(isEnabled = false)
        val list = _locations.value.toMutableList()
        val idx = list.indexOfFirst { it.id == location.id }
        if (idx != -1) {
            list[idx] = disabled
            _locations.value = list
            preferencesManager.saveLocations(list)
            refreshMonitoredGeofences()
        }

        app.alarmLocationName.value = location.name
        app.alarmHandler.sendNotification(location.name, location.notificationMessage)
        app.alarmHandler.playAlarm()
        app.alarmHandler.triggerVibration()
    }

    fun playAlarmSound() {
        app.alarmHandler.playAlarm()
    }

    fun stopAlarm() {
        app.alarmHandler.stopAlarm()
    }

    // MARK: - CRUD

    fun addLocation(location: GeofenceLocation) {
        val updated = _locations.value + location
        _locations.value = updated
        preferencesManager.saveLocations(updated)

        if (location.isEnabled) {
            refreshMonitoredGeofences()

            // Check if already inside geofence
            _currentLocation.value?.let { userLocation ->
                val results = FloatArray(1)
                Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    location.latitude, location.longitude,
                    results
                )
                if (results[0] <= location.radius) {
                    triggeredAlerts.add(location.id)
                    triggerAlarm(location)
                }
            }
        }
    }

    fun deleteLocation(location: GeofenceLocation) {
        triggeredAlerts.remove(location.id)
        val updated = _locations.value.filter { it.id != location.id }
        _locations.value = updated
        preferencesManager.saveLocations(updated)
        refreshMonitoredGeofences()
    }

    fun deleteLocations(indices: Set<Int>) {
        val current = _locations.value.toMutableList()
        indices.sortedDescending().forEach { index ->
            if (index in current.indices) {
                triggeredAlerts.remove(current[index].id)
                current.removeAt(index)
            }
        }
        _locations.value = current
        preferencesManager.saveLocations(current)
        refreshMonitoredGeofences()
    }

    fun updateLocation(updated: GeofenceLocation) {
        val list = _locations.value.toMutableList()
        val index = list.indexOfFirst { it.id == updated.id }
        if (index == -1) return

        triggeredAlerts.remove(updated.id)
        list[index] = updated
        _locations.value = list
        preferencesManager.saveLocations(list)
        refreshMonitoredGeofences()
    }

    fun toggleLocation(location: GeofenceLocation) {
        val toggled = location.copy(isEnabled = !location.isEnabled)
        updateLocation(toggled)

        if (toggled.isEnabled) {
            triggeredAlerts.remove(toggled.id)
            // Check if already inside
            _currentLocation.value?.let { userLocation ->
                val results = FloatArray(1)
                Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    toggled.latitude, toggled.longitude,
                    results
                )
                if (results[0] <= toggled.radius) {
                    triggeredAlerts.add(toggled.id)
                    triggerAlarm(toggled)
                }
            }
        }
    }

    // MARK: - Alert History

    private fun recordHistory(location: GeofenceLocation) {
        val entry = AlertHistoryEntry(
            locationName = location.name,
            latitude = location.latitude,
            longitude = location.longitude,
            radius = location.radius
        )
        val updated = listOf(entry) + _alertHistory.value
        _alertHistory.value = updated
        preferencesManager.saveHistory(updated)
    }

    fun clearHistory() {
        _alertHistory.value = emptyList()
        preferencesManager.saveHistory(emptyList())
    }

    fun deleteHistoryEntry(entry: AlertHistoryEntry) {
        val updated = _alertHistory.value.filter { it.id != entry.id }
        _alertHistory.value = updated
        preferencesManager.saveHistory(updated)
    }

    // MARK: - Distance & ETA

    fun distanceTo(location: GeofenceLocation): Float? {
        val current = _currentLocation.value ?: return null
        val results = FloatArray(1)
        Location.distanceBetween(
            current.latitude, current.longitude,
            location.latitude, location.longitude,
            results
        )
        return results[0]
    }

    fun formattedDistance(location: GeofenceLocation): String? {
        val distance = distanceTo(location) ?: return null
        return if (distance >= 1000) {
            String.format("%.1f km", distance / 1000)
        } else {
            "${distance.toInt()} m"
        }
    }

    /** ETA estimated from straight-line distance at ~40 km/h average. */
    fun formattedETA(location: GeofenceLocation): String? {
        val distance = distanceTo(location) ?: return null
        val etaSeconds = distance / 11.1f  // 40 km/h ≈ 11.1 m/s
        val minutes = (etaSeconds / 60).toInt()
        return when {
            minutes < 1 -> "< 1 min"
            minutes < 60 -> "~$minutes min"
            else -> {
                val hours = minutes / 60
                val remainingMins = minutes % 60
                if (remainingMins == 0) "~$hours hr" else "~$hours hr $remainingMins min"
            }
        }
    }

    // MARK: - Location Search (Photon autocomplete + Nominatim fallback)

    fun search(query: String) {
        searchJob?.cancel()
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(150)  // short debounce for fast type-ahead feel

            val photonResults = mutableListOf<SearchResult>()
            val seen = mutableSetOf<String>()

            // ── Phase 1: Photon (instant autocomplete) — show results ASAP ──
            withContext(Dispatchers.IO) {
                try {
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    val loc = _currentLocation.value
                    val latParam = loc?.latitude ?: 12.97
                    val lonParam = loc?.longitude ?: 77.59
                    val photonUrl = "https://photon.komoot.io/api/?q=$encoded&limit=8&lat=$latParam&lon=$lonParam&lang=en"
                    val photonJson = URL(photonUrl).readText()
                    val root = JSONObject(photonJson)
                    val features = root.getJSONArray("features")
                    for (i in 0 until features.length()) {
                        val feature = features.getJSONObject(i)
                        val geometry = feature.getJSONObject("geometry")
                        val coords = geometry.getJSONArray("coordinates")
                        val lng = coords.getDouble(0)
                        val lat = coords.getDouble(1)
                        val props = feature.getJSONObject("properties")
                        val name = props.optString("name", "")
                        val city = props.optString("city", "")
                        val state = props.optString("state", "")
                        val country = props.optString("country", "")
                        val title = name.ifBlank {
                            listOf(city, state, country).filter { it.isNotBlank() }.joinToString(", ")
                        }
                        val subtitle = listOf(city, state, country)
                            .filter { it.isNotBlank() && it != title }
                            .joinToString(", ")
                        if (title.isNotBlank() && seen.add(title.lowercase())) {
                            photonResults.add(SearchResult(title, subtitle, lat, lng))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Photon search failed", e)
                }
            }

            // Show Photon results immediately (don't wait for Nominatim)
            if (photonResults.isNotEmpty()) {
                _searchResults.value = photonResults.take(10)
            }

            // ── Phase 2: Nominatim (better India POI coverage) — merge in ──
            withContext(Dispatchers.IO) {
                try {
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    val loc = _currentLocation.value
                    val nominatimUrl = buildString {
                        append("https://nominatim.openstreetmap.org/search?q=$encoded")
                        append("&format=json&addressdetails=1&limit=6")
                        append("&countrycodes=in")
                        if (loc != null) {
                            val lat = loc.latitude
                            val lon = loc.longitude
                            append("&viewbox=${lon - 2},${lat + 2},${lon + 2},${lat - 2}")
                            append("&bounded=0")
                        }
                        append("&accept-language=en")
                    }
                    val conn = URL(nominatimUrl).openConnection() as java.net.HttpURLConnection
                    conn.setRequestProperty("User-Agent", getApplication<Application>().packageName)
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    val json = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()

                    val arr = org.json.JSONArray(json)
                    val merged = photonResults.toMutableList()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val lat = obj.getDouble("lat")
                        val lng = obj.getDouble("lon")
                        val displayName = obj.getString("display_name")
                        val addr = obj.optJSONObject("address")
                        val name = obj.optString("name", "").ifBlank {
                            addr?.let {
                                it.optString("amenity", "")
                                    .ifBlank { it.optString("road", "") }
                                    .ifBlank { it.optString("village", "") }
                                    .ifBlank { it.optString("town", "") }
                                    .ifBlank { it.optString("city", "") }
                            } ?: ""
                        }
                        val city = addr?.optString("city", "")
                            ?: addr?.optString("town", "")
                            ?: addr?.optString("village", "")
                            ?: ""
                        val state = addr?.optString("state", "") ?: ""
                        val title = name.ifBlank {
                            displayName.split(",").firstOrNull()?.trim() ?: displayName
                        }
                        val subtitle = listOf(city, state)
                            .filter { it.isNotBlank() && it != title }
                            .joinToString(", ")
                        if (title.isNotBlank() && seen.add(title.lowercase())) {
                            merged.add(SearchResult(title, subtitle, lat, lng))
                        }
                    }
                    _searchResults.value = merged.take(12)
                } catch (e: Exception) {
                    Log.e(TAG, "Nominatim search failed", e)
                }
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    /** Reverse geocode a coordinate using Nominatim (OpenStreetMap). */
    suspend fun reverseGeocode(lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lng&format=json&zoom=18&addressdetails=1"
            val connection = java.net.HttpURLConnection::class.java.cast(
                URL(url).openConnection()
            )!!.apply {
                setRequestProperty("User-Agent", "AlertSpot-Android/1.0")
            }
            val json = connection.inputStream.bufferedReader().readText()
            val root = JSONObject(json)
            val displayName = root.optString("display_name", "")
            val address = root.optJSONObject("address")
            // Try to get a short, meaningful name
            val name = address?.optString("amenity", "")
                ?.ifBlank { address.optString("building", "") }
                ?.ifBlank { address.optString("road", "") }
                ?.ifBlank { address.optString("suburb", "") }
                ?.ifBlank { address.optString("city", "") }
                ?.ifBlank { null }
            name ?: displayName.split(",").firstOrNull()?.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Reverse geocoding failed", e)
            null
        }
    }

    // MARK: - Dark Mode

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        preferencesManager.setDarkMode(enabled)
    }

    // MARK: - Helpers

    private fun hasLocationPermission(): Boolean {
        val ctx: Application = getApplication()
        return ActivityCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    val activeAlertCount: Int
        get() = _locations.value.count { it.isEnabled }
}
