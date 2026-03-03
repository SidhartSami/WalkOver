package com.sidhart.walkover.utils

object StreakManager {
    fun calculateStreakBonusXP(currentStreak: Int, baseWalkXP: Int): Int {
        val bonusMultiplier = when {
            currentStreak >= 90 -> 0.35  // 35% bonus
            currentStreak >= 60 -> 0.30  // 30% bonus
            currentStreak >= 30 -> 0.25  // 25% bonus
            currentStreak >= 14 -> 0.20  // 20% bonus
            currentStreak >= 7 -> 0.15   // 15% bonus
            currentStreak >= 1 -> 0.10   // 10% bonus
            else -> 0.0
        }
        
        return (baseWalkXP * bonusMultiplier).toInt()
    }

}
