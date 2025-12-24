# Examples

## M3U Playlist Parsing

### Example M3U File Format

```m3u
#EXTM3U
#EXTINF:-1 tvg-id="channel1" tvg-name="BBC One" tvg-logo="http://example.com/bbc1.png" group-title="UK Channels",BBC One
http://stream.example.com/bbc1/index.m3u8
#EXTINF:-1 tvg-id="channel2" tvg-name="ITV" tvg-logo="http://example.com/itv.png" group-title="UK Channels",ITV
http://stream.example.com/itv/index.m3u8
#EXTINF:-1 tvg-id="channel3" tvg-name="CNN" tvg-logo="http://example.com/cnn.png" group-title="News",CNN International
http://stream.example.com/cnn/index.m3u8
```

### Parsing M3U in Kotlin

```kotlin
import com.suptv.shared.parser.M3UParser
import java.io.File

fun main() {
    // Load M3U file
    val m3uContent = File("playlist.m3u").readText()
    
    // Parse the playlist
    val parser = M3UParser()
    val playlist = parser.parse(m3uContent, "My IPTV Playlist")
    
    // Access channels
    println("Playlist: ${playlist.name}")
    println("Total channels: ${playlist.channels.size}")
    
    // Iterate through channels
    playlist.channels.forEach { channel ->
        println("\nChannel: ${channel.name}")
        println("  ID: ${channel.id}")
        println("  Group: ${channel.group ?: "None"}")
        println("  Logo: ${channel.logo ?: "No logo"}")
        println("  URL: ${channel.url}")
        println("  EPG ID: ${channel.epgId ?: "No EPG"}")
    }
    
    // Filter channels by group
    val ukChannels = playlist.channels.filter { it.group == "UK Channels" }
    println("\nUK Channels: ${ukChannels.size}")
    
    // Find specific channel
    val bbcOne = playlist.channels.find { it.name == "BBC One" }
    bbcOne?.let {
        println("\nFound BBC One: ${it.url}")
    }
}
```

## XStream API Integration

### Using XStreamClient

```kotlin
import com.suptv.shared.api.XStreamClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // Create XStream client
    val client = XStreamClient(
        baseUrl = "http://your-server.com:8080",
        username = "your_username",
        password = "your_password"
    )
    
    try {
        // Fetch live streams
        val playlist = client.getLiveStreams()
        
        println("XStream Playlist: ${playlist.name}")
        println("Total channels: ${playlist.channels.size}")
        
        // Display channels
        playlist.channels.take(10).forEach { channel ->
            println("\nChannel: ${channel.name}")
            println("  Stream ID: ${channel.id}")
            println("  URL: ${channel.url}")
            println("  Logo: ${channel.logo ?: "No logo"}")
        }
    } catch (e: Exception) {
        println("Error fetching streams: ${e.message}")
    } finally {
        // Always close the client
        client.close()
    }
}
```

## Android TV Video Playback

### Playing a Channel with VideoPlayer

```kotlin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.suptv.shared.model.Channel
import com.suptv.tv.player.VideoPlayer

class PlayerActivity : ComponentActivity() {
    private lateinit var videoPlayer: VideoPlayer
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize video player
        videoPlayer = VideoPlayer(this)
        
        // Create a sample channel
        val channel = Channel(
            id = "1",
            name = "Test Channel",
            url = "http://stream.example.com/channel.m3u8",
            logo = null,
            group = "Test",
            epgId = null
        )
        
        // Play the channel
        videoPlayer.playChannel(channel)
        
        setContent {
            // Your Compose UI here
        }
    }
    
    override fun onPause() {
        super.onPause()
        videoPlayer.pause()
    }
    
    override fun onResume() {
        super.onResume()
        videoPlayer.play()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        videoPlayer.release()
    }
}
```

## Compose UI with Channel List

### Display Channels in Android TV

```kotlin
import androidx.compose.runtime.*
import com.suptv.shared.model.Channel
import com.suptv.shared.parser.M3UParser
import com.suptv.tv.ui.ChannelList
import kotlinx.coroutines.launch

@Composable
fun ChannelsScreen(
    m3uContent: String,
    onChannelSelected: (Channel) -> Unit
) {
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch {
            // Parse M3U in background
            val parser = M3UParser()
            val playlist = parser.parse(m3uContent, "My Channels")
            channels = playlist.channels
        }
    }
    
    // Display channel list with D-pad navigation
    ChannelList(
        channels = channels,
        onChannelSelected = onChannelSelected
    )
}
```

## Complete Android TV Example

### Main Activity with Parsing and Playback

```kotlin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.suptv.shared.model.Channel
import com.suptv.shared.parser.M3UParser
import com.suptv.tv.player.VideoPlayer
import com.suptv.tv.ui.ChannelList
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var videoPlayer: VideoPlayer
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        videoPlayer = VideoPlayer(this)
        
        setContent {
            var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
            val scope = rememberCoroutineScope()
            
            LaunchedEffect(Unit) {
                scope.launch {
                    // Load and parse M3U
                    val m3uContent = """
                        #EXTM3U
                        #EXTINF:-1 tvg-id="1" tvg-name="Test Channel",Test Channel
                        http://stream.example.com/test.m3u8
                    """.trimIndent()
                    
                    val parser = M3UParser()
                    val playlist = parser.parse(m3uContent)
                    channels = playlist.channels
                }
            }
            
            ChannelList(
                channels = channels,
                onChannelSelected = { channel ->
                    videoPlayer.playChannel(channel)
                }
            )
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        videoPlayer.release()
    }
}
```

## Testing M3U Parser

### Unit Test Example

```kotlin
import com.suptv.shared.parser.M3UParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class M3UParserTest {
    
    @Test
    fun testBasicM3UParsing() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-id="1" tvg-name="Test",Test Channel
            http://example.com/test.m3u8
        """.trimIndent()
        
        val parser = M3UParser()
        val playlist = parser.parse(m3uContent)
        
        assertEquals(1, playlist.channels.size)
        assertEquals("Test Channel", playlist.channels[0].name)
        assertEquals("1", playlist.channels[0].id)
        assertEquals("http://example.com/test.m3u8", playlist.channels[0].url)
    }
    
    @Test
    fun testMultipleChannels() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-id="1",Channel 1
            http://example.com/ch1.m3u8
            #EXTINF:-1 tvg-id="2",Channel 2
            http://example.com/ch2.m3u8
        """.trimIndent()
        
        val parser = M3UParser()
        val playlist = parser.parse(m3uContent)
        
        assertEquals(2, playlist.channels.size)
    }
}
```
