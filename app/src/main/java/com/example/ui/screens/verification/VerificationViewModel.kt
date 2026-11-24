package com.example.ui.screens.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ScanMethod
import com.example.data.model.ScanReportItem
import com.example.data.model.VerificationInput
import com.example.data.repository.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

sealed interface VerificationUiState {
    data object Processing : VerificationUiState
    data class Ready(val report: ScanReportItem) : VerificationUiState
    data class Error(val message: String) : VerificationUiState
}

class VerificationViewModel(
    private val scanRepository: ScanRepository,
    private val isOnlineProvider: () -> Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow<VerificationUiState>(VerificationUiState.Processing)
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    fun runVerification(
        userId: String,
        methodName: String,
        rawValue: String,
        formatLabel: String,
        imageUrisJson: String,
        notes: String,
        onCompleted: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = VerificationUiState.Processing
            try {
                val method = ScanMethod.entries.firstOrNull { it.name == methodName } ?: ScanMethod.BARCODE
                val imageUris = try {
                    json.decodeFromString<List<String>>(imageUrisJson)
                } catch (_: Exception) {
                    emptyList()
                }

                val report = scanRepository.verifyAndSaveScan(
                    userId = userId,
                    input = VerificationInput(
                        method = method,
                        rawValue = rawValue,
                        formatLabel = formatLabel,
                        imageUriStrings = imageUris,
                        notes = notes
                    ),
                    isOnline = isOnlineProvider()
                )
                _uiState.value = VerificationUiState.Ready(report)
                onCompleted(report.id)
            } catch (e: Exception) {
                _uiState.value = VerificationUiState.Error(
                    e.localizedMessage ?: "Verification could not be completed. Please try again."
                )
            }
        }
    }

    fun loadReport(userId: String, scanId: String) {
        viewModelScope.launch {
            _uiState.value = VerificationUiState.Processing
            val report = scanRepository.getReportAndRecordView(userId, scanId)
            if (report != null) {
                _uiState.value = VerificationUiState.Ready(report)
            } else {
                _uiState.value = VerificationUiState.Error("Verification report not found.")
            }
        }
    }
}
