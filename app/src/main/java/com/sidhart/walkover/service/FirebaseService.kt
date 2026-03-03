package com.sidhart.walkover.service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.sidhart.walkover.data.User
import com.sidhart.walkover.data.Walk
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.GoogleAuthProvider
import com.sidhart.walkover.utils.ChallengeManager
import com.google.firebase.firestore.FieldValue
import com.sidhart.walkover.data.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import java.util.*
import com.sidhart.walkover.repository.FriendsRepository
import com.sidhart.walkover.repository.LeaderboardRepository
import com.sidhart.walkover.repository.DuelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlin.math.abs


class FirebaseService {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Offline queue manager - set from MainActivity
    var offlineQueueManager: OfflineQueueManager? = null

    companion object {
        private const val WALKS_COLLECTION = "walks"
        private const val USERS_COLLECTION = "users"
        private const val TERRITORIES_COLLECTION = "territories"
    }

    private val friendsRepository = FriendsRepository()
    private val leaderboardRepository = LeaderboardRepository()
    private val duelRepository = DuelRepository()

    
    /**
     * Follow a user
     */
    suspend fun followUser(targetUserId: String, targetUsername: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))
            
            friendsRepository.followUser(
                currentUserId = currentUser.uid,
                currentUsername = currentUser.displayName ?: "User",
                targetUserId = targetUserId,
                targetUsername = targetUsername
            )
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error following user", e)
            Result.failure(e)
        }
    }
    
    /**
     * Unfollow a user
     */
    suspend fun unfollowUser(targetUserId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))
            
            friendsRepository.unfollowUser(currentUser.uid, targetUserId)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error unfollowing user", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get list of followers
     */
    suspend fun getFollowers(): Result<List<UserSearchResult>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))
            
            friendsRepository.getFollowers(currentUser.uid)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error getting followers", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get list of users I'm following
     */
    suspend fun getFollowing(): Result<List<UserSearchResult>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))
            
            friendsRepository.getFollowing(currentUser.uid)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error getting following", e)
            Result.failure(e)
        }
    }
    
    /**
     * Search users by username or email
     */
    suspend fun searchUsers(query: String): Result<List<UserSearchResult>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))
            
            friendsRepository.searchUsers(query, currentUser.uid)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error searching users", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get friend recommendations
     */
    suspend fun getFriendRecommendations(): Result<List<FriendRecommendation>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))
            
            friendsRepository.getFriendRecommendations(currentUser.uid)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error getting recommendations", e)
            Result.failure(e)
        }
    }
    

    suspend fun createDuelChallenge(opponentId: String, opponentUsername: String, durationDays: Int): Result<DuelChallenge> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))
            duelRepository.createDuelChallenge(currentUser.uid, currentUser.displayName ?: "User", opponentId, opponentUsername, durationDays)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPendingDuels(): Result<List<DuelChallenge>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))
            duelRepository.getPendingDuels(currentUser.uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSentPendingDuels(): Result<List<DuelChallenge>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))
            duelRepository.getSentPendingDuels(currentUser.uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptDuelChallenge(challengeId: String, durationDays: Int): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))
        return duelRepository.acceptDuelChallenge(challengeId, durationDays, currentUser.uid)
    }

    suspend fun declineDuelChallenge(challengeId: String): Result<Unit> {
        return duelRepository.declineDuelChallenge(challengeId)
    }

    suspend fun getActiveDuel(): Result<DuelChallenge?> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))
            duelRepository.getActiveDuel(currentUser.uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDuelDistance(challengeId: String, isChallenger: Boolean, distanceKm: Double): Result<Unit> {
        return duelRepository.updateDuelDistance(challengeId, isChallenger, distanceKm)
    }
    
    // ============= LEADERBOARD METHODS =============
    
    /**
     * Get global leaderboard
     */
    suspend fun getLeaderboard(
        category: LeaderboardCategory,
        period: LeaderboardPeriod = LeaderboardPeriod.ALL_TIME
    ): Result<List<LeaderboardEntry>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))
            
            leaderboardRepository.getLeaderboard(category, currentUser.uid)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error getting leaderboard", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get friends-only leaderboard
     */
    suspend fun getFriendsLeaderboard(
        category: LeaderboardCategory
    ): Result<List<LeaderboardEntry>> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))
            
            leaderboardRepository.getFriendsLeaderboard(category, currentUser.uid)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error getting friends leaderboard", e)
            Result.failure(e)
        }
    }

    /**
     * Unified leaderboard data fetch — routes to global or friends based on friendsOnly param.
     */
    suspend fun getLeaderboardData(
        category: LeaderboardCategory,
        period: LeaderboardPeriod,
        friendsOnly: Boolean
    ): Result<List<LeaderboardEntry>> {
        return if (friendsOnly) getFriendsLeaderboard(category)
               else getLeaderboard(category, period)
    }

    // ============= AUTHENTICATION METHODS =============
    suspend fun resendVerificationEmailForUnverifiedUser(email: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            
            if (currentUser != null && currentUser.email == email) {
                // Already signed in with this email
                if (!currentUser.isEmailVerified) {
                    currentUser.sendEmailVerification().await()
                    android.util.Log.d("FirebaseService", "Verification email resent to: $email")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Email already verified"))
                }
            } else {
                // Not signed in - cannot resend without password
                Result.failure(Exception("Please provide your password to resend verification email"))
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Failed to resend verification email", e)
            Result.failure(Exception("Failed to resend verification email: ${e.message}"))
        }
    }

    suspend fun getDailyChallenges(): Result<List<UserChallenge>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))

            android.util.Log.d("FirebaseService", "Fetching daily challenges for user: $userId")

            // Check if we need to refresh challenges
            val userProgress = getUserProgress(userId).getOrNull()
            val shouldRefresh = userProgress?.let {
                ChallengeManager.shouldRefreshChallenges(it.lastChallengeRefresh)
            } ?: true

            android.util.Log.d("FirebaseService", "Should refresh challenges: $shouldRefresh")

            if (shouldRefresh) {
                // Generate and assign new daily challenges
                assignDailyChallenges(userId)
            }

            // Fetch today's challenges
            val today = ChallengeManager.getTodayMidnight()
            val todayTimestamp = today.time

            android.util.Log.d("FirebaseService", "Fetching challenges from today: $today")

            val querySnapshot = firestore.collection("userChallenges")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("assignedDate", Date(todayTimestamp))
                .get()
                .await()

            // Manually set document IDs (similar to how we do it for walks)
            val challenges = querySnapshot.documents.mapNotNull { document ->
                try {
                    val challenge = document.toObject(UserChallenge::class.java)
                    if (challenge != null) {
                        challenge.id = document.id
                        android.util.Log.d("FirebaseService", "Loaded challenge: ${challenge.challenge.title} (ID: ${challenge.id})")
                        challenge
                    } else {
                        android.util.Log.w("FirebaseService", "Failed to deserialize challenge document: ${document.id}")
                        null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseService", "Error parsing challenge document: ${document.id}", e)
                    null
                }
            }.sortedBy { it.challenge.difficulty.ordinal }

            android.util.Log.d("FirebaseService", "Successfully loaded ${challenges.size} challenges")

            Result.success(challenges)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error getting daily challenges", e)
            Result.failure(e)
        }
    }

    suspend fun assignDailyChallenges(userId: String) {
        try {
            android.util.Log.d("FirebaseService", "Assigning daily challenges for user: $userId")

            // Generate today's challenges
            val challenges = ChallengeManager.generateDailyChallenges()
            val today = ChallengeManager.getTodayMidnight()

            android.util.Log.d("FirebaseService", "Generated ${challenges.size} challenges for today: $today")
            challenges.forEach { challenge ->
                android.util.Log.d("FirebaseService", "  - ${challenge.difficulty}: ${challenge.title}")
            }

            // Delete ALL old challenges (from before today) to prevent duplicates
            val oldChallenges = firestore.collection("userChallenges")
                .whereEqualTo("userId", userId)
                .whereLessThan("assignedDate", today)
                .get()
                .await()

            android.util.Log.d("FirebaseService", "Deleting ${oldChallenges.documents.size} old challenges from previous days")
            oldChallenges.documents.forEach { it.reference.delete().await() }

            // Also delete any duplicate challenges from today (in case of multiple refreshes)
            val todayChallenges = firestore.collection("userChallenges")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("assignedDate", today)
                .get()
                .await()

            android.util.Log.d("FirebaseService", "Found ${todayChallenges.documents.size} existing challenges for today, deleting them")
            todayChallenges.documents.forEach { it.reference.delete().await() }

            // Assign new challenges
            challenges.forEach { challenge ->
                val userChallenge = UserChallenge(
                    userId = userId,
                    challengeId = UUID.randomUUID().toString(),
                    challenge = challenge,
                    currentProgress = 0.0,
                    isCompleted = false,
                    assignedDate = today
                )

                val docRef = firestore.collection("userChallenges")
                    .add(userChallenge)
                    .await()

                android.util.Log.d("FirebaseService", "Created challenge: ${challenge.title} with doc ID: ${docRef.id}")
            }

            // Update last refresh time
            firestore.collection("userProgress")
                .document(userId)
                .set(mapOf("lastChallengeRefresh" to System.currentTimeMillis()),
                    com.google.firebase.firestore.SetOptions.merge())
                .await()

            android.util.Log.d("FirebaseService", "✅ Successfully assigned ${challenges.size} daily challenges")

        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "❌ Error assigning daily challenges", e)
        }
    }



    suspend fun updateChallengeProgress(
        userChallengeId: String,
        newProgress: Double
    ): Result<Boolean> {
        return try {
            val challengeRef = firestore.collection("userChallenges").document(userChallengeId)
            val challenge = challengeRef.get().await().toObject(UserChallenge::class.java)
                ?: return Result.failure(Exception("Challenge not found"))

            val isCompleted = newProgress >= challenge.challenge.targetValue

            val updates = hashMapOf<String, Any>(
                "currentProgress" to newProgress,
                "isCompleted" to isCompleted
            )

            if (isCompleted && !challenge.isCompleted) {
                updates["completedDate"] = Date()

                // Award XP to user
                awardXP(challenge.userId, challenge.challenge.xpReward)
            }

            challengeRef.update(updates).await()
            Result.success(isCompleted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun awardXP(userId: String, xpAmount: Int) {
        try {
            val userRef = firestore.collection("users").document(userId)
            val userDoc = userRef.get().await()
            val user = userDoc.toObject(User::class.java) ?: return

            val newTotalXP = user.totalXPEarned + xpAmount
            val newCurrentXP = user.currentXP + xpAmount
            val newLevel = UserProgress.calculateLevelFromXP(newTotalXP)

            userRef.update(
                mapOf(
                    "currentXP" to newCurrentXP,
                    "totalXPEarned" to newTotalXP,
                    "currentLevel" to newLevel
                )
            ).await()

        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error awarding XP", e)
        }
    }

    suspend fun getUserProgress(userId: String): Result<UserProgress> {
        return try {
            val doc = firestore.collection("userProgress")
                .document(userId)
                .get()
                .await()

            if (doc.exists()) {
                Result.success(doc.toObject(UserProgress::class.java)!!)
            } else {
                // Create initial progress
                val initialProgress = UserProgress(userId = userId)
                firestore.collection("userProgress")
                    .document(userId)
                    .set(initialProgress)
                    .await()
                Result.success(initialProgress)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============= STREAK MANAGEMENT =============

    /**
     * Get user's streak data
     * Also checks if streak should be reset due to inactivity
     */
    suspend fun getStreakData(userId: String): Result<StreakData> {
        return try {
            val doc = firestore.collection("streaks")
                .document(userId)
                .get()
                .await()

            if (doc.exists()) {
                val streak = doc.toObject(StreakData::class.java)
                if (streak != null) {
                    streak.id = doc.id
                    
                    // Check if streak should be reset due to inactivity
                    val now = Date()
                    val daysSinceLastWalk = StreakData.getDaysBetween(streak.lastWalkDate, now)
                    
                    // If more than 1 day has passed since last walk, reset the streak
                    if (daysSinceLastWalk > 1 && streak.currentStreak > 0) {
                        android.util.Log.d("FirebaseService", "💔 Streak broken due to inactivity. Days since last walk: $daysSinceLastWalk")
                        
                        val resetStreak = streak.copy(
                            currentStreak = 0,
                            // Keep other stats like longestStreak and totalDaysWalked
                        )
                        
                        // Update Firestore with reset streak
                        firestore.collection("streaks")
                            .document(userId)
                            .set(resetStreak)
                            .await()
                        
                        Result.success(resetStreak)
                    } else {
                        Result.success(streak)
                    }
                } else {
                    Result.failure(Exception("Failed to parse streak data"))
                }
            } else {
                // Create initial streak
                val initialStreak = StreakData(userId = userId)
                firestore.collection("streaks")
                    .document(userId)
                    .set(initialStreak)
                    .await()
                Result.success(initialStreak)
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error getting streak data", e)
            Result.failure(e)
        }
    }

suspend fun updateStreakAfterWalk(walk1: Walk): Result<Int> {
    return try {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        
        // Get current streak (this will auto-reset if needed)
        val currentStreak = getStreakData(userId).getOrNull() 
            ?: StreakData(userId = userId)

        val now = Date()
        val daysSinceLastWalk = StreakData.getDaysBetween(currentStreak.lastWalkDate, now)

        val updatedStreak = when {
            // Same day - just update last walk date
            daysSinceLastWalk == 0 -> {
                currentStreak.copy(lastWalkDate = now)
            }
            // Streak is 0 (was reset) - start new streak
            currentStreak.currentStreak == 0 -> {
                android.util.Log.d("FirebaseService", "🔥 Starting new streak!")
                currentStreak.copy(
                    currentStreak = 1,
                    longestStreak = maxOf(1, currentStreak.longestStreak),
                    lastWalkDate = now,
                    streakStartDate = now,
                    totalDaysWalked = currentStreak.totalDaysWalked + 1
                )
            }
            // Next day - increment streak
            daysSinceLastWalk == 1 -> {
                val newStreak = currentStreak.currentStreak + 1
                val milestoneBonus = calculateStreakMilestoneBonus(newStreak, currentStreak.currentStreak)
                
                if (milestoneBonus > 0) {
                    awardXP(userId, milestoneBonus)
                    android.util.Log.d("FirebaseService", "🎉 Streak milestone! +$milestoneBonus XP")
                }
                
                currentStreak.copy(
                    currentStreak = newStreak,
                    longestStreak = maxOf(newStreak, currentStreak.longestStreak),
                    lastWalkDate = now,
                    totalDaysWalked = currentStreak.totalDaysWalked + 1,
                    sevenDayStreakCount = if (newStreak % 7 == 0) currentStreak.sevenDayStreakCount + 1 else currentStreak.sevenDayStreakCount,
                    fourteenDayStreakCount = if (newStreak % 14 == 0) currentStreak.fourteenDayStreakCount + 1 else currentStreak.fourteenDayStreakCount
                )
            }
            // More than 1 day gap - start fresh (this shouldn't happen often since getStreakData resets it)
            else -> {
                android.util.Log.d("FirebaseService", "💔 Streak broken after ${currentStreak.currentStreak} days")
                currentStreak.copy(
                    currentStreak = 1,
                    lastWalkDate = now,
                    streakStartDate = now,
                    totalDaysWalked = currentStreak.totalDaysWalked + 1
                )
            }
        }

        // Save updated streak
        firestore.collection("streaks")
            .document(userId)
            .set(updatedStreak)
            .await()

        android.util.Log.d("FirebaseService", "Streak updated: ${updatedStreak.currentStreak} days")
        Result.success(0) // No bonus here, already awarded in saveWalk
    } catch (e: Exception) {
        android.util.Log.e("FirebaseService", "Error updating streak", e)
        Result.failure(e)
    }
}

/**
 * Award one-time milestone bonuses for streak achievements
 */
private fun calculateStreakMilestoneBonus(newStreak: Int, oldStreak: Int): Int {
    return when (newStreak) {
        7 -> if (oldStreak < 7) 100 else 0     // First 7-day streak
        30 -> if (oldStreak < 30) 500 else 0   // First 30-day streak
        90 -> if (oldStreak < 90) 1500 else 0  // First 90-day streak
        365 -> if (oldStreak < 365) 5000 else 0 // First year streak!
        else -> 0
    }
}
    /**
     * Get current user's streak data
     */
    suspend fun getCurrentUserStreak(): Result<StreakData> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            getStreakData(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Call this after a walk is completed to update challenge progress
    suspend fun updateChallengesAfterWalk(walk: Walk) {
        try {
            val challenges = getDailyChallenges().getOrNull() ?: return

            challenges.forEach { userChallenge ->
                if (!userChallenge.isCompleted) {
                    val newProgress = when (userChallenge.challenge.type) {
                        ChallengeType.DISTANCE -> {
                            userChallenge.currentProgress + (walk.distanceCovered / 1000.0) // Convert to km
                        }
                        ChallengeType.DURATION -> {
                            userChallenge.currentProgress + (walk.duration / 60000.0) // Convert to minutes
                        }
                        ChallengeType.WALKS_COUNT -> {
                            userChallenge.currentProgress + 1.0
                        }
                        ChallengeType.SPEED -> {
                            val avgSpeed = (walk.distanceCovered / 1000.0) / (walk.duration / 3600000.0)
                            if (avgSpeed >= userChallenge.challenge.targetValue) {
                                userChallenge.challenge.targetValue // Complete it
                            } else {
                                userChallenge.currentProgress
                            }
                        }
                    }

                    updateChallengeProgress(userChallenge.id, newProgress)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error updating challenges after walk", e)
        }
    }

    /**
     * Enhanced registration that provides better error messages
     * Now includes automatic verification email sending
     */
    suspend fun registerWithEmailEnhanced(email: String, password: String, username: String): Result<User> {
        return try {
            android.util.Log.d("FirebaseService", "Starting enhanced registration for: $email")

            // First check if username is already taken (Case Insensitive Check)
            val usernameVariations = listOf(
                username,
                username.lowercase(Locale.getDefault()), 
                username.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            ).distinct()

            for (variation in usernameVariations) {
                val query = firestore.collection(USERS_COLLECTION)
                    .whereEqualTo("username", variation)
                    .limit(1).get().await()
                    
                if (!query.isEmpty) {
                    return Result.failure(Exception("Username '$variation' is already taken. Please choose a different one."))
                }
            }

            // Try to create the account
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                // Update display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(username)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()

                // Send verification email immediately
                try {
                    android.util.Log.d("FirebaseService", "Sending verification email...")
                    firebaseUser.sendEmailVerification().await()
                    android.util.Log.d("FirebaseService", "✅ Verification email sent to: $email")
                } catch (emailError: Exception) {
                    android.util.Log.e("FirebaseService", "Failed to send verification email", emailError)
                    // Continue anyway - user can request it later
                }

                // Create user document in Firestore
                val user = User(
                    id = firebaseUser.uid,
                    username = username,
                    email = email,
                    totalDistanceWalked = 0.0,
                    totalWalks = 0,
                    lastWalkDate = System.currentTimeMillis(),
                    isAnonymous = false
                )

                firestore.collection(USERS_COLLECTION)
                    .document(firebaseUser.uid)
                    .set(user)
                    .await()

                android.util.Log.d("FirebaseService", "✅ Registration successful: ${firebaseUser.uid}")

                // Sign out immediately so they go to verification screen
                auth.signOut()

                Result.success(user)
            } else {
                Result.failure(Exception("Registration failed: No user data returned"))
            }
        } catch (e: FirebaseAuthException) {
            android.util.Log.e("FirebaseService", "Firebase Auth error: ${e.errorCode}", e)
            val errorMessage = when (e.errorCode) {
                "ERROR_EMAIL_ALREADY_IN_USE" -> {
                    "This email is already registered. If you can't access your account:\n\n" +
                            "• If you never verified your email, please contact support\n" +
                            "• If you forgot your password, use 'Forgot Password' on the login screen\n" +
                            "• Or try logging in with this email"
                }
                "ERROR_INVALID_EMAIL" -> "Invalid email address format"
                "ERROR_WEAK_PASSWORD" -> "Password is too weak. Please use at least 6 characters"
                else -> "Registration failed: ${e.message}"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Registration failed", e)
            Result.failure(Exception("Registration failed: ${e.message}"))
        }
    }

    /**
     * Sign in with Google credential
     * Industry best practice: Auto-create account for new users, direct login for existing users
     */
    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<User> {
        return try {
            // Add global timeout of 15 seconds to prevent infinite loading
            withTimeout(15000L) {
                android.util.Log.d("FirebaseService", "🔐 Starting Google Sign-In: ${account.email}")
    
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                android.util.Log.d("FirebaseService", "🔐 Credential created, signing in with Firebase Auth...")
                
                val result = auth.signInWithCredential(credential).await()
                val firebaseUser = result.user
                android.util.Log.d("FirebaseService", "🔐 Firebase Auth sign-in complete. User: ${firebaseUser?.uid}")
    
                if (firebaseUser != null) {
                    // Update Firebase Auth profile with Google display name
                    if (firebaseUser.displayName == null && account.displayName != null) {
                        try {
                            android.util.Log.d("FirebaseService", "👤 Updating profile name...")
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(account.displayName)
                                .build()
                            firebaseUser.updateProfile(profileUpdates).await()
                        } catch (e: Exception) {
                            android.util.Log.w("FirebaseService", "⚠️ Failed to update profile name", e)
                        }
                    }
    
                    // Check if user exists in Firestore
                    android.util.Log.d("FirebaseService", "📂 Checking Firestore for user doc...")
                    val userDoc = firestore.collection(USERS_COLLECTION)
                        .document(firebaseUser.uid)
                        .get()
                        .await()
    
                    val user: User
                    if (userDoc.exists()) {
                        // Existing user - direct login
                        android.util.Log.d("FirebaseService", "👤 User doc exists, parsing...")
                        val existingUser = userDoc.toObject(User::class.java)
                        if (existingUser != null) {
                            user = existingUser
                            android.util.Log.d("FirebaseService", "✅ Existing user logged in: ${user.username}")
                        } else {
                            android.util.Log.e("FirebaseService", "❌ Failed to parse existing user data")
                            throw Exception("Failed to parse user data")
                        }
                    } else {
                        // New user - auto-create account (industry standard for OAuth)
                        android.util.Log.d("FirebaseService", "🆕 New Google user - creating account")
                        
                        val username = (account.displayName ?: account.email?.substringBefore("@") ?: "User").lowercase()
                        
                        user = User(
                            id = firebaseUser.uid,
                            username = username,
                            email = firebaseUser.email ?: "",
                            isAnonymous = false
                        )
                        
                        // Create user document
                        android.util.Log.d("FirebaseService", "💾 Saving new user to Firestore...")
                        firestore.collection(USERS_COLLECTION)
                            .document(firebaseUser.uid)
                            .set(user)
                            .await()
                        
                        // Initialize gamification features for new user
                        try {
                            android.util.Log.d("FirebaseService", "🎮 Initializing gamification...")
                            // Assign daily challenges
                            assignDailyChallenges(firebaseUser.uid)
                            
                            // Initialize streak data
                            val initialStreak = StreakData(userId = firebaseUser.uid)
                            firestore.collection("streaks")
                                .document(firebaseUser.uid)
                                .set(initialStreak)
                                .await()
                            android.util.Log.d("FirebaseService", "✅ Gamification initialized")
                            
                        } catch (e: Exception) {
                            android.util.Log.w("FirebaseService", "⚠️ Failed to initialize some features, will retry later", e)
                        }
                        
                        android.util.Log.d("FirebaseService", "🎉 New user account created successfully: $username")
                    }
    
                    Result.success(user)
                } else {
                    android.util.Log.e("FirebaseService", "❌ Google sign-in failed: No Firebase user")
                    Result.failure(Exception("Google sign-in failed: No Firebase user"))
                }
            }
        } catch (e: TimeoutCancellationException) {
            android.util.Log.e("FirebaseService", "⏰ Google sign-in timed out", e)
            Result.failure(Exception("Sign-in timed out. Please check your connection."))
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "❌ Google sign-in failed", e)
            val errorMessage = when (e) {
                is FirebaseAuthUserCollisionException -> 
                    "An account with this email already exists using a different sign-in method. Please sign in with your email/password first, then link Google in settings."
                is FirebaseAuthException -> {
                    if (e.errorCode == "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL") {
                        "This email is already registered. Please login with your original method (e.g. Email/Password)."
                    } else {
                        e.message ?: "Google sign-in failed"
                    }
                }
                else -> e.message ?: "Google sign-in failed"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    // ============= STATS METHODS =============

    suspend fun getWeeklyStats(): Result<WeeklyStats> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))

            // Use Calendar for date calculations
            val oneWeekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
            val twoWeeksAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -14) }
            
            val oneWeekAgoTime = oneWeekAgo.time.time
            val twoWeeksAgoTime = twoWeeksAgo.time.time

            // Fetch ALL walks for the user (avoids composite index requirements)
            // This is safer for immediate usage. Optimization: limit(500) if needed.
            val allWalksSnapshot = firestore.collection(WALKS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
                
            val allWalks = allWalksSnapshot.documents.mapNotNull { it.toObject(Walk::class.java) }
                .filter { it.status == "VALID" }

            // Filter in memory
            val thisWeekWalks = allWalks.filter { 
                it.timestamp.time >= oneWeekAgoTime 
            }

            val lastWeekWalks = allWalks.filter { 
                it.timestamp.time >= twoWeeksAgoTime && it.timestamp.time < oneWeekAgoTime 
            }

            // Calculate stats
            val currentWalks = thisWeekWalks.size
            val currentDistance = thisWeekWalks.sumOf { it.distanceCovered } / 1000.0 // km
            val currentTime = thisWeekWalks.sumOf { it.duration } / 60000 // minutes

            val lastWalks = lastWeekWalks.size
            val lastDistance = lastWeekWalks.sumOf { it.distanceCovered } / 1000.0
            val lastTime = lastWeekWalks.sumOf { it.duration } / 60000

            // Calculate percentage changes
            val walksChange = calculateChange(currentWalks.toDouble(), lastWalks.toDouble())
            val distanceChange = calculateChange(currentDistance, lastDistance)
            val timeChange = calculateChange(currentTime.toDouble(), lastTime.toDouble())

            val overallChange = (walksChange + distanceChange + timeChange) / 3

            Result.success(
                WeeklyStats(
                    totalWalks = currentWalks,
                    totalTimeMinutes = currentTime,
                    totalDistanceKm = currentDistance,
                    walksChangePercent = walksChange,
                    timeChangePercent = timeChange,
                    distanceChangePercent = distanceChange,
                    overallChangePercent = overallChange
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error getting weekly stats", e)
            Result.failure(e)
        }
    }

    private fun calculateChange(current: Double, previous: Double): Double {
        return if (previous > 0) {
            ((current - previous) / previous) * 100
        } else if (current > 0) {
            100.0 // 100% increase if previous was 0 but current is > 0
        } else {
            0.0
        }
    }
    /**
     * Sign out current user
     */
    fun signOut() {
        auth.signOut()
        android.util.Log.d("FirebaseService", "User signed out")
    }

    /**
     * Get current Firebase user
     */
    fun getCurrentUser() = auth.currentUser

    /**
     * Get current user data from Firestore
     */
    suspend fun getCurrentUserData(): Result<User> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Result.failure(Exception("No user signed in"))
            } else {
                val userDoc = firestore.collection(USERS_COLLECTION)
                    .document(currentUser.uid)
                    .get()
                    .await()

                val user = userDoc.toObject(User::class.java)
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("User data not found"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============= WALK MANAGEMENT =============

    /**
     * Delete a walk from Firestore
     */
    suspend fun deleteWalk(walkId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Result.failure(Exception("User not authenticated"))
            } else {
                // Get the walk document
                val walkDoc = firestore.collection(WALKS_COLLECTION)
                    .document(walkId)
                    .get()
                    .await()

                val walk = walkDoc.toObject(Walk::class.java)

                if (walk != null && walk.userId == currentUser.uid) {
                    // Delete the walk
                    firestore.collection(WALKS_COLLECTION)
                        .document(walkId)
                        .delete()
                        .await()

                    // Update user statistics (subtract the walk stats)
                    updateUserStatsAfterDelete(walk)

                    android.util.Log.d("FirebaseService", "Walk deleted successfully: $walkId")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Walk not found or unauthorized"))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error deleting walk", e)
            Result.failure(Exception("Failed to delete walk: ${e.message}"))
        }
    }
    /**
     * Update user stats after deleting a walk
     */
    private suspend fun updateUserStatsAfterDelete(walk: Walk) {
        try {
            val userRef = firestore.collection(USERS_COLLECTION).document(walk.userId)
            val userDoc = userRef.get().await()

            if (userDoc.exists()) {
                val currentUser = userDoc.toObject(User::class.java) ?: User()
                val updatedUser = currentUser.copy(
                    totalDistanceWalked = (currentUser.totalDistanceWalked - walk.distanceCovered).coerceAtLeast(0.0),
                    totalWalks = (currentUser.totalWalks - 1).coerceAtLeast(0)
                )
                userRef.set(updatedUser).await()
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error updating user stats after delete", e)
        }
    }

    suspend fun saveWalk(walk: Walk): Result<String> {
    return try {
        val userId = getCurrentUser()?.uid ?: return Result.failure(Exception("User not authenticated"))
        val username = getCurrentUser()?.displayName ?: "Anonymous"

        val walkData = hashMapOf(
            "userId" to userId,
            "username" to username,
            "polylineCoordinates" to walk.polylineCoordinates,
            "distanceCovered" to walk.distanceCovered,
            "timestamp" to walk.timestamp,
            "duration" to walk.duration,
            "mode" to walk.mode,
            "capturedPolygon" to walk.capturedPolygon,
            "capturedAreaM2" to walk.capturedAreaM2,
            "status" to walk.status
        )

        // Save walk to Firestore
        val docRef = firestore.collection("walks").add(walkData).await()
        val walkId = docRef.id
        
        android.util.Log.d("FirebaseService", "✅ Walk saved successfully: $walkId (mode=${walk.mode})")

        // ── ANTI-CHEAT: BYPASS REWARDS ──
        if (walk.status != "VALID") {
            android.util.Log.d("FirebaseService", "⚠️ Walk rejected (status=${walk.status}), skipping rewards and territory.")
            return Result.success(walkId)
        }

        // For COMPETE mode walks with a polygon, save territory
        if (walk.mode == "COMPETE" && walk.capturedPolygon.size >= 3 && walk.capturedAreaM2 > 0) {
            try {
                android.util.Log.d("FirebaseService", "✅ Creating territory for COMPETE walk: polygon=${walk.capturedPolygon.size} points, area=${walk.capturedAreaM2} m²")
                val territory = Territory(
                    ownerId = userId,
                    ownerUsername = username,
                    polygon = walk.capturedPolygon,
                    areaM2 = walk.capturedAreaM2,
                    sourceWalkId = walkId,
                    minLat = walk.capturedPolygon.minOf { it.latitude },
                    maxLat = walk.capturedPolygon.maxOf { it.latitude },
                    minLng = walk.capturedPolygon.minOf { it.longitude },
                    maxLng = walk.capturedPolygon.maxOf { it.longitude }
                )
                saveTerritory(territory)
                android.util.Log.d("FirebaseService", "✅ Territory saved for compete walk $walkId")
            } catch (e: Exception) {
                android.util.Log.w("FirebaseService", "⚠️ Failed to save territory for walk $walkId", e)
            }
        } else {
            android.util.Log.d("FirebaseService", "⚠️ Not saving territory: mode=${walk.mode}, polygonSize=${walk.capturedPolygon.size}, area=${walk.capturedAreaM2}")
        }


        // Track which operations succeed/fail
        var streakUpdated = false
        var xpAwarded = false
        var statsUpdated = false
        var dailyActivityUpdated = false

        // Calculate base XP for this walk (distance-based)
        val baseWalkXP = calculateWalkXP(walk)
        
        // Get current streak and calculate bonus
        val streakData = getStreakData(userId).getOrNull() ?: StreakData(userId = userId)
        val streakBonusXP = com.sidhart.walkover.utils.StreakManager.calculateStreakBonusXP(
            currentStreak = streakData.currentStreak,
            baseWalkXP = baseWalkXP
        )
        
        // Award base XP + streak bonus
        val totalXP = baseWalkXP + streakBonusXP
        
        try {
            awardXP(userId, totalXP)
            xpAwarded = true
            android.util.Log.d("FirebaseService", "✅ XP awarded: $totalXP")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "❌ Failed to award XP", e)
            // Queue for retry
            offlineQueueManager?.addPendingOperation(
                PendingOperation(
                    type = OperationType.AWARD_XP,
                    walkId = walkId,
                    userId = userId,
                    timestamp = walk.timestamp,
                    data = mapOf("xp" to totalXP)
                )
            )
        }
        
        try {
            updateDailyActivity(totalXP)
            dailyActivityUpdated = true
            android.util.Log.d("FirebaseService", "✅ Daily activity updated")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "❌ Failed to update daily activity", e)
            offlineQueueManager?.addPendingOperation(
                PendingOperation(
                    type = OperationType.UPDATE_DAILY_ACTIVITY,
                    walkId = walkId,
                    userId = userId,
                    timestamp = walk.timestamp
                )
            )
        }

        if (streakBonusXP > 0) {
            android.util.Log.d("FirebaseService", "🔥 Streak bonus: +$streakBonusXP XP (${streakData.currentStreak} day streak)")
        }

        // Update streak after walk
        try {
            streakUpdated = true
            android.util.Log.d("FirebaseService", "✅ Streak updated")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "❌ Failed to update streak", e)
            offlineQueueManager?.addPendingOperation(
                PendingOperation(
                    type = OperationType.UPDATE_STREAK,
                    walkId = walkId,
                    userId = userId,
                    timestamp = walk.timestamp
                )
            )
        }

        // Update user stats with Monthly Logic
        try {
            val userRef = firestore.collection("users").document(userId)
            val currentMonthId = java.text.SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentUser = snapshot.toObject(User::class.java) ?: User()
                
                val currentDistance = currentUser.totalDistanceWalked
                val currentWalks = currentUser.totalWalks
                
                // Check for month change
                val storedMonthId = currentUser.currentMonthId
                val monthlyDistance = if (storedMonthId == currentMonthId) {
                    currentUser.monthlyDistanceWalked + walk.distanceCovered
                } else {
                    // New month! Reset monthly distance
                    walk.distanceCovered
                }

                // Update User
                transaction.update(
                    userRef,
                    mapOf(
                        "totalDistanceWalked" to (currentDistance + walk.distanceCovered),
                        "totalWalks" to (currentWalks + 1),
                        "lastWalkDate" to System.currentTimeMillis(),
                        "monthlyDistanceWalked" to monthlyDistance,
                        "currentMonthId" to currentMonthId
                    )
                )
                
                // Update Monthly Leaderboard Entry
                val monthlyRef = firestore.collection("monthly_leaderboards")
                    .document(currentMonthId)
                    .collection("users")
                    .document(userId)
                    
                val monthlyData = mapOf(
                    "userId" to userId,
                    "username" to username,
                    "distance" to monthlyDistance,
                    "updatedAt" to System.currentTimeMillis()
                )
                
                transaction.set(monthlyRef, monthlyData)

                storedMonthId
            }.await().let { oldMonthId ->
                // Post-transaction: Check for rewards if month changed
                if (oldMonthId != null && oldMonthId != currentMonthId && oldMonthId.isNotEmpty()) {
                    checkAndAwardMonthlyRewards(userId, oldMonthId)
                }
            }
            
            statsUpdated = true
            android.util.Log.d("FirebaseService", "✅ User stats updated")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "❌ Failed to update user stats", e)
            offlineQueueManager?.addPendingOperation(
                PendingOperation(
                    type = OperationType.UPDATE_USER_STATS,
                    walkId = walkId,
                    userId = userId,
                    timestamp = walk.timestamp,
                    data = mapOf(
                        "distance" to walk.distanceCovered,
                        "duration" to walk.duration
                    )
                )
            )
        }

        // Update challenges after walk
        var challengesUpdated = false
        try {
            updateChallengesAfterWalk(walk)
            challengesUpdated = true
            android.util.Log.d("FirebaseService", "✅ Challenges updated")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "❌ Failed to update challenges", e)
            offlineQueueManager?.addPendingOperation(
                PendingOperation(
                    type = OperationType.UPDATE_CHALLENGES,
                    walkId = walkId,
                    userId = userId,
                    timestamp = walk.timestamp,
                    data = mapOf(
                        "distance" to walk.distanceCovered,
                        "duration" to walk.duration
                    )
                )
            )
        }

        // Log summary
        val failedOperations = mutableListOf<String>()
        if (!streakUpdated) failedOperations.add("streak")
        if (!xpAwarded) failedOperations.add("XP")
        if (!statsUpdated) failedOperations.add("stats")
        if (!dailyActivityUpdated) failedOperations.add("daily activity")
        if (!challengesUpdated) failedOperations.add("challenges")
        
        if (failedOperations.isNotEmpty()) {
            android.util.Log.w("FirebaseService", "⚠️ Walk saved but some operations failed: ${failedOperations.joinToString(", ")}")
            android.util.Log.w("FirebaseService", "📋 Failed operations queued for retry when internet is restored")
        }
        // Handle Duel Challenge update if applicable
        if (walk.mode == "DUEL") {
            try {
                val duelResult = getActiveDuel()
                val activeDuel = duelResult.getOrNull()
                if (activeDuel != null) {
                    val isChallenger = activeDuel.challengerId == userId
                    val additionalKm = walk.distanceCovered / 1000.0
                    val currentKm = if (isChallenger) activeDuel.challengerDistanceKm else activeDuel.opponentDistanceKm
                    updateDuelDistance(activeDuel.id, isChallenger, currentKm + additionalKm)
                    android.util.Log.d("FirebaseService", "✅ Duel distance updated for walk $walkId")
                }
            } catch (e: Exception) {
                android.util.Log.e("FirebaseService", "❌ Failed to update Duel distance", e)
            }
        }

        Result.success(walkId)
    } catch (e: Exception) {
        android.util.Log.e("FirebaseService", "Error saving walk", e)
        Result.failure(e)
    }
}

private suspend fun checkAndAwardMonthlyRewards(userId: String, monthId: String) {
    try {
        val leaderboardRef = firestore.collection("monthly_leaderboards")
            .document(monthId)
            .collection("users")
            .orderBy("distance", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(3)
            .get()
            .await()

        var rank = 0
        var reward = 0

        for ((index, doc) in leaderboardRef.documents.withIndex()) {
            if (doc.id == userId) {
                rank = index + 1
                reward = when (rank) {
                    1 -> 100
                    2 -> 50
                    3 -> 25
                    else -> 0
                }
                break
            }
        }

        if (reward > 0) {
            awardXP(userId, reward)
            android.util.Log.d("FirebaseService", "🏆 Monthly Reward for $monthId: Rank $rank, +$reward XP")
        }
    } catch (e: Exception) {
        android.util.Log.e("FirebaseService", "Error checking monthly rewards", e)
    }
}

/**
 * Calculate base XP for a walk (distance-based)
 */
private fun calculateWalkXP(walk: Walk): Int {
    val distanceKm = walk.distanceCovered / 1000.0
    return when {
        distanceKm >= 10.0 -> 100  // 10km+ = 100 XP
        distanceKm >= 5.0 -> 75    // 5-10km = 75 XP
        distanceKm >= 3.0 -> 50    // 3-5km = 50 XP
        distanceKm >= 1.0 -> 30    // 1-3km = 30 XP
        distanceKm >= 0.5 -> 20    // 0.5-1km = 20 XP
        else -> 10                 // < 0.5km = 10 XP
    }
}

    suspend fun getWalks(): Result<List<Walk>> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                android.util.Log.e("FirebaseService", "No authenticated user")
                return Result.failure(Exception("User not authenticated"))
            }

            android.util.Log.d("FirebaseService", "Fetching walks for user: ${currentUser.uid}")

            // Get documents and manually set the ID to ensure it's populated
            val querySnapshot = firestore.collection(WALKS_COLLECTION)
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()

            val walks = querySnapshot.documents.mapNotNull { document ->
                try {
                    val walk = document.toObject(Walk::class.java)
                    if (walk != null) {
                        // Manually set the document ID since @get:Exclude might prevent auto-population
                        walk.id = document.id
                        android.util.Log.d("FirebaseService", "Loaded walk with ID: ${walk.id}")
                        walk
                    } else {
                        android.util.Log.w("FirebaseService", "Failed to deserialize walk document: ${document.id}")
                        null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseService", "Error parsing walk document: ${document.id}", e)
                    null
                }
            }.filter { it.status == "VALID" }.sortedByDescending { it.timestamp }

            android.util.Log.d("FirebaseService", "Successfully loaded ${walks.size} walks")

            Result.success(walks)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error getting walks", e)
            Result.failure(Exception("Failed to load walks: ${e.message}"))
        }
    }

    // ============= PROFILE MANAGEMENT =============

    /**
     * Check if user is authenticated AND email is verified
     */
    fun isUserAuthenticated(): Boolean {
        return try {
            val currentUser = auth.currentUser

            if (currentUser == null) {
                android.util.Log.d("FirebaseService", "No user logged in")
                return false
            }

            // Anonymous users don't need email verification
            if (currentUser.isAnonymous) {
                android.util.Log.d("FirebaseService", "Anonymous user - no verification needed")
                return true
            }

            // Google users don't need email verification (it's handled by Google)
            // This fixes the issue where Google Sign-In users might be blocked if firebase thinks they are unverified
            val isGoogle = currentUser.providerData.any { it.providerId == "google.com" }
            if (isGoogle) {
                android.util.Log.d("FirebaseService", "Google user - bypassing verification check")
                return true
            }

            // Regular users must have verified email
            val isVerified = currentUser.isEmailVerified
            android.util.Log.d("FirebaseService", "User: ${currentUser.uid}, Email verified: $isVerified")

            isVerified
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error checking auth status", e)
            false
        }
    }

    /**
 * Update daily activity record after walk
 */
suspend fun updateDailyActivity( xpEarned: Int) {
    try {
        val userId = auth.currentUser?.uid ?: return
        val today = ChallengeManager.getTodayMidnight()
        
        // Get or create today's activity record
        val activityRef = firestore.collection("dailyActivity")
            .document("${userId}_${today.time}")
        
        val activityDoc = activityRef.get().await()
        
        val currentActivity = if (activityDoc.exists()) {
            activityDoc.toObject(DailyActivityRecord::class.java) ?: DailyActivityRecord(userId = userId, date = today)
        } else {
            DailyActivityRecord(userId = userId, date = today)
        }
        
        // Check challenges completed today
        val challenges = getDailyChallenges().getOrNull() ?: emptyList()
        val completedToday = challenges.count { it.isCompleted }
        
        // Calculate total distance today
        val todayWalks = getTodayWalks()
        val totalDistance = todayWalks.sumOf { it.distanceCovered }
        
        val hasMinActivity = completedToday >= 1  // Only challenges count for streak
        
        val updatedActivity = currentActivity.copy(
            walksCompleted = currentActivity.walksCompleted + 1,
            distanceCovered = totalDistance,
            challengesCompleted = completedToday,
            xpEarned = currentActivity.xpEarned + xpEarned,
            hasMinimumActivity = hasMinActivity
        )
        
        activityRef.set(updatedActivity).await()
        
    } catch (e: Exception) {
        android.util.Log.e("FirebaseService", "Error updating daily activity", e)
    }
}

private suspend fun getTodayWalks(): List<Walk> {
    val allWalks = getWalks().getOrNull() ?: emptyList()
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
    
    return allWalks.filter { it.timestamp >= todayStart }
}

/**
 * Get daily activity records for heat map
 * @param days Number of days to fetch (30, 90, 180, 365)
 */
suspend fun getDailyActivityRecords(days: Int = 30): Result<List<DailyActivityRecord>> {
    return try {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        
        val startDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val records = firestore.collection("dailyActivity")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("date", startDate)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(DailyActivityRecord::class.java) }
            .sortedBy { it.date }
        
        Result.success(records)
    } catch (e: Exception) {
        android.util.Log.e("FirebaseService", "Error getting daily activity", e)
        Result.failure(e)
    }
}
    /**
     * Sign in with email OR username - CHECKS EMAIL VERIFICATION
     */
    suspend fun signInWithEmailOrUsername(emailOrUsername: String, password: String): Result<User> {
        return try {
            // Trim inputs
            val trimmedInput = emailOrUsername.trim()
            val trimmedPassword = password.trim()

            android.util.Log.d("FirebaseService", "Attempting sign-in for: $trimmedInput")

            // Check if input looks like an email
            val isEmail = trimmedInput.contains("@")

            val email = if (isEmail) {
                trimmedInput
            } else {
                // Username login - find email first (Case Insensitive Lookup)
                android.util.Log.d("FirebaseService", "Looking up email for username: $trimmedInput")
                
                var foundEmail: String? = null
                
                // Try variations: Exact -> Lowercase -> Capitalized
                val variations = listOf(
                    trimmedInput,
                    trimmedInput.lowercase(Locale.getDefault()),
                    trimmedInput.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                ).distinct()
                
                for (variation in variations) {
                    android.util.Log.d("FirebaseService", "Checking variation: $variation")
                    val query = firestore.collection(USERS_COLLECTION)
                        .whereEqualTo("username", variation)
                        .limit(1).get().await()
                        
                    if (!query.isEmpty) {
                        foundEmail = query.documents[0].getString("email")
                        if (!foundEmail.isNullOrBlank()) break
                    }
                }

                if (foundEmail.isNullOrBlank()) {
                    return Result.failure(Exception("No account found with username: $trimmedInput"))
                }

                android.util.Log.d("FirebaseService", "Found email for username")
                foundEmail
            }

            // Sign in with email
            android.util.Log.d("FirebaseService", "Signing in with email...")
            val result = auth.signInWithEmailAndPassword(email, trimmedPassword).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                // CHECK EMAIL VERIFICATION
                if (!firebaseUser.isEmailVerified) {
                    android.util.Log.w("FirebaseService", "Email not verified for user: ${firebaseUser.uid}")

                    // Sign out the user since they're not verified
                    auth.signOut()

                    return Result.failure(Exception("EMAIL_NOT_VERIFIED|${firebaseUser.email}"))
                }

                // Fetch user data from Firestore
                val userDoc = firestore.collection(USERS_COLLECTION)
                    .document(firebaseUser.uid)
                    .get()
                    .await()

                val user = userDoc.toObject(User::class.java) ?: User(
                    id = firebaseUser.uid,
                    username = firebaseUser.displayName ?: "User",
                    email = firebaseUser.email ?: email,
                    isAnonymous = false
                )

                android.util.Log.d("FirebaseService", "Sign-in successful: ${firebaseUser.uid}")
                Result.success(user)
            } else {
                Result.failure(Exception("Sign-in failed: No user data"))
            }
        } catch (e: FirebaseAuthException) {
            android.util.Log.e("FirebaseService", "Firebase Auth Exception", e)

            val errorMessage = when {
                e.message?.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) == true ||
                        e.message?.contains("invalid-credential", ignoreCase = true) == true ->
                    "Invalid email/username or password"

                e.errorCode == "ERROR_INVALID_EMAIL" -> "Invalid email format"
                e.errorCode == "ERROR_WRONG_PASSWORD" -> "Incorrect password"
                e.errorCode == "ERROR_USER_NOT_FOUND" -> "No account found"
                e.errorCode == "ERROR_USER_DISABLED" -> "Account disabled"
                e.errorCode == "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Try again later"

                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Check your internet connection"

                else -> {
                    android.util.Log.e("FirebaseService", "Unmapped error: ${e.errorCode}, ${e.message}")
                    "Sign-in failed. Check your credentials and try again"
                }
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Sign-in failed", e)

            val errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Check your internet connection"
                else ->
                    "Sign-in failed: ${e.message ?: "Unknown error"}"
            }

            Result.failure(Exception(errorMessage))
        }
    
    }

    // ============= TERRITORY CLEANUP =============

    /**
     * One-time Firestore cleanup: merge all overlapping / adjacent territories owned
     * by the current user into unified polygons.
     *
     * Same exhaustive buffer-aware merge as the map renderer, but this one actually
     * writes the results back to Firestore — deleting old fragmented documents and
     * saving one clean document per cluster.
     *
     * @return Number of territory documents removed (merged away)
     */
    suspend fun cleanupMyTerritories(): Result<Int> {
        return try {
            val userId   = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))
            val username = auth.currentUser?.displayName ?: "User"

            // Fetch ALL territories owned by this user
            val snapshot = firestore.collection(TERRITORIES_COLLECTION)
                .whereEqualTo("ownerId", userId)
                .get()
                .await()

            val myTerritories = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Territory::class.java)?.also { it.id = doc.id }
            }
            android.util.Log.d("FirebaseService", "🧹 Cleanup: found ${myTerritories.size} territories")

            if (myTerritories.size <= 1) {
                android.util.Log.d("FirebaseService", "🧹 Nothing to merge")
                return Result.success(0)
            }

            // ── Exhaustive merge (same algorithm as drawTerritories) ──────
            // Each cluster = (list of original Territory docs, merged convex hull)
            data class Cluster(
                val docs: MutableList<Territory>,
                var hull: List<org.osmdroid.util.GeoPoint>
            )

            val clusters = mutableListOf<Cluster>()

            myTerritories.forEach { territory ->
                val poly = territory.polygon.map {
                    org.osmdroid.util.GeoPoint(it.latitude, it.longitude)
                }
                var currentHull  = poly
                val currentDocs  = mutableListOf(territory)

                // Keep merging until no existing cluster touches currentHull
                var recheck = true
                while (recheck) {
                    recheck = false
                    val iter    = clusters.iterator()
                    val toMerge = mutableListOf<Cluster>()
                    while (iter.hasNext()) {
                        val existing = iter.next()
                        if (com.sidhart.walkover.utils.TerritoryConflict
                                .polygonsOverlapWithBuffer(currentHull, existing.hull, 80.0)) {
                            toMerge.add(existing)
                            iter.remove()
                        }
                    }
                    if (toMerge.isNotEmpty()) {
                        currentHull = com.sidhart.walkover.utils.ConvexHull.merge(
                            currentHull, toMerge.flatMap { it.hull }
                        )
                        toMerge.forEach { currentDocs.addAll(it.docs) }
                        recheck = true
                    }
                }
                clusters.add(Cluster(currentDocs, currentHull))
            }

            android.util.Log.d("FirebaseService",
                "🧹 Cleanup: ${myTerritories.size} territories → ${clusters.size} unified clusters")

            var removedCount = 0
            val colorIndex   = abs(userId.hashCode()) % 8
            var newTotalArea = 0.0

            clusters.forEach { cluster ->
                val mergedArea = com.sidhart.walkover.utils.MapUtils.calculatePolygonArea(cluster.hull)
                newTotalArea += mergedArea

                if (cluster.docs.size > 1) {
                    // Delete all old fragmented documents
                    cluster.docs.forEach { t ->
                        firestore.collection(TERRITORIES_COLLECTION).document(t.id).delete().await()
                        android.util.Log.d("FirebaseService", "🗑️ Deleted old territory ${t.id}")
                    }
                    removedCount += cluster.docs.size - 1  // net reduction

                    // Write one unified territory document
                    val unified = Territory(
                        ownerId       = userId,
                        ownerUsername = username,
                        polygon       = cluster.hull.map {
                            com.google.firebase.firestore.GeoPoint(it.latitude, it.longitude)
                        },
                        areaM2        = mergedArea,
                        colorIndex    = colorIndex,
                        sourceWalkId  = cluster.docs.first().sourceWalkId,
                        capturedAt    = cluster.docs.minOf { it.capturedAt },
                        updatedAt     = Date(),
                        minLat        = cluster.hull.minOf { it.latitude },
                        maxLat        = cluster.hull.maxOf { it.latitude },
                        minLng        = cluster.hull.minOf { it.longitude },
                        maxLng        = cluster.hull.maxOf { it.longitude }
                    )
                    val ref = firestore.collection(TERRITORIES_COLLECTION).add(unified).await()
                    android.util.Log.d("FirebaseService", "✅ Saved unified territory ${ref.id} (${mergedArea.toInt()} m²)")
                }
                // Single-doc clusters are left untouched
            }

            // Fix the user's totalTerritoryM2 to reflect the cleaned-up state
            firestore.collection(USERS_COLLECTION).document(userId)
                .update("totalTerritoryM2", newTotalArea)
                .await()

            android.util.Log.d("FirebaseService",
                "✅ Merge complete: removed $removedCount doc(s), total area = ${newTotalArea.toInt()} m²")
            Result.success(removedCount)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "❌ Cleanup failed", e)
            Result.failure(e)
        }
    }

    /**
     * Full cleanup: Phase 1 resolves cross-user territory conflicts using time as the tiebreaker,
     * then Phase 2 merges same-user adjacent/overlapping territories.
     *
     * Time rule:
     *   - If territory B (ANY user) covers ≥ 50% of territory A, AND B was captured AFTER A
     *     → A is deleted (B wins the contested land)
     *   - If < 50% overlap → both coexist (the newer walk didn't earn the older land)
     *
     * Returns Pair(sameUserMerged, crossUserResolved)
     */
    suspend fun cleanupAllConflicts(): Result<Pair<Int, Int>> {
        return try {
            val userId   = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))
            val username = auth.currentUser?.displayName ?: "User"

            // ── Fetch ALL territories ──────────────────────────────────────
            val snapshot = firestore.collection(TERRITORIES_COLLECTION)
                .orderBy("capturedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(500)
                .get()
                .await()

            val allTerritories = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Territory::class.java)?.also { it.id = doc.id }
            }.toMutableList()

            android.util.Log.d("FirebaseService", "🧹 Full cleanup: ${allTerritories.size} total territories")

            // ── Phase 1: Cross-user conflict resolution (time wins) ────────
            val toDelete = mutableSetOf<String>()  // doc IDs to delete

            // Compare every pair; skip already-marked ones
            for (i in allTerritories.indices) {
                val a = allTerritories[i]
                if (a.id in toDelete) continue
                val aPoly = a.polygon.map { org.osmdroid.util.GeoPoint(it.latitude, it.longitude) }

                for (j in i + 1 until allTerritories.size) {
                    val b = allTerritories[j]
                    if (b.id in toDelete) continue
                    if (a.ownerId == b.ownerId) continue   // Same owner — handled in Phase 2
                    val bPoly = b.polygon.map { org.osmdroid.util.GeoPoint(it.latitude, it.longitude) }

                    if (!com.sidhart.walkover.utils.TerritoryConflict.polygonsOverlap(aPoly, bPoly)) continue

                    // How much of the OLDER territory is covered by the NEWER one?
                    val aIsNewer = a.capturedAt.time >= b.capturedAt.time
                    val (newer, older, newerPoly, olderPoly) = if (aIsNewer)
                        listOf(a, b, aPoly, bPoly) as List<*>
                    else
                        listOf(b, a, bPoly, aPoly) as List<*>

                    @Suppress("UNCHECKED_CAST")
                    val newerPolyT = newerPoly as List<org.osmdroid.util.GeoPoint>
                    @Suppress("UNCHECKED_CAST")
                    val olderPolyT = olderPoly as List<org.osmdroid.util.GeoPoint>
                    val newerT = newer as Territory
                    val olderT = older as Territory

                    // What fraction of the OLDER territory is covered by the NEWER walk?
                    val fraction = com.sidhart.walkover.utils.TerritoryConflict
                        .overlapFraction(olderPolyT, newerPolyT, samples = 100)

                    android.util.Log.d("FirebaseService",
                        "⚔️ ${newerT.ownerUsername}(newer) covers ${String.format("%.0f", fraction*100)}% " +
                        "of ${olderT.ownerUsername}(older)")

                    if (fraction >= 0.40) {
                        // Newer walk covered ≥40% of older territory → newer wins, delete older
                        toDelete.add(olderT.id)
                        android.util.Log.d("FirebaseService",
                            "🗑️ Deleting older territory ${olderT.id} (${olderT.ownerUsername}) — covered ${String.format("%.0f", fraction*100)}% by newer")
                    }
                    // else: < 50% → both coexist, older territory stands
                }
            }

            // Delete cross-user losers and update their stats
            var crossResolved = 0
            toDelete.forEach { id ->
                val t = allTerritories.find { it.id == id } ?: return@forEach
                firestore.collection(TERRITORIES_COLLECTION).document(id).delete().await()
                // Subtract area from loser's user stats
                if (t.ownerId.isNotBlank() && t.areaM2 > 0) {
                    try {
                        firestore.collection(USERS_COLLECTION).document(t.ownerId)
                            .update("totalTerritoryM2", FieldValue.increment(-t.areaM2))
                            .await()
                    } catch (_: Exception) {}
                }
                crossResolved++
            }

            android.util.Log.d("FirebaseService", "⚔️ Phase 1 done: $crossResolved cross-user conflicts resolved")

            // ── Phase 2: Same-user merge (uses existing logic) ────────────
            val myTerritories = allTerritories
                .filter { it.ownerId == userId && it.id !in toDelete }

            if (myTerritories.size <= 1) {
                return Result.success(Pair(0, crossResolved))
            }

            data class Cluster(
                val docs: MutableList<Territory>,
                var hull: List<org.osmdroid.util.GeoPoint>
            )
            val clusters = mutableListOf<Cluster>()

            myTerritories.forEach { territory ->
                val poly = territory.polygon.map {
                    org.osmdroid.util.GeoPoint(it.latitude, it.longitude)
                }
                var currentHull = poly
                val currentDocs = mutableListOf(territory)
                var recheck = true
                while (recheck) {
                    recheck = false
                    val iter    = clusters.iterator()
                    val toMerge = mutableListOf<Cluster>()
                    while (iter.hasNext()) {
                        val existing = iter.next()
                        if (com.sidhart.walkover.utils.TerritoryConflict
                                .polygonsOverlapWithBuffer(currentHull, existing.hull, 80.0)) {
                            toMerge.add(existing)
                            iter.remove()
                        }
                    }
                    if (toMerge.isNotEmpty()) {
                        currentHull = com.sidhart.walkover.utils.ConvexHull.merge(
                            currentHull, toMerge.flatMap { it.hull }
                        )
                        toMerge.forEach { currentDocs.addAll(it.docs) }
                        recheck = true
                    }
                }
                clusters.add(Cluster(currentDocs, currentHull))
            }

            var sameUserMerged = 0
            val colorIndex     = abs(userId.hashCode()) % 8
            var newTotalArea   = 0.0

            clusters.forEach { cluster ->
                val mergedArea = com.sidhart.walkover.utils.MapUtils.calculatePolygonArea(cluster.hull)
                newTotalArea += mergedArea
                if (cluster.docs.size > 1) {
                    cluster.docs.forEach { t ->
                        firestore.collection(TERRITORIES_COLLECTION).document(t.id).delete().await()
                    }
                    sameUserMerged += cluster.docs.size - 1
                    val unified = Territory(
                        ownerId       = userId,
                        ownerUsername = username,
                        polygon       = cluster.hull.map {
                            com.google.firebase.firestore.GeoPoint(it.latitude, it.longitude)
                        },
                        areaM2        = mergedArea,
                        colorIndex    = colorIndex,
                        sourceWalkId  = cluster.docs.first().sourceWalkId,
                        capturedAt    = cluster.docs.minOf { it.capturedAt },
                        updatedAt     = Date(),
                        minLat        = cluster.hull.minOf { it.latitude },
                        maxLat        = cluster.hull.maxOf { it.latitude },
                        minLng        = cluster.hull.minOf { it.longitude },
                        maxLng        = cluster.hull.maxOf { it.longitude }
                    )
                    firestore.collection(TERRITORIES_COLLECTION).add(unified).await()
                    android.util.Log.d("FirebaseService", "✅ Merged ${cluster.docs.size} → 1 (${mergedArea.toInt()} m²)")
                }
            }

            firestore.collection(USERS_COLLECTION).document(userId)
                .update("totalTerritoryM2", newTotalArea)
                .await()

            android.util.Log.d("FirebaseService",
                "✅ Full cleanup done: $sameUserMerged same-user merged, $crossResolved cross-user resolved")
            Result.success(Pair(sameUserMerged, crossResolved))
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "❌ Full cleanup failed", e)
            Result.failure(e)
        }
    }

    /**
     * Save a new territory from a walk, with full conflict resolution:
     *  1. Fetch nearby territories (bounding box)
     *  2. Merge with own adjacent territories (80 m buffer)
     *  3. For enemy territories:
     *       - If new walk covers ≥ 50% of enemy's area → enemy is older, new walk wins → delete enemy
     *       - If < 50% → enemy stays, new walk hull is clipped to exclude enemy land
     *  4. Save the final clean hull
     *
     * Returns the saved territory document ID, or failure.
     */
    suspend fun resolveConflictsAndSave(
        newHullPoints: List<org.osmdroid.util.GeoPoint>,
        sourceWalkId: String,
    ): Result<String> {
        return try {
            val userId   = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))
            val username = auth.currentUser?.displayName ?: "User"
            val colorIndex = abs(userId.hashCode()) % 8
            val now = Date()

            if (newHullPoints.size < 3) return Result.failure(Exception("Hull too small"))

            // ── 1. Fetch nearby territories ───────────────────────────────
            val minLat = newHullPoints.minOf { it.latitude }
            val maxLat = newHullPoints.maxOf { it.latitude }
            val minLng = newHullPoints.minOf { it.longitude }
            val maxLng = newHullPoints.maxOf { it.longitude }

            val nearby = getTerritoriesInBoundingBox(minLat, maxLat, minLng, maxLng)
                .getOrElse { return Result.failure(it) }
                .filter { com.sidhart.walkover.utils.TerritoryConflict.polygonsOverlap(
                    newHullPoints,
                    it.polygon.map { p -> org.osmdroid.util.GeoPoint(p.latitude, p.longitude) }
                )}

            val myTerritories    = nearby.filter { it.ownerId == userId }
            val enemyTerritories = nearby.filter { it.ownerId != userId }

            android.util.Log.d("FirebaseService",
                "🗺️ Conflict check: ${myTerritories.size} own, ${enemyTerritories.size} enemy overlapping")

            // ── 2. Enemy resolution (time-based, 40% threshold) ──────────
            // We ONLY capture enemies we covered ≥40% of.
            // Enemies below 40% stay on the map — cleanupAllConflicts() resolves
            // any remaining overlaps by capture-time after we save.
            val toCapture = mutableListOf<Territory>()

            enemyTerritories.forEach { enemy ->
                val enemyPoly = enemy.polygon.map { org.osmdroid.util.GeoPoint(it.latitude, it.longitude) }
                val fraction  = com.sidhart.walkover.utils.TerritoryConflict
                    .overlapFraction(enemyPoly, newHullPoints, samples = 100)

                android.util.Log.d("FirebaseService",
                    "⚔️ Enemy ${enemy.ownerUsername}: new walk covers ${String.format("%.0f", fraction*100)}%")

                if (fraction >= 0.40) toCapture.add(enemy)
            }

            // ── 3. Merge with own adjacent territories ────────────────────
            data class Cluster(val docs: MutableList<Territory>, var hull: List<org.osmdroid.util.GeoPoint>)
            val clusters = mutableListOf(Cluster(mutableListOf(), newHullPoints))
            myTerritories.forEach { own ->
                val ownPoly = own.polygon.map { org.osmdroid.util.GeoPoint(it.latitude, it.longitude) }
                var currentHull = ownPoly
                val currentDocs = mutableListOf(own)
                var recheck = true
                while (recheck) {
                    recheck = false
                    val iter    = clusters.iterator()
                    val toMerge = mutableListOf<Cluster>()
                    while (iter.hasNext()) {
                        val c = iter.next()
                        if (com.sidhart.walkover.utils.TerritoryConflict
                                .polygonsOverlapWithBuffer(currentHull, c.hull, 80.0)) {
                            toMerge.add(c); iter.remove()
                        }
                    }
                    if (toMerge.isNotEmpty()) {
                        currentHull = com.sidhart.walkover.utils.ConvexHull.merge(
                            currentHull, toMerge.flatMap { it.hull }
                        )
                        toMerge.forEach { currentDocs.addAll(it.docs) }
                        recheck = true
                    }
                }
                clusters.add(Cluster(currentDocs, currentHull))
            }

            // The "new walk" cluster has an empty docs list — find it
            val newCluster   = clusters.first { it.docs.isEmpty() }
            val mergedOldOwn = clusters.filter { it.docs.isNotEmpty() }

            // Delete old own territories that got merged
            mergedOldOwn.flatMap { it.docs }.forEach { old ->
                firestore.collection(TERRITORIES_COLLECTION).document(old.id).delete().await()
                try {
                    firestore.collection(USERS_COLLECTION).document(userId)
                        .update("totalTerritoryM2", FieldValue.increment(-old.areaM2))
                        .await()
                } catch (_: Exception) {}
            }

            // Delete captured enemy territories
            toCapture.forEach { enemy ->
                firestore.collection(TERRITORIES_COLLECTION).document(enemy.id).delete().await()
                if (enemy.ownerId.isNotBlank() && enemy.areaM2 > 0) {
                    try {
                        firestore.collection(USERS_COLLECTION).document(enemy.ownerId)
                            .update("totalTerritoryM2", FieldValue.increment(-enemy.areaM2))
                            .await()
                    } catch (_: Exception) {}
                }
                android.util.Log.d("FirebaseService",
                    "🎯 Captured ${enemy.ownerUsername}'s territory ${enemy.id}")
            }

            // ── 4. Save final unified hull ────────────────────────────────
            val mergedHull = if (mergedOldOwn.isNotEmpty()) {
                com.sidhart.walkover.utils.ConvexHull.merge(
                    newCluster.hull,
                    mergedOldOwn.flatMap { it.hull }
                )
            } else {
                newCluster.hull
            }

            val finalArea = com.sidhart.walkover.utils.MapUtils.calculatePolygonArea(mergedHull)
            val territory = Territory(
                ownerId       = userId,
                ownerUsername = username,
                polygon       = mergedHull.map {
                    com.google.firebase.firestore.GeoPoint(it.latitude, it.longitude)
                },
                areaM2        = finalArea,
                colorIndex    = colorIndex,
                sourceWalkId  = sourceWalkId,
                capturedAt    = now,
                updatedAt     = now,
                minLat        = mergedHull.minOf { it.latitude },
                maxLat        = mergedHull.maxOf { it.latitude },
                minLng        = mergedHull.minOf { it.longitude },
                maxLng        = mergedHull.maxOf { it.longitude }
            )

            val ref = firestore.collection(TERRITORIES_COLLECTION).add(territory).await()
            firestore.collection(USERS_COLLECTION).document(userId)
                .update(mapOf(
                    "totalTerritoryM2"   to FieldValue.increment(finalArea),
                    "totalCompeteWalks"  to FieldValue.increment(1L),
                    "territoryColorIndex" to colorIndex
                ))
                .await()

            android.util.Log.d("FirebaseService",
                "✅ Territory saved: ${ref.id} (${finalArea.toInt()} m²), " +
                "captured=${toCapture.size}, merged_own=${mergedOldOwn.flatMap{it.docs}.size}")
            Result.success(ref.id)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "❌ resolveConflictsAndSave failed", e)
            Result.failure(e)
        }
    }

    // ============= TERRITORY MANAGEMENT =============

    /**
     * Save a captured territory to Firestore.
     * Assigns a consistent color index per user (hash of userId mod 8).
     * Also updates the user's totalTerritoryM2.
     */
    suspend fun saveTerritory(territory: Territory): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            // Assign a color deterministically from userId
            val colorIndex = abs(userId.hashCode()) % 8

            val territoryWithColor = territory.copy(
                ownerId  = userId,
                ownerUsername = auth.currentUser?.displayName ?: "User",
                colorIndex = colorIndex,
                capturedAt = Date(),
                updatedAt  = Date()
            )

            val docRef = firestore.collection(TERRITORIES_COLLECTION)
                .add(territoryWithColor)
                .await()

            // Update user's total territory
            firestore.collection(USERS_COLLECTION).document(userId)
                .update(
                    mapOf(
                        "totalTerritoryM2"   to FieldValue.increment(territory.areaM2),
                        "totalCompeteWalks"  to FieldValue.increment(1L),
                        "territoryColorIndex" to colorIndex
                    )
                )
                .await()

            android.util.Log.d("FirebaseService", "✅ Territory saved: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "❌ Failed to save territory", e)
            Result.failure(e)
        }
    }

    /**
     * Listen to real-time territory updates for competitive mode.
     * Returns a Flow that emits updated territory lists whenever territories change.
     */
    fun observeTerritoriesRealtime(): Flow<List<Territory>> = callbackFlow {
        android.util.Log.d("FirebaseService", "🔵 observeTerritoriesRealtime: Setting up listener...")
        
        val listener = firestore.collection(TERRITORIES_COLLECTION)
            .orderBy("capturedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(500)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseService", "❌ observeTerritoriesRealtime ERROR: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        val territories = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(Territory::class.java)?.also { it.id = doc.id }
                            } catch (e: Exception) {
                                android.util.Log.e("FirebaseService", "❌ Failed to parse territory doc: ${e.message}")
                                null
                            }
                        }
                        android.util.Log.d("FirebaseService", "✅ Real-time listener: Loaded ${territories.size} territories")
                        if (territories.isNotEmpty()) {
                            territories.take(3).forEach { t ->
                                android.util.Log.d("FirebaseService", "   - ${t.id}: ${t.ownerUsername} (${t.areaM2} m²)")
                            }
                        }
                        val sendResult = trySend(territories)
                        if (sendResult.isFailure) {
                            android.util.Log.e("FirebaseService", "❌ Failed to send territories: ${sendResult.exceptionOrNull()?.message ?: "unknown"}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseService", "❌ Error parsing territories: ${e.message}", e)
                    }
                } else {
                    android.util.Log.w("FirebaseService", "⚠️ Snapshot is null in observeTerritoriesRealtime")
                }
            }

        awaitClose { 
            android.util.Log.d("FirebaseService", "🔴 observeTerritoriesRealtime: Removing listener")
            listener.remove() 
        }
    }

    /**
     * Listen to territories within a geographic range (radius) in real-time.
     * Uses bounding box for efficient querying.
     * 
     * @param centerLat User's current latitude
     * @param centerLng User's current longitude
     * @param radiusKm Search radius in kilometers
     */
    fun observeTerritoriesInRange(
        centerLat: Double,
        centerLng: Double,
        radiusKm: Double = 5.0
    ): Flow<List<Territory>> = callbackFlow {
        // Firestore only supports range filters on a SINGLE field in a query.
        // We filter on minLat in Firestore and apply the longitude + distance filter client-side.
        val latDelta = radiusKm / 111.0
        val lngDelta = radiusKm / (111.0 * kotlin.math.cos(Math.toRadians(centerLat)))

        val queryMinLat = centerLat - latDelta
        val queryMaxLat = centerLat + latDelta
        val queryMinLng = centerLng - lngDelta
        val queryMaxLng = centerLng + lngDelta

        android.util.Log.d("FirebaseService", "Querying territories in range: lat($queryMinLat-$queryMaxLat), lng($queryMinLng-$queryMaxLng)")

        // Single-field range on minLat only to avoid Firestore compound inequality restriction
        val listener = firestore.collection(TERRITORIES_COLLECTION)
            .whereGreaterThanOrEqualTo("minLat", queryMinLat - 0.01)
            .whereLessThanOrEqualTo("minLat", queryMaxLat + 0.01)
            .orderBy("minLat", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(300)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseService", "❌ Range query ERROR: ${error.message ?: ""}", error)
                    // Don't close the flow — just log. A Firestore index error can be recovered
                    // when the index is created; closing would kill the listener permanently.
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        val territories = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Territory::class.java)?.also { it.id = doc.id }
                        }.filter { t ->
                            // Apply longitude bounding-box filter client-side
                            t.minLng <= queryMaxLng + 0.01 && t.maxLng >= queryMinLng - 0.01
                        }
                        android.util.Log.d("FirebaseService", "✅ Range listener: ${territories.size} territories after client-side lng filter")
                        val sendResult = trySend(territories)
                        if (sendResult.isFailure) {
                            android.util.Log.e("FirebaseService", "❌ Failed to send territories: ${sendResult.exceptionOrNull()?.message ?: "unknown"}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseService", "❌ Error parsing range territories: ${e.message}", e)
                    }
                } else {
                    android.util.Log.w("FirebaseService", "⚠️ Snapshot is null in range query")
                }
            }

        awaitClose { listener.remove() }
    }
    suspend fun getTerritoriesInBoundingBox(
        minLat: Double, maxLat: Double,
        minLng: Double, maxLng: Double
    ): Result<List<Territory>> {
        return try {
            val snapshot = firestore.collection(TERRITORIES_COLLECTION)
                .whereGreaterThanOrEqualTo("minLat", minLat - 0.01)
                .whereLessThanOrEqualTo("minLat", maxLat + 0.01)
                .orderBy("minLat", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(300)
                .get()
                .await()

            val territories = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Territory::class.java)?.also { it.id = doc.id }
            }.filter { t ->
                // Client-side: filter by maxLat and longitude bounding box
                t.maxLat >= minLat - 0.01 &&
                t.minLng <= maxLng + 0.01 &&
                t.maxLng >= minLng - 0.01
            }

            android.util.Log.d("FirebaseService", "BBox found ${territories.size} territories")
            Result.success(territories)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "BBox query failed", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a territory and subtract its area from the previous owner stats.
     * Called when territory is stolen by a new walk, or merged into a larger polygon.
     */
    suspend fun deleteTerritory(
        territoryId: String,
        ownerId: String,
        areaM2: Double
    ): Result<Unit> {
        return try {
            firestore.collection(TERRITORIES_COLLECTION).document(territoryId).delete().await()
            if (ownerId.isNotBlank() && areaM2 > 0) {
                try {
                    firestore.collection(USERS_COLLECTION).document(ownerId)
                        .update("totalTerritoryM2", FieldValue.increment(-areaM2))
                        .await()
                } catch (_: Exception) {}
            }
            android.util.Log.d("FirebaseService", "Deleted territory $territoryId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Failed to delete territory $territoryId", e)
            Result.failure(e)
        }
    }
}


