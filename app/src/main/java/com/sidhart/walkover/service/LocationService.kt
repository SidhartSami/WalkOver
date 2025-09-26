package com.sidhart.walkover.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
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
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    fun getCurrentLocation(): Flow<LocationPoint> = callbackFlow {
        if (!hasLocationPermission()) {
            close(Exception("Location permission not granted"))
            return@callbackFlow
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val locationPoint = LocationPoint(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = location.time,
                    accuracy = location.accuracy
                )
                trySend(locationPoint)
                close()
            } else {
                close(Exception("Location not available"))
            }
        }.addOnFailureListener { exception ->
            close(exception)
        }

        awaitClose { }
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

            // Calculate area using spherical geometry
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

