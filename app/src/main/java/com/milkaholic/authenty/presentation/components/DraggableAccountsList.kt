package com.milkaholic.authenty.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.milkaholic.authenty.data.AccountWithProfile
import com.milkaholic.authenty.presentation.utils.HapticUtils

import com.milkaholic.authenty.presentation.components.SimpleSwipeToDelete
import com.milkaholic.authenty.presentation.components.CategoryChip
import androidx.compose.material.icons.filled.List
import androidx.compose.foundation.clickable
import com.milkaholic.authenty.ui.theme.AuthentyGradients

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableAccountsList(
    accounts: List<AccountWithProfile>,
    currentTime: Long,
    hapticUtils: HapticUtils,
    onAccountReorder: (List<Long>) -> Unit,
    onCategoryChange: (Long, String) -> Unit,
    onAccountDelete: (Long) -> Unit,
    onShowBackupCodes: (AccountWithProfile) -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggedAccount by remember { mutableStateOf<AccountWithProfile?>(null) }
    var draggedIndex by remember { mutableStateOf(-1) }
    var targetIndex by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }
    
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    
    // Use accounts directly for display, only reorder on drag end
    val displayAccounts = accounts
    
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = displayAccounts,
            key = { _, account -> account.account.id }
        ) { index, accountWithProfile ->
            
            val isDragging = draggedIndex == index
            val elevation by animateDpAsState(
                targetValue = if (isDragging) 8.dp else 2.dp,
                animationSpec = tween(200, easing = FastOutSlowInEasing),
                label = "elevation"
            )
            
            // Swipe to delete wrapper
            SimpleSwipeToDelete(
                item = accountWithProfile.account,
                onDelete = { accountToDelete ->
                    hapticUtils.warning()
                    onAccountDelete(accountToDelete.id)
                },
                confirmationRequired = true
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 1f else 0f)
                        .shadow(elevation, RoundedCornerShape(12.dp))
                        .graphicsLayer {
                            scaleX = if (isDragging) 1.02f else 1f
                            scaleY = if (isDragging) 1.02f else 1f
                        }
                        .clickable {
                            // Copy code on click
                            hapticUtils.lightTap()
                            val code = com.milkaholic.authenty.domain.TokenGenerator.generateTOTP(
                                secret = accountWithProfile.account.secret,
                                algorithm = accountWithProfile.account.safeAlgorithm,
                                digits = accountWithProfile.account.safeDigits,
                                period = accountWithProfile.account.safePeriod,
                                time = currentTime
                            )
                            onCopy(code)
                        }
                        .animateItem(),
                    elevation = CardDefaults.cardElevation(defaultElevation = elevation),
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isDragging) AuthentyGradients.neonGradient() else AuthentyGradients.subtleGradient()
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Drag handle
                            Icon(
                                imageVector = Icons.Default.DragIndicator,
                                contentDescription = "Drag to reorder",
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(32.dp) // Increased touch target
                                    .pointerInput(displayAccounts.size) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                hapticUtils.lightTap()
                                                draggedAccount = accountWithProfile
                                                draggedIndex = index
                                                targetIndex = index
                                                dragOffset = 0f
                                            },
                                            onDragEnd = {
                                                if (draggedIndex != -1 && targetIndex != -1 && draggedIndex != targetIndex) {
                                                    // Perform the reorder only once on drag end
                                                    val newList = displayAccounts.toMutableList()
                                                    val draggedItem = newList.removeAt(draggedIndex)
                                                    newList.add(targetIndex, draggedItem)
                                                    
                                                    // Notify parent of the reorder
                                                    onAccountReorder(newList.map { it.account.id })
                                                    
                                                    hapticUtils.success()
                                                }
                                                
                                                draggedAccount = null
                                                draggedIndex = -1
                                                targetIndex = -1
                                                dragOffset = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount.y
                                                
                                                // Calculate target index based on cumulative drag offset
                                                val itemHeight = 100.dp // Account for card height + spacing
                                                val itemHeightPx = with(density) { itemHeight.toPx() }
                                                
                                                val offsetItems = (dragOffset / itemHeightPx).toInt()
                                                val newTargetIndex = (draggedIndex + offsetItems)
                                                    .coerceIn(0, displayAccounts.size - 1)
                                                
                                                if (newTargetIndex != targetIndex) {
                                                    targetIndex = newTargetIndex
                                                    hapticUtils.lightTap()
                                                }
                                            }
                                        )
                                    },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            
                            // Account content
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = accountWithProfile.account.issuer,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = accountWithProfile.account.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { 
                                            try {
                                                onShowBackupCodes(accountWithProfile) 
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }) {
                                            Icon(
                                                Icons.Default.List,
                                                contentDescription = "Backup Codes",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        
                                        CategoryChip(
                                            category = accountWithProfile.category,
                                            onCategoryChange = { newCategory ->
                                                onCategoryChange(accountWithProfile.account.id, newCategory)
                                            }
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Optimize TOTP generation
                                    val period = accountWithProfile.account.safePeriod
                                    val timeSlot = currentTime / 1000 / period
                                    
                                    // Use derivedStateOf to ensure we only recompose when the code actually changes
                                    // But remember(key) is already doing that.
                                    // The issue might be that currentTime updates frequently, causing frequent recompositions of this block.
                                    // But remember(timeSlot) should prevent the expensive calculation.
                                    
                                    val code = remember(accountWithProfile.account.id, timeSlot) {
                                        com.milkaholic.authenty.domain.TokenGenerator.generateTOTP(
                                            secret = accountWithProfile.account.secret,
                                            algorithm = accountWithProfile.account.safeAlgorithm,
                                            digits = accountWithProfile.account.safeDigits,
                                            period = accountWithProfile.account.safePeriod,
                                            time = currentTime
                                        )
                                    }
                                    
                                    Text(
                                        text = code,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    CircularProgressIndicator(
                                        progress = com.milkaholic.authenty.domain.TokenGenerator.getProgress(accountWithProfile.account.safePeriod),
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp,
                                        color = MaterialTheme.colorScheme.primary
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


@Composable
fun ReorderModeToggle(
    isReorderMode: Boolean,
    onToggleReorderMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Reorder mode",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(
            checked = isReorderMode,
            onCheckedChange = onToggleReorderMode,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
    }
}