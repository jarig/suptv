package com.suptv.tv.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.suptv.shared.db.Category
import com.suptv.shared.db.EpgProgram
import com.suptv.shared.db.Item
import com.suptv.shared.db.Provider
import com.suptv.tv.viewmodel.PlaylistViewModel
import com.suptv.tv.util.PreferencesManager
import java.text.SimpleDateFormat
import java.util.*

private enum class FocusedColumn {
    CATEGORIES, ITEMS, EPG_LIST, EPG_DETAILS
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: PlaylistViewModel,
    provider: Provider,
    preferencesManager: PreferencesManager,
    onItemSelected: (Item) -> Unit,
    onBack: () -> Unit,
    onEpgImport: () -> Unit = {},
    restoreFocusToItems: Boolean = false,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()
    val items by viewModel.items.collectAsState()
    val epgPrograms by viewModel.epgPrograms.collectAsState()
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var focusedColumn by remember { mutableStateOf<FocusedColumn>(FocusedColumn.ITEMS) }
    var focusedEpgProgram by remember { mutableStateOf<EpgProgram?>(null) }
    var isRestoringState by remember { mutableStateOf(true) }
    var shouldFocusItems by remember { mutableStateOf(false) }
    var categoryChangedManually by remember { mutableStateOf(false) }
    
    // Restore focus to items column when returning from player
    LaunchedEffect(restoreFocusToItems) {
        if (restoreFocusToItems) {
            focusedColumn = FocusedColumn.ITEMS
            shouldFocusItems = true
        }
    }
    
    LaunchedEffect(provider.id) {
        viewModel.loadCategories(provider.id)
    }
    
    LaunchedEffect(selectedCategory) {
        selectedCategory?.let { category ->
            viewModel.loadItems(category.id)
        }
    }
    
    // Load EPG when item is selected
    LaunchedEffect(selectedItem) {
        val item = selectedItem
        Log.i("CategoriesScreen", "selectedItem changed: ${item?.name}, epgId: ${item?.epgId}")
        if (item != null) {
            val epgId = item.epgId
            if (epgId != null) {
                Log.i("CategoriesScreen", "Loading EPG for item: $epgId")
                viewModel.loadEpgPrograms(epgId)
            } else {
                Log.i("CategoriesScreen", "Item has no EPG ID, clearing programs")
                viewModel.clearEpgPrograms()
            }
        } else {
            Log.i("CategoriesScreen", "No item selected, clearing programs")
            viewModel.clearEpgPrograms()
        }
    }
    
    // Restore last selected channel or auto-select first category
    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && selectedCategory == null && isRestoringState) {
            val lastSelected = preferencesManager.getLastSelectedChannel(provider.id)
            if (lastSelected != null) {
                // Find and select the last category
                val category = categories.find { it.id == lastSelected.categoryId }
                if (category != null) {
                    selectedCategory = category
                    // Will restore item after items are loaded
                } else {
                    // Category not found, select first
                    selectedCategory = categories.first()
                    isRestoringState = false
                }
            } else {
                // No saved state, select first
                selectedCategory = categories.first()
                isRestoringState = false
            }
        }
    }
    
    // Restore last selected item after items are loaded OR reset to first item if category changed manually
    LaunchedEffect(items) {
        if (items.isNotEmpty()) {
            if (categoryChangedManually) {
                // Reset to first item when category is changed manually
                selectedItem = items.first()
                shouldFocusItems = true
                categoryChangedManually = false
            } else if (selectedItem == null && isRestoringState) {
                // Restore last selected item only during initial state restoration
                val lastSelected = preferencesManager.getLastSelectedChannel(provider.id)
                if (lastSelected != null) {
                    val item = items.find { it.id == lastSelected.itemId }
                    if (item != null) {
                        selectedItem = item
                    }
                }
                isRestoringState = false
            }
        }
    }
    
    // Save selection when item is clicked (played)
    val onItemClicked: (Item) -> Unit = { item ->
        selectedCategory?.let { category ->
            preferencesManager.saveLastSelectedChannel(provider.id, category.id, item.id)
        }
        onItemSelected(item)
    }
    
    if (categories.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Loading categories...",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
        }
        return
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header with back button and clock
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                // Current time display
                var currentTime by remember { mutableStateOf(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())) }
                
                LaunchedEffect(Unit) {
                    while (true) {
                        currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        kotlinx.coroutines.delay(1000)
                    }
                }
                
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    text = "${categories.size} categories • ${items.size} items",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var epgButtonFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = onEpgImport,
                    modifier = Modifier.onFocusChanged { epgButtonFocused = it.isFocused },
                    colors = ButtonDefaults.colors(
                        containerColor = if (epgButtonFocused) Color.White else Color(0xFF1E88E5),
                        contentColor = if (epgButtonFocused) Color.Black else Color.White
                    ),
                    scale = ButtonDefaults.scale(
                        focusedScale = 1.05f,
                        pressedScale = 0.95f
                    )
                ) {
                    Text("📺 Import EPG")
                }
                
                var backButtonFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = onBack,
                    modifier = Modifier.onFocusChanged { backButtonFocused = it.isFocused },
                    colors = ButtonDefaults.colors(
                        containerColor = if (backButtonFocused) Color.White else Color.DarkGray,
                        contentColor = if (backButtonFocused) Color.Black else Color.White
                    ),
                    scale = ButtonDefaults.scale(
                        focusedScale = 1.05f,
                        pressedScale = 0.95f
                    )
                ) {
                    Text("← Back")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Three-column layout with dynamic visibility
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Show categories only when not focused on EPG details
            if (focusedColumn != FocusedColumn.EPG_DETAILS) {
                LazyColumn(
                    modifier = Modifier
                        .weight(0.2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        CategoryItem(
                            category = category,
                            isSelected = selectedCategory?.id == category.id,
                            onFocusChanged = { focused ->
                                if (focused) focusedColumn = FocusedColumn.CATEGORIES
                            },
                            onClick = { 
                                if (selectedCategory?.id != category.id) {
                                    selectedCategory = category
                                    selectedItem = null
                                    categoryChangedManually = true
                                }
                            }
                        )
                    }
                }
            }
            
            // Items list - always shown
            Box(
                modifier = Modifier
                    .weight(if (focusedColumn == FocusedColumn.EPG_DETAILS) 0.3f else 0.35f)
                    .fillMaxHeight()
            ) {
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No items in this category",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                } else {
                    val itemsListState = rememberLazyListState()
                    
                    // Auto-scroll to selected item when restoring focus
                    LaunchedEffect(shouldFocusItems, selectedItem?.id) {
                        if (shouldFocusItems && selectedItem != null) {
                            val index = items.indexOfFirst { it.id == selectedItem?.id }
                            if (index >= 0) {
                                itemsListState.animateScrollToItem(index)
                            }
                        }
                    }
                    
                    LazyColumn(
                        state = itemsListState,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items) { item ->
                            val shouldRequestFocus = (selectedItem?.id == item.id) && shouldFocusItems
                            ItemCard(
                                item = item,
                                isSelected = selectedItem?.id == item.id,
                                requestFocus = shouldRequestFocus,
                                onFocusChanged = { focused ->
                                    if (focused) {
                                        selectedItem = item
                                        focusedColumn = FocusedColumn.ITEMS
                                        if (shouldFocusItems) {
                                            shouldFocusItems = false
                                        }
                                        Log.i("CategoriesScreen", "Item focused: ${item.name}, epgId: ${item.epgId}")
                                    }
                                },
                                onClick = {
                                    onItemClicked(item)
                                }
                            )
                        }
                    }
                }
            }
            
            // EPG Programs list - always shown when there are programs
            Box(
                modifier = Modifier
                    .weight(if (focusedColumn == FocusedColumn.EPG_DETAILS) 0.3f else 0.45f)
                    .fillMaxHeight()
            ) {
                if (selectedItem == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Select a channel to view EPG",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                } else if (selectedItem!!.epgId == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No EPG data for this channel",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = selectedItem!!.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        if (epgPrograms.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No EPG programs available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(epgPrograms) { program ->
                                    EpgProgramCard(
                                        program = program,
                                        onFocusChanged = { focused ->
                                            if (focused) {
                                                focusedColumn = FocusedColumn.EPG_LIST
                                                focusedEpgProgram = program
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // EPG Details - shown only when EPG program is focused
            if (focusedColumn == FocusedColumn.EPG_LIST && focusedEpgProgram != null) {
                Box(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                ) {
                    EpgDetailsPanel(program = focusedEpgProgram!!)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CategoryItem(
    category: Category,
    isSelected: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .onFocusChanged { 
                val newFocused = it.isFocused
                if (isFocused != newFocused) {
                    isFocused = newFocused
                    onFocusChanged(newFocused)
                }
            },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = when {
                isFocused -> Color.White
                isSelected -> Color(0xFF1E88E5)
                else -> Color(0xFF2C2C2C)
            },
            contentColor = when {
                isFocused -> Color.Black
                else -> Color.White
            }
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1.02f,
            pressedScale = 0.98f
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ItemCard(
    item: Item,
    isSelected: Boolean,
    requestFocus: Boolean = false,
    onFocusChanged: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
        }
    }
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .then(if (requestFocus) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(onClick = onClick)
            .onFocusChanged { focusState ->
                val newFocused = focusState.isFocused
                if (isFocused != newFocused) {
                    isFocused = newFocused
                    onFocusChanged(newFocused)
                }
            },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = when {
                isFocused -> Color.White
                isSelected -> Color(0xFF1E88E5)
                else -> Color(0xFF2C2C2C)
            },
            contentColor = when {
                isFocused -> Color.Black
                else -> Color.White
            }
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1.02f,
            pressedScale = 0.98f
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isFocused) Color.Black else Color.White,
                    maxLines = 1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.type,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isFocused) Color.DarkGray else Color.LightGray
                    )
                    item.epgId?.let {
                        Text(
                            text = "• EPG",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isFocused) Color.DarkGray else Color.LightGray
                        )
                    }
                }
            }
            
            // Play indicator
            Text(
                text = "▶",
                style = MaterialTheme.typography.titleLarge,
                color = if (isFocused) Color.Black else if (isSelected) Color.White else Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpgProgramCard(
    program: EpgProgram,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") } }
    val localTimeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val currentTime = System.currentTimeMillis()
    val isNowPlaying = currentTime >= program.startTime && currentTime < program.endTime
    var isFocused by remember { mutableStateOf(false) }
    
    Surface(
        onClick = { },
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .onFocusChanged { 
                val newFocused = it.isFocused
                if (isFocused != newFocused) {
                    isFocused = newFocused
                    onFocusChanged(newFocused)
                }
            },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = when {
                isFocused -> Color.White
                isNowPlaying -> Color(0xFF1E88E5)
                else -> Color(0xFF2C2C2C)
            },
            contentColor = when {
                isFocused -> Color.Black
                else -> Color.White
            }
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1.02f,
            pressedScale = 0.98f
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${localTimeFormat.format(Date(program.startTime))} - ${localTimeFormat.format(Date(program.endTime))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFocused) Color.DarkGray else if (isNowPlaying) Color.White else Color.LightGray
                )
                if (isNowPlaying) {
                    Text(
                        text = "NOW",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFocused) Color.Black else Color.White
                    )
                }
            }
            
            Text(
                text = program.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isFocused) Color.Black else Color.White,
                maxLines = 1
            )
            
            program.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFocused) Color.DarkGray else Color.LightGray,
                    maxLines = 2
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpgDetailsPanel(
    program: EpgProgram,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()) }
    val currentTime = System.currentTimeMillis()
    val isNowPlaying = currentTime >= program.startTime && currentTime < program.endTime
    
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Now playing badge
        if (isNowPlaying) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔴 NOW PLAYING",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF1E88E5),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        
        // Title
        Text(
            text = program.title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        
        // Date and time
        Text(
            text = dateFormat.format(Date(program.startTime)),
            style = MaterialTheme.typography.titleMedium,
            color = Color.LightGray
        )
        
        Text(
            text = "${timeFormat.format(Date(program.startTime))} - ${timeFormat.format(Date(program.endTime))}",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        
        // Duration
        val durationMinutes = (program.endTime - program.startTime) / 60000
        Text(
            text = "Duration: $durationMinutes minutes",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.LightGray
        )
        
        // Description
        program.description?.let { desc ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Description",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.LightGray
            )
        }
    }
}
