package com.example.data.model

import kotlinx.serialization.Serializable

enum class UserRole {
    CONSUMER,
    ADMIN
}

enum class AccountStatus {
    ACTIVE,
    SUSPENDED
}

enum class ScanMethod(val label: String) {
    BARCODE("Barcode Scan"),
    QR_CODE("QR Code Scan"),
    SECURITY_CODE("Security Code"),
    PRODUCT_PHOTO("Product Photo")
}

enum class VerificationStatus(val heading: String) {
    REAL("PRODUCT VERIFIED"),
    SUSPICIOUS("SUSPICIOUS PRODUCT"),
    FAKE("POSSIBLE COUNTERFEIT DETECTED"),
    UNVERIFIED("UNABLE TO VERIFY")
}

enum class ProductCategory(val displayName: String) {
    MEDICINES("Medicines & Pharmaceuticals"),
    FOOD_BEVERAGES("Food & Beverages"),
    BEAUTY_PERSONAL_CARE("Beauty & Personal Care"),
    OTHER("Consumer Goods")
}

enum class ActivityType(val label: String) {
    SIGN_IN("Signed In"),
    PRODUCT_SCAN("Performed Product Scan"),
    VIEW_REPORT("Viewed Verification Report"),
    CORE_FEATURE("Used Core Feature")
}

enum class ProviderConnectionStatus(val label: String) {
    CONNECTED("Connected"),
    DEMO_MODE("Demonstration Registry"),
    CREDENTIALS_REQUIRED("Authorization Required"),
    UNAVAILABLE("Temporarily Unavailable")
}

@Serializable
data class ExternalProviderStatus(
    val providerId: String,
    val name: String,
    val status: ProviderConnectionStatus,
    val detail: String
)

@Serializable
data class VerificationEvidence(
    val evidenceChecked: List<String> = emptyList(),
    val evidenceMatched: List<String> = emptyList(),
    val evidenceFailed: List<String> = emptyList(),
    val batchNumber: String? = null,
    val expiryDate: String? = null,
    val registrationNumber: String? = null,
    val providerLimitations: List<String> = emptyList(),
    val aiNotice: String? = null
)

@Serializable
data class VerificationInput(
    val method: ScanMethod,
    val rawValue: String,
    val formatLabel: String = "",
    val imageUriStrings: List<String> = emptyList(),
    val notes: String = ""
)

@Serializable
data class AiPackagingInspection(
    val productName: String? = null,
    val brand: String? = null,
    val category: ProductCategory = ProductCategory.OTHER,
    val manufacturerInfo: String? = null,
    val registrationNumber: String? = null,
    val batchNumber: String? = null,
    val expiryDate: String? = null,
    val manufacturingDate: String? = null,
    val visibleCharacteristics: List<String> = emptyList(),
    val potentialRedFlags: List<String> = emptyList(),
    val imageClarityAcceptable: Boolean = true,
    val isPlaceholderResponse: Boolean = true,
    val notice: String = ""
)

data class UserProfile(
    val id: String,
    val fullName: String,
    val email: String,
    val role: UserRole,
    val status: AccountStatus,
    val emailVerified: Boolean,
    val createdAtUtc: Long,
    val lastActiveAtUtc: Long,
    val scanCount: Int = 0
)

data class ScanReportItem(
    val id: String,
    val userId: String,
    val productName: String,
    val brand: String,
    val category: ProductCategory,
    val productImageRef: String?,
    val status: VerificationStatus,
    val method: ScanMethod,
    val rawIdentifier: String,
    val reason: String,
    val verificationSource: String,
    val recommendedAction: String,
    val timestampUtc: Long,
    val evidence: VerificationEvidence,
    val isPendingOfflineSync: Boolean = false
)

data class NotificationItem(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String,
    val timestampUtc: Long,
    val isRead: Boolean
)

data class NotificationPreferences(
    val verificationCompleted: Boolean = true,
    val suspiciousAlerts: Boolean = true,
    val codeReuseAlerts: Boolean = true,
    val safetyRecalls: Boolean = true
)

data class DailyMetricPoint(
    val dateUtc: String,
    val count: Int
)

data class AdminDashboardSnapshot(
    val totalUsers: Int,
    val dailyActiveUsers: Int,
    val monthlyActiveUsers: Int,
    val newUsersToday: Int,
    val newUsers7Days: Int,
    val newUsers30Days: Int,
    val totalScans: Int,
    val scansToday: Int,
    val scans7Days: Int,
    val scans30Days: Int,
    val realCount: Int,
    val suspiciousCount: Int,
    val fakeCount: Int,
    val unverifiedCount: Int,
    val topScanMethod: String,
    val topCategory: String,
    val dailyRegistrationsSeries: List<DailyMetricPoint>,
    val dailyActiveUsersSeries: List<DailyMetricPoint>,
    val dailyScansSeries: List<DailyMetricPoint>,
    val weeklyGrowthSeries: List<DailyMetricPoint>,
    val monthlyGrowthSeries: List<DailyMetricPoint>,
    val users: List<UserProfile>
)
