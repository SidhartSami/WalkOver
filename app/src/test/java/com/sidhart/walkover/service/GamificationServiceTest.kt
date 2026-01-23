package com.sidhart.walkover.service

import com.sidhart.walkover.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import java.util.*

// ================= FAKES & TESTABLE SUBCLASSES =================

open class FakeFirebaseService : FirebaseService() {
    var mockUser: User? = null
    var lastUpdatedUser: User? = null
    val challenges = mutableMapOf<String, MutableMap<String, Challenge>>()
    val usersList = mutableListOf<User>()

    override suspend fun getCurrentUserData(): Result<User> {
        return mockUser?.let { Result.success(it) } 
            ?: Result.failure(Exception("User not found"))
    }

    override suspend fun updateUserProfile(user: User): Result<Unit> {
        lastUpdatedUser = user
        mockUser = user
        return Result.success(Unit)
    }
    
    override suspend fun getChallenges(userId: String): Result<List<Challenge>> {
        return Result.success(challenges[userId]?.values?.toList() ?: emptyList())
    }

    override suspend fun saveChallenge(userId: String, challenge: Challenge): Result<Unit> {
        val userChallenges = challenges.getOrPut(userId) { mutableMapOf() }
        userChallenges[challenge.id] = challenge
        return Result.success(Unit)
    }

    override suspend fun getDistanceLeaderboard(): Result<List<User>> {
        return Result.success(usersList.sortedByDescending { it.totalDistanceWalked })
    }
}

class TestableStreakService(
    firebaseService: FirebaseService,
    private val fixedDate: String
) : StreakService(firebaseService) {
    override fun getCurrentDate(): String = fixedDate
}

class TestableChallengeService(
    firebaseService: FirebaseService,
    xpService: XPService,
    private val fixedDate: String
) : ChallengeService(firebaseService, xpService) {
    override fun getCurrentDate(): String = fixedDate
}

// ================= TEST CLASSES =================

class XPServiceTest {
    @Test
    fun calculateDistanceXP_returnsCorrectValue() {
        val fakeFirebase = FakeFirebaseService()
        val xpService = XPService(fakeFirebase)
        assertEquals(15, xpService.calculateDistanceXP(1.5))
        assertEquals(50, xpService.calculateDistanceXP(5.0))
    }
    
    @Test
    fun checkLevelUp_detectsLevelChange() {
        val fakeFirebase = FakeFirebaseService()
        val xpService = XPService(fakeFirebase)
        var result = xpService.checkLevelUp(90, 1)
        assertFalse(result.first)
        result = xpService.checkLevelUp(110, 1)
        assertTrue(result.first)
        assertEquals(2, result.second)
    }
    
    @Test
    fun awardXP_updatesUserAndLevel() = runBlocking {
        val fakeFirebase = FakeFirebaseService()
        val initialUser = User(id = "u1", username = "Test", xp = 50, level = 1)
        fakeFirebase.mockUser = initialUser
        val xpService = XPService(fakeFirebase)
        
        // Award 60 XP (Total 110) -> Level 2
        val result = xpService.awardXP("u1", 60, "test", false)
        assertTrue(result.second)
        assertEquals(2, result.first)
        assertEquals(2, fakeFirebase.lastUpdatedUser!!.level)
    }
}

class StreakServiceTest {
    private fun createService(date: String, user: User): Pair<TestableStreakService, FakeFirebaseService> {
        val fakeFirebase = FakeFirebaseService()
        fakeFirebase.mockUser = user
        val service = TestableStreakService(fakeFirebase, date)
        return Pair(service, fakeFirebase)
    }

    @Test
    fun recordDailyActivity_logic() = runBlocking {
        // New streak
        val u1 = User(id = "u1", streakData = StreakData())
        val (s1, fb1) = createService("2023-01-01", u1)
        assertEquals(1, s1.recordDailyActivity("u1").dailyStreak)
        
        // Consecutive
        val u2 = User(id = "u2", streakData = StreakData(dailyStreak = 5, lastActivityDate = "2023-01-01"))
        val (s2, fb2) = createService("2023-01-02", u2)
        assertEquals(6, s2.recordDailyActivity("u2").dailyStreak)
        
        // Broken
        val (s3, fb3) = createService("2023-01-04", u2) // Skipped 2 days
        assertEquals(1, s3.recordDailyActivity("u2").dailyStreak)
    }
    
    @Test
    fun freezeCard_logic() = runBlocking {
        // Earn card (streak 6 -> 7)
        val u1 = User(id = "u1", streakData = StreakData(dailyStreak = 6, lastActivityDate = "2023-01-01", freezeCardsAvailable = 0))
        val (s1, _) = createService("2023-01-02", u1)
        val res = s1.recordDailyActivity("u1")
        assertEquals(7, res.dailyStreak)
        assertEquals(1, res.freezeCardsAvailable)
        
        // Use card
        val u2 = User(id = "u2", streakData = StreakData(dailyStreak = 10, lastActivityDate = "2023-01-01", freezeCardsAvailable = 1))
        // Date is 03 (missed 02)
        val (s2, fb2) = createService("2023-01-03", u2)
        assertTrue(s2.useFreezeCard("u2"))
        
        val updated = fb2.lastUpdatedUser!!.streakData
        assertEquals(0, updated.freezeCardsAvailable)
        assertEquals("2023-01-03", updated.lastActivityDate)
        assertEquals(10, updated.dailyStreak) // Streak count preserved
    }
}

class ChallengeServiceTest {
    private fun createServices(user: User): Pair<ChallengeService, FakeFirebaseService> {
        val fakeFirebase = FakeFirebaseService()
        fakeFirebase.mockUser = user
        val xpService = XPService(fakeFirebase)
        val challengeService = TestableChallengeService(fakeFirebase, xpService, "2023-01-01")
        return Pair(challengeService, fakeFirebase)
    }

    @Test
    fun challenge_flow() = runBlocking {
        val user = User(id = "u1", username = "Test", xp = 0)
        val (service, fb) = createServices(user)
        
        // Generate
        val challenges = service.generateDailyChallenges("u1")
        assertEquals(3, challenges.size)
        
        // Update
        val cid = challenges[0].id
        // Hack: Update generic challenge goal to avoid logic complexity
        // Better: create specific challenge
        val cCustom = Challenge(id = "c1", goal = ChallengeGoal.Distance(1.0), reward = ChallengeReward(50, RewardType.XP))
        fb.saveChallenge("u1", cCustom)
        
        // Complete (100% progress)
        val updated = service.updateChallengeProgress("u1", "c1", 100.0)
        assertTrue(updated!!.completed)
        
        // XP awarded?
        assertEquals(50L, fb.lastUpdatedUser!!.xp)
    }
}

class LeaderboardServiceTest {
     private fun createService(users: List<User>): LeaderboardService {
        val fakeFirebase = FakeFirebaseService()
        fakeFirebase.usersList.addAll(users)
        return LeaderboardService(fakeFirebase)
    }

    @Test
    fun leaderboard_ranking() = runBlocking {
        val u1 = User(id = "u1", totalDistanceWalked = 100.0)
        val u2 = User(id = "u2", totalDistanceWalked = 200.0)
        
        val service = createService(listOf(u1, u2))
        val list = service.getWeeklyDistanceLeaderboard()
        
        assertEquals("u2", list[0].userId) // Higher distance first
        assertEquals("u1", list[1].userId)
    }
}
