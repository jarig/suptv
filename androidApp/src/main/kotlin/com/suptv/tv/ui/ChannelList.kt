package com.suptv.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.suptv.shared.model.Channel

/**
 * TV-optimized channel list with D-pad navigation
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelList(
    channels: List<Channel>,
    onChannelSelected: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(channels) { channel ->
            ChannelItem(
                channel = channel,
                onClick = { onChannelSelected(channel) }
            )
        }
    }
}

/**
 * Individual channel item with focus support
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelItem(
    channel: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent
            ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isFocused) Color(0xFF1E88E5) else Color(0xFF263238),
            contentColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                channel.group?.let { group ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = group,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}
