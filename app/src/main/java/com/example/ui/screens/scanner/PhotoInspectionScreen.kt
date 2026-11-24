package com.example.ui.screens.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.screens.auth.trueCheckFieldColors
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedSoft
import com.example.ui.theme.LightMintBackground
import com.example.ui.theme.MainText
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftMint
import com.example.ui.theme.WarningAmberSoft
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Composable
fun PhotoInspectionScreen(
    scannerViewModel: ScannerViewModel,
    onBack: () -> Unit,
    onSubmitPhotos: (imageUrisJson: String, notes: String) -> Unit
) {
    val context = LocalContext.current
    val state by scannerViewModel.photoState.collectAsStateWithLifecycle()

    var showCameraCapture by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (granted) showCameraCapture = true
        }
    )

    // Zero-permission Android Photo Picker (Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                scannerViewModel.addImageUri(context, uri)
            }
        }
    )

    if (showCameraCapture && hasCameraPermission) {
        LivePhotoCaptureView(
            onBack = { showCameraCapture = false },
            onCaptured = { uri ->
                showCameraCapture = false
                scannerViewModel.addImageUri(context, uri)
            }
        )
        return
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
            Column(modifier = Modifier.widthIn(max = 580.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("photo_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MainText)
                    }
                    Text("Packaging Photo Inspection", style = MaterialTheme.typography.titleLarge, color = MainText)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Firebase AI Logic / Visual Inspection Disclosure
                Card(
                    colors = CardDefaults.cardColors(containerColor = WarningAmberSoft),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFB36E0A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.ai_firebase_notice) +
                                " Visual inspection alone never confirms chemical authenticity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MainText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Capture Front, Back & Side Angles",
                    style = MaterialTheme.typography.titleMedium,
                    color = MainText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Take clear, well-lit photos of labels, batch numbers, seals, and logos (up to 4 photos).",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (hasCameraPermission) {
                                showCameraCapture = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepTeal, contentColor = PureWhite),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("photo_open_camera_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Take Photo")
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("photo_pick_gallery_button")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery", color = DeepTeal)
                    }
                }

                if (state.isCompressing) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DeepTeal, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compressing image...", style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                    }
                }

                if (state.error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ErrorRedSoft),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(state.error!!, color = ErrorRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (state.capturedUris.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LightMintBackground),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = DeepTeal, modifier = Modifier.size(34.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No packaging photos selected yet", style = MaterialTheme.typography.bodyMedium, color = MainText)
                        }
                    }
                } else {
                    val angleLabels = listOf("Front", "Back", "Side 1", "Side 2")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        itemsIndexed(state.capturedUris) { idx, uriStr ->
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SoftMint)
                            ) {
                                AsyncImage(
                                    model = uriStr,
                                    contentDescription = "Product angle ${idx + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = angleLabels.getOrElse(idx) { "Angle ${idx + 1}" },
                                        color = PureWhite,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { scannerViewModel.removePhotoAt(idx) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(28.dp)
                                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = PureWhite, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { scannerViewModel.updatePhotoNotes(it) },
                    label = { Text("Optional packaging notes (e.g. spelling, seal condition)") },
                    colors = trueCheckFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = {
                        val urisJson = Json.encodeToString(state.capturedUris)
                        val notes = state.notes
                        scannerViewModel.clearPhotos()
                        onSubmitPhotos(urisJson, notes)
                    },
                    enabled = state.capturedUris.isNotEmpty() && !state.isCompressing,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal, contentColor = PureWhite),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("photo_submit_inspection_button")
                ) {
                    Text(
                        text = "ANALYZE PACKAGING",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LivePhotoCaptureView(
    onBack: () -> Unit,
    onCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    var captureError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                captureError = "Camera unavailable: ${e.localizedMessage}"
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                future.get().unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .safeDrawingPadding()
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close camera", tint = PureWhite)
        }

        if (captureError != null) {
            Surface(
                color = ErrorRed,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Text(captureError!!, color = PureWhite, modifier = Modifier.padding(14.dp))
            }
        }

        Button(
            onClick = {
                val outFile = File(context.cacheDir, "tc_raw_${System.currentTimeMillis()}.jpg")
                val outputOpts = ImageCapture.OutputFileOptions.Builder(outFile).build()
                imageCapture.takePicture(
                    outputOpts,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            onCaptured(Uri.fromFile(outFile))
                        }

                        override fun onError(exception: ImageCaptureException) {
                            captureError = "Capture failed: ${exception.localizedMessage}"
                        }
                    }
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = DeepTeal, contentColor = PureWhite),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(bottom = 28.dp)
                .height(56.dp)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Capture Photo")
        }
    }
}
