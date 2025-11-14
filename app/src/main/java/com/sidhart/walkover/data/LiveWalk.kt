package com.sidhart.walkover.data

/**
 * Represents the state of an active walk session
 */
data class LiveWalkState(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val startTime: Long = 0L,
    val pauseStartTime: Long = 0L,
    val totalPausedTime: Long = 0L,
    val points: List<LocationPoint> = emptyList(),
    val stats: WalkStats = WalkStats()
) {
    /**
     * Updates the walk statistics (without area calculation)
     */
    fun updateStats(distance: Double, area: Double, pointCount: Int): LiveWalkState {
        val elapsedTime = if (isTracking) {
            System.currentTimeMillis() - startTime - totalPausedTime
        } else {
            0L
        }

        val activeTime = if (isPaused) {
            elapsedTime - (System.currentTimeMillis() - pauseStartTime)
        } else {
            elapsedTime
        }

        val avgSpeed = if (activeTime > 0) {
            (distance / 1000.0) / (activeTime / 3600000.0) // km/h
        } else {
            0.0
        }

        val calories = (distance / 1000.0) * 60.0 // Rough estimate: 60 cal per km

        return copy(
            stats = WalkStats(
                distanceMeters = distance,
                distanceKm = distance / 1000.0,
                elapsedTimeMillis = elapsedTime,
                activeTimeMillis = activeTime,
                pausedTimeMillis = totalPausedTime,
                averageSpeedKmh = avgSpeed,
                caloriesBurned = calories,
                points = pointCount
            )
        )
    }
}

/**
 * Walk statistics for display (area removed)
 */
data class WalkStats(
    val distanceMeters: Double = 0.0,
    val distanceKm: Double = 0.0,
    val elapsedTimeMillis: Long = 0L,
    val activeTimeMillis: Long = 0L,
    val pausedTimeMillis: Long = 0L,
    val averageSpeedKmh: Double = 0.0,
    val caloriesBurned: Double = 0.0,
    val points: Int = 0
) {
    /**
     * Format elapsed time as HH:MM:SS
     */
    fun formatElapsedTime(): String {
        val seconds = (elapsedTimeMillis / 1000) % 60
        val minutes = (elapsedTimeMillis / 60000) % 60
        val hours = elapsedTimeMillis / 3600000
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Format active time (excluding pauses)
     */
    fun formatActiveTime(): String {
        val seconds = (activeTimeMillis / 1000) % 60
        val minutes = (activeTimeMillis / 60000) % 60
        val hours = activeTimeMillis / 3600000
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}