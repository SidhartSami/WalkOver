package com.sidhart.walkover.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sidhart.walkover.data.User
import com.sidhart.walkover.data.Walk
import com.sidhart.walkover.service.FirebaseService
import com.sidhart.walkover.ui.WeeklyStats
import com.sidhart.walkover.ui.MonthlyStats
import com.sidhart.walkover.ui.calculateWeeklyStats
import com.sidhart.walkover.ui.calculateMonthlyStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

                android.util.Log.d("ProfileViewModel", "Profile data loaded successfully: ${walks.size} walks")

            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Failed to load profile data", e)
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