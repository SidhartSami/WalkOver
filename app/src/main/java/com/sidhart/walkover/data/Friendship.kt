package com.sidhart.walkover.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import java.util.Date

/**
 * Represents a follow relationship between users
 * Uses unidirectional follow model (like Instagram/Twitter)
 */
data class Friendship(
    @DocumentId
    @get:Exclude
    var id: String = "",
    
    val followerId: String = "",        // User who is following
    val followerUsername: String = "",
    
    val followingId: String = "",       // User being followed
    val followingUsername: String = "",
    
    val createdAt: Date = Date(),
    val active: Boolean = true        // For soft deletes
)
/**
 * User profile for search results
 */
data class UserSearchResult(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val currentLevel: Int = 1,
    val totalXPEarned: Int = 0,
    val totalWalks: Int = 0,
    val totalDistanceWalked: Double = 0.0,
    val isFollowing: Boolean = false,   // Computed field
    val isFollowedBy: Boolean = false   // Computed field
)

/**
 * Leaderboard entry with ranking
 */
data class LeaderboardEntry(
    val userId: String = "",
    val username: String = "",
    val currentLevel: Int = 1,
    val rank: Int = 0,
    val score: Double = 0.0,            // Can be XP, distance, walks count, or territory m²
    val totalWalks: Int = 0,
    val totalDistanceWalked: Double = 0.0,
    val totalXPEarned: Int = 0,
    val totalTerritoryM2: Double = 0.0, // Total territory area for TERRITORY category
    val isCurrentUser: Boolean = false,
    val isFollowing: Boolean = false
)

/**
 * Friend recommendation based on activity similarity
 */
data class FriendRecommendation(
    val user: UserSearchResult,
    val reason: String = "",            // "Similar level", "Active walker", etc.
    val score: Double = 0.0             // Recommendation strength
)



/**
 * Leaderboard time period
 */
enum class LeaderboardPeriod {
    ALL_TIME
}

/**
 * Leaderboard category
 */
enum class LeaderboardCategory {
    XP,
    DISTANCE,
    WALKS_COUNT,
    STREAK,
    TERRITORY   // Compete mode: total m² captured
}