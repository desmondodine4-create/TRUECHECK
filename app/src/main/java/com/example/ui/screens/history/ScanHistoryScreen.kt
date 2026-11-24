package com.example.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.VerificationStatus
import com.example.ui.screens.auth.trueCheckFieldColors
import com.example.ui.screens.home.ConsumerBottomBar
import com.example.ui.screens.home.ConsumerTab
import com.example.ui.screens.home.RecentScanCard
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MainText
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftMint

@Composable
fun ScanHistoryScreen(
    historyViewModel: HistoryViewModel,
    onOpenReport: (String) -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    val state by historyViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = PureWhite,
        bottomBar = {
            ConsumerBottomBar(
                selected = ConsumerTab.HISTORY,
                onHome = onNavigateHome,
                onHistory = {},
                onProfile = onNavigateProfile
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text("Scan History", style = MaterialTheme.typography.headlineMedium, color = MainText)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Review past product authentications and safety reports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { historyViewModel.setSearchQuery(it) },
                    placeholder = { Text("Search product, brand, or code") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true,
                    colors = trueCheckFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                val filters: List<Pair<String, VerificationStatus?>> = listOf(
                    "All" to null,
                    "REAL" to VerificationStatus.REAL,
                    "SUSPICIOUS" to VerificationStatus.SUSPICIOUS,
                    "FAKE" to VerificationStatus.FAKE,
                    "UNVERIFIED" to VerificationStatus.UNVERIFIED
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filters) { (label, status) ->
                        val selected = state.selectedFilter == status
                        FilterChip(
                            selected = selected,
                            onClick = { historyViewModel.setFilter(status) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftMint,
                                selectedLabelColor = DeepTeal
                            ),
                            modifier = Modifier.testTag("history_filter_$label")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (state.scans.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = DeepTeal,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No matching scans",
                                style = MaterialTheme.typography.titleMedium,
                                color = MainText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your completed barcode, QR, security code, and photo inspections will be saved privately to your account.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.scans, key = { it.id }) { scan ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RecentScanCard(
                                    scan = scan,
                                    onClick = { onOpenReport(scan.id) },
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { historyViewModel.deleteScan(scan.id) },
                                    modifier = Modifier.testTag("delete_scan_${scan.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete scan record",
                                        tint = ErrorRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
