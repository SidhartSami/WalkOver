package com.sidhart.walkover.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
    var monthlyStats by remember { mutableStateOf<MonthlyStats?>(null) }

    val decimalFormat = remember { DecimalFormat("#,##0.##") }

    // Load user data and walks
    LaunchedEffect(Unit) {
        scope.launch {
            // Load user data
            firebaseService.getCurrentUserData().fold(
                onSuccess = { user ->
                    userData = user
                    isLoadingUser = false
                    android.util.Log.d("ProfileScreen", "User data loaded: ${user.username}")
                },
                onFailure = { error ->
                    isLoadingUser = false
                    android.util.Log.e("ProfileScreen", "Failed to load user data", error)
                    Toast.makeText(context, "Failed to load profile: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )

            // Load all walks
            firebaseService.getWalks().fold(
                onSuccess = { walks ->
                    android.util.Log.d("ProfileScreen", "Loaded ${walks.size} walks")
                    allWalks = walks
                    recentWalks = walks.take(3) // Get 3 most recent

                    // Calculate weekly and monthly stats
                    weeklyStats = calculateWeeklyStats(walks)
                    monthlyStats = calculateMonthlyStats(walks)

                    isLoadingWalks = false
                },
                onFailure = { error ->
                    isLoadingWalks = false
                    android.util.Log.e("ProfileScreen", "Failed to load walks", error)
                    Toast.makeText(context, "Failed to load walks: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profile",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Icon(
                imageVector = Icons.Outlined.AccountCircle,
                contentDescription = "Profile",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }

        // User Profile Card
        if (isLoadingUser) {
            LoadingCard()
        } else if (userData != null) {
            AnimatedVisibility(
                visible = userData != null,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                    initialOffsetY = { -20 },
                    animationSpec = tween(600)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                UserProfileCard(userData = userData!!, decimalFormat = decimalFormat)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Weekly Progress with Graph - Moved above achievements
        if (weeklyStats != null) {
            WeeklyProgressCardWithGraph(
                stats = weeklyStats!!,
                decimalFormat = decimalFormat,
                walks = allWalks
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Achievement Badges
        AchievementBadgesSection(userData = userData)

        Spacer(modifier = Modifier.height(20.dp))

        // Recent Walks Section
        RecentWalksSection(
            walks = recentWalks,
            isLoading = isLoadingWalks,
            onViewAll = onNavigateToWalkHistory,
            decimalFormat = decimalFormat
        )
    }
}

@Composable
fun UserProfileCard(userData: User, decimalFormat: DecimalFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userData.username.firstOrNull()?.uppercase() ?: "U",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // User Info - Fixed to prevent overlapping
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = userData.username,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (!userData.isAnonymous && userData.email.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = userData.email,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    } else if (userData.isAnonymous) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Guest",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AchievementBadgesSection(userData: User?) {
    if (userData == null) return

    val allBadges = getAllAchievementBadges(userData)
        .sortedByDescending { it.isUnlocked }
    var isExpanded by remember { mutableStateOf(false) }

    val displayedBadges = if (isExpanded) allBadges else allBadges.take(3)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.EmojiEvents,
                    contentDescription = "Achievements",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Achievements",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${allBadges.count { it.isUnlocked }}/${allBadges.size}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                displayedBadges.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { badge ->
                            AchievementBadge(badge = badge)
                        }
                        // Fill remaining slots
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.width(90.dp))
                        }
                    }
                    if (row != displayedBadges.chunked(3).last()) {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // Expand/Collapse button
                if (allBadges.size > 3) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isExpanded) "Show Less" else "Show All Achievements",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementBadge(badge: BadgeWithStatus) {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(90.dp)
            .clickable { if (!badge.isUnlocked) showDialog = true }
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    if (badge.isUnlocked) {
                        badge.color.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = badge.icon,
                contentDescription = badge.name,
                tint = if (badge.isUnlocked) badge.color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(36.dp)
            )
            if (!badge.isUnlocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = badge.name,
            fontSize = 12.sp,
            color = if (badge.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontWeight = if (badge.isUnlocked) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
        Text(
            text = badge.description,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }

    // Dialog for locked badges
    if (showDialog && !badge.isUnlocked) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(badge.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = badge.icon,
                            contentDescription = badge.name,
                            tint = badge.color,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = badge.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "How to unlock",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = badge.unlockHint,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        "Got it!",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }
}

@Composable
fun WeeklyProgressCardWithGraph(
    stats: WeeklyStats,
    decimalFormat: DecimalFormat,
    walks: List<Walk>
) {
    // Animation state for the entire section
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(600)) +
                slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Three stat containers above "This Week" heading
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GreyStatCard(
                    label = "Walks",
                    value = stats.walks.toString(),
                    icon = Icons.Outlined.DirectionsWalk,
                    iconColor = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f),
                    animationDelay = 0
                )
                GreyStatCard(
                    label = "Distance",
                    value = "${decimalFormat.format(stats.distance / 1000)} km",
                    icon = Icons.Outlined.Route,
                    iconColor = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f),
                    animationDelay = 100
                )
                GreyStatCard(
                    label = "Avg/Day",
                    value = "${decimalFormat.format(stats.distance / 7000)} km",
                    icon = Icons.Outlined.TrendingUp,
                    iconColor = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f),
                    animationDelay = 200
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "This Week" Card with Graph
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = "Weekly",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "This Week",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Weekly Graph
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
    }
}

@Composable
fun GreyStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    animationDelay: Int = 0
) {
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()

    // Scale animation for card appearance
    var isVisible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(animationDelay.toLong())
        isVisible = true
    }

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun WeeklyGraph(
    dailyData: List<DailyStat>,
    modifier: Modifier = Modifier
) {
    // Get colors outside Canvas scope
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val yAxisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val xAxisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val barColor = MaterialTheme.colorScheme.primary
    val emptyBarColor = MaterialTheme.colorScheme.surfaceVariant

    // Convert distances to km for display
    val dailyDataKm = dailyData.map { it.copy(distance = it.distance / 1000.0) }
    val maxDistanceKm = dailyDataKm.maxOfOrNull { it.distance } ?: 1.0

    // Calculate Y-axis grid lines (5 horizontal lines for better spacing)
    val yAxisSteps = 5
    val stepValue = if (maxDistanceKm > 0) (maxDistanceKm / yAxisSteps).let {
        // Round up to nice numbers
        when {
            it < 0.1 -> 0.1
            it < 0.5 -> 0.5
            it < 1.0 -> 1.0
            it < 2.0 -> 2.0
            it < 5.0 -> 5.0
            else -> (it / 5.0).toInt() * 5.0
        }
    } else 1.0
    val maxYValue = stepValue * yAxisSteps

    val animatedHeights = remember(dailyDataKm) {
        dailyDataKm.map { Animatable(0f) }
    }

    LaunchedEffect(dailyDataKm) {
        animatedHeights.forEachIndexed { index, animatable ->
            kotlinx.coroutines.delay(index * 80L) // Stagger the animation
            val targetHeight = (dailyDataKm[index].distance / maxYValue).toFloat().coerceIn(0f, 1f)
            animatable.animateTo(
                targetValue = targetHeight,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    Column(modifier = modifier) {
        // Main graph area with grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Grid lines background
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 50.dp, end = 8.dp, bottom = 32.dp, top = 8.dp)
            ) {
                // Draw horizontal grid lines
                repeat(yAxisSteps + 1) { i ->
                    val yPos = size.height * (i.toFloat() / yAxisSteps)
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, yPos),
                        end = Offset(size.width, yPos),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // Y-axis labels (left side)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(50.dp)
                    .padding(end = 8.dp, bottom = 32.dp, top = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(yAxisSteps + 1) { i ->
                    val value = maxYValue - (i * stepValue)
                    Text(
                        text = if (value >= 0.1) "%.1f".format(value) else "0",
                        fontSize = 10.sp,
                        color = yAxisLabelColor,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Graph bars and labels
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 50.dp, end = 8.dp, bottom = 32.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyDataKm.forEachIndexed { index, day ->
                    val height by animatedHeights[index].asState()

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Distance value on top of bar (if there's data)
                        if (day.distance > 0 && height > 0.15f) {
                            Text(
                                text = "%.1f".format(day.distance),
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(11.dp))
                        }

                        // Bar with minimum height for visibility
                        val minHeight = 0.05f
                        val displayHeight = if (height < minHeight && day.distance > 0) minHeight else height

                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .fillMaxHeight(displayHeight)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (day.distance > 0) {
                                        barColor.copy(alpha = 0.85f)
                                    } else {
                                        emptyBarColor.copy(alpha = 0.4f)
                                    }
                                )
                        )
                    }
                }
            }
        }

        // Day labels at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 50.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dailyDataKm.forEach { day ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.day,
                        fontSize = 11.sp,
                        color = if (day.distance > 0)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (day.distance > 0) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Unit label
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 4.dp)
        ) {
            Text(
                text = "Distance (km)",
                fontSize = 9.sp,
                color = xAxisLabelColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun WeeklyProgressCard(stats: WeeklyStats, decimalFormat: DecimalFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "Weekly",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "This Week",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProgressStatItem(
                    label = "Walks",
                    value = stats.walks.toString(),
                    icon = Icons.Outlined.DirectionsWalk,
                    color = Color(0xFF4CAF50)
                )
                ProgressStatItem(
                    label = "Distance",
                    value = "${decimalFormat.format(stats.distance / 1000)} km",
                    icon = Icons.Outlined.Route,
                    color = Color(0xFF2196F3)
                )
                ProgressStatItem(
                    label = "Area",
                    value = "${decimalFormat.format(stats.area)} m²",
                    icon = Icons.Outlined.GridOn,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
fun MonthlyProgressCard(stats: MonthlyStats, decimalFormat: DecimalFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = "Monthly",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "This Month",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProgressStatItem(
                    label = "Walks",
                    value = stats.walks.toString(),
                    icon = Icons.Outlined.DirectionsWalk,
                    color = Color(0xFF4CAF50)
                )
                ProgressStatItem(
                    label = "Distance",
                    value = "${decimalFormat.format(stats.distance / 1000)} km",
                    icon = Icons.Outlined.Route,
                    color = Color(0xFF2196F3)
                )
                ProgressStatItem(
                    label = "Area",
                    value = "${decimalFormat.format(stats.area)} m²",
                    icon = Icons.Outlined.GridOn,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
fun ProgressStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RecentWalksSection(
    walks: List<Walk>,
    isLoading: Boolean,
    onViewAll: () -> Unit,
    decimalFormat: DecimalFormat
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = "Recent",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recent Walks",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            TextButton(
                onClick = onViewAll,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "View All",
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View All",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            LoadingCard()
        } else if (walks.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsWalk,
                        contentDescription = "No walks",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No walks yet",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Start walking to track your activity",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            walks.forEach { walk ->
                RecentWalkItem(walk = walk, decimalFormat = decimalFormat)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun RecentWalkItem(walk: Walk, decimalFormat: DecimalFormat) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.DirectionsWalk,
                    contentDescription = "Walk",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormat.format(walk.timestamp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

// Data classes
data class WeeklyStats(
    val walks: Int,
    val distance: Double,
    val area: Double,
    val dailyData: List<DailyStat> = emptyList() // For graph
)

data class DailyStat(
    val day: String, // "Mon", "Tue", etc.
    val distance: Double, // in meters
    val walks: Int
)

data class MonthlyStats(
    val walks: Int,
    val distance: Double,
    val area: Double
)

data class BadgeWithStatus(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val isUnlocked: Boolean,
    val unlockHint: String
)

// Helper functions

fun getAllAchievementBadges(user: User): List<BadgeWithStatus> {
    val badges = mutableListOf<BadgeWithStatus>()

    // First Walk
    badges.add(BadgeWithStatus(
        name = "First Steps",
        description = "1 walk",
        icon = Icons.Outlined.DirectionsWalk,
        color = Color(0xFF4CAF50),
        isUnlocked = user.totalWalks >= 1,
        unlockHint = "Complete your first walk"
    ))

    // Walk milestones
    badges.add(BadgeWithStatus(
        name = "Explorer",
        description = "10 walks",
        icon = Icons.Outlined.Explore,
        color = Color(0xFF2196F3),
        isUnlocked = user.totalWalks >= 10,
        unlockHint = "Complete 10 walks"
    ))

    badges.add(BadgeWithStatus(
        name = "Adventurer",
        description = "50 walks",
        icon = Icons.Outlined.Hiking,
        color = Color(0xFFFF9800),
        isUnlocked = user.totalWalks >= 50,
        unlockHint = "Complete 50 walks"
    ))

    badges.add(BadgeWithStatus(
        name = "Champion",
        description = "100 walks",
        icon = Icons.Outlined.EmojiEvents,
        color = Color(0xFFFFD700),
        isUnlocked = user.totalWalks >= 100,
        unlockHint = "Complete 100 walks"
    ))

    // Distance milestones
    badges.add(BadgeWithStatus(
        name = "Kilometer",
        description = "1 km",
        icon = Icons.Outlined.Route,
        color = Color(0xFF9C27B0),
        isUnlocked = user.totalDistanceWalked >= 1000,
        unlockHint = "Walk a total distance of 1 kilometer (1,000 meters)"
    ))

    badges.add(BadgeWithStatus(
        name = "Marathon",
        description = "10 km",
        icon = Icons.Outlined.DirectionsRun,
        color = Color(0xFFE91E63),
        isUnlocked = user.totalDistanceWalked >= 10000,
        unlockHint = "Walk a total distance of 10 kilometers (10,000 meters)"
    ))

    return badges
}

// Calculate weekly stats from walks
fun calculateWeeklyStats(walks: List<Walk>): WeeklyStats {
    val calendar = Calendar.getInstance()
    val now = System.currentTimeMillis()
    val weekAgo = now - (7 * 24 * 60 * 60 * 1000L)

    val weeklyWalks = walks.filter { it.timestamp.time >= weekAgo }
    val totalDistance = weeklyWalks.sumOf { it.distanceCovered }
    val totalArea = 0.0 // Area calculation removed

    // Calculate daily stats for graph - Get last 7 days starting from Sunday
    val dailyData = mutableListOf<DailyStat>()
    val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    // Get the start of the current week (Sunday)
    calendar.timeInMillis = now
    val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday, etc.
    val daysFromSunday = (currentDayOfWeek - Calendar.SUNDAY + 7) % 7

    // Start from Sunday of current week
    calendar.add(Calendar.DAY_OF_MONTH, -daysFromSunday)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val weekStart = calendar.timeInMillis

    // Generate data for each day of the week (Sun to Sat)
    for (i in 0 until 7) {
        val dayStart = weekStart + (i * 24 * 60 * 60 * 1000L)
        val dayEnd = dayStart + (24 * 60 * 60 * 1000L)

        calendar.timeInMillis = dayStart
        val dayName = dayNames[calendar.get(Calendar.DAY_OF_WEEK) - 1]

        val dayWalks = weeklyWalks.filter {
            it.timestamp.time >= dayStart && it.timestamp.time < dayEnd
        }
        val dayDistance = dayWalks.sumOf { it.distanceCovered }

        dailyData.add(DailyStat(dayName, dayDistance, dayWalks.size))
    }

    return WeeklyStats(
        walks = weeklyWalks.size,
        distance = totalDistance,
        area = totalArea,
        dailyData = dailyData
    )
}

// Calculate monthly stats from walks
fun calculateMonthlyStats(walks: List<Walk>): MonthlyStats {
    val now = System.currentTimeMillis()
    val monthAgo = now - (30 * 24 * 60 * 60 * 1000L)

    val monthlyWalks = walks.filter { it.timestamp.time >= monthAgo }
    val totalDistance = monthlyWalks.sumOf { it.distanceCovered }
    val totalArea = 0.0 // Area calculation removed

    return MonthlyStats(
        walks = monthlyWalks.size,
        distance = totalDistance,
        area = totalArea
    )
}