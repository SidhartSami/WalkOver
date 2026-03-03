package com.sidhart.walkover.data

data class UserProgress(
    val userId: String = "",
    val currentXP: Int = 0,
    val currentLevel: Int = 1,
    val totalXPEarned: Int = 0,
    val lastChallengeRefresh: Long = 0L // Timestamp of last daily challenge refresh
) {

    companion object {
        // XP required for each level (exponential scaling)
        fun calculateXPForLevel(level: Int): Int {
            if (level <= 1) return 0
            // Base XP required for level 2 is 100.
            // Formula: 100 * (1.5 ^ (level - 2))
            // Level 2: 100
            // Level 3: 150
            // Level 4: 225
            // Level 5: 337
            // Level 10: 2562
            // Level 20: 147789
            // Level 50: ~28 billion
            return (100.0 * Math.pow(1.5, (level - 2).toDouble())).toInt()
        }

        // Calculate level from total XP
        fun calculateLevelFromXP(totalXP: Int): Int {
            var level = 1
            while (totalXP >= calculateXPForLevel(level + 1)) {
                level++
            }
            return level
        }
    }
}