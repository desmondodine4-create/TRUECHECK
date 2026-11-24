package com.example.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesManager
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthFormState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val currentUser: StateFlow<UserProfile?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val onboardingCompleted: StateFlow<Boolean> = preferencesManager.onboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    private val _formState = MutableStateFlow(AuthFormState())
    val formState: StateFlow<AuthFormState> = _formState.asStateFlow()

    fun clearMessages() {
        _formState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesManager.setOnboardingCompleted(true)
        }
    }

    fun login(email: String, password: String, onSuccess: (UserProfile) -> Unit) {
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val result = authRepository.login(email, password)
            result.fold(
                onSuccess = { profile ->
                    _formState.update { it.copy(isLoading = false) }
                    onSuccess(profile)
                },
                onFailure = { error ->
                    _formState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Sign in failed."
                        )
                    }
                }
            )
        }
    }

    fun register(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        onSuccess: (UserProfile) -> Unit
    ) {
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val result = authRepository.register(fullName, email, password, confirmPassword)
            result.fold(
                onSuccess = { profile ->
                    _formState.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = "Account created for ${profile.email}."
                        )
                    }
                    onSuccess(profile)
                },
                onFailure = { error ->
                    _formState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Registration failed."
                        )
                    }
                }
            )
        }
    }

    fun resetPassword(
        email: String,
        newPassword: String,
        confirmPassword: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val result = authRepository.resetPassword(email, newPassword, confirmPassword)
            result.fold(
                onSuccess = {
                    _formState.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = "Password updated. You can now sign in with your new password."
                        )
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    _formState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Password reset failed."
                        )
                    }
                }
            )
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }
}
