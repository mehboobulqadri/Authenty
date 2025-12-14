package com.milkaholic.authenty.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.milkaholic.authenty.domain.ValidationUtils
import com.milkaholic.authenty.domain.Base32

import com.milkaholic.authenty.ui.theme.GradientBackground
import com.milkaholic.authenty.ui.theme.BouncyButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    navController: NavController,
    onSave: (String, String, String, String, Int, Int) -> Unit
) {
    var issuer by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var secretVisible by remember { mutableStateOf(false) }
    
    // Advanced settings
    var algorithm by remember { mutableStateOf("SHA1") }
    var digits by remember { mutableStateOf("6") }
    var period by remember { mutableStateOf("30") }
    var showAdvanced by remember { mutableStateOf(false) }
    
    // Validation states
    var issuerError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var secretError by remember { mutableStateOf<String?>(null) }
    
    // Validation functions
    fun validateIssuer(value: String) {
        issuerError = ValidationUtils.validateIssuer(value.trim())?.message
    }
    
    fun validateName(value: String) {
        nameError = ValidationUtils.validateAccountName(value.trim())?.message
    }
    
    fun validateSecret(value: String) {
        secretError = ValidationUtils.validateBase32Secret(value.trim())?.message
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Account Manually") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        GradientBackground(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                // Help text
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Enter your account details manually. You can find the secret key in your service's 2FA settings.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            OutlinedTextField(
                value = issuer,
                onValueChange = { 
                    issuer = it
                    if (issuerError != null) validateIssuer(it)
                },
                label = { Text("Issuer (e.g. Google, GitHub)") },
                modifier = Modifier.fillMaxWidth(),
                isError = issuerError != null,
                supportingText = issuerError?.let { { Text(it) } },
                onBlur = { validateIssuer(issuer) }
            )

            OutlinedTextField(
                value = name,
                onValueChange = { 
                    name = it
                    if (nameError != null) validateName(it)
                },
                label = { Text("Account Name (e.g. user@gmail.com)") },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                onBlur = { validateName(name) }
            )

            OutlinedTextField(
                value = secret,
                onValueChange = { 
                    secret = it.uppercase().replace(" ", "").replace("-", "")
                    if (secretError != null) validateSecret(it)
                },
                label = { Text("Secret Key (Base32)") },
                modifier = Modifier.fillMaxWidth(),
                isError = secretError != null,
                supportingText = secretError?.let { { Text(it) } } ?: { 
                    Text("16-32 characters, letters A-Z and numbers 2-7 only") 
                },
                visualTransformation = if (secretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { secretVisible = !secretVisible }) {
                        Icon(
                            if (secretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (secretVisible) "Hide secret" else "Show secret"
                        )
                    }
                },
                onBlur = { validateSecret(secret) }
            )
            
            // Real-time validation indicator
            if (secret.isNotEmpty()) {
                val isValidBase32 = Base32.isValidBase32(secret)
                Row {
                    Icon(
                        imageVector = if (isValidBase32) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isValidBase32) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isValidBase32) "Valid Base32 secret" else "Invalid Base32 format",
                        color = if (isValidBase32) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Advanced Options Toggle
            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Text(if (showAdvanced) "Hide Advanced Options" else "Show Advanced Options")
            }

            if (showAdvanced) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Algorithm Selection
                        Text("Algorithm", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("SHA1", "SHA256", "SHA512").forEach { alg ->
                                FilterChip(
                                    selected = algorithm == alg,
                                    onClick = { algorithm = alg },
                                    label = { Text(alg) }
                                )
                            }
                        }

                        // Digits Selection
                        Text("Digits", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("6", "8").forEach { d ->
                                FilterChip(
                                    selected = digits == d,
                                    onClick = { digits = d },
                                    label = { Text(d) }
                                )
                            }
                        }

                        // Period Input
                        OutlinedTextField(
                            value = period,
                            onValueChange = { if (it.all { char -> char.isDigit() }) period = it },
                            label = { Text("Period (seconds)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            BouncyButton(
                onClick = {
                    // Validate all fields
                    validateIssuer(issuer)
                    validateName(name)
                    validateSecret(secret)
                    
                    // Only save if all validations pass
                    if (issuerError == null && nameError == null && secretError == null && 
                        issuer.isNotBlank() && name.isNotBlank() && secret.isNotBlank()) {
                        
                        val digitsInt = digits.toIntOrNull() ?: 6
                        val periodInt = period.toIntOrNull() ?: 30
                        
                        onSave(issuer.trim(), name.trim(), secret.trim(), algorithm, digitsInt, periodInt)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = issuer.isNotBlank() && name.isNotBlank() && secret.isNotBlank()
            ) {
                Text("Save Account")
            }
        }
    }
}
}

// Extension for onBlur-like behavior
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    onBlur: () -> Unit = {}
) {
    var hasFocus by remember { mutableStateOf(false) }
    
    LaunchedEffect(hasFocus) {
        if (!hasFocus && value.isNotEmpty()) {
            onBlur()
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .onFocusChanged { focusState ->
                val newFocus = focusState.isFocused
                if (hasFocus && !newFocus) {
                    onBlur()
                }
                hasFocus = newFocus
            },
        label = label,
        isError = isError,
        supportingText = supportingText,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        singleLine = true
    )
}