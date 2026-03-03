package com.sidhart.walkover.utils

import com.sidhart.walkover.data.*
import java.util.*
import kotlin.random.Random

object ChallengeManager {

    // Generate 3 daily challenges (Easy, Medium, Hard) with different types
    fun generateDailyChallenges(date: Date = Date()): List<Challenge> {
        // Use date as seed for consistent daily challenges
        val seed = date.time / (24 * 60 * 60 * 1000) // Day-based seed
        val random = Random(seed)

        // Shuffle challenge types to ensure variety
        val shuffledTypes = ChallengeType.entries.shuffled(random)

        return listOf(
            createEasyChallenge(shuffledTypes[0]),
            createMediumChallenge(shuffledTypes[1 % shuffledTypes.size]),
            createHardChallenge(shuffledTypes[2 % shuffledTypes.size])
        )
    }


    private fun createEasyChallenge(type: ChallengeType): Challenge {
        return when (type) {
            ChallengeType.DISTANCE -> Challenge(
                type = ChallengeType.DISTANCE,
                difficulty = ChallengeDifficulty.EASY,
                title = "Short Stroll",
                description = "Walk 1 kilometer",
                targetValue = 1.0,
                xpReward = ChallengeDifficulty.EASY.xpReward
            )
            ChallengeType.DURATION -> Challenge(
                type = ChallengeType.DURATION,
                difficulty = ChallengeDifficulty.EASY,
                title = "Quick Walk",
                description = "Walk for 15 minutes",
                targetValue = 15.0,
                xpReward = ChallengeDifficulty.EASY.xpReward
            )
            ChallengeType.WALKS_COUNT -> Challenge(
                type = ChallengeType.WALKS_COUNT,
                difficulty = ChallengeDifficulty.EASY,
                title = "Get Moving",
                description = "Complete 1 walk",
                targetValue = 1.0,
                xpReward = ChallengeDifficulty.EASY.xpReward
            )
            ChallengeType.SPEED -> Challenge(
                type = ChallengeType.SPEED,
                difficulty = ChallengeDifficulty.EASY,
                title = "Steady Pace",
                description = "Maintain 3 km/h average speed",
                targetValue = 3.0,
                xpReward = ChallengeDifficulty.EASY.xpReward
            )
        }
    }

    private fun createMediumChallenge(type: ChallengeType): Challenge {
        return when (type) {
            ChallengeType.DISTANCE -> Challenge(
                type = ChallengeType.DISTANCE,
                difficulty = ChallengeDifficulty.MEDIUM,
                title = "Distance Runner",
                description = "Walk 3 kilometers",
                targetValue = 3.0,
                xpReward = ChallengeDifficulty.MEDIUM.xpReward
            )
            ChallengeType.DURATION -> Challenge(
                type = ChallengeType.DURATION,
                difficulty = ChallengeDifficulty.MEDIUM,
                title = "Endurance Walk",
                description = "Walk for 30 minutes",
                targetValue = 30.0,
                xpReward = ChallengeDifficulty.MEDIUM.xpReward
            )
            ChallengeType.WALKS_COUNT -> Challenge(
                type = ChallengeType.WALKS_COUNT,
                difficulty = ChallengeDifficulty.MEDIUM,
                title = "Multiple Missions",
                description = "Complete 2 walks",
                targetValue = 2.0,
                xpReward = ChallengeDifficulty.MEDIUM.xpReward
            )
            ChallengeType.SPEED -> Challenge(
                type = ChallengeType.SPEED,
                difficulty = ChallengeDifficulty.MEDIUM,
                title = "Brisk Walker",
                description = "Maintain 4.5 km/h average speed",
                targetValue = 4.5,
                xpReward = ChallengeDifficulty.MEDIUM.xpReward
            )
        }
    }

    private fun createHardChallenge(type: ChallengeType): Challenge {
        return when (type) {
            ChallengeType.DISTANCE -> Challenge(
                type = ChallengeType.DISTANCE,
                difficulty = ChallengeDifficulty.HARD,
                title = "Marathon Walker",
                description = "Walk 5 kilometers",
                targetValue = 5.0,
                xpReward = ChallengeDifficulty.HARD.xpReward
            )
            ChallengeType.DURATION -> Challenge(
                type = ChallengeType.DURATION,
                difficulty = ChallengeDifficulty.HARD,
                title = "Extended Journey",
                description = "Walk for 60 minutes",
                targetValue = 60.0,
                xpReward = ChallengeDifficulty.HARD.xpReward
            )
            ChallengeType.WALKS_COUNT -> Challenge(
                type = ChallengeType.WALKS_COUNT,
                difficulty = ChallengeDifficulty.HARD,
                title = "Walk Champion",
                description = "Complete 3 walks",
                targetValue = 3.0,
                xpReward = ChallengeDifficulty.HARD.xpReward
            )
            ChallengeType.SPEED -> Challenge(
                type = ChallengeType.SPEED,
                difficulty = ChallengeDifficulty.HARD,
                title = "Speed Demon",
                description = "Maintain 6 km/h average speed",
                targetValue = 6.0,
                xpReward = ChallengeDifficulty.HARD.xpReward
            )
        }
    }

    // Check if challenges need refresh (new day)
    fun shouldRefreshChallenges(lastRefreshTime: Long): Boolean {
        val lastRefreshDate = Calendar.getInstance().apply {
            timeInMillis = lastRefreshTime
        }
        val today = Calendar.getInstance()

        return lastRefreshDate.get(Calendar.DAY_OF_YEAR) != today.get(Calendar.DAY_OF_YEAR) ||
                lastRefreshDate.get(Calendar.YEAR) != today.get(Calendar.YEAR)
    }

    // Get today's date at midnight for consistent challenge generation
    fun getTodayMidnight(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }
}