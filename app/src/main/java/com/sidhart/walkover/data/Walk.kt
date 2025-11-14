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
    val duration: Long = 0L // in milliseconds
)