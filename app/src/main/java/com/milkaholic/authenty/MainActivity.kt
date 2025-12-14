package com.milkaholic.authenty

import android.app.Application
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.milkaholic.authenty.data.PinManager
import com.milkaholic.authenty.domain.AutoLockManager
import com.milkaholic.authenty.domain.OtpUriParser
import com.milkaholic.authenty.domain.SecurityManager
import com.milkaholic.authenty.presentation.BiometricHelper
import com.milkaholic.authenty.presentation.MainViewModel
import com.milkaholic.authenty.presentation.components.AppDrawer
import com.milkaholic.authenty.presentation.screens.AddAccountScreen
import com.milkaholic.authenty.presentation.screens.HomeScreen
import com.milkaholic.authenty.presentation.screens.ProfileManagementScreen
import com.milkaholic.authenty.presentation.screens.ScanScreen
import com.milkaholic.authenty.presentation.screens.SecurityReportsScreen
import com.milkaholic.authenty.presentation.screens.SecuritySettingsScreen
import com.milkaholic.authenty.presentation.screens.SettingsScreen
import com.milkaholic.authenty.ui.theme.AuthentyTheme
import kotlinx.coroutines.launch

import com.milkaholic.authenty.presentation.components.PinEntryScreen
import com.milkaholic.authenty.data.PinVerificationResult
import com.milkaholic.authenty.domain.AuthentyResult

class MainActivity : FragmentActivity() {

    private var isUnlocked by mutableStateOf(false)
    private var showPinEntry by mutableStateOf(false)
    private var pinError by mutableStateOf<String?>(null)
    private var isPromptShowing = false
    
    private lateinit var securityManager: SecurityManager
    private lateinit var autoLockManager: AutoLockManager
    private lateinit var biometricHelper: BiometricHelper
    private lateinit var pinManager: PinManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        
        // Initialize security components
        securityManager = SecurityManager.getInstance(this)
        pinManager = PinManager(this)
        autoLockManager = AutoLockManager.getInstance(this, securityManager)
        biometricHelper = BiometricHelper.getInstance(this, securityManager, autoLockManager, pinManager)
        
        // Initialize security system
        securityManager.initialize()
        autoLockManager.initialize()
        
        // Set auto-lock callback
        autoLockManager.onAutoLockTriggered = {
            isUnlocked = false
            isPromptShowing = false
        }
        
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleListener())
        lifecycle.addObserver(autoLockManager)

        setContent {
            val context = LocalContext.current
            val viewModel: MainViewModel = viewModel { MainViewModel(context.applicationContext as Application) }
            // Observe the theme state from ViewModel
            val darkTheme by viewModel.isDarkMode

            // Pass the darkTheme boolean to your Theme wrapper
            AuthentyTheme(darkTheme = darkTheme) {

                Box(modifier = Modifier.fillMaxSize()) {

                    // --- APP CONTENT ---
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                        val scope = rememberCoroutineScope()

                        // Wrap navigation in the Drawer
                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            drawerContent = {
                                AppDrawer(
                                    navController = navController,
                                    closeDrawer = { scope.launch { drawerState.close() } },
                                    onPanic = { securityManager.triggerDuressMode() }
                                )
                            }
                        ) {
                            NavHost(navController = navController, startDestination = "home") {

                                composable("home") {
                                    HomeScreen(
                                        navController = navController,
                                        viewModel = viewModel,
                                        openDrawer = { scope.launch { drawerState.open() } }
                                    )
                                }

                                composable("settings") {
                                    SettingsScreen(
                                        navController = navController,
                                        openDrawer = { scope.launch { drawerState.open() } },
                                        viewModel = viewModel
                                    )
                                }

                                composable("security_settings") {
                                    SecuritySettingsScreen(
                                        navController = navController,
                                        openDrawer = { scope.launch { drawerState.open() } }
                                    )
                                }

                                composable("security_reports") {
                                    SecurityReportsScreen(
                                        navController = navController,
                                        openDrawer = { scope.launch { drawerState.open() } }
                                    )
                                }

                                composable("security_events") {
                                    SecurityReportsScreen(
                                        navController = navController,
                                        openDrawer = { scope.launch { drawerState.open() } }
                                    )
                                }

                                composable("profile_management") {
                                    ProfileManagementScreen(
                                        navController = navController,
                                        openDrawer = { scope.launch { drawerState.open() } }
                                    )
                                }

                                composable("add_account") {
                                    AddAccountScreen(navController) { issuer, name, secret, algorithm, digits, period ->
                                        viewModel.addAccount(name, issuer, secret, "Personal", algorithm, digits, period)
                                    }
                                }

                                composable("scan") {
                                    ScanScreen(navController) { scannedUrl ->
                                        val result = OtpUriParser.parseWithResult(scannedUrl)
                                        result.onSuccess { account ->
                                            viewModel.addAccount(
                                                name = account.name, 
                                                issuer = account.issuer, 
                                                secret = account.secret,
                                                algorithm = account.safeAlgorithm,
                                                digits = account.safeDigits,
                                                period = account.safePeriod
                                            )
                                            navController.popBackStack()
                                        }.onError { error ->
                                            viewModel.showError(error)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- LOCK OVERLAY ---
                    if (!isUnlocked) {
                        if (showPinEntry) {
                            PinEntryScreen(
                                title = "Unlock Authenty",
                                onPinEntered = { pin ->
                                    val result = biometricHelper.verifyPin(pin)
                                    if (result is AuthentyResult.Success && result.data) {
                                        unlockApp()
                                    } else {
                                        pinError = "Incorrect PIN"
                                    }
                                },
                                onCancel = {
                                    showPinEntry = false
                                    isPromptShowing = false
                                },
                                onBiometricRequested = {
                                    triggerBiometrics()
                                },
                                showBiometricOption = biometricHelper.isBiometricAvailable(),
                                isError = pinError != null,
                                errorMessage = pinError
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { triggerBiometrics() },
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    modifier = Modifier.padding(32.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Authenty is Locked",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Tap to unlock with biometric or PIN",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun triggerBiometrics() {
        // 1. If already unlocked OR a prompt is already on screen (and not showing PIN), do nothing.
        if (isUnlocked || (isPromptShowing && !showPinEntry)) return

        // 2. Check hardware and lockout status
        if (biometricHelper.shouldUseBiometric()) {
            isPromptShowing = true
            showPinEntry = false // Hide PIN if showing biometric

            biometricHelper.showAuthenticationPrompt(
                activity = this,
                onSuccess = { method ->
                    // Auth Good: Unlock and reset flag
                    unlockApp()
                },
                onError = { error ->
                    // Auth Failed/Cancelled
                    isPromptShowing = false
                    
                    // Handle PIN fallback if needed
                    if (error == "pin_fallback_needed" || biometricHelper.shouldUsePinFallback()) {
                        showPinEntry = true
                        isPromptShowing = true
                    }
                }
            )
        } else if (biometricHelper.shouldUsePinFallback()) {
            showPinEntry = true
            isPromptShowing = true
        } else {
            // No security methods available -> Auto unlock
            unlockApp()
        }
    }

    private fun unlockApp() {
        isUnlocked = true
        showPinEntry = false
        isPromptShowing = false
        pinError = null
        autoLockManager.updateActivity()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::autoLockManager.isInitialized) {
            autoLockManager.cleanup()
        }
    }

    inner class AppLifecycleListener : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) { 
            if (!isUnlocked) triggerBiometrics() 
        }
        override fun onStop(owner: LifecycleOwner) { 
            isUnlocked = false
            isPromptShowing = false 
        }
    }
}