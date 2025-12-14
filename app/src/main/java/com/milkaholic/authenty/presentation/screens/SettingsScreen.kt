package com.milkaholic.authenty.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.milkaholic.authenty.presentation.components.BackupPasswordDialog
import com.milkaholic.authenty.ui.theme.GradientBackground
import com.milkaholic.authenty.presentation.MainViewModel
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.milkaholic.authenty.data.PinManager
import com.milkaholic.authenty.domain.SecurityManager
import com.milkaholic.authenty.domain.AuthentyResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    openDrawer: () -> Unit,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPasswordDialog by remember { mutableStateOf(false) }
    var isExportOperation by remember { mutableStateOf(true) }
    var pendingExportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var settingsTapCount by remember { mutableStateOf(0) }
    var showDuressPinDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        try {
            uri?.let { 
                pendingExportUri = it
                isExportOperation = true
                showPasswordDialog = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context, 
                "Error opening file picker: ${e.message}", 
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        try {
            uri?.let {
                pendingImportUri = it
                isExportOperation = false
                showPasswordDialog = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context, 
                "Error opening file picker: ${e.message}", 
                Toast.LENGTH_LONG
            ).show()
        }
    }

    if (showPasswordDialog) {
        BackupPasswordDialog(
            isExport = isExportOperation,
            onDismiss = { showPasswordDialog = false },
            onConfirm = { password ->
                showPasswordDialog = false
                try {
                    if (isExportOperation) {
                        pendingExportUri?.let { 
                            viewModel.exportBackup(password, it)
                            Toast.makeText(
                                context, 
                                "Export started... Check for errors in logs if it fails", 
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        pendingImportUri?.let { 
                            viewModel.importBackup(password, it)
                            Toast.makeText(
                                context, 
                                "Import started...", 
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        context, 
                        "Error: ${e.message ?: "Unknown error occurred"}", 
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    if (showDuressPinDialog) {
        DuressPinExitDialog(
            onDismiss = { 
                showDuressPinDialog = false
                settingsTapCount = 0
            },
            onConfirm = { pin ->
                showDuressPinDialog = false
                settingsTapCount = 0
                
                scope.launch {
                    try {
                        val pinManager = PinManager(context)
                        val result = pinManager.verifyDuressPin(pin)
                        
                        if (result is AuthentyResult.Success && result.data) {
                            val securityManager = SecurityManager.getInstance(context)
                            securityManager.clearDuressMode()
                            viewModel.loadAccounts()
                            Toast.makeText(context, "Panic mode disabled", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Incorrect duress PIN", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = openDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { padding ->
        GradientBackground(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Dark Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dark Mode")
                Switch(
                    checked = viewModel.isDarkMode.value,
                    onCheckedChange = { viewModel.toggleTheme(it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = "Backup & Restore",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Export Backup
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        try {
                            exportLauncher.launch("authenty_backup.enc")
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(
                                context, 
                                "Error: ${e.message ?: "Cannot open file picker"}", 
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = "Export Backup",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Export Backup",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Save encrypted backup to file",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Import Backup
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        try {
                            importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(
                                context, 
                                "Error: ${e.message ?: "Cannot open file picker"}", 
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Import Backup",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Import Backup",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Restore from encrypted backup file",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = "Security",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Security Settings Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        navController.navigate("security_settings") {
                            launchSingleTop = true
                        }
                    }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Security Settings",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "PIN, auto-lock, and security reports",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Go to Security Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Credits section with secret duress exit
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = {
                            settingsTapCount++
                            if (settingsTapCount >= 7) {
                                showDuressPinDialog = true
                            }
                        },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Made by Dev Team",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version 1.0",
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
fun DuressPinExitDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exit Panic Mode") },
        text = {
            Column {
                Text("Enter duress PIN to restore your accounts.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 12 && it.all { char -> char.isDigit() }) pin = it },
                    label = { Text("Duress PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pin) },
                enabled = pin.length >= 4
            ) {
                Text("Verify")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}