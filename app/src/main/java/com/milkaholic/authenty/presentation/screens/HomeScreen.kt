package com.milkaholic.authenty.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.milkaholic.authenty.data.AccountModel
import com.milkaholic.authenty.data.AccountWithProfile
import com.milkaholic.authenty.data.AccountCategory
import com.milkaholic.authenty.domain.TokenGenerator
import com.milkaholic.authenty.presentation.MainViewModel
import com.milkaholic.authenty.presentation.components.BackupCodesDialog
import com.milkaholic.authenty.presentation.components.CategoryChip
import com.milkaholic.authenty.presentation.components.DraggableAccountsList
import com.milkaholic.authenty.presentation.components.ErrorDialog
import com.milkaholic.authenty.presentation.components.ReorderModeToggle
import com.milkaholic.authenty.presentation.components.ReorderModeToggle
import com.milkaholic.authenty.presentation.components.SearchAndFilterBar
import com.milkaholic.authenty.presentation.components.SearchResultsInfo
import com.milkaholic.authenty.presentation.components.SimpleSwipeToDelete
import com.milkaholic.authenty.presentation.utils.HapticUtils
import com.milkaholic.authenty.presentation.utils.AccessibilityUtils
import com.milkaholic.authenty.presentation.utils.rememberHapticUtils
import com.milkaholic.authenty.ui.theme.*
import com.milkaholic.authenty.ui.theme.AuthentyAnimations
import com.milkaholic.authenty.ui.theme.AuthentyPurple
import com.milkaholic.authenty.ui.theme.FadeInOut
import com.milkaholic.authenty.ui.theme.SlideInFromTop
import com.milkaholic.authenty.ui.theme.PrimaryGradientCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: MainViewModel,
    openDrawer: () -> Unit
) {
    val accounts by viewModel.accounts
    val currentTime by viewModel.currentTime
    val progress by viewModel.progress // Keep for backward compatibility if needed
    val currentError by viewModel.currentError
    val showErrorDialog by viewModel.showErrorDialog
    val isAddingAccount by viewModel.isAddingAccount
    val isDeletingAccount by viewModel.isDeletingAccount
    
    // Search and filter state
    val searchQuery by viewModel.searchQuery
    val selectedCategory by viewModel.selectedCategory
    val displayAccounts = viewModel.getDisplayAccounts()
    val availableCategories = viewModel.getAvailableCategories()
    
    // Reorder mode state
    val isReorderMode by viewModel.isReorderMode
    
    val hapticUtils = rememberHapticUtils()
    
    var accountForBackupCodes by remember { mutableStateOf<AccountWithProfile?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Authenty",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { openDrawer() }
                    ) {
                        Icon(
                            Icons.Default.Menu, 
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            var isPressed by remember { mutableStateOf(false) }
            
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.9f else 1f,
                animationSpec = AuthentyAnimations.fastSpring,
                label = "fab_scale"
            )
            
            FloatingActionButton(
                onClick = { 
                    hapticUtils.click()
                    isPressed = true
                    navController.navigate("scan") 
                },
                modifier = Modifier
                    .semantics {
                        contentDescription = "Add new authenticator account by scanning QR code"
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                containerColor = AuthentyPurple,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add account")
            }
            
            LaunchedEffect(isPressed) {
                if (isPressed) {
                    kotlinx.coroutines.delay(100)
                    isPressed = false
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search and Filter Bar OR Reorder Toggle (when there are accounts)
            if (accounts.isNotEmpty()) {
                if (isReorderMode) {
                    // Reorder mode controls
                    SlideInFromTop(
                        visible = true,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        PrimaryGradientCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Reorder Mode",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    
                                    TextButton(
                                        onClick = { viewModel.toggleReorderMode() },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text("Done")
                                    }
                                }
                                
                                Text(
                                    text = "Long press and drag accounts to reorder them",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                } else {
                    // Normal search and filter mode
                    SearchAndFilterBar(
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        availableCategories = availableCategories,
                        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                        onCategoryFilter = { viewModel.filterByCategory(it) },
                        onClearFilters = { viewModel.clearFilters() },
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    // Search Results Info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SearchResultsInfo(
                                totalResults = displayAccounts.size,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory
                            )
                        }
                    }
                }
            }
            
            // Content area
            if (accounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics {
                            contentDescription = "No authenticator accounts added yet. Use the add button to scan a QR code and add your first account."
                        }, 
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No accounts yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to add your first account",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (displayAccounts.isEmpty() && (searchQuery.isNotEmpty() || selectedCategory != null)) {
                // No search results
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No accounts found",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try adjusting your search or filters",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Unified list with drag & drop, swipe to delete, and filtering
                DraggableAccountsList(
                    accounts = displayAccounts, // Use filtered accounts
                    currentTime = currentTime,
                    hapticUtils = hapticUtils,
                    onAccountReorder = { accountIds ->
                        // Only allow reordering if not filtering
                        if (searchQuery.isEmpty() && selectedCategory == null) {
                            viewModel.reorderAccounts(accountIds)
                        } else {
                            // Optional: Show toast that reordering is disabled while filtering
                        }
                    },
                    onCategoryChange = { accountId, newCategory ->
                        viewModel.updateAccountCategory(accountId, newCategory)
                    },
                    onAccountDelete = { accountId ->
                        viewModel.deleteAccount(accountId)
                    },
                    onShowBackupCodes = { accountWithProfile ->
                        accountForBackupCodes = accountWithProfile
                    },
                    onCopy = { code ->
                        viewModel.copyToClipboard(code, "TOTP Code")
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    
    // Backup Codes Dialog
    accountForBackupCodes?.let { accountWithProfile ->
        BackupCodesDialog(
            backupCodes = accountWithProfile.account.safeBackupCodes,
            onDismiss = { accountForBackupCodes = null },
            onUpdateCodes = { newCodes ->
                viewModel.updateBackupCodes(accountWithProfile.account.id, newCodes)
                // Update the local state to reflect changes immediately in the dialog
                accountForBackupCodes = accountWithProfile.copy(
                    account = accountWithProfile.account.copy(backupCodes = newCodes)
                )
            }
        )
    }
    
    // Error Dialog at HomeScreen level
    if (showErrorDialog && currentError != null) {
        ErrorDialog(
            error = currentError!!,
            onDismiss = { viewModel.dismissError() },
            onRetry = { viewModel.retryLastOperation() }
        )
    }
}

@Composable
fun AccountItem(
    account: AccountModel, 
    progress: Float, 
    position: Int,
    total: Int,
    hapticUtils: HapticUtils,
    showDeleteButton: Boolean = true,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val code = TokenGenerator.generateTOTP(
        account.secret,
        account.safeAlgorithm,
        account.safeDigits,
        account.safePeriod
    )
    val secondsRemaining = (progress * account.safePeriod).toInt()

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = AccessibilityUtils.getAccountCardDescription(
                    account.issuer, account.name, position, total
                )
            }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.issuer,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = account.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // --- CLICKABLE CODE WITH HAPTICS & ACCESSIBILITY ---
                Text(
                    text = "${code.substring(0, 3)} ${code.substring(3, 6)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 4.sp,
                    modifier = Modifier
                        .clickable {
                            // Haptic feedback
                            hapticUtils.success()
                            
                            // Copy to clipboard via callback (handled by ViewModel)
                            // We don't have a callback here in AccountItem, but we should probably add one
                            // or just use the context safely.
                            // For now, let's use a safe local copy if no callback is provided.
                            
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("TOTP Code", code)
                                clipboard.setPrimaryClip(clip)
                                
                                // Visual feedback
                                Toast.makeText(
                                    context, 
                                    AccessibilityUtils.getCopyCodeAnnouncement(account.issuer), 
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                // Ignore clipboard errors
                            }
                        }
                        .semantics {
                            role = Role.Button
                            contentDescription = AccessibilityUtils.getTotpContentDescription(
                                account.issuer, account.name, code, secondsRemaining
                            )
                        }
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .size(40.dp)
                        .semantics {
                            contentDescription = AccessibilityUtils.getProgressContentDescription(progress)
                        },
                    trackColor = MaterialTheme.colorScheme.secondaryContainer,
                )
                
                // Show seconds remaining as text for accessibility
                Text(
                    text = "${secondsRemaining}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (showDeleteButton && onDelete != null) {
                    IconButton(
                        onClick = {
                            hapticUtils.heavyImpact()
                            onDelete()
                        },
                        modifier = Modifier
                            .semantics {
                                contentDescription = "Delete ${account.issuer} account for ${account.name}"
                                role = Role.Button
                            }
                    ) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = "Delete account",
                            tint = Color.Red.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccountItemWithCategory(
    accountWithProfile: AccountWithProfile,
    currentTime: Long,
    position: Int,
    total: Int,
    hapticUtils: HapticUtils,
    onCategoryChange: (String) -> Unit,
    onShowBackupCodes: (AccountModel) -> Unit,
    onCopy: (String) -> Unit
) {
    val context = LocalContext.current
    val account = accountWithProfile.account
    
    // Optimize TOTP generation: only regenerate when the time slot changes
    val period = account.safePeriod
    val timeSlot = currentTime / 1000 / period
    
    val code = remember(account.id, timeSlot) {
        TokenGenerator.generateTOTP(
            account.secret, 
            account.safeAlgorithm, 
            account.safeDigits, 
            account.safePeriod, 
            currentTime
        )
    }
    
    val progress = TokenGenerator.getProgress(account.safePeriod)
    
    GradientCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                hapticUtils.lightTap()
                // For copying, we might want the freshest code, but the memoized one is valid for the current slot.
                // However, if we are right at the edge, it might be better to regenerate.
                // But for now, using the memoized code is fine and consistent with UI.
                onCopy(code)
                
                // Toast is handled by ViewModel/ClipboardHelper now
            }
            .semantics {
                contentDescription = "${account.issuer} account for ${account.name}. Position $position of $total. ${accountWithProfile.category} category."
                role = Role.Button
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.issuer,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onShowBackupCodes(account) }) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = "Backup Codes",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Category chip
                    CategoryChip(
                        category = accountWithProfile.category,
                        onCategoryChange = onCategoryChange
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Token and progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}