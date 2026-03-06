package com.sidhart.walkover.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sidhart.walkover.data.DuelChallenge
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.sin
import kotlin.random.Random

// ────────────────────────────────────────────────────────────────────────────
// Confetti data
// ────────────────────────────────────────────────────────────────────────────

private data class DuelConfettiParticle(
    val x: Float,
    val size: Dp,
    val color: Color,
    val shape: Int,          // 0 = circle, 1 = rect, 2 = diamond
    val speedFactor: Float,
    val swayAmplitude: Float,
    val delay: Float
)

private val WIN_CONFETTI_COLORS = listOf(
    Color(0xFFFFD700),
    Color(0xFFFFA500),
    Color(0xFFFF3B30),
    Color(0xFF34C759),
    Color(0xFF007AFF),
    Color(0xFFAF52DE),
    Color(0xFFFF2D55),
    Color(0xFFFFCC00),
    Color(0xFFC0F11C),
)

private val LOSS_CONFETTI_COLORS = listOf(
    Color(0xFFAAAAAA),
    Color(0xFF888888),
    Color(0xFF666666),
    Color(0xFF555555),
)

private fun buildParticles(count: Int, didWin: Boolean): List<DuelConfettiParticle> =
    List(count) {
        DuelConfettiParticle(
            x = Random.nextFloat(),
            size = (4 + Random.nextInt(10)).dp,
            color = if (didWin) WIN_CONFETTI_COLORS.random() else LOSS_CONFETTI_COLORS.random(),
            shape = Random.nextInt(3),
            speedFactor = 0.4f + Random.nextFloat() * 1.3f,
            swayAmplitude = Random.nextFloat() * 55f,
            delay = Random.nextFloat() * 0.6f
        )
    }

// ────────────────────────────────────────────────────────────────────────────
// Diamond shape (local, no cross-file dependency)
// ────────────────────────────────────────────────────────────────────────────

private val LocalDiamondShape = object : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width / 2f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(size.width / 2f, size.height)
            lineTo(0f, size.height / 2f)
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Main composable
// ────────────────────────────────────────────────────────────────────────────

/**
 * Duel result celebration displayed as a map overlay.
 * The map remains visible behind a semi-transparent scrim.
 * Card and colours adapt to dark / light theme.
 */
@Composable
fun DuelVictoryCelebration(
    challenge: DuelChallenge,
    currentUserId: String,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val isChallenger = challenge.challengerId == currentUserId
    val myDistance   = if (isChallenger) challenge.challengerDistanceKm else challenge.opponentDistanceKm
    val theirDistance = if (isChallenger) challenge.opponentDistanceKm else challenge.challengerDistanceKm
    val opponentName = if (isChallenger) challenge.opponentUsername else challenge.challengerUsername

    val didWin = challenge.winnerId == currentUserId
    val isTie  = challenge.winnerId == null

    // ── Accent colour (theme-aware) ─────────────────────────────────────────
    val accentColor = when {
        isTie   -> if (isDark) Color(0xFF4DA6FF) else Color(0xFF007AFF)
        didWin  -> if (isDark) Color(0xFFFFD700) else Color(0xFFCC8800)
        else    -> if (isDark) Color(0xFFFF6B6B) else Color(0xFFCC2200)
    }

    // ── Scrim colour — lighter in light mode ────────────────────────────────
    val scrimColor = if (isDark)
        Color(0xFF000000).copy(alpha = 0.65f)
    else
        Color(0xFF000000).copy(alpha = 0.42f)

    // ── Card surface colour ─────────────────────────────────────────────────
    val cardBg = if (isDark)
        Color(0xFF111318).copy(alpha = 0.96f)
    else
        Color(0xFFFAFAFA).copy(alpha = 0.97f)

    // ── On-card text colours ─────────────────────────────────────────────────
    val primaryText   = if (isDark) Color(0xFFF0F0F0) else Color(0xFF0D0D0D)
    val secondaryText = if (isDark) Color(0xFFAAAAAA)  else Color(0xFF555555)

    // ── Confetti ─────────────────────────────────────────────────────────────
    val particles = remember { buildParticles(70, didWin || isTie) }

    // ── Card entrance animation ───────────────────────────────────────────────
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(120); visible = true }

    val cardScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.45f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label = "cardScale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350),
        label = "cardAlpha"
    )
    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "scrimAlpha"
    )

    // ── Particle + emoji animations ───────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val tick by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart),
        label = "tick"
    )
    val emojiScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.16f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "emojiPulse"
    )

    // ── XP badge spring-in ─────────────────────────────────────────────────
    var xpVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(550); xpVisible = true }
    val xpScale by animateFloatAsState(
        targetValue = if (xpVisible) 1f else 0f,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow),
        label = "xpScale"
    )

    // ── Root overlay — fills the map Box ──────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(scrimAlpha)
            .background(scrimColor)
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
        contentAlignment = Alignment.Center
    ) {
        // ── Confetti layer ────────────────────────────────────────────────────
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val sw = maxWidth.value
            val sh = maxHeight.value
            particles.forEach { p ->
                val t = ((tick + p.x * 0.4f + p.delay) % 1f) * p.speedFactor
                val y = t * sh
                val x = p.x * sw + sin(t * 6.28f * 2f) * p.swayAmplitude
                val a = (1f - t * 0.8f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .size(p.size)
                        .offset(x.dp, y.dp)
                        .alpha(a)
                        .then(
                            when (p.shape) {
                                0    -> Modifier.clip(CircleShape)
                                1    -> Modifier.clip(RoundedCornerShape(2.dp))
                                else -> Modifier.clip(LocalDiamondShape)
                            }
                        )
                        .background(p.color)
                )
            }
        }

        // ── Card ──────────────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .scale(cardScale)
                .alpha(cardAlpha),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // Top chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accentColor.copy(alpha = 0.14f))
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "DUEL COMPLETE · ${challenge.durationDays} DAYS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        letterSpacing = 1.4.sp
                    )
                }

                // Emoji
                Text(
                    text = when {
                        isTie  -> "🤝"
                        didWin -> "🏆"
                        else   -> "💪"
                    },
                    fontSize = 64.sp,
                    modifier = Modifier.scale(emojiScale)
                )

                // Headline
                Text(
                    text = when {
                        isTie  -> "It's a Tie!"
                        didWin -> "You Won!"
                        else   -> "Good Fight!"
                    },
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    textAlign = TextAlign.Center
                )

                // Sub-text
                Text(
                    text = when {
                        isTie  -> "Both walkers matched stride for stride."
                        didWin -> "You outwalked $opponentName. Impressive! 🔥"
                        else   -> "Keep training — you'll get them next time!"
                    },
                    fontSize = 13.sp,
                    color = secondaryText,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(2.dp))

                // Distance comparison panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.05f)
                            else Color.Black.copy(alpha = 0.04f)
                        )
                        .padding(vertical = 18.dp, horizontal = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // YOU
                        DistanceColumn(
                            label = "YOU",
                            km = myDistance,
                            highlight = didWin || isTie,
                            accentColor = accentColor,
                            primaryText = primaryText,
                            secondaryText = secondaryText
                        )

                        // VS divider
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isDark) Color.White.copy(0.07f) else Color.Black.copy(0.06f)
                                )
                                .padding(9.dp)
                        ) {
                            Text(
                                "VS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = secondaryText,
                                letterSpacing = 1.sp
                            )
                        }

                        // THEM
                        DistanceColumn(
                            label = opponentName.uppercase().take(9),
                            km = theirDistance,
                            highlight = !didWin && !isTie && challenge.winnerId != null,
                            accentColor = if (isDark) Color(0xFFFF6B6B) else Color(0xFFCC2200),
                            primaryText = primaryText,
                            secondaryText = secondaryText
                        )
                    }
                }

                // XP badge
                Box(
                    modifier = Modifier
                        .scale(xpScale)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFC0F11C).copy(alpha = if (isDark) 0.18f else 0.12f),
                                    Color(0xFF34C759).copy(alpha = if (isDark) 0.10f else 0.07f)
                                )
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 13.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚡", fontSize = 20.sp)
                        Column {
                            Text(
                                "+100 XP",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color(0xFFC0F11C) else Color(0xFF2E7D32)
                            )
                            Text(
                                "Duel completion reward",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFC0F11C).copy(0.6f) else Color(0xFF2E7D32).copy(0.7f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                // Dismiss button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = if (isDark) Color(0xFF0A0A0A) else Color.White
                    )
                ) {
                    Text(
                        text = when {
                            didWin -> "Claim Victory! 🏆"
                            isTie  -> "Respect the Tie 🤝"
                            else   -> "Keep Walking 💪"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    "Tap anywhere to dismiss",
                    fontSize = 11.sp,
                    color = secondaryText.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun DistanceColumn(
    label: String,
    km: Double,
    highlight: Boolean,
    accentColor: Color,
    primaryText: Color,
    secondaryText: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = secondaryText,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = String.format(Locale.US, "%.2f", km),
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = if (highlight) accentColor else primaryText
        )
        Text(
            text = "km",
            fontSize = 12.sp,
            color = secondaryText,
            fontWeight = FontWeight.Medium
        )
    }
}
