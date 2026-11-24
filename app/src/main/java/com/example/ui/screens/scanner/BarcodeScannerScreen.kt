package com.example.ui.screens.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LightMintBackground
import com.example.ui.theme.MainText
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftMint
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerScreen(
    onBack: () -> Unit,
    onBarcodeDetected: (rawValue: String, formatLabel: String) -> Unit,
    onSwitchToManualCode: () -> Unit
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
    var showManualBarcodeDialog by remember { mutableStateOf(false) }
    var manualBarcodeText by remember { mutableStateOf("") }
    var manualBarcodeError by remember { mutableStateOf<String?>(null) }

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
                    formats = intArrayOf(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_CODE_39,
                        Barcode.FORMAT_QR_CODE
                    ),
                    torchEnabled = torchEnabled,
                    onDetected = onBarcodeDetected,
                    onError = { msg -> cameraError = msg }
                )
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
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera permission",
                            tint = DeepTeal,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MainText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Grant camera access to scan product barcodes (EAN-13, EAN-8, UPC-A, UPC-E, Code 128, Code 39) in real time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepTeal, contentColor = PureWhite),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Allow Camera Access")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showManualBarcodeDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Enter Barcode Manually", color = DeepTeal)
                    }
                }
            }

            // Scanning Frame Reticle Overlay (when camera active)
            if (hasCameraPermission) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val frameW = size.width * 0.78f
                    val frameH = frameW * 0.56f
                    val left = (size.width - frameW) / 2f
                    val top = (size.height - frameH) / 2f

                    drawRect(color = Color.Black.copy(alpha = 0.45f))
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(left, top),
                        size = Size(frameW, frameH),
                        cornerRadius = CornerRadius(24f, 24f),
                        blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                    )
                    drawRoundRect(
                        color = EmeraldGreen,
                        topLeft = Offset(left, top),
                        size = Size(frameW, frameH),
                        cornerRadius = CornerRadius(24f, 24f),
                        style = Stroke(width = 6f)
                    )
                }
            }

            // Top Controls
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
                            .testTag("barcode_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite
                        )
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "Barcode Scanner",
                            color = PureWhite,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    IconButton(
                        onClick = { torchEnabled = !torchEnabled },
                        enabled = hasCameraPermission,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .testTag("barcode_torch_button")
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Toggle flashlight",
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

            // Bottom Alternatives Bar
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
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Align the barcode inside the frame",
                            style = MaterialTheme.typography.titleSmall,
                            color = MainText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Supports EAN-13, EAN-8, UPC-A, UPC-E, Code 128, Code 39",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showManualBarcodeDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("barcode_manual_entry_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Type Barcode", color = DeepTeal)
                            }
                            Button(
                                onClick = onSwitchToManualCode,
                                colors = ButtonDefaults.buttonColors(containerColor = DeepTeal, contentColor = PureWhite),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("barcode_switch_security_code")
                            ) {
                                Text("Security PIN")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManualBarcodeDialog) {
        AlertDialog(
            onDismissRequest = { showManualBarcodeDialog = false },
            containerColor = PureWhite,
            title = { Text("Enter Barcode Number", color = MainText) },
            text = {
                Column {
                    Text(
                        text = "Enter a product barcode manually (e.g. demo sample 8901234567890).",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = manualBarcodeText,
                        onValueChange = { manualBarcodeText = it; manualBarcodeError = null },
                        label = { Text("Barcode digits") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_barcode_input")
                    )
                    if (manualBarcodeError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(manualBarcodeError!!, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = LightMintBackground,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Demo test barcodes: 8901234567890 (Real), 6151100043219 (Suspicious), 0001112223334 (Fake)",
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
                        val trimmed = manualBarcodeText.trim()
                        if (trimmed.length < 6) {
                            manualBarcodeError = "Enter at least 6 barcode characters."
                        } else {
                            showManualBarcodeDialog = false
                            onBarcodeDetected(trimmed, "Manual Barcode")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                    modifier = Modifier.testTag("manual_barcode_submit")
                ) {
                    Text("Verify")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualBarcodeDialog = false }) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }
}

@Composable
fun BarcodeCameraPreview(
    formats: IntArray,
    torchEnabled: Boolean,
    onDetected: (rawValue: String, formatLabel: String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember {
        MlKitBarcodeAnalyzer(
            formats = formats,
            onDetected = onDetected,
            onError = { err -> onError(err.localizedMessage ?: "Barcode scan error") }
        )
    }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(torchEnabled, boundCamera) {
        try {
            boundCamera?.cameraControl?.enableTorch(torchEnabled)
        } catch (_: Exception) {
        }
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor, analyzer) }

                cameraProvider.unbindAll()
                boundCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
                onError("Unable to open camera: ${e.localizedMessage ?: "Hardware unavailable"}")
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (_: Exception) {
            }
            analyzer.close()
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}
