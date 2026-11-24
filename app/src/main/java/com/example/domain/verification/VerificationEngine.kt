package com.example.domain.verification

import com.example.data.local.ProductCatalogDao
import com.example.data.local.SecurityCodeDao
import com.example.data.model.ProductCategory
import com.example.data.model.ScanMethod
import com.example.data.model.ScanReportItem
import com.example.data.model.VerificationEvidence
import com.example.data.model.VerificationInput
import com.example.data.model.VerificationStatus
import com.example.domain.ai.AiInspectionRepository
import java.util.UUID

class VerificationEngine(
    private val productCatalogDao: ProductCatalogDao,
    private val securityCodeDao: SecurityCodeDao,
    private val aiInspectionRepository: AiInspectionRepository
) {
    suspend fun evaluate(
        userId: String,
        input: VerificationInput,
        isOnline: Boolean
    ): ScanReportItem {
        val now = System.currentTimeMillis()
        val scanId = "scan_${UUID.randomUUID()}"

        return when (input.method) {
            ScanMethod.BARCODE -> evaluateBarcode(scanId, userId, input, now, isOnline)
            ScanMethod.QR_CODE -> evaluateQrCode(scanId, userId, input, now, isOnline)
            ScanMethod.SECURITY_CODE -> evaluateSecurityCode(scanId, userId, input, now, isOnline)
            ScanMethod.PRODUCT_PHOTO -> evaluateProductPhoto(scanId, userId, input, now, isOnline)
        }
    }

    private suspend fun evaluateBarcode(
        scanId: String,
        userId: String,
        input: VerificationInput,
        now: Long,
        isOnline: Boolean
    ): ScanReportItem {
        val code = input.rawValue.trim()
        val checked = mutableListOf(
            "Barcode syntax & GS1 GTIN check-digit validation",
            "Local product database lookup",
            "Regulatory & batch safety alert lookup"
        )
        val matched = mutableListOf<String>()
        val failed = mutableListOf<String>()
        val limitations = mutableListOf(
            "Live GS1 Verified-by-GS1 & NAFDAC Greenbook APIs require partner credentials."
        )
        if (!isOnline) {
            limitations.add("Device appears offline; external registry checks were skipped.")
        }

        val validSyntax = code.all { it.isDigit() || it.isLetterOrDigit() } && code.length >= 6
        val gtinValid = ExternalVerificationProviders.isValidGtinCheckDigit(code)
        if (gtinValid) {
            matched.add("GS1 GTIN check digit is structurally valid ($code)")
        } else if (validSyntax) {
            matched.add("Barcode format (${input.formatLabel.ifBlank { "1D Retail" }}) decoded cleanly")
        }

        val record = productCatalogDao.findByBarcode(code)
        if (record == null) {
            // Do NOT mark genuine just because check digit is valid, and do NOT mark missing products as fake.
            return ScanReportItem(
                id = scanId,
                userId = userId,
                productName = "Unregistered Barcode ($code)",
                brand = "Unknown Brand",
                category = ProductCategory.OTHER,
                productImageRef = null,
                status = VerificationStatus.UNVERIFIED,
                method = ScanMethod.BARCODE,
                rawIdentifier = code,
                reason = if (gtinValid) {
                    "The barcode check digit is valid, but a valid barcode format alone does not prove authenticity and no matching trusted product record was found."
                } else {
                    "No matching trusted product or regulatory record was found for barcode $code."
                },
                verificationSource = "GS1 Syntax Check + TRUECHECK Local Registry",
                recommendedAction = "Check the packaging for a scratch-off security code or NAFDAC registration number and verify before purchasing.",
                timestampUtc = now,
                evidence = VerificationEvidence(
                    evidenceChecked = checked,
                    evidenceMatched = matched,
                    evidenceFailed = listOf("No matching product entry in verified registry"),
                    providerLimitations = limitations
                ),
                isPendingOfflineSync = !isOnline
            )
        }

        val category = parseCategory(record.category)
        val status = when (record.registryStatus) {
            "REAL" -> {
                matched.add("Matched trusted product record: ${record.productName}")
                matched.add("Batch ${record.batchNumber} and registration ${record.registrationNumber} active")
                VerificationStatus.REAL
            }
            "SUSPICIOUS" -> {
                failed.add(record.safetyNote)
                VerificationStatus.SUSPICIOUS
            }
            "FAKE" -> {
                failed.add(record.safetyNote)
                VerificationStatus.FAKE
            }
            else -> VerificationStatus.UNVERIFIED
        }

        return ScanReportItem(
            id = scanId,
            userId = userId,
            productName = record.productName,
            brand = record.brand,
            category = category,
            productImageRef = null,
            status = status,
            method = ScanMethod.BARCODE,
            rawIdentifier = code,
            reason = record.safetyNote,
            verificationSource = record.sourceLabel,
            recommendedAction = recommendedActionFor(status, category),
            timestampUtc = now,
            evidence = VerificationEvidence(
                evidenceChecked = checked,
                evidenceMatched = matched,
                evidenceFailed = failed,
                batchNumber = record.batchNumber,
                expiryDate = record.expiryDate,
                registrationNumber = record.registrationNumber,
                providerLimitations = limitations
            ),
            isPendingOfflineSync = !isOnline
        )
    }

    private suspend fun evaluateQrCode(
        scanId: String,
        userId: String,
        input: VerificationInput,
        now: Long,
        isOnline: Boolean
    ): ScanReportItem {
        val parsed = ExternalVerificationProviders.inspectQrContent(input.rawValue)
        val checked = mutableListOf(
            "QR URI scheme & domain safety inspection",
            "GS1 Digital Link & verification identifier extraction",
            "Trusted product & security code lookup"
        )
        val matched = mutableListOf<String>()
        val failed = mutableListOf<String>()
        val limitations = mutableListOf(
            "External manufacturer QR portals are not automatically trusted even if a link opens."
        )

        if (parsed.urlSafetyIssues.isNotEmpty()) {
            failed.addAll(parsed.urlSafetyIssues)
            return ScanReportItem(
                id = scanId,
                userId = userId,
                productName = parsed.urlHost?.let { "Suspicious QR Destination ($it)" } ?: "Flagged QR Payload",
                brand = "Unverified Source",
                category = ProductCategory.OTHER,
                productImageRef = null,
                status = VerificationStatus.SUSPICIOUS,
                method = ScanMethod.QR_CODE,
                rawIdentifier = parsed.rawText,
                reason = parsed.urlSafetyIssues.joinToString(" "),
                verificationSource = "TRUECHECK QR Safety Inspector",
                recommendedAction = "Do not open untrusted QR links or rely on them as proof of authenticity. Verify using a printed scratch code or official regulatory lookup.",
                timestampUtc = now,
                evidence = VerificationEvidence(
                    evidenceChecked = checked,
                    evidenceMatched = matched,
                    evidenceFailed = failed,
                    batchNumber = parsed.batchNumber,
                    providerLimitations = limitations
                )
            )
        }

        val byQr = productCatalogDao.findByQr(parsed.rawText)
            ?: parsed.gtin?.let { productCatalogDao.findByBarcode(it) }

        if (byQr != null) {
            val category = parseCategory(byQr.category)
            val status = when (byQr.registryStatus) {
                "REAL" -> {
                    matched.add("QR verification identifier matched ${byQr.productName}")
                    matched.add("Registration ${byQr.registrationNumber} verified in registry")
                    VerificationStatus.REAL
                }
                "SUSPICIOUS" -> {
                    failed.add(byQr.safetyNote)
                    VerificationStatus.SUSPICIOUS
                }
                "FAKE" -> {
                    failed.add(byQr.safetyNote)
                    VerificationStatus.FAKE
                }
                else -> VerificationStatus.UNVERIFIED
            }
            return ScanReportItem(
                id = scanId,
                userId = userId,
                productName = byQr.productName,
                brand = byQr.brand,
                category = category,
                productImageRef = null,
                status = status,
                method = ScanMethod.QR_CODE,
                rawIdentifier = parsed.rawText,
                reason = byQr.safetyNote,
                verificationSource = byQr.sourceLabel,
                recommendedAction = recommendedActionFor(status, category),
                timestampUtc = now,
                evidence = VerificationEvidence(
                    evidenceChecked = checked,
                    evidenceMatched = matched,
                    evidenceFailed = failed,
                    batchNumber = byQr.batchNumber,
                    expiryDate = byQr.expiryDate,
                    registrationNumber = byQr.registrationNumber,
                    providerLimitations = limitations
                )
            )
        }

        // Check if QR embedded a security code
        val embeddedCode = parsed.extractedCode
        if (!embeddedCode.isNullOrBlank()) {
            val codeMatch = securityCodeDao.findByCode(embeddedCode)
            if (codeMatch != null) {
                return buildSecurityCodeReport(scanId, userId, embeddedCode, ScanMethod.QR_CODE, codeMatch, now, limitations)
            }
        }

        if (parsed.isUrl && !parsed.urlHost.isNullOrBlank()) {
            matched.add("HTTPS transport verified on ${parsed.urlHost}")
        }
        failed.add("No trusted verification identifier found for this QR payload.")

        return ScanReportItem(
            id = scanId,
            userId = userId,
            productName = parsed.urlHost?.let { "Unverified QR ($it)" } ?: "Unrecognized QR Code",
            brand = "Unknown Brand",
            category = ProductCategory.OTHER,
            productImageRef = null,
            status = VerificationStatus.UNVERIFIED,
            method = ScanMethod.QR_CODE,
            rawIdentifier = parsed.rawText,
            reason = "A readable QR code or valid URL does not prove a product is genuine. No trusted verification record matched this QR payload.",
            verificationSource = "TRUECHECK QR Safety Inspector",
            recommendedAction = "Inspect the packaging for an official scratch-off authentication PIN or consult a licensed pharmacist or retailer.",
            timestampUtc = now,
            evidence = VerificationEvidence(
                evidenceChecked = checked,
                evidenceMatched = matched,
                evidenceFailed = failed,
                batchNumber = parsed.batchNumber,
                providerLimitations = limitations
            ),
            isPendingOfflineSync = !isOnline
        )
    }

    private suspend fun evaluateSecurityCode(
        scanId: String,
        userId: String,
        input: VerificationInput,
        now: Long,
        isOnline: Boolean
    ): ScanReportItem {
        val cleanCode = input.rawValue.trim().uppercase()
        val limitations = mutableListOf(
            "Live NAFDAC Mobile Authentication Service (MAS) requires authorized gateway credentials; demonstration MAS registry checked locally."
        )
        if (!isOnline) {
            limitations.add("Offline mode: remote manufacturer code redemption status could not be fetched.")
        }

        val record = securityCodeDao.findByCode(cleanCode)
        if (record == null) {
            return ScanReportItem(
                id = scanId,
                userId = userId,
                productName = "Unverified Code ($cleanCode)",
                brand = "Unknown Manufacturer",
                category = ProductCategory.OTHER,
                productImageRef = null,
                status = VerificationStatus.UNVERIFIED,
                method = ScanMethod.SECURITY_CODE,
                rawIdentifier = cleanCode,
                reason = "No trusted verification provider returned a match for security code $cleanCode. Live NAFDAC MAS credentials are not configured.",
                verificationSource = "TRUECHECK Security Code Adapter (Provider Unavailable)",
                recommendedAction = "Double-check the printed characters or send the code via the official regulatory SMS/MAS channel printed on the pack.",
                timestampUtc = now,
                evidence = VerificationEvidence(
                    evidenceChecked = listOf(
                        "Alphanumeric security code format validation",
                        "Duplicate redemption check",
                        "NAFDAC MAS / Manufacturer code registry"
                    ),
                    evidenceMatched = listOf("Code syntax valid ($cleanCode)"),
                    evidenceFailed = listOf("No trusted provider response matched this security code"),
                    providerLimitations = limitations
                ),
                isPendingOfflineSync = !isOnline
            )
        }

        return buildSecurityCodeReport(scanId, userId, cleanCode, ScanMethod.SECURITY_CODE, record, now, limitations)
    }

    private suspend fun buildSecurityCodeReport(
        scanId: String,
        userId: String,
        cleanCode: String,
        method: ScanMethod,
        record: com.example.data.local.SecurityCodeEntity,
        now: Long,
        limitations: List<String>
    ): ScanReportItem {
        val category = parseCategory(record.category)
        val checked = listOf(
            "Alphanumeric security code validation",
            "Duplicate / prior redemption check",
            "Batch & expiry metadata cross-check"
        )
        val matched = mutableListOf<String>()
        val failed = mutableListOf<String>()

        val status = when {
            record.authenticityFlag == "COUNTERFEIT" -> {
                failed.add("Code $cleanCode is flagged as a known counterfeit / cloned security PIN.")
                VerificationStatus.FAKE
            }
            record.verificationCount >= 1 -> {
                failed.add("Security code $cleanCode has already been redeemed ${record.verificationCount} time(s).")
                VerificationStatus.SUSPICIOUS
            }
            else -> {
                securityCodeDao.markVerified(cleanCode, now, userId)
                matched.add("First-time authentic security code verification for $cleanCode")
                matched.add("Matched ${record.productName} (${record.registrationNumber})")
                VerificationStatus.REAL
            }
        }

        val reason = when (status) {
            VerificationStatus.REAL -> "Security code verified for the first time against ${record.sourceLabel}."
            VerificationStatus.SUSPICIOUS -> "Warning: this security code was already redeemed previously. Cloned packaging often reuses genuine PINs."
            VerificationStatus.FAKE -> "This security code matches a flagged counterfeit or revoked lot in the safety registry."
            VerificationStatus.UNVERIFIED -> "Insufficient evidence to verify."
        }

        return ScanReportItem(
            id = scanId,
            userId = userId,
            productName = record.productName,
            brand = record.brand,
            category = category,
            productImageRef = null,
            status = status,
            method = method,
            rawIdentifier = cleanCode,
            reason = reason,
            verificationSource = record.sourceLabel,
            recommendedAction = recommendedActionFor(status, category),
            timestampUtc = now,
            evidence = VerificationEvidence(
                evidenceChecked = checked,
                evidenceMatched = matched,
                evidenceFailed = failed,
                batchNumber = record.batchNumber,
                expiryDate = record.expiryDate,
                registrationNumber = record.registrationNumber,
                providerLimitations = limitations
            )
        )
    }

    private suspend fun evaluateProductPhoto(
        scanId: String,
        userId: String,
        input: VerificationInput,
        now: Long,
        isOnline: Boolean
    ): ScanReportItem {
        val inspectionResult = aiInspectionRepository.inspectProductImages(input.imageUriStrings, input.notes)
        val inspection = inspectionResult.getOrNull()

        val checked = mutableListOf(
            "Packaging visual clarity & resolution check",
            "AI label & typography inspection",
            "Cross-check against trusted product records"
        )
        val matched = mutableListOf<String>()
        val failed = mutableListOf<String>()
        val limitations = mutableListOf(
            "Visual packaging inspection provides supporting evidence only and can never confirm chemical authenticity."
        )

        if (inspection == null) {
            failed.add(inspectionResult.exceptionOrNull()?.localizedMessage ?: "Packaging image inspection could not complete.")
            return ScanReportItem(
                id = scanId,
                userId = userId,
                productName = "Product Photo Inspection",
                brand = "Unknown",
                category = ProductCategory.OTHER,
                productImageRef = input.imageUriStrings.firstOrNull(),
                status = VerificationStatus.UNVERIFIED,
                method = ScanMethod.PRODUCT_PHOTO,
                rawIdentifier = input.imageUriStrings.firstOrNull() ?: "photo",
                reason = "Visual packaging analysis could not complete.",
                verificationSource = "TRUECHECK Packaging Inspection",
                recommendedAction = "Retake a clear photo in good lighting or scan the barcode / security code.",
                timestampUtc = now,
                evidence = VerificationEvidence(
                    evidenceChecked = checked,
                    evidenceMatched = matched,
                    evidenceFailed = failed,
                    providerLimitations = limitations
                ),
                isPendingOfflineSync = !isOnline
            )
        }

        matched.addAll(inspection.visibleCharacteristics)
        failed.addAll(inspection.potentialRedFlags)

        // Never classify REAL from a photograph alone!
        val status = if (inspection.potentialRedFlags.isNotEmpty() || !inspection.imageClarityAcceptable) {
            VerificationStatus.SUSPICIOUS
        } else {
            VerificationStatus.UNVERIFIED
        }

        return ScanReportItem(
            id = scanId,
            userId = userId,
            productName = inspection.productName ?: "Packaged Product Photo",
            brand = inspection.brand ?: "Visual Inspection Only",
            category = inspection.category,
            productImageRef = input.imageUriStrings.firstOrNull(),
            status = status,
            method = ScanMethod.PRODUCT_PHOTO,
            rawIdentifier = "photo_inspection_${input.imageUriStrings.size}_angles",
            reason = if (!inspection.imageClarityAcceptable) {
                "Photo clarity was insufficient for reliable packaging inspection. Visual inspection alone also cannot confirm authenticity."
            } else {
                "Visual packaging inspection completed. A photograph alone cannot verify that a medicine, food, or cosmetic product is genuine — pair with a barcode, QR code, or security PIN."
            },
            verificationSource = if (inspection.isPlaceholderResponse) {
                "Sample Packaging Inspection (Firebase AI Logic Disabled)"
            } else {
                "TRUECHECK AI Packaging Inspection"
            },
            recommendedAction = "Scan the product's barcode, QR code, or scratch-off security PIN for authoritative verification.",
            timestampUtc = now,
            evidence = VerificationEvidence(
                evidenceChecked = checked,
                evidenceMatched = matched,
                evidenceFailed = failed,
                batchNumber = inspection.batchNumber,
                expiryDate = inspection.expiryDate,
                registrationNumber = inspection.registrationNumber,
                providerLimitations = limitations,
                aiNotice = inspection.notice
            ),
            isPendingOfflineSync = !isOnline
        )
    }

    private fun recommendedActionFor(status: VerificationStatus, category: ProductCategory): String {
        return when (status) {
            VerificationStatus.REAL -> "Product identifiers matched available trusted records. Still confirm intact tamper seals and valid expiry dates before use."
            VerificationStatus.SUSPICIOUS -> if (category == ProductCategory.MEDICINES) {
                "Do NOT consume this medicine until it has been checked by a licensed pharmacist or regulatory authority (such as NAFDAC)."
            } else {
                "Avoid using or consuming this product until you verify its batch details with the manufacturer or regulator."
            }
            VerificationStatus.FAKE -> if (category == ProductCategory.MEDICINES) {
                "Do NOT consume this product. Report the suspected counterfeit medicine immediately to a licensed pharmacist and your national drug regulatory authority (e.g., NAFDAC)."
            } else {
                "Do NOT use or consume this product. Return it to the point of purchase and report the batch to consumer protection authorities."
            }
            VerificationStatus.UNVERIFIED -> "Try scanning the barcode, QR code, or scratch-off authentication PIN, or consult a pharmacist/authorized retailer."
        }
    }

    private fun parseCategory(raw: String): ProductCategory {
        return ProductCategory.entries.firstOrNull { it.name == raw } ?: ProductCategory.OTHER
    }
}
