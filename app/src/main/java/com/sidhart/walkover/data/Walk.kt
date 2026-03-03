package com.sidhart.walkover.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.Date

@IgnoreExtraProperties
data class Walk(
    @DocumentId
    @get:Exclude  // Don't save this to Firestore
    var id: String = "",
    val userId: String = "",
    val username: String = "",
    val polylineCoordinates: List<GeoPoint> = emptyList(),
    val distanceCovered: Double = 0.0, // in meters
    val timestamp: Date = Date(),
    val duration: Long = 0L, // in milliseconds

    // Walk mode — ROAM or COMPETE
    val mode: String = WalkMode.ROAM.name,

    // For COMPETE mode: the closed polygon that forms the captured territory.
    // This is a simplified polygon (convex hull or the walked path closed back to start).
    val capturedPolygon: List<GeoPoint> = emptyList(),

    // Area of captured polygon in square meters (0 for ROAM walks)
    val capturedAreaM2: Double = 0.0,

    // Anti-cheat status: "VALID", "REJECTED_SPEED", "REJECTED_DISTANCE", etc.
    val status: String = "VALID"
)