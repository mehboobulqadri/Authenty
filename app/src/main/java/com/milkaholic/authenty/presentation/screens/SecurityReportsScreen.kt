package com.milkaholic.authenty.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.milkaholic.authenty.data.SecurityEvent
import com.milkaholic.authenty.data.SecurityEventSeverity
import com.milkaholic.authenty.data.SecurityEventType
import com.milkaholic.authenty.data.SecuritySummary
import com.milkaholic.authenty.domain.AuthentyResult
import com.milkaholic.authenty.domain.SecurityManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import com.milkaholic.authenty.ui.theme.GradientBackground
import com.milkaholic.authenty.ui.theme.GradientCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityReportsScreen(
    navController: NavController,
    openDrawer: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val securityManager = remember { SecurityManager.getInstance(context) }
    
    var securityEvents by remember { mutableStateOf<List<SecurityEvent>>(emptyList()) }
    var securitySummary by remember { mutableStateOf<SecuritySummary?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTimeFilter by remember { mutableStateOf(TimeFilter.LAST_24H) }

    // Load security data
    LaunchedEffect(selectedTimeFilter) {
        scope.launch {
            isLoading = true
            
            val eventsResult = when (selectedTimeFilter) {
                TimeFilter.LAST_24H -> securityManager.getRecentSecurityEvents(24)
                TimeFilter.LAST_7D -> securityManager.getRecentSecurityEvents(24 * 7)
                TimeFilter.ALL_TIME -> securityManager.getAllSecurityEvents()
            }
            
            if (eventsResult is AuthentyResult.Success) {
                securityEvents = eventsResult.data
            }
            
            val summaryResult = securityManager.getSecuritySummary()
            if (summaryResult is AuthentyResult.Success) {
                securitySummary = summaryResult.data
            }
            
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Reports") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Time filter dropdown
                    var showFilterMenu by remember { mutableStateOf(false) }
                    
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                        
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            TimeFilter.values().forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(filter.displayName) },
                                    onClick = {
                                        selectedTimeFilter = filter
                                        showFilterMenu = false
                                    },
                                    leadingIcon = {
                                        if (selectedTimeFilter == filter) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        GradientBackground(modifier = Modifier.padding(paddingValues)) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Summary Card
                item {
                    SecuritySummaryCard(securitySummary)
                }
                
                // Filter Info
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Showing ${selectedTimeFilter.displayName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${securityEvents.size} events found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Security Events List
                if (securityEvents.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = "No events",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "No security events",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No security events found for the selected time period",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(securityEvents) { event ->
                        SecurityEventCard(event)
                    }
                }
            }
        }
    }
}
}

@Composable
fun SecuritySummaryCard(summary: SecuritySummary?) {
    GradientCard {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Security Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                if (summary?.isDeviceCompromised == true) {
                    AssistChip(
                        onClick = { },
                        label = { Text("COMPROMISED") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Warning",
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            labelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            summary?.let { s ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryStatItem(
                        label = "Total Events",
                        value = s.totalEvents.toString(),
                        icon = Icons.Default.Assessment
                    )
                    SummaryStatItem(
                        label = "Successful Auth",
                        value = s.successfulAuthentications.toString(),
                        icon = Icons.Default.CheckCircle,
                        color = Color.Green
                    )
                    SummaryStatItem(
                        label = "Failed Auth",
                        value = s.failedAuthentications.toString(),
                        icon = Icons.Default.Error,
                        color = if (s.failedAuthentications > 0) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                    SummaryStatItem(
                        label = "Threats",
                        value = s.securityThreats.toString(),
                        icon = Icons.Default.Warning,
                        color = if (s.securityThreats > 0) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (s.lastLoginTimestamp != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Last Login: ${formatTimestamp(s.lastLoginTimestamp)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryStatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SecurityEventCard(event: SecurityEvent) {
    GradientCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Severity indicator with icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(getSeverityColor(event.severity))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.severity.name.take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = getSeverityColor(event.severity),
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = getEventTitle(event.type),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = formatEventTime(event.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
                
                if (event.metadata.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    event.metadata.forEach { (key, value) ->
                        Text(
                            text = "$key: $value",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AssistChip(
                    onClick = { },
                    label = { 
                        Text(
                            text = event.severity.name,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = getSeverityColor(event.severity).copy(alpha = 0.1f),
                        labelColor = getSeverityColor(event.severity)
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }
        }
    }
}

fun getSeverityColor(severity: SecurityEventSeverity): Color {
    return when (severity) {
        SecurityEventSeverity.LOW -> Color.Green
        SecurityEventSeverity.INFO -> Color.Blue
        SecurityEventSeverity.MEDIUM -> Color(0xFFFF9800)
        SecurityEventSeverity.HIGH -> Color(0xFFFF5722)
        SecurityEventSeverity.CRITICAL -> Color.Red
    }
}

fun getEventTitle(type: SecurityEventType): String {
    return when (type) {
        SecurityEventType.BIOMETRIC_AUTH_SUCCESS -> "Biometric Authentication Successful"
        SecurityEventType.BIOMETRIC_AUTH_FAILURE -> "Biometric Authentication Failed"
        SecurityEventType.PIN_AUTH_SUCCESS -> "PIN Authentication Successful"
        SecurityEventType.PIN_AUTH_FAILURE -> "PIN Authentication Failed"
        SecurityEventType.ACCOUNT_ADDED -> "Account Added"
        SecurityEventType.ACCOUNT_DELETED -> "Account Deleted"
        SecurityEventType.ACCOUNT_MODIFIED -> "Account Modified"
        SecurityEventType.APP_LOCKED -> "App Locked"
        SecurityEventType.APP_UNLOCKED -> "App Unlocked"
        SecurityEventType.ROOT_DETECTED -> "Root Access Detected"
        SecurityEventType.TAMPER_DETECTED -> "Tampering Detected"
        SecurityEventType.SECURITY_BREACH_DETECTED -> "Security Breach Detected"
        SecurityEventType.AUTO_LOCK_TRIGGERED -> "Auto-lock Triggered"
        SecurityEventType.PROGRESSIVE_LOCKOUT -> "Progressive Lockout"
        SecurityEventType.SETTINGS_MODIFIED -> "Settings Modified"
        SecurityEventType.BACKUP_CREATED -> "Backup Created"
        SecurityEventType.BACKUP_RESTORED -> "Backup Restored"
        SecurityEventType.APP_INSTALL_VERIFIED -> "App Install Verified"
        SecurityEventType.SUSPICIOUS_ACTIVITY -> "Suspicious Activity"
    }
}

fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    return formatter.format(date)
}

fun formatEventTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d ago"
        else -> {
            val date = Date(timestamp)
            val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
            formatter.format(date)
        }
    }
}

enum class TimeFilter(val displayName: String) {
    LAST_24H("Last 24 Hours"),
    LAST_7D("Last 7 Days"),
    ALL_TIME("All Time")
}