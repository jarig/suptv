package com.suptv.tv.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.suptv.shared.db.Provider
import com.suptv.tv.viewmodel.PlaylistViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProvidersScreen(
    viewModel: PlaylistViewModel,
    onProviderSelected: (Provider) -> Unit,
    onAddM3UProvider: () -> Unit,
    onAddXStreamProvider: () -> Unit,
    modifier: Modifier = Modifier
) {
    val providers by viewModel.providers.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Playlists",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                var m3uButtonFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onAddM3UProvider() }
                ) {
                    Button(
                        onClick = onAddM3UProvider,
                        modifier = Modifier.onFocusChanged { m3uButtonFocused = it.isFocused },
                        colors = ButtonDefaults.colors(
                            containerColor = if (m3uButtonFocused) Color.White else Color(0xFF4CAF50),
                            contentColor = if (m3uButtonFocused) Color.Black else Color.White
                        ),
                        scale = ButtonDefaults.scale(
                            focusedScale = 1.05f,
                            pressedScale = 0.95f
                        )
                    ) {
                        Text("Add M3U")
                    }
                }
                
                var xstreamButtonFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onAddXStreamProvider() }
                ) {
                    Button(
                        onClick = onAddXStreamProvider,
                        modifier = Modifier.onFocusChanged { xstreamButtonFocused = it.isFocused },
                        colors = ButtonDefaults.colors(
                            containerColor = if (xstreamButtonFocused) Color.White else Color(0xFF2196F3),
                            contentColor = if (xstreamButtonFocused) Color.Black else Color.White
                        ),
                        scale = ButtonDefaults.scale(
                            focusedScale = 1.05f,
                            pressedScale = 0.95f
                        )
                    ) {
                        Text("Add XStream")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (providers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No playlists found",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Gray
                    )
                    Text(
                        text = "Add your first M3U playlist to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(providers) { provider ->
                    ProviderItem(
                        provider = provider,
                        onClick = { onProviderSelected(provider) },
                        onDelete = { viewModel.deleteProvider(provider.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProviderItem(
    provider: Provider,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick)
            .onFocusChanged { isFocused = it.isFocused },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isFocused) Color(0xFF1E88E5) else Color(0xFF263238),
            contentColor = Color.White
        ),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1.02f,
            pressedScale = 0.98f
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = provider.type,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }
            
            if (isFocused) {
                var deleteButtonFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.onFocusChanged { deleteButtonFocused = it.isFocused },
                    colors = ButtonDefaults.colors(
                        containerColor = if (deleteButtonFocused) Color.White else Color.Red,
                        contentColor = if (deleteButtonFocused) Color.Red else Color.White
                    ),
                    scale = ButtonDefaults.scale(
                        focusedScale = 1.05f,
                        pressedScale = 0.95f
                    )
                ) {
                    Text("Delete")
                }
            }
        }
    }
    
    if (showDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { androidx.compose.material3.Text("Delete Playlist") },
            text = { androidx.compose.material3.Text("Are you sure you want to delete ${provider.name}?") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    androidx.compose.material3.Text("Delete")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showDeleteConfirm = false }
                ) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }
}
