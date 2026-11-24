package com.example.domain.ai

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.data.model.AiPackagingInspection
import com.example.data.model.ProductCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Gemini packaging inspection repository backed by BuildConfig.GEMINI_API_KEY
 * configured through AI Studio Secrets (.env).
 * Falls back cleanly to PlaceholderAiInspectionRepository when no key is configured.
 */
class GeminiApiKeyInspectionRepository(
    private val context: Context,
    private val apiKey: String,
    private val fallbackRepository: AiInspectionRepository
) : AiInspectionRepository {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(55, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun inspectProductImages(
        imageUriStrings: List<String>,
        notes: String
    ): Result<AiPackagingInspection> {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty() || trimmedKey == PLACEHOLDER_KEY) {
            return fallbackRepository.inspectProductImages(imageUriStrings, notes)
        }
        if (imageUriStrings.isEmpty()) {
            return Result.failure(IllegalArgumentException("At least one product photo is required."))
        }

        return try {
            withTimeout(60_000L) {
                withContext(Dispatchers.IO) {
                    val partsArray = JSONArray()
                    val promptText = buildString {
                        append(
                            "You are inspecting consumer product packaging photos for authenticity red flags. " +
                                "Inspect typography, spelling accuracy, tamper seals, print sharpness, label alignment, " +
                                "batch/lot markings, expiry date, and regulatory registration numbers (such as NAFDAC). " +
                                "Never claim chemical authenticity from a photo alone."
                        )
                        if (notes.isNotBlank()) {
                            append(" Consumer notes: ").append(notes.trim())
                        }
                    }
                    partsArray.put(JSONObject().put("text", promptText))

                    var totalBytes = 0
                    var attachedCount = 0
                    for (uriStr in imageUriStrings.take(4)) {
                        val bytes = readImageBytes(Uri.parse(uriStr)) ?: continue
                        if (totalBytes + bytes.size > MAX_TOTAL_IMAGE_BYTES) continue
                        totalBytes += bytes.size
                        attachedCount++
                        val inlineData = JSONObject()
                            .put("mimeType", "image/jpeg")
                            .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))
                        partsArray.put(JSONObject().put("inlineData", inlineData))
                    }

                    if (attachedCount == 0) {
                        return@withContext Result.failure(
                            IllegalStateException("Could not read selected packaging photo bytes for inspection.")
                        )
                    }

                    val requestPayload = buildRequestJson(partsArray)
                    val request = Request.Builder()
                        .url(ENDPOINT_URL)
                        .addHeader("x-goog-api-key", trimmedKey)
                        .addHeader("Content-Type", "application/json")
                        .post(requestPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                        .build()

                    client.newCall(request).execute().use { response ->
                        val bodyText = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            Log.e(TAG, "Gemini inspection request failed with HTTP ${response.code}")
                            return@withContext Result.failure(
                                IOException("AI inspection service returned HTTP ${response.code}. Please verify your API key in Secrets and try again.")
                            )
                        }
                        Result.success(parseGeminiResponseJson(bodyText))
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini packaging inspection error", e)
            Result.failure(
                IllegalStateException(
                    e.localizedMessage ?: "Packaging inspection failed. Please try again."
                )
            )
        }
    }

    private fun readImageBytes(uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildRequestJson(partsArray: JSONArray): JSONObject {
        val properties = JSONObject()
            .put("productName", JSONObject().put("type", "STRING").put("nullable", true))
            .put("brand", JSONObject().put("type", "STRING").put("nullable", true))
            .put(
                "category",
                JSONObject()
                    .put("type", "STRING")
                    .put(
                        "enum",
                        JSONArray(
                            listOf("MEDICINES", "FOOD_BEVERAGES", "BEAUTY_PERSONAL_CARE", "OTHER")
                        )
                    )
            )
            .put("manufacturerInfo", JSONObject().put("type", "STRING").put("nullable", true))
            .put("registrationNumber", JSONObject().put("type", "STRING").put("nullable", true))
            .put("batchNumber", JSONObject().put("type", "STRING").put("nullable", true))
            .put("expiryDate", JSONObject().put("type", "STRING").put("nullable", true))
            .put("manufacturingDate", JSONObject().put("type", "STRING").put("nullable", true))
            .put(
                "visibleCharacteristics",
                JSONObject()
                    .put("type", "ARRAY")
                    .put("items", JSONObject().put("type", "STRING"))
            )
            .put(
                "potentialRedFlags",
                JSONObject()
                    .put("type", "ARRAY")
                    .put("items", JSONObject().put("type", "STRING"))
            )
            .put("imageClarityAcceptable", JSONObject().put("type", "BOOLEAN"))

        val schema = JSONObject()
            .put("type", "OBJECT")
            .put("properties", properties)
            .put(
                "required",
                JSONArray(
                    listOf(
                        "category",
                        "visibleCharacteristics",
                        "potentialRedFlags",
                        "imageClarityAcceptable"
                    )
                )
            )

        val generationConfig = JSONObject()
            .put("responseMimeType", "application/json")
            .put("responseSchema", schema)

        return JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", partsArray)))
            .put("generationConfig", generationConfig)
    }

    companion object {
        private const val TAG = "GeminiApiKeyInspection"
        private const val PLACEHOLDER_KEY = "MY_GEMINI_API_KEY"
        private const val MODEL_ID = "gemini-3.8-flash"
        private const val ENDPOINT_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_ID:generateContent"
        private const val MAX_TOTAL_IMAGE_BYTES = 14 * 1024 * 1024
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        internal fun parseGeminiResponseJson(rawEnvelopeJson: String): AiPackagingInspection {
            val root = JSONObject(rawEnvelopeJson)
            val candidates = root.optJSONArray("candidates")
                ?: throw IllegalStateException("AI response contained no candidates.")
            if (candidates.length() == 0) {
                throw IllegalStateException("AI response returned empty candidates.")
            }
            val parts = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
            val jsonText = (0 until parts.length())
                .mapNotNull { idx -> parts.optJSONObject(idx)?.optString("text")?.takeIf { it.isNotBlank() } }
                .joinToString("")
            if (jsonText.isBlank()) {
                throw IllegalStateException("AI response returned empty structured output.")
            }

            val payload = JSONObject(jsonText)
            val rawCategory = payload.optString("category", "OTHER")
            val category = ProductCategory.entries.firstOrNull { it.name == rawCategory } ?: ProductCategory.OTHER

            return AiPackagingInspection(
                productName = payload.optNullableString("productName"),
                brand = payload.optNullableString("brand"),
                category = category,
                manufacturerInfo = payload.optNullableString("manufacturerInfo"),
                registrationNumber = payload.optNullableString("registrationNumber"),
                batchNumber = payload.optNullableString("batchNumber"),
                expiryDate = payload.optNullableString("expiryDate"),
                manufacturingDate = payload.optNullableString("manufacturingDate"),
                visibleCharacteristics = payload.optStringList("visibleCharacteristics"),
                potentialRedFlags = payload.optStringList("potentialRedFlags"),
                imageClarityAcceptable = payload.optBoolean("imageClarityAcceptable", true),
                isPlaceholderResponse = false,
                notice = "AI visual packaging inspection completed. Visual cues cannot prove chemical authenticity—verify barcode, QR, or security code for confirmation."
            )
        }

        private fun JSONObject.optNullableString(key: String): String? {
            if (!has(key) || isNull(key)) return null
            return optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }
        }

        private fun JSONObject.optStringList(key: String): List<String> {
            val arr = optJSONArray(key) ?: return emptyList()
            return buildList {
                for (i in 0 until arr.length()) {
                    val value = arr.optString(i).trim()
                    if (value.isNotEmpty()) add(value)
                }
            }
        }
    }
}
