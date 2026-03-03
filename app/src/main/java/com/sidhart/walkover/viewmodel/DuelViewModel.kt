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
}
