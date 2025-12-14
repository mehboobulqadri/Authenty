package com.milkaholic.authenty.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Composable for swipe-to-delete functionality with confirmation dialog
 */
@Composable
fun <T> SwipeToDeleteContainer(
    item: T,
    onDelete: (T) -> Unit,
    modifier: Modifier = Modifier,
    confirmationRequired: Boolean = true,
    content: @Composable (T) -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isRevealed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
    ) {
        // Background delete action
        DeleteBackground(
            isRevealed = isRevealed,
            modifier = Modifier.matchParentSize()
        )

        // Main content with swipe gesture
        SwipeableContent(
            item = item,
            onSwipeRevealed = { isRevealed = it },
            onDeleteRequested = {
                if (confirmationRequired) {
                    showConfirmDialog = true
                } else {
                    onDelete(item)
                }
            },
            content = content
        )
    }

    // Confirmation dialog
    if (showConfirmDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                onDelete(item)
                showConfirmDialog = false
                isRevealed = false
            },
            onDismiss = {
                showConfirmDialog = false
                isRevealed = false
            }
        )
    }
}

@Composable
private fun DeleteBackground(
    isRevealed: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isRevealed) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
        label = "delete_background_color"
    )
    
    val iconScale by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0.8f,
        label = "delete_icon_scale"
    )

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clip(RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isRevealed) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.scale(iconScale)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Delete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SwipeableContent(
    item: T,
    onSwipeRevealed: (Boolean) -> Unit,
    onDeleteRequested: () -> Unit,
    content: @Composable (T) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> false
                SwipeToDismissBoxValue.EndToStart -> {
                    onDeleteRequested()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    // Track swipe state for visual feedback
    LaunchedEffect(dismissState.targetValue) {
        onSwipeRevealed(dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { },
        content = {
            content(item)
        }
    )
}

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Warning",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Delete Account?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "This will permanently delete this authenticator account.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You will need to re-add it if you want to use it again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Alternative simple swipe-to-delete without background (for cards that have their own background)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SimpleSwipeToDelete(
    item: T,
    onDelete: (T) -> Unit,
    modifier: Modifier = Modifier,
    confirmationRequired: Boolean = true,
    content: @Composable (T) -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    if (confirmationRequired) {
                        showConfirmDialog = true
                        false // Don't dismiss immediately
                    } else {
                        onDelete(item)
                        true
                    }
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val backgroundColor = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                else -> Color.Transparent
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        },
        content = {
            content(item)
        }
    )

    // Reset swipe state when dialog is dismissed
    LaunchedEffect(showConfirmDialog) {
        if (!showConfirmDialog) {
            dismissState.reset()
        }
    }

    // Confirmation dialog
    if (showConfirmDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                onDelete(item)
                showConfirmDialog = false
            },
            onDismiss = {
                showConfirmDialog = false
            }
        )
    }
}