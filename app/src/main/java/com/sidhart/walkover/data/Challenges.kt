package com.sidhart.walkover.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import java.util.Date

enum class ChallengeDifficulty(val xpReward: Int) {
    EASY(10),
    MEDIUM(25),
    HARD(50)
}

enum class ChallengeType {
    DISTANCE,      // Walk X kilometers
    DURATION,      // Walk for X minutes
    WALKS_COUNT,   // Complete X walks
    SPEED          // Maintain average speed of X km/h
}

data class Challenge(
    @DocumentId
    @get:Exclude
    var id: String = "",
    val type: ChallengeType = ChallengeType.DISTANCE,
    val difficulty: ChallengeDifficulty = ChallengeDifficulty.EASY,
    val title: String = "",
    val description: String = "",
    val targetValue: Double = 0.0, // Target distance/duration/count/speed
    val xpReward: Int = 0,
    val createdDate: Date = Date()
)

data class UserChallenge(
    @DocumentId
    @get:Exclude
    var id: String = "",
    val userId: String = "",
    val challengeId: String = "",
    val challenge: Challenge = Challenge(),
    val currentProgress: Double = 0.0,
    val isCompleted: Boolean = false,
    val completedDate: Date? = null,
    val assignedDate: Date = Date()
) {
    @Exclude
    fun getProgressPercentage(): Float {
        return (currentProgress / challenge.targetValue).coerceIn(0.0, 1.0).toFloat()
    }
}