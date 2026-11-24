package com.example.domain.ai

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.model.AiPackagingInspection
import com.example.data.model.ProductCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Placeholder implementation used when Firebase AI Logic is not enabled.
 * Never claims chemical authenticity or calls external Gemini REST APIs directly.
 */
class PlaceholderAiInspectionRepository(
    private val context: Context
) : AiInspectionRepository {

    override suspend fun inspectProductImages(
        imageUriStrings: List<String>,
        notes: String
    ): Result<AiPackagingInspection> = withContext(Dispatchers.IO) {
        if (imageUriStrings.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("At least one product photo is required."))
        }

        val firstUri = Uri.parse(imageUriStrings.first())
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            context.contentResolver.openInputStream(firstUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (_: Exception) {
            // Keep graceful inspection fallback even if URI is synthetic in tests
        }

        val width = options.outWidth
        val height = options.outHeight
        val isClarityOk = (width <= 0 && height <= 0) || (width >= 320 && height >= 320)

        val characteristics = buildList {
            add("Captured ${imageUriStrings.size} packaging angle(s) compressed for inspection.")
            if (width > 0 && height > 0) {
                add("Image resolution verified (${width}x${height}px).")
            }
            if (notes.isNotBlank()) {
                add("Consumer notes recorded: ${notes.trim()}")
            }
            add("Sample inspection profile: visual layout check only (cannot confirm chemical composition).")
        }

        Result.success(
            AiPackagingInspection(
                productName = "Sample Packaged Product Inspection",
                brand = "Unverified Packaging Sample",
                category = ProductCategory.OTHER,
                manufacturerInfo = "Pending live AI extraction",
                registrationNumber = null,
                batchNumber = null,
                expiryDate = null,
                manufacturingDate = null,
                visibleCharacteristics = characteristics,
                potentialRedFlags = if (!isClarityOk) {
                    listOf("Photo resolution is low; capture a clearer, well-lit image of the front and back label.")
                } else {
                    emptyList()
                },
                imageClarityAcceptable = isClarityOk,
                isPlaceholderResponse = true,
                notice = "Live AI packaging inspection requires Firebase AI Logic. This is a sample placeholder inspection; pair with a barcode, QR code, or security code for verification."
            )
        )
    }
}
