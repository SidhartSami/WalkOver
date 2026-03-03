package com.sidhart.walkover.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import kotlin.math.roundToInt
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.graphicsLayer
// ============================================================================
// DATA MODELS - USING REAL WALK DATA
// ============================================================================

data class DailyActivityMetrics(
    // Real data from walks
    val distanceKm: Double,
    val distanceGoalKm: Double = 5.0,
    val walkCount: Int,
    val walkCountGoal: Int = 3,
    val activeMinutes: Int,
    val activeMinutesGoal: Int = 30,
    val averagePaceKmPerMin: Double,
    
    // Comparison data
    val previousDayDistance: Double = 0.0,
    val previousDayWalkCount: Int = 0,
    val weeklyAverageDistance: Double = 0.0,
    val currentStreak: Int = 0,
    val bestPaceThisWeek: Double = Double.MAX_VALUE
)

data class ActivityInsight(
    val message: String,
    val type: InsightType,
    val icon: ImageVector
)

enum class InsightType {
    POSITIVE, NEUTRAL, ACHIEVEMENT
}

// ============================================================================
// CALCULATION SERVICE - REAL DATA ONLY
// ============================================================================

object ActivityStatsCalculator {
    
    fun generateInsights(metrics: DailyActivityMetrics): List<ActivityInsight> {
        val insights = mutableListOf<ActivityInsight>()
        
        // Compare distance to yesterday
        if (metrics.previousDayDistance > 0) {
            val percentChange = ((metrics.distanceKm - metrics.previousDayDistance) / metrics.previousDayDistance * 100).roundToInt()
            if (percentChange > 10) {
                insights.add(ActivityInsight(
                    "+$percentChange% more distance than yesterday",
                    InsightType.POSITIVE,
                    Icons.Default.TrendingUp
                ))
            }
        }
        
        // Compare walk count to yesterday
        if (metrics.previousDayWalkCount > 0 && metrics.walkCount > metrics.previousDayWalkCount) {
            insights.add(ActivityInsight(
                "${metrics.walkCount - metrics.previousDayWalkCount} more walks than yesterday",
                InsightType.POSITIVE,
                Icons.Default.TrendingUp
            ))
        }
        
        // Best pace check
        if (metrics.averagePaceKmPerMin > 0 && metrics.averagePaceKmPerMin >= metrics.bestPaceThisWeek) {
            insights.add(ActivityInsight(
                "Best pace this week",
                InsightType.ACHIEVEMENT,
                Icons.Outlined.Speed
            ))
        }
        
        // Streak
        if (metrics.currentStreak >= 3) {
            insights.add(ActivityInsight(
                "${metrics.currentStreak} day streak active 🔥",
                InsightType.ACHIEVEMENT,
                Icons.Outlined.LocalFireDepartment
            ))
        }
        
        // Goal progress
        val distancePercent = (metrics.distanceKm / metrics.distanceGoalKm * 100).roundToInt()
        if (distancePercent >= 90 && distancePercent < 100) {
            insights.add(ActivityInsight(
                "Almost at distance goal!",
                InsightType.POSITIVE,
                Icons.Outlined.EmojiEvents
            ))
        }
        
        return insights
    }
    
    fun calculatePercentage(current: Double, goal: Double): Float {
        return (current / goal * 100).coerceIn(0.0, 100.0).toFloat()
    }
    
    fun calculatePercentage(current: Int, goal: Int): Float {
        return (current.toDouble() / goal * 100).coerceIn(0.0, 100.0).toFloat()
    }
}

// ============================================================================
// MULTI-LAYER ACTIVITY RING COMPONENT - REAL DATA
// ============================================================================

@Composable
fun MultiMetricActivityRing(
    metrics: DailyActivityMetrics,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }
    
    // Calculate percentages from REAL data
    val distancePercent = remember(metrics.distanceKm, metrics.distanceGoalKm) {
        ActivityStatsCalculator.calculatePercentage(metrics.distanceKm, metrics.distanceGoalKm)
    }
    val walkCountPercent = remember(metrics.walkCount, metrics.walkCountGoal) {
        ActivityStatsCalculator.calculatePercentage(metrics.walkCount, metrics.walkCountGoal)
    }
    val activePercent = remember(metrics.activeMinutes, metrics.activeMinutesGoal) {
        ActivityStatsCalculator.calculatePercentage(metrics.activeMinutes, metrics.activeMinutesGoal)
    }
    
    // Animated progress values
    val animatedDistance by animateFloatAsState(
        targetValue = if (isVisible) distancePercent else 0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "distance"
    )
    val animatedWalkCount by animateFloatAsState(
        targetValue = if (isVisible) walkCountPercent else 0f,
        animationSpec = tween(1200, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "walkCount"
    )
    val animatedActive by animateFloatAsState(
        targetValue = if (isVisible) activePercent else 0f,
        animationSpec = tween(1200, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "active"
    )
    
    // Animated counter for distance
    val animatedDistanceValue by animateFloatAsState(
        targetValue = if (isVisible) metrics.distanceKm.toFloat() else 0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "distanceValue"
    )
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    
    Box(
        modifier = modifier
            .size(280.dp)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        // Rings Canvas
        Canvas(modifier = Modifier.size(280.dp)) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            
            val strokeWidth = 20.dp.toPx()
            val spacing = 8.dp.toPx()
            
            // Outer Ring - Distance (largest)
            val outerRadius = size.minDimension / 2 - strokeWidth / 2
            drawArc(
                color = primaryColor.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(centerX - outerRadius, centerY - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                brush = if (distancePercent >= 100f) {
                    Brush.sweepGradient(
                        colors = listOf(primaryColor, primaryColor.copy(alpha = 0.7f), primaryColor)
                    )
                } else {
                    Brush.linearGradient(colors = listOf(primaryColor, primaryColor))
                },
                startAngle = -90f,
                sweepAngle = (animatedDistance / 100f * 360f).coerceAtMost(360f),
                useCenter = false,
                topLeft = Offset(centerX - outerRadius, centerY - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
            
            // Middle Ring - Walk Count
            val middleRadius = outerRadius - strokeWidth - spacing
            drawArc(
                color = secondaryColor.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(centerX - middleRadius, centerY - middleRadius),
                size = Size(middleRadius * 2, middleRadius * 2),
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.linearGradient(colors = listOf(secondaryColor, secondaryColor)),
                startAngle = -90f,
                sweepAngle = (animatedWalkCount / 100f * 360f).coerceAtMost(360f),
                useCenter = false,
                topLeft = Offset(centerX - middleRadius, centerY - middleRadius),
                size = Size(middleRadius * 2, middleRadius * 2),
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
            
            // Inner Ring - Active Minutes
            val innerRadius = middleRadius - strokeWidth - spacing
            drawArc(
                color = tertiaryColor.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(centerX - innerRadius, centerY - innerRadius),
                size = Size(innerRadius * 2, innerRadius * 2),
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.linearGradient(colors = listOf(tertiaryColor, tertiaryColor)),
                startAngle = -90f,
                sweepAngle = (animatedActive / 100f * 360f).coerceAtMost(360f),
                useCenter = false,
                topLeft = Offset(centerX - innerRadius, centerY - innerRadius),
                size = Size(innerRadius * 2, innerRadius * 2),
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        // Center content - Show Distance
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = DecimalFormat("0.0").format(animatedDistanceValue),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "km Today",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================================================
// STATS SUMMARY TILES GRID - REAL DATA
// ============================================================================

@Composable
fun StatsSummaryGrid(
    metrics: DailyActivityMetrics,
    modifier: Modifier = Modifier
) {
    val decimalFormat = remember { DecimalFormat("#,##0.0") }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Distance Tile
            StatTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Route,
                label = "Distance",
                value = "${decimalFormat.format(metrics.distanceKm)} km",
                progress = ActivityStatsCalculator.calculatePercentage(metrics.distanceKm, metrics.distanceGoalKm) / 100f,
                trendIndicator = calculateDistanceTrend(metrics.distanceKm, metrics.previousDayDistance),
                color = MaterialTheme.colorScheme.primary
            )
            
            // Walk Count Tile
            StatTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.DirectionsWalk,
                label = "Walks",
                value = "${metrics.walkCount}",
                progress = ActivityStatsCalculator.calculatePercentage(metrics.walkCount, metrics.walkCountGoal) / 100f,
                trendIndicator = if (metrics.walkCount > metrics.previousDayWalkCount && metrics.previousDayWalkCount > 0) {
                    "+${metrics.walkCount - metrics.previousDayWalkCount}"
                } else null,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Active Time Tile
            StatTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Timer,
                label = "Active Time",
                value = "${metrics.activeMinutes} min",
                progress = null,
                trendIndicator = if (metrics.currentStreak >= 3) "🔥 ${metrics.currentStreak} days" else null,
                color = MaterialTheme.colorScheme.tertiary
            )
            
            // Average Pace Tile
            StatTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Speed,
                label = "Avg Pace",
                value = if (metrics.averagePaceKmPerMin > 0) {
                    "${decimalFormat.format(metrics.averagePaceKmPerMin)} km/min"
                } else {
                    "-- km/min"
                },
                progress = null,
                trendIndicator = null,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun StatTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    progress: Float?,
    trendIndicator: String?,
    color: Color
) {
    var isVisible by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        isVisible = true
    }
    
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                
                if (trendIndicator != null) {
                    Text(
                        text = trendIndicator,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = color
                    )
                }
            }
            
            Column {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                
                if (progress != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = color,
                        trackColor = color.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${(progress * 100).roundToInt()}% of goal",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun calculateDistanceTrend(current: Double, previous: Double): String? {
    if (previous == 0.0) return null
    val percentChange = ((current - previous) / previous * 100).roundToInt()
    return when {
        percentChange > 5 -> "↑ +$percentChange%"
        percentChange < -5 -> "↓ $percentChange%"
        else -> null
    }
}

// ============================================================================
// INSIGHT CHIPS ROW
// ============================================================================

@Composable
fun InsightChipsRow(
    insights: List<ActivityInsight>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        insights.forEach { insight ->
            InsightChip(insight = insight)
        }
    }
}

@Composable
private fun InsightChip(insight: ActivityInsight) {
    val backgroundColor = when (insight.type) {
        InsightType.POSITIVE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        InsightType.ACHIEVEMENT -> Color(0xFFFFD700).copy(alpha = 0.2f)
        InsightType.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = when (insight.type) {
        InsightType.POSITIVE -> MaterialTheme.colorScheme.primary
        InsightType.ACHIEVEMENT -> Color(0xFFFFB300)
        InsightType.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = insight.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = insight.message,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

// ============================================================================
// COMPLETE ACTIVITY OVERVIEW SECTION
// ============================================================================

@Composable
fun ActivityOverviewSection(
    metrics: DailyActivityMetrics,
    modifier: Modifier = Modifier,
    onRingTap: () -> Unit = {}
) {
    val insights = remember(metrics) {
        ActivityStatsCalculator.generateInsights(metrics)
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Activity Ring
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            MultiMetricActivityRing(
                metrics = metrics,
                onTap = onRingTap
            )
        }
        
        // Stats Grid
        StatsSummaryGrid(metrics = metrics)
        
        // Insights
        if (insights.isNotEmpty()) {
            InsightChipsRow(insights = insights)
        }
    }
}

