package com.sidhart.walkover.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sidhart.walkover.data.UserSearchResult
import com.sidhart.walkover.data.DuelChallenge
import com.sidhart.walkover.service.FirebaseService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsListScreen(
    firebaseService: FirebaseService,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Stalking List State
    var followingList by remember { mutableStateOf<List<UserSearchResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Search State
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    
    // Challenge State
    var showChallengeDialog by remember { mutableStateOf<UserSearchResult?>(null) }
    var pendingChallenges by remember { mutableStateOf<List<DuelChallenge>>(emptyList()) }
    var sentChallenges by remember { mutableStateOf<List<DuelChallenge>>(emptyList()) }
    var activeDuel by remember { mutableStateOf<DuelChallenge?>(null) }

    // Load following data and pending challenges
    LaunchedEffect(Unit) {
        coroutineScope {
            val f1 = async { firebaseService.getFollowing() }
            val f2 = async { firebaseService.getPendingDuels() }
            val f3 = async { firebaseService.getSentPendingDuels() }
            val f4 = async { firebaseService.getActiveDuel() }
            
            f1.await().fold(
                onSuccess = { followingList = it },
                onFailure = { android.util.Log.e("FriendsListScreen", "Error loading following", it) }
            )
            f2.await().fold(onSuccess = { pendingChallenges = it }, onFailure = {})
            f3.await().fold(onSuccess = { sentChallenges = it }, onFailure = {})
            f4.await().fold(onSuccess = { activeDuel = it }, onFailure = {})
        }
        isLoading = false
    }

    // Debounced search logic
    LaunchedEffect(searchQuery) {
        searchJob?.cancel()
        if (searchQuery.length >= 2) {
            searchJob = scope.launch {
                delay(500)
                isSearching = true
                firebaseService.searchUsers(searchQuery).fold(
                    onSuccess = { results -> searchResults = results },
                    onFailure = { searchResults = emptyList() }
                )
                isSearching = false
            }
        } else {
            searchResults = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stalking", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Find people to stalk...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            if (isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // List Content
            val displayList = if (searchQuery.length >= 2) searchResults else followingList

            if (isLoading && !isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (displayList.isEmpty() && !isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No results found" else "You aren't stalking anyone yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (searchQuery.isEmpty() && pendingChallenges.isNotEmpty()) {
                        item {
                            Text(
                                "Pending Duel Challenges (${pendingChallenges.size})",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        items(pendingChallenges) { challenge ->
                            PendingChallengeItem(
                                challenge = challenge,
                                onAccept = {
                                    scope.launch {
                                        firebaseService.acceptDuelChallenge(challenge.id, challenge.durationDays).fold(
                                            onSuccess = {
                                                // Clear all pending when one is accepted
                                                pendingChallenges = emptyList()
                                                Toast.makeText(context, "Challenge Accepted!", Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = {}
                                        )
                                    }
                                },
                                onDecline = {
                                    scope.launch {
                                        firebaseService.declineDuelChallenge(challenge.id).fold(
                                            onSuccess = {
                                                pendingChallenges = pendingChallenges.filter { it.id != challenge.id }
                                                Toast.makeText(context, "Challenge Declined", Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = {}
                                        )
                                    }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    if (searchQuery.isEmpty() && followingList.isNotEmpty()) {
                        item {
                            Text(
                                "People You Stalk",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    items(displayList) { user ->
                        // check if the user is already being stalked (exists in followingList)
                        val isAlreadyStalking = followingList.any { it.id == user.id }
                        val isChallengePending = sentChallenges.any { it.opponentId == user.id }
                        val hasActiveDuel = activeDuel != null
                        
                        FriendItem(
                            user = user,
                            isStalking = isAlreadyStalking,
                            isChallengePending = isChallengePending,
                            hasActiveDuel = hasActiveDuel,
                            onActionClick = {
                                scope.launch {
                                    if (isAlreadyStalking) {
                                        firebaseService.unfollowUser(user.id).fold(
                                            onSuccess = {
                                                followingList = followingList.filter { it.id != user.id }
                                                Toast.makeText(context, "Unstalked ${user.username}", Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = {}
                                        )
                                    } else {
                                        // Stalk action
                                        firebaseService.followUser(user.id, user.username).fold(
                                            onSuccess = {
                                                followingList = followingList + user.copy(isFollowing = true)
                                                Toast.makeText(context, "Stalking ${user.username}", Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = {}
                                        )
                                    }
                                }
                            },
                            onChallengeClick = {
                                showChallengeDialog = user
                            }
                        )
                    }
                }
            }
        }
    }

    showChallengeDialog?.let { userToChallenge ->
        AlertDialog(
            onDismissRequest = { showChallengeDialog = null },
            title = { Text("Challenge ${userToChallenge.username}") },
            text = { Text("Select duration for the Duel Challenge:") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        firebaseService.createDuelChallenge(userToChallenge.id, userToChallenge.username, 7).fold(
                            onSuccess = {
                                Toast.makeText(context, "Challenge sent!", Toast.LENGTH_SHORT).show()
                                firebaseService.getSentPendingDuels().fold(
                                    onSuccess = { sentChallenges = it },
                                    onFailure = {}
                                )
                                showChallengeDialog = null
                            },
                            onFailure = {
                                Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                showChallengeDialog = null
                            }
                        )
                    }
                }) { Text("7 Days") }
            },
            dismissButton = {
                Button(onClick = {
                    scope.launch {
                        firebaseService.createDuelChallenge(userToChallenge.id, userToChallenge.username, 3).fold(
                            onSuccess = {
                                Toast.makeText(context, "Challenge sent!", Toast.LENGTH_SHORT).show()
                                firebaseService.getSentPendingDuels().fold(
                                    onSuccess = { sentChallenges = it },
                                    onFailure = {}
                                )
                                showChallengeDialog = null
                            },
                            onFailure = {
                                Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                showChallengeDialog = null
                            }
                        )
                    }
                }) { Text("3 Days") }
            }
        )
    }
}

@Composable
private fun FriendItem(
    user: UserSearchResult,
    isStalking: Boolean,
    isChallengePending: Boolean = false,
    hasActiveDuel: Boolean = false,
    onActionClick: () -> Unit,
    onChallengeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.username.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Level ${user.currentLevel}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Challenge Button
            if (hasActiveDuel) {
                // Return nothing so button vanishes
            } else if (isChallengePending) {
                IconButton(
                    onClick = { },
                    enabled = false,
                    colors = IconButtonDefaults.iconButtonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = "Pending Challenge")
                }
            } else {
                IconButton(
                    onClick = onChallengeClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = "Challenge")
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            // Action Button
            IconButton(
                onClick = onActionClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isStalking) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                    contentColor = if (isStalking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (isStalking) Icons.Default.PersonOff else Icons.Default.Person,
                    contentDescription = if (isStalking) "Unstalk" else "Stalk"
                )
            }
        }
    }
}

@Composable
private fun PendingChallengeItem(
    challenge: DuelChallenge,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val titleText = if (challenge.challengerUsername.isNotEmpty()) {
                    "Duel Request from ${challenge.challengerUsername}"
                } else {
                    "Duel Challenge Request"
                }
                Text(
                    text = titleText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Duration: ${challenge.durationDays} Days",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f)
                )
            }
            
            IconButton(
                onClick = onAccept,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = "Accept")
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDecline,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = "Decline")
            }
        }
    }
}
