package com.sidhart.walkover.data

data class WeeklyStats(
    val totalWalks: Int = 0,
    val totalTimeMinutes: Long = 0L,
    val totalDistanceKm: Double = 0.0,
    val walksChangePercent: Double = 0.0,
    val timeChangePercent: Double = 0.0,
    val distanceChangePercent: Double = 0.0,
    val overallChangePercent: Double = 0.0
)
