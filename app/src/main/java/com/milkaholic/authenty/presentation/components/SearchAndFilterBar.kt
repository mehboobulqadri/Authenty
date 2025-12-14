package com.milkaholic.authenty.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
fun SearchAndFilterBar(
    searchQuery: String,
    selectedCategory: String?,
    availableCategories: List<String>,
    onSearchQueryChange: (String) -> Unit,
    onCategoryFilter: (String?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search accounts...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else null,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
        
        // Category Filter Chips
        if (availableCategories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // All categories chip
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { onCategoryFilter(null) },
                            label = { Text("All") }
                        )
                    }
                    
                    // Individual category chips
                    items(availableCategories) { category ->
                        val accountCategory = AccountCategory.fromString(category)
                        val categoryColor = remember(accountCategory) {
                            try {
                                Color(android.graphics.Color.parseColor(accountCategory.colorHex))
                            } catch (e: IllegalArgumentException) {
                                Color(0xFF2196F3)
                            }
                        }
                        
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { 
                                if (selectedCategory == category) {
                                    onCategoryFilter(null) // Deselect if already selected
                                } else {
                                    onCategoryFilter(category)
                                }
                            },
                            label = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = categoryColor,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                    )
                                    Text(category)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = categoryColor.copy(alpha = 0.2f),
                                selectedLabelColor = categoryColor
                            )
                        )
                    }
                }
            }
        }
        
        // Clear filters button (when filters are active)
        if (searchQuery.isNotEmpty() || selectedCategory != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onClearFilters,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear all filters")
                }
            }
        }
    }
}

@Composable
fun SearchResultsInfo(
    totalResults: Int,
    searchQuery: String,
    selectedCategory: String?,
    modifier: Modifier = Modifier
) {
    if (searchQuery.isNotEmpty() || selectedCategory != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = buildString {
                        append("$totalResults result${if (totalResults != 1) "s" else ""}")
                        if (searchQuery.isNotEmpty()) {
                            append(" for \"$searchQuery\"")
                        }
                        if (selectedCategory != null) {
                            append(" in $selectedCategory")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}