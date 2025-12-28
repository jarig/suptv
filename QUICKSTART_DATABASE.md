# Quick Start: M3U Playlist Import

## Overview
SupTV now includes SQLDelight database integration with full M3U playlist import functionality.

## What's New

### Database Tables
- **providers**: Store M3U playlist sources
- **categories**: Group channels by type (Sports, Movies, etc.)
- **items**: Individual channels/streams with URLs
- **favorites**: User's favorite channels
- **history**: Playback history with position tracking
- **epg_channels**: EPG channel metadata (optional)
- **epg_programs**: EPG program schedule

### UI Flow
```
MainActivity
    └── ProvidersScreen (List all playlists)
            ├── "Add Playlist" → PlaylistImportScreen
            │       ├── Enter URL
            │       ├── Enter Name
            │       ├── Optional: Username/Password
            │       └── Import → Database
            └── Select Provider → Categories → Items
```

## Code Examples

### 1. Import M3U Playlist

```kotlin
// In ViewModel or Service
val viewModel = PlaylistViewModel(database)

viewModel.importPlaylistFromUrl(
    url = "http://example.com/playlist.m3u",
    name = "My IPTV"
)
```

### 2. List Providers

```kotlin
// Observe providers
viewModel.providers.collectAsState().value.forEach { provider ->
    println("${provider.name}: ${provider.url}")
}
```

### 3. Browse Content

```kotlin
// Get categories for a provider
viewModel.loadCategories(providerId)
val categories = viewModel.categories.value

// Get items in a category
viewModel.loadItems(categoryId)
val items = viewModel.items.value
```

### 4. Search Channels

```kotlin
viewModel.searchItems("sports")
val results = viewModel.items.value
```

### 5. Add to Favorites

```kotlin
val favoriteRepo = FavoriteRepository(database)
favoriteRepo.addFavorite(itemId = channelId)
```

### 6. Track History

```kotlin
val historyRepo = HistoryRepository(database)
historyRepo.addHistoryEntry(
    itemId = channelId,
    duration = durationMs,
    position = positionMs
)
```

## M3U Format Support

### Supported Attributes
- `tvg-id`: Channel ID for EPG matching
- `tvg-name`: Alternative channel name
- `tvg-logo`: Channel logo URL
- `group-title`: Category/group name
- `epg-id`: EPG channel identifier

### Example M3U Entry
```m3u
#EXTM3U
#EXTINF:-1 tvg-id="channel1" tvg-name="ESPN" tvg-logo="http://logo.png" group-title="Sports",ESPN
http://stream.example.com/espn
```

### Parser Behavior
- Extracts all `#EXTINF` metadata
- Groups channels by `group-title`
- Creates categories automatically
- Handles missing attributes gracefully
- Preserves channel order via `sortOrder`

## Database Schema

### Provider → Category → Item Hierarchy
```
Provider (M3U Source)
    ├── Category (Sports)
    │       ├── Item (ESPN)
    │       ├── Item (Fox Sports)
    │       └── Item (NBC Sports)
    └── Category (Movies)
            ├── Item (HBO)
            └── Item (Showtime)
```

### Relationships
- Provider has many Categories (1:N)
- Category has many Items (1:N)
- Item can be in Favorites (1:1)
- Item can have many History entries (1:N)
- Item can link to EPG via epgId

## Building & Running

```bash
# Build project
./gradlew build

# Install debug APK
./gradlew installDebug

# Or install release
./gradlew installRelease
```

## File Locations

### Database Schema
```
shared/src/commonMain/sqldelight/com/suptv/shared/db/*.sq
```

### Repositories
```
shared/src/commonMain/kotlin/com/suptv/shared/db/
├── PlaylistRepository.kt
├── FavoriteRepository.kt
└── HistoryRepository.kt
```

### UI Components
```
androidApp/src/main/kotlin/com/suptv/tv/
├── viewmodel/PlaylistViewModel.kt
└── ui/
    ├── ProvidersScreen.kt
    └── PlaylistImportScreen.kt
```

## Android Database Location
```
/data/data/com.suptv.tv/databases/suptv.db
```

View with:
```bash
adb shell
run-as com.suptv.tv
sqlite3 databases/suptv.db
```

## Testing Import

### Test M3U Content
```m3u
#EXTM3U
#EXTINF:-1 tvg-id="1" tvg-logo="https://example.com/logo1.png" group-title="News",CNN
http://stream.example.com/cnn
#EXTINF:-1 tvg-id="2" tvg-logo="https://example.com/logo2.png" group-title="News",BBC
http://stream.example.com/bbc
#EXTINF:-1 tvg-id="3" group-title="Sports",ESPN
http://stream.example.com/espn
```

Save as `test.m3u` and import via:
```kotlin
viewModel.importPlaylistFromString(
    content = File("test.m3u").readText(),
    name = "Test Playlist"
)
```

## Troubleshooting

### Build Issues
1. Clean build: `./gradlew clean`
2. Regenerate SQLDelight: `./gradlew generateCommonMainSupTvDatabaseInterface`
3. Invalidate caches in IDE

### Import Failures
- Check URL is accessible
- Verify M3U format is valid
- Check network permissions in manifest
- Look for errors in import state

### Database Queries
```kotlin
// Get all data
database.providerQueries.selectAll().executeAsList()
database.itemQueries.selectAll().executeAsList()

// Debug counts
println("Providers: ${database.providerQueries.selectAll().executeAsList().size}")
println("Items: ${database.itemQueries.selectAll().executeAsList().size}")
```

## Documentation

See `DATABASE.md` for comprehensive documentation including:
- Complete schema details
- All repository methods
- Migration strategies
- Performance considerations
- Future enhancements

## Support

For issues or questions:
1. Check `DATABASE.md` for detailed info
2. Review SQLDelight docs: https://cashapp.github.io/sqldelight/
3. Check M3U parser: `shared/src/commonMain/kotlin/com/suptv/shared/parser/M3UParser.kt`
