package com.milkaholic.authenty.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.milkaholic.authenty.data.PinManager
import com.milkaholic.authenty.data.SecurityEvent
import com.milkaholic.authenty.data.SecurityEventSeverity
import com.milkaholic.authenty.data.SecuritySummary
import com.milkaholic.authenty.domain.*
import com.milkaholic.authenty.presentation.components.PinSetupScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.milkaholic.authenty.ui.theme.GradientBackground
import com.milkaholic.authenty.ui.theme.GradientCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    navController: NavController,
    openDrawer: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Initialize security components
    val securityManager = remember { SecurityManager.getInstance(context) }
    val pinManager = remember { PinManager(context) }
    val autoLockManager = remember { AutoLockManager.getInstance(context, securityManager) }
    val deviceSecurityChecker = remember { DeviceSecurityChecker(context) }
    
    var showPinSetup by remember { mutableStateOf(false) }
    var pinSetupMode by remember { mutableStateOf(PinSetupMode.SET_PRIMARY) }
    var originalPin by remember { mutableStateOf<String?>(null) }
    var securitySummary by remember { mutableStateOf<SecuritySummary?>(null) }
    var securityReport by remember { mutableStateOf<SecurityCheckReport?>(null) }

    // Load security data
    LaunchedEffect(Unit) {
        scope.launch {
            val summaryResult = withContext(Dispatchers.IO) {
                securityManager.getSecuritySummary()
            }
            if (summaryResult is AuthentyResult.Success) {
                securitySummary = summaryResult.data
            }
            
            securityReport = withContext(Dispatchers.IO) {
                deviceSecurityChecker.performComprehensiveSecurityCheck()
            }
        }
    }

    if (showPinSetup) {
        when (pinSetupMode) {
            PinSetupMode.SET_PRIMARY -> {
                PinSetupScreen(
                    title = "Set Primary PIN",
                    subtitle = "Choose a 4-6 digit PIN for authentication",
                    onPinSet = { pin ->
                        originalPin = pin
                        pinSetupMode = PinSetupMode.CONFIRM_PRIMARY
                    },
                    onCancel = { showPinSetup = false }
                )
            }
            PinSetupMode.CONFIRM_PRIMARY -> {
                PinSetupScreen(
                    title = "Confirm PIN",
                    subtitle = "Enter your PIN again to confirm",
                    onPinSet = { pin ->
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                pinManager.setPrimaryPin(pin)
                            }
                            if (result is AuthentyResult.Success) {
                                showPinSetup = false
                            }
                        }
                    },
                    onCancel = { 
                        pinSetupMode = PinSetupMode.SET_PRIMARY
                        originalPin = null
                    },
                    confirmPin = true,
                    originalPin = originalPin
                )
            }
            PinSetupMode.SET_BACKUP -> {
                PinSetupScreen(
                    title = "Set Backup PIN",
                    subtitle = "Choose a different PIN as backup",
                    onPinSet = { pin ->
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                pinManager.setBackupPin(pin)
                            }
                            if (result is AuthentyResult.Success) {
                                showPinSetup = false
                            }
                        }
                    },
                    onCancel = { showPinSetup = false }
                )
            }
            PinSetupMode.SET_DURESS -> {
                PinSetupScreen(
                    title = "Set Duress PIN",
                    subtitle = "Choose a PIN that will trigger Duress Mode (wipe data view)",
                    onPinSet = { pin ->
                        originalPin = pin
                        pinSetupMode = PinSetupMode.CONFIRM_DURESS
                    },
                    onCancel = { showPinSetup = false }
                )
            }
            PinSetupMode.CONFIRM_DURESS -> {
                PinSetupScreen(
                    title = "Confirm Duress PIN",
                    subtitle = "Enter your Duress PIN again to confirm",
                    onPinSet = { pin ->
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                pinManager.setDuressPin(pin)
                            }
                            if (result is AuthentyResult.Success) {
                                showPinSetup = false
                            }
                        }
                    },
                    onCancel = { 
                        pinSetupMode = PinSetupMode.SET_DURESS
                        originalPin = null
                    },
                    confirmPin = true,
                    originalPin = originalPin
                )
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Settings") },
                navigationIcon = {
                    IconButton(onClick = openDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { paddingValues ->
        GradientBackground(modifier = Modifier.padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Security Score Card
            item {
                SecurityScoreCard(
                    securityReport = securityReport,
                    securitySummary = securitySummary
                )
            }

            // Authentication Settings
            item {
                AuthenticationSettingsCard(
                    pinManager = pinManager,
                    onSetupPrimaryPin = { 
                        pinSetupMode = PinSetupMode.SET_PRIMARY
                        showPinSetup = true
                    },
                    onSetupBackupPin = { 
                        pinSetupMode = PinSetupMode.SET_BACKUP
                        showPinSetup = true
                    },
                    onSetupDuressPin = {
                        pinSetupMode = PinSetupMode.SET_DURESS
                        showPinSetup = true
                    }
                )
            }

            // Auto-lock Settings
            item {
                AutoLockSettingsCard(
                    autoLockManager = autoLockManager
                )
            }

            // Security Reports
            item {
                SecurityReportsCard(
                    onViewReports = {
                        navController.navigate("security_reports")
                    },
                    onViewEvents = {
                        navController.navigate("security_events")
                    }
                )
            }

            // Device Security
            item {
                DeviceSecurityCard(
                    securityReport = securityReport,
                    onRefresh = {
                        scope.launch {
                            securityReport = withContext(Dispatchers.IO) {
                                deviceSecurityChecker.performComprehensiveSecurityCheck()
                            }
                        }
                    }
                )
            }
        }
    }
}
}

@Composable
fun SecurityScoreCard(
    securityReport: SecurityCheckReport?,
    securitySummary: SecuritySummary?
) {
    GradientCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Security Score",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                securityReport?.let { report ->
                    val color = when (report.overallRisk) {
                        RiskLevel.LOW -> Color.Green
                        RiskLevel.MEDIUM -> Color.Yellow
                        RiskLevel.HIGH -> Color(0xFFFF9800)
                        RiskLevel.CRITICAL -> Color.Red
                    }
                    
                    Text(
                        text = "${report.securityScore}/100",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            securitySummary?.let { summary ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SecurityStat("Total Events", summary.totalEvents.toString())
                    SecurityStat("Security Threats", summary.securityThreats.toString())
                    SecurityStat("Recent Failures", summary.recentFailures.toString())
                }
            }
        }
    }
}

@Composable
fun SecurityStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AuthenticationSettingsCard(
    pinManager: PinManager,
    onSetupPrimaryPin: () -> Unit,
    onSetupBackupPin: () -> Unit,
    onSetupDuressPin: () -> Unit
) {
    // Load duress pin state asynchronously to avoid blocking UI
    var isDuressSet by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            isDuressSet = pinManager.isDuressPinSet()
        }
    }

    GradientCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Authentication",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            // Info about device security
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Device Security Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "App is secured with biometric authentication and device PIN fallback",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                icon = Icons.Default.Fingerprint,
                title = "Biometric Authentication",
                subtitle = "Primary security method using fingerprint/face unlock",
                onClick = { /* Info only */ },
                trailing = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = Color.Green
                    )
                }
            )

            SettingsItem(
                icon = Icons.Default.Pin,
                title = "Device PIN Fallback",
                subtitle = "Your device PIN serves as backup authentication",
                onClick = { /* Info only */ },
                trailing = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = Color.Green
                    )
                }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Duress PIN
            SettingsItem(
                icon = Icons.Default.Warning,
                title = "Duress PIN",
                subtitle = if (isDuressSet) "Duress PIN is set" else "Set a PIN to wipe data view in emergency",
                onClick = onSetupDuressPin,
                trailing = {
                    if (isDuressSet) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Set",
                            tint = Color.Green
                        )
                    } else {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Set Duress PIN",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun AutoLockSettingsCard(
    autoLockManager: AutoLockManager
) {
    var autoLockEnabled by remember { mutableStateOf(autoLockManager.isAutoLockEnabled()) }
    var lockOnSwitch by remember { mutableStateOf(autoLockManager.shouldLockOnAppSwitch()) }
    var currentTimeout by remember { mutableStateOf(autoLockManager.getAutoLockTimeout()) }
    var showTimeoutSelector by remember { mutableStateOf(false) }

    GradientCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Auto-lock",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            SettingsItem(
                icon = Icons.Default.Lock,
                title = "Auto-lock Enabled",
                subtitle = if (autoLockEnabled) "App will lock automatically" else "Auto-lock disabled",
                onClick = { /* Handle in trailing */ },
                trailing = {
                    Switch(
                        checked = autoLockEnabled,
                        onCheckedChange = { enabled ->
                            autoLockEnabled = enabled
                            autoLockManager.setAutoLockEnabled(enabled)
                        }
                    )
                }
            )

            if (autoLockEnabled) {
                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = "Auto-lock Timeout",
                    subtitle = AutoLockManager.TimeoutPresets.getDisplayName(currentTimeout),
                    onClick = { showTimeoutSelector = true }
                )

                SettingsItem(
                    icon = Icons.Default.SwapHoriz,
                    title = "Lock on App Switch",
                    subtitle = if (lockOnSwitch) "Lock when switching apps" else "Don't lock on app switch",
                    onClick = { /* Handle in trailing */ },
                    trailing = {
                        Switch(
                            checked = lockOnSwitch,
                            onCheckedChange = { enabled ->
                                lockOnSwitch = enabled
                                autoLockManager.setLockOnAppSwitch(enabled)
                            }
                        )
                    }
                )
            }
        }
    }

    // Timeout Selector Dialog
    if (showTimeoutSelector) {
        AlertDialog(
            onDismissRequest = { showTimeoutSelector = false },
            title = { Text("Auto-lock Timeout") },
            text = {
                LazyColumn {
                    items(AutoLockManager.TimeoutPresets.getAllPresets()) { (displayName, timeoutValue) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentTimeout = timeoutValue
                                    autoLockManager.setAutoLockTimeout(timeoutValue)
                                    showTimeoutSelector = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentTimeout == timeoutValue,
                                onClick = {
                                    currentTimeout = timeoutValue
                                    autoLockManager.setAutoLockTimeout(timeoutValue)
                                    showTimeoutSelector = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimeoutSelector = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SecurityReportsCard(
    onViewReports: () -> Unit,
    onViewEvents: () -> Unit
) {
    GradientCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Security Reports",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            SettingsItem(
                icon = Icons.Default.Assessment,
                title = "View Security Reports",
                subtitle = "Detailed security analysis",
                onClick = onViewReports
            )

            SettingsItem(
                icon = Icons.Default.History,
                title = "Security Event Log",
                subtitle = "View all security events",
                onClick = onViewEvents
            )
        }
    }
}

@Composable
fun DeviceSecurityCard(
    securityReport: SecurityCheckReport?,
    onRefresh: () -> Unit
) {
    GradientCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Device Security",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            securityReport?.let { report ->
                Text(
                    text = "Overall Risk: ${report.overallRisk.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = when (report.overallRisk) {
                        RiskLevel.LOW -> Color.Green
                        RiskLevel.MEDIUM -> Color.Yellow
                        RiskLevel.HIGH -> Color(0xFFFF9800)
                        RiskLevel.CRITICAL -> Color.Red
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${report.passedChecks}/${report.totalChecks} security checks passed",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (report.recommendations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Recommendations:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    report.recommendations.take(2).forEach { recommendation ->
                        Text(
                            text = "• $recommendation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        trailing?.invoke()
    }
}

enum class PinSetupMode {
    SET_PRIMARY,
    CONFIRM_PRIMARY,
    SET_BACKUP,
    SET_DURESS,
    CONFIRM_DURESS
}