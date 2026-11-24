package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.NotificationItem
import com.example.data.model.ProductCategory
import com.example.data.model.ScanReportItem
import com.example.data.model.UserProfile
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.DividerMint
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LightMintBackground
import com.example.ui.theme.MainText
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftMint
import com.example.ui.theme.TrueCheckWordmark
import com.example.ui.theme.VerificationStatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ConsumerTab { HOME, HISTORY, PROFILE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    user: UserProfile,
    homeViewModel: HomeViewModel,
    onScanBarcode: () -> Unit,
    onScanQrCode: () -> Unit,
    onEnterSecurityCode: () -> Unit,
    onScanProductPhoto: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenReport: (String) -> Unit
) {
    val recentScans by homeViewModel.recentScans.collectAsStateWithLifecycle()
    val notifications by homeViewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by homeViewModel.unreadCount.collectAsStateWithLifecycle()
    var showNotificationsSheet by remember { mutableStateOf(false) }

    val firstName = user.fullName.trim().substringBefore(" ").ifBlank { "there" }

    Scaffold(
        containerColor = PureWhite,
        bottomBar = {
            ConsumerBottomBar(
                selected = ConsumerTab.HOME,
                onHome = {},
                onHistory = onOpenHistory,
                onProfile = onOpenProfile
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            TrueCheckWordmark(shieldSize = 28.dp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Hi, $firstName",
                                style = MaterialTheme.typography.titleMedium,
                                color = MainText
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    showNotificationsSheet = true
                                    homeViewModel.markNotificationsRead()
                                },
                                modifier = Modifier.testTag("home_notifications_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadCount > 0) {
                                            Badge(containerColor = DeepTeal) {
                                                Text(unreadCount.coerceAtMost(9).toString())
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                        tint = MainText
                                    )
                                }
                            }
                            IconButton(
                                onClick = onOpenProfile,
                                modifier = Modifier.testTag("home_profile_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(SoftMint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile",
                                        tint = DeepTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Hero Verification Card
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = LightMintBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(LightMintBackground, SoftMint)
                                    )
                                )
                                .padding(22.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Verify Your Product",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = DarkGreen
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Scan or photograph a product to check its authenticity.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MainText.copy(alpha = 0.85f)
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = onScanBarcode,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DeepTeal,
                                        contentColor = PureWhite
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("home_scan_product_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "SCAN PRODUCT",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 4 Scanning Options Grid (2x2)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ScanOptionTile(
                                title = "Scan Barcode",
                                subtitle = "EAN, UPC, Code 128",
                                icon = Icons.Default.ViewWeek,
                                onClick = onScanBarcode,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("option_scan_barcode")
                            )
                            ScanOptionTile(
                                title = "Scan QR Code",
                                subtitle = "Links & Digital IDs",
                                icon = Icons.Default.QrCodeScanner,
                                onClick = onScanQrCode,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("option_scan_qr")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ScanOptionTile(
                                title = "Enter Security Code",
                                subtitle = "PIN & MAS scratch code",
                                icon = Icons.Default.Pin,
                                onClick = onEnterSecurityCode,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("option_security_code")
                            )
                            ScanOptionTile(
                                title = "Scan Product Photo",
                                subtitle = "Packaging inspection",
                                icon = Icons.Default.CameraAlt,
                                onClick = onScanProductPhoto,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("option_product_photo")
                            )
                        }
                    }
                }

                // Supported Categories
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Supported Categories",
                            style = MaterialTheme.typography.titleMedium,
                            color = MainText
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CategoryChipCard(
                                label = "Medicines & Pharmaceuticals",
                                icon = Icons.Default.LocalPharmacy,
                                modifier = Modifier.weight(1f)
                            )
                            CategoryChipCard(
                                label = "Food & Beverages",
                                icon = Icons.Default.Restaurant,
                                modifier = Modifier.weight(1f)
                            )
                            CategoryChipCard(
                                label = "Beauty & Personal Care",
                                icon = Icons.Default.Spa,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Recent Scans Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Scans",
                            style = MaterialTheme.typography.titleMedium,
                            color = MainText
                        )
                        TextButton(
                            onClick = onOpenHistory,
                            modifier = Modifier.testTag("home_view_all_history")
                        ) {
                            Text("View All", color = DeepTeal, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (recentScans.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = LightMintBackground),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = DeepTeal,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No scans yet",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MainText
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Scan a barcode, QR code, security PIN, or product photo to see your recent verifications here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryText
                                )
                            }
                        }
                    }
                } else {
                    items(recentScans, key = { it.id }) { scan ->
                        RecentScanCard(
                            scan = scan,
                            onClick = { onOpenReport(scan.id) }
                        )
                    }
                }
            }
        }
    }

    if (showNotificationsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationsSheet = false },
            containerColor = PureWhite
        ) {
            NotificationsSheetContent(
                notifications = notifications,
                onDismiss = { showNotificationsSheet = false }
            )
        }
    }
}

@Composable
private fun ScanOptionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerMint),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SoftMint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = DeepTeal,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MainText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryChipCard(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = LightMintBackground,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = DeepTeal,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MainText,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RecentScanCard(
    scan: ScanReportItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateLabel = remember(scan.timestampUtc) {
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
            .format(Date(scan.timestampUtc))
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerMint),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(LightMintBackground),
                contentAlignment = Alignment.Center
            ) {
                if (!scan.productImageRef.isNullOrBlank()) {
                    AsyncImage(
                        model = scan.productImageRef,
                        contentDescription = scan.productName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val catIcon = when (scan.category) {
                        ProductCategory.MEDICINES -> Icons.Default.LocalPharmacy
                        ProductCategory.FOOD_BEVERAGES -> Icons.Default.Restaurant
                        ProductCategory.BEAUTY_PERSONAL_CARE -> Icons.Default.Spa
                        ProductCategory.OTHER -> Icons.Default.QrCodeScanner
                    }
                    Icon(catIcon, contentDescription = scan.category.displayName, tint = DeepTeal)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scan.productName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MainText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${scan.method.label} • $dateLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            VerificationStatusBadge(status = scan.status)
        }
    }
}

@Composable
private fun NotificationsSheetContent(
    notifications: List<NotificationItem>,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Safety & Verification Alerts", style = MaterialTheme.typography.titleLarge, color = MainText)
            TextButton(onClick = onDismiss) {
                Text("Close", color = DeepTeal)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (notifications.isEmpty()) {
            Text(
                text = "No verification alerts yet. Safety warnings and completed scan notifications will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            notifications.take(15).forEach { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = LightMintBackground),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall, color = MainText)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(item.message, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ConsumerBottomBar(
    selected: ConsumerTab,
    onHome: () -> Unit,
    onHistory: () -> Unit,
    onProfile: () -> Unit
) {
    NavigationBar(
        containerColor = PureWhite,
        tonalElevation = 4.dp
    ) {
        NavigationBarItem(
            selected = selected == ConsumerTab.HOME,
            onClick = onHome,
            icon = {
                Icon(
                    if (selected == ConsumerTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepTeal,
                selectedTextColor = DeepTeal,
                indicatorColor = SoftMint,
                unselectedIconColor = SecondaryText,
                unselectedTextColor = SecondaryText
            ),
            modifier = Modifier.testTag("nav_home")
        )
        NavigationBarItem(
            selected = selected == ConsumerTab.HISTORY,
            onClick = onHistory,
            icon = {
                Icon(
                    if (selected == ConsumerTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                    contentDescription = "History"
                )
            },
            label = { Text("History") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepTeal,
                selectedTextColor = DeepTeal,
                indicatorColor = SoftMint,
                unselectedIconColor = SecondaryText,
                unselectedTextColor = SecondaryText
            ),
            modifier = Modifier.testTag("nav_history")
        )
        NavigationBarItem(
            selected = selected == ConsumerTab.PROFILE,
            onClick = onProfile,
            icon = {
                Icon(
                    if (selected == ConsumerTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = "Profile"
                )
            },
            label = { Text("Profile") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DeepTeal,
                selectedTextColor = DeepTeal,
                indicatorColor = SoftMint,
                unselectedIconColor = SecondaryText,
                unselectedTextColor = SecondaryText
            ),
            modifier = Modifier.testTag("nav_profile")
        )
    }
}
