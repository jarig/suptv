package com.suptv.tv.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.suptv.shared.db.Provider
import com.suptv.tv.viewmodel.ImportState
import com.suptv.tv.viewmodel.PlaylistViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpgImportScreen(
    viewModel: PlaylistViewModel,
    provider: Provider,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val importState by viewModel.importState.collectAsState()
    
    var epgUrl by remember { mutableStateOf(provider.epgUrl ?: "") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    
    // Handle import state changes
    LaunchedEffect(importState) {
        when (val state = importState) {
            is ImportState.Success -> {
                statusMessage = state.message
                kotlinx.coroutines.delay(2000)
                viewModel.resetImportState()
                onBack()
            }
            is ImportState.Error -> {
                statusMessage = "Error: ${state.message}"
                kotlinx.coroutines.delay(3000)
                statusMessage = null
                viewModel.resetImportState()
            }
            else -> {}
        }
    }
    
    val scrollState = rememberScrollState()
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Import EPG",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = "For provider: ${provider.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                
                var backButtonFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = onBack,
                    modifier = Modifier.onFocusChanged { backButtonFocused = it.isFocused },
                    colors = ButtonDefaults.colors(
                        containerColor = if (backButtonFocused) Color.White else Color.DarkGray,
                        contentColor = if (backButtonFocused) Color.Black else Color.White
                    )
                ) {
                    Text("← Back")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // EPG URL Input Section
            Text(
                text = "EPG URL (XMLTV format, .xml or .gz)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            
            var urlFocused by remember { mutableStateOf(false) }
            Surface(
                onClick = { /* Focus handled by BasicTextField */ },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp)
                    .onFocusChanged { urlFocused = it.isFocused },
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (urlFocused) Color.White else Color(0xFF424242),
                    contentColor = if (urlFocused) Color.Black else Color.White
                ),
                shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = epgUrl,
                        onValueChange = { epgUrl = it },
                        textStyle = TextStyle(
                            color = if (urlFocused) Color.Black else Color.White,
                            fontSize = 16.sp
                        ),
                        cursorBrush = SolidColor(if (urlFocused) Color.Black else Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (epgUrl.isEmpty()) {
                                Text(
                                    text = "http://example.com/epg.xml.gz",
                                    color = Color.Gray,
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }
            
            Text(
                text = "Example: http://s02.wsbof.com:8080/xml/myepkg.gz",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Import Button
            val canImport = epgUrl.isNotBlank() && importState !is ImportState.Loading
            
            var importButtonFocused by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    if (canImport) {
                        viewModel.updateProviderEpgUrl(provider.id, epgUrl)
                        viewModel.importEpgFromUrl(epgUrl, provider.id)
                    }
                },
                enabled = canImport,
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .onFocusChanged { importButtonFocused = it.isFocused },
                colors = ButtonDefaults.colors(
                    containerColor = if (importButtonFocused) Color.White else Color(0xFF1E88E5),
                    contentColor = if (importButtonFocused) Color.Black else Color.White,
                    disabledContainerColor = Color.DarkGray,
                    disabledContentColor = Color.Gray
                )
            ) {
                if (importState is ImportState.Loading) {
                    Text("Importing EPG...")
                } else {
                    Text("Import EPG")
                }
            }
            
            // Progress indicator
            if (importState is ImportState.Loading) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(0.7f),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color(0xFF263238),
                        contentColor = Color.White
                    ),
                    shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val loadingState = importState as? ImportState.Loading
                        if (loadingState != null) {
                            Text(
                                text = loadingState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = loadingState.progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = Color(0xFF1E88E5),
                                trackColor = Color(0xFF424242)
                            )
                            
                            Text(
                                text = "${(loadingState.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Info
            Surface(
                onClick = {},
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color(0xFF263238),
                    contentColor = Color.White
                ),
                shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "📺 EPG Import Information",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    
                    Text(
                        text = "• EPG (Electronic Program Guide) shows TV schedules",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                    
                    Text(
                        text = "• Supports XMLTV format (.xml or .gz compressed)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                    
                    Text(
                        text = "• EPG data is linked to channels by channel ID",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                    
                    Text(
                        text = "• Import may take a few moments for large EPG files",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                    
                    Text(
                        text = "• Note: Use remote control to type URL character by character",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Yellow
                    )
                }
            }
            
            // Status message
            statusMessage?.let { message ->
                Surface(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(top = 24.dp),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (message.startsWith("Error")) Color(0xFFD32F2F) else Color(0xFF388E3C),
                        contentColor = Color.White
                    ),
                    shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium)
                ) {
                    Box(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
