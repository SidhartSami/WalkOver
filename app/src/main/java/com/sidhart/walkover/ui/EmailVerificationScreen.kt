package com.sidhart.walkover.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sidhart.walkover.data.User
import com.sidhart.walkover.data.Walk
import com.sidhart.walkover.service.FirebaseService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


@Composable
fun EmailVerificationScreen(
    firebaseService: FirebaseService,
    userEmail: String,
    onVerificationComplete: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isChecking by remember { mutableStateOf(false) }
    var isSendingEmail by remember { mutableStateOf(false) }

    // Auto-check verification every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            if (!isChecking) {
                isChecking = true
                try {
                    firebaseService.reloadUser().fold(
                        onSuccess = { isVerified ->
                            if (isVerified) {
                                // Add delay before navigation to prevent crash
                                delay(500)
                                Toast.makeText(context, "Email verified! Welcome!", Toast.LENGTH_SHORT).show()
                                onVerificationComplete()
                            }
                        },
                        onFailure = { /* Silent fail for auto-check */ }
                    )
                } catch (e: Exception) {
                    android.util.Log.e("EmailVerification", "Auto-check failed", e)
                } finally {
                    isChecking = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Email Icon
            Icon(
                imageVector = Icons.Outlined.MarkEmailRead,
                contentDescription = "Verify Email",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Verify Your Email",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = "We've sent a verification link to:",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Email
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = userEmail,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Instructions
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InstructionItem(
                        number = "1",
                        text = "Check your email inbox (and spam folder)"
                    )
                    InstructionItem(
                        number = "2",
                        text = "Click the verification link"
                    )
                    InstructionItem(
                        number = "3",
                        text = "Return here - we'll detect it automatically"
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Auto-checking status
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Checking verification status...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // Helper text
            Text(
                text = "Didn't receive the email? Check your spam folder or resend.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Back to Login
            TextButton(
                onClick = onBackToLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back to Login",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun InstructionItem(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Sealed class representing all possible UI states for the Profile screen.
 * This prevents race conditions and ensures consistent UI rendering.
 */
sealed class ProfileUiState {
    /**
     * Initial loading state when entering the screen for the first time
     */
    object Loading : ProfileUiState()

    /**
     * Data loaded successfully
     */
    data class Success(
        val user: User,
        val allWalks: List<Walk>,
        val recentWalks: List<Walk>,
        val weeklyStats: WeeklyStats,
        val monthlyStats: MonthlyStats
    ) : ProfileUiState()

    /**
     * Error occurred during data loading
     */
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : ProfileUiState()
}

class ProfileViewModel(
    private val firebaseService: FirebaseService
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfileData()
    }

    /**
     * Loads all profile data in a single coordinated operation.
     * This prevents multiple loading states and ensures data consistency.
     */
    fun loadProfileData() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading

            try {
                // Load user data
                val userResult = firebaseService.getCurrentUserData()
                val user = userResult.getOrNull()
                    ?: throw Exception("Failed to load user data: ${userResult.exceptionOrNull()?.message}")

                // Load walks
                val walksResult = firebaseService.getWalks()
                val walks = walksResult.getOrNull()
                    ?: throw Exception("Failed to load walks: ${walksResult.exceptionOrNull()?.message}")

                // Calculate statistics
                val weeklyStats = calculateWeeklyStats(walks)
                val monthlyStats = calculateMonthlyStats(walks)
                val recentWalks = walks.take(3)

                // Update to success state with all data
                _uiState.value = ProfileUiState.Success(
                    user = user,
                    allWalks = walks,
                    recentWalks = recentWalks,
                    weeklyStats = weeklyStats,
                    monthlyStats = monthlyStats
                )

                Log.d("ProfileViewModel", "Profile data loaded successfully: ${walks.size} walks")

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to load profile data", e)
                _uiState.value = ProfileUiState.Error(
                    message = e.message ?: "Unknown error occurred",
                    canRetry = true
                )
            }
        }
    }

    /**
     * Refreshes the profile data. Can be called from pull-to-refresh or retry actions.
     */
    fun refresh() {
        loadProfileData()
    }
}
