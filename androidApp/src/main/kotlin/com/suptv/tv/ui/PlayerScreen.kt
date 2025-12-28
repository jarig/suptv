package com.suptv.tv.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import com.suptv.shared.db.Item
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    item: Item,
    player: Player,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    val focusRequester = remember { FocusRequester() }
    
    // Auto-hide controls after 5 seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }
    
    // Monitor playback state
    LaunchedEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
    }
    
    // Request focus on first composition
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        // LEFT key - go back immediately
                        Key.DirectionLeft -> {
                            onBack()
                            true
                        }
                        // BACK key - go back
                        Key.Back -> {
                            onBack()
                            true
                        }
                        // CENTER/SELECT key - toggle play/pause
                        Key.DirectionCenter, Key.Enter -> {
                            if (isPlaying) {
                                player.pause()
                            } else {
                                player.play()
                            }
                            isPlaying = !isPlaying
                            showControls = true
                            true
                        }
                        // Any other key - show controls
                        else -> {
                            showControls = true
                            true
                        }
                    }
                } else {
                    false
                }
            }
    ) {
        // Video player view
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false // We'll use custom controls
                    keepScreenOn = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Show controls overlay when visible
        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                // Top bar with title and back button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Text(
                            text = item.type,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )
                    }
                    
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.colors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("← Back")
                    }
                }
                
                // Center playback controls
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Play/Pause button
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                player.pause()
                            } else {
                                player.play()
                            }
                            isPlaying = !isPlaying
                            showControls = true
                        },
                        modifier = Modifier.size(80.dp)
                    ) {
                        Text(
                            text = if (isPlaying) "⏸" else "▶",
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White
                        )
                    }
                }
                
                // Bottom info bar with keyboard shortcuts
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                ) {
                    item.url.let { url ->
                        Text(
                            text = "Stream: ${url.take(50)}${if (url.length > 50) "..." else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Controls: ← Back | SELECT Play/Pause | Any key Show controls",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
