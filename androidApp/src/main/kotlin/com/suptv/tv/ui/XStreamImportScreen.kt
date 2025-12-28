package com.suptv.tv.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.suptv.tv.viewmodel.ImportState
import com.suptv.tv.viewmodel.PlaylistViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun XStreamImportScreen(
    viewModel: PlaylistViewModel,
    onImportSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val importState by viewModel.importState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var providerName by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var includeLive by remember { mutableStateOf(true) }
    var includeVod by remember { mutableStateOf(false) }
    
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
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        Text(
            text = "Import XStream Provider",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        
        Text(
            text = "Enter your XStream (Xtream Codes) API credentials",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Provider Name
        TVTextField(
            value = providerName,
            onValueChange = { providerName = it },
            label = "Provider Name",
            placeholder = "My IPTV Provider"
        )
        
        // Server URL
        TVTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = "Server URL",
            placeholder = "http://server.com:port"
        )
        
        // Username
        TVTextField(
            value = username,
            onValueChange = { username = it },
            label = "Username",
            placeholder = "your_username"
        )
        
        // Password
        TVTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            placeholder = "your_password",
            isPassword = true
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Content Type Selection
        Row(
            modifier = Modifier.fillMaxWidth(0.7f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Include Live TV",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )
                var liveButtonFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = { includeLive = !includeLive },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { liveButtonFocused = it.isFocused },
                    colors = ButtonDefaults.colors(
                        containerColor = when {
                            liveButtonFocused -> Color.White
                            includeLive -> Color(0xFF4CAF50)
                            else -> Color.DarkGray
                        },
                        contentColor = when {
                            liveButtonFocused -> Color.Black
                            else -> Color.White
                        }
                    )
                ) {
                    Text(if (includeLive) "✓ Yes" else "No")
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Include VOD",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )
                var vodButtonFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = { includeVod = !includeVod },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { vodButtonFocused = it.isFocused },
                    colors = ButtonDefaults.colors(
                        containerColor = when {
                            vodButtonFocused -> Color.White
                            includeVod -> Color(0xFF4CAF50)
                            else -> Color.DarkGray
                        },
                        contentColor = when {
                            vodButtonFocused -> Color.Black
                            else -> Color.White
                        }
                    )
                ) {
                    Text(if (includeVod) "✓ Yes" else "No")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Import Button
        TVButton(
            onClick = {
                if (serverUrl.isNotBlank() && username.isNotBlank() && 
                    password.isNotBlank() && providerName.isNotBlank()) {
                    viewModel.importXStreamProvider(
                        baseUrl = serverUrl,
                        username = username,
                        password = password,
                        name = providerName,
                        includeLive = includeLive,
                        includeVod = includeVod
                    )
                }
            },
            text = "Import XStream",
            enabled = serverUrl.isNotBlank() && username.isNotBlank() && 
                     password.isNotBlank() && providerName.isNotBlank() && 
                     importState !is ImportState.Loading
        )
        
        // Cancel Button
        TVButton(
            onClick = onBack,
            text = "Cancel"
        )
        
        // Status Messages - Only show loading
        when (importState) {
            is ImportState.Loading -> {
                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Text("Importing XStream provider...", color = Color.White)
                Text("This may take a while", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            else -> {}
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Info text
        Text(
            text = "Note: Live TV and VOD will be imported separately into different categories",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
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
            .onFocusChanged { isFocused = it.isFocused },
        colors = ButtonDefaults.colors(
            containerColor = if (isFocused) Color.White else Color(0xFF1E88E5),
            contentColor = if (isFocused) Color.Black else Color.White
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
