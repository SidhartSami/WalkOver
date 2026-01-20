package com.sidhart.walkover.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sidhart.walkover.ui.theme.NeonGreen

data class TutorialStep(
    val id: String,
    val title: String,
    val description: String,
    val targetCoordinates: LayoutCoordinates? = null,
    val targetRect: Rect? = null // Fallback if coordinates aren't used
)

@Composable
fun TutorialOverlay(
    currentStep: TutorialStep,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val targetRect = remember(currentStep) {
        currentStep.targetCoordinates?.let { coords ->
            val position = coords.positionInRoot()
            val size = coords.size
            Rect(
                left = position.x,
                top = position.y,
                right = position.x + size.width,
                bottom = position.y + size.height
            )
        } ?: currentStep.targetRect ?: Rect(0f, 0f, 0f, 0f)
    }
    
    // Animation for the "breathing" focus effect
    val infiniteTransition = rememberInfiniteTransition(label = "focus_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = false) {} // Consume clicks
    ) {
        // Darkened Background with Focus Hole
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.99f)) {
            // Draw semi-transparent black overlay
            drawRect(Color.Black.copy(alpha = 0.8f))

            // Cut out the hole using BlendMode.Clear
            if (targetRect.width > 0) {
                 drawCircle(
                     color = Color.Transparent,
                     radius = (maxOf(targetRect.width, targetRect.height) / 2) * 1.2f * pulseScale,
                     center = targetRect.center,
                     blendMode = BlendMode.Clear
                 )
                // Optional: Draw a ring around it
                drawCircle(
                    color = NeonGreen,
                    radius = (maxOf(targetRect.width, targetRect.height) / 2) * 1.25f * pulseScale,
                    center = targetRect.center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                )
            }
        }

        // Tooltip Content
        // Position logic: Place above or below the target depending on screen position
        // Simplification: Always center horizontally, adapt verticality
        val isTargetInTopHalf = targetRect.center.y < 1000f // Approximate, better to use screen height
        
        Card(
            modifier = Modifier
                .align(if (isTargetInTopHalf) Alignment.BottomCenter else Alignment.TopCenter)
                .padding(24.dp)
                .padding(bottom = if (isTargetInTopHalf) 48.dp else 0.dp) // Lift up from bottom
                .padding(top = if (!isTargetInTopHalf) 120.dp else 0.dp) // Push down from top
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = currentStep.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentStep.description,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onSkip) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onNext,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                    ) {
                        Text("Next")
                    }
                }
            }
        }
    }
}
