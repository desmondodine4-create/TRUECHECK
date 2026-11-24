package com.example.domain.verification

import android.net.Uri
import com.example.data.model.ExternalProviderStatus
import com.example.data.model.ProviderConnectionStatus

data class ParsedQrResult(
    val rawText: String,
    val extractedCode: String?,
    val gtin: String?,
    val batchNumber: String?,
    val expiryDate: String?,
    val urlHost: String?,
    val isUrl: Boolean,
    val urlSafetyIssues: List<String>
)

object ExternalVerificationProviders {

    fun getProviderStatuses(): List<ExternalProviderStatus> = listOf(
        ExternalProviderStatus(
            providerId = "nafdac_mas",
            name = "NAFDAC Mobile Authentication Service (MAS)",
            status = ProviderConnectionStatus.CREDENTIALS_REQUIRED,
            detail = "Authorized NAFDAC MAS API credentials are not configured in this build. Only clearly labeled local demonstration codes are active."
        ),
        ExternalProviderStatus(
            providerId = "nafdac_greenbook",
            name = "NAFDAC Registered Products Database",
            status = ProviderConnectionStatus.CREDENTIALS_REQUIRED,
            detail = "Requires authorized regulatory API gateway access. Unrecognized registration numbers return Unverified."
        ),
        ExternalProviderStatus(
            providerId = "gs1_registry",
            name = "GS1 Global Product Identification",
            status = ProviderConnectionStatus.DEMO_MODE,
            detail = "GTIN check-digit and GS1 Digital Link parsing runs locally; live Verified-by-GS1 lookups require partner API keys."
        ),
        ExternalProviderStatus(
            providerId = "truecheck_demo",
            name = "TRUECHECK Demonstration Registry",
            status = ProviderConnectionStatus.DEMO_MODE,
            detail = "Offline sample catalog for testing barcode, QR, and security code workflows."
        )
    )

    fun isValidGtinCheckDigit(digits: String): Boolean {
        if (digits.length !in listOf(8, 12, 13, 14) || !digits.all { it.isDigit() }) {
            return false
        }
        val payload = digits.dropLast(1)
        val expectedCheck = digits.last().digitToInt()
        var sum = 0
        payload.reversed().forEachIndexed { index, ch ->
            val weight = if (index % 2 == 0) 3 else 1
            sum += ch.digitToInt() * weight
        }
        val calculated = (10 - (sum % 10)) % 10
        return calculated == expectedCheck
    }

    fun inspectQrContent(raw: String): ParsedQrResult {
        val trimmed = raw.trim()
        val lower = trimmed.lowercase()

        val dangerousSchemes = listOf("javascript:", "data:", "file:", "intent:", "vbscript:")
        if (dangerousSchemes.any { lower.startsWith(it) }) {
            return ParsedQrResult(
                rawText = trimmed,
                extractedCode = null,
                gtin = null,
                batchNumber = null,
                expiryDate = null,
                urlHost = null,
                isUrl = true,
                urlSafetyIssues = listOf("Blocked unsafe URI scheme detected in QR code.")
            )
        }

        val isHttpUrl = lower.startsWith("http://") || lower.startsWith("https://")
        if (isHttpUrl) {
            val uri = try { Uri.parse(trimmed) } catch (_: Exception) { null }
            val host = uri?.host?.lowercase().orEmpty()
            val issues = mutableListOf<String>()

            if (lower.startsWith("http://")) {
                issues.add("Unencrypted HTTP link — legitimate verification portals use HTTPS.")
            }
            if (host.isBlank()) {
                issues.add("Malformed URL host in QR payload.")
            }
            val shorteners = setOf("bit.ly", "tinyurl.com", "t.co", "goo.gl", "cutt.ly", "rb.gy")
            if (host in shorteners) {
                issues.add("Obfuscated short-link domain ($host) hides the real destination.")
            }
            if (host.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){3}$"))) {
                issues.add("Raw IP address host detected instead of an official manufacturer domain.")
            }

            // Parse GS1 Digital Link pattern (/01/<gtin>/10/<batch>/21/<serial>) or query ?code=
            val pathSegments = uri?.pathSegments ?: emptyList()
            var gtin: String? = null
            var batch: String? = null
            var code: String? = uri?.getQueryParameter("code")
                ?: uri?.getQueryParameter("pin")
                ?: uri?.getQueryParameter("serial")

            for (i in 0 until pathSegments.size - 1) {
                when (pathSegments[i]) {
                    "01" -> gtin = pathSegments[i + 1]
                    "10" -> batch = pathSegments[i + 1]
                    "21" -> if (code == null) code = pathSegments[i + 1]
                }
            }

            return ParsedQrResult(
                rawText = trimmed,
                extractedCode = code,
                gtin = gtin,
                batchNumber = batch,
                expiryDate = null,
                urlHost = host.ifBlank { null },
                isUrl = true,
                urlSafetyIssues = issues
            )
        }

        // Plain identifier or key-value verification token
        return ParsedQrResult(
            rawText = trimmed,
            extractedCode = trimmed,
            gtin = trimmed.takeIf { isValidGtinCheckDigit(it) },
            batchNumber = null,
            expiryDate = null,
            urlHost = null,
            isUrl = false,
            urlSafetyIssues = emptyList()
        )
    }
}
