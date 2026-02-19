package com.alertspot.manager

import android.content.Context
import android.content.SharedPreferences
import com.alertspot.model.AlertHistoryEntry
import com.alertspot.model.GeofenceLocation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("alert_spot_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // MARK: - Locations

    fun saveLocations(locations: List<GeofenceLocation>) {
        prefs.edit().putString(KEY_LOCATIONS, gson.toJson(locations)).apply()
    }

    fun loadLocations(): List<GeofenceLocation> {
        val json = prefs.getString(KEY_LOCATIONS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<GeofenceLocation>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // MARK: - Alert History

    fun saveHistory(history: List<AlertHistoryEntry>) {
        prefs.edit().putString(KEY_HISTORY, gson.toJson(history)).apply()
    }

    fun loadHistory(): List<AlertHistoryEntry> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<AlertHistoryEntry>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // MARK: - Dark Mode

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)

    companion object {
        private const val KEY_LOCATIONS = "saved_locations"
        private const val KEY_HISTORY = "alert_history"
        private const val KEY_DARK_MODE = "dark_mode"
    }
}
