package com.example

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.local.SampleCatalogSeeder
import com.example.data.model.AccountStatus
import com.example.data.model.ActivityType
import com.example.data.model.ScanMethod
import com.example.data.model.UserRole
import com.example.data.model.VerificationInput
import com.example.data.model.VerificationStatus
import com.example.data.repository.ActivityTrackingRepository
import com.example.data.repository.AdminAnalyticsRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.ScanRepository
import com.example.domain.ai.PlaceholderAiInspectionRepository
import com.example.domain.verification.VerificationEngine
import com.example.ui.screens.scanner.ScannerViewModel
import com.example.ui.screens.scanner.SecurityCodeScreen
import com.example.ui.theme.TrueCheckTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TrueCheckWorkflowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var activityTrackingRepository: ActivityTrackingRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var scanRepository: ScanRepository
    private lateinit var adminAnalyticsRepository: AdminAnalyticsRepository
    private lateinit var aiInspectionRepository: PlaceholderAiInspectionRepository
    private lateinit var verificationEngine: VerificationEngine

    @Before
    fun setup() = runTest {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferencesManager = PreferencesManager(context)
        aiInspectionRepository = PlaceholderAiInspectionRepository(context)
        activityTrackingRepository = ActivityTrackingRepository(
            userActivityDao = database.userActivityDao(),
            userDao = database.userDao()
        )
        verificationEngine = VerificationEngine(
            productCatalogDao = database.productCatalogDao(),
            securityCodeDao = database.securityCodeDao(),
            aiInspectionRepository = aiInspectionRepository
        )
        authRepository = AuthRepository(
            userDao = database.userDao(),
            scanDao = database.scanDao(),
            userActivityDao = database.userActivityDao(),
            notificationDao = database.notificationDao(),
            preferencesManager = preferencesManager,
            activityTrackingRepository = activityTrackingRepository
        )
        scanRepository = ScanRepository(
            scanDao = database.scanDao(),
            notificationDao = database.notificationDao(),
            offlineScanQueueDao = database.offlineScanQueueDao(),
            preferencesManager = preferencesManager,
            verificationEngine = verificationEngine,
            activityTrackingRepository = activityTrackingRepository
        )
        adminAnalyticsRepository = AdminAnalyticsRepository(
            userDao = database.userDao(),
            scanDao = database.scanDao(),
            userActivityDao = database.userActivityDao()
        )

        SampleCatalogSeeder.seedIfNeeded(
            userDao = database.userDao(),
            productCatalogDao = database.productCatalogDao(),
            securityCodeDao = database.securityCodeDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    // 1. Registration
    @Test
    fun `registration validates input prevents duplicates and enforces consumer role`() = runTest {
        val shortPassword = authRepository.register("Ada Obi", "ada@example.com", "1234567", "1234567")
        assertTrue(shortPassword.isFailure)

        val mismatch = authRepository.register("Ada Obi", "ada@example.com", "Password123", "Password999")
        assertTrue(mismatch.isFailure)

        val created = authRepository.register("Ada Obi", "ada@example.com", "Password123", "Password123")
        assertTrue(created.isSuccess)
        val profile = created.getOrThrow()
        assertEquals(UserRole.CONSUMER, profile.role)
        assertEquals(AccountStatus.ACTIVE, profile.status)

        val duplicate = authRepository.register("Ada Duplicate", "ADA@example.com", "Password123", "Password123")
        assertTrue(duplicate.isFailure)
    }

    // 2. Login + 3. Logout
    @Test
    fun `login authenticates user records DAU once per UTC day and logout clears session`() = runTest {
        val registered = authRepository.register("Chidi Musa", "chidi@example.com", "SecretPass1", "SecretPass1").getOrThrow()
        authRepository.logout()
        assertNull(authRepository.currentUser.first())

        val wrongPassword = authRepository.login("chidi@example.com", "WrongPassword")
        assertTrue(wrongPassword.isFailure)

        val loggedIn = authRepository.login("chidi@example.com", "SecretPass1")
        assertTrue(loggedIn.isSuccess)
        assertEquals(registered.id, authRepository.currentUser.first()?.id)

        // Additional activity on the same UTC day still counts as 1 DAU user
        activityTrackingRepository.recordQualifyingActivity(registered.id, ActivityType.CORE_FEATURE)
        val todayUtc = ActivityTrackingRepository.utcDateString(System.currentTimeMillis())
        val dauCount = database.userActivityDao().getAllActivity()
            .filter { it.activityDateUtc == todayUtc }
            .map { it.userId }
            .distinct()
            .size
        assertEquals(1, dauCount)
        assertEquals(3, database.userActivityDao().getForDay(registered.id, todayUtc)?.eventCount)

        authRepository.logout()
        assertNull(authRepository.currentUser.first())
    }

    // 4. Password recovery
    @Test
    fun `password recovery updates credentials and rejects previous password`() = runTest {
        authRepository.register("Ngozi Ade", "ngozi@example.com", "OldPass123", "OldPass123").getOrThrow()
        authRepository.logout()

        val unknownEmail = authRepository.resetPassword("missing@example.com", "NewPass123", "NewPass123")
        assertTrue(unknownEmail.isFailure)

        val reset = authRepository.resetPassword("ngozi@example.com", "NewPass123!", "NewPass123!")
        assertTrue(reset.isSuccess)

        assertTrue(authRepository.login("ngozi@example.com", "OldPass123").isFailure)
        assertTrue(authRepository.login("ngozi@example.com", "NewPass123!").isSuccess)
    }

    // 5. Barcode scanning
    @Test
    fun `barcode verification differentiates trusted suspicious fake and valid-syntax unknown GTIN`() = runTest {
        val user = authRepository.register("Bola Ade", "bola@example.com", "SecurePass1", "SecurePass1").getOrThrow()

        val realReport = scanRepository.verifyAndSaveScan(
            userId = user.id,
            input = VerificationInput(ScanMethod.BARCODE, "8901234567890", "EAN-13"),
            isOnline = true
        )
        assertEquals(VerificationStatus.REAL, realReport.status)

        val suspiciousReport = scanRepository.verifyAndSaveScan(
            userId = user.id,
            input = VerificationInput(ScanMethod.BARCODE, "6151100043219", "EAN-13"),
            isOnline = true
        )
        assertEquals(VerificationStatus.SUSPICIOUS, suspiciousReport.status)

        val fakeReport = scanRepository.verifyAndSaveScan(
            userId = user.id,
            input = VerificationInput(ScanMethod.BARCODE, "0001112223334", "EAN-13"),
            isOnline = true
        )
        assertEquals(VerificationStatus.FAKE, fakeReport.status)

        // Valid EAN-13 check digit (4006381333931) NOT in trusted registry must NEVER return REAL
        val unverifiedValidCheckDigit = scanRepository.verifyAndSaveScan(
            userId = user.id,
            input = VerificationInput(ScanMethod.BARCODE, "4006381333931", "EAN-13"),
            isOnline = true
        )
        assertEquals(VerificationStatus.UNVERIFIED, unverifiedValidCheckDigit.status)
    }

    // 6. QR code scanning
    @Test
    fun `qr verification flags unsafe shortened links and leaves unknown HTTPS links unverified`() = runTest {
        val user = authRepository.register("Kemi Eze", "kemi@example.com", "SecurePass1", "SecurePass1").getOrThrow()

        val realQr = scanRepository.verifyAndSaveScan(
            userId = user.id,
            input = VerificationInput(ScanMethod.QR_CODE, "TC-VERIFY-AMOX-2026", "QR"),
            isOnline = true
        )
        assertEquals(VerificationStatus.REAL, realQr.status)

        val unsafeShortQr = scanRepository.verifyAndSaveScan(
            userId = user.id,
            input = VerificationInput(ScanMethod.QR_CODE, "http://bit.ly/free-meds", "QR"),
            isOnline = true
        )
        assertEquals(VerificationStatus.SUSPICIOUS, unsafeShortQr.status)

        val unknownHttpsQr = scanRepository.verifyAndSaveScan(
            userId = user.id,
            input = VerificationInput(ScanMethod.QR_CODE, "https://manufacturer.example.org/pack/12345", "QR"),
            isOnline = true
        )
        assertEquals(VerificationStatus.UNVERIFIED, unknownHttpsQr.status)
    }

    // 7. Manual security code entry (including UI validation + duplicate redemption detection)
    @Test
    fun `manual security code checks first redemption real repeat suspicious counterfeit fake and unknown unverified`() = runTest {
        val user = authRepository.register("Tunde Bello", "tunde@example.com", "SecurePass1", "SecurePass1").getOrThrow()

        val firstCheck = scanRepository.verifyAndSaveScan(
            userId = user.id,
            input = VerificationInput(ScanMethod.SECURITY_CODE, "TC88429104", "Security PIN"),
            isOnline = true
        )
        assertEquals(VerificationStatus.REAL, firstCheck.status)

        val repeatCheck = scanRepository.verifyAndSaveScan(
            userId = user.id,
            input = VerificationInput(ScanMethod.SECURITY_CODE, "TC88429104", "Security PIN"),
            isOnline = true
        )
        assertEquals(VerificationStatus.SUSPICIOUS, repeatCheck.status)

        val counterfeitCheck = scanRepository.verifyAndSaveScan(
            userId = user.id,
            input = VerificationInput(ScanMethod.SECURITY_CODE, "FAKE99001122", "Security PIN"),
            isOnline = true
        )
        assertEquals(VerificationStatus.FAKE, counterfeitCheck.status)

        val unknownCode = scanRepository.verifyAndSaveScan(
            userId = user.id,
            input = VerificationInput(ScanMethod.SECURITY_CODE, "ZZ99887766", "Security PIN"),
            isOnline = true
        )
        assertEquals(VerificationStatus.UNVERIFIED, unknownCode.status)
    }

    @Test
    fun `security code screen validates empty and short input and submits valid code`() {
        val vm = ScannerViewModel()
        var submittedCode: String? = null

        composeRule.setContent {
            TrueCheckTheme {
                SecurityCodeScreen(
                    scannerViewModel = vm,
                    onBack = {},
                    onSubmitCode = { submittedCode = it }
                )
            }
        }

        // Empty submission shows validation error
        composeRule.onNodeWithTag("verify_product_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Please enter the security code printed on your packaging.").assertIsDisplayed()
        assertNull(submittedCode)

        // Short input shows length error
        composeRule.onNodeWithTag("security_code_input").performTextInput("AB12")
        composeRule.onNodeWithTag("verify_product_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Security code must be 6–24 alphanumeric characters.").assertIsDisplayed()
        assertNull(submittedCode)

        // Valid input submits normalized uppercase code
        composeRule.onNodeWithTag("security_code_input").performTextInput("3456")
        composeRule.onNodeWithTag("verify_product_button").performClick()
        composeRule.waitForIdle()
        assertEquals("AB123456", submittedCode)
    }

    // 8. Product photography & placeholder AI notice
    @Test
    fun `product photo inspection returns labeled placeholder response and never marks genuine from photo alone`() = runTest {
        val user = authRepository.register("Zainab Sani", "zainab@example.com", "SecurePass1", "SecurePass1").getOrThrow()

        val photoUri = Uri.parse("file:///tmp/sample_pack.jpg").toString()
        val inspection = aiInspectionRepository.inspectProductImages(listOf(photoUri), "Front label check").getOrThrow()
        assertTrue(inspection.isPlaceholderResponse)
        assertTrue(inspection.notice.contains("Firebase AI Logic", ignoreCase = true))

        val report = scanRepository.verifyAndSaveScan(
            userId = user.id,
            input = VerificationInput(
                method = ScanMethod.PRODUCT_PHOTO,
                rawValue = "photo_bundle",
                formatLabel = "Packaging Photo",
                imageUriStrings = listOf(photoUri),
                notes = "Seal looks intact"
            ),
            isOnline = true
        )
        assertFalse(report.status == VerificationStatus.REAL)
        assertEquals(VerificationStatus.UNVERIFIED, report.status)
        assertNotNull(report.evidence.aiNotice)
    }

    // Private Admin boundary & analytics verification
    @Test
    fun `admin analytics blocks consumers and serves KPIs to seeded admin`() = runTest {
        val consumer = authRepository.register("User One", "user1@example.com", "Password123", "Password123").getOrThrow()
        val denied = adminAnalyticsRepository.loadSnapshot(consumer.id, daysWindow = 7)
        assertTrue(denied.isFailure)
        assertTrue(denied.exceptionOrNull() is SecurityException)

        val admin = authRepository.login(SampleCatalogSeeder.TRUSTED_ADMIN_EMAIL, "TrueCheckAdmin#2026").getOrThrow()
        assertEquals(UserRole.ADMIN, admin.role)

        val snapshot = adminAnalyticsRepository.loadSnapshot(admin.id, daysWindow = 7).getOrThrow()
        assertTrue(snapshot.totalUsers >= 2)
        assertEquals(7, snapshot.dailyScansSeries.size)
        assertEquals(7, snapshot.dailyActiveUsersSeries.size)

        val suspendResult = adminAnalyticsRepository.setUserStatus(admin.id, consumer.id, AccountStatus.SUSPENDED)
        assertTrue(suspendResult.isSuccess)
        assertTrue(authRepository.login("user1@example.com", "Password123").isFailure)
    }

    @Test
    fun `gemini api key repository falls back when key is placeholder and parses structured response`() = runTest {
        val repo = com.example.domain.ai.GeminiApiKeyInspectionRepository(
            context = context,
            apiKey = "MY_GEMINI_API_KEY",
            fallbackRepository = aiInspectionRepository
        )
        val photoUri = Uri.parse("file:///tmp/sample_pack.jpg").toString()
        val fallbackResult = repo.inspectProductImages(listOf(photoUri), "Check batch").getOrThrow()
        assertTrue(fallbackResult.isPlaceholderResponse)

        val sampleEnvelope = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\"productName\":\"Amoxil 500mg\",\"brand\":\"MedSafe\",\"category\":\"MEDICINES\",\"manufacturerInfo\":\"MedSafe Labs\",\"registrationNumber\":\"A4-0192\",\"batchNumber\":\"B123\",\"expiryDate\":\"2027-08\",\"manufacturingDate\":\"2025-08\",\"visibleCharacteristics\":[\"Clear blister seal\"],\"potentialRedFlags\":[\"Smudged batch print\"],\"imageClarityAcceptable\":true}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = com.example.domain.ai.GeminiApiKeyInspectionRepository.parseGeminiResponseJson(sampleEnvelope)
        assertFalse(parsed.isPlaceholderResponse)
        assertEquals("Amoxil 500mg", parsed.productName)
        assertEquals("MedSafe", parsed.brand)
        assertEquals(com.example.data.model.ProductCategory.MEDICINES, parsed.category)
        assertEquals(listOf("Smudged batch print"), parsed.potentialRedFlags)
    }
}
