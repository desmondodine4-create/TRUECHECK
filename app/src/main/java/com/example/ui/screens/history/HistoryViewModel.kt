package com.example.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ScanReportItem
import com.example.data.model.VerificationStatus
import com.example.data.repository.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val scans: List<ScanReportItem> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: VerificationStatus? = null
)

class HistoryViewModel(
    private val scanRepository: ScanRepository,
    private val userId: String
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow<VerificationStatus?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        scanRepository.observeAllScans(userId),
        searchQuery,
        selectedFilter
    ) { allScans, query, filter ->
        val filtered = allScans.filter { item ->
            val matchesFilter = filter == null || item.status == filter
            val matchesQuery = query.isBlank() ||
                item.productName.contains(query, ignoreCase = true) ||
                item.brand.contains(query, ignoreCase = true) ||
                item.rawIdentifier.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
        HistoryUiState(
            scans = filtered,
            searchQuery = query,
            selectedFilter = filter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HistoryUiState()
    )

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setFilter(status: VerificationStatus?) {
        selectedFilter.value = status
    }

    fun deleteScan(scanId: String) {
        viewModelScope.launch {
            scanRepository.deleteScan(userId, scanId)
        }
    }
}
