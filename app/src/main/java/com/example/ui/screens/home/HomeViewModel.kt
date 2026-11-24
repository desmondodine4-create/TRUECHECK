package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.NotificationItem
import com.example.data.model.ScanReportItem
import com.example.data.repository.ScanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val scanRepository: ScanRepository,
    private val userId: String
) : ViewModel() {

    val recentScans: StateFlow<List<ScanReportItem>> =
        scanRepository.observeRecentScans(userId, limit = 5)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<NotificationItem>> =
        scanRepository.observeNotifications(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val unreadCount: StateFlow<Int> =
        scanRepository.observeUnreadNotificationCount(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    fun markNotificationsRead() {
        viewModelScope.launch {
            scanRepository.markAllNotificationsRead(userId)
        }
    }
}
