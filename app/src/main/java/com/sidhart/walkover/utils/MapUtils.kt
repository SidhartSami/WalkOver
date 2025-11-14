package com.sidhart.walkover.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.sidhart.walkover.R
import com.sidhart.walkover.data.LocationPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.*

object MapUtils {

    /**
     * Check if the current theme is dark mode
     */
    fun isDarkMode(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Get theme-appropriate polyline color for tracking
     */
    fun getTrackingPolylineColor(context: Context): Int {
        return if (isDarkMode(context)) {
            android.graphics.Color.parseColor("#39FF14") // Neon Green for dark mode
        } else {
            android.graphics.Color.parseColor("#7CB342") // Material Green for light mode
        }
    }

    /**
     * Get theme-appropriate polyline color for saved walks
     */
    fun getSavedWalkPolylineColor(context: Context): Int {
        return if (isDarkMode(context)) {
            android.graphics.Color.parseColor("#C0F11C") // Neon Green for dark mode
        } else {
            android.graphics.Color.parseColor("#558B2F") // Darker green for light mode
        }
    }

    /**
     * Get the appropriate location pin icon based on current theme
     */
    fun getLocationPinIcon(context: Context): Drawable? {
        return try {
            val iconRes = if (isDarkMode(context)) {
                R.drawable.ic_location_pin_dark
            } else {
                R.drawable.ic_location_pin_light
            }
            ContextCompat.getDrawable(context, iconRes)
        } catch (e: Exception) {
            android.util.Log.w("MapUtils", "Could not load location pin icon", e)
            null
        }
    }

    /**
     * Convert LocationPoint to GeoPoint for osmdroid
     */
    fun LocationPoint.toGeoPoint(): GeoPoint {
        return GeoPoint(this.latitude, this.longitude)
    }

    /**
     * Convert Location to GeoPoint for osmdroid
     */
    fun Location.toGeoPoint(): GeoPoint {
        return GeoPoint(this.latitude, this.longitude)
    }

    /**
     * Convert LocationPoint to GeoPoint for osmdroid (static method)
     */
    fun convertLocationPointToGeoPoint(locationPoint: LocationPoint): GeoPoint {
        return GeoPoint(locationPoint.latitude, locationPoint.longitude)
    }

    /**
     * Convert Location to GeoPoint for osmdroid (static method)
     */
    fun convertLocationToGeoPoint(location: Location): GeoPoint {
        return GeoPoint(location.latitude, location.longitude)
    }

    /**
     * Calculate distance between two GeoPoints in meters
     */
    fun calculateDistance(point1: GeoPoint, point2: GeoPoint): Double {
        val lat1 = Math.toRadians(point1.latitude)
        val lat2 = Math.toRadians(point2.latitude)
        val deltaLat = Math.toRadians(point2.latitude - point1.latitude)
        val deltaLon = Math.toRadians(point2.longitude - point1.longitude)

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1) * cos(lat2) *
                sin(deltaLon / 2) * sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return 6371000 * c // Earth's radius in meters
    }

    /**
     * Calculate total distance of a route
     */
    fun calculateRouteDistance(points: List<GeoPoint>): Double {
        if (points.size < 2) return 0.0

        var totalDistance = 0.0
        for (i in 1 until points.size) {
            totalDistance += calculateDistance(points[i - 1], points[i])
        }
        return totalDistance
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
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
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
        polyline.color = color ?: getTrackingPolylineColor(mapView.context)
        polyline.width = width
        mapView.overlays.add(polyline)
        mapView.invalidate()
        return polyline
    }

    /**
     * Center map on a specific location with zoom
     */
    fun centerMapOnLocation(
        mapView: MapView,
        geoPoint: GeoPoint,
        zoomLevel: Double = 15.0
    ) {
        mapView.controller.setCenter(geoPoint)
        mapView.controller.setZoom(zoomLevel)
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
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    onSuccess(location)
                } else {
                    onFailure(Exception("Location is null"))
                }
            }.addOnFailureListener { exception ->
                onFailure(exception)
            }
        } catch (e: SecurityException) {
            onFailure(e)
        }
    }

    /**
     * Format distance in meters to human readable string
     */
    fun formatDistance(distanceInMeters: Double): String {
        return when {
            distanceInMeters < 1000 -> "${distanceInMeters.toInt()}m"
            distanceInMeters < 10000 -> "${(distanceInMeters / 1000).let { "%.1f".format(it) }}km"
            else -> "${(distanceInMeters / 1000).toInt()}km"
        }
    }

    /**
     * Add modern marker with theme-aware styling
     */
    fun addModernMarker(
        mapView: org.osmdroid.views.MapView,
        geoPoint: org.osmdroid.util.GeoPoint,
        title: String,
        snippet: String = "",
        isLocationMarker: Boolean = false,
        isActiveTracking: Boolean = false
    ): org.osmdroid.views.overlay.Marker {
        val marker = org.osmdroid.views.overlay.Marker(mapView)
        marker.position = geoPoint
        marker.title = title
        marker.snippet = snippet

        if (isLocationMarker) {
            // Try to use custom drawable, fallback to theme-appropriate default
            val drawable = try {
                if (isActiveTracking) {
                    androidx.core.content.ContextCompat.getDrawable(
                        mapView.context,
                        R.drawable.location_pin_active
                    )
                } else {
                    androidx.core.content.ContextCompat.getDrawable(
                        mapView.context,
                        R.drawable.location_pin
                    )
                }
            } catch (e: Exception) {
                // Fallback to theme-appropriate icon
                getLocationPinIcon(mapView.context)
            }
            marker.icon = drawable
        }

        marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
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
}