package com.milkaholic.authenty.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.milkaholic.authenty.data.AccountModel
import com.milkaholic.authenty.domain.TokenGenerator
import com.milkaholic.authenty.presentation.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val accounts by viewModel.accounts
    val progress by viewModel.progress

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Authenty") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("scan") }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        if (accounts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No accounts yet. Tap + to add one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(accounts) { account ->
                    AccountItem(account, progress, onDelete = { viewModel.deleteAccount(account.id) })
                }
            }
        }
    }
}

@Composable
fun AccountItem(account: AccountModel, progress: Float, onDelete: () -> Unit) {
    val context = LocalContext.current
    val code = TokenGenerator.generateTOTP(account.secret)

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
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

                // --- CLICKABLE CODE ---
                Text(
                    text = "${code.substring(0, 3)} ${code.substring(3, 6)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 4.sp,
                    modifier = Modifier.clickable {
                        // 1. Get Clipboard Manager
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        // 2. Create Clip
                        val clip = ClipData.newPlainText("TOTP Code", code)
                        // 3. Set to Clipboard
                        clipboard.setPrimaryClip(clip)
                        // 4. Show Feedback
                        Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(40.dp),
                    trackColor = MaterialTheme.colorScheme.secondaryContainer,
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.6f))
                }
            }
        }
    }
}