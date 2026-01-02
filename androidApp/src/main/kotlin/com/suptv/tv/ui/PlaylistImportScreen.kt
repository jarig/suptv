package com.suptv.tv.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.suptv.tv.viewmodel.ImportState
import com.suptv.tv.viewmodel.PlaylistViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlaylistImportScreen(
    viewModel: PlaylistViewModel,
    onImportSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val importState by viewModel.importState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var playlistUrl by remember { mutableStateOf("") }
    var playlistName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }
    
    LaunchedEffect(importState) {
        when (val state = importState) {
            is ImportState.Success -> {
                onImportSuccess()
            }
            is ImportState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    withDismissAction = true
                )
            }
            else -> {}
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        Text(
            text = "Import M3U Playlist",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Playlist Name
        TVTextField(
            value = playlistName,
            onValueChange = { playlistName = it },
            label = "Playlist Name",
            placeholder = "My IPTV Provider"
        )
        
        // Playlist URL
        TVTextField(
            value = playlistUrl,
            onValueChange = { playlistUrl = it },
            label = "Playlist URL",
            placeholder = "http://example.com/playlist.m3u"
        )
        
        // Advanced Options Toggle
        TVButton(
            onClick = { showAdvanced = !showAdvanced },
            text = if (showAdvanced) "Hide Advanced" else "Show Advanced"
        )
        
        if (showAdvanced) {
            // Username
            TVTextField(
                value = username,
                onValueChange = { username = it },
                label = "Username (Optional)",
                placeholder = "username"
            )
            
            // Password
            TVTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password (Optional)",
                placeholder = "password",
                isPassword = true
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Import Button
        TVButton(
            onClick = {
                if (playlistUrl.isNotBlank() && playlistName.isNotBlank()) {
                    viewModel.importPlaylistFromUrl(
                        url = playlistUrl,
                        name = playlistName,
                        username = username.takeIf { it.isNotBlank() },
                        password = password.takeIf { it.isNotBlank() }
                    )
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Please fill in all required fields (Playlist Name and URL)",
                            withDismissAction = true
                        )
                    }
                }
            },
            text = "Import Playlist",
            enabled = playlistUrl.isNotBlank() && playlistName.isNotBlank() && importState !is ImportState.Loading
        )
        
        // Cancel Button
        TVButton(
            onClick = onBack,
            text = "Cancel"
        )
        
        // Status Messages
        when (val state = importState) {
            is ImportState.Loading -> {
                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Text("Importing playlist...", color = Color.White)
            }
            is ImportState.Error -> {
                Text(
                    text = "Error: ${state.message}",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            is ImportState.Success -> {
                Text(
                    text = state.message,
                    color = Color.Green,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            else -> {}
        }
    }

        // Snackbar for error messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TVTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Column(modifier = modifier.fillMaxWidth(0.7f)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.LightGray
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                androidx.compose.material3.Text(placeholder)
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            singleLine = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = if (isPassword) {
                KeyboardOptions(keyboardType = KeyboardType.Password)
            } else {
                KeyboardOptions.Default
            },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.LightGray
            )
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TVButton(
    onClick: () -> Unit,
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth(0.5f)
            .height(56.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick),
        colors = ButtonDefaults.colors(
            containerColor = if (isFocused) Color.White else Color(0xFF1E88E5),
            contentColor = if (isFocused) Color.Black else Color.White
        ),
        scale = ButtonDefaults.scale(
            focusedScale = 1.05f,
            pressedScale = 0.95f
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
