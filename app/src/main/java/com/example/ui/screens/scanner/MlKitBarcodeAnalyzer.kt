package com.example.ui.screens.scanner

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

class MlKitBarcodeAnalyzer(
    formats: IntArray,
    private val onDetected: (rawValue: String, formatLabel: String) -> Unit,
    private val onError: (Throwable) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner: BarcodeScanner = run {
        val builder = BarcodeScannerOptions.Builder()
        if (formats.size == 1) {
            builder.setBarcodeFormats(formats[0])
        } else if (formats.size > 1) {
            builder.setBarcodeFormats(formats[0], *formats.drop(1).toIntArray())
        }
        BarcodeScanning.getClient(builder.build())
    }

    private val isProcessing = AtomicBoolean(false)
    private val hasDispatched = AtomicBoolean(false)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (hasDispatched.get() || !isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            isProcessing.set(false)
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val first = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                if (first != null && hasDispatched.compareAndSet(false, true)) {
                    onDetected(first.rawValue.orEmpty(), formatName(first.format))
                }
            }
            .addOnFailureListener { err ->
                onError(err)
            }
            .addOnCompleteListener {
                isProcessing.set(false)
                imageProxy.close()
            }
    }

    fun resetDetection() {
        hasDispatched.set(false)
    }

    fun close() {
        scanner.close()
    }

    private fun formatName(format: Int): String = when (format) {
        Barcode.FORMAT_EAN_13 -> "EAN-13"
        Barcode.FORMAT_EAN_8 -> "EAN-8"
        Barcode.FORMAT_UPC_A -> "UPC-A"
        Barcode.FORMAT_UPC_E -> "UPC-E"
        Barcode.FORMAT_CODE_128 -> "Code 128"
        Barcode.FORMAT_CODE_39 -> "Code 39"
        Barcode.FORMAT_QR_CODE -> "QR Code"
        else -> "Barcode"
    }
}
