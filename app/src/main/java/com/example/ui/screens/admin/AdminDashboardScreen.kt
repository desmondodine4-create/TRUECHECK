package com.example.ui.screens.admin

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AccountStatus
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.screens.auth.trueCheckFieldColors
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.DividerMint
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedSoft
import com.example.ui.theme.LightMintBackground
import com.example.ui.theme.MainText
import com.example.ui.theme.NeutralSlate
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftMint
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AdminTab(val label: String) {
    OVERVIEW("Overview"),
    ANALYTICS("Charts"),
    USERS("Users")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    actingUser: UserProfile,
    adminViewModel: AdminViewModel,
    onExitToConsumer: () -> Unit,
    onLogout: () -> Unit
) {
    // Hard UI-level guard in addition to Repository-level role verification
    if (actingUser.role != UserRole.ADMIN) {
        Surface(modifier = Modifier.fillMaxSize(), color = PureWhite) {
            Column(
                modifier = Modifier.fillMaxSize().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Access Restricted", style = MaterialTheme.typography.headlineSmall, color = ErrorRed)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Administrator privileges are controlled server-side and are not enabled for this account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(onClick = onExitToConsumer, colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)) {
                    Text("Return")
                }
            }
        }
        return
    }

    val uiState by adminViewModel.uiState.collectAsStateWithLifecycle()
    var activeTab by remember { mutableStateOf(AdminTab.OVERVIEW) }

    Scaffold(
        containerColor = LightMintBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkGreen,
                    titleContentColor = PureWhite,
                    navigationIconContentColor = PureWhite,
                    actionIconContentColor = PureWhite
                ),
                navigationIcon = {
                    IconButton(onClick = onExitToConsumer, modifier = Modifier.testTag("admin_exit_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to app")
                    }
                },
                title = {
                    Column {
                        Text("TRUECHECK Admin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Private Governance Console (UTC)", style = MaterialTheme.typography.bodySmall, color = SoftMint)
                    }
                },
                actions = {
                    IconButton(onClick = { adminViewModel.refresh() }, modifier = Modifier.testTag("admin_refresh_button")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh analytics")
                    }
                    IconButton(onClick = onLogout, modifier = Modifier.testTag("admin_logout_button")) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = PureWhite) {
                NavigationBarItem(
                    selected = activeTab == AdminTab.OVERVIEW,
                    onClick = { activeTab = AdminTab.OVERVIEW },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Overview") },
                    label = { Text(AdminTab.OVERVIEW.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepTeal,
                        selectedTextColor = DeepTeal,
                        indicatorColor = SoftMint
                    )
                )
                NavigationBarItem(
                    selected = activeTab == AdminTab.ANALYTICS,
                    onClick = { activeTab = AdminTab.ANALYTICS },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Charts") },
                    label = { Text(AdminTab.ANALYTICS.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepTeal,
                        selectedTextColor = DeepTeal,
                        indicatorColor = SoftMint
                    )
                )
                NavigationBarItem(
                    selected = activeTab == AdminTab.USERS,
                    onClick = { activeTab = AdminTab.USERS },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Users") },
                    label = { Text(AdminTab.USERS.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepTeal,
                        selectedTextColor = DeepTeal,
                        indicatorColor = SoftMint
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val state = uiState) {
                is AdminUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DeepTeal)
                    }
                }
                is AdminUiState.DeniedOrError -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(state.message, color = ErrorRed)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onExitToConsumer, colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)) {
                            Text("Exit Admin Console")
                        }
                    }
                }
                is AdminUiState.Ready -> {
                    val snap = state.snapshot
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp),
                        contentPadding = PaddingValues(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Date Filter Row
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Window:", style = MaterialTheme.typography.labelLarge, color = MainText)
                                FilterChip(
                                    selected = state.daysWindow == 7,
                                    onClick = { adminViewModel.refresh(7) },
                                    label = { Text("Last 7 Days") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftMint, selectedLabelColor = DeepTeal)
                                )
                                FilterChip(
                                    selected = state.daysWindow == 30,
                                    onClick = { adminViewModel.refresh(30) },
                                    label = { Text("Last 30 Days") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftMint, selectedLabelColor = DeepTeal)
                                )
                            }
                        }

                        if (state.feedbackMessage != null) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SoftMint),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = state.feedbackMessage,
                                        color = DeepTeal,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }

                        when (activeTab) {
                            AdminTab.OVERVIEW -> {
                                item {
                                    Text("Key Performance Indicators", style = MaterialTheme.typography.titleMedium, color = MainText)
                                }
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            KpiCard("TOTAL REGISTERED USERS", snap.totalUsers.toString(), DeepTeal, Modifier.weight(1f))
                                            KpiCard("DAILY ACTIVE USERS", snap.dailyActiveUsers.toString(), EmeraldGreen, Modifier.weight(1f))
                                            KpiCard("MONTHLY ACTIVE USERS", snap.monthlyActiveUsers.toString(), DarkGreen, Modifier.weight(1f))
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            KpiCard("TOTAL PRODUCT SCANS", snap.totalScans.toString(), DeepTeal, Modifier.weight(1f))
                                            KpiCard("SCANS TODAY", snap.scansToday.toString(), EmeraldGreen, Modifier.weight(1f))
                                            KpiCard("REAL PRODUCTS DETECTED", snap.realCount.toString(), EmeraldGreen, Modifier.weight(1f))
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            KpiCard("SUSPICIOUS PRODUCTS", snap.suspiciousCount.toString(), WarningAmber, Modifier.weight(1f))
                                            KpiCard("FAKE PRODUCTS DETECTED", snap.fakeCount.toString(), ErrorRed, Modifier.weight(1f))
                                            KpiCard("UNVERIFIED PRODUCTS", snap.unverifiedCount.toString(), NeutralSlate, Modifier.weight(1f))
                                        }
                                    }
                                }

                                item {
                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DividerMint),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Analytics, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("User & Usage Summary", style = MaterialTheme.typography.titleSmall, color = MainText)
                                            }
                                            SummaryLine("New Registrations (Today / 7d / 30d)", "${snap.newUsersToday} / ${snap.newUsers7Days} / ${snap.newUsers30Days}")
                                            SummaryLine("Product Scans (Today / 7d / 30d)", "${snap.scansToday} / ${snap.scans7Days} / ${snap.scans30Days}")
                                            SummaryLine("Top Scanning Method", snap.topScanMethod)
                                            SummaryLine("Most Scanned Category", snap.topCategory)
                                        }
                                    }
                                }

                                item {
                                    OutcomeDistributionCard(
                                        realCount = snap.realCount,
                                        suspiciousCount = snap.suspiciousCount,
                                        fakeCount = snap.fakeCount,
                                        unverifiedCount = snap.unverifiedCount
                                    )
                                }
                            }

                            AdminTab.ANALYTICS -> {
                                item {
                                    MetricBarChartCard(
                                        title = "Daily Registrations",
                                        subtitle = "New accounts created per UTC day",
                                        points = snap.dailyRegistrationsSeries,
                                        barColor = DeepTeal
                                    )
                                }
                                item {
                                    MetricBarChartCard(
                                        title = "Daily Active Users (DAU)",
                                        subtitle = "Unique authenticated users with qualifying activity per UTC day",
                                        points = snap.dailyActiveUsersSeries,
                                        barColor = EmeraldGreen
                                    )
                                }
                                item {
                                    MetricBarChartCard(
                                        title = "Daily Product Scans",
                                        subtitle = "Verification scans submitted per UTC day",
                                        points = snap.dailyScansSeries,
                                        barColor = DarkGreen
                                    )
                                }
                                item {
                                    GrowthLineChartCard(
                                        title = "Weekly User Growth",
                                        subtitle = "Cumulative registered accounts over recent weeks",
                                        points = snap.weeklyGrowthSeries
                                    )
                                }
                                item {
                                    GrowthLineChartCard(
                                        title = "Monthly User Growth",
                                        subtitle = "Cumulative registered accounts over recent months",
                                        points = snap.monthlyGrowthSeries
                                    )
                                }
                                item {
                                    OutcomeDistributionCard(
                                        realCount = snap.realCount,
                                        suspiciousCount = snap.suspiciousCount,
                                        fakeCount = snap.fakeCount,
                                        unverifiedCount = snap.unverifiedCount
                                    )
                                }
                            }

                            AdminTab.USERS -> {
                                item {
                                    OutlinedTextField(
                                        value = state.userSearchQuery,
                                        onValueChange = { adminViewModel.setUserSearchQuery(it) },
                                        placeholder = { Text("Search users by name or email") },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search users") },
                                        singleLine = true,
                                        colors = trueCheckFieldColors(),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("admin_user_search")
                                    )
                                }

                                val filteredUsers = snap.users.filter { u ->
                                    state.userSearchQuery.isBlank() ||
                                        u.fullName.contains(state.userSearchQuery, ignoreCase = true) ||
                                        u.email.contains(state.userSearchQuery, ignoreCase = true)
                                }

                                items(filteredUsers, key = { it.id }) { profile ->
                                    AdminUserRowCard(
                                        user = profile,
                                        isSelf = profile.id == actingUser.id,
                                        onToggleStatus = {
                                            adminViewModel.toggleUserStatus(profile.id, profile.status)
                                        }
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

@Composable
private fun KpiCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerMint),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MainText)
    }
}

@Composable
private fun AdminUserRowCard(
    user: UserProfile,
    isSelf: Boolean,
    onToggleStatus: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val created = dateFmt.format(Date(user.createdAtUtc))
    val lastSeen = dateFmt.format(Date(user.lastActiveAtUtc))
    val isSuspended = user.status == AccountStatus.SUSPENDED

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerMint),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.fullName, style = MaterialTheme.typography.titleSmall, color = MainText)
                    Text(user.email, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSuspended) ErrorRedSoft else SoftMint
                ) {
                    Text(
                        text = "${user.role.name} • ${user.status.name}",
                        color = if (isSuspended) ErrorRed else DeepTeal,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Created: $created", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                Text("Last active: $lastSeen", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                Text("Scans: ${user.scanCount}", style = MaterialTheme.typography.labelSmall, color = DeepTeal)
            }
            if (!isSelf) {
                OutlinedButton(
                    onClick = onToggleStatus,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text(
                        text = if (isSuspended) "Reactivate Account" else "Suspend Account",
                        color = if (isSuspended) DeepTeal else ErrorRed,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
