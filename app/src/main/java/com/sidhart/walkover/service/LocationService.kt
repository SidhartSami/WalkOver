package com.sidhart.walkover.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.sidhart.walkover.data.LocationPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.*

class LocationService(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        1000L // 1 second
    ).apply {
        setMinUpdateIntervalMillis(500L)
        setMaxUpdateDelayMillis(2000L)
    }.build()

    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun getLocationUpdates(): Flow<LocationPoint> = callbackFlow {
        if (!hasLocationPermission()) {
            close(Exception("Location permission not granted"))
            return@callbackFlow
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val locationPoint = LocationPoint(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = location.time,
                        accuracy = location.accuracy
                    )
                    trySend(locationPoint)
                }
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                if (!locationAvailability.isLocationAvailable) {
                    Log.w("LocationService", "Location is not available")
                }
            }
        }

        try {
            // Double-check permission before making the call
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                close(Exception("Location permission not granted"))
                return@callbackFlow
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            ).addOnFailureListener { exception ->
                Log.e("LocationService", "Failed to request location updates", exception)
                close(exception)
            }
        } catch (securityException: SecurityException) {
            Log.e("LocationService", "Security exception when requesting location updates", securityException)
            close(securityException)
        } catch (exception: Exception) {
            Log.e("LocationService", "Unexpected exception when requesting location updates", exception)
            close(exception)
        }

        awaitClose {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            } catch (exception: Exception) {
                Log.e("LocationService", "Error removing location updates", exception)
            }
        }
    }

    fun getCurrentLocation(): Flow<LocationPoint> = callbackFlow {
        if (!hasLocationPermission()) {
            close(Exception("Location permission not granted"))
            return@callbackFlow
        }

        var locationCallback: LocationCallback? = null

        // Define requestFreshLocation function at the proper scope
        fun requestFreshLocation() {
            try {
                val freshLocationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    5000L // 5 seconds
                ).apply {
                    setMaxUpdateDelayMillis(10000L)
                    setMaxUpdates(1) // Only get one update
                }.build()

                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        locationResult.lastLocation?.let { freshLocation ->
                            val locationPoint = LocationPoint(
                                latitude = freshLocation.latitude,
                                longitude = freshLocation.longitude,
                                timestamp = freshLocation.time,
                                accuracy = freshLocation.accuracy
                            )
                            trySend(locationPoint)
                            close()
                        } ?: run {
                            close(Exception("No location available"))
                        }
                    }

                    override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                        if (!locationAvailability.isLocationAvailable) {
                            Log.w("LocationService", "Location is not available for current location request")
                            close(Exception("Location not available"))
                        }
                    }
                }

                // Double-check permission again before the actual call
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    close(Exception("Location permission not granted"))
                    return
                }

                fusedLocationClient.requestLocationUpdates(
                    freshLocationRequest,
                    locationCallback!!,
                    null
                ).addOnFailureListener { exception ->
                    Log.e("LocationService", "Failed to request fresh location", exception)
                    close(exception)
                }

            } catch (securityException: SecurityException) {
                Log.e("LocationService", "Security exception when requesting fresh location", securityException)
                close(securityException)
            } catch (exception: Exception) {
                Log.e("LocationService", "Unexpected exception when requesting fresh location", exception)
                close(exception)
            }
        }

        try {
            // Double-check permission before making the call
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                close(Exception("Location permission not granted"))
                return@callbackFlow
            }

            // First try to get last known location
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null && location.time > System.currentTimeMillis() - 60000) { // Less than 1 minute old
                    val locationPoint = LocationPoint(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = location.time,
                        accuracy = location.accuracy
                    )
                    trySend(locationPoint)
                    close()
                } else {
                    // Last location is too old or null, request fresh location
                    requestFreshLocation()
                }
            }.addOnFailureListener { exception ->
                Log.e("LocationService", "Failed to get last known location", exception)
                // Try to get fresh location as fallback
                requestFreshLocation()
            }


        } catch (securityException: SecurityException) {
            Log.e("LocationService", "Security exception when getting current location", securityException)
            close(securityException)
        } catch (exception: Exception) {
            Log.e("LocationService", "Unexpected exception when getting current location", exception)
            close(exception)
        }

        awaitClose {
            try {
                locationCallback?.let { callback ->
                    fusedLocationClient.removeLocationUpdates(callback)
                }
            } catch (exception: Exception) {
                Log.e("LocationService", "Error removing location updates in getCurrentLocation", exception)
            }
        }
    }

    companion object {
        fun calculateDistance(point1: LocationPoint, point2: LocationPoint): Double {
            val lat1Rad = Math.toRadians(point1.latitude)
            val lat2Rad = Math.toRadians(point2.latitude)
            val deltaLatRad = Math.toRadians(point2.latitude - point1.latitude)
            val deltaLonRad = Math.toRadians(point2.longitude - point1.longitude)

            val a = sin(deltaLatRad / 2) * sin(deltaLatRad / 2) +
                    cos(lat1Rad) * cos(lat2Rad) *
                    sin(deltaLonRad / 2) * sin(deltaLonRad / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))

            return 6371000 * c // Earth's radius in meters
        }

        fun calculateArea(points: List<LocationPoint>): Double {
            if (points.size < 3) return 0.0

            // Convert to radians
            val radianPoints = points.map { point ->
                Pair(Math.toRadians(point.longitude), Math.toRadians(point.latitude))
            }

            // Calculate area using spherical geometry (Shoelace formula adapted for sphere)
            var area = 0.0
            val n = radianPoints.size

            for (i in 0 until n) {
                val j = (i + 1) % n
                val lon1 = radianPoints[i].first
                val lat1 = radianPoints[i].second
                val lon2 = radianPoints[j].first
                val lat2 = radianPoints[j].second

                area += (lon2 - lon1) * (2 + sin(lat1) + sin(lat2))
            }

            area = abs(area) * 6371000 * 6371000 / 2.0 // Earth's radius squared
            return area
        }
    }
}