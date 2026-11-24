package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.local.SampleCatalogSeeder
import com.example.domain.ai.AiInspectionRepository
import com.example.domain.ai.GeminiApiKeyInspectionRepository
import com.example.domain.ai.PlaceholderAiInspectionRepository
import com.example.domain.verification.VerificationEngine

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: AppDatabase = AppDatabase.getInstance(appContext)
    val preferencesManager: PreferencesManager = PreferencesManager(appContext)

    private val placeholderAiInspectionRepository = PlaceholderAiInspectionRepository(appContext)
    val aiInspectionRepository: AiInspectionRepository = GeminiApiKeyInspectionRepository(
        context = appContext,
        apiKey = BuildConfig.GEMINI_API_KEY,
        fallbackRepository = placeholderAiInspectionRepository
    )

    val activityTrackingRepository = ActivityTrackingRepository(
        userActivityDao = database.userActivityDao(),
        userDao = database.userDao()
    )

    val verificationEngine = VerificationEngine(
        productCatalogDao = database.productCatalogDao(),
        securityCodeDao = database.securityCodeDao(),
        aiInspectionRepository = aiInspectionRepository
    )

    val authRepository = AuthRepository(
        userDao = database.userDao(),
        scanDao = database.scanDao(),
        userActivityDao = database.userActivityDao(),
        notificationDao = database.notificationDao(),
        preferencesManager = preferencesManager,
        activityTrackingRepository = activityTrackingRepository
    )

    val scanRepository = ScanRepository(
        scanDao = database.scanDao(),
        notificationDao = database.notificationDao(),
        offlineScanQueueDao = database.offlineScanQueueDao(),
        preferencesManager = preferencesManager,
        verificationEngine = verificationEngine,
        activityTrackingRepository = activityTrackingRepository
    )

    val adminAnalyticsRepository = AdminAnalyticsRepository(
        userDao = database.userDao(),
        scanDao = database.scanDao(),
        userActivityDao = database.userActivityDao()
    )

    suspend fun initializeSeedData() {
        SampleCatalogSeeder.seedIfNeeded(
            userDao = database.userDao(),
            productCatalogDao = database.productCatalogDao(),
            securityCodeDao = database.securityCodeDao()
        )
    }

    fun isNetworkAvailable(): Boolean {
        return try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }
}
