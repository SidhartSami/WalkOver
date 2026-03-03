package com.sidhart.walkover.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import java.util.Date

enum class DuelStatus {
    PENDING,
    ACTIVE,
    COMPLETED,
    DECLINED,
    EXPIRED
}

/**
 * Represents a 1v1 distance walking challenge between two users.
 */
data class DuelChallenge(
    @DocumentId
    @get:Exclude
    var id: String = "",
    val challengerId: String = "",
    val challengerUsername: String = "",
    val opponentId: String = "",
    val opponentUsername: String = "",
    val status: String = DuelStatus.PENDING.name, // "PENDING", "ACTIVE", "COMPLETED", "DECLINED", "EXPIRED"
    val durationDays: Int = 3, // 3 or 7 days
    val requestTimestamp: Date = Date(), // When the challenge was sent
    val startTimestamp: Date? = null, // Set when accepted
    val endTimestamp: Date? = null, // Set when accepted (midnight of the last day)
    val challengerDistanceKm: Double = 0.0,
    val opponentDistanceKm: Double = 0.0,
    val winnerId: String? = null
)
