package com.sidhart.walkover.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sidhart.walkover.data.User
import com.sidhart.walkover.data.Walk
import com.sidhart.walkover.viewmodel.ProfileViewModel
import com.sidhart.walkover.viewmodel.ProfileUiState
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel,
    onNavigateToWalkHistory: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val decimalFormat = remember { DecimalFormat("#,##0.##") }

    val isRefreshing = uiState is ProfileUiState.Loading
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.Error) {
            Toast.makeText(
                context,
                (uiState as ProfileUiState.Error).message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = { viewModel.refresh() },
        indicator = { state, trigger ->
            SwipeRefreshIndicator(
                state = state,
                refreshTriggerDistance = trigger,
                scale = true,
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            )
        },
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    ProfileLoadingSkeleton()
                }

                is ProfileUiState.Success -> {
                    ProfileContent(
                        user = state.user,
                        recentWalks = state.recentWalks,
                        allWalks = state.allWalks,
                        weeklyStats = state.weeklyStats,
                        monthlyStats = state.monthlyStats,
                        decimalFormat = decimalFormat,
                        onNavigateToWalkHistory = onNavigateToWalkHistory
                    )
                }

                is ProfileUiState.Error -> {
                    ProfileErrorState(
                        message = state.message,
                        onRetry = if (state.canRetry) {
                            { viewModel.refresh() }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    user: User,
    recentWalks: List<Walk>,
    allWalks: List<Walk>,
    weeklyStats: WeeklyStats,
    monthlyStats: MonthlyStats,
    decimalFormat: DecimalFormat,
    onNavigateToWalkHistory: () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
            initialOffsetY = { -20 },
            animationSpec = tween(600)
        )
    ) {
        Column {
            UserProfileCard(userData = user, decimalFormat = decimalFormat)
            Spacer(modifier = Modifier.height(20.dp))
            WeeklyProgressCardWithVicoGraph(
                allWalks = allWalks,
                decimalFormat = decimalFormat
            )
            Spacer(modifier = Modifier.height(20.dp))
            AchievementBadgesSection(userData = user)
            Spacer(modifier = Modifier.height(20.dp))
            RecentWalksSection(
                walks = recentWalks,
                onViewAll = onNavigateToWalkHistory,
                decimalFormat = decimalFormat
            )
        }
    }
}

@Composable
private fun ProfileLoadingSkeleton() {
    Column {
        LoadingCard(height = 104.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(3) {
                LoadingCard(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LoadingCard(height = 340.dp)
        Spacer(modifier = Modifier.height(20.dp))
        LoadingCard(height = 200.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .shimmerEffect()
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            repeat(3) {
                LoadingCard(height = 80.dp)
                if (it < 2) Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ProfileErrorState(
    message: String,
    onRetry: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Failed to Load Profile",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            if (onRetry != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun LoadingCard(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(height?.let { Modifier.height(it) } ?: Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shimmerEffect()
        )
    }
}

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alphaValue by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    return this.graphicsLayer {
        alpha = alphaValue
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
fun WeeklyProgressCardWithVicoGraph(
    allWalks: List<Walk>,
    decimalFormat: DecimalFormat
) {
    var weeksOffset by remember { mutableStateOf(0) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        isVisible = true
    }

    val currentStats = remember(weeksOffset, allWalks) {
        calculateWeeklyStatsForOffset(allWalks, weeksOffset)
    }

    val weekRange = remember(weeksOffset) {
        getWeekDateRange(weeksOffset)
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(600)) +
                slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GreyStatCard(
                    label = "Walks",
                    value = currentStats.walks.toString(),
                    icon = Icons.Outlined.DirectionsWalk,
                    iconColor = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f),
                    animationDelay = 0
                )
                GreyStatCard(
                    label = "Distance",
                    value = "${decimalFormat.format(currentStats.distance / 1000)} km",
                    icon = Icons.Outlined.Route,
                    iconColor = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f),
                    animationDelay = 100
                )
                GreyStatCard(
                    label = "Avg/Day",
                    value = "${decimalFormat.format(currentStats.distance / 7000)} km",
                    icon = Icons.Outlined.TrendingUp,
                    iconColor = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f),
                    animationDelay = 200
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { weeksOffset++ }
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "This Week",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = weekRange,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { if (weeksOffset > 0) weeksOffset-- }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (currentStats.dailyData.isNotEmpty()) {
                        VicoAreaChart(
                            dailyData = currentStats.dailyData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No data for this week",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VicoAreaChart(
    dailyData: List<DailyStat>,
    modifier: Modifier = Modifier
) {
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val yAxisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val barColor = MaterialTheme.colorScheme.primary
    val emptyBarColor = MaterialTheme.colorScheme.surfaceVariant

    val dailyDataKm = remember(dailyData) {
        dailyData.map { it.copy(distance = it.distance / 1000.0) }
    }

    val maxDistanceKm = remember(dailyDataKm) {
        dailyDataKm.maxOfOrNull { it.distance } ?: 1.0
    }

    val yAxisSteps = 5
    val stepValue = remember(maxDistanceKm) {
        if (maxDistanceKm > 0) {
            (maxDistanceKm / yAxisSteps).let {
                when {
                    it < 0.1 -> 0.1
                    it < 0.5 -> 0.5
                    it < 1.0 -> 1.0
                    it < 2.0 -> 2.0
                    it < 5.0 -> 5.0
                    else -> (it / 5.0).toInt() * 5.0
                }
            }
        } else 1.0
    }
    val maxYValue = stepValue * yAxisSteps

    val animatedHeights = remember(dailyDataKm) {
        dailyDataKm.map { Animatable(0f) }
    }

    LaunchedEffect(dailyDataKm) {
        animatedHeights.forEachIndexed { index, animatable ->
            kotlinx.coroutines.delay(index * 80L)
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 50.dp, end = 8.dp, bottom = 32.dp, top = 8.dp)
            ) {
                val strokeWidth = 1.dp.toPx()
                repeat(yAxisSteps + 1) { i ->
                    val yPos = size.height * (i.toFloat() / yAxisSteps)
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, yPos),
                        end = Offset(size.width, yPos),
                        strokeWidth = strokeWidth
                    )
                }
            }

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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 4.dp)
        ) {
            Text(
                text = "Distance (km)",
                fontSize = 9.sp,
                color = yAxisLabelColor,
                fontWeight = FontWeight.Medium
            )
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
    val isDarkTheme = isSystemInDarkTheme()
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
            .scale(scale)
            .graphicsLayer {
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
fun AchievementBadgesSection(userData: User) {
    val allBadges = remember(userData) {
        getAllAchievementBadges(userData).sortedByDescending { it.isUnlocked }
    }
    var isExpanded by remember { mutableStateOf(false) }

    val displayedBadges = if (isExpanded) allBadges else allBadges.take(3)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
            Column(modifier = Modifier.padding(20.dp)) {
                displayedBadges.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { badge ->
                            AchievementBadge(badge = badge)
                        }
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.width(90.dp))
                        }
                    }
                    if (row != displayedBadges.chunked(3).last()) {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

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
                    Text("Got it!", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
fun RecentWalksSection(
    walks: List<Walk>,
    onViewAll: () -> Unit,
    decimalFormat: DecimalFormat
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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

        if (walks.isEmpty()) {
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

// Data classes
data class WeeklyStats(
    val walks: Int,
    val distance: Double,
    val area: Double,
    val dailyData: List<DailyStat> = emptyList()
)

data class DailyStat(
    val day: String,
    val distance: Double,
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
    return listOf(
        BadgeWithStatus(
            name = "First Steps",
            description = "1 walk",
            icon = Icons.Outlined.DirectionsWalk,
            color = Color(0xFF4CAF50),
            isUnlocked = user.totalWalks >= 1,
            unlockHint = "Complete your first walk"
        ),
        BadgeWithStatus(
            name = "Explorer",
            description = "10 walks",
            icon = Icons.Outlined.Explore,
            color = Color(0xFF2196F3),
            isUnlocked = user.totalWalks >= 10,
            unlockHint = "Complete 10 walks"
        ),
        BadgeWithStatus(
            name = "Adventurer",
            description = "50 walks",
            icon = Icons.Outlined.Hiking,
            color = Color(0xFFFF9800),
            isUnlocked = user.totalWalks >= 50,
            unlockHint = "Complete 50 walks"
        ),
        BadgeWithStatus(
            name = "Champion",
            description = "100 walks",
            icon = Icons.Outlined.EmojiEvents,
            color = Color(0xFFFFD700),
            isUnlocked = user.totalWalks >= 100,
            unlockHint = "Complete 100 walks"
        ),
        BadgeWithStatus(
            name = "Kilometer",
            description = "1 km",
            icon = Icons.Outlined.Route,
            color = Color(0xFF9C27B0),
            isUnlocked = user.totalDistanceWalked >= 1000,
            unlockHint = "Walk a total distance of 1 kilometer (1,000 meters)"
        ),
        BadgeWithStatus(
            name = "Marathon",
            description = "10 km",
            icon = Icons.Outlined.DirectionsRun,
            color = Color(0xFFE91E63),
            isUnlocked = user.totalDistanceWalked >= 10000,
            unlockHint = "Walk a total distance of 10 kilometers (10,000 meters)"
        )
    )
}

fun calculateWeeklyStatsForOffset(walks: List<Walk>, weeksOffset: Int): WeeklyStats {
    val calendar = Calendar.getInstance()
    val now = System.currentTimeMillis()

    calendar.timeInMillis = now
    val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysFromSunday = (currentDayOfWeek - Calendar.SUNDAY + 7) % 7

    calendar.add(Calendar.DAY_OF_MONTH, -daysFromSunday)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    calendar.add(Calendar.WEEK_OF_YEAR, -weeksOffset)

    val weekStart = calendar.timeInMillis
    val weekEnd = weekStart + (7 * 24 * 60 * 60 * 1000L)

    val weeklyWalks = walks.filter { it.timestamp.time >= weekStart && it.timestamp.time < weekEnd }
    val totalDistance = weeklyWalks.sumOf { it.distanceCovered }

    val dailyData = mutableListOf<DailyStat>()
    val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    calendar.timeInMillis = weekStart

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
        area = 0.0,
        dailyData = dailyData
    )
}

fun getWeekDateRange(weeksOffset: Int): String {
    val calendar = Calendar.getInstance()
    val now = System.currentTimeMillis()

    calendar.timeInMillis = now
    val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysFromSunday = (currentDayOfWeek - Calendar.SUNDAY + 7) % 7

    calendar.add(Calendar.DAY_OF_MONTH, -daysFromSunday)
    calendar.add(Calendar.WEEK_OF_YEAR, -weeksOffset)

    val startDate = calendar.time
    calendar.add(Calendar.DAY_OF_MONTH, 6)
    val endDate = calendar.time

    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    return "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
}

fun calculateMonthlyStats(walks: List<Walk>): MonthlyStats {
    val now = System.currentTimeMillis()
    val monthAgo = now - (30 * 24 * 60 * 60 * 1000L)

    val monthlyWalks = walks.filter { it.timestamp.time >= monthAgo }
    val totalDistance = monthlyWalks.sumOf { it.distanceCovered }

    return MonthlyStats(
        walks = monthlyWalks.size,
        distance = totalDistance,
        area = 0.0
    )
}

fun calculateWeeklyStats(walks: List<Walk>): WeeklyStats {
    val calendar = Calendar.getInstance()
    val now = System.currentTimeMillis()
    val weekAgo = now - (7 * 24 * 60 * 60 * 1000L)

    val weeklyWalks = walks.filter { it.timestamp.time >= weekAgo }
    val totalDistance = weeklyWalks.sumOf { it.distanceCovered }

    val dailyData = mutableListOf<DailyStat>()
    val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    calendar.timeInMillis = now
    val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysFromSunday = (currentDayOfWeek - Calendar.SUNDAY + 7) % 7

    calendar.add(Calendar.DAY_OF_MONTH, -daysFromSunday)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val weekStart = calendar.timeInMillis

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
        area = 0.0,
        dailyData = dailyData
    )
}