package com.sidhart.walkover.data

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val altitude: Double? = null,
    val accuracy: Float = 20f  // GPS accuracy in meters (lower is better)
)
