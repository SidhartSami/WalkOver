package com.sidhart.walkover.data

import com.google.firebase.firestore.GeoPoint
import java.util.Date

data class Walk(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val polylineCoordinates: List<GeoPoint> = emptyList(),
    val distanceCovered: Double = 0.0, // in meters
    val timestamp: Date = Date(),
    val areaCaptured: Double = 0.0 // in square meters
) {
    constructor() : this("", "", "", emptyList(), 0.0, Date(), 0.0)
}

