package com.sidhart.walkover.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import com.sidhart.walkover.data.*
import com.sidhart.walkover.ui.theme.*
import java.text.DecimalFormat

@Composable
fun DailyChallengesSection(
    challenges: List<UserChallenge>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    // Display only first 3 challenges
    val displayChallenges = challenges.take(3)
    
    Column(modifier = modifier) {
        // Section Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.EmojiEvents,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Daily Challenges",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            ChallengesSkeleton()
        } else if (displayChallenges.isEmpty()) {
            EmptyChallengesCard()
        } else {
            displayChallenges.forEach { challenge ->
                ChallengeCard(challenge = challenge)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ChallengeCard(challenge: UserChallenge) {
    val progress = challenge.getProgressPercentage()
    
    // Cap the displayed progress at 100% (1.0f) even if user exceeded target
    val cappedProgress = progress.coerceIn(0f, 1f)
    val isOverCompleted = progress > 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (challenge.isCompleted)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (challenge.isCompleted) 0.dp else 1.dp),
        border = if (challenge.isCompleted)
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        else
            null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Challenge info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Challenge type icon
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getChallengeTypeIcon(challenge.challenge.type),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = challenge.challenge.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (challenge.isCompleted)
                                androidx.compose.ui.text.style.TextDecoration.LineThrough
                            else
                                null
                        )
                        Text(
                            text = challenge.challenge.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // XP reward or completion check
                if (challenge.isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    // reward bubble background adapts to theme (dark -> black, light -> white)
                    val xpBg = if (isSystemInDarkTheme()) Color.Black else Color.White
                    Surface(
                        color = xpBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "XP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonGreen
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+${challenge.challenge.xpReward}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            Column {
                LinearProgressIndicator(
                    progress = { cappedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    // progress line should use neon green to stand out
                    color = NeonGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatProgress(challenge),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isOverCompleted) "100%" else "${(progress * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengesSkeleton() {
    // new skeleton mimics the current card layout with placeholder shapes for icon, text, xp bubble and progress
    repeat(3) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp), // slightly taller for new design
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // circular icon placeholder
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                    // xp bubble placeholder
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                // progress bar placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun EmptyChallengesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No challenges available",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Check back tomorrow!",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getDifficultyColor(difficulty: ChallengeDifficulty): Color {
    return when (difficulty) {
        ChallengeDifficulty.EASY -> Color(0xFF4CAF50)
        ChallengeDifficulty.MEDIUM -> Color(0xFFFF9800)
        ChallengeDifficulty.HARD -> Color(0xFFF44336)
    }
}

private fun getDifficultyIcon(difficulty: ChallengeDifficulty): ImageVector {
    return when (difficulty) {
        ChallengeDifficulty.EASY -> Icons.Outlined.SentimentSatisfied
        ChallengeDifficulty.MEDIUM -> Icons.Default.TrendingUp
        ChallengeDifficulty.HARD -> Icons.Outlined.Whatshot
    }
}

private fun getChallengeTypeIcon(type: ChallengeType): ImageVector {
    return when (type) {
        ChallengeType.DISTANCE -> Icons.Outlined.Route
        ChallengeType.DURATION -> Icons.Outlined.Timer
        ChallengeType.WALKS_COUNT -> Icons.Default.DirectionsWalk
        ChallengeType.SPEED -> Icons.Outlined.Speed
    }
}

private fun getChallengeTypeColor(type: ChallengeType): Color {
    return when (type) {
        ChallengeType.DISTANCE -> ElectricBlue
        ChallengeType.DURATION -> CoralOrange
        ChallengeType.WALKS_COUNT -> SuccessGreen
        ChallengeType.SPEED -> WarningAmber
    }
}

private fun formatProgress(challenge: UserChallenge): String {
    val current = challenge.currentProgress.coerceAtMost(challenge.challenge.targetValue)
    val target = challenge.challenge.targetValue

    return when (challenge.challenge.type) {
        ChallengeType.DISTANCE -> "${String.format("%.1f", current)}/${String.format("%.1f", target)} km"
        ChallengeType.DURATION -> "${current.toInt()}/${target.toInt()} min"
        ChallengeType.WALKS_COUNT -> "${current.toInt()}/${target.toInt()} walks"
        ChallengeType.SPEED -> "${String.format("%.1f", target)} km/h"
    }
}