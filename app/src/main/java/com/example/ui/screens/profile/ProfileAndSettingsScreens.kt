package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ProviderConnectionStatus
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.domain.verification.ExternalVerificationProviders
import com.example.ui.screens.home.ConsumerBottomBar
import com.example.ui.screens.home.ConsumerTab
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.DividerMint
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedSoft
import com.example.ui.theme.LightMintBackground
import com.example.ui.theme.MainText
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftMint
import com.example.ui.theme.WarningAmberSoft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    user: UserProfile,
    onNavigateHome: () -> Unit,
    onNavigateHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onLogout: () -> Unit
) {
    val initials = user.fullName.trim()
        .split(Regex("\\s+"))
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "TC" }

    val joinedDate = remember(user.createdAtUtc) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(user.createdAtUtc))
    }

    Scaffold(
        containerColor = PureWhite,
        bottomBar = {
            ConsumerBottomBar(
                selected = ConsumerTab.PROFILE,
                onHome = onNavigateHome,
                onHistory = onNavigateHistory,
                onProfile = {}
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
                    .widthIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Profile", style = MaterialTheme.typography.headlineMedium, color = MainText)

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = LightMintBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(DeepTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleLarge,
                                color = PureWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.fullName, style = MaterialTheme.typography.titleLarge, color = MainText)
                            Text(user.email, style = MaterialTheme.typography.bodyMedium, color = SecondaryText)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Member since $joinedDate",
                                style = MaterialTheme.typography.bodySmall,
                                color = DeepTeal
                            )
                        }
                    }
                }

                // Private Administrator Console is ONLY displayed if the authenticated server/DB role is ADMIN.
                if (user.role == UserRole.ADMIN) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftMint),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onOpenAdminDashboard)
                            .testTag("open_admin_dashboard_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = DeepTeal)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Private Administrator Dashboard", style = MaterialTheme.typography.titleSmall, color = MainText)
                                Text("Analytics, DAU/MAU & user governance", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = DeepTeal)
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DividerMint),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ProfileMenuRow(
                            icon = Icons.Default.Settings,
                            title = "Account & Notification Settings",
                            subtitle = "Edit profile, password, privacy & preferences",
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("profile_settings_button")
                        )
                        HorizontalDivider(color = DividerMint)
                        ProfileMenuRow(
                            icon = Icons.AutoMirrored.Filled.HelpOutline,
                            title = "Help & Support",
                            subtitle = "Verification guide, provider status & reporting",
                            onClick = onOpenHelp,
                            modifier = Modifier.testTag("profile_help_button")
                        )
                    }
                }

                OutlinedButton(
                    onClick = onLogout,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("profile_logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = ErrorRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    user: UserProfile,
    profileViewModel: ProfileViewModel,
    onBack: () -> Unit,
    onOpenHelp: () -> Unit,
    onLogout: () -> Unit
) {
    val actionState by profileViewModel.actionState.collectAsStateWithLifecycle()
    val notifPrefs by profileViewModel.notificationPrefs.collectAsStateWithLifecycle()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var legalDialogTitle by remember { mutableStateOf<String?>(null) }
    var legalDialogBody by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = PureWhite) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 600.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MainText)
                    }
                    Text("Settings", style = MaterialTheme.typography.titleLarge, color = MainText)
                }

                if (actionState.message != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (actionState.isError) ErrorRedSoft else SoftMint
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = actionState.message!!,
                            color = if (actionState.isError) ErrorRed else DeepTeal,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Text("Account", style = MaterialTheme.typography.titleSmall, color = SecondaryText)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DividerMint),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ProfileMenuRow(
                            icon = Icons.Default.Edit,
                            title = "Edit Profile",
                            subtitle = user.fullName,
                            onClick = { showEditNameDialog = true; profileViewModel.clearMessage() }
                        )
                        HorizontalDivider(color = DividerMint)
                        ProfileMenuRow(
                            icon = Icons.Default.Lock,
                            title = "Change Password",
                            subtitle = "Minimum 8 characters",
                            onClick = { showChangePasswordDialog = true; profileViewModel.clearMessage() }
                        )
                    }
                }

                Text("Notification Preferences", style = MaterialTheme.typography.titleSmall, color = SecondaryText)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DividerMint),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        NotificationToggleRow(
                            label = "Verification completed",
                            checked = notifPrefs.verificationCompleted,
                            onCheckedChange = {
                                profileViewModel.updateNotificationPrefs(notifPrefs.copy(verificationCompleted = it))
                            }
                        )
                        HorizontalDivider(color = DividerMint)
                        NotificationToggleRow(
                            label = "Suspicious product warnings",
                            checked = notifPrefs.suspiciousAlerts,
                            onCheckedChange = {
                                profileViewModel.updateNotificationPrefs(notifPrefs.copy(suspiciousAlerts = it))
                            }
                        )
                        HorizontalDivider(color = DividerMint)
                        NotificationToggleRow(
                            label = "Security code already used",
                            checked = notifPrefs.codeReuseAlerts,
                            onCheckedChange = {
                                profileViewModel.updateNotificationPrefs(notifPrefs.copy(codeReuseAlerts = it))
                            }
                        )
                        HorizontalDivider(color = DividerMint)
                        NotificationToggleRow(
                            label = "Official recalls & safety alerts",
                            checked = notifPrefs.safetyRecalls,
                            onCheckedChange = {
                                profileViewModel.updateNotificationPrefs(notifPrefs.copy(safetyRecalls = it))
                            }
                        )
                    }
                }

                Text("Legal & Support", style = MaterialTheme.typography.titleSmall, color = SecondaryText)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DividerMint),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ProfileMenuRow(
                            icon = Icons.Default.PrivacyTip,
                            title = "Privacy Policy",
                            subtitle = "How your scan history and photos are handled",
                            onClick = {
                                legalDialogTitle = "TRUECHECK Privacy Policy"
                                legalDialogBody = "TRUECHECK stores your scan history and account profile privately for your account only. Packaging photos are compressed prior to inspection. Passwords are stored using salted cryptographic hashes, and consumer accounts never gain access to other users' scan records."
                            }
                        )
                        HorizontalDivider(color = DividerMint)
                        ProfileMenuRow(
                            icon = Icons.Default.Description,
                            title = "Terms & Conditions",
                            subtitle = "Verification scope & consumer safety disclaimer",
                            onClick = {
                                legalDialogTitle = "Terms & Conditions"
                                legalDialogBody = "TRUECHECK aggregates product identifiers, regulatory references, and visual packaging cues to support consumer decision-making. Visual or unverified scans do not replace laboratory testing or licensed pharmacist guidance for medicines."
                            }
                        )
                        HorizontalDivider(color = DividerMint)
                        ProfileMenuRow(
                            icon = Icons.AutoMirrored.Filled.HelpOutline,
                            title = "Help and Support",
                            subtitle = "FAQs and counterfeit reporting contacts",
                            onClick = onOpenHelp
                        )
                    }
                }

                if (user.role != UserRole.ADMIN) {
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = ErrorRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Account", color = ErrorRed)
                    }
                }

                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal, contentColor = PureWhite),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Sign Out")
                }
            }
        }
    }

    if (showEditNameDialog) {
        var nameDraft by remember { mutableStateOf(user.fullName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = PureWhite,
            title = { Text("Edit Profile", color = MainText) },
            text = {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    label = { Text("Full name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        profileViewModel.updateFullName(nameDraft)
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showChangePasswordDialog) {
        var curPw by remember { mutableStateOf("") }
        var newPw by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            containerColor = PureWhite,
            title = { Text("Change Password", color = MainText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = curPw,
                        onValueChange = { curPw = it },
                        label = { Text("Current password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPw,
                        onValueChange = { newPw = it },
                        label = { Text("New password (8+ chars)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        profileViewModel.changePassword(curPw, newPw)
                        showChangePasswordDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
                ) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = PureWhite,
            title = { Text("Delete Account?", color = ErrorRed) },
            text = {
                Text("This permanently deletes your profile and private scan history from this device.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        profileViewModel.deleteAccount(onLogout)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (legalDialogTitle != null) {
        AlertDialog(
            onDismissRequest = { legalDialogTitle = null },
            containerColor = PureWhite,
            title = { Text(legalDialogTitle!!, color = MainText) },
            text = { Text(legalDialogBody, style = MaterialTheme.typography.bodyMedium, color = SecondaryText) },
            confirmButton = {
                TextButton(onClick = { legalDialogTitle = null }) {
                    Text("Close", color = DeepTeal)
                }
            }
        )
    }
}

@Composable
fun HelpSupportScreen(
    onBack: () -> Unit
) {
    val providers = remember { ExternalVerificationProviders.getProviderStatuses() }

    Surface(modifier = Modifier.fillMaxSize(), color = PureWhite) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 600.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MainText)
                    }
                    Text("Help & Support", style = MaterialTheme.typography.titleLarge, color = MainText)
                }

                Text("Understanding Verification Outcomes", style = MaterialTheme.typography.titleMedium, color = MainText)
                HelpInfoCard("PRODUCT VERIFIED (REAL)", "Trusted registry or unique first-time security code evidence matched this product.")
                HelpInfoCard("SUSPICIOUS PRODUCT", "Conflicting batch details, suspicious URLs, or a previously redeemed security code were detected.")
                HelpInfoCard("POSSIBLE COUNTERFEIT (FAKE)", "Positive safety-registry evidence flagged this lot or security code as counterfeit or revoked.")
                HelpInfoCard("UNABLE TO VERIFY (UNVERIFIED)", "Insufficient registry evidence exists. Missing from a database does not automatically mean counterfeit.")

                Spacer(modifier = Modifier.height(6.dp))
                Text("External Verification Providers", style = MaterialTheme.typography.titleMedium, color = MainText)
                providers.forEach { provider ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (provider.status == ProviderConnectionStatus.DEMO_MODE) {
                                LightMintBackground
                            } else {
                                WarningAmberSoft
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(provider.name, style = MaterialTheme.typography.titleSmall, color = MainText, modifier = Modifier.weight(1f))
                                Text(provider.status.label, style = MaterialTheme.typography.labelMedium, color = DeepTeal)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(provider.detail, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRedSoft),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Reporting Counterfeit Medicines", style = MaterialTheme.typography.titleSmall, color = ErrorRed)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Never consume suspicious or counterfeit medicines. Consult a licensed pharmacist immediately and report the batch number and point of sale to NAFDAC Pharmacovigilance or your national regulatory authority.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MainText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpInfoCard(title: String, body: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightMintBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = DeepTeal)
            Spacer(modifier = Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = MainText)
        }
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MainText)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SecondaryText)
    }
}

@Composable
private fun NotificationToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MainText)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PureWhite,
                checkedTrackColor = EmeraldGreen
            )
        )
    }
}
