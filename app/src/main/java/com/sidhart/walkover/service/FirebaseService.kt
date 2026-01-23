package com.sidhart.walkover.service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.sidhart.walkover.data.User
import com.sidhart.walkover.data.Walk
import kotlinx.coroutines.tasks.await
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.GoogleAuthProvider


class FirebaseService {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val WALKS_COLLECTION = "walks"
        private const val USERS_COLLECTION = "users"
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
    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            android.util.Log.d("FirebaseService", "Attempting email sign-in for: $email")
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
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
            android.util.Log.e("FirebaseService", "Firebase Auth error: ${e.errorCode}", e)
            val errorMessage = when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> "Invalid email address"
                "ERROR_WRONG_PASSWORD" -> "Incorrect password"
                "ERROR_USER_NOT_FOUND" -> "No account found with this email"
                "ERROR_USER_DISABLED" -> "This account has been disabled"
                "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please try again later"
                else -> "Sign-in failed: ${e.message}"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Sign-in failed", e)
            Result.failure(Exception("Sign-in failed: ${e.message}"))
        }
    }
    // Add these methods to your FirebaseService.kt class

    /**
     * Check if an email is already registered and if it's verified
     * Returns: null if available, "verified" if taken and verified, "unverified" if taken but not verified
     */
    suspend fun checkEmailStatus(email: String): Result<String?> {
        return try {
            val signInMethods = auth.fetchSignInMethodsForEmail(email).await()

            if (signInMethods.signInMethods.isNullOrEmpty()) {
                // Email is available
                Result.success(null)
            } else {
                // Email is registered - but we can't directly check if it's verified without signing in
                // So we return that it exists
                Result.success("registered")
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error checking email status", e)
            Result.failure(e)
        }
    }

    /**
     * Enhanced registration that provides better error messages
     * Now includes automatic verification email sending
     */
    suspend fun registerWithEmailEnhanced(email: String, password: String, username: String): Result<User> {
        return try {
            android.util.Log.d("FirebaseService", "Starting enhanced registration for: $email")

            // First check if username is already taken
            val usernameQuery = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .await()

            if (!usernameQuery.documents.isEmpty()) {
                return Result.failure(Exception("Username '$username' is already taken. Please choose a different one."))
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
     */
    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<User> {
        return try {
            android.util.Log.d("FirebaseService", "Signing in with Google: ${account.email}")

            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                // Check if user document exists
                val userRef = firestore.collection(USERS_COLLECTION).document(firebaseUser.uid)
                val userDoc = userRef.get().await()

                val user = if (userDoc.exists()) {
                    // Existing user
                    userDoc.toObject(User::class.java) ?: User(
                        id = firebaseUser.uid,
                        username = firebaseUser.displayName ?: "User",
                        email = firebaseUser.email ?: "",
                        isAnonymous = false
                    )
                } else {
                    // New user - create document
                    val newUser = User(
                        id = firebaseUser.uid,
                        username = firebaseUser.displayName ?: "User",
                        email = firebaseUser.email ?: "",
                        totalDistanceWalked = 0.0,
                        totalWalks = 0,
                        lastWalkDate = System.currentTimeMillis(),
                        isAnonymous = false
                    )
                    userRef.set(newUser).await()
                    newUser
                }

                android.util.Log.d("FirebaseService", "Google sign-in successful: ${firebaseUser.uid}")
                Result.success(user)
            } else {
                Result.failure(Exception("Google sign-in failed: No user data"))
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Google sign-in failed", e)
            Result.failure(Exception("Google sign-in failed: ${e.message}"))
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

    /**
     * Save walk with optional territory creation
     */
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
                "duration" to walk.duration
            )

            val docRef = firestore.collection("walks").add(walkData).await()

            // Update user stats
            val userRef = firestore.collection("users").document(userId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentDistance = snapshot.getDouble("totalDistanceWalked") ?: 0.0
                val currentWalks = snapshot.getLong("totalWalks")?.toInt() ?: 0

                transaction.update(
                    userRef,
                    mapOf(
                        "totalDistanceWalked" to (currentDistance + walk.distanceCovered),
                        "totalWalks" to (currentWalks + 1),
                        "lastWalkDate" to System.currentTimeMillis()
                    )
                )
            }.await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error saving walk", e)
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
                    totalWalks = 1,
                    lastWalkDate = walk.timestamp.time
                )
                userRef.set(newUser).await()
            }
        } catch (e: Exception) {
            // Log error but don't fail the walk save
            android.util.Log.e("FirebaseService", "Error updating user stats", e)
        }
    }

    suspend fun getLeaderboard(): Result<List<User>> {
        return try {
            val users = firestore.collection(USERS_COLLECTION)
                .orderBy("totalAreaCaptured", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100)
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
                .limit(100)
                .get()
                .await()
                .toObjects(User::class.java)

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Replace the getWalks() method in FirebaseService.kt with this:

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
            }.sortedByDescending { it.timestamp }

            android.util.Log.d("FirebaseService", "Successfully loaded ${walks.size} walks")

            Result.success(walks)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error getting walks", e)
            Result.failure(Exception("Failed to load walks: ${e.message}"))
        }
    }

    // ============= PROFILE MANAGEMENT =============

    /**
     * Get user profile by ID
     */
    suspend fun getUserProfile(userId: String): Result<User?> {
        return try {
            val user = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
                .toObject(User::class.java)

            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error getting user profile", e)
            Result.failure(e)
        }
    }

    /**
     * Update user profile information
     */
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .await()

            android.util.Log.d("FirebaseService", "User profile updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error updating user profile", e)
            Result.failure(e)
        }
    }

    /**
     * Update email
     */
    suspend fun updateEmail(newEmail: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Result.failure(Exception("User not authenticated"))
            } else {
                // Update email in auth
                currentUser.updateEmail(newEmail).await()

                // Update email in Firestore
                firestore.collection(USERS_COLLECTION)
                    .document(currentUser.uid)
                    .update("email", newEmail)
                    .await()

                android.util.Log.d("FirebaseService", "Email updated to: $newEmail")
                Result.success(Unit)
            }
        } catch (e: FirebaseAuthException) {
            android.util.Log.e("FirebaseService", "Firebase Auth error: ${e.errorCode}", e)
            val errorMessage = when (e.errorCode) {
                "ERROR_EMAIL_ALREADY_IN_USE" -> "This email is already in use"
                "ERROR_INVALID_EMAIL" -> "Invalid email address"
                "ERROR_REQUIRES_RECENT_LOGIN" -> "Please sign in again to update email"
                else -> "Failed to update email: ${e.message}"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error updating email", e)
            Result.failure(Exception("Failed to update email: ${e.message}"))
        }
    }

    /**
     * Change password
     */
    suspend fun changePassword(newPassword: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Result.failure(Exception("User not authenticated"))
            } else {
                currentUser.updatePassword(newPassword).await()
                android.util.Log.d("FirebaseService", "Password updated successfully")
                Result.success(Unit)
            }
        } catch (e: FirebaseAuthException) {
            android.util.Log.e("FirebaseService", "Firebase Auth error: ${e.errorCode}", e)
            val errorMessage = when (e.errorCode) {
                "ERROR_WEAK_PASSWORD" -> "Password is too weak. Use at least 6 characters"
                "ERROR_REQUIRES_RECENT_LOGIN" -> "Please sign in again to change password"
                else -> "Failed to change password: ${e.message}"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error changing password", e)
            Result.failure(Exception("Failed to change password: ${e.message}"))
        }
    }

    /**
     * Delete user account
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Result.failure(Exception("User not authenticated"))
            } else {
                val userId = currentUser.uid

                // Delete user data from Firestore
                firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .delete()
                    .await()

                // Delete all user walks
                val walks = firestore.collection(WALKS_COLLECTION)
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                walks.documents.forEach { doc ->
                    doc.reference.delete().await()
                }

                // Delete auth account
                currentUser.delete().await()

                android.util.Log.d("FirebaseService", "Account deleted successfully")
                Result.success(Unit)
            }
        } catch (e: FirebaseAuthException) {
            android.util.Log.e("FirebaseService", "Firebase Auth error: ${e.errorCode}", e)
            val errorMessage = when (e.errorCode) {
                "ERROR_REQUIRES_RECENT_LOGIN" -> "Please sign in again to delete your account"
                else -> "Failed to delete account: ${e.message}"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error deleting account", e)
            Result.failure(Exception("Failed to delete account: ${e.message}"))
        }
    }

    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            android.util.Log.d("FirebaseService", "Password reset email sent to: $email")
            Result.success(Unit)
        } catch (e: FirebaseAuthException) {
            android.util.Log.e("FirebaseService", "Firebase Auth error: ${e.errorCode}", e)
            val errorMessage = when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> "Invalid email address"
                "ERROR_USER_NOT_FOUND" -> "No account found with this email"
                else -> "Failed to send reset email: ${e.message}"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Error sending reset email", e)
            Result.failure(Exception("Failed to send reset email: ${e.message}"))
        }
    }

    // Add these methods to your FirebaseService.kt

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
     * Check if user is logged in (regardless of verification)
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Check if current user's email is verified
     */
    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified ?: false
    }

    /**
     * Send verification email to current user
     */
    suspend fun sendVerificationEmail(): Result<Unit> {
        return try {
            val currentUser = auth.currentUser

            if (currentUser == null) {
                return Result.failure(Exception("No user logged in"))
            }

            if (currentUser.isAnonymous) {
                return Result.failure(Exception("Anonymous users don't need verification"))
            }

            if (currentUser.isEmailVerified) {
                return Result.failure(Exception("Email already verified"))
            }

            android.util.Log.d("FirebaseService", "Sending verification email to: ${currentUser.email}")
            currentUser.sendEmailVerification().await()

            android.util.Log.d("FirebaseService", "Verification email sent successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Failed to send verification email", e)
            Result.failure(Exception("Failed to send verification email: ${e.message}"))
        }
    }

    /**
     * Reload user data to check if email was verified
     */
    suspend fun reloadUser(): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser

            if (currentUser == null) {
                return Result.failure(Exception("No user logged in"))
            }

            android.util.Log.d("FirebaseService", "Reloading user data...")
            currentUser.reload().await()

            val isVerified = currentUser.isEmailVerified
            android.util.Log.d("FirebaseService", "User reloaded. Email verified: $isVerified")

            Result.success(isVerified)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Failed to reload user", e)
            Result.failure(Exception("Failed to check verification status: ${e.message}"))
        }
    }

    /**
     * Register new user with email, password, and username + SEND VERIFICATION EMAIL
     */
    suspend fun registerWithEmail(email: String, password: String, username: String): Result<User> {
        return try {
            android.util.Log.d("FirebaseService", "Attempting registration for: $email")

            // Create auth user
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                // Update display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(username)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()

                // Send verification email
                try {
                    android.util.Log.d("FirebaseService", "Sending verification email...")
                    firebaseUser.sendEmailVerification().await()
                    android.util.Log.d("FirebaseService", "Verification email sent to: $email")
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

                android.util.Log.d("FirebaseService", "Registration successful: ${firebaseUser.uid}")
                Result.success(user)
            } else {
                Result.failure(Exception("Registration failed: No user data"))
            }
        } catch (e: FirebaseAuthException) {
            android.util.Log.e("FirebaseService", "Firebase Auth error: ${e.errorCode}", e)
            val errorMessage = when (e.errorCode) {
                "ERROR_EMAIL_ALREADY_IN_USE" -> "An account already exists with this email"
                "ERROR_INVALID_EMAIL" -> "Invalid email address"
                "ERROR_WEAK_PASSWORD" -> "Password is too weak. Use at least 6 characters"
                else -> "Registration failed: ${e.message}"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            android.util.Log.e("FirebaseService", "Registration failed", e)
            Result.failure(Exception("Registration failed: ${e.message}"))
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
                // Username login - find email first
                android.util.Log.d("FirebaseService", "Looking up email for username: $trimmedInput")

                val userQuery = firestore.collection(USERS_COLLECTION)
                    .whereEqualTo("username", trimmedInput)
                    .limit(1)
                    .get()
                    .await()

                if (userQuery.documents.isEmpty()) {
                    return Result.failure(Exception("No account found with username: $trimmedInput"))
                }

                val userDoc = userQuery.documents[0]
                val foundEmail = userDoc.getString("email")

                if (foundEmail.isNullOrBlank()) {
                    return Result.failure(Exception("Account has no email associated"))
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
}