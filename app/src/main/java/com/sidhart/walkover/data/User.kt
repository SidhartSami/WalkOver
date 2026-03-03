package com.sidhart.walkover.data

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val totalDistanceWalked: Double = 0.0, // in meters
    val totalWalks: Int = 0,
    val lastWalkDate: Long = 0L, // timestamp
    val isAnonymous: Boolean = false,

    // New fields for gamification
    val currentXP: Int = 0,
    val currentLevel: Int = 1,
    val totalXPEarned: Int = 0,
    
    // Monthly Leaderboard fields
    val monthlyDistanceWalked: Double = 0.0,
    val currentMonthId: String = "",

    // Territory / Compete mode fields
    val totalTerritoryM2: Double = 0.0,       // Total area owned across all territories
    val totalCompeteWalks: Int = 0,            // Number of compete-mode walks completed
    val territoryColorIndex: Int = -1          // Assigned color index (0-7), -1 = unassigned
) {
    constructor() : this("", "", "", 0.0, 0, 0L, false, 0, 1, 0, 0.0, "", 0.0, 0, -1)

    // Helper methods for XP and levels
    fun getXPForCurrentLevel(): Int {
        return UserProgress.calculateXPForLevel(currentLevel)
    }

    fun getXPForNextLevel(): Int {
        return UserProgress.calculateXPForLevel(currentLevel + 1)
    }

    fun getProgressToNextLevel(): Float {
        val currentLevelXP = getXPForCurrentLevel()
        val nextLevelXP = getXPForNextLevel()
        val xpInCurrentLevel = currentXP - currentLevelXP
        val xpNeededForNextLevel = nextLevelXP - currentLevelXP

        return if (xpNeededForNextLevel > 0) {
            (xpInCurrentLevel.toFloat() / xpNeededForNextLevel).coerceIn(0f, 1f)
        } else {
            1f
        }
    }

    fun getXPToNextLevel(): Int {
        return (getXPForNextLevel() - currentXP).coerceAtLeast(0)
    }
}