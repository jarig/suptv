package com.suptv.tv.ui

import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.media3.ui.AspectRatioFrameLayout
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
    
    // Auto-start playback and request focus
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        player.play()
        isPlaying = true
    }
    
    // Enhanced watchdog to detect stuck streams and rendering issues
    LaunchedEffect(player) {
        var lastPosition = 0L
        var stuckCount = 0
        var bufferingStartTime = 0L
        var lastRenderedFrameTime = System.currentTimeMillis()
        var readyStateTime = 0L
        
        while (true) {
            delay(3000) // Check every 3 seconds
            
            val currentPosition = player.currentPosition
            val playerIsPlaying = player.isPlaying
            val playbackState = player.playbackState
            val bufferedPercentage = player.bufferedPercentage
            val bufferedPosition = player.bufferedPosition
            val currentTime = System.currentTimeMillis()
            
            val stateStr = when (playbackState) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "UNKNOWN"
            }
            
            Log.d("PlayerScreen", "Watchdog: state=$stateStr, playing=$playerIsPlaying, pos=$currentPosition, lastPos=$lastPosition, buffered=$bufferedPercentage%, bufferedPos=$bufferedPosition")
            
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    if (bufferingStartTime == 0L) {
                        bufferingStartTime = currentTime
                    }
                    
                    val bufferingDuration = currentTime - bufferingStartTime
                    
                    // If buffering for more than 20 seconds, restart
                    if (bufferingDuration > 20000) {
                        Log.e("PlayerScreen", "Buffering stuck for ${bufferingDuration}ms, restarting stream...")
                        player.stop()
                        delay(500)
                        player.prepare()
                        player.play()
                        stuckCount = 0
                        bufferingStartTime = 0L
                        readyStateTime = 0L
                        lastPosition = 0L
                        lastRenderedFrameTime = currentTime
                    }
                }
                Player.STATE_READY -> {
                    bufferingStartTime = 0L
                    
                    if (readyStateTime == 0L) {
                        readyStateTime = currentTime
                    }
                    
                    // Only check for rendering issues if player is actively playing
                    if (playerIsPlaying) {
                        val timeInReadyState = currentTime - readyStateTime
                        
                        // For live streams, if we've been in READY state for more than 15 seconds
                        // without any user interaction (pauses), but the stream seems stuck,
                        // force a renderer reset
                        if (timeInReadyState > 15000) {
                            // Check if we have full buffer but suspicious behavior
                            if (bufferedPercentage >= 95) {
                                Log.w("PlayerScreen", "Possible renderer freeze detected (ready for ${timeInReadyState}ms with full buffer), forcing reset...")
                                
                                // Store current position
                                val savedPosition = player.currentPosition
                                
                                // Force complete reset
                                player.stop()
                                delay(300)
                                player.prepare()
                                
                                // Try to seek close to where we were (for live streams this may not work)
                                if (savedPosition > 0) {
                                    try {
                                        player.seekTo(savedPosition)
                                    } catch (e: Exception) {
                                        Log.w("PlayerScreen", "Could not seek after reset: ${e.message}")
                                    }
                                }
                                
                                player.play()
                                readyStateTime = currentTime
                                lastRenderedFrameTime = currentTime
                            }
                        }
                    } else {
                        // Player is paused by user, reset counters
                        readyStateTime = currentTime
                    }
                }
                Player.STATE_ENDED -> {
                    // Live stream ended unexpectedly, try to reconnect
                    Log.w("PlayerScreen", "Stream ended unexpectedly, attempting reconnect...")
                    delay(2000)
                    player.prepare()
                    player.play()
                    readyStateTime = 0L
                    bufferingStartTime = 0L
                }
                Player.STATE_IDLE -> {
                    Log.w("PlayerScreen", "Player in IDLE state, may need restart")
                }
            }
            
            lastPosition = currentPosition
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Toggle controls on screen click
                showControls = !showControls
            }
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
                        // CENTER/SELECT key - toggle play/pause and show controls
                        Key.DirectionCenter, Key.Enter -> {
                            if (isPlaying) {
                                player.pause()
                            } else {
                                Log.i("PlayerScreen", "Starting playback")
                                player.play()
                            }
                            isPlaying = !isPlaying
                            showControls = true
                            true
                        }
                        // Ignore volume keys
                        Key.VolumeUp, Key.VolumeDown, Key.VolumeMute -> {
                            false
                        }
                        // Any other key - show controls
                        else -> {
                            showControls = true
                            false
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
                    useController = false
                    keepScreenOn = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
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
                        ),
                        modifier = Modifier.clickable(onClick = onBack)
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
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (isPlaying) {
                                    player.pause()
                                } else {
                                    player.play()
                                }
                                isPlaying = !isPlaying
                                showControls = true
                            }
                            .focusable()
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyUp && 
                                    keyEvent.key == Key.Enter) {
                                    if (isPlaying) {
                                        player.pause()
                                    } else {
                                        player.play()
                                    }
                                    isPlaying = !isPlaying
                                    showControls = true
                                    true
                                } else {
                                    false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPlaying) "⏸" else "▶",
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White
                        )
                    }
                    
                    // Reload Stream button
                    Button(
                        onClick = {
                            Log.i("PlayerScreen", "Manual stream reload requested")
                            player.stop()
                            player.prepare()
                            player.play()
                            isPlaying = true
                            showControls = true
                        },
                        colors = ButtonDefaults.colors(
                            containerColor = Color.White.copy(alpha = 0.3f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.clickable {
                            Log.i("PlayerScreen", "Manual stream reload requested")
                            player.stop()
                            player.prepare()
                            player.play()
                            isPlaying = true
                            showControls = true
                        }
                    ) {
                        Text("🔄 Reload")
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
                        text = "Controls: ← Back | SELECT Play/Pause | 🔄 Reload stream if frozen",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
