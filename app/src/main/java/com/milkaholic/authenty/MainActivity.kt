package com.milkaholic.authenty

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.milkaholic.authenty.domain.OtpUriParser
import com.milkaholic.authenty.presentation.BiometricHelper
import com.milkaholic.authenty.presentation.MainViewModel
import com.milkaholic.authenty.presentation.screens.AddAccountScreen
import com.milkaholic.authenty.presentation.screens.HomeScreen
import com.milkaholic.authenty.presentation.screens.ScanScreen
import com.milkaholic.authenty.ui.theme.AuthentyTheme

class MainActivity : FragmentActivity() {

    // Global Lock State
    private var isUnlocked by mutableStateOf(false)

    // Flag to prevent multiple prompts stacking on top of each other
    private var isPromptShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Security: Block Screenshots
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        // Register for App-Wide Lifecycle Events (Foreground/Background)
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleListener())

        setContent {
            AuthentyTheme {
                Box(modifier = Modifier.fillMaxSize()) {

                    // --- THE APP (Underneath) ---
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        val viewModel: MainViewModel = viewModel()

                        NavHost(navController = navController, startDestination = "home") {
                            composable("home") { HomeScreen(navController, viewModel) }
                            composable("add_account") {
                                AddAccountScreen(navController) { issuer, name, secret ->
                                    viewModel.addAccount(issuer, name, secret)
                                }
                            }
                            composable("scan") {
                                ScanScreen(navController) { scannedUrl ->
                                    val account = OtpUriParser.parse(scannedUrl)
                                    if (account != null) {
                                        viewModel.addAccount(account.issuer, account.name, account.secret)
                                        navController.popBackStack()
                                    }
                                }
                            }
                        }
                    }

                    // --- THE LOCK OVERLAY (On Top) ---
                    if (!isUnlocked) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    // If user taps the locked screen, try auth again
                                    triggerBiometrics()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Locked. Tap to unlock.")
                        }
                    }
                }
            }
        }
    }

    // --- HELPER FUNCTION TO TRIGGER AUTH ---
    private fun triggerBiometrics() {
        if (isUnlocked || isPromptShowing) return // Don't ask if open or already asking

        if (BiometricHelper.isBiometricAvailable(this)) {
            isPromptShowing = true
            BiometricHelper.showPrompt(
                activity = this,
                onSuccess = {
                    isUnlocked = true
                    isPromptShowing = false
                },
                onError = {
                    isUnlocked = false
                    isPromptShowing = false
                    // We stay on the locked screen
                }
            )
        } else {
            // No security setup on phone -> Allow access
            isUnlocked = true
        }
    }

    // --- LIFECYCLE OBSERVER ---
    // This tracks the WHOLE APP, not just the Activity.
    // Solves the "Permission Dialog Crash" and "Inconsistent Locking"
    inner class AppLifecycleListener : DefaultLifecycleObserver {

        // App comes to Foreground (Open App)
        override fun onStart(owner: LifecycleOwner) {
            if (!isUnlocked) {
                triggerBiometrics()
            }
        }

        // App goes to Background (Home Button / Switch Apps)
        override fun onStop(owner: LifecycleOwner) {
            isUnlocked = false
            isPromptShowing = false // Reset flag so it asks again next time
        }
    }
}