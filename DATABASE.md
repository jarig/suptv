# SQLDelight Database Integration

## Overview

This document describes the SQLDelight database integration added to SupTV for managing playlists, channels, categories, favorites, history, and EPG data.

## Database Schema

### Tables

#### 1. Provider
Stores M3U playlist providers (sources)
- `id`: Primary key (auto-increment)
- `name`: Provider name
- `url`: Playlist URL
- `username`: Optional authentication
- `password`: Optional authentication
- `type`: Provider type (M3U, XStream, etc.)
- `createdAt`: Creation timestamp
- `updatedAt`: Last update timestamp

#### 2. Category
Organizes content into groups
- `id`: Primary key (auto-increment)
- `providerId`: Foreign key to Provider
- `name`: Category name (e.g., "Sports", "Movies")
- `sortOrder`: Display order

#### 3. Item
Represents individual channels/streams
- `id`: Primary key (auto-increment)
- `providerId`: Foreign key to Provider
- `categoryId`: Foreign key to Category (nullable)
- `externalId`: External ID from provider
- `name`: Channel/stream name
- `logo`: Logo URL
- `url`: Stream URL
- `epgId`: EPG channel ID for matching program data
- `type`: Content type (LIVE, VOD, etc.)
- `sortOrder`: Display order

#### 4. Favorite
User's favorite channels
- `id`: Primary key (auto-increment)
- `itemId`: Foreign key to Item (unique)
- `addedAt`: Timestamp when favorited

#### 5. History
Playback history
- `id`: Primary key (auto-increment)
- `itemId`: Foreign key to Item
- `watchedAt`: Timestamp of playback
- `duration`: Total duration in milliseconds
- `position`: Last playback position in milliseconds

#### 6. EpgChannel
EPG channel metadata
- `id`: Channel ID (text primary key)
- `displayName`: Display name
- `icon`: Icon URL
- `url`: Channel URL

#### 7. EpgProgram
EPG program schedule
- `id`: Primary key (auto-increment)
- `channelId`: Foreign key to EpgChannel
- `title`: Program title
- `description`: Program description
- `startTime`: Start timestamp
- `endTime`: End timestamp
- `category`: Program category
- `icon`: Program icon URL

## Repositories

### PlaylistRepository
Handles playlist import and content management
- `importPlaylist()`: Import M3U playlist to database
- `getAllProviders()`: Get all providers
- `getProvider()`: Get single provider
- `deleteProvider()`: Delete provider and all its content
- `getCategoriesByProvider()`: Get categories for a provider
- `getItemsByCategory()`: Get items in a category
- `getItemsByProvider()`: Get all items from a provider
- `searchItems()`: Search items by name

### FavoriteRepository
Manages user favorites
- `addFavorite()`: Add item to favorites
- `removeFavorite()`: Remove from favorites
- `isFavorite()`: Check if item is favorited
- `getAllFavorites()`: Get all favorites with item details
- `clearAllFavorites()`: Remove all favorites

### HistoryRepository
Tracks viewing history
- `addHistoryEntry()`: Add new history entry
- `updatePosition()`: Update playback position
- `getAllHistory()`: Get complete history
- `getRecentHistory()`: Get recent history (limited)
- `getHistoryForItem()`: Get latest history for item
- `deleteHistoryEntry()`: Delete single entry
- `deleteHistoryForItem()`: Delete all history for item
- `deleteOldHistory()`: Delete history before timestamp
- `clearAllHistory()`: Remove all history

## Services

### PlaylistImportService
Handles M3U playlist import workflow
- `importFromUrl()`: Download and import M3U from URL
- `importFromString()`: Import M3U from string content

## UI Components

### ProvidersScreen
Displays list of imported playlists
- Shows all providers
- Add new provider button
- Delete provider with confirmation
- Navigate to provider content

### PlaylistImportScreen
UI for importing M3U playlists
- Playlist name input
- Playlist URL input
- Optional username/password for authenticated playlists
- Import progress indicator
- Success/error feedback

### PlaylistViewModel
ViewModel managing playlist state
- Import state management
- Provider list
- Categories and items
- Search functionality

## Usage Example

### Importing a Playlist

```kotlin
// In your Activity/Fragment
val database = createDatabase(DatabaseDriverFactory(context))
val viewModel = PlaylistViewModel(database)

// Import from URL
viewModel.importPlaylistFromUrl(
    url = "http://example.com/playlist.m3u",
    name = "My IPTV Provider",
    username = "user",  // optional
    password = "pass"   // optional
)

// Observe state
viewModel.importState.collect { state ->
    when (state) {
        is ImportState.Loading -> // Show loading
        is ImportState.Success -> // Show success
        is ImportState.Error -> // Show error
        else -> {}
    }
}
```

### Accessing Content

```kotlin
// Get all providers
viewModel.loadProviders()
val providers = viewModel.providers.value

// Get categories for a provider
viewModel.loadCategories(providerId = 1)
val categories = viewModel.categories.value

// Get items in a category
viewModel.loadItems(categoryId = 1)
val items = viewModel.items.value

// Search items
viewModel.searchItems("sports")
```

### Managing Favorites

```kotlin
val favoriteRepo = FavoriteRepository(database)

// Add to favorites
favoriteRepo.addFavorite(itemId = 123)

// Check if favorited
val isFav = favoriteRepo.isFavorite(itemId = 123)

// Get all favorites
val favorites = favoriteRepo.getAllFavorites()
```

### Tracking History

```kotlin
val historyRepo = HistoryRepository(database)

// Add history entry
historyRepo.addHistoryEntry(
    itemId = 123,
    duration = 3600000,  // 1 hour in ms
    position = 1800000   // 30 min in ms
)

// Get recent history
val recent = historyRepo.getRecentHistory(limit = 20)
```

## Database Configuration

### Location
- Android: `/data/data/com.suptv.tv/databases/suptv.db`

### Driver Factory
Platform-specific database driver creation is handled by `DatabaseDriverFactory`:
- Android: Uses `AndroidSqliteDriver`

### Initialization
The database is initialized in `MainActivity`:

```kotlin
private val database by lazy {
    createDatabase(DatabaseDriverFactory(applicationContext))
}
```

## M3U Parser Integration

The M3U parser (`M3UParser`) extracts channel information from M3U playlists:
- Parses `#EXTINF` lines for metadata
- Extracts attributes: `tvg-id`, `tvg-name`, `tvg-logo`, `group-title`
- Maps to `Channel` model
- Grouped by category (`group-title`)

Import flow:
1. Download/read M3U content
2. Parse with `M3UParser`
3. Group channels by category
4. Insert Provider → Categories → Items

## Migrations

SQLDelight schema is defined in `.sq` files in:
```
shared/src/commonMain/sqldelight/com/suptv/shared/db/
```

For schema changes:
1. Update `.sq` files
2. SQLDelight generates migration files automatically
3. Implement migration in database creation if needed

## Performance Considerations

- Indexes on foreign keys for faster joins
- Indexes on search fields (EPG time ranges)
- Batch operations for large playlist imports
- Coroutines with Dispatchers.Default for database operations
- Cascade deletes to maintain referential integrity

## Future Enhancements

1. **Multi-provider sync**: Background sync for multiple providers
2. **EPG integration**: Full XMLTV EPG parser and importer
3. **Backup/Restore**: Export/import database
4. **Statistics**: Watch time, most watched channels
5. **Recommendations**: Based on viewing history
6. **Provider management**: Update existing providers, refresh content
7. **Advanced search**: Filter by category, type, EPG data
