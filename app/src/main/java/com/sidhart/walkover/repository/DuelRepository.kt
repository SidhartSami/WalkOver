package com.sidhart.walkover.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.sidhart.walkover.data.DuelChallenge
import com.sidhart.walkover.data.DuelStatus
import kotlinx.coroutines.tasks.await
import java.util.*

class DuelRepository {
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val DUELS_COLLECTION = "duelChallenges"
    }

    suspend fun createDuelChallenge(challengerId: String, challengerUsername: String, opponentId: String, opponentUsername: String, durationDays: Int): Result<DuelChallenge> {
        return try {
            val challenge = DuelChallenge(
                challengerId = challengerId,
                challengerUsername = challengerUsername,
                opponentId = opponentId,
                opponentUsername = opponentUsername,
                durationDays = durationDays,
                status = DuelStatus.PENDING.name,
                requestTimestamp = Date()
            )

            val docRef = firestore.collection(DUELS_COLLECTION).add(challenge).await()
            val savedChallenge = challenge.copy(id = docRef.id)
            Result.success(savedChallenge)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveDuel(userId: String): Result<DuelChallenge?> {
        return try {
            val now = Date()
            
            suspend fun processChallenge(doc: com.google.firebase.firestore.DocumentSnapshot): DuelChallenge? {
                val challenge = doc.toObject(DuelChallenge::class.java)?.apply { id = doc.id }
                if (challenge != null && challenge.endTimestamp != null && challenge.endTimestamp.before(now)) {
                    // Challenge is over
                    val winnerId = when {
                        challenge.challengerDistanceKm > challenge.opponentDistanceKm -> challenge.challengerId
                        challenge.opponentDistanceKm > challenge.challengerDistanceKm -> challenge.opponentId
                        else -> null // Tie
                    }
                    val completedChallenge = challenge.copy(status = DuelStatus.COMPLETED.name, winnerId = winnerId)
                    firestore.collection(DUELS_COLLECTION).document(challenge.id)
                        .update(
                            mapOf(
                                "status" to DuelStatus.COMPLETED.name,
                                "winnerId" to winnerId
                            )
                        ).await()
                    return completedChallenge
                }
                return challenge
            }

            // Firestore doesn't support logical OR across different fields easily in one query
            // We do two queries: one where user is challenger, one where user is opponent
            val asChallenger = firestore.collection(DUELS_COLLECTION)
                .whereEqualTo("challengerId", userId)
                .whereEqualTo("status", DuelStatus.ACTIVE.name)
                .get().await()

            if (!asChallenger.isEmpty) {
                return Result.success(processChallenge(asChallenger.documents[0]))
            }

            val asOpponent = firestore.collection(DUELS_COLLECTION)
                .whereEqualTo("opponentId", userId)
                .whereEqualTo("status", DuelStatus.ACTIVE.name)
                .get().await()

            if (!asOpponent.isEmpty) {
                return Result.success(processChallenge(asOpponent.documents[0]))
            }

            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPendingDuels(userId: String): Result<List<DuelChallenge>> {
        return try {
            val pending = firestore.collection(DUELS_COLLECTION)
                .whereEqualTo("opponentId", userId)
                .whereEqualTo("status", DuelStatus.PENDING.name)
                .get().await()

            val challenges = mutableListOf<DuelChallenge>()
            val nowMs = Date().time
            for (doc in pending.documents) {
                try {
                    val challenge = doc.toObject(DuelChallenge::class.java)?.apply { id = doc.id }
                    if (challenge != null) {
                        try {
                            val ageMs = nowMs - challenge.requestTimestamp.time
                            if (ageMs > 24L * 60 * 60 * 1000) {
                                firestore.collection(DUELS_COLLECTION).document(doc.id).update("status", DuelStatus.EXPIRED.name)
                            } else {
                                challenges.add(challenge)
                            }
                        } catch (_: Exception) {
                            // If timestamp is somehow missing/null natively
                            challenges.add(challenge)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DuelRepo", "Failed to parse challenge ${doc.id}", e)
                }
            }
            Result.success(challenges)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSentPendingDuels(userId: String): Result<List<DuelChallenge>> {
        return try {
            val pending = firestore.collection(DUELS_COLLECTION)
                .whereEqualTo("challengerId", userId)
                .whereEqualTo("status", DuelStatus.PENDING.name)
                .get().await()

            val challenges = mutableListOf<DuelChallenge>()
            val nowMs = Date().time
            for (doc in pending.documents) {
                try {
                    val challenge = doc.toObject(DuelChallenge::class.java)?.apply { id = doc.id }
                    if (challenge != null) {
                        try {
                            val ageMs = nowMs - challenge.requestTimestamp.time
                            if (ageMs > 24L * 60 * 60 * 1000) {
                                firestore.collection(DUELS_COLLECTION).document(doc.id).update("status", DuelStatus.EXPIRED.name)
                            } else {
                                challenges.add(challenge)
                            }
                        } catch (_: Exception) {
                            challenges.add(challenge)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DuelRepo", "Failed to parse sent challenge ${doc.id}", e)
                }
            }
            Result.success(challenges)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptDuelChallenge(challengeId: String, durationDays: Int, userId: String): Result<Unit> {
        return try {
            val calendar = Calendar.getInstance()
            // Reset to midnight
            calendar.time = Date()
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.add(Calendar.DAY_OF_YEAR, durationDays - 1)
            val endTimestamp = calendar.time

            firestore.collection(DUELS_COLLECTION).document(challengeId)
                .update(
                    mapOf(
                        "status" to DuelStatus.ACTIVE.name,
                        "startTimestamp" to Date(),
                        "endTimestamp" to endTimestamp
                    )
                ).await()

            // Decline all other incoming requests
            val pendingIncoming = firestore.collection(DUELS_COLLECTION)
                .whereEqualTo("opponentId", userId)
                .whereEqualTo("status", DuelStatus.PENDING.name)
                .get().await()
            for (doc in pendingIncoming.documents) {
                if (doc.id != challengeId) {
                    firestore.collection(DUELS_COLLECTION).document(doc.id).update("status", DuelStatus.DECLINED.name)
                }
            }

            // Decline all other outgoing requests
            val pendingOutgoing = firestore.collection(DUELS_COLLECTION)
                .whereEqualTo("challengerId", userId)
                .whereEqualTo("status", DuelStatus.PENDING.name)
                .get().await()
            for (doc in pendingOutgoing.documents) {
                if (doc.id != challengeId) {
                    firestore.collection(DUELS_COLLECTION).document(doc.id).update("status", DuelStatus.DECLINED.name)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineDuelChallenge(challengeId: String): Result<Unit> {
        return try {
            firestore.collection(DUELS_COLLECTION).document(challengeId)
                .update("status", DuelStatus.DECLINED.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateDuelDistance(challengeId: String, isChallenger: Boolean, distanceKm: Double): Result<Unit> {
        return try {
            val fieldName = if (isChallenger) "challengerDistanceKm" else "opponentDistanceKm"
            firestore.collection(DUELS_COLLECTION).document(challengeId)
                .update(fieldName, distanceKm)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
