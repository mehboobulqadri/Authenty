package com.milkaholic.authenty.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.milkaholic.authenty.data.ProfileSettings
import com.milkaholic.authenty.data.UserProfile
import com.milkaholic.authenty.domain.AuthentyResult
import com.milkaholic.authenty.domain.ProfileManager
import kotlinx.coroutines.launch

import com.milkaholic.authenty.ui.theme.GradientBackground
import com.milkaholic.authenty.ui.theme.GradientCard
import com.milkaholic.authenty.ui.theme.PrimaryGradientCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileManagementScreen(
    navController: NavController,
    openDrawer: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileManager = remember { ProfileManager.getInstance(context) }
    
    val currentProfile by profileManager.currentProfile.collectAsState()
    val allProfiles by profileManager.allProfiles.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<UserProfile?>(null) }
    var showEditDialog by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Profile Management",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Profile")
                    }
                }
            )
        },
        snackbarHost = {
            snackbarMessage?.let { message ->
                LaunchedEffect(message) {
                    kotlinx.coroutines.delay(3000)
                    snackbarMessage = null
                }
                SnackbarHost(hostState = remember { SnackbarHostState() }) {
                    Snackbar { Text(message) }
                }
            }
        }
    ) { paddingValues ->
        GradientBackground(modifier = Modifier.padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Current Profile Header
            item {
                CurrentProfileCard(
                    currentProfile = currentProfile,
                    onSwitchProfile = { profile ->
                        scope.launch {
                            isLoading = true
                            val result = profileManager.setCurrentProfile(profile.id)
                            if (result is AuthentyResult.Error) {
                                snackbarMessage = result.error.message
                            } else {
                                snackbarMessage = "Switched to ${profile.name}"
                            }
                            isLoading = false
                        }
                    }
                )
            }

            // All Profiles Section
            item {
                Text(
                    text = "All Profiles (${allProfiles.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(allProfiles) { profile ->
                ProfileCard(
                    profile = profile,
                    isCurrentProfile = profile.id == currentProfile?.id,
                    onSwitchTo = { 
                        scope.launch {
                            isLoading = true
                            val result = profileManager.setCurrentProfile(profile.id)
                            if (result is AuthentyResult.Error) {
                                snackbarMessage = result.error.message
                            } else {
                                snackbarMessage = "Switched to ${profile.name}"
                            }
                            isLoading = false
                        }
                    },
                    onEdit = {
                        showEditDialog = profile
                    },
                    onDelete = { 
                        if (!profile.isDefault && allProfiles.size > 1) {
                            showDeleteDialog = profile
                        } else {
                            snackbarMessage = "Cannot delete default or last remaining profile"
                        }
                    },
                    accountCount = profileManager.getAccountsForProfile(profile.id).size
                )
            }

            // Statistics Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ProfileStatisticsCard(
                    profileManager = profileManager,
                    allProfiles = allProfiles
                )
            }
        }
    }
}

    // Create Profile Dialog
    if (showCreateDialog) {
        CreateProfileDialog(
            onDismiss = { showCreateDialog = false },
            onCreateProfile = { name ->
                scope.launch {
                    isLoading = true
                    val result = profileManager.createProfile(name)
                    if (result is AuthentyResult.Success) {
                        snackbarMessage = "Profile '$name' created successfully"
                        showCreateDialog = false
                    } else if (result is AuthentyResult.Error) {
                        snackbarMessage = result.error.message
                    }
                    isLoading = false
                }
            }
        )
    }

    // Delete Profile Dialog
    showDeleteDialog?.let { profile ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Profile") },
            text = { 
                Text("Are you sure you want to delete '${profile.name}'? This will remove all accounts in this profile.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val result = profileManager.deleteProfile(profile.id)
                            if (result is AuthentyResult.Success) {
                                snackbarMessage = "Profile deleted successfully"
                            } else if (result is AuthentyResult.Error) {
                                snackbarMessage = result.error.message
                            }
                            showDeleteDialog = null
                            isLoading = false
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Profile Dialog
    showEditDialog?.let { profile ->
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditDialog = null },
            onUpdateProfile = { newName ->
                scope.launch {
                    isLoading = true
                    val updatedProfile = profile.copy(name = newName)
                    val result = profileManager.updateProfile(profile.id, updatedProfile)
                    if (result is AuthentyResult.Success) {
                        snackbarMessage = "Profile updated successfully"
                    } else if (result is AuthentyResult.Error) {
                        snackbarMessage = result.error.message
                    }
                    showEditDialog = null
                    isLoading = false
                }
            }
        )
    }

    // Loading Indicator
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun CurrentProfileCard(
    currentProfile: UserProfile?,
    onSwitchProfile: (UserProfile) -> Unit
) {
    PrimaryGradientCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentProfile?.name?.take(1)?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Current Profile",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = currentProfile?.name ?: "No Profile Selected",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (currentProfile?.isDefault == true) {
                        Text(
                            text = "Default Profile",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Current Profile",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileCard(
    profile: UserProfile,
    isCurrentProfile: Boolean,
    onSwitchTo: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    accountCount: Int
) {
    val content = @Composable {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCurrentProfile) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isCurrentProfile) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isCurrentProfile) {
                            Spacer(modifier = Modifier.width(8.dp))
                            AssistChip(
                                onClick = { },
                                label = { Text("Active") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    labelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        if (profile.isDefault) {
                            Spacer(modifier = Modifier.width(8.dp))
                            AssistChip(
                                onClick = { },
                                label = { Text("Default") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }
                    
                    Text(
                        text = "$accountCount accounts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Actions
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    if (!isCurrentProfile) {
                        IconButton(onClick = onSwitchTo) {
                            Icon(Icons.Default.Login, contentDescription = "Switch")
                        }
                    }
                    IconButton(onClick = onDelete, enabled = !profile.isDefault) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = "Delete",
                            tint = if (profile.isDefault) Color.Gray else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (isCurrentProfile) {
        GradientCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            content()
        }
    }
}

@Composable
fun ProfileStatisticsCard(
    profileManager: ProfileManager,
    allProfiles: List<UserProfile>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Total Profiles",
                    value = allProfiles.size.toString(),
                    icon = Icons.Default.Group
                )
                
                StatisticItem(
                    label = "Total Accounts",
                    value = allProfiles.sumOf { 
                        profileManager.getAccountsForProfile(it.id).size 
                    }.toString(),
                    icon = Icons.Default.Security
                )
                
                StatisticItem(
                    label = "Categories",
                    value = allProfiles.flatMap { 
                        profileManager.getAccountCategories(it.id) 
                    }.distinct().size.toString(),
                    icon = Icons.Default.Category
                )
            }
        }
    }
}

@Composable
fun StatisticItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CreateProfileDialog(
    onDismiss: () -> Unit,
    onCreateProfile: (String) -> Unit
) {
    var profileName by remember { mutableStateOf("") }
    var isValid by remember { mutableStateOf(false) }

    LaunchedEffect(profileName) {
        isValid = profileName.isNotBlank() && profileName.length >= 2
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Profile") },
        text = {
            Column {
                Text("Enter a name for your new profile:")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g., Work, Personal, Gaming") },
                    singleLine = true,
                    isError = profileName.isNotBlank() && !isValid
                )
                if (profileName.isNotBlank() && !isValid) {
                    Text(
                        text = "Name must be at least 2 characters",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreateProfile(profileName.trim()) },
                enabled = isValid
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onUpdateProfile: (String) -> Unit
) {
    var profileName by remember { mutableStateOf(profile.name) }
    val isValid = profileName.trim().length >= 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column {
                Text(
                    text = "Edit the profile name",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g., Work, Personal, Gaming") },
                    singleLine = true,
                    isError = profileName.isNotBlank() && !isValid
                )
                if (profileName.isNotBlank() && !isValid) {
                    Text(
                        text = "Name must be at least 2 characters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid && profileName.trim() != profile.name) {
                        onUpdateProfile(profileName.trim())
                    } else {
                        onDismiss()
                    }
                },
                enabled = isValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}