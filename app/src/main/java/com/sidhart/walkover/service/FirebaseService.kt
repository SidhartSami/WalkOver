package com.sidhart.walkover.service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.sidhart.walkover.data.User
import com.sidhart.walkover.data.Walk
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseService {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    init {
        // Ensure Firebase is initialized
        if (auth.app == null) {
            throw IllegalStateException("Firebase not initialized")
        }
    }

    companion object {
        private const val WALKS_COLLECTION = "walks"
        private const val USERS_COLLECTION = "users"
    }

    suspend fun saveWalk(walk: Walk): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Result.failure(Exception("User not authenticated"))
            } else {
                val walkData = walk.copy(
                    userId = currentUser.uid,
                    username = currentUser.displayName ?: "Anonymous"
                )
                
                val docRef = firestore.collection(WALKS_COLLECTION).add(walkData).await()
                
                // Update user statistics
                updateUserStats(walkData)
                
                Result.success(docRef.id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateUserStats(walk: Walk) {
        try {
            val userRef = firestore.collection(USERS_COLLECTION).document(walk.userId)
            val userDoc = userRef.get().await()
            
            if (userDoc.exists()) {
                val currentUser = userDoc.toObject(User::class.java) ?: User()
                val updatedUser = currentUser.copy(
                    totalDistanceWalked = currentUser.totalDistanceWalked + walk.distanceCovered,
                    totalAreaCaptured = currentUser.totalAreaCaptured + walk.areaCaptured,
                    totalWalks = currentUser.totalWalks + 1,
                    lastWalkDate = walk.timestamp.time
                )
                userRef.set(updatedUser).await()
            } else {
                // Create new user document
                val newUser = User(
                    id = walk.userId,
                    username = walk.username,
                    totalDistanceWalked = walk.distanceCovered,
                    totalAreaCaptured = walk.areaCaptured,
                    totalWalks = 1,
                    lastWalkDate = walk.timestamp.time
                )
                userRef.set(newUser).await()
            }
        } catch (e: Exception) {
            // Log error but don't fail the walk save
            e.printStackTrace()
        }
    }

    suspend fun getLeaderboard(): Result<List<User>> {
        return try {
            val users = firestore.collection(USERS_COLLECTION)
                .orderBy("totalAreaCaptured", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()
                .toObjects(User::class.java)
            
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDistanceLeaderboard(): Result<List<User>> {
        return try {
            val users = firestore.collection(USERS_COLLECTION)
                .orderBy("totalDistanceWalked", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()
                .toObjects(User::class.java)
            
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser() = auth.currentUser

    fun isUserAuthenticated() = auth.currentUser != null

    suspend fun signInAnonymously(): Result<Unit> {
        return try {
            android.util.Log.d("FirebaseService", "Attempting anonymous sign-in...")
            val task = auth.signInAnonymously()
            val result = task.await()
            android.util.Log.d("FirebaseService", "Sign-in successful: ${result.user?.uid}")
            Result.success(Unit)
        } catch (e: FirebaseAuthException) {
            android.util.Log.e("FirebaseService", "Firebase Auth error: ${e.errorCode}", e)
            Result.failure(Exception("Firebase Auth error: ${e.errorCode} - ${e.message}", e))
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Sign-in failed", e)
            Result.failure(Exception("Firebase sign-in failed: ${e.message}", e))
        }
    }

    suspend fun getWalks(): Result<List<Walk>> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Result.failure(Exception("User not authenticated"))
            } else {
                val walks = firestore.collection(WALKS_COLLECTION)
                    .whereEqualTo("userId", currentUser.uid)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                    .toObjects(Walk::class.java)
                
                Result.success(walks)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

