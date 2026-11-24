package com.example.domain.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageCompressionUtil {
    private const val MAX_DIMENSION = 1280
    private const val JPEG_QUALITY = 82

    suspend fun compressToCacheFile(context: Context, sourceUri: Uri): Result<Uri> =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(sourceUri)
                    ?: return@withContext Result.failure(IllegalArgumentException("Unable to open selected image"))

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                inputStream.use { BitmapFactory.decodeStream(it, null, bounds) }

                val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION, MAX_DIMENSION)
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }

                val decodedStream = context.contentResolver.openInputStream(sourceUri)
                    ?: return@withContext Result.failure(IllegalArgumentException("Unable to decode selected image"))
                val bitmap = decodedStream.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
                    ?: return@withContext Result.failure(IllegalArgumentException("Image file could not be decoded"))

                val scaled = scaleDownIfNeeded(bitmap, MAX_DIMENSION)
                val outFile = File(context.cacheDir, "tc_compressed_${System.currentTimeMillis()}.jpg")
                ByteArrayOutputStream().use { baos ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos)
                    FileOutputStream(outFile).use { fos ->
                        fos.write(baos.toByteArray())
                    }
                }
                if (scaled !== bitmap) scaled.recycle()
                bitmap.recycle()
                Result.success(Uri.fromFile(outFile))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun calculateInSampleSize(width: Int, height: Int, reqW: Int, reqH: Int): Int {
        var inSampleSize = 1
        if (height > reqH || width > reqW) {
            val halfH = height / 2
            val halfW = width / 2
            while (halfH / inSampleSize >= reqH && halfW / inSampleSize >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        val newW = (w * ratio).toInt().coerceAtLeast(1)
        val newH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}
