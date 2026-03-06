package com.sidhart.walkover.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
