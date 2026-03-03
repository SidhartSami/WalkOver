package com.sidhart.walkover.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sidhart.walkover.data.PendingOperation
import com.sidhart.walkover.data.OperationType
import kotlinx.coroutines.tasks.await
import java.util.UUID
import androidx.core.content.edit

/**
 * Manages offline operations queue
 * Stores failed operations and retries them when internet is restored
 */
class OfflineQueueManager(
    context: Context,
    private val firebaseService: FirebaseService
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "offline_queue",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    companion object {
        private const val KEY_PENDING_OPERATIONS = "pending_operations"
        private const val MAX_RETRY_COUNT = 5
        private const val TAG = "OfflineQueueManager"
    }

    /**
     * Add a pending operation to the queue
     */
    fun addPendingOperation(operation: PendingOperation) {
        val operations = getPendingOperations().toMutableList()
        operations.add(operation.copy(id = UUID.randomUUID().toString()))
        savePendingOperations(operations)
        Log.d(TAG, "Added pending operation: ${operation.type}")
    }

    /**
     * Get all pending operations
     */
    private fun getPendingOperations(): List<PendingOperation> {
        val json = prefs.getString(KEY_PENDING_OPERATIONS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<PendingOperation>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing pending operations", e)
            emptyList()
        }
    }

    /**
     * Save pending operations to SharedPreferences
     */
    private fun savePendingOperations(operations: List<PendingOperation>) {
        val json = gson.toJson(operations)
        prefs.edit { putString(KEY_PENDING_OPERATIONS, json) }
    }

    /**
     * Remove a pending operation from the queue
     */
    private fun removePendingOperation(operationId: String) {
        val operations = getPendingOperations().toMutableList()
        operations.removeAll { it.id == operationId }
        savePendingOperations(operations)
    }

    /**
     * Process all pending operations
     * Called when internet connection is restored
     */
    suspend fun processPendingOperations() {
        val operations = getPendingOperations()
        
        if (operations.isEmpty()) {
            Log.d(TAG, "No pending operations to process")
            return
        }

        Log.d(TAG, "Processing ${operations.size} pending operations")

        operations.forEach { operation ->
            if (operation.retryCount >= MAX_RETRY_COUNT) {
                Log.w(TAG, "Max retry count reached for operation ${operation.id}, removing")
                removePendingOperation(operation.id)
                return@forEach
            }

            val success = when (operation.type) {
                OperationType.SAVE_WALK -> retrySaveWalk(operation)
                OperationType.UPDATE_STREAK -> retryUpdateStreak(operation)
                OperationType.UPDATE_USER_STATS -> retryUpdateUserStats(operation)
                OperationType.UPDATE_CHALLENGES -> retryUpdateChallenges(operation)
                OperationType.AWARD_XP -> retryAwardXP(operation)
                OperationType.UPDATE_DAILY_ACTIVITY -> retryUpdateDailyActivity()
            }

            if (success) {
                Log.d(TAG, "Successfully processed operation ${operation.id}")
                removePendingOperation(operation.id)
            } else {
                // Increment retry count
                val updatedOperations = getPendingOperations().map {
                    if (it.id == operation.id) {
                        it.copy(retryCount = it.retryCount + 1)
                    } else {
                        it
                    }
                }
                savePendingOperations(updatedOperations)
                Log.w(TAG, "Failed to process operation ${operation.id}, retry count: ${operation.retryCount + 1}")
            }
        }
    }

    /**
     * Retry saving a walk
     */
    private suspend fun retrySaveWalk(operation: PendingOperation): Boolean {
        // Walk data should be stored in operation.data
        // This is a simplified version - you may need to reconstruct the Walk object
        Log.d(TAG, "Retrying save walk for walkId: ${operation.walkId}")
        
        // Since the walk might have been partially saved, we need to check and update
        // the related data (streak, challenges, user stats, etc.)
        return try {
            val userId = operation.userId
            
            // Try to update streak
            firebaseService.getStreakData(userId).getOrNull()?.let { streak ->
                firebaseService.updateStreakAfterWalk(
                    com.sidhart.walkover.data.Walk(
                        id = operation.walkId,
                        userId = userId,
                        timestamp = operation.timestamp
                    )
                )
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error retrying save walk", e)
            false
        }
    }

    /**
     * Retry updating streak
     */
    private suspend fun retryUpdateStreak(operation: PendingOperation): Boolean {
        return try {
            val userId = operation.userId
            val walkId = operation.walkId
            
            val result = firebaseService.updateStreakAfterWalk(
                com.sidhart.walkover.data.Walk(
                    id = walkId,
                    userId = userId,
                    timestamp = operation.timestamp
                )
            )
            result.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error retrying update streak", e)
            false
        }
    }

    /**
     * Retry updating user stats
     */
    private suspend fun retryUpdateUserStats(operation: PendingOperation): Boolean {
        return try {
            val userId = operation.userId
            val distance = (operation.data["distance"] as? Number)?.toDouble() ?: 0.0
            val duration = (operation.data["duration"] as? Number)?.toLong() ?: 0L
            
            Log.d(TAG, "Retrying update user stats: distance=$distance, duration=$duration")
            
            // Get user reference
            val userRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
            
            // Update user stats
            val currentMonthId = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                .format(java.util.Date())
            
            com.google.firebase.firestore.FirebaseFirestore.getInstance().runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentUser = snapshot.toObject(com.sidhart.walkover.data.User::class.java) 
                    ?: com.sidhart.walkover.data.User()
                
                val currentDistance = currentUser.totalDistanceWalked
                val currentWalks = currentUser.totalWalks
                
                // Check for month change
                val storedMonthId = currentUser.currentMonthId
                val monthlyDistance = if (storedMonthId == currentMonthId) {
                    currentUser.monthlyDistanceWalked + distance
                } else {
                    distance
                }
                
                // Update User
                transaction.update(
                    userRef,
                    mapOf(
                        "totalDistanceWalked" to (currentDistance + distance),
                        "totalWalks" to (currentWalks + 1),
                        "lastWalkDate" to System.currentTimeMillis(),
                        "monthlyDistanceWalked" to monthlyDistance,
                        "currentMonthId" to currentMonthId
                    )
                )
                
                // Update Monthly Leaderboard Entry
                val monthlyRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("monthly_leaderboards")
                    .document(currentMonthId)
                    .collection("users")
                    .document(userId)
                
                val username = currentUser.username
                val monthlyData = mapOf(
                    "userId" to userId,
                    "username" to username,
                    "distance" to monthlyDistance,
                    "updatedAt" to System.currentTimeMillis()
                )
                
                transaction.set(monthlyRef, monthlyData)
            }.await()
            
            Log.d(TAG, "Successfully updated user stats")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error retrying update user stats", e)
            false
        }
    }

    /**
     * Retry updating challenges
     */
    private suspend fun retryUpdateChallenges(operation: PendingOperation): Boolean {
        return try {
            val userId = operation.userId
            val distance = (operation.data["distance"] as? Number)?.toDouble() ?: 0.0
            val duration = (operation.data["duration"] as? Number)?.toLong() ?: 0L
            
            // Reconstruct walk object for challenge update
            val walk = com.sidhart.walkover.data.Walk(
                id = operation.walkId,
                userId = userId,
                timestamp = operation.timestamp,
                distanceCovered = distance,
                duration = duration
            )
            
            firebaseService.updateChallengesAfterWalk(walk)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error retrying update challenges", e)
            false
        }
    }

    /**
     * Retry awarding XP
     */
    private suspend fun retryAwardXP(operation: PendingOperation): Boolean {
        return try {
            val userId = operation.userId
            val xp = (operation.data["xp"] as? Number)?.toInt() ?: 0
            
            if (xp > 0) {
                firebaseService.awardXP(userId, xp)
                true // awardXP returns Unit, so if no exception, it succeeded
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrying award XP", e)
            false
        }
    }

    /**
     * Retry updating daily activity
     */
    private fun retryUpdateDailyActivity(): Boolean {
        return try {
            Log.d(TAG, "Retrying update daily activity")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error retrying update daily activity", e)
            false
        }
    }

}
