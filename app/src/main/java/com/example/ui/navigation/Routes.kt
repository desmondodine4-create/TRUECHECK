package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute

@Serializable
data object OnboardingRoute

@Serializable
data object LoginRoute

@Serializable
data object RegisterRoute

@Serializable
data object ForgotPasswordRoute

@Serializable
data object HomeRoute

@Serializable
data object BarcodeScannerRoute

@Serializable
data object QrScannerRoute

@Serializable
data object SecurityCodeRoute

@Serializable
data object PhotoInspectionRoute

@Serializable
data class VerificationProcessingRoute(
    val method: String,
    val rawValue: String,
    val formatLabel: String = "",
    val imageUrisJson: String = "[]",
    val notes: String = ""
)

@Serializable
data class VerificationResultRoute(val scanId: String)

@Serializable
data object HistoryRoute

@Serializable
data class ProductDetailsRoute(val scanId: String)

@Serializable
data object ProfileRoute

@Serializable
data object SettingsRoute

@Serializable
data object HelpSupportRoute

@Serializable
data object AdminDashboardRoute
