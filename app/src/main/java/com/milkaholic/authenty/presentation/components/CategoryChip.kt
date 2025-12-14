package com.milkaholic.authenty.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.milkaholic.authenty.data.AccountCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChip(
    category: String,
    onCategoryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val currentCategory = AccountCategory.fromString(category)
    val categoryColor = remember(currentCategory) {
        try {
            Color(android.graphics.Color.parseColor(currentCategory.colorHex))
        } catch (e: IllegalArgumentException) {
            Color(0xFF2196F3) // Default blue color as fallback
        }
    }
    
    Box(modifier = modifier) {
        // Category chip
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .background(
                    color = categoryColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = categoryColor.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = currentCategory.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = categoryColor
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Change category",
                modifier = Modifier.size(16.dp),
                tint = categoryColor
            )
        }
        
        // Dropdown menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AccountCategory.getAllCategories().forEach { categoryOption ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val optionColor = remember(categoryOption) {
                                try {
                                    Color(android.graphics.Color.parseColor(categoryOption.colorHex))
                                } catch (e: IllegalArgumentException) {
                                    Color(0xFF2196F3) // Default blue color as fallback
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        color = optionColor,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                            )
                            Text(
                                text = categoryOption.displayName,
                                color = if (categoryOption == currentCategory) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (categoryOption == currentCategory) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                }
                            )
                        }
                    },
                    onClick = {
                        onCategoryChange(categoryOption.displayName)
                        expanded = false
                    }
                )
            }
        }
    }
}