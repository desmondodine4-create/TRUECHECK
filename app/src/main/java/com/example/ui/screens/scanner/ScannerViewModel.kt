package com.example.ui.screens.scanner

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.ai.ImageCompressionUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SecurityCodeUiState(
    val code: String = "",
    val error: String? = null,
    val attemptsInWindow: Int = 0,
    val cooldownUntilMillis: Long = 0L
)

data class PhotoInspectionUiState(
    val capturedUris: List<String> = emptyList(),
    val notes: String = "",
    val isCompressing: Boolean = false,
    val error: String? = null
)

class ScannerViewModel : ViewModel() {

    private val _securityCodeState = MutableStateFlow(SecurityCodeUiState())
    val securityCodeState: StateFlow<SecurityCodeUiState> = _securityCodeState.asStateFlow()

    private val _photoState = MutableStateFlow(PhotoInspectionUiState())
    val photoState: StateFlow<PhotoInspectionUiState> = _photoState.asStateFlow()

    fun updateSecurityCode(input: String) {
        val filtered = input.uppercase().filter { it.isLetterOrDigit() || it == '-' }
        _securityCodeState.update { it.copy(code = filtered, error = null) }
    }

    fun validateAndSubmitSecurityCode(onValid: (String) -> Unit) {
        val now = System.currentTimeMillis()
        val current = _securityCodeState.value

        if (now < current.cooldownUntilMillis) {
            val secondsLeft = ((current.cooldownUntilMillis - now) / 1000L).coerceAtLeast(1)
            _securityCodeState.update {
                it.copy(error = "Too many verification attempts. Please wait ${secondsLeft}s before trying again.")
            }
            return
        }

        val clean = current.code.trim()
        if (clean.isEmpty()) {
            _securityCodeState.update { it.copy(error = "Please enter the security code printed on your packaging.") }
            return
        }
        if (clean.length < 6 || clean.length > 24 || !clean.matches(Regex("^[A-Z0-9\\-]+$"))) {
            _securityCodeState.update {
                it.copy(error = "Security code must be 6–24 alphanumeric characters.")
            }
            return
        }

        val nextAttempts = current.attemptsInWindow + 1
        val nextCooldown = if (nextAttempts >= 5) now + 30_000L else 0L
        _securityCodeState.update {
            it.copy(
                error = null,
                attemptsInWindow = if (nextAttempts >= 5) 0 else nextAttempts,
                cooldownUntilMillis = nextCooldown
            )
        }
        onValid(clean)
    }

    fun addImageUri(context: Context, uri: Uri) {
        if (_photoState.value.capturedUris.size >= 4) {
            _photoState.update { it.copy(error = "Maximum 4 packaging angles can be attached per inspection.") }
            return
        }
        viewModelScope.launch {
            _photoState.update { it.copy(isCompressing = true, error = null) }
            val compressed = ImageCompressionUtil.compressToCacheFile(context, uri)
            compressed.fold(
                onSuccess = { outUri ->
                    _photoState.update {
                        it.copy(
                            isCompressing = false,
                            capturedUris = it.capturedUris + outUri.toString()
                        )
                    }
                },
                onFailure = { err ->
                    _photoState.update {
                        it.copy(
                            isCompressing = false,
                            error = err.localizedMessage ?: "Failed to process image."
                        )
                    }
                }
            )
        }
    }

    fun removePhotoAt(index: Int) {
        _photoState.update { state ->
            val updated = state.capturedUris.toMutableList()
            if (index in updated.indices) updated.removeAt(index)
            state.copy(capturedUris = updated, error = null)
        }
    }

    fun updatePhotoNotes(notes: String) {
        _photoState.update { it.copy(notes = notes.take(300)) }
    }

    fun clearPhotos() {
        _photoState.value = PhotoInspectionUiState()
    }
}
