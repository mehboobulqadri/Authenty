package com.milkaholic.authenty.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@Composable
fun PinEntryScreen(
    title: String = "Enter PIN",
    subtitle: String? = null,
    onPinEntered: (String) -> Unit,
    onCancel: (() -> Unit)? = null,
    onBiometricRequested: (() -> Unit)? = null,
    maxPinLength: Int = 6,
    showBiometricOption: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    var enteredPin by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isError) {
        if (isError) {
            enteredPin = ""
        }
    }

    LaunchedEffect(enteredPin) {
        if (enteredPin.length == maxPinLength) {
            onPinEntered(enteredPin)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            PinDots(
                enteredLength = enteredPin.length,
                totalLength = maxPinLength,
                isError = isError
            )

            if (errorMessage != null && isError) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Hidden TextField for native keyboard input
            OutlinedTextField(
                value = enteredPin,
                onValueChange = { newPin ->
                    if (newPin.length <= maxPinLength && newPin.all { it.isDigit() }) {
                        enteredPin = newPin
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .width(200.dp),
                label = { Text("Enter PIN") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword
                ),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = isError,
                supportingText = if (errorMessage != null && isError) {
                    { Text(errorMessage) }
                } else null
            )
        }

        if (showBiometricOption && onBiometricRequested != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (onCancel != null) {
                    OutlinedButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBiometricRequested()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Use Biometric",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Biometric")
                }
            }
        } else if (onCancel != null) {
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun PinDots(
    enteredLength: Int,
    totalLength: Int,
    isError: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalLength) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isError -> MaterialTheme.colorScheme.error
                            index < enteredLength -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        }
                    )
            )
        }
    }
}

@Composable
fun NumPad(
    onNumberClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onCancelClick: (() -> Unit)?
) {
    val haptic = LocalHapticFeedback.current
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        for (row in 0..2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (col in 0..2) {
                    val number = (row * 3 + col + 1).toString()
                    NumPadButton(
                        text = number,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNumberClick(number)
                        }
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onCancelClick != null) {
                NumPadButton(
                    icon = Icons.Default.Close,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCancelClick()
                    }
                )
            } else {
                Spacer(modifier = Modifier.size(72.dp))
            }

            NumPadButton(
                text = "0",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNumberClick("0")
                }
            )

            NumPadButton(
                icon = Icons.Default.Backspace,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBackspaceClick()
                }
            )
        }
    }
}

@Composable
fun NumPadButton(
    text: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (text != null) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PinSetupScreen(
    title: String,
    subtitle: String,
    onPinSet: (String) -> Unit,
    onCancel: () -> Unit,
    confirmPin: Boolean = false,
    originalPin: String? = null
) {
    var enteredPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(enteredPin.length) {
        showError = false
    }

    PinEntryScreen(
        title = title,
        subtitle = subtitle,
        onPinEntered = { pin ->
            if (confirmPin && originalPin != null) {
                if (pin == originalPin) {
                    onPinSet(pin)
                } else {
                    showError = true
                    errorMessage = "PINs don't match. Please try again."
                }
            } else {
                onPinSet(pin)
            }
        },
        onCancel = onCancel,
        showBiometricOption = false,
        isError = showError,
        errorMessage = errorMessage
    )
}