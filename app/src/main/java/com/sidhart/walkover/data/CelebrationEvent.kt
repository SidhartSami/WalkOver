package com.sidhart.walkover.data

data class CelebrationEvent(
    val areaM2: Double = 0.0,
    val distanceKm: Double = 0.0,
    val isRoam: Boolean = false,
    val stolenFrom: String? = null,
    val mergedCount: Int = 0,
    val title: String = "",
    val description: String = "",
    val type: CelebrationType = CelebrationType.TERRITORY_CAPTURE
)

enum class CelebrationType {
    TERRITORY_CAPTURE,
    ACHIEVEMENT_EARNED,
    STREAK_MAINTAINED
}
