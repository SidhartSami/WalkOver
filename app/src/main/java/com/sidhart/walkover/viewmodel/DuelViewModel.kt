package com.sidhart.walkover.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sidhart.walkover.data.DuelChallenge
import com.sidhart.walkover.repository.DuelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DuelViewModel : ViewModel() {
    private val repository = DuelRepository()

    private val _activeDuel = MutableStateFlow<DuelChallenge?>(null)
    val activeDuel: StateFlow<DuelChallenge?> = _activeDuel.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** A completed duel whose result the current user has not yet acknowledged. */
    private val _pendingResultDuel = MutableStateFlow<DuelChallenge?>(null)
    val pendingResultDuel: StateFlow<DuelChallenge?> = _pendingResultDuel.asStateFlow()

    fun checkActiveDuel(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getActiveDuel(userId)
            if (result.isSuccess) {
                _activeDuel.value = result.getOrNull()
            }
            _isLoading.value = false
        }
    }

    fun clearActiveDuel() {
        _activeDuel.value = null
    }

    /**
     * Called on app open / login to see if there's a completed duel
     * the user hasn't celebrated yet.
     */
    fun checkForUnseenDuelResult(userId: String) {
        viewModelScope.launch {
            val result = repository.getUnseenCompletedDuels(userId)
            if (result.isSuccess) {
                // Show only the most recent one at a time
                _pendingResultDuel.value = result.getOrNull()?.firstOrNull()
            }
        }
    }

    /**
     * Mark the pending result as seen so the celebration won't appear again.
     */
    fun markResultSeen(challenge: DuelChallenge, currentUserId: String) {
        viewModelScope.launch {
            val isChallenger = challenge.challengerId == currentUserId
            repository.markResultSeen(challenge.id, isChallenger)
            _pendingResultDuel.value = null
        }
    }
}
