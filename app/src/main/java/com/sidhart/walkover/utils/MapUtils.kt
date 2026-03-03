package com.sidhart.walkover.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.location.Location
import com.sidhart.walkover.data.LocationPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.*
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable

object MapUtils {

    /**
     * Check if the current theme is dark mode
     */
    fun isDarkMode(context: Context): Boolean {
        val nightModeFlags =
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Get theme-appropriate polyline color for tracking
     */
    fun getTrackingPolylineColor(context: Context): Int {
        return if (isDarkMode(context)) {
            "#39FF14".toColorInt() // Neon Green for dark mode
        } else {
            "#7CB342".toColorInt() // Material Green for light mode
        }
    }

    /**
     * Get theme-appropriate polyline color for saved walks
     */
    fun getSavedWalkPolylineColor(context: Context): Int {
        return if (isDarkMode(context)) {
            "#C0F11C".toColorInt() // Neon Green for dark mode
        } else {
            "#558B2F".toColorInt() // Darker green for light mode
        }
    }

    /**
     * Create a simple dot marker drawable with outline
     */
    fun createDotMarkerDrawable(context: Context, fillColor: Int, outlineColor: Int): Drawable {
        val sizePx = (16 * context.resources.displayMetrics.density).toInt()
        val borderPx = (3 * context.resources.displayMetrics.density).toInt()

        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        // Draw outline
        paint.color = outlineColor
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)

        // Draw inner circle
        paint.color = fillColor
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, (sizePx / 2f) - borderPx, paint)

        return bitmap.toDrawable(context.resources)
    }

    /**
     * Get the appropriate location pin icon based on current theme, now as a beautiful neon dot
     */
    fun getLocationPinIcon(context: Context): Drawable? {
        return try {
            val outlineCol =
                if (isDarkMode(context)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            val fillCol = "#C0F11C".toColorInt() // Neon Green
            createDotMarkerDrawable(context, fillCol, outlineCol)
        } catch (e: Exception) {
            android.util.Log.w("MapUtils", "Could not create location pin icon", e)
            null
        }
    }

    /**
     * Convert LocationPoint to GeoPoint for osmdroid (static method)
     */
    fun convertLocationPointToGeoPoint(locationPoint: LocationPoint): GeoPoint {
        return GeoPoint(locationPoint.latitude, locationPoint.longitude)
    }


    /**
     * Add a marker to the map with professional location pin icon
     */
    fun addMarker(
        mapView: MapView,
        geoPoint: GeoPoint,
        title: String = "",
        snippet: String = "",
        isLocationMarker: Boolean = false
    ): Marker {
        val marker = Marker(mapView)
        marker.position = geoPoint
        marker.title = title
        marker.snippet = snippet

        // Set professional location pin icon for location markers
        if (isLocationMarker) {
            val locationIcon = getLocationPinIcon(mapView.context)
            locationIcon?.let { icon ->
                marker.icon = icon
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
        }

        mapView.overlays.add(marker)
        mapView.invalidate()
        return marker
    }

    /**
     * Draw a polyline on the map with theme-aware color
     */
    fun drawPolyline(
        mapView: MapView,
        points: List<GeoPoint>,
        color: Int? = null,
        width: Float = 5f
    ): Polyline {
        val polyline = Polyline()
        polyline.setPoints(points)
        polyline.outlinePaint.color = color ?: getTrackingPolylineColor(mapView.context)
        polyline.outlinePaint.strokeWidth = width
        polyline.outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
        polyline.outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
        polyline.outlinePaint.isAntiAlias = true
        polyline.outlinePaint.pathEffect = android.graphics.CornerPathEffect(50f)
        mapView.overlays.add(polyline)
        mapView.invalidate()
        return polyline
    }

    /**
     * Center map on a specific location with zoom
     * If currently zoomed out, smoothly zooms in to minZoomLevel.
     */
    fun centerMapOnLocation(
        mapView: MapView,
        geoPoint: GeoPoint,
        minZoomLevel: Double = 17.5
    ) {
        val currentZoom = mapView.zoomLevelDouble
        val targetZoom = if (currentZoom < minZoomLevel) minZoomLevel else currentZoom
        
        mapView.controller.animateTo(geoPoint, targetZoom, 800L)
    }

    /**
     * Fit map to show all points in the list
     */
    fun fitMapToPoints(mapView: MapView, points: List<GeoPoint>, padding: Int = 50) {
        if (points.isEmpty()) return

        val boundingBox = org.osmdroid.util.BoundingBox.fromGeoPoints(points)
        mapView.zoomToBoundingBox(boundingBox, true, padding)
    }

    /**
     * Get current location using FusedLocationProviderClient
     */
    fun getCurrentLocation(
        context: Context,
        onSuccess: (Location) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient =
            com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)

        try {
            val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            )
                .addOnSuccessListener { location ->
                    if (location != null) {
                        onSuccess(location)
                    } else {
                        // Fallback to last known if current location fails
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                            if (lastLocation != null) onSuccess(lastLocation)
                            else onFailure(Exception("Location is completely null"))
                        }.addOnFailureListener { e -> onFailure(e) }
                    }
                }.addOnFailureListener { exception ->
                    onFailure(exception)
                }
        } catch (e: SecurityException) {
            onFailure(e)
        }
    }

    /**
     * Add modern marker with theme-aware styling
     */
    fun addModernMarker(
        mapView: MapView,
        geoPoint: GeoPoint,
        title: String,
        snippet: String = "",
        isLocationMarker: Boolean = false,
    ): Marker {
        val marker = Marker(mapView)
        marker.position = geoPoint
        marker.title = title
        marker.snippet = snippet

        if (isLocationMarker) {
            // Use the modernized neon dot with outline
            val drawable = getLocationPinIcon(mapView.context)
            marker.icon = drawable
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        } else {
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(marker)
        mapView.invalidate()

        return marker
    }

    /**
     * Clear all overlays from map
     */
    fun clearMap(mapView: MapView) {
        mapView.overlays.clear()
        mapView.invalidate()
    }

    /**
     * Calculate the area of a closed polygon in square meters using the Shoelace Formula.
     * Assumes points are given in order around the perimeter.
     */
    fun calculatePolygonArea(points: List<GeoPoint>): Double {
        if (points.size < 3) return 0.0

        var area = 0.0
        val earthRadius = 6378137.0 // Earth's radius in meters

        // Convert coordinates to radians and apply shoelace on a spherical approximate projection
        for (i in points.indices) {
            val p1 = points[i]
            val p2 = points[(i + 1) % points.size]

            val x1 = Math.toRadians(p1.longitude) * earthRadius * cos(Math.toRadians(p1.latitude))
            val y1 = Math.toRadians(p1.latitude) * earthRadius
            val x2 = Math.toRadians(p2.longitude) * earthRadius * cos(Math.toRadians(p2.latitude))
            val y2 = Math.toRadians(p2.latitude) * earthRadius

            area += (x1 * y2) - (x2 * y1)
        }

        return abs(area) / 2.0
    }
}