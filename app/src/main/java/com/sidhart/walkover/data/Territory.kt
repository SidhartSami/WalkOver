package com.sidhart.walkover.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.Date

/**
 * Represents a territory tile captured by a user in COMPETE walk mode.
 *
 * Each territory captures the geographic polygon walked by the user.
 * Territories are owned by whoever walked there most recently.
 * They are globally visible on the map to all users.
 *
 * Firestore collection: "territories"
 */
@IgnoreExtraProperties
data class Territory(
    @DocumentId
    @get:Exclude
    var id: String = "",

    // Owner info
    val ownerId: String = "",
    val ownerUsername: String = "",

    // The closed polygon of the walked area (in order)
    val polygon: List<GeoPoint> = emptyList(),

    // Area in square meters
    val areaM2: Double = 0.0,

    // Index into TerritoryColors list (0–7) assigned per-user for consistent coloring
    val colorIndex: Int = 0,

    // Source walk that created this territory
    val sourceWalkId: String = "",

    // Timestamps
    val capturedAt: Date = Date(),
    val updatedAt: Date = Date(),

    // Bounding box for quick geo queries
    val minLat: Double = 0.0,
    val maxLat: Double = 0.0,
    val minLng: Double = 0.0,
    val maxLng: Double = 0.0
)