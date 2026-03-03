package com.sidhart.walkover.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sidhart.walkover.data.DuelChallenge
import java.util.Locale
@Composable
fun DuelCompletionDialog(
    challenge: DuelChallenge,
    currentUserId: String,
    onDismiss: () -> Unit
) {
    val isChallenger = challenge.challengerId == currentUserId
    val myDistance = if (isChallenger) challenge.challengerDistanceKm else challenge.opponentDistanceKm
    val theirDistance = if (isChallenger) challenge.opponentDistanceKm else challenge.challengerDistanceKm

    val didIWin = challenge.winnerId == currentUserId
    val isTie = challenge.winnerId == null

    val title = when {
        isTie -> "It's a Tie!"
        didIWin -> "You Won!"
        else -> "You Lost!"
    }

    val subtitle = "The Duel Challenge has ended."

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (didIWin) MaterialTheme.colorScheme.primary else if (isTie) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                // Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("You", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(String.format(Locale.US, "%.2f km", myDistance), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    }
                    Text("vs", modifier = Modifier.align(Alignment.CenterVertically), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Them", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(String.format(Locale.US, "%.2f km", theirDistance), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Awesome!")
                }
            }
        }
    }
}
