package com.sidhart.walkover.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sidhart.walkover.data.*
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.math.abs

/**
 * Repository for friends and social features
 * Handles all Firestore operations for friendships, search, and recommendations
 */
class FriendsRepository {
    private val firestore = FirebaseFirestore.getInstance()
    
    companion object {
        private const val FRIENDSHIPS_COLLECTION = "friendships"
        private const val USERS_COLLECTION = "users"

        private const val MAX_SEARCH_RESULTS = 50
        private const val MAX_RECOMMENDATIONS = 10
    }
    
    // ============= FOLLOW/UNFOLLOW =============
    
    /**
     * Follow a user (create friendship)
     */
    suspend fun followUser(currentUserId: String, currentUsername: String, 
                          targetUserId: String, targetUsername: String): Result<Unit> {
        return try {
            // Check if already following
            val existing = firestore.collection(FRIENDSHIPS_COLLECTION)
                .whereEqualTo("followerId", currentUserId)
                .whereEqualTo("followingId", targetUserId)
                .whereEqualTo("active", true)
                .get()
                .await()
            
            if (!existing.isEmpty) {
                return Result.failure(Exception("Already following this user"))
            }
            
            val friendship = Friendship(
                followerId = currentUserId,
                followerUsername = currentUsername,
                followingId = targetUserId,
                followingUsername = targetUsername,
                createdAt = Date(),
                active = true
            )
            
            firestore.collection(FRIENDSHIPS_COLLECTION)
                .add(friendship)
                .await()
            
            android.util.Log.d("FriendsRepository", "User $currentUserId now following $targetUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FriendsRepository", "Error following user", e)
            Result.failure(e)
        }
    }
    
    /**
     * Unfollow a user (soft delete friendship)
     */
    suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            val friendships = firestore.collection(FRIENDSHIPS_COLLECTION)
                .whereEqualTo("followerId", currentUserId)
                .whereEqualTo("followingId", targetUserId)
                .whereEqualTo("active", true)
                .get()
                .await()
            
            if (friendships.isEmpty) {
                return Result.failure(Exception("Not following this user"))
            }
            
            // Soft delete all matching friendships
            friendships.documents.forEach { doc ->
                doc.reference.update("active", false).await()
            }
            
            android.util.Log.d("FriendsRepository", "User $currentUserId unfollowed $targetUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FriendsRepository", "Error unfollowing user", e)
            Result.failure(e)
        }
    }
    
    // ============= FOLLOWERS/FOLLOWING LISTS =============
    
    /**
     * Get list of users following the current user
     */
    suspend fun getFollowers(userId: String): Result<List<UserSearchResult>> {
        return try {
            val friendships = firestore.collection(FRIENDSHIPS_COLLECTION)
                .whereEqualTo("followingId", userId)
                .whereEqualTo("active", true)
                .get()
                .await()
            
            val followerIds = friendships.documents.map { it.toObject(Friendship::class.java)?.followerId ?: "" }
            
            if (followerIds.isEmpty()) {
                return Result.success(emptyList())
            }
            
            // Fetch user details
            val users = getUsersByIds(followerIds, userId)
            
            Result.success(users)
        } catch (e: Exception) {
            android.util.Log.e("FriendsRepository", "Error getting followers", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get list of users the current user is following
     */
    suspend fun getFollowing(userId: String): Result<List<UserSearchResult>> {
        return try {
            val friendships = firestore.collection(FRIENDSHIPS_COLLECTION)
                .whereEqualTo("followerId", userId)
                .whereEqualTo("active", true)
                .get()
                .await()
            
            val followingIds = friendships.documents.map { it.toObject(Friendship::class.java)?.followingId ?: "" }
            
            if (followingIds.isEmpty()) {
                return Result.success(emptyList())
            }
            
            val users = getUsersByIds(followingIds, userId)
            
            Result.success(users)
        } catch (e: Exception) {
            android.util.Log.e("FriendsRepository", "Error getting following", e)
            Result.failure(e)
        }
    }
    
    // ============= SEARCH =============
    
    /**
     * Search users by username or email
     */
    suspend fun searchUsers(query: String, currentUserId: String): Result<List<UserSearchResult>> {
        return try {
            val trimmedQuery = query.trim().lowercase()
            
            if (trimmedQuery.length < 2) {
                return Result.success(emptyList())
            }
            
            // Search by username (startsWith for efficiency)
            val usernameResults = firestore.collection(USERS_COLLECTION)
                .orderBy("username")
                .startAt(trimmedQuery)
                .endAt(trimmedQuery + "\uf8ff")
                .limit(MAX_SEARCH_RESULTS.toLong())
                .get()
                .await()
            
            val users = usernameResults.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.let { user ->
                    if (user.id != currentUserId) { // Exclude current user
                        UserSearchResult(
                            id = user.id,
                            username = user.username,
                            email = user.email,
                            currentLevel = user.currentLevel,
                            totalXPEarned = user.totalXPEarned,
                            totalWalks = user.totalWalks,
                            totalDistanceWalked = user.totalDistanceWalked
                        )
                    } else null
                }
            }
            
            // Enrich with follow status
            val enrichedUsers = enrichWithFollowStatus(users, currentUserId)
            
            Result.success(enrichedUsers)
        } catch (e: Exception) {
            android.util.Log.e("FriendsRepository", "Error searching users", e)
            Result.failure(e)
        }
    }
    
    // ============= RECOMMENDATIONS =============
    
    /**
     * Get IDs of users the current user is following (Lightweight version)
     */
    suspend fun getFollowingIds(userId: String): Result<Set<String>> {
         return try {
            val friendships = firestore.collection(FRIENDSHIPS_COLLECTION)
                .whereEqualTo("followerId", userId)
                .whereEqualTo("active", true)
                .get()
                .await()
            
            val ids = friendships.documents.mapNotNull { 
                it.getString("followingId") 
            }.toSet()
            
            Result.success(ids)
        } catch (e: Exception) {
            android.util.Log.e("FriendsRepository", "Error getting following IDs", e)
            Result.failure(e)
        }
    }

    /**
     * Get friend recommendations based on activity level and mutual follows
     */
    suspend fun getFriendRecommendations(userId: String): Result<List<FriendRecommendation>> {
        return try {
            val currentUser = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
                .toObject(User::class.java) ?: return Result.success(emptyList())
            
            // Get users already following (IDs only, more robust)
            val followingIds = getFollowingIds(userId).getOrNull() ?: emptySet()
            val excludedIds = followingIds + userId
            
            // Find active users (excluding current user and already followed)
            val potentialFriends = firestore.collection(USERS_COLLECTION)
                .orderBy("totalWalks", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
                .documents
                .mapNotNull { doc -> 
                    val user = doc.toObject(User::class.java) ?: return@mapNotNull null
                    // Ensure ID is set correctly for filtering
                    if (user.id.isEmpty() || user.id != doc.id) user.copy(id = doc.id) else user
                }
                .filter { it.id !in excludedIds && it.totalWalks > 0 }
            
            // Score and rank recommendations
            val recommendations = potentialFriends.map { user ->
                val score = calculateRecommendationScore(currentUser, user)
                val reason = getRecommendationReason(currentUser, user)
                
                FriendRecommendation(
                    user = UserSearchResult(
                        id = user.id, // Now safely guaranteed to be correct
                        username = user.username,
                        email = user.email,
                        currentLevel = user.currentLevel,
                        totalXPEarned = user.totalXPEarned,
                        totalWalks = user.totalWalks,
                        totalDistanceWalked = user.totalDistanceWalked,
                        isFollowing = false
                    ),
                    reason = reason,
                    score = score
                )
            }
                .sortedByDescending { it.score }
                .take(MAX_RECOMMENDATIONS)
            
            Result.success(recommendations)
        } catch (e: Exception) {
            android.util.Log.e("FriendsRepository", "Error getting recommendations", e)
            Result.failure(e)
        }
    }
    
    // ============= HELPER METHODS =============
    
    private suspend fun getUsersByIds(userIds: List<String>, currentUserId: String): List<UserSearchResult> {
        if (userIds.isEmpty()) return emptyList()
        
        // Firestore 'in' query limited to 10 items, so batch if needed
        val batches = userIds.chunked(10)
        val allUsers = mutableListOf<UserSearchResult>()
        
        batches.forEach { batch ->
            try {
                val users = firestore.collection(USERS_COLLECTION)
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), batch)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc ->
                        doc.toObject(User::class.java)?.let { user ->
                            UserSearchResult(
                                id = doc.id, // Use document ID explicitly to be safe
                                username = user.username,
                                email = user.email,
                                currentLevel = user.currentLevel,
                                totalXPEarned = user.totalXPEarned,
                                totalWalks = user.totalWalks,
                                totalDistanceWalked = user.totalDistanceWalked
                            )
                        }
                    }
                allUsers.addAll(users)
            } catch (e: Exception) {
                android.util.Log.e("FriendsRepository", "Error fetching batch users", e)
            }
        }
        
        return enrichWithFollowStatus(allUsers, currentUserId)
    }
    
    private suspend fun enrichWithFollowStatus(users: List<UserSearchResult>, currentUserId: String): List<UserSearchResult> {
        if (users.isEmpty()) return users
        
        val userIds = users.map { it.id }
        
        // Get following status (current user -> others)
        val following = firestore.collection(FRIENDSHIPS_COLLECTION)
            .whereEqualTo("followerId", currentUserId)
            .whereIn("followingId", userIds.take(10)) // Limited to 10
            .whereEqualTo("active", true)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(Friendship::class.java)?.followingId }
            .toSet()
        
        // Get follower status (others -> current user)
        val followers = firestore.collection(FRIENDSHIPS_COLLECTION)
            .whereEqualTo("followingId", currentUserId)
            .whereIn("followerId", userIds.take(10))
            .whereEqualTo("active", true)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(Friendship::class.java)?.followerId }
            .toSet()
        
        return users.map { user ->
            user.copy(
                isFollowing = user.id in following,
                isFollowedBy = user.id in followers
            )
        }
    }
    
    private fun calculateRecommendationScore(currentUser: User, targetUser: User): Double {
        var score = 0.0
        
        // Similar level (higher score for closer levels)
        val levelDiff = abs(currentUser.currentLevel - targetUser.currentLevel)
        score += (10 - levelDiff.coerceAtMost(10)) * 2.0
        
        // Activity level (favor active users)
        if (targetUser.totalWalks >= 10) score += 15.0
        if (targetUser.totalWalks >= 50) score += 10.0
        
        // Recent activity (last walk within 7 days)
        val daysSinceLastWalk = (System.currentTimeMillis() - targetUser.lastWalkDate) / (1000 * 60 * 60 * 24)
        if (daysSinceLastWalk <= 7) score += 20.0
        else if (daysSinceLastWalk <= 30) score += 10.0
        
        return score
    }
    
    private fun getRecommendationReason(currentUser: User, targetUser: User): String {
        val levelDiff = abs(currentUser.currentLevel - targetUser.currentLevel)
        
        return when {
            levelDiff <= 2 -> "Similar level (Level ${targetUser.currentLevel})"
            targetUser.totalWalks >= 50 -> "Very active walker"
            targetUser.totalWalks >= 10 -> "Active walker"
            else -> "New to WalkOver"
        }
    }
}