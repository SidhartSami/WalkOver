package com.sidhart.walkover.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import java.util.Date

/**
 * Represents a user's walking streak data
 */
data class StreakData(
    @DocumentId
    @get:Exclude
    var id: String = "",
    val userId: String = "",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastWalkDate: Date = Date(),
    val streakStartDate: Date = Date(),
    val totalDaysWalked: Int = 0,
    val sevenDayStreakCount: Int = 0,  // Number of times user achieved 7-day streak
    val fourteenDayStreakCount: Int = 0  // Number of times user achieved 14-day streak
) {


    companion object {
        fun getDaysBetween(start: Date, end: Date): Int {
            val diff = end.time - start.time
            return (diff / (24 * 60 * 60 * 1000)).toInt()
        }
    }
}


/**
 * Tracks daily activity for heat map (saved separately from StreakData)
 */
data class DailyActivityRecord(
    val userId: String = "",
    val date: Date = Date(),
    val walksCompleted: Int = 0,
    val distanceCovered: Double = 0.0, // in meters
    val challengesCompleted: Int = 0,
    val xpEarned: Int = 0,
    val hasMinimumActivity: Boolean = false // true if 0.5km+ or 1 challenge done
)