package com.sidhart.walkover.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sidhart.walkover.data.User
import com.sidhart.walkover.data.Walk
import com.sidhart.walkover.service.FirebaseService
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.*
import com.sidhart.walkover.data.UserChallenge
import com.sidhart.walkover.data.StreakData
import com.sidhart.walkover.ui.components.CommonHeader
import com.sidhart.walkover.ui.theme.NeonGreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    firebaseService: FirebaseService,
    onNavigateToWalkHistory: () -> Unit,
    onNavigateToDetailedStats: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onNavigateToFriendsList: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var userData by remember { mutableStateOf<User?>(null) }
    var allWalks by remember { mutableStateOf<List<Walk>>(emptyList()) }
    var isLoadingUser by remember { mutableStateOf(true) }
    // var isLoadingWalks by remember { mutableStateOf(true) } // no longer used for button
    var dailyChallenges by remember { mutableStateOf<List<UserChallenge>>(emptyList()) }
    var streakData by remember { mutableStateOf<StreakData?>(null) }
    var isLoadingStreak by remember { mutableStateOf(true) }
    var todayWalks by remember { mutableStateOf<List<Walk>>(emptyList()) }
    var followersCount by remember { mutableStateOf<Int?>(null) }
    val decimalFormat = remember { DecimalFormat("#,##0.##") }


    // Load all data
    LaunchedEffect(Unit) {
        scope.launch {
            // Load user data
            firebaseService.getCurrentUserData().fold(
                onSuccess = { user ->
                    userData = user
                    isLoadingUser = false
                },
                onFailure = { error ->
                    isLoadingUser = false
                    // Error handling suppressed
                }
            )

            // Load streak data
            firebaseService.getCurrentUserStreak().fold(
                onSuccess = { streak ->
                    streakData = streak
                    isLoadingStreak = false
                },
                onFailure = {
                    isLoadingStreak = false
                }
            )

            // Load daily challenges
            firebaseService.getDailyChallenges().fold(
                onSuccess = { challenges ->
                    dailyChallenges = challenges
                },
                onFailure = { error ->
                    // Error handling suppressed
                }
            )

            // Load followers count
            firebaseService.getFollowers().fold(
                onSuccess = { followers ->
                    followersCount = followers.size
                },
                onFailure = {
                    followersCount = 0
                }
            )

            // Load walks
            firebaseService.getWalks().fold(
                onSuccess = { walks ->
                    allWalks = walks
                    
                    // Filter today's walks for streak card
                    val todayStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time
                    todayWalks = walks.filter { it.timestamp >= todayStart }
                },
                onFailure = { error ->
                    // Error handling suppressed
                }
            )
        }
    }


    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header (profile header should simply read "Profile" without logo)
            CommonHeader(
                title = "Profile",
                onSettingsClick = onNavigateToSettings,
                showLogo = false,
                onAddFriendClick = onNavigateToFriendsList,
                onNavigateBack = onNavigateBack
            )

            // User Profile Card
            if (isLoadingUser) {
                UserProfileCardSkeleton()
            } else if (userData != null) {
                UserProfileCardWithStats(
                    userData = userData!!,
                    allWalks = allWalks,
                    followersCount = followersCount,
                    decimalFormat = decimalFormat,
                    onNavigateToFriendsList = onNavigateToFriendsList
                )
            }

            // Streak Card (NEW - prominent placement)
            if (isLoadingStreak) {
                StreakCardSkeleton()
            } else if (streakData != null) {
                StreakCard(
                    streakData = streakData!!,
                    todayWalks = todayWalks,
                    dailyChallenges = dailyChallenges
                )
            }

            // Daily Challenges


            // View Detailed Stats Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = true, onClick = onNavigateToDetailedStats),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = null,
                                // neon tint for detailed stats icon
                                tint = NeonGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Detailed Statistics",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "See your detailed stats",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            RecentWalkButton(onClick = onNavigateToWalkHistory)
        }
    }
}

@Composable
private fun UserProfileCardWithStats(
    userData: User,
    allWalks: List<Walk>,
    followersCount: Int?,
    decimalFormat: DecimalFormat,
    onNavigateToFriendsList: () -> Unit
) {
    val totalDistance = allWalks.sumOf { it.distanceCovered }
    val progress = userData.getProgressToNextLevel()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // User Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userData.username.firstOrNull()?.uppercase() ?: "U",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // User Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userData.username,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (!userData.isAnonymous && userData.email.isNotEmpty()) {
                        Text(
                            text = userData.email,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Guest User",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Level Badge
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Level",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = userData.currentLevel.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = userData.totalWalks.toString(),
                    label = "Walks",
                    color = MaterialTheme.colorScheme.onSurface
                )

                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                StatItem(
                    value = followersCount?.toString() ?: "-",
                    label = "Stalkers",
                    color = MaterialTheme.colorScheme.onSurface,
                    onClick = onNavigateToFriendsList
                )

                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                StatItem(
                    value = "${decimalFormat.format(totalDistance / 1000)}",
                    label = "Kilometers",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // XP Progress bar
            Column {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    // progress line should use neon green to match Discover
                    color = NeonGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${userData.getXPToNextLevel()} XP to Level ${userData.currentLevel + 1}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StreakCard(
    streakData: StreakData,
    todayWalks: List<Walk>,
    dailyChallenges: List<UserChallenge> = emptyList()  // ADD THIS
) {
    // Check if at least one challenge is completed today OR if user has walked today
    val hasCompletedChallenge = dailyChallenges.any { it.isCompleted }
    val hasCompletedMinimum = hasCompletedChallenge || todayWalks.isNotEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Flame icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasCompletedMinimum)
                                    Color(0xFFFFD700).copy(alpha = 0.15f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = if (hasCompletedMinimum)
                                Color(0xFFFFD700)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Current Streak",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${streakData.currentStreak}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "days",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }

                // Bonus badge (only if streak >= 7)
                if (streakData.currentStreak >= 7) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "+${calculateStreakBonusPercentage(streakData.currentStreak)}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status indicator
            Surface(
                color = if (hasCompletedMinimum)
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (hasCompletedMinimum)
                            Icons.Default.CheckCircle
                        else
                            Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (hasCompletedMinimum)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Complete 1 challenge to maintain streak",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (hasCompletedMinimum) 
                             MaterialTheme.colorScheme.onSurfaceVariant
                             else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (hasCompletedMinimum) 
                             androidx.compose.ui.text.style.TextDecoration.LineThrough 
                             else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Removed Compact Stats
        }
    }
}


@Composable
fun StreakCardSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Flame icon placeholder
                    ShimmerEffect(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )

                    Column {
                        ShimmerEffect(
                            modifier = Modifier
                                .width(80.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ShimmerEffect(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }

                // Bonus badge placeholder
                ShimmerEffect(
                    modifier = Modifier
                        .width(60.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status indicator skeleton
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Compact stats skeleton
            // Removed Compact stats skeleton
        }
    }
}

private fun calculateStreakBonusPercentage(currentStreak: Int): Int {
    return when {
        currentStreak >= 90 -> 35
        currentStreak >= 60 -> 30
        currentStreak >= 30 -> 25
        currentStreak >= 14 -> 20
        currentStreak >= 7 -> 15
        else -> 10
    }
}

@Composable
fun RecentWalkButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = true, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "Walk History",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "View your recent walks",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}