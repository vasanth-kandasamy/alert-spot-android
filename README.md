# AlertSpot (Android)

AlertSpot is an Android app that triggers loud alarms or notifications when you reach a user-configured GPS location, ideal for commuters, travelers, or anyone needing reliable arrival alerts.

## Features

- **Geofence Alerts** — Add, edit, delete, and toggle location-based alerts with configurable radius
- **Live Map** — Google Maps view with geofence circles, markers, and distance badges
- **Smart Monitoring** — Dynamic GPS accuracy and polling frequency based on proximity to nearest alert
- **Dual Detection** — Android Geofencing API + timer-based fallback distance checks
- **Alarm System** — Looping audio, vibration, high-priority notification, and full-screen alarm overlay
- **Location Search** — Geocoder-based location search for quick alert setup
- **Alert History** — Auto-logged when alarms fire, grouped by day, with delete and clear actions
- **Live Distance & ETA** — Shown on active alerts in the list and on map markers
- **Settings** — Dark mode, permission status, monitoring stats, test alarm, alert history, version info

## Setup

### Google Maps API Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Enable **Maps SDK for Android**
3. Create an API key
4. Create a `local.properties` file in the project root:
   ```
   MAPS_API_KEY=your_api_key_here
   ```

### Build

Open the project in Android Studio and run on a device or emulator.

**Minimum SDK:** 26 (Android 8.0)
**Target SDK:** 35

## Architecture

- **Kotlin + Jetpack Compose** — Modern declarative UI
- **MVVM** — ViewModel with StateFlow for reactive state
- **Google Play Services** — FusedLocationProvider + GeofencingClient
- **Material Design 3** — Consistent, modern UI components
