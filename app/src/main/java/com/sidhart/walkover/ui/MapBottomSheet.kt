package com.sidhart.walkover.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sidhart.walkover.data.LiveWalkState
import com.sidhart.walkover.ui.theme.NeonGreen
import com.sidhart.walkover.ui.theme.CoralOrange
import com.sidhart.walkover.ui.theme.DeepMidnight
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreenBottomSheet(
    walkState: LiveWalkState,
    selectedMode: String,
    hasActiveDuel: Boolean = false,
    onModeSelect: (String) -> Unit,
    onStartWalk: () -> Unit,
    onPauseResume: () -> Unit,
    onStop: () -> Unit,
    scaffoldState: BottomSheetScaffoldState? = null
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val sheetBgColor = if (isDark) DeepMidnight else Color.White
    val scope = rememberCoroutineScope()

    Surface(
        color = sheetBgColor,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        modifier = Modifier
            .fillMaxWidth()
            // Height adapts to the content dynamically
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Drag handle row (tap to collapse / expand) ─────
            val handleColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.18f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (scaffoldState != null) {
                            scope.launch {
                                val isExpanded = scaffoldState.bottomSheetState.currentValue ==
                                    SheetValue.Expanded
                                if (isExpanded) scaffoldState.bottomSheetState.partialExpand()
                                else scaffoldState.bottomSheetState.expand()
                            }
                        }
                    }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(handleColor)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!walkState.isTracking) {
                PreWalkSelection(
                    selectedMode = selectedMode,
                    hasActiveDuel = hasActiveDuel,
                    onModeSelect = onModeSelect,
                    onStartWalk  = onStartWalk,
                    walkState    = walkState
                )
            } else {
                ActiveWalkStats(
                    walkState    = walkState,
                    onPauseResume = onPauseResume,
                    onStop       = onStop
                )
            }
        }
    }
}

@Composable
private fun PreWalkSelection(
    selectedMode: String,
    hasActiveDuel: Boolean,
    onModeSelect: (String) -> Unit,
    onStartWalk: () -> Unit,
    walkState: LiveWalkState
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val titleColor = if (isDark) Color.White else Color.Black

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = Modifier.fillMaxWidth(), // Removed align(Alignment.Start) as it's not needed if fillMaxWidth
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Select Walk Mode",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = titleColor
            )
            Text(
                text = "Choose how you want to walk today",
                fontSize = 14.sp,
                color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.DarkGray
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ModeCard(
                title = "Ghost",
                subtitle = "Race yourself based on previous bests",
                icon = Icons.Default.DirectionsRun,
                isSelected = selectedMode == "Ghost",
                onClick = { onModeSelect("Ghost") },
                accentColor = Color(0xFF800000) // Dark Maroon
            )
            ModeCard(
                title = "Duel",
                subtitle = "Walk together with a friend remotely",
                icon = Icons.Default.Handshake,
                isSelected = selectedMode == "Duel",
                onClick = { onModeSelect("Duel") },
                accentColor = Color(0xFF9C27B0) // Purple
            )
            ModeCard(
                title = "Compete",
                subtitle = "Race against others in real-time",
                icon = Icons.Default.Bolt,
                isSelected = selectedMode == "Competitive",
                onClick = { onModeSelect("Competitive") },
                accentColor = CoralOrange
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        val buttonText = when {
            selectedMode == "Duel" && !hasActiveDuel -> "CHALLENGE FRIEND"
            else -> "START WALK"
        }
        val buttonColor = if (selectedMode == "Duel" && !hasActiveDuel) Color(0xFFFF9800) else NeonGreen

        // Start Button
        Button(
            onClick = onStartWalk,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DirectionsWalk,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = buttonText,
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun ActiveWalkStats(
    walkState: LiveWalkState,
    onPauseResume: () -> Unit,
    onStop: () -> Unit
) {
    if (walkState.mode == "Duel") {
        DuelWalkStats(walkState, onPauseResume, onStop)
    } else if (walkState.mode == "Competitive" || walkState.mode == "Compete") {
        CompeteWalkStats(walkState, onPauseResume, onStop)
    } else {
        StandardWalkStats(walkState, onPauseResume, onStop)
    }
}

@Composable
private fun StandardWalkStats(
    walkState: LiveWalkState,
    onPauseResume: () -> Unit,
    onStop: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color.Black

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Status Pill
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (walkState.mode == "Ghost") Color(0xFF800000).copy(alpha = 0.15f) else if (isDark) Color(0xFF1B4332) else NeonGreen.copy(alpha = 0.15f), // Pill bg
            modifier = Modifier.wrapContentSize()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (walkState.mode == "Ghost") Color(0xFF800000) else if (walkState.mode == "Duel") Color(0xFF9C27B0) else if (isDark) NeonGreen else Color(0xFF2E7D32))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        walkState.isPaused -> "PAUSED"
                        walkState.mode == "Ghost" -> "GHOST MODE"
                        walkState.mode == "Duel" -> "DUEL MODE"
                        else -> "WALKING"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (walkState.mode == "Ghost") {
                        if (isDark) Color(0xFFE57373) else Color(0xFF800000) // Maroon
                    } else if (walkState.mode == "Duel") {
                        if (isDark) Color(0xFFE1BEE7) else Color(0xFF9C27B0)
                    } else {
                        if (isDark) NeonGreen else Color(0xFF2E7D32)
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Duration
        Text(
            text = walkState.stats.formatElapsedTime(),
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = textColor
        )
        Text(
            text = "DURATION",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallStatCard(
                modifier = Modifier.weight(1f),
                label = String.format("%.2f", walkState.stats.distanceKm),
                unit = "km",
                icon = Icons.Default.LocationOn,
                accentColor = if (walkState.mode == "Ghost") Color(0xFF800000) else Color(0xFF4CAF50)
            )
            SmallStatCard(
                modifier = Modifier.weight(1f),
                label = String.format("%.1f", walkState.stats.averageSpeedKmh),
                unit = "km/h",
                icon = Icons.Default.Speed,
                accentColor = if (walkState.mode == "Ghost") Color(0xFF800000) else Color(0xFF2196F3)
            )
            SmallStatCard(
                modifier = Modifier.weight(1f),
                label = String.format("%.0f", walkState.stats.caloriesBurned),
                unit = "kcal",
                icon = Icons.Default.LocalFireDepartment,
                accentColor = if (walkState.mode == "Ghost") Color(0xFF800000) else Color(0xFFFF9800)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pause/Resume Button
            Button(
                onClick = onPauseResume,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .height(60.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFFFFD166) else Color(0xFFFFD166).copy(alpha = 0.9f)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (walkState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        tint = Color.Black
                    )
                    Text(
                        text = if (walkState.isPaused) "RESUME" else "PAUSE",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Stop Button
            Button(
                onClick = onStop,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .height(60.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (walkState.mode == "Ghost") Color(0xFF800000) else Color(0xFFEF476F))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                    Text(
                        text = "STOP",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DuelWalkStats(
    walkState: LiveWalkState,
    onPauseResume: () -> Unit,
    onStop: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color.Black

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Status Pill
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF9C27B0).copy(alpha = 0.15f), // Purple pill
            modifier = Modifier.wrapContentSize()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF9C27B0))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DUEL MODE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9C27B0)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Duration
        Text(
            text = walkState.stats.formatElapsedTime(),
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            color = textColor
        )
        Text(
            text = "DURATION",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Versus Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // YOU side
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = String.format("%.2f", walkState.stats.distanceKm),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "KM",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = String.format("%.1f", walkState.stats.averageSpeedKmh) + " KM/H",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "YOU",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonGreen
                )
            }

            // VS Divider
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Divider(
                    modifier = Modifier.height(40.dp).width(1.dp),
                    color = textColor.copy(alpha = 0.2f)
                )
                Surface(
                    shape = CircleShape,
                    color = textColor.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "VS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
                Divider(
                    modifier = Modifier.height(40.dp).width(1.dp),
                    color = textColor.copy(alpha = 0.2f)
                )
            }

            // OPPONENT side
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "0.00",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "KM",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "0.0 KM/H",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "OPPONENT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF9C27B0) // Purple
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Leading by ${String.format("%.2f", walkState.stats.distanceKm)} km",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = NeonGreen
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPauseResume,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFE0E0E0))
            ) {
                Text(
                    text = if (walkState.isPaused) "RESUME" else "PAUSE",
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Button(
                onClick = onStop,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
            ) {
                Text(
                    text = "END MATCH",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CompeteWalkStats(
    walkState: LiveWalkState,
    onPauseResume: () -> Unit,
    onStop: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color.Black

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Status Pill
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CoralOrange.copy(alpha = 0.15f), // Orange pill
            modifier = Modifier.wrapContentSize()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(CoralOrange)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "COMPETE MODE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoralOrange
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Duration
        Text(
            text = walkState.stats.formatElapsedTime(),
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            color = textColor
        )
        Text(
            text = "DURATION",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Large Area Stat
        Text(
            text = "0", // Usually would be area / territories captured
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CoralOrange
        )
        Text(
            text = "TERRITORIES CAPTURED",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CoralOrange.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.2f", walkState.stats.distanceKm),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "DISTANCE (KM)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.5f)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.1f", walkState.stats.averageSpeedKmh),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "SPEED (KM/H)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.5f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPauseResume,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFE0E0E0))
            ) {
                Text(
                    text = if (walkState.isPaused) "RESUME" else "PAUSE",
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Button(
                onClick = onStop,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralOrange)
            ) {
                Text(
                    text = "END MATCH",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val background = when (title) {
        "Ghost" -> Brush.verticalGradient(
            if (isDark) listOf(Color(0xFF4A1E2B), Color(0xFF1F0C15)) // Red/Pink
            else listOf(Color(0xFFF3D5DF), Color(0xFFFBE8EF))
        )
        "Compete" -> Brush.verticalGradient(
            if (isDark) listOf(Color(0xFF4A2A10), Color(0xFF1F1005)) // Orange
            else listOf(Color(0xFFFFE4B5), Color(0xFFFFF8EC))
        )
        else -> Brush.verticalGradient(
            if (isDark) listOf(Color(0xFF2D1E4A), Color(0xFF120C1F)) // Purple
            else listOf(Color(0xFFD4C4EB), Color(0xFFEBE3F6))
        )
    }

    val contentColor = if (isDark) Color.White else Color.Black
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color.DarkGray

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, accentColor) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon Background
                Surface(
                    shape = CircleShape,
                    color = accentColor,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, color = contentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = subtitle, color = subtitleColor, fontSize = 12.sp, lineHeight = 14.sp)
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = subtitleColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomSheetStatItem(label: String, value: String, unit: String? = null) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val valueColor = if (isDark) Color.White else Color.Black
    val unitColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) NeonGreen else Color(0xFF2E7D32)
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
            )
            unit?.let {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = unitColor,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SmallStatCard(
    modifier: Modifier = Modifier,
    label: String,
    unit: String,
    icon: ImageVector,
    accentColor: Color
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val valueColor = if (isDark) Color.White else Color.Black
    val unitColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
    val bgColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)

    Surface(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = label, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = unit, fontSize = 10.sp, color = unitColor, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}
