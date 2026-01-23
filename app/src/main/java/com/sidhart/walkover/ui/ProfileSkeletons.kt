package com.sidhart.walkover.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Shimmer effect colors
@Composable
fun rememberShimmerColors(): List<Color> {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val shimmerColor = MaterialTheme.colorScheme.surface
    return remember(baseColor, shimmerColor) {
        listOf(
            baseColor.copy(alpha = 0.6f),
            shimmerColor.copy(alpha = 0.2f),
            baseColor.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier
) {
    val shimmerColors = rememberShimmerColors()

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation, translateAnimation),
        end = Offset(translateAnimation + 200f, translateAnimation + 200f)
    )

    Box(
        modifier = modifier.background(brush)
    )
}

// Skeleton for User Profile Card
@Composable
fun UserProfileCardSkeleton() {
    Box(modifier = Modifier.fillMaxWidth()) {
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
                    // Avatar skeleton
                    ShimmerEffect(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // User Details skeleton
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerEffect(
                            modifier = Modifier
                                .width(120.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ShimmerEffect(
                            modifier = Modifier
                                .width(80.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }

                    // Level Badge skeleton
                    ShimmerEffect(
                        modifier = Modifier
                            .width(60.dp)
                            .height(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(20.dp))

                // Stats Row skeleton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(3) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ShimmerEffect(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            ShimmerEffect(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress bar skeleton
                ShimmerEffect(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerEffect(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }

        // Loading indicator centered on card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
    }
}

// Skeleton for Achievements Section
@Composable
fun AchievementsSectionSkeleton() {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerEffect(
                modifier = Modifier
                    .width(130.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            ShimmerEffect(
                modifier = Modifier
                    .width(50.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
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
                // Two rows of 3 badges each
                repeat(2) { rowIndex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(3) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(80.dp)
                            ) {
                                ShimmerEffect(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ShimmerEffect(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                    if (rowIndex == 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// Skeleton for Recent Walks Section
@Composable
fun RecentWalksSectionSkeleton() {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerEffect(
                modifier = Modifier
                    .width(110.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            ShimmerEffect(
                modifier = Modifier
                    .width(70.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3 recent walk item skeletons
        repeat(3) {
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
                    ShimmerEffect(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerEffect(
                            modifier = Modifier
                                .width(140.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }

                    ShimmerEffect(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// Skeleton for Weekly Stats Cards
@Composable
fun WeeklyStatsCardsSkeleton() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(2) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    ShimmerEffect(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ShimmerEffect(
                        modifier = Modifier
                            .width(60.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ShimmerEffect(
                        modifier = Modifier
                            .width(80.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    ShimmerEffect(
                        modifier = Modifier
                            .width(60.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

// Skeleton for Weekly Graph
@Composable
fun WeeklyGraphSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            ShimmerEffect(
                modifier = Modifier
                    .width(120.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Graph bars skeleton
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                repeat(7) { index ->
                    val heights = listOf(0.3f, 0.7f, 0.5f, 0.9f, 0.4f, 0.6f, 0.8f)
                    ShimmerEffect(
                        modifier = Modifier
                            .width(32.dp)
                            .fillMaxHeight(heights[index])
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day labels skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(7) {
                    ShimmerEffect(
                        modifier = Modifier
                            .width(24.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

// Complete Overview Tab Skeleton
@Composable
fun OverviewTabSkeleton() {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column {
            AchievementsSectionSkeleton()
            Spacer(modifier = Modifier.height(24.dp))
            RecentWalksSectionSkeleton()
        }

        // Loading indicator overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 120.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
    }
}

// Complete Stats Tab Skeleton
@Composable
fun StatsTabSkeleton() {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column {
            WeeklyStatsCardsSkeleton()
            Spacer(modifier = Modifier.height(20.dp))
            WeeklyGraphSkeleton()
        }

        // Loading indicator overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 100.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
    }
}