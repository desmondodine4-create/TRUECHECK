package com.example.data.repository

import com.example.data.local.NotificationDao
import com.example.data.local.NotificationEntity
import com.example.data.local.OfflineScanQueueDao
import com.example.data.local.OfflineScanQueueEntity
import com.example.data.local.PreferencesManager
import com.example.data.local.ScanDao
import com.example.data.local.ScanEntity
import com.example.data.model.ActivityType
import com.example.data.model.NotificationItem
import com.example.data.model.ProductCategory
import com.example.data.model.ScanMethod
import com.example.data.model.ScanReportItem
import com.example.data.model.VerificationEvidence
import com.example.data.model.VerificationInput
import com.example.data.model.VerificationStatus
import com.example.domain.verification.VerificationEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class ScanRepository(
    private val scanDao: ScanDao,
    private val notificationDao: NotificationDao,
    private val offlineScanQueueDao: OfflineScanQueueDao,
    private val preferencesManager: PreferencesManager,
    private val verificationEngine: VerificationEngine,
    private val activityTrackingRepository: ActivityTrackingRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observeRecentScans(userId: String, limit: Int = 5): Flow<List<ScanReportItem>> {
        return scanDao.observeRecentScansForUser(userId, limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun observeAllScans(userId: String): Flow<List<ScanReportItem>> {
        return scanDao.observeScansForUser(userId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun observeNotifications(userId: String): Flow<List<NotificationItem>> {
        return notificationDao.observeForUser(userId).map { list ->
            list.map {
                NotificationItem(
                    id = it.id,
                    userId = it.userId,
                    title = it.title,
                    message = it.message,
                    type = it.type,
                    timestampUtc = it.timestampUtc,
                    isRead = it.isRead
                )
            }
        }
    }

    fun observeUnreadNotificationCount(userId: String): Flow<Int> {
        return notificationDao.observeUnreadCount(userId)
    }

    suspend fun markAllNotificationsRead(userId: String) {
        notificationDao.markAllRead(userId)
    }

    suspend fun verifyAndSaveScan(
        userId: String,
        input: VerificationInput,
        isOnline: Boolean
    ): ScanReportItem {
        val report = verificationEngine.evaluate(userId, input, isOnline)
        scanDao.insert(report.toEntity())

        if (!isOnline) {
            offlineScanQueueDao.enqueue(
                OfflineScanQueueEntity(
                    id = report.id,
                    userId = userId,
                    method = input.method.name,
                    rawValue = input.rawValue,
                    formatLabel = input.formatLabel,
                    imageUrisJson = json.encodeToString(input.imageUriStrings),
                    createdAtUtc = report.timestampUtc
                )
            )
        }

        activityTrackingRepository.recordQualifyingActivity(userId, ActivityType.PRODUCT_SCAN)
        emitVerificationNotification(userId, report)
        return report
    }

    suspend fun getReportAndRecordView(userId: String, scanId: String): ScanReportItem? {
        val entity = scanDao.getScanById(scanId, userId) ?: return null
        activityTrackingRepository.recordQualifyingActivity(userId, ActivityType.VIEW_REPORT)
        return entity.toDomain()
    }

    suspend fun deleteScan(userId: String, scanId: String) {
        scanDao.deleteScan(scanId, userId)
    }

    private suspend fun emitVerificationNotification(userId: String, report: ScanReportItem) {
        val prefs = preferencesManager.notificationPreferences.first()
        val shouldEmit = when (report.status) {
            VerificationStatus.REAL -> prefs.verificationCompleted
            VerificationStatus.SUSPICIOUS -> prefs.suspiciousAlerts || prefs.codeReuseAlerts
            VerificationStatus.FAKE -> prefs.suspiciousAlerts || prefs.safetyRecalls
            VerificationStatus.UNVERIFIED -> prefs.verificationCompleted
        }
        if (!shouldEmit) return

        val (title, message) = when (report.status) {
            VerificationStatus.REAL ->
                "Verification Completed" to "${report.productName} matched available registry evidence."
            VerificationStatus.SUSPICIOUS ->
                "Suspicious Product Detected" to "${report.productName}: ${report.reason}"
            VerificationStatus.FAKE ->
                "Counterfeit Safety Alert" to "Do not use ${report.productName}. ${report.reason}"
            VerificationStatus.UNVERIFIED ->
                "Product Information Unavailable" to "Unable to verify ${report.productName} from current sources."
        }

        notificationDao.insert(
            NotificationEntity(
                id = "notif_${UUID.randomUUID()}",
                userId = userId,
                title = title,
                message = message,
                type = report.status.name,
                timestampUtc = report.timestampUtc,
                isRead = false
            )
        )
    }

    private fun ScanReportItem.toEntity(): ScanEntity {
        return ScanEntity(
            id = id,
            userId = userId,
            productName = productName,
            brand = brand,
            category = category.name,
            productImageRef = productImageRef,
            status = status.name,
            method = method.name,
            rawIdentifier = rawIdentifier,
            reason = reason,
            verificationSource = verificationSource,
            recommendedAction = recommendedAction,
            timestampUtc = timestampUtc,
            evidenceJson = json.encodeToString(evidence),
            isPendingOfflineSync = isPendingOfflineSync
        )
    }

    private fun ScanEntity.toDomain(): ScanReportItem {
        val decodedEvidence = try {
            json.decodeFromString<VerificationEvidence>(evidenceJson)
        } catch (_: Exception) {
            VerificationEvidence()
        }
        return ScanReportItem(
            id = id,
            userId = userId,
            productName = productName,
            brand = brand,
            category = ProductCategory.entries.firstOrNull { it.name == category } ?: ProductCategory.OTHER,
            productImageRef = productImageRef,
            status = VerificationStatus.entries.firstOrNull { it.name == status } ?: VerificationStatus.UNVERIFIED,
            method = ScanMethod.entries.firstOrNull { it.name == method } ?: ScanMethod.BARCODE,
            rawIdentifier = rawIdentifier,
            reason = reason,
            verificationSource = verificationSource,
            recommendedAction = recommendedAction,
            timestampUtc = timestampUtc,
            evidence = decodedEvidence,
            isPendingOfflineSync = isPendingOfflineSync
        )
    }
}
