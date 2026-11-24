package com.example.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.model.ScanMethod
import com.example.data.model.UserRole
import com.example.data.repository.AppContainer
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.admin.AdminViewModel
import com.example.ui.screens.auth.AuthViewModel
import com.example.ui.screens.auth.ForgotPasswordScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.history.HistoryViewModel
import com.example.ui.screens.history.ScanHistoryScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.onboarding.SplashScreen
import com.example.ui.screens.profile.HelpSupportScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.screens.profile.SettingsScreen
import com.example.ui.screens.scanner.BarcodeScannerScreen
import com.example.ui.screens.scanner.PhotoInspectionScreen
import com.example.ui.screens.scanner.QrScannerScreen
import com.example.ui.screens.scanner.ScannerViewModel
import com.example.ui.screens.scanner.SecurityCodeScreen
import com.example.ui.screens.verification.ProductDetailsScreen
import com.example.ui.screens.verification.VerificationProcessingScreen
import com.example.ui.screens.verification.VerificationResultScreen
import com.example.ui.screens.verification.VerificationViewModel

@Composable
fun TrueCheckNavHost(
    container: AppContainer
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(
        factory = simpleFactory { AuthViewModel(container.authRepository, container.preferencesManager) }
    )
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val onboardingCompleted by authViewModel.onboardingCompleted.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = SplashRoute
    ) {
        composable<SplashRoute> {
            SplashScreen(
                onFinished = {
                    val dest: Any = when {
                        currentUser != null -> HomeRoute
                        !onboardingCompleted -> OnboardingRoute
                        else -> LoginRoute
                    }
                    navController.navigate(dest) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<OnboardingRoute> {
            OnboardingScreen(
                onFinished = {
                    authViewModel.completeOnboarding()
                    navController.navigate(LoginRoute) {
                        popUpTo(OnboardingRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<LoginRoute> {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(HomeRoute) {
                        popUpTo(LoginRoute) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(RegisterRoute) },
                onNavigateToForgotPassword = { navController.navigate(ForgotPasswordRoute) }
            )
        }

        composable<RegisterRoute> {
            RegisterScreen(
                authViewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(HomeRoute) {
                        popUpTo(LoginRoute) { inclusive = true }
                    }
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable<ForgotPasswordRoute> {
            ForgotPasswordScreen(
                authViewModel = authViewModel,
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable<HomeRoute> {
            val user = currentUser
            if (user == null) {
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = {},
                    onNavigateToRegister = { navController.navigate(RegisterRoute) },
                    onNavigateToForgotPassword = { navController.navigate(ForgotPasswordRoute) }
                )
            } else {
                val homeVm: HomeViewModel = viewModel(
                    key = "home_${user.id}",
                    factory = simpleFactory { HomeViewModel(container.scanRepository, user.id) }
                )
                HomeScreen(
                    user = user,
                    homeViewModel = homeVm,
                    onScanBarcode = { navController.navigate(BarcodeScannerRoute) },
                    onScanQrCode = { navController.navigate(QrScannerRoute) },
                    onEnterSecurityCode = { navController.navigate(SecurityCodeRoute) },
                    onScanProductPhoto = { navController.navigate(PhotoInspectionRoute) },
                    onOpenHistory = {
                        navController.navigate(HistoryRoute) {
                            launchSingleTop = true
                        }
                    },
                    onOpenProfile = {
                        navController.navigate(ProfileRoute) {
                            launchSingleTop = true
                        }
                    },
                    onOpenReport = { scanId ->
                        navController.navigate(ProductDetailsRoute(scanId))
                    }
                )
            }
        }

        composable<BarcodeScannerRoute> {
            BarcodeScannerScreen(
                onBack = { navController.popBackStack() },
                onBarcodeDetected = { raw, format ->
                    navController.navigate(
                        VerificationProcessingRoute(
                            method = ScanMethod.BARCODE.name,
                            rawValue = Uri.encode(raw),
                            formatLabel = format
                        )
                    ) {
                        popUpTo(BarcodeScannerRoute) { inclusive = true }
                    }
                },
                onSwitchToManualCode = {
                    navController.navigate(SecurityCodeRoute) {
                        popUpTo(BarcodeScannerRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<QrScannerRoute> {
            QrScannerScreen(
                onBack = { navController.popBackStack() },
                onQrDetected = { raw ->
                    navController.navigate(
                        VerificationProcessingRoute(
                            method = ScanMethod.QR_CODE.name,
                            rawValue = Uri.encode(raw),
                            formatLabel = "QR Code"
                        )
                    ) {
                        popUpTo(QrScannerRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<SecurityCodeRoute> {
            val scannerVm: ScannerViewModel = viewModel()
            SecurityCodeScreen(
                scannerViewModel = scannerVm,
                onBack = { navController.popBackStack() },
                onSubmitCode = { code ->
                    navController.navigate(
                        VerificationProcessingRoute(
                            method = ScanMethod.SECURITY_CODE.name,
                            rawValue = Uri.encode(code),
                            formatLabel = "Security PIN"
                        )
                    ) {
                        popUpTo(SecurityCodeRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<PhotoInspectionRoute> {
            val scannerVm: ScannerViewModel = viewModel()
            PhotoInspectionScreen(
                scannerViewModel = scannerVm,
                onBack = { navController.popBackStack() },
                onSubmitPhotos = { urisJson, notes ->
                    navController.navigate(
                        VerificationProcessingRoute(
                            method = ScanMethod.PRODUCT_PHOTO.name,
                            rawValue = "photo_bundle",
                            formatLabel = "Packaging Photo",
                            imageUrisJson = Uri.encode(urisJson),
                            notes = Uri.encode(notes)
                        )
                    ) {
                        popUpTo(PhotoInspectionRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<VerificationProcessingRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<VerificationProcessingRoute>()
            val user = currentUser
            if (user != null) {
                val verificationVm: VerificationViewModel = viewModel(
                    key = "verify_${user.id}_${route.method}_${route.rawValue}",
                    factory = simpleFactory {
                        VerificationViewModel(container.scanRepository) { container.isNetworkAvailable() }
                    }
                )
                VerificationProcessingScreen(
                    userId = user.id,
                    methodName = route.method,
                    rawValue = Uri.decode(route.rawValue),
                    formatLabel = route.formatLabel,
                    imageUrisJson = Uri.decode(route.imageUrisJson),
                    notes = Uri.decode(route.notes),
                    verificationViewModel = verificationVm,
                    onCompleted = { scanId ->
                        navController.navigate(VerificationResultRoute(scanId)) {
                            popUpTo(VerificationProcessingRoute::class) { inclusive = true }
                        }
                    },
                    onCancel = {
                        navController.navigate(HomeRoute) {
                            popUpTo(HomeRoute) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable<VerificationResultRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<VerificationResultRoute>()
            val user = currentUser
            if (user != null) {
                val verificationVm: VerificationViewModel = viewModel(
                    key = "result_${user.id}_${route.scanId}",
                    factory = simpleFactory {
                        VerificationViewModel(container.scanRepository) { container.isNetworkAvailable() }
                    }
                )
                VerificationResultScreen(
                    userId = user.id,
                    scanId = route.scanId,
                    verificationViewModel = verificationVm,
                    onScanAnother = {
                        navController.navigate(BarcodeScannerRoute) {
                            popUpTo(VerificationResultRoute::class) { inclusive = true }
                        }
                    },
                    onViewDetails = { scanId ->
                        navController.navigate(ProductDetailsRoute(scanId))
                    },
                    onBackToHome = {
                        navController.navigate(HomeRoute) {
                            popUpTo(HomeRoute) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable<ProductDetailsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ProductDetailsRoute>()
            val user = currentUser
            if (user != null) {
                val verificationVm: VerificationViewModel = viewModel(
                    key = "details_${user.id}_${route.scanId}",
                    factory = simpleFactory {
                        VerificationViewModel(container.scanRepository) { container.isNetworkAvailable() }
                    }
                )
                ProductDetailsScreen(
                    userId = user.id,
                    scanId = route.scanId,
                    verificationViewModel = verificationVm,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable<HistoryRoute> {
            val user = currentUser
            if (user != null) {
                val historyVm: HistoryViewModel = viewModel(
                    key = "history_${user.id}",
                    factory = simpleFactory { HistoryViewModel(container.scanRepository, user.id) }
                )
                ScanHistoryScreen(
                    historyViewModel = historyVm,
                    onOpenReport = { scanId -> navController.navigate(ProductDetailsRoute(scanId)) },
                    onNavigateHome = {
                        navController.navigate(HomeRoute) {
                            popUpTo(HomeRoute) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onNavigateProfile = {
                        navController.navigate(ProfileRoute) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable<ProfileRoute> {
            val user = currentUser
            if (user != null) {
                ProfileScreen(
                    user = user,
                    onNavigateHome = {
                        navController.navigate(HomeRoute) {
                            popUpTo(HomeRoute) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onNavigateHistory = {
                        navController.navigate(HistoryRoute) {
                            launchSingleTop = true
                        }
                    },
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                    onOpenHelp = { navController.navigate(HelpSupportRoute) },
                    onOpenAdminDashboard = {
                        if (user.role == UserRole.ADMIN) {
                            navController.navigate(AdminDashboardRoute)
                        }
                    },
                    onLogout = {
                        authViewModel.logout {
                            navController.navigate(LoginRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }

        composable<SettingsRoute> {
            val user = currentUser
            if (user != null) {
                val profileVm: ProfileViewModel = viewModel(
                    key = "profile_${user.id}",
                    factory = simpleFactory {
                        ProfileViewModel(container.authRepository, container.preferencesManager, user.id)
                    }
                )
                SettingsScreen(
                    user = user,
                    profileViewModel = profileVm,
                    onBack = { navController.popBackStack() },
                    onOpenHelp = { navController.navigate(HelpSupportRoute) },
                    onLogout = {
                        authViewModel.logout {
                            navController.navigate(LoginRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }

        composable<HelpSupportRoute> {
            HelpSupportScreen(onBack = { navController.popBackStack() })
        }

        composable<AdminDashboardRoute> {
            val user = currentUser
            if (user != null) {
                val adminVm: AdminViewModel = viewModel(
                    key = "admin_${user.id}",
                    factory = simpleFactory {
                        AdminViewModel(container.adminAnalyticsRepository, user.id)
                    }
                )
                AdminDashboardScreen(
                    actingUser = user,
                    adminViewModel = adminVm,
                    onExitToConsumer = { navController.popBackStack() },
                    onLogout = {
                        authViewModel.logout {
                            navController.navigate(LoginRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T : ViewModel> simpleFactory(creator: () -> T): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
            return creator() as VM
        }
    }
}
