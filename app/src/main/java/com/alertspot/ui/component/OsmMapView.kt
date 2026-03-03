package com.alertspot.ui.component

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.Color as AndroidColor
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import android.widget.TextView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
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
 * Custom overlay that draws a radius circle always at the map's visual center.
 * Uses the map projection to convert meters → pixels on every draw call,
 * so it never lags or slides during drag.
 */
private class CenterRadiusOverlay : Overlay() {
    var radiusMeters: Double = 0.0
    var circleFillColor: Int = AndroidColor.argb(30, 0, 122, 255)
    var circleStrokeColor: Int = AndroidColor.argb(102, 0, 122, 255)
    var circleStrokeWidth: Float = 3f
    var isActive: Boolean = false

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || !isActive || radiusMeters <= 0) return

        val centerGeo = mapView.mapCenter as GeoPoint
        val centerPx = Point()
        mapView.projection.toPixels(centerGeo, centerPx)

        // Compute a point `radiusMeters` to the east to get pixel radius
        val edgeGeo = centerGeo.destinationPoint(radiusMeters, 90.0)
        val edgePx = Point()
        mapView.projection.toPixels(edgeGeo, edgePx)

        val pixelRadius = kotlin.math.hypot(
            (edgePx.x - centerPx.x).toDouble(),
            (edgePx.y - centerPx.y).toDouble()
        ).toFloat()

        fillPaint.color = circleFillColor
        strokePaint.color = circleStrokeColor
        strokePaint.strokeWidth = circleStrokeWidth

        canvas.drawCircle(centerPx.x.toFloat(), centerPx.y.toFloat(), pixelRadius, fillPaint)
        canvas.drawCircle(centerPx.x.toFloat(), centerPx.y.toFloat(), pixelRadius, strokePaint)
    }
}

/**
 * Creates a Google-Maps-style teardrop pin bitmap.
 * The pin has a coloured body, subtle gradient, white inner circle, and a soft shadow.
 */
private fun createMapPinBitmap(color: Int, sizePx: Int): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val w = sizePx.toFloat()
    // Layout: the pin occupies the full height
    val headRadius = w * 0.36f           // radius of the round head
    val cx = w / 2f
    val headCy = headRadius + w * 0.06f  // small top padding for shadow
    val tipY = w * 0.95f                 // bottom tip

    // Shadow (slightly offset, semi-transparent)
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = AndroidColor.argb(50, 0, 0, 0)
        style = Paint.Style.FILL
    }
    val shadowPath = Path().apply {
        addCircle(cx + 1f, headCy + 2f, headRadius + 1f, Path.Direction.CW)
        moveTo(cx - headRadius * 0.45f + 1f, headCy + headRadius * 0.7f + 2f)
        lineTo(cx + 1f, tipY + 2f)
        lineTo(cx + headRadius * 0.45f + 1f, headCy + headRadius * 0.7f + 2f)
        close()
    }
    canvas.drawPath(shadowPath, shadowPaint)

    // Pin body path (circle + pointed tail)
    val bodyPath = Path().apply {
        addCircle(cx, headCy, headRadius, Path.Direction.CW)
        moveTo(cx - headRadius * 0.45f, headCy + headRadius * 0.7f)
        lineTo(cx, tipY)
        lineTo(cx + headRadius * 0.45f, headCy + headRadius * 0.7f)
        close()
    }

    // Fill with slight vertical gradient for depth
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader = LinearGradient(
            cx, headCy - headRadius, cx, tipY,
            lighten(color, 0.15f), darken(color, 0.10f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawPath(bodyPath, bodyPaint)

    // Thin white border around the head circle
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = w * 0.025f
    }
    canvas.drawCircle(cx, headCy, headRadius, borderPaint)

    // White inner circle (the "cutout" look)
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, headCy, headRadius * 0.38f, dotPaint)

    return bmp
}

private fun lighten(color: Int, fraction: Float): Int {
    val r = AndroidColor.red(color) + ((255 - AndroidColor.red(color)) * fraction).toInt()
    val g = AndroidColor.green(color) + ((255 - AndroidColor.green(color)) * fraction).toInt()
    val b = AndroidColor.blue(color) + ((255 - AndroidColor.blue(color)) * fraction).toInt()
    return AndroidColor.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
}

private fun darken(color: Int, fraction: Float): Int {
    val r = (AndroidColor.red(color) * (1 - fraction)).toInt()
    val g = (AndroidColor.green(color) * (1 - fraction)).toInt()
    val b = (AndroidColor.blue(color) * (1 - fraction)).toInt()
    return AndroidColor.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
}

/**
 * Manages a floating label above a tapped pin.
 * Only one label is visible at a time; tapping the map dismisses it.
 */
private class PinLabelManager {
    private var currentBubble: android.view.View? = null
    private var currentMapView: MapView? = null

    fun show(mapView: MapView, marker: Marker) {
        dismiss() // remove any existing

        val title = marker.title
        if (title.isNullOrBlank()) return

        val ctx = mapView.context
        val density = ctx.resources.displayMetrics.density

        val tv = TextView(ctx).apply {
            text = title
            setTextColor(AndroidColor.rgb(30, 30, 30))
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            val hPad = (14 * density).toInt()
            val vPad = (8 * density).toInt()
            setPadding(hPad, vPad, hPad, vPad)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(AndroidColor.WHITE)
                cornerRadius = 12 * density
            }
            elevation = 6 * density
        }

        val layoutParams = MapView.LayoutParams(
            MapView.LayoutParams.WRAP_CONTENT,
            MapView.LayoutParams.WRAP_CONTENT,
            marker.position,
            MapView.LayoutParams.CENTER,
            0, -(52 * density).toInt()
        )

        mapView.addView(tv, layoutParams)
        currentBubble = tv
        currentMapView = mapView
    }

    fun dismiss() {
        currentBubble?.let { currentMapView?.removeView(it) }
        currentBubble = null
        currentMapView = null
    }
}

/**
 * A reusable Compose wrapper around osmdroid's MapView.
 *
 * @param modifier         Compose modifier
 * @param center           Center of the map
 * @param zoom             Zoom level (0–20)
 * @param markers          List of markers to display
 * @param circles          List of radius circles to display (geo-positioned)
 * @param centerRadiusMeters If > 0, draws a radius circle always at map center (no lag during drag)
 * @param centerRadiusFillColor   Fill color for center radius circle
 * @param centerRadiusStrokeColor Stroke color for center radius circle
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
    centerRadiusMeters: Double = 0.0,
    centerRadiusFillColor: Color = Color(0x1A007AFF),
    centerRadiusStrokeColor: Color = Color(0x66007AFF),
    gesturesEnabled: Boolean = true,
    onCenterChanged: ((GeoPoint) -> Unit)? = null,
    userLocation: GeoPoint? = null,
    animateKey: Int = 0
) {
    val context = LocalContext.current

    // osmdroid is pre-configured in AlertSpotApp.onCreate() — no per-view setup needed

    // Remember the MapView so it survives recomposition
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val blueDotOverlay = remember { BlueDotOverlay() }
    val centerRadiusOverlay = remember { CenterRadiusOverlay() }
    val pinLabelManager = remember { PinLabelManager() }

    // Update center radius overlay when params change
    LaunchedEffect(centerRadiusMeters, centerRadiusFillColor, centerRadiusStrokeColor) {
        centerRadiusOverlay.radiusMeters = centerRadiusMeters
        centerRadiusOverlay.circleFillColor = centerRadiusFillColor.toArgb()
        centerRadiusOverlay.circleStrokeColor = centerRadiusStrokeColor.toArgb()
        centerRadiusOverlay.isActive = centerRadiusMeters > 0
        mapViewRef.value?.invalidate()
    }

    // Animate to center only on programmatic triggers (animateKey change)
    LaunchedEffect(animateKey) {
        mapViewRef.value?.controller?.animateTo(center, zoom, 300L)
    }

    // Update blue dot when userLocation changes
    LaunchedEffect(userLocation) {
        blueDotOverlay.updateLocation(userLocation)
        mapViewRef.value?.invalidate()
    }

    // Rebuild circle/marker overlays when the list definition changes
    val circleSignature = circles.map { "${it.radiusMeters}|${it.fillColor}|${it.strokeColor}|${it.strokeWidth}" }
    LaunchedEffect(markers, circleSignature) {
        val mapView = mapViewRef.value ?: return@LaunchedEffect
        // Dismiss any open label before rebuilding
        pinLabelManager.dismiss()
        // Remove old marker/circle overlays
        mapView.overlays.removeAll { it is Polygon || it is Marker }

        // Add circles (geo-positioned — stay at their location)
        circles.forEach { circle ->
            val polygon = Polygon(mapView).apply {
                points = Polygon.pointsAsCircle(circle.center, circle.radiusMeters)
                fillPaint.color = circle.fillColor.toArgb()
                outlinePaint.color = circle.strokeColor.toArgb()
                outlinePaint.strokeWidth = circle.strokeWidth
            }
            mapView.overlays.add(polygon)
        }

        // Add markers with Google-Maps-style pin bitmaps
        val density = mapView.context.resources.displayMetrics.density
        val pinSizePx = (44 * density).toInt() // 44dp
        markers.forEach { m ->
            val pinBitmap = createMapPinBitmap(m.tintColor, pinSizePx)
            val marker = Marker(mapView).apply {
                position = m.position
                title = m.title
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setInfoWindow(null) // disable default bubble
                icon = BitmapDrawable(mapView.context.resources, pinBitmap)
                setOnMarkerClickListener { clickedMarker, mv ->
                    pinLabelManager.show(mv, clickedMarker)
                    true
                }
            }
            mapView.overlays.add(marker)
        }

        mapView.invalidate()
    }

    // Keep a reference to onCenterChanged so the MapListener can call it
    val latestOnCenterChanged by rememberUpdatedState(onCenterChanged)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                // ── Performance: hardware-accelerate & reduce tile fetches ──
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                // Use OSM MAPNIK tiles with explicit multi-subdomain setup
                // for parallel downloads (a/b/c subdomains).
                // MAPNIK has the most complete POI & label coverage.
                val fastMapnik = XYTileSource(
                    "Mapnik-Fast",
                    0, 19, 256, ".png",
                    arrayOf(
                        "https://a.tile.openstreetmap.org/",
                        "https://b.tile.openstreetmap.org/",
                        "https://c.tile.openstreetmap.org/"
                    ),
                    "\u00a9 OpenStreetMap contributors"
                )
                setTileSource(fastMapnik)

                setMultiTouchControls(true)
                isTilesScaledToDpi = true          // fewer tiles per screen
                isHorizontalMapRepetitionEnabled = false
                isVerticalMapRepetitionEnabled = false
                overlayManager.tilesOverlay.loadingBackgroundColor = AndroidColor.rgb(242, 239, 233)
                overlayManager.tilesOverlay.loadingLineColor = AndroidColor.rgb(220, 218, 212)

                zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                controller.setCenter(center)
                controller.setZoom(zoom)

                // Add blue dot overlay
                overlays.add(blueDotOverlay)

                // Add center radius overlay (always draws at screen center)
                overlays.add(centerRadiusOverlay)

                // Dismiss label on map tap (outside any marker)
                overlays.add(object : Overlay() {
                    override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean {
                        pinLabelManager.dismiss()
                        return false // don't consume — let other overlays handle too
                    }
                })

                if (!gesturesEnabled) {
                    setOnTouchListener { _, _ -> true } // consume all touches
                }

                // Use MapListener to notify Compose of center changes during scroll
                addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?): Boolean {
                        val c = mapCenter as GeoPoint
                        latestOnCenterChanged?.invoke(c)
                        return false
                    }

                    override fun onZoom(event: ZoomEvent?): Boolean {
                        return false
                    }
                })

                mapViewRef.value = this
            }
        },
        update = { /* center/zoom handled by LaunchedEffect */ }
    )
}

data class OsmMarker(
    val position: GeoPoint,
    val title: String = "",
    val snippet: String = "",
    val tintColor: Int = AndroidColor.rgb(52, 199, 89) // default green
)

data class OsmCircle(
    val center: GeoPoint,
    val radiusMeters: Double,
    val fillColor: Color = Color(0x1A007AFF),
    val strokeColor: Color = Color(0x66007AFF),
    val strokeWidth: Float = 3f
)
