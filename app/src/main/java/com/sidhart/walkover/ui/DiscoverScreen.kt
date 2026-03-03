package com.sidhart.walkover.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import com.sidhart.walkover.data.*
import com.sidhart.walkover.service.FirebaseService
import com.sidhart.walkover.ui.components.CommonHeader
import kotlinx.coroutines.launch
import com.sidhart.walkover.ui.theme.NeonGreen
import java.util.Locale

@Composable
fun DiscoverScreen(
    firebaseService: FirebaseService,
    onNavigateToSettings: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToFriendsList: () -> Unit,
    onNavigateToSearchFriends: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var weeklyStats by remember { mutableStateOf<WeeklyStats?>(null) }
    var isLoadingStats by remember { mutableStateOf(true) }

    var dailyChallenges by remember { mutableStateOf<List<UserChallenge>>(emptyList()) }
    var isLoadingChallenges by remember { mutableStateOf(true) }

    var recommendations by remember { mutableStateOf<List<FriendRecommendation>>(emptyList()) }
    var isLoadingRecommendations by remember { mutableStateOf(true) }
    var optimisticFollowing by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Load data
    LaunchedEffect(Unit) {
        scope.launch {
            // Load weekly stats
            firebaseService.getWeeklyStats().fold(
                onSuccess = { stats ->
                    weeklyStats = stats
                    isLoadingStats = false
                },
                onFailure = {
                    weeklyStats = WeeklyStats()
                    isLoadingStats = false
                }
            )

            // Load challenges
            firebaseService.getDailyChallenges().fold(
                onSuccess = { challenges ->
                    dailyChallenges = challenges
                    isLoadingChallenges = false
                },
                onFailure = {
                    dailyChallenges = emptyList()
                    isLoadingChallenges = false
                }
            )

            // Load Recommendations
            firebaseService.getFriendRecommendations().fold(
                onSuccess = { recoms ->
                    recommendations = recoms
                    isLoadingRecommendations = false
                },
                onFailure = {
                    isLoadingRecommendations = false
                }
            )
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Header
        item {
            CommonHeader(
                title = "WalkOver",
                onSettingsClick = onNavigateToSettings,
                onAddFriendClick = onNavigateToFriendsList,
                onNavigateBack = onNavigateBack
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Weekly Stats - Prominent placement
        item {
            WeeklyStatsSection(
                stats = weeklyStats,
                isLoading = isLoadingStats,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(28.dp))
        }

        // Quick Actions Grid
        item {
            QuickActionsGrid(
                onLeaderboardClick = onNavigateToLeaderboard,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(28.dp))
        }

        // People to Stalk (Recommendations)
        item {
            PeopleToStalkSection(
                recommendations = recommendations.take(10), // Limit to 10 visible at any time
                isLoading = isLoadingRecommendations,
                optimisticFollowing = optimisticFollowing,
                onFollowClick = { rec -> 
                    optimisticFollowing = optimisticFollowing + rec.user.id
                    // Remove optimistic following from list immediately and let another fill in
                    recommendations = recommendations.filter { it.user.id != rec.user.id }
                    scope.launch {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                            firebaseService.followUser(rec.user.id, rec.user.username)
                        }
                    }
                },
                onRemoveClick = { rec -> 
                    // Remove from list so a new person can take their place
                    recommendations = recommendations.filter { it.user.id != rec.user.id }
                },
                onSeeAllClick = onNavigateToSearchFriends,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(28.dp))
        }

        // Daily Challenges Section
        item {
            com.sidhart.walkover.ui.components.DailyChallengesSection(
                challenges = dailyChallenges,
                isLoading = isLoadingChallenges,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun WeeklyStatsSection(
    stats: WeeklyStats?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "This Week",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (isLoading) {
            WeeklyStatsSkeleton(modifier = Modifier.fillMaxWidth())
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Activities (Walks)
                StravaStatCard(
                    modifier = Modifier.weight(1f),
                    label = "Activities",
                    value = stats?.totalWalks?.toString() ?: "0",
                    change = stats?.walksChangePercent ?: 0.0,
                    accentColor = NeonGreen
                )

                // Time
                StravaStatCard(
                    modifier = Modifier.weight(1f),
                    label = "Time",
                    value = formatWeeklyDuration(stats?.totalTimeMinutes ?: 0),
                    change = stats?.timeChangePercent ?: 0.0,
                    accentColor = NeonGreen
                )

                // Distance
                StravaStatCard(
                    modifier = Modifier.weight(1f),
                    label = "Distance",
                    value = String.format(Locale.US, "%.1f", stats?.totalDistanceKm ?: 0.0),
                    unit = "km",
                    change = stats?.distanceChangePercent ?: 0.0,
                    accentColor = NeonGreen
                )
            }
        }
    }
}

@Composable
private fun StravaStatCard(
    label: String,
    value: String,
    change: Double,
    accentColor: Color,
    modifier: Modifier = Modifier,
    unit: String = ""
) {
    val isDark = isSystemInDarkTheme()
    
    Surface(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (isDark) Color(0xFF16161F) else MaterialTheme.colorScheme.surface,
        border = if (!isDark) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)) else null,
        tonalElevation = 2.dp,
        shadowElevation = if (isDark) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                val valueFontSize = when {
                    value.length >= 6 -> 18.sp
                    value.length >= 4 -> 21.sp
                    else -> 26.sp
                }
                
                Text(
                    text = value,
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(1.dp))
                    Text(
                        text = unit,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
            
            // Bottom pill for progress
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.wrapContentSize()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (change >= 0) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (change >= 0) Color(0xFF4CAF50) else Color(0xFFFF5252)
                    )
                    Text(
                        text = "${abs(change).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionsGrid(
    onLeaderboardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Quick Access",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = true, onClick = onLeaderboardClick),
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
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            // trophy icon should be neon green
                            tint = NeonGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Global Leaderboard",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "See how you rank",
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
}

@Composable
private fun PeopleToStalkSection(
    recommendations: List<FriendRecommendation>,
    isLoading: Boolean,
    optimisticFollowing: Set<String>,
    onFollowClick: (FriendRecommendation) -> Unit,
    onRemoveClick: (FriendRecommendation) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "People to Stalk",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Level up your network",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (recommendations.isNotEmpty() && !isLoading) {
                TextButton(onClick = onSeeAllClick) {
                    Text(
                        text = "See All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            PeopleToStalkSkeleton()
        } else if (recommendations.isEmpty()) {
            EmptyState(
                icon = Icons.Default.PersonAdd,
                title = "No Suggestions Yet",
                subtitle = "Complete walks to get recommendations"
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                items(recommendations.take(5)) { recommendation ->
                    FollowCard(
                        recommendation = recommendation,
                        isFollowing = optimisticFollowing.contains(recommendation.user.id),
                        onFollowClick = { onFollowClick(recommendation) },
                        onRemoveClick = { onRemoveClick(recommendation) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FollowCard(
    recommendation: FriendRecommendation,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        NeonGreen.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = recommendation.user.username.firstOrNull()?.uppercase() ?: "U",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = recommendation.user.username,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Lvl ${recommendation.user.currentLevel}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = recommendation.reason,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(modifier = Modifier.weight(1f))

            // Follow Button
            Button(
                onClick = onFollowClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else NeonGreen,
                    contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp),
                enabled = !isFollowing
            ) {
                    if (isFollowing) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Stalking",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (recommendation.user.isFollowedBy) "Stalk back" else "Stalk",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Close button
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatWeeklyDuration(minutes: Long): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 -> "${hours}h ${mins}m"
        else -> "${mins}m"
    }
}
// ---- additional skeleton composables for discover loading states ----

@Composable
private fun WeeklyStatsSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(3) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(14.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        )
                        Box(
                            modifier = Modifier
                                .width(30.dp)
                                .height(24.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        )
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(12.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeopleToStalkSkeleton(modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        items(3) {
            Card(
                modifier = Modifier.size(160.dp, 180.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface // keep card white
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    )
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(14.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    )
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(14.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    )
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

