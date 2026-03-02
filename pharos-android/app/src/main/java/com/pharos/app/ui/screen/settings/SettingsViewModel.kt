package com.pharos.app.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharos.app.data.repository.SettingsRepository
import com.pharos.app.network.api.AiApiProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKey: String = "",
    val hasStoredKey: Boolean = false,
    val onlyChangedFiles: Boolean = true,
    val testResult: String? = null,
    val isTesting: Boolean = false,
    val isError: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val aiApiProvider: AiApiProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val hasKey = settingsRepository.getApiKey() != null
        val onlyChanged = settingsRepository.getOnlyChangedFiles()

        _uiState.value = _uiState.value.copy(
            hasStoredKey = hasKey,
            onlyChangedFiles = onlyChanged
        )
    }

    fun updateApiKey(key: String) {
        _uiState.value = _uiState.value.copy(apiKey = key)
    }

    fun saveApiKey() {
        val key = _uiState.value.apiKey.trim()
        if (key.isNotEmpty()) {
            settingsRepository.saveApiKey(key)
            _uiState.value = _uiState.value.copy(
                hasStoredKey = true,
                testResult = "Key saved successfully",
                isError = false
            )
        }
    }

    fun deleteApiKey() {
        settingsRepository.deleteApiKey()
        _uiState.value = _uiState.value.copy(
            apiKey = "",
            hasStoredKey = false,
            testResult = "Key deleted",
            isError = false
        )
    }

    fun testApiKey() {
        val key = _uiState.value.apiKey.trim().ifEmpty {
            settingsRepository.getApiKey() ?: ""
        }

        if (key.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                testResult = "No API key to test",
                isError = true
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true, testResult = null)
            try {
                aiApiProvider.testApiKey(key)
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    testResult = "API key is valid!",
                    isError = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    testResult = "Test failed: ${e.message}",
                    isError = true
                )
            }
        }
    }

    fun toggleOnlyChangedFiles() {
        val newValue = !_uiState.value.onlyChangedFiles
        settingsRepository.setOnlyChangedFiles(newValue)
        _uiState.value = _uiState.value.copy(onlyChangedFiles = newValue)
    }

    fun clearTestResult() {
        _uiState.value = _uiState.value.copy(testResult = null)
    }
}
