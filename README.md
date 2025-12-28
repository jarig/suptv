# SupTV

Kotlin Multiplatform IPTV application with complete playlist management, video playback, and watch history tracking.

## ✨ Features

### Core Functionality
- **M3U Playlist Import**: Import playlists from URL or string with authentication support
- **SQLDelight Database**: Local storage for playlists, channels, favorites, and history
- **Category Organization**: Automatic channel grouping by category
- **Video Playback**: Full-screen streaming with ExoPlayer/Media3
- **Watch History**: Automatic playback tracking with position resume
- **TV-Optimized UI**: D-pad navigation and focus management

### Playlist Management
- Multiple provider support
- M3U/M3U8 format parsing with metadata extraction
- XStream API integration (ready for implementation)
- Category-based organization
- Search across all channels
- Provider CRUD operations

### Playback Features
- HLS and direct stream URL support
- Play/pause controls
- Auto-hide player controls (5 seconds)
- Playback position tracking (every 10 seconds)
- Keep screen on during playback
- Back navigation with proper cleanup

### Database & History
- 7 tables: Provider, Category, Item, Favorite, History, EpgChannel, EpgProgram
- Foreign key relationships with cascade delete
- Indexed queries for performance
- Favorites system (ready to use)
- Watch history with position tracking
- EPG support (infrastructure ready)

## 🎯 User Flow

```
Launch App → Import Playlist → Browse Categories → Select Item → Watch Video
    ↓            ↓                  ↓                  ↓             ↓
Providers     Enter URL         Horizontal        Vertical     Full-screen
  Screen        & Name           Categories        Items         Player
                                   Scroll           List        + Controls
```

## 📁 Project Structure

```
suptv/
├── shared/                          # Kotlin Multiplatform shared module
│   └── src/
│       ├── commonMain/
│       │   ├── kotlin/com/suptv/shared/
│       │   │   ├── db/              # Database repositories
│       │   │   ├── model/           # Data models
│       │   │   ├── parser/          # M3U parser
│       │   │   ├── api/             # XStream API client
│       │   │   └── service/         # Business logic services
│       │   └── sqldelight/          # Database schema (.sq files)
│       └── androidMain/kotlin/      # Android-specific implementations
│
└── androidApp/                      # Android TV application
    └── src/main/kotlin/com/suptv/tv/
        ├── ui/                      # Compose UI screens
        │   ├── ProvidersScreen.kt
        │   ├── PlaylistImportScreen.kt
        │   ├── CategoriesScreen.kt
        │   └── PlayerScreen.kt
        ├── viewmodel/               # ViewModels
        │   ├── PlaylistViewModel.kt
        │   └── PlayerViewModel.kt
        ├── player/                  # Video player
        └── MainActivity.kt          # Entry point
```

## 🛠 Technology Stack

### Shared Module
- **Kotlin Multiplatform**: Cross-platform business logic
- **SQLDelight 2.0**: Type-safe SQL with Kotlin
- **Kotlinx Serialization**: JSON parsing
- **Ktor Client**: HTTP client for network requests
- **Kotlinx Coroutines**: Async operations

### Android TV App
- **Jetpack Compose for TV**: Declarative UI framework
- **AndroidX TV Libraries**: TV-optimized components
- **ExoPlayer/Media3**: Professional video playback
- **Material 3**: Design system
- **ViewModel & StateFlow**: State management

## 🚀 Building the Project

### Prerequisites
- JDK 17 or higher
- Android SDK with API 34
- Gradle 8.5 (included via wrapper)

### Build Commands

```bash
# Build the entire project
./gradlew build

# Build Android TV app
./gradlew :androidApp:assembleDebug

# Install on Android TV device/emulator
./gradlew :androidApp:installDebug
```

## Architecture

### M3U Parser
The M3U parser (`shared/src/commonMain/.../parser/M3UParser.kt`) supports:
- Extended M3U format (#EXTM3U)
- Channel metadata extraction (tvg-id, tvg-name, tvg-logo, group-title)
- EPG integration support

### XStream Client
The XStream client (`shared/src/commonMain/.../api/XStreamClient.kt`) provides:
- Authentication with username/password
- Live streams fetching
- Channel metadata parsing
- Stream URL generation

### Android TV Integration
- **Leanback Launcher**: Proper Android TV home screen integration
- **D-pad Navigation**: Full remote control support via Compose
- **Landscape Mode**: TV-optimized orientation
- **No Touchscreen Required**: Designed for TV input devices

## 📱 Usage

### Quick Start

1. **Launch the app** on your Android TV device
2. **Import a playlist**:
   - Click "Add Playlist"
   - Enter playlist name (e.g., "My IPTV")
   - Enter M3U URL
   - Optionally add username/password for authenticated streams
   - Click "Import Playlist"
3. **Browse content**:
   - Select the imported provider
   - Navigate categories with LEFT/RIGHT
   - Browse items with UP/DOWN
4. **Watch video**:
   - Select an item to start playback
   - Use SELECT to play/pause
   - Press BACK to return to browsing

### D-Pad Controls

**Providers/Categories Screen:**
- UP/DOWN: Navigate items
- LEFT/RIGHT: Navigate categories
- SELECT: Choose item
- BACK: Previous screen

**Player Screen:**
- SELECT: Play/Pause
- BACK: Stop and return
- Any button: Show controls

## 📚 Documentation

- **[DATABASE.md](DATABASE.md)** - Complete database schema and repository guide
- **[QUICKSTART_DATABASE.md](QUICKSTART_DATABASE.md)** - Quick reference for database operations
- **[PLAYER_GUIDE.md](PLAYER_GUIDE.md)** - Player integration and usage guide
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Architecture and design decisions
- **[EXAMPLES.md](EXAMPLES.md)** - Code examples and recipes
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Contribution guidelines

## 🎯 Roadmap

### Completed ✅
- [x] M3U playlist parsing
- [x] SQLDelight database integration
- [x] Playlist import from URL
- [x] Category-based organization
- [x] Video playback with ExoPlayer
- [x] Watch history tracking
- [x] Multiple playlist management
- [x] Android TV optimized UI
- [x] D-pad navigation
- [x] EPG (Electronic Program Guide) integration
- [x] XStream API support

### In Progress 🚧
- [ ] Choose 'default' playlist that should be chosen on the app start
- [ ] Resume playback from last position
- [ ] Favorites management UI

### Planned 📋
- [ ] Search among items using local database and be able to find / select and play found items
- [ ] Advanced player controls (seek, speed)

## Future Roadmap

- [ ] iOS support via Kotlin Multiplatform
- [ ] macOS support
- [ ] Windows support
- [ ] VOD (Video on Demand) support
- [ ] Channel categories and favorites
- [ ] Playback controls and seeking
- [ ] Multiple playlist management

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

For detailed architecture information, see [ARCHITECTURE.md](ARCHITECTURE.md).

For usage examples, see [EXAMPLES.md](EXAMPLES.md).

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
