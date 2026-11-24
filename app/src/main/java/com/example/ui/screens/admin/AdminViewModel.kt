package com.example.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AccountStatus
import com.example.data.model.AdminDashboardSnapshot
import com.example.data.repository.AdminAnalyticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AdminUiState {
    data object Loading : AdminUiState
    data class Ready(
        val snapshot: AdminDashboardSnapshot,
        val daysWindow: Int = 7,
        val userSearchQuery: String = "",
        val feedbackMessage: String? = null
    ) : AdminUiState
    data class DeniedOrError(val message: String) : AdminUiState
}

class AdminViewModel(
    private val adminAnalyticsRepository: AdminAnalyticsRepository,
    private val actingUserId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminUiState>(AdminUiState.Loading)
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private var currentDaysWindow = 7
    private var currentQuery = ""

    init {
        refresh()
    }

    fun refresh(daysWindow: Int = currentDaysWindow) {
        currentDaysWindow = daysWindow
        viewModelScope.launch {
            val previousFeedback = (_uiState.value as? AdminUiState.Ready)?.feedbackMessage
            if (_uiState.value !is AdminUiState.Ready) {
                _uiState.value = AdminUiState.Loading
            }
            adminAnalyticsRepository.loadSnapshot(actingUserId, daysWindow).fold(
                onSuccess = { snapshot ->
                    _uiState.value = AdminUiState.Ready(
                        snapshot = snapshot,
                        daysWindow = daysWindow,
                        userSearchQuery = currentQuery,
                        feedbackMessage = previousFeedback
                    )
                },
                onFailure = { error ->
                    _uiState.value = AdminUiState.DeniedOrError(
                        error.localizedMessage ?: "Administrator access denied."
                    )
                }
            )
        }
    }

    fun setUserSearchQuery(query: String) {
        currentQuery = query
        _uiState.update { state ->
            if (state is AdminUiState.Ready) state.copy(userSearchQuery = query) else state
        }
    }

    fun toggleUserStatus(targetUserId: String, currentStatus: AccountStatus) {
        val nextStatus = if (currentStatus == AccountStatus.ACTIVE) {
            AccountStatus.SUSPENDED
        } else {
            AccountStatus.ACTIVE
        }
        viewModelScope.launch {
            adminAnalyticsRepository.setUserStatus(actingUserId, targetUserId, nextStatus).fold(
                onSuccess = {
                    _uiState.update { state ->
                        if (state is AdminUiState.Ready) {
                            state.copy(feedbackMessage = "Account status updated to ${nextStatus.name}.")
                        } else state
                    }
                    refresh(currentDaysWindow)
                },
                onFailure = { err ->
                    _uiState.update { state ->
                        if (state is AdminUiState.Ready) {
                            state.copy(feedbackMessage = err.localizedMessage ?: "Update failed.")
                        } else state
                    }
                }
            )
        }
    }

    fun clearFeedback() {
        _uiState.update { state ->
            if (state is AdminUiState.Ready) state.copy(feedbackMessage = null) else state
        }
    }
}
