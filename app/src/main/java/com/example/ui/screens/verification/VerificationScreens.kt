package com.example.ui.screens.verification

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.ScanReportItem
import com.example.data.model.VerificationStatus
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.DividerMint
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedSoft
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LightMintBackground
import com.example.ui.theme.MainText
import com.example.ui.theme.NeutralSlate
import com.example.ui.theme.NeutralSlateSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftMint
import com.example.ui.theme.TrueCheckShieldMark
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberSoft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VerificationProcessingScreen(
    userId: String,
    methodName: String,
    rawValue: String,
    formatLabel: String,
    imageUrisJson: String,
    notes: String,
    verificationViewModel: VerificationViewModel,
    onCompleted: (String) -> Unit,
    onCancel: () -> Unit
) {
    val uiState by verificationViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId, methodName, rawValue, imageUrisJson) {
        verificationViewModel.runVerification(
            userId = userId,
            methodName = methodName,
            rawValue = rawValue,
            formatLabel = formatLabel,
            imageUrisJson = imageUrisJson,
            notes = notes,
            onCompleted = onCompleted
        )
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_scale"
    )

    Surface(modifier = Modifier.fillMaxSize(), color = PureWhite) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (uiState is VerificationUiState.Error) {
                val err = (uiState as VerificationUiState.Error).message
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Verification error",
                    tint = ErrorRed,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Verification Interrupted", style = MaterialTheme.typography.headlineSmall, color = MainText)
                Spacer(modifier = Modifier.height(8.dp))
                Text(err, style = MaterialTheme.typography.bodyMedium, color = SecondaryText, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Back to Home")
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(124.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(SoftMint),
                    contentAlignment = Alignment.Center
                ) {
                    TrueCheckShieldMark(size = 68.dp)
                }
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "Verifying Product Evidence",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MainText,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Checking product registries, identifier integrity, batch alerts, and safety records...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        }
    }
}

@Composable
fun VerificationResultScreen(
    userId: String,
    scanId: String,
    verificationViewModel: VerificationViewModel,
    onScanAnother: () -> Unit,
    onViewDetails: (String) -> Unit,
    onBackToHome: () -> Unit
) {
    val uiState by verificationViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId, scanId) {
        verificationViewModel.loadReport(userId, scanId)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = PureWhite) {
        when (val state = uiState) {
            is VerificationUiState.Processing -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    TrueCheckShieldMark(size = 64.dp)
                }
            }
            is VerificationUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(state.message, color = ErrorRed, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onBackToHome,
                        colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
                    ) {
                        Text("Back to Home")
                    }
                }
            }
            is VerificationUiState.Ready -> {
                VerificationResultContent(
                    report = state.report,
                    onScanAnother = onScanAnother,
                    onViewDetails = { onViewDetails(state.report.id) },
                    onBackToHome = onBackToHome
                )
            }
        }
    }
}

@Composable
private fun VerificationResultContent(
    report: ScanReportItem,
    onScanAnother: () -> Unit,
    onViewDetails: () -> Unit,
    onBackToHome: () -> Unit
) {
    val visual = statusVisual(report.status)
    val dateLabel = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault())
        .format(Date(report.timestampUtc))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 600.dp)) {
            // Status Hero Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = visual.bgColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(PureWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = visual.icon,
                            contentDescription = report.status.heading,
                            tint = visual.fgColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = report.status.heading,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = visual.fgColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("result_status_heading")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = report.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MainText,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (report.isPendingOfflineSync) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = WarningAmberSoft),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Captured while offline — queued for later remote verification and not marked as remotely confirmed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MainText,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (!report.evidence.aiNotice.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = LightMintBackground),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = report.evidence.aiNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepTeal,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Product Summary Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, DividerMint),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    if (!report.productImageRef.isNullOrBlank()) {
                        AsyncImage(
                            model = report.productImageRef,
                            contentDescription = report.productName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    ResultDetailRow("Product", report.productName)
                    ResultDetailRow("Brand", report.brand)
                    ResultDetailRow("Category", report.category.displayName)
                    ResultDetailRow("Method", report.method.label)
                    ResultDetailRow("Date & Time", dateLabel)
                    ResultDetailRow("Source", report.verificationSource)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recommended Action Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (report.status == VerificationStatus.FAKE || report.status == VerificationStatus.SUSPICIOUS) {
                        visual.bgColor
                    } else {
                        LightMintBackground
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Recommended Action",
                        style = MaterialTheme.typography.titleSmall,
                        color = MainText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = report.recommendedAction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MainText
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = onViewDetails,
                colors = ButtonDefaults.buttonColors(containerColor = DeepTeal, contentColor = PureWhite),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("result_view_details_button")
            ) {
                Text("View Verification Details", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onScanAnother,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("result_scan_another_button")
                ) {
                    Text("Scan Another", color = DeepTeal)
                }
                OutlinedButton(
                    onClick = onBackToHome,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("result_back_home_button")
                ) {
                    Text("Back to Home", color = MainText)
                }
            }
        }
    }
}

@Composable
fun ProductDetailsScreen(
    userId: String,
    scanId: String,
    verificationViewModel: VerificationViewModel,
    onBack: () -> Unit
) {
    val uiState by verificationViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId, scanId) {
        verificationViewModel.loadReport(userId, scanId)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = PureWhite) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 600.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("details_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MainText)
                    }
                    Text("Verification Report Details", style = MaterialTheme.typography.titleLarge, color = MainText)
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (val state = uiState) {
                    is VerificationUiState.Processing -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            TrueCheckShieldMark(size = 52.dp)
                        }
                    }
                    is VerificationUiState.Error -> {
                        Text(state.message, color = ErrorRed)
                    }
                    is VerificationUiState.Ready -> {
                        val r = state.report
                        val visual = statusVisual(r.status)
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = visual.bgColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(r.status.heading, style = MaterialTheme.typography.titleLarge, color = visual.fgColor)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(r.productName, style = MaterialTheme.typography.titleMedium, color = MainText)
                                Text("${r.brand} • ${r.category.displayName}", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DividerMint),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Identifier & Batch Metadata", style = MaterialTheme.typography.titleSmall, color = MainText)
                                Spacer(modifier = Modifier.height(8.dp))
                                ResultDetailRow("Scanned Value", r.rawIdentifier, mono = true)
                                ResultDetailRow("Method", r.method.label)
                                r.evidence.registrationNumber?.let { ResultDetailRow("Registration No.", it, mono = true) }
                                r.evidence.batchNumber?.let { ResultDetailRow("Batch No.", it, mono = true) }
                                r.evidence.expiryDate?.let { ResultDetailRow("Expiry Date", it, mono = true) }
                                ResultDetailRow("Verification Source", r.verificationSource)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        EvidenceSectionCard(
                            title = "Evidence Checked",
                            items = r.evidence.evidenceChecked,
                            icon = Icons.Default.Shield,
                            tint = DeepTeal
                        )
                        if (r.evidence.evidenceMatched.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            EvidenceSectionCard(
                                title = "Evidence Matched",
                                items = r.evidence.evidenceMatched,
                                icon = Icons.Default.CheckCircle,
                                tint = EmeraldGreen
                            )
                        }
                        if (r.evidence.evidenceFailed.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            EvidenceSectionCard(
                                title = "Failed / Flagged Evidence",
                                items = r.evidence.evidenceFailed,
                                icon = Icons.Default.Warning,
                                tint = ErrorRed
                            )
                        }
                        if (r.evidence.providerLimitations.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            EvidenceSectionCard(
                                title = "Verification Limitations",
                                items = r.evidence.providerLimitations,
                                icon = Icons.Default.Info,
                                tint = NeutralSlate
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EvidenceSectionCard(
    title: String,
    items: List<String>,
    icon: ImageVector,
    tint: Color
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = LightMintBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, color = MainText)
            }
            items.forEach { entry ->
                Text("• $entry", style = MaterialTheme.typography.bodySmall, color = MainText)
            }
        }
    }
}

@Composable
private fun ResultDetailRow(label: String, value: String, mono: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = SecondaryText)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = if (mono) {
                MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily)
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = MainText
        )
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = DividerMint.copy(alpha = 0.6f))
    }
}

private data class StatusVisual(
    val icon: ImageVector,
    val bgColor: Color,
    val fgColor: Color
)

private fun statusVisual(status: VerificationStatus): StatusVisual = when (status) {
    VerificationStatus.REAL -> StatusVisual(Icons.Default.VerifiedUser, SoftMint, DeepTeal)
    VerificationStatus.SUSPICIOUS -> StatusVisual(Icons.Default.Warning, WarningAmberSoft, Color(0xFFB36E0A))
    VerificationStatus.FAKE -> StatusVisual(Icons.Default.Dangerous, ErrorRedSoft, ErrorRed)
    VerificationStatus.UNVERIFIED -> StatusVisual(Icons.Default.HelpOutline, NeutralSlateSoft, NeutralSlate)
}
