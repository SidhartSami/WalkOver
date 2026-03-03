package com.sidhart.walkover.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sidhart.walkover.data.*
import kotlinx.coroutines.tasks.await

/**
 * Repository for leaderboard data
 */
class LeaderboardRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val friendsRepo = FriendsRepository()

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val STREAKS_COLLECTION = "streaks"

        private const val MAX_LEADERBOARD_SIZE = 100
    }

    /**
     * Get global leaderboard by category and period
     */
    suspend fun getLeaderboard(
        category: LeaderboardCategory,
        currentUserId: String
    ): Result<List<LeaderboardEntry>> {
        return try {
            when (category) {
                LeaderboardCategory.XP          -> getXPLeaderboard(currentUserId)
                LeaderboardCategory.DISTANCE    -> getDistanceLeaderboard( currentUserId)
                LeaderboardCategory.WALKS_COUNT -> getWalksLeaderboard(currentUserId)
                LeaderboardCategory.STREAK      -> getStreakLeaderboard(currentUserId)
                LeaderboardCategory.TERRITORY   -> Result.success(emptyList()) // handled at FirebaseService level
            }
        } catch (e: Exception) {
            android.util.Log.e("LeaderboardRepository", "Error getting leaderboard", e)
            Result.failure(e)
        }
    }

    /**
     * Get friends-only leaderboard
     */
    suspend fun getFriendsLeaderboard(
        category: LeaderboardCategory,
        currentUserId: String
    ): Result<List<LeaderboardEntry>> {
        return try {
            // Get following list
            val following = friendsRepo.getFollowing(currentUserId).getOrNull() ?: emptyList()
            val friendIds = following.map { it.id } + currentUserId

            if (friendIds.size == 1) {
                // Only current user, return them
                return getCurrentUserLeaderboardEntry(currentUserId, category)
            }

            // Fetch friends' data and rank
            val entries = when (category) {
                LeaderboardCategory.XP          -> getFriendsXPLeaderboard(friendIds, currentUserId)
                LeaderboardCategory.DISTANCE    -> getFriendsDistanceLeaderboard(friendIds, currentUserId)
                LeaderboardCategory.WALKS_COUNT -> getFriendsWalksLeaderboard(friendIds, currentUserId)
                LeaderboardCategory.STREAK      -> getFriendsStreakLeaderboard(friendIds, currentUserId)
                LeaderboardCategory.TERRITORY   -> emptyList() // handled at FirebaseService level
            }

            Result.success(entries)
        } catch (e: Exception) {
            android.util.Log.e("LeaderboardRepository", "Error getting friends leaderboard", e)
            Result.failure(e)
        }
    }

    // ============= XP LEADERBOARD =============

    private suspend fun getXPLeaderboard(
        currentUserId: String
    ): Result<List<LeaderboardEntry>> {
        val users = firestore.collection(USERS_COLLECTION)
            .orderBy("totalXPEarned", Query.Direction.DESCENDING)
            .limit(MAX_LEADERBOARD_SIZE.toLong())
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(User::class.java) }

        val entries = users.mapIndexed { index, user ->
            LeaderboardEntry(
                userId = user.id,
                username = user.username,
                currentLevel = user.currentLevel,
                rank = index + 1,
                score = user.totalXPEarned.toDouble(),
                totalWalks = user.totalWalks,
                totalDistanceWalked = user.totalDistanceWalked,
                totalXPEarned = user.totalXPEarned,
                isCurrentUser = user.id == currentUserId
            )
        }

        return Result.success(enrichWithFollowStatus(entries, currentUserId))
    }

    // ============= DISTANCE LEADERBOARD =============

    private suspend fun getDistanceLeaderboard(
        currentUserId: String
    ): Result<List<LeaderboardEntry>> {
        // We fetch top users by total distance as a base pool.
        // This ensures we always show "Top Players" even if the monthly leaderboard is fresh/empty.
        val users = firestore.collection(USERS_COLLECTION)
            .orderBy("totalDistanceWalked", Query.Direction.DESCENDING)
            .limit(MAX_LEADERBOARD_SIZE.toLong())
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(User::class.java) }

        val currentMonthId = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())

        val entries = users.map { user ->
            // Calculate effective monthly distance
            val monthlyScore = if (user.currentMonthId == currentMonthId) {
                user.monthlyDistanceWalked / 1000.0 // KM
            } else {
                0.0
            }

            LeaderboardEntry(
                userId = user.id,
                username = user.username,
                currentLevel = user.currentLevel,
                rank = 0, // Will assign after sort
                score = monthlyScore,
                totalWalks = user.totalWalks,
                totalDistanceWalked = user.totalDistanceWalked,
                totalXPEarned = user.totalXPEarned,
                isCurrentUser = user.id == currentUserId
            )
        }.filter { it.score >= 1.0 } // Filter out users with < 1km
        .sortedWith(
            compareByDescending<LeaderboardEntry> { it.score } // Primary: Monthly Score
                .thenByDescending { it.totalDistanceWalked }  // Secondary: Total Distance (Fallback)
        ).mapIndexed { index, entry ->
            entry.copy(rank = index + 1)
        }

        return Result.success(enrichWithFollowStatus(entries, currentUserId))
    }

    // ============= WALKS COUNT LEADERBOARD =============

    private suspend fun getWalksLeaderboard(
        currentUserId: String
    ): Result<List<LeaderboardEntry>> {
        val users = firestore.collection(USERS_COLLECTION)
            .orderBy("totalWalks", Query.Direction.DESCENDING)
            .limit(MAX_LEADERBOARD_SIZE.toLong())
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(User::class.java) }

        val entries = users.mapIndexed { index, user ->
            LeaderboardEntry(
                userId = user.id,
                username = user.username,
                currentLevel = user.currentLevel,
                rank = index + 1,
                score = user.totalWalks.toDouble(),
                totalWalks = user.totalWalks,
                totalDistanceWalked = user.totalDistanceWalked,
                totalXPEarned = user.totalXPEarned,
                isCurrentUser = user.id == currentUserId
            )
        }

        return Result.success(enrichWithFollowStatus(entries, currentUserId))
    }

    // ============= STREAK LEADERBOARD =============

    private suspend fun getStreakLeaderboard(currentUserId: String): Result<List<LeaderboardEntry>> {
        val streaks = firestore.collection(STREAKS_COLLECTION)
            .orderBy("currentStreak", Query.Direction.DESCENDING)
            .limit(MAX_LEADERBOARD_SIZE.toLong())
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(StreakData::class.java) }

        // Fetch user data for each streak
        val userIds = streaks.map { it.userId }
        val users = if (userIds.isNotEmpty()) {
            firestore.collection(USERS_COLLECTION)
                .whereIn("id", userIds.take(10))
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(User::class.java) }
                .associateBy { it.id }
        } else {
            emptyMap()
        }

        val entries = streaks.mapIndexed { index, streak ->
            val user = users[streak.userId]
            LeaderboardEntry(
                userId = streak.userId,
                username = user?.username ?: "Unknown",
                currentLevel = user?.currentLevel ?: 1,
                rank = index + 1,
                score = streak.currentStreak.toDouble(),
                totalWalks = user?.totalWalks ?: 0,
                totalDistanceWalked = user?.totalDistanceWalked ?: 0.0,
                totalXPEarned = user?.totalXPEarned ?: 0,
                isCurrentUser = streak.userId == currentUserId
            )
        }

        return Result.success(enrichWithFollowStatus(entries, currentUserId))
    }

    // ============= FRIENDS LEADERBOARDS =============

    private suspend fun getFriendsXPLeaderboard(
        friendIds: List<String>,
        currentUserId: String
    ): List<LeaderboardEntry> {
        val batches = friendIds.chunked(10)
        val allUsers = mutableListOf<User>()

        batches.forEach { batch ->
            val users = firestore.collection(USERS_COLLECTION)
                .whereIn("id", batch)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(User::class.java) }
            allUsers.addAll(users)
        }

        return allUsers
            .sortedByDescending { it.totalXPEarned }
            .mapIndexed { index, user ->
                LeaderboardEntry(
                    userId = user.id,
                    username = user.username,
                    currentLevel = user.currentLevel,
                    rank = index + 1,
                    score = user.totalXPEarned.toDouble(),
                    totalWalks = user.totalWalks,
                    totalDistanceWalked = user.totalDistanceWalked,
                    totalXPEarned = user.totalXPEarned,
                    isCurrentUser = user.id == currentUserId,
                    isFollowing = user.id != currentUserId
                )
            }
    }

    private suspend fun getFriendsDistanceLeaderboard(
        friendIds: List<String>,
        currentUserId: String
    ): List<LeaderboardEntry> {
        val batches = friendIds.chunked(10)
        val allUsers = mutableListOf<User>()
        val currentMonthId = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())

        batches.forEach { batch ->
            val users = firestore.collection(USERS_COLLECTION)
                .whereIn("id", batch)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(User::class.java) }
            allUsers.addAll(users)
        }

        return allUsers
            .map { user ->
                // If user's month ID is old, their monthly distance is effectively 0 for the leaderboard
                val effectiveDistance = if (user.currentMonthId == currentMonthId) {
                    user.monthlyDistanceWalked
                } else {
                    0.0
                }
                user to effectiveDistance
            }
            .filter { it.second >= 1000.0 } // Filter out users with < 1km (1000m)
            .sortedByDescending { it.second }
            .mapIndexed { index, (user, distance) ->
                LeaderboardEntry(
                    userId = user.id,
                    username = user.username,
                    currentLevel = user.currentLevel,
                    rank = index + 1,
                    score = distance / 1000.0,
                    totalWalks = user.totalWalks,
                    totalDistanceWalked = user.totalDistanceWalked,
                    totalXPEarned = user.totalXPEarned,
                    isCurrentUser = user.id == currentUserId,
                    isFollowing = user.id != currentUserId
                )
            }
    }

    private suspend fun getFriendsWalksLeaderboard(
        friendIds: List<String>,
        currentUserId: String
    ): List<LeaderboardEntry> {
        val batches = friendIds.chunked(10)
        val allUsers = mutableListOf<User>()

        batches.forEach { batch ->
            val users = firestore.collection(USERS_COLLECTION)
                .whereIn("id", batch)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(User::class.java) }
            allUsers.addAll(users)
        }

        return allUsers
            .sortedByDescending { it.totalWalks }
            .mapIndexed { index, user ->
                LeaderboardEntry(
                    userId = user.id,
                    username = user.username,
                    currentLevel = user.currentLevel,
                    rank = index + 1,
                    score = user.totalWalks.toDouble(),
                    totalWalks = user.totalWalks,
                    totalDistanceWalked = user.totalDistanceWalked,
                    totalXPEarned = user.totalXPEarned,
                    isCurrentUser = user.id == currentUserId,
                    isFollowing = user.id != currentUserId
                )
            }
    }

    private suspend fun getFriendsStreakLeaderboard(
        friendIds: List<String>,
        currentUserId: String
    ): List<LeaderboardEntry> {
        val batches = friendIds.chunked(10)
        val allStreaks = mutableListOf<StreakData>()

        batches.forEach { batch ->
            val streaks = firestore.collection(STREAKS_COLLECTION)
                .whereIn("userId", batch)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(StreakData::class.java) }
            allStreaks.addAll(streaks)
        }

        // Fetch user data
        val users = firestore.collection(USERS_COLLECTION)
            .whereIn("id", friendIds.take(10))
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(User::class.java) }
            .associateBy { it.id }

        return allStreaks
            .sortedByDescending { it.currentStreak }
            .mapIndexed { index, streak ->
                val user = users[streak.userId]
                LeaderboardEntry(
                    userId = streak.userId,
                    username = user?.username ?: "Unknown",
                    currentLevel = user?.currentLevel ?: 1,
                    rank = index + 1,
                    score = streak.currentStreak.toDouble(),
                    totalWalks = user?.totalWalks ?: 0,
                    totalDistanceWalked = user?.totalDistanceWalked ?: 0.0,
                    totalXPEarned = user?.totalXPEarned ?: 0,
                    isCurrentUser = streak.userId == currentUserId,
                    isFollowing = streak.userId != currentUserId
                )
            }
    }

// ============= HELPER METHODS =============

    private suspend fun enrichWithFollowStatus(
        entries: List<LeaderboardEntry>,
        currentUserId: String
    ): List<LeaderboardEntry> {
        val userIds = entries.map { it.userId }.filter { it != currentUserId }

        if (userIds.isEmpty()) return entries

        val following = friendsRepo.getFollowing(currentUserId).getOrNull()?.map { it.id }?.toSet()
            ?: emptySet()

        return entries.map { entry ->
            entry.copy(isFollowing = entry.userId in following)
        }
    }

    private suspend fun getCurrentUserLeaderboardEntry(
        userId: String,
        category: LeaderboardCategory
    ): Result<List<LeaderboardEntry>> {
        val user = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .await()
            .toObject(User::class.java) ?: return Result.success(emptyList())

        val score = when (category) {
            LeaderboardCategory.XP          -> user.totalXPEarned.toDouble()
            LeaderboardCategory.DISTANCE    -> {
                val currentMonthId = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())
                if (user.currentMonthId == currentMonthId) user.monthlyDistanceWalked / 1000.0 else 0.0
            }
            LeaderboardCategory.WALKS_COUNT -> user.totalWalks.toDouble()
            LeaderboardCategory.STREAK      -> {
                val streak = firestore.collection(STREAKS_COLLECTION)
                    .document(userId).get().await().toObject(StreakData::class.java)
                streak?.currentStreak?.toDouble() ?: 0.0
            }
            LeaderboardCategory.TERRITORY   -> user.totalTerritoryM2
        }

        val entry = LeaderboardEntry(
            userId = user.id,
            username = user.username,
            currentLevel = user.currentLevel,
            rank = 1,
            score = score,
            totalWalks = user.totalWalks,
            totalDistanceWalked = user.totalDistanceWalked,
            totalXPEarned = user.totalXPEarned,
            isCurrentUser = true,
            isFollowing = false
        )

        return Result.success(listOf(entry))
    }
}