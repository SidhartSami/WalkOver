package com.sidhart.walkover.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sidhart.walkover.data.LeaderboardCategory
import com.sidhart.walkover.data.LeaderboardEntry
import com.sidhart.walkover.data.LeaderboardPeriod
import com.sidhart.walkover.service.FirebaseService
import com.sidhart.walkover.ui.theme.NeonGreen
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.outlined.ArrowBack


private enum class LeaderboardScope { GLOBAL, FRIENDS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    firebaseService: FirebaseService,
    onNavigateBack: () -> Unit,
    onNavigateToFriendsList: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var selectedScope by remember { mutableStateOf(LeaderboardScope.GLOBAL) }
    var entries       by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var isLoading     by remember { mutableStateOf(false) }
    var errorMessage  by remember { mutableStateOf<String?>(null) }
    var isRefreshing  by remember { mutableStateOf(false) }

    val pullToRefreshState = rememberPullToRefreshState()

    // Auto-sort by XP (all-time total score) — single criterion, no chip clutter
    fun loadLeaderboard() {
        scope.launch {
            isLoading = true
            errorMessage = null
            val result = when (selectedScope) {
                LeaderboardScope.GLOBAL  -> firebaseService.getLeaderboardData(
                    category    = LeaderboardCategory.XP,
                    period      = LeaderboardPeriod.ALL_TIME,
                    friendsOnly = false
                )
                LeaderboardScope.FRIENDS -> firebaseService.getLeaderboardData(
                    category    = LeaderboardCategory.XP,
                    period      = LeaderboardPeriod.ALL_TIME,
                    friendsOnly = true
                )
            }
            result.fold(onSuccess = { entries = it }, onFailure = { errorMessage = it.message })
            isLoading = false
            isRefreshing = false
        }
    }

    LaunchedEffect(selectedScope) { loadLeaderboard() }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    // ── Title row ─────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = "Leaderboard",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                        // Removed Refresh Button as we have Pull To Refresh now
                        // Add Friend (friends scope) or just always visible
                        IconButton(onClick = onNavigateToFriendsList) {
                            Icon(
                                Icons.Outlined.PersonAdd,
                                contentDescription = "Add Friend",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // ── Global / Friends toggle ────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp),
                    ) {
                        LeaderboardScope.entries.forEach { scopeOption ->
                            val selected = scopeOption == selectedScope
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                onClick = { selectedScope = scopeOption }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 11.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (scopeOption == LeaderboardScope.GLOBAL)
                                            Icons.Outlined.Public else Icons.Outlined.Group,
                                        contentDescription = null,
                                        tint = if (selected) MaterialTheme.colorScheme.onPrimary
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(7.dp))
                                    Text(
                                        text = scopeOption.name
                                            .replaceFirstChar { it.uppercaseChar() },
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.WifiOff, null, modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text(errorMessage!!, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { loadLeaderboard() }) { Text("Retry") }
                    }
                }
                entries.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏆", fontSize = 72.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (selectedScope == LeaderboardScope.FRIENDS)
                            "No friends yet!\nTap the 👤+ button to find people to stalk."
                        else "No walkers yet — be first on the board!",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedScope == LeaderboardScope.FRIENDS) {
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = onNavigateToFriendsList) {
                            Icon(Icons.Outlined.PersonAdd, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Find People")
                        }
                    }
                    }
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            loadLeaderboard()
                        },
                        state = pullToRefreshState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (entries.size >= 3) {
                                item { Podium(top3 = entries.take(3)) }
                                item { Spacer(Modifier.height(4.dp)) }
                            }
                            val rest = if (entries.size > 3) entries.drop(3) else entries
                            itemsIndexed(rest, key = { _, e -> e.userId }) { _, entry ->
                                LeaderboardRow(entry = entry)
                            }
                        }
                    }
                }
            }
        }
    }
}



// ─────────────────────────────────────────────────────────────────
// Podium — top 3
// ─────────────────────────────────────────────────────────────────
@Composable
private fun Podium(top3: List<LeaderboardEntry>) {
    val gold   = Color(0xFFFFD700)
    val silver = Color(0xFFB0B8C8)
    val bronze = Color(0xFFCD7F32)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // 2nd — left
            PodiumSlot(entry = top3[1], medal = "🥈", columnHeight = 72.dp, badgeColor = silver)
            // 1st — centre (tallest)
            PodiumSlot(entry = top3[0], medal = "🥇", columnHeight = 104.dp, badgeColor = gold)
            // 3rd — right
            PodiumSlot(entry = top3[2], medal = "🥉", columnHeight = 56.dp, badgeColor = bronze)
        }
    }
}

@Composable
private fun RowScope.PodiumSlot(
    entry: LeaderboardEntry,
    medal: String,
    columnHeight: androidx.compose.ui.unit.Dp,
    badgeColor: Color
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(badgeColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = medal,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeColor
                )
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = entry.username,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(columnHeight)
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(badgeColor.copy(alpha = 0.35f), badgeColor.copy(alpha = 0.12f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("Lv.${entry.currentLevel}",
                style = MaterialTheme.typography.labelSmall.copy(color = badgeColor))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Leaderboard Row — rank 4+
// ─────────────────────────────────────────────────────────────────
@Composable
private fun LeaderboardRow(entry: LeaderboardEntry) {
    val isDark = isSystemInDarkTheme()

    // Highlight current user lightly without breaking theme
    val containerColor = if (entry.isCurrentUser) {
        if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = MaterialTheme.colorScheme.onSurface
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = if (entry.isCurrentUser) {
        if (isDark) NeonGreen else MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary
    }
    val rankColor = when (entry.rank) {
        1    -> Color(0xFFFFD700)
        2    -> Color(0xFFB0B8C8)
        3    -> Color(0xFFCD7F32)
        else -> labelColor
    }

    val cardModifier = if (entry.isCurrentUser && !isDark) {
        Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    } else Modifier.fillMaxWidth()

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(if (entry.isCurrentUser) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rank
            Text(
                text = "#${entry.rank}",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = rankColor
                ),
                modifier = Modifier.width(34.dp),
                textAlign = TextAlign.Center
            )

            // Avatar bubble
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.username.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                )
            }

            // Username + level
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.username,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    if (entry.isCurrentUser) {
                        Spacer(Modifier.width(6.dp))
                        // "YOU" tag — uses primary color, no dark background
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "YOU",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
                Text(
                    "Level ${entry.currentLevel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = labelColor
                )
            }

            // XP score
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.totalXPEarned} XP",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                )
                Text(
                    text = "${entry.totalWalks} walks",
                    style = MaterialTheme.typography.bodySmall,
                    color = labelColor
                )
            }
        }
    }
}