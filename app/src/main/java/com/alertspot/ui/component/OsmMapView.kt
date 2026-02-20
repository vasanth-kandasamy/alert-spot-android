package com.alertspot.ui.component

import android.graphics.Color as AndroidColor
import android.view.MotionEvent
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

/**
 * A reusable Compose wrapper around osmdroid's MapView.
 *
 * @param modifier         Compose modifier
 * @param center           Initial center of the map
 * @param zoom             Initial zoom level (0–20)
 * @param markers          List of markers to display
 * @param circles          List of radius circles to display
 * @param gesturesEnabled  Whether the user can scroll/zoom the map
 * @param onCenterChanged  Callback fired when the map center changes (after user drag)
 * @param myLocationEnabled Whether to show the blue GPS dot
 */
@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    center: GeoPoint = GeoPoint(20.5937, 78.9629),
    zoom: Double = 14.0,
    markers: List<OsmMarker> = emptyList(),
    circles: List<OsmCircle> = emptyList(),
    gesturesEnabled: Boolean = true,
    onCenterChanged: ((GeoPoint) -> Unit)? = null,
    myLocationEnabled: Boolean = false
) {
    val context = LocalContext.current

    // Ensure osmdroid config uses our user-agent
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    // Remember the MapView so it survives recomposition
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    // Update center/zoom when params change
    LaunchedEffect(center, zoom) {
        mapViewRef.value?.controller?.apply {
            setCenter(center)
            setZoom(zoom)
        }
    }

    // Update overlays when markers/circles change
    LaunchedEffect(markers, circles) {
        val mapView = mapViewRef.value ?: return@LaunchedEffect
        // Remove old marker/circle overlays, keep base tile overlay
        mapView.overlays.removeAll { it is Marker || it is Polygon }

        // Add circles
        circles.forEach { circle ->
            val polygon = Polygon(mapView).apply {
                points = Polygon.pointsAsCircle(circle.center, circle.radiusMeters)
                fillPaint.color = circle.fillColor.toArgb()
                outlinePaint.color = circle.strokeColor.toArgb()
                outlinePaint.strokeWidth = circle.strokeWidth
            }
            mapView.overlays.add(polygon)
        }

        // Add markers
        markers.forEach { m ->
            val marker = Marker(mapView).apply {
                position = m.position
                title = m.title
                snippet = m.snippet
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(marker)
        }

        mapView.invalidate()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setCenter(center)
                controller.setZoom(zoom)

                if (!gesturesEnabled) {
                    setOnTouchListener { _, _ -> true } // consume all touches
                }

                // Fire callback when user finishes dragging
                if (onCenterChanged != null) {
                    addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                        // Use a scroll listener via overriding touch events
                    }
                    setOnTouchListener { v, event ->
                        if (!gesturesEnabled) return@setOnTouchListener true
                        if (event.action == MotionEvent.ACTION_UP ||
                            event.action == MotionEvent.ACTION_CANCEL
                        ) {
                            val c = mapCenter as GeoPoint
                            onCenterChanged(c)
                        }
                        false // let map handle the touch too
                    }
                }

                mapViewRef.value = this
            }
        },
        update = { mapView ->
            mapView.controller.setCenter(center)
            mapView.controller.setZoom(zoom)
        }
    )
}

data class OsmMarker(
    val position: GeoPoint,
    val title: String = "",
    val snippet: String = ""
)

data class OsmCircle(
    val center: GeoPoint,
    val radiusMeters: Double,
    val fillColor: Color = Color(0x1A007AFF),
    val strokeColor: Color = Color(0x66007AFF),
    val strokeWidth: Float = 3f
)
