package com.sidhart.walkover.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sidhart.walkover.data.User
import com.sidhart.walkover.data.Walk
import com.sidhart.walkover.service.FirebaseService
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import androidx.compose.ui.graphics.graphicsLayer
import java.util.*

enum class ProfileTab(val displayName: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Outlined.GridView),
    STATS("Statistics", Icons.Outlined.Insights)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    firebaseService: FirebaseService,
    onNavigateToWalkHistory: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var userData by remember { mutableStateOf<User?>(null) }
    var recentWalks by remember { mutableStateOf<List<Walk>>(emptyList()) }
    var allWalks by remember { mutableStateOf<List<Walk>>(emptyList()) }
    var isLoadingUser by remember { mutableStateOf(true) }
    var isLoadingWalks by remember { mutableStateOf(true) }
    var weeklyStats by remember { mutableStateOf<WeeklyStats?>(null) }
    var selectedTab by remember { mutableStateOf(ProfileTab.OVERVIEW) }

    val decimalFormat = remember { DecimalFormat("#,##0.##") }

    // Load user data and walks
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
                    Toast.makeText(context, "Failed to load profile: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )

            // Load all walks
            firebaseService.getWalks().fold(
                onSuccess = { walks ->
                    allWalks = walks
                    recentWalks = walks.take(5)
                    weeklyStats = calculateWeeklyStats(walks)
                    isLoadingWalks = false
                },
                onFailure = { error ->
                    isLoadingWalks = false
                    Toast.makeText(context, "Failed to load walks: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Profile Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 20.dp)
        ) {
            // Title
            Text(
                text = "Profile",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(20.dp))

            // User Profile Card with Stats
            if (isLoadingUser) {
                UserProfileCardSkeleton()  // Use proper skeleton
            } else if (userData != null) {
                UserProfileCardWithStats(
                    userData = userData!!,
                    allWalks = allWalks,
                    decimalFormat = decimalFormat
                )
            }
        }

        // Tab Navigation
        TabNavigation(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        )

        // Tab Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            when (selectedTab) {
                ProfileTab.OVERVIEW -> {
                    OverviewTabContent(
                        userData = userData,
                        recentWalks = recentWalks,
                        isLoading = isLoadingWalks,
                        onNavigateToWalkHistory = onNavigateToWalkHistory,
                        decimalFormat = decimalFormat
                    )
                }
                ProfileTab.STATS -> {
                    StatsTabContent(
                        weeklyStats = weeklyStats,
                        allWalks = allWalks,
                        isLoading = isLoadingWalks,
                        decimalFormat = decimalFormat
                    )
                }
            }
        }
    }
}

@Composable
private fun UserProfileCardWithStats(
    userData: User,
    allWalks: List<Walk>,
    decimalFormat: DecimalFormat
) {
    val totalDistance = allWalks.sumOf { it.distanceCovered }
    val level = calculateLevel(userData.totalWalks)
    val nextLevelWalks = getNextLevelRequirement(level)
    val progress = (userData.totalWalks.toFloat() / nextLevelWalks).coerceIn(0f, 1f)

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
                            text = level.toString(),
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

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = userData.totalWalks.toString(),
                    label = "Walks",
                    color = MaterialTheme.colorScheme.primary
                )

                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                StatItem(
                    value = "${decimalFormat.format(totalDistance / 1000)}",
                    label = "Kilometers",
                    color = MaterialTheme.colorScheme.primary
                )

                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                StatItem(
                    value = "${(progress * 100).toInt()}%",
                    label = "To Lvl ${level + 1}",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            Column {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${userData.totalWalks} / $nextLevelWalks walks",
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
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
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
private fun TabNavigation(
    selectedTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ProfileTab.entries.forEach { tab ->
                TabButton(
                    tab = tab,
                    isSelected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TabButton(
    tab: ProfileTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = tab.displayName,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OverviewTabContent(
    userData: User?,
    recentWalks: List<Walk>,
    isLoading: Boolean,
    onNavigateToWalkHistory: () -> Unit,
    decimalFormat: DecimalFormat
) {
    if (isLoading) {
        // Show skeleton for entire overview tab
        OverviewTabSkeleton()
    } else {
        Column {
            // Achievements Section
            if (userData != null) {
                AchievementsCompactSection(userData = userData)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Recent Activity
            RecentWalksSection(
                walks = recentWalks,
                isLoading = false, // Don't show nested loading
                onViewAll = onNavigateToWalkHistory,
                decimalFormat = decimalFormat
            )
        }
    }
}

@Composable
private fun AchievementsCompactSection(userData: User) {
    val allBadges = getAllAchievementBadges(userData)
    val unlockedCount = allBadges.count { it.isUnlocked }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Achievements",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "$unlockedCount/${allBadges.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                allBadges.take(6).chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { badge ->
                            CompactBadge(badge = badge)
                        }
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    if (row != allBadges.take(6).chunked(3).last()) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactBadge(badge: BadgeWithStatus) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (badge.isUnlocked)
                        badge.color.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = badge.icon,
                contentDescription = badge.name,
                tint = if (badge.isUnlocked) badge.color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = badge.name,
            fontSize = 11.sp,
            color = if (badge.isUnlocked)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            fontWeight = if (badge.isUnlocked) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun StatsTabContent(
    weeklyStats: WeeklyStats?,
    allWalks: List<Walk>,
    isLoading: Boolean,
    decimalFormat: DecimalFormat
) {
    if (isLoading) {
        // Show skeleton for entire stats tab
        StatsTabSkeleton()
    } else if (weeklyStats != null && allWalks.isNotEmpty()) {
        Column {
            // Use the animated version with grey stat cards + graph
            WeeklyProgressCardWithGraph(
                stats = weeklyStats,
                decimalFormat = decimalFormat,
                walks = allWalks
            )
        }
    } else {
        EmptyStateCard(
            icon = Icons.Outlined.Insights,
            title = "No statistics yet",
            subtitle = "Start walking to see your progress"
        )
    }
}
@Composable
private fun WeeklyStatsCards(
    stats: WeeklyStats,
    decimalFormat: DecimalFormat
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SmallStatCard(
            value = stats.walks.toString(),
            label = "Walks",
            subtitle = "This Week",
            icon = Icons.Outlined.DirectionsWalk,
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
        SmallStatCard(
            value = "${decimalFormat.format(stats.distance / 1000)}",
            label = "Kilometers",
            subtitle = "This Week",
            icon = Icons.Outlined.Route,
            color = Color(0xFF2196F3),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SmallStatCard(
    value: String,
    label: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun WeeklyGraphCard(
    stats: WeeklyStats,
    walks: List<Walk>,
    decimalFormat: DecimalFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Weekly Activity",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (stats.dailyData.isNotEmpty()) {
                WeeklyGraph(
                    dailyData = stats.dailyData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }
        }
    }
}

// Helper functions
fun calculateLevel(totalWalks: Int): Int {
    return when {
        totalWalks < 5 -> 1
        totalWalks < 15 -> 2
        totalWalks < 30 -> 3
        totalWalks < 50 -> 4
        totalWalks < 100 -> 5
        totalWalks < 200 -> 6
        totalWalks < 500 -> 7
        else -> 8
    }
}

fun getNextLevelRequirement(currentLevel: Int): Int {
    return when (currentLevel) {
        1 -> 5
        2 -> 15
        3 -> 30
        4 -> 50
        5 -> 100
        6 -> 200
        7 -> 500
        8 -> 1000
        else -> 1000
    }
}