package com.sidhart.walkover.ui

import com.sidhart.walkover.data.CelebrationEvent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
// Confetti particle data
// ─────────────────────────────────────────────────────────────────────────────

private data class ConfettiParticle(
    val x: Float,
    val size: Dp,
    val color: Color,
    val shape: Int,       // 0 = circle, 1 = rect, 2 = diamond
    val speedFactor: Float,
    val rotationSpeed: Float,
    val swayAmplitude: Float
)

private val CONFETTI_COLORS = listOf(
    Color(0xFFFF3B30), // Red
    Color(0xFFFF9500), // Orange
    Color(0xFFFFCC00), // Yellow
    Color(0xFF34C759), // Green
    Color(0xFF007AFF), // Blue
    Color(0xFFAF52DE), // Purple
    Color(0xFFFF2D55), // Pink
    Color(0xFF5AC8FA), // Light blue
)

private fun buildParticles(count: Int = 70): List<ConfettiParticle> =
    List(count) {
        ConfettiParticle(
            x              = Random.nextFloat(),
            size           = (5 + Random.nextInt(11)).dp,
            color          = CONFETTI_COLORS.random(),
            shape          = Random.nextInt(3),
            speedFactor    = 0.6f + Random.nextFloat() * 1.1f,
            rotationSpeed  = (0.5f + Random.nextFloat() * 2f) * if (Random.nextBoolean()) 1f else -1f,
            swayAmplitude  = Random.nextFloat() * 55f
        )
    }

// ─────────────────────────────────────────────────────────────────────────────
// Celebration overlay composable
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen celebration shown after a territory is captured or a walk is saved.
 * autoDismissMs = 0  → stays until tapped
 * autoDismissMs > 0  → auto-dismisses after N ms
 * Double-tap          → restarts confetti for testing
 */
@Composable
fun TerritoryCaptureCelebration(
    event: CelebrationEvent,
    onDismiss: () -> Unit,
    autoDismissMs: Long = 5000L
) {
    // Auto-dismiss
    LaunchedEffect(event) {
        if (autoDismissMs > 0L) {
            delay(autoDismissMs)
            onDismiss()
        }
    }

    // resetKey: double-tap increments to restart particles
    var resetKey by remember { mutableIntStateOf(0) }
    val particles = remember(resetKey) { buildParticles(70) }

    // Card entrance pop
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val cardScale by animateFloatAsState(
        targetValue  = if (visible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "cardScale"
    )

    // Infinite particle ticker
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val tick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tick"
    )

    // Accent colour per event type
    val isCardDark = androidx.compose.foundation.isSystemInDarkTheme()
    val cardBg = if (isCardDark) Color(0xFF0A0A0F) else Color.White // DeepMidnight
    val headlineColor = if (isCardDark) Color.White else Color(0xFF111111)
    val subLineColor = if (isCardDark) Color.White.copy(alpha = 0.7f) else Color(0xFF555555)
    val accentColor = when {
        event.isRoam             -> Color(0xFFC0F11C) // Brand NeonGreen
        event.stolenFrom != null -> Color(0xFFFF3B30) // ErrorRed
        else                     -> Color(0xFF00B4FF) // ElectricBlue
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Semi-transparent dark scrim so map is still vaguely visible
            .background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap       = { onDismiss() },   // single tap → dismiss
                    onDoubleTap = { resetKey++ }      // double tap → restart confetti
                )
            },
        contentAlignment = Alignment.Center
    ) {

        // ── Confetti ──────────────────────────────────────────────────────────
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenW = maxWidth.value
            val screenH = maxHeight.value

            particles.forEach { p ->
                val t          = ((tick + p.x * 0.5f) % 1f) * p.speedFactor
                val vertical   = t * screenH
                val horizontal = p.x * screenW + sin(t * 6.28f * 2) * p.swayAmplitude
                val alpha      = (1f - t * 0.8f).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .size(p.size)
                        .offset(x = horizontal.dp, y = vertical.dp)
                        .alpha(alpha)
                        .then(
                            when (p.shape) {
                                0    -> Modifier.clip(CircleShape)
                                1    -> Modifier.clip(RoundedCornerShape(2.dp))
                                else -> Modifier.clip(DiamondShape)
                            }
                        )
                        .background(p.color)
                )
            }
        }

        // ── Card ─────────────────────────────────────────────────────────────
        Card(
            modifier  = Modifier
                .fillMaxWidth(0.86f)
                .scale(cardScale)
                .shadow(32.dp, RoundedCornerShape(28.dp)),
            shape     = RoundedCornerShape(28.dp),
            colors    = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // ── Accent pill ────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accentColor.copy(alpha = 0.12f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    val tagLabel = when {
                        event.isRoam             -> "WALK COMPLETE"
                        event.stolenFrom != null -> "TERRITORY STOLEN"
                        else                     -> "TERRITORY CAPTURED"
                    }
                    Text(
                        text      = tagLabel,
                        fontSize  = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color     = accentColor,
                        letterSpacing = 1.5.sp
                    )
                }

                // ── Emoji with pulse ───────────────────────────────────────
                val emojiScale by infiniteTransition.animateFloat(
                    initialValue  = 1f,
                    targetValue   = 1.12f,
                    animationSpec = infiniteRepeatable(
                        animation  = tween(700, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "emojiPulse"
                )
                Text(
                    text     = if (event.isRoam) "🏃" else if (event.stolenFrom != null) "⚔️" else "🏆",
                    fontSize = 60.sp,
                    modifier = Modifier.scale(emojiScale)
                )

                // ── Headline ────────────────────────────────────────────────
                val headline = when {
                    event.isRoam                -> "Walk Complete!"
                    event.stolenFrom != null    -> "Territory Stolen!"
                    event.mergedCount > 0       -> "Territories Merged!"
                    else                        -> "Territory Claimed!"
                }
                Text(
                    text       = headline,
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = headlineColor,
                    textAlign  = TextAlign.Center
                )

                // ── Sub-line ────────────────────────────────────────────────
                val sub = when {
                    event.isRoam             -> "%.2f km explored today".format(event.distanceKm)
                    event.stolenFrom != null -> "Ripped from ${event.stolenFrom}! 🔥"
                    event.mergedCount > 0    -> "+${event.mergedCount} zones absorbed into your empire"
                    else                     -> "Your empire just got bigger!"
                }
                Text(
                    text      = sub,
                    fontSize  = 14.sp,
                    color     = subLineColor,
                    textAlign = TextAlign.Center
                )

                // ── Area badge (compete only) ────────────────────────────────
                if (!event.isRoam && event.areaM2 > 0) {
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(accentColor.copy(alpha = 0.15f), accentColor.copy(alpha = 0.05f))
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text      = buildAreaLabel(event.areaM2),
                            fontSize  = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color     = accentColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ── Dismiss hint + progress bar ──────────────────────────────
                Text(
                    text  = if (autoDismissMs > 0L) "Tap to dismiss" else "Tap to close  •  Double-tap to replay 🔄",
                    fontSize = 11.sp,
                    color = Color(0xFFAAAAAA)
                )
                if (autoDismissMs > 0L) {
                    AutoDismissBar(durationMs = autoDismissMs, color = accentColor)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun buildAreaLabel(areaM2: Double): String = when {
    areaM2 >= 1_000_000 -> "%.2f km²".format(areaM2 / 1_000_000.0)
    else                -> "%,.0f m²".format(areaM2)
}

@Composable
private fun AutoDismissBar(durationMs: Long, color: Color) {
    var targetProgress by remember { mutableFloatStateOf(1f) }
    val progress by animateFloatAsState(
        targetValue   = targetProgress,
        animationSpec = tween(durationMs.toInt(), easing = LinearEasing),
        label         = "dismissBar"
    )
    LaunchedEffect(Unit) { targetProgress = 0f }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
    }
}

private val DiamondShape = object : androidx.compose.ui.graphics.Shape {
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
