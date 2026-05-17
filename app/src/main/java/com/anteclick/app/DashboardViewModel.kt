package com.anteclick.app

import androidx.lifecycle.ViewModel
import com.anteclick.app.backend.ReputationCache
import com.anteclick.app.models.BackendThreatResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DetectedThreat(
    val domain: String,
    val threatType: String,
    val timestamp: Long
)

class DashboardViewModel : ViewModel() {
    private val _detectedThreats = MutableStateFlow<List<DetectedThreat>>(emptyList())
    val detectedThreats: StateFlow<List<DetectedThreat>> = _detectedThreats.asStateFlow()

    fun refreshThreats() {
        // Access cache to get real detected threats
        // Note: ReputationCache doesn't expose all entries, so we'll need to track threats separately
        // For now, this returns empty list - threats should be logged when detected
        _detectedThreats.value = emptyList()
    }
}
