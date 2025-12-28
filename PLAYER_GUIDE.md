# Player Integration Guide

## Overview

This guide covers the complete video player integration with categories browsing, item selection, and playback with history tracking.

## Features Implemented

### 1. Categories & Items Browsing
- **CategoriesScreen**: Browse channels organized by category
- **Horizontal category selection**: Easy navigation with D-pad
- **Vertical items list**: Shows all items in selected category
- **Real-time loading**: Categories and items load dynamically
- **Visual feedback**: Focus states and selection indicators

### 2. Video Player
- **PlayerScreen**: Full-screen video playback
- **ExoPlayer Integration**: Using Media3/ExoPlayer for streaming
- **HLS Support**: Handles HLS and direct stream URLs
- **Player Controls**: Play/pause, back navigation
- **Auto-hide controls**: Controls hide after 5 seconds
- **Keep screen on**: Prevents screen timeout during playback

### 3. History Tracking
- **Automatic tracking**: Records every playback session
- **Position tracking**: Updates playback position every 10 seconds
- **Resume support**: Can resume from last position (ready for implementation)
- **PlayerViewModel**: Manages history state

## User Flow

```
Launch App
    ↓
ProvidersScreen (List playlists)
    ↓ Select provider
CategoriesScreen
    ├── Horizontal: Browse categories
    └── Vertical: Browse items in category
        ↓ Select item
PlayerScreen (Full-screen playback)
    ├── Video playback with ExoPlayer
    ├── Overlay controls
    └── History tracking
        ↓ Press Back
CategoriesScreen (returns to browsing)
```

## Screens

### CategoriesScreen

**Location**: `androidApp/src/main/kotlin/com/suptv/tv/ui/CategoriesScreen.kt`

**Features**:
- Provider name in header
- Category count display
- Back button to return to providers
- Horizontal category chips with focus states
- Selected category highlighting
- Vertical items list with details
- Empty state handling

**Components**:
- `CategoriesScreen` - Main composable
- `CategoryChip` - Individual category selector
- `ItemCard` - Channel/stream item display

**D-pad Navigation**:
- UP/DOWN: Navigate items or categories
- LEFT/RIGHT: Navigate between categories (when focused)
- SELECT: Choose category or item
- BACK: Return to providers

### PlayerScreen

**Location**: `androidApp/src/main/kotlin/com/suptv/tv/ui/PlayerScreen.kt`

**Features**:
- Full-screen video playback
- Channel name and type overlay
- Play/pause control
- Back button with playback stop
- Auto-hide controls (5 seconds)
- Stream URL display
- Screen always on during playback

**Controls**:
- SELECT: Play/Pause
- BACK: Stop playback and return
- Any button: Show controls

## ViewModels

### PlaylistViewModel

**Location**: `androidApp/src/main/kotlin/com/suptv/tv/viewmodel/PlaylistViewModel.kt`

**Responsibilities**:
- Load providers, categories, and items
- Import playlists
- Search functionality
- State management for UI

**Key Methods**:
```kotlin
fun loadCategories(providerId: Long)
fun loadItems(categoryId: Long)
fun searchItems(query: String)
```

### PlayerViewModel

**Location**: `androidApp/src/main/kotlin/com/suptv/tv/viewmodel/PlayerViewModel.kt`

**Responsibilities**:
- Track playback history
- Update playback position
- Manage current playing item

**Key Methods**:
```kotlin
fun startPlayback(item: Item)
fun startPositionTracking(player: Player)
fun stopPlayback(player: Player)
```

**History Update Logic**:
- Creates history entry on playback start
- Updates position every 10 seconds
- Final update on playback stop
- Stores: itemId, duration, position, watchedAt

## ExoPlayer Integration

### Initialization

In `MainActivity`:
```kotlin
private val exoPlayer by lazy {
    ExoPlayer.Builder(applicationContext).build()
}
```

### Playback Start

```kotlin
val mediaItem = MediaItem.Builder()
    .setUri(item.url)
    .setMediaId(item.id.toString())
    .build()

exoPlayer.setMediaItem(mediaItem)
exoPlayer.prepare()
exoPlayer.play()
```

### Cleanup

```kotlin
override fun onDestroy() {
    super.onDestroy()
    exoPlayer.release()
}

override fun onPause() {
    super.onPause()
    exoPlayer.pause()
}
```

## MainActivity Structure

**Location**: `androidApp/src/main/kotlin/com/suptv/tv/MainActivity.kt`

**Screen Enum**:
```kotlin
enum class Screen {
    Providers,      // List playlists
    ImportPlaylist, // Import new playlist
    Categories,     // Browse categories & items
    Player          // Video playback
}
```

**State Management**:
- `currentScreen`: Current navigation state
- `selectedProvider`: Selected playlist provider
- `selectedItem`: Currently playing item

## Code Examples

### Navigate to Categories

```kotlin
ProvidersScreen(
    viewModel = playlistViewModel,
    onProviderSelected = { provider ->
        selectedProvider = provider
        currentScreen = Screen.Categories
    }
)
```

### Start Playback

```kotlin
CategoriesScreen(
    viewModel = playlistViewModel,
    provider = provider,
    onItemSelected = { item ->
        selectedItem = item
        currentScreen = Screen.Player
        
        // Setup playback
        val mediaItem = MediaItem.Builder()
            .setUri(item.url)
            .setMediaId(item.id.toString())
            .build()
        
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
        
        // Track history
        playerViewModel.startPlayback(item)
        playerViewModel.startPositionTracking(exoPlayer)
    }
)
```

### Stop Playback

```kotlin
PlayerScreen(
    item = item,
    player = exoPlayer,
    onBack = {
        playerViewModel.stopPlayback(exoPlayer)
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        currentScreen = Screen.Categories
    }
)
```

## Testing

### Test Complete Flow

1. **Import Playlist**:
   - Launch app
   - Click "Add Playlist"
   - Enter test M3U URL
   - Import

2. **Browse Content**:
   - Select imported provider
   - Navigate categories with LEFT/RIGHT
   - Browse items with UP/DOWN
   - Note focus and selection states

3. **Playback**:
   - Select an item
   - Verify video starts playing
   - Test play/pause control
   - Test back navigation
   - Check controls auto-hide

4. **History**:
   - Play multiple items
   - Query database to verify history entries
   ```kotlin
   val history = historyRepository.getAllHistory()
   println("History entries: ${history.size}")
   ```

### Test M3U Content

```m3u
#EXTM3U
#EXTINF:-1 tvg-id="1" tvg-logo="http://logo.png" group-title="Test",Test Channel 1
http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4
#EXTINF:-1 tvg-id="2" group-title="Test",Test Channel 2
http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4
```

## Performance Considerations

### Memory Management
- ExoPlayer instance is activity-scoped (single instance)
- Player released in `onDestroy()`
- Proper cleanup prevents memory leaks

### Position Tracking
- Updates every 10 seconds (configurable)
- Coroutine-based, canceled on stop
- Uses `Dispatchers.Default` for database operations

### UI Responsiveness
- Categories loaded asynchronously
- Items loaded on category selection
- StateFlow for reactive UI updates

## Troubleshooting

### Video Not Playing
1. Check stream URL is accessible
2. Verify INTERNET permission in manifest
3. Check ExoPlayer logs for errors
4. Test with known working stream

### Controls Not Responding
1. Ensure D-pad/remote is properly configured
2. Check focus states in UI
3. Verify button click handlers

### History Not Saving
1. Check database is initialized
2. Verify PlayerViewModel is created
3. Check coroutine scope is active
4. Query database directly to debug

### Navigation Issues
1. Verify currentScreen state updates
2. Check navigation callbacks
3. Ensure proper cleanup on screen transitions

## Future Enhancements

### Resume Playback
```kotlin
// In startPlayback()
val lastHistory = historyRepository.getHistoryForItem(item.id)
lastHistory?.let {
    if (it.position > 0) {
        exoPlayer.seekTo(it.position)
    }
}
```

### Picture-in-Picture
```kotlin
// In PlayerScreen
enterPictureInPictureMode(
    PictureInPictureParams.Builder()
        .setAspectRatio(Rational(16, 9))
        .build()
)
```

### EPG Integration
- Display current program info overlay
- Show program schedule
- Auto-update during playback

### Advanced Controls
- Seek forward/backward
- Subtitle selection
- Audio track selection
- Playback speed control

### Favorites Integration
- Add to favorites from player
- Quick access to favorites
- Favorites indicator in items list

## API Reference

### Item Model
```kotlin
data class Item(
    val id: Long,
    val providerId: Long,
    val categoryId: Long?,
    val externalId: String?,
    val name: String,
    val logo: String?,
    val url: String,
    val epgId: String?,
    val type: String,
    val sortOrder: Long
)
```

### History Model
```kotlin
data class History(
    val id: Long,
    val itemId: Long,
    val watchedAt: Long,
    val duration: Long,
    val position: Long
)
```

## Resources

- **ExoPlayer Documentation**: https://developer.android.com/media/media3/exoplayer
- **Compose for TV**: https://developer.android.com/jetpack/compose/tv
- **Media3 Guide**: https://developer.android.com/guide/topics/media/media3
- **SQLDelight**: https://cashapp.github.io/sqldelight/
