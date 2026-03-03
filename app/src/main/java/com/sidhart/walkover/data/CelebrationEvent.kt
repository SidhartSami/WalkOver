package com.sidhart.walkover.data

data class CelebrationEvent(
    val title: String,
    val description: String,
    val type: CelebrationType = CelebrationType.TERRITORY_CAPTURE
)

enum class CelebrationType {
    TERRITORY_CAPTURE,
    ACHIEVEMENT_EARNED,
    STREAK_MAINTAINED
}
