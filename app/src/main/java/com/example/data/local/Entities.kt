package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val role: String,
    val status: String,
    val emailVerified: Boolean,
    val createdAtUtc: Long,
    val lastActiveAtUtc: Long
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val barcode: String,
    val qrIdentifier: String,
    val productName: String,
    val brand: String,
    val category: String,
    val manufacturer: String,
    val registrationNumber: String,
    val batchNumber: String,
    val expiryDate: String,
    val registryStatus: String,
    val sourceLabel: String,
    val safetyNote: String
)

@Entity(tableName = "security_codes")
data class SecurityCodeEntity(
    @PrimaryKey val code: String,
    val productId: String,
    val productName: String,
    val brand: String,
    val category: String,
    val manufacturer: String,
    val registrationNumber: String,
    val batchNumber: String,
    val expiryDate: String,
    val authenticityFlag: String, // GENUINE, COUNTERFEIT, EXPIRED
    val verificationCount: Int,
    val firstVerifiedAtUtc: Long?,
    val firstVerifiedByUserId: String?,
    val sourceLabel: String
)

@Entity(
    tableName = "scans",
    indices = [Index(value = ["userId"]), Index(value = ["timestampUtc"])]
)
data class ScanEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val productName: String,
    val brand: String,
    val category: String,
    val productImageRef: String?,
    val status: String,
    val method: String,
    val rawIdentifier: String,
    val reason: String,
    val verificationSource: String,
    val recommendedAction: String,
    val timestampUtc: Long,
    val evidenceJson: String,
    val isPendingOfflineSync: Boolean
)

@Entity(
    tableName = "user_activity",
    primaryKeys = ["userId", "activityDateUtc"],
    indices = [Index(value = ["activityDateUtc"])]
)
data class UserActivityEntity(
    val userId: String,
    val activityDateUtc: String, // yyyy-MM-dd UTC
    val activityType: String,
    val lastActiveTimestampUtc: Long,
    val eventCount: Int
)

@Entity(
    tableName = "notifications",
    indices = [Index(value = ["userId"])]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String,
    val timestampUtc: Long,
    val isRead: Boolean
)

@Entity(tableName = "offline_scan_queue")
data class OfflineScanQueueEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val method: String,
    val rawValue: String,
    val formatLabel: String,
    val imageUrisJson: String,
    val createdAtUtc: Long
)
