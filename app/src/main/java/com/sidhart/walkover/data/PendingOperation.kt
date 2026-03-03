package com.sidhart.walkover.data

import java.util.Date

/**
 * Represents a pending operation that failed due to network issues
 */
data class PendingOperation(
    val id: String = "",
    val type: OperationType = OperationType.SAVE_WALK,
    val walkId: String = "",
    val userId: String = "",
    val timestamp: Date = Date(),
    val retryCount: Int = 0,
    val data: Map<String, Any> = emptyMap()
)

enum class OperationType {
    SAVE_WALK,
    UPDATE_STREAK,
    UPDATE_USER_STATS,
    UPDATE_CHALLENGES,
    AWARD_XP,
    UPDATE_DAILY_ACTIVITY
}
