package com.sidhart.walkover.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnboardingPage(
    val titleTop: String,
    val titleBottom: String,
    val description: String,
    val modeName: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage
    val isLastPage = currentPage == 2

    val pages = listOf(
        OnboardingPage(
            titleTop = "Walk Without a Trace,",
            titleBottom = "Ghost Mode",
            description = "Track your fitness privately with no rivals or map painters — just you and the open road.",
            modeName = "Ghost Mode",
            icon = Icons.Outlined.VisibilityOff,
            accentColor = Color(0xFF800000) // Maroon
        ),
        OnboardingPage(
            titleTop = "Walk Together,",
            titleBottom = "Even Apart",
            description = "Sync your steps with a friend in real-time. Experience the motivation of a shared journey, no matter the distance.",
            modeName = "Duel Mode",
            icon = Icons.Outlined.People,
            accentColor = Color(0xFF9C27B0) // Purple
        ),
        OnboardingPage(
            titleTop = "Claim Your Territory,",
            titleBottom = "Compete Mode",
            description = "Capture real-world territory, steal land from rivals, and dominate the global leaderboard. Show the world who owns this city.",
            modeName = "Compete Mode",
            icon = Icons.Outlined.Public,
            accentColor = Color(0xFFF57C00) // Orange
        )
    )

    val animatedAccent by animateColorAsState(
        targetValue = pages[currentPage].accentColor,
        animationSpec = tween(300),
        label = "accent"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Skip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isLastPage) {
                    TextButton(onClick = onOnboardingComplete) {
                        Text(
                            "Skip",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                userScrollEnabled = true
            ) { page ->
                OnboardingPageContent(
                    page = pages[page],
                    accentColor = if (page == currentPage) animatedAccent else pages[page].accentColor,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Dots + Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dots
                Row(
                    modifier = Modifier.padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentPage) animatedAccent
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .width(if (index == currentPage) 24.dp else 8.dp)
                        )
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentPage > 0) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(currentPage - 1)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = animatedAccent
                            ),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = SolidColor(animatedAccent)
                            )
                        ) {
                            Text("Back", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            if (isLastPage) onOnboardingComplete()
                            else scope.launch {
                                pagerState.animateScrollToPage(currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = animatedAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (isLastPage) "Get Started" else "Next",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Mode Name Pill (Moved to top)
        Surface(
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.15f),
        ) {
            Text(
                text = page.modeName.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = page.titleTop,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = page.titleBottom,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = accentColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 23.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        when (page.modeName) {
            "Ghost Mode" -> GhostModeMockupCard(accentColor = accentColor)
            "Duel Mode" -> DuelModeMockupCard(accentColor = accentColor)
            "Compete Mode" -> CompeteModeMockupCard(accentColor = accentColor)
        }
    }
}

@Composable
private fun DuelModeMockupCard(accentColor: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAnim"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square
            .clip(RoundedCornerShape(32.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF2A1C3C), Color(0xFF160D20))
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val center = center
            val radius = size.minDimension / 3.5f
            // Animated rings
            val radius1 = radius + (radius * 0.5f * pulseAnim)
            val alpha1 = 0.2f * (1f - pulseAnim)
            drawCircle(
                color = accentColor.copy(alpha = alpha1.coerceAtLeast(0f)),
                radius = radius1,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            
            val pulseAnim2 = (pulseAnim + 0.5f) % 1f
            val radius2 = radius + (radius * 0.5f * pulseAnim2)
            val alpha2 = 0.2f * (1f - pulseAnim2)
            drawCircle(
                color = accentColor.copy(alpha = alpha2.coerceAtLeast(0f)),
                radius = radius2,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            val p1 = center.copy(x = center.x - 80f, y = center.y - 80f)
            val p2 = center.copy(x = center.x + 80f, y = center.y + 80f)
            drawLine(
                color = accentColor,
                start = p1,
                end = p2,
                strokeWidth = 4.dp.toPx()
            )
        }

        // P1 Avatar
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.sidhart.walkover.R.drawable.duel_avatar_1),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier
                .offset(x = (-40).dp, y = (-40).dp)
                .size(72.dp)
                .clip(CircleShape)
                .border(3.dp, Color(0xFF2A1C3C), CircleShape)
        )

        // P2 Avatar
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.sidhart.walkover.R.drawable.duel_avatar_2),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier
                .offset(x = 40.dp, y = 40.dp)
                .size(72.dp)
                .clip(CircleShape)
                .border(3.dp, Color(0xFF2A1C3C), CircleShape)
        )

    }
}

@Composable
private fun GhostModeMockupCard(accentColor: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ghostPulse")
    
    val alphaAnim1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 0),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha1"
    )
    
    val alphaAnim2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha2"
    )
    
    val alphaAnim3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha3"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square
            .clip(RoundedCornerShape(32.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF2B1010), Color(0xFF100808))
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Large faint background footprint
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.DirectionsWalk,
            contentDescription = null,
            tint = accentColor.copy(alpha = 0.05f),
            modifier = Modifier.size(200.dp)
        )
        
        // Sequentially fading footprints drawing a path
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.DirectionsWalk,
            contentDescription = null,
            tint = accentColor.copy(alpha = alphaAnim1),
            modifier = Modifier
                .offset(x = (-40).dp, y = 40.dp)
                .size(48.dp)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.DirectionsWalk,
            contentDescription = null,
            tint = accentColor.copy(alpha = alphaAnim2),
            modifier = Modifier
                .offset(x = 10.dp, y = 0.dp)
                .size(48.dp)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.DirectionsWalk,
            contentDescription = null,
            tint = accentColor.copy(alpha = alphaAnim3),
            modifier = Modifier
                .offset(x = 60.dp, y = (-40).dp)
                .size(48.dp)
        )
    }
}

@Composable
private fun CompeteModeMockupCard(accentColor: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "gridAnim")
    val alphaAnim1 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha1"
    )
    val alphaAnim2 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha2"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square
            .clip(RoundedCornerShape(32.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF2A1B0B), Color(0xFF1A1208))
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val cols = 6
            val rows = 6
            val cellW = size.width / cols
            val cellH = size.height / rows

            for (i in 0 until cols) {
                for (j in 0 until rows) {
                    val rectAlpha = when {
                        (i == 2 && j == 2) -> alphaAnim1
                        (i == 3 && j == 2) -> alphaAnim1 * 0.8f
                        (i == 2 && j == 3) -> alphaAnim1 * 0.6f
                        (i == 4 && j == 4) -> alphaAnim2
                        (i == 4 && j == 5) -> alphaAnim2 * 0.7f
                        else -> 0.05f
                    }
                    val color = accentColor.copy(alpha = rectAlpha)
                    drawRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(i * cellW + 4.dp.toPx(), j * cellH + 4.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(cellW - 8.dp.toPx(), cellH - 8.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Fill
                    )
                }
            }
        }

        // Flag icon floating in middle
        Box(
            modifier = Modifier
                .offset(y = (-10).dp)
                .size(72.dp)
                .background(Color(0xFF2A1B0B), CircleShape)
                .border(2.dp, accentColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Flag, contentDescription = null, tint = accentColor, modifier = Modifier.size(36.dp))
        }
    }
}