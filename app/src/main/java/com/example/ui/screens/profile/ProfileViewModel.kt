package com.example.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesManager
import com.example.data.model.NotificationPreferences
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileActionState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val preferencesManager: PreferencesManager,
    private val userId: String
) : ViewModel() {

    val notificationPrefs: StateFlow<NotificationPreferences> =
        preferencesManager.notificationPreferences
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), NotificationPreferences())

    private val _actionState = MutableStateFlow(ProfileActionState())
    val actionState: StateFlow<ProfileActionState> = _actionState.asStateFlow()

    fun clearMessage() {
        _actionState.update { it.copy(message = null, isError = false) }
    }

    fun updateFullName(newName: String) {
        viewModelScope.launch {
            _actionState.update { it.copy(isLoading = true, message = null) }
            authRepository.updateProfile(userId, newName).fold(
                onSuccess = {
                    _actionState.value = ProfileActionState(message = "Profile updated.")
                },
                onFailure = { e ->
                    _actionState.value = ProfileActionState(
                        message = e.localizedMessage ?: "Failed to update profile.",
                        isError = true
                    )
                }
            )
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _actionState.update { it.copy(isLoading = true, message = null) }
            authRepository.changePassword(userId, currentPassword, newPassword).fold(
                onSuccess = {
                    _actionState.value = ProfileActionState(message = "Password changed successfully.")
                },
                onFailure = { e ->
                    _actionState.value = ProfileActionState(
                        message = e.localizedMessage ?: "Failed to change password.",
                        isError = true
                    )
                }
            )
        }
    }

    fun updateNotificationPrefs(updated: NotificationPreferences) {
        viewModelScope.launch {
            preferencesManager.updateNotificationPreferences(updated)
        }
    }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _actionState.update { it.copy(isLoading = true, message = null) }
            authRepository.deleteAccount(userId).fold(
                onSuccess = {
                    _actionState.value = ProfileActionState()
                    onDeleted()
                },
                onFailure = { e ->
                    _actionState.value = ProfileActionState(
                        message = e.localizedMessage ?: "Cannot delete account.",
                        isError = true
                    )
                }
            )
        }
    }
}
