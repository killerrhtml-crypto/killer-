package com.killer.automation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.killer.automation.data.EncryptedDataManager
import com.killer.automation.data.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val encryptedDataManager: EncryptedDataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserPreferences.default("default_user"))
    val uiState: StateFlow<UserPreferences> = _uiState

    init {
        viewModelScope.launch {
            try {
                encryptedDataManager.getUserPreferences()?.let {
                    _uiState.value = it
                }
            } catch (t: Throwable) {
                // Fail-safe: keep defaults
            }
        }
    }

    fun updateBotEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value
            val updated = current.copy(isEnabled = isEnabled, lastUpdated = System.currentTimeMillis())
            encryptedDataManager.saveUserPreferences(updated)
            _uiState.value = updated
        }
    }

    fun updateMinPrice(price: Double) {
        viewModelScope.launch {
            val current = _uiState.value
            val updated = current.copy(minPrice = price, lastUpdated = System.currentTimeMillis())
            encryptedDataManager.saveUserPreferences(updated)
            _uiState.value = updated
        }
    }

    fun updateSelectedZone(zone: String) {
        viewModelScope.launch {
            val current = _uiState.value
            val updated = current.copy(selectedZone = zone, lastUpdated = System.currentTimeMillis())
            encryptedDataManager.saveUserPreferences(updated)
            _uiState.value = updated
        }
    }
}
