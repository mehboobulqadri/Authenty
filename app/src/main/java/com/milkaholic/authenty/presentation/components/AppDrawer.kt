package com.milkaholic.authenty.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun AppDrawer(
    navController: NavController,
    closeDrawer: () -> Unit,
    onPanic: () -> Unit = {}
) {
    ModalDrawerSheet {
        // --- Header ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(16.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = "Authenty",
                fontSize = 24.sp,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        HorizontalDivider()

        // --- Menu Items ---
        Spacer(modifier = Modifier.height(12.dp))

        DrawerItem(
            icon = Icons.Default.Home,
            label = "All Accounts",
            onClick = {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
                closeDrawer()
            }
        )

        DrawerItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            onClick = {
                navController.navigate("settings") {
                    launchSingleTop = true
                }
                closeDrawer()
            }
        )

        DrawerItem(
            icon = Icons.Default.Person,
            label = "Profiles",
            onClick = {
                navController.navigate("profile_management") {
                    launchSingleTop = true
                }
                closeDrawer()
            }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        HorizontalDivider()
        
        NavigationDrawerItem(
            label = { Text("Panic", color = MaterialTheme.colorScheme.error) },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Panic", tint = MaterialTheme.colorScheme.error) },
            selected = false,
            onClick = {
                onPanic()
                closeDrawer()
            },
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
fun DrawerItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, contentDescription = null) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}