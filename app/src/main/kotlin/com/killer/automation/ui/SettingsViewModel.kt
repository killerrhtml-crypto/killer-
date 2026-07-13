package com.killer.automation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.killer.automation.data.UserPreferences
import com.killer.automation.data.OfferData
import com.killer.automation.data.OfferEvaluationResult
import com.killer.automation.data.security.EncryptedDataManager
import com.killer.automation.engine.DecisionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val encryptedDataManager: EncryptedDataManager,
    private val decisionEngine: DecisionEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(encryptedDataManager.getUserPreferences() ?: UserPreferences.default("user_default"))
    val uiState: StateFlow<UserPreferences> = _uiState

    fun updateBotEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.copy(isEnabled = isEnabled, lastUpdated = System.currentTimeMillis())
            encryptedDataManager.saveUserPreferences(updated)
            _uiState.value = updated
        }
    }

    fun updateMinPrice(price: Double) {
        viewModelScope.launch {
            val updated = _uiState.value.copy(minPrice = price, lastUpdated = System.currentTimeMillis())
            encryptedDataManager.saveUserPreferences(updated)
            _uiState.value = updated
        }
    }

    // Diagnóstico: Probar pipeline completo
    suspend fun simulateOffer(): OfferEvaluationResult {
        val dummyOffer = OfferData(
            offerId = "test_001",
            price = 50.0,
            distance = 1.0,
            estimatedTime = 5,
            pickupLatitude = 0.0,
            pickupLongitude = 0.0,
            dropoffLatitude = 0.0,
            dropoffLongitude = 0.0,
            pickupAddress = "Test",
            dropoffAddress = "Test",
            offerTimestamp = System.currentTimeMillis(),
            expirationTime = 10000L
        )
        return decisionEngine.evaluateOffer(dummyOffer)
    }
}
