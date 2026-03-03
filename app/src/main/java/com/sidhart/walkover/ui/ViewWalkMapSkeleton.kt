package com.sidhart.walkover.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun CompactStatsCardSkeleton(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Neon Green accent bar placeholder
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Compact Stats Row Skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Distance - Prominent
                Column(
                    modifier = Modifier.weight(1.3f)
                ) {
                    ShimmerEffect(
                        modifier = Modifier
                            .width(60.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ShimmerEffect(
                            modifier = Modifier
                                .width(80.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        ShimmerEffect(
                            modifier = Modifier
                                .width(20.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }
                
                // Other stats
                repeat(2) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = androidx.compose.ui.Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ShimmerEffect(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ShimmerEffect(
                            modifier = Modifier
                                .width(50.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Additional Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                ShimmerEffect(
                    modifier = Modifier
                        .width(100.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
                
                ShimmerEffect(
                    modifier = Modifier
                        .width(60.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
        }
    }
}
