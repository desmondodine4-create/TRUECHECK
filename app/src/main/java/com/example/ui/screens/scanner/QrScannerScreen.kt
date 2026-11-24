package com.example.ui.screens.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LightMintBackground
import com.example.ui.theme.MainText
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftMint
import com.google.mlkit.vision.barcode.common.Barcode

@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    onQrDetected: (rawValue: String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var torchEnabled by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var showManualQrDialog by remember { mutableStateOf(false) }
    var manualQrInput by remember { mutableStateOf("") }
    var manualQrError by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0E1F1B)) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasCameraPermission) {
                BarcodeCameraPreview(
                    formats = intArrayOf(Barcode.FORMAT_QR_CODE),
                    torchEnabled = torchEnabled,
                    onDetected = { raw, _ -> onQrDetected(raw) },
                    onError = { msg -> cameraError = msg }
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val frameSize = size.width * 0.68f
                    val left = (size.width - frameSize) / 2f
                    val top = (size.height - frameSize) / 2f

                    drawRect(color = Color.Black.copy(alpha = 0.45f))
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(left, top),
                        size = Size(frameSize, frameSize),
                        cornerRadius = CornerRadius(28f, 28f),
                        blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                    )
                    drawRoundRect(
                        color = EmeraldGreen,
                        topLeft = Offset(left, top),
                        size = Size(frameSize, frameSize),
                        cornerRadius = CornerRadius(28f, 28f),
                        style = Stroke(width = 6f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PureWhite)
                        .safeDrawingPadding()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(SoftMint, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "QR Scanner",
                            tint = DeepTeal,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Camera Access Required", style = MaterialTheme.typography.headlineSmall, color = MainText)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Allow camera access to scan QR verification codes and inspect URLs safely before trusting them.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Allow Camera Access")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showManualQrDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Inspect QR Payload Manually", color = DeepTeal)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeDrawingPadding()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .testTag("qr_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "QR Code Verification",
                            color = PureWhite,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    IconButton(
                        onClick = { torchEnabled = !torchEnabled },
                        enabled = hasCameraPermission,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            if (torchEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Flashlight",
                            tint = PureWhite
                        )
                    }
                }

                if (cameraError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = ErrorRed.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = cameraError!!,
                            color = PureWhite,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (hasCameraPermission) {
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Point camera at the product QR code",
                            style = MaterialTheme.typography.titleSmall,
                            color = MainText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "URLs and verification tokens are inspected before trusting any destination.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showManualQrDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("qr_manual_input_button")
                        ) {
                            Text("Paste or Test QR Content", color = DeepTeal)
                        }
                    }
                }
            }
        }
    }

    if (showManualQrDialog) {
        AlertDialog(
            onDismissRequest = { showManualQrDialog = false },
            containerColor = PureWhite,
            title = { Text("Inspect QR Content", color = MainText) },
            text = {
                Column {
                    OutlinedTextField(
                        value = manualQrInput,
                        onValueChange = { manualQrInput = it; manualQrError = null },
                        label = { Text("QR text or URL") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_qr_input")
                    )
                    if (manualQrError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(manualQrError!!, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = LightMintBackground,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Demo QR IDs: TC-VERIFY-AMOX-2026 (Real), http://bit.ly/fake (Suspicious URL), TC-FAKE-COUNTERFEIT-00 (Fake)",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepTeal,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = manualQrInput.trim()
                        if (trimmed.isEmpty()) {
                            manualQrError = "Please enter QR text or URL."
                        } else {
                            showManualQrDialog = false
                            onQrDetected(trimmed)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                    modifier = Modifier.testTag("manual_qr_submit")
                ) {
                    Text("Inspect QR")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualQrDialog = false }) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }
}
