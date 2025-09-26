package com.sidhart.walkover.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.sidhart.walkover.data.User
import com.sidhart.walkover.service.FirebaseService
import com.sidhart.walkover.ui.theme.WalkOverTheme
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class LeaderboardActivity : ComponentActivity() {
    
    private lateinit var firebaseService: FirebaseService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        firebaseService = FirebaseService()
        
        setContent {
            WalkOverTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LeaderboardScreen(
                        modifier = Modifier.padding(innerPadding),
                        firebaseService = firebaseService,
                        context = this@LeaderboardActivity
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    modifier: Modifier = Modifier,
    firebaseService: FirebaseService,
    context: ComponentActivity
) {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var currentScoreType by remember { mutableStateOf(ScoreType.AREA) }
    var isLoading by remember { mutableStateOf(false) }
    
    val decimalFormat = remember { DecimalFormat("#.##") }
    
    // Load leaderboard when score type changes
    LaunchedEffect(currentScoreType) {
        isLoading = true
        val result = when (currentScoreType) {
            ScoreType.AREA -> firebaseService.getLeaderboard()
            ScoreType.DISTANCE -> firebaseService.getDistanceLeaderboard()
        }
        
        result.fold(
            onSuccess = { userList ->
                users = userList
                isLoading = false
                if (userList.isEmpty()) {
                    Toast.makeText(context, "No data available", Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { error ->
                isLoading = false
                Toast.makeText(context, "Failed to load leaderboard: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Leaderboard",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Toggle buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { currentScoreType = ScoreType.AREA },
                modifier = Modifier.weight(1f),
                enabled = currentScoreType != ScoreType.AREA
            ) {
                Text("Area")
            }
            
            Button(
                onClick = { currentScoreType = ScoreType.DISTANCE },
                modifier = Modifier.weight(1f),
                enabled = currentScoreType != ScoreType.DISTANCE
            ) {
                Text("Distance")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Leaderboard list
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(users) { index, user ->
                    LeaderboardItem(
                        rank = index + 1,
                        user = user,
                        scoreType = currentScoreType,
                        decimalFormat = decimalFormat
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { context.finish() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Map")
        }
    }
}

@Composable
fun LeaderboardItem(
    rank: Int,
    user: User,
    scoreType: ScoreType,
    decimalFormat: DecimalFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rank.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.username.ifEmpty { "Anonymous" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                val score = when (scoreType) {
                    ScoreType.AREA -> user.totalAreaCaptured
                    ScoreType.DISTANCE -> user.totalDistanceWalked
                }
                
                val unit = when (scoreType) {
                    ScoreType.AREA -> "m²"
                    ScoreType.DISTANCE -> "m"
                }
                
                Text(
                    text = "${decimalFormat.format(score)} $unit",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

enum class ScoreType {
    AREA, DISTANCE
}

