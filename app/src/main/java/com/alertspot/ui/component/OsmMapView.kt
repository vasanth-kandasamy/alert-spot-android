package com.alertspot.ui.component

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import android.graphics.Point
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
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon

/**
 * Custom overlay that draws a blue dot with white border and a translucent pulse
 * ring to indicate the user's current location.
 */
private class BlueDotOverlay(private var location: GeoPoint? = null) : Overlay() {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(0, 122, 255)
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(40, 0, 122, 255)
        style = Paint.Style.FILL
    }

    fun updateLocation(geoPoint: GeoPoint?) {
        location = geoPoint
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val loc = location ?: return
        val point = Point()
        mapView.projection.toPixels(loc, point)
        // Pulse ring
        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), 40f, pulsePaint)
        // White border
        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), 12f, borderPaint)
        // Blue fill
        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), 10f, fillPaint)
    }
}

/**
 * A reusable Compose wrapper around osmdroid's MapView.
 *
 * @param modifier         Compose modifier
 * @param center           Center of the map
 * @param zoom             Zoom level (0–20)
 * @param markers          List of markers to display
 * @param circles          List of radius circles to display
 * @param gesturesEnabled  Whether the user can scroll/zoom the map
 * @param onCenterChanged  Callback fired when the map center changes (after user drag)
 * @param userLocation     If set, draws a blue dot at this position
 * @param animateKey        Change this value to force the map to re-animate to [center]
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
    userLocation: GeoPoint? = null,
    animateKey: Int = 0
) {
    val context = LocalContext.current

    // Ensure osmdroid config uses our user-agent and increase tile cache
    LaunchedEffect(Unit) {
        val config = Configuration.getInstance()
        config.userAgentValue = context.packageName
        // Increase tile download threads for faster loading
        config.tileDownloadThreads = 6
        config.tileFileSystemThreads = 4
        // Increase cache sizes
        config.tileFileSystemCacheMaxBytes = 512L * 1024 * 1024 // 512 MB
        config.tileFileSystemCacheTrimBytes = 384L * 1024 * 1024 // trim at 384 MB
        config.cacheMapTileCount = 12
    }

    // Remember the MapView so it survives recomposition
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val blueDotOverlay = remember { BlueDotOverlay() }

    // Animate to center when center or animateKey changes
    LaunchedEffect(center, zoom, animateKey) {
        mapViewRef.value?.controller?.animateTo(center, zoom, 300L)
    }

    // Update blue dot when userLocation changes
    LaunchedEffect(userLocation) {
        blueDotOverlay.updateLocation(userLocation)
        mapViewRef.value?.invalidate()
    }

    // Update overlays when markers/circles change
    LaunchedEffect(markers, circles) {
        val mapView = mapViewRef.value ?: return@LaunchedEffect
        // Remove old marker/circle overlays, keep base tile overlay and blue dot
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
                zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                controller.setCenter(center)
                controller.setZoom(zoom)

                // Add blue dot overlay
                overlays.add(blueDotOverlay)

                if (!gesturesEnabled) {
                    setOnTouchListener { _, _ -> true } // consume all touches
                }

                // Fire callback when user finishes dragging
                if (onCenterChanged != null) {
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
        update = { /* center/zoom handled by LaunchedEffect */ }
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
