# suptv

Kotlin Multiplatform IPTV application supporting M3U playlists and XStream API integration.

## Features

- **M3U Playlist Parsing**: Full support for M3U/M3U8 playlist formats with metadata extraction
- **XStream API Integration**: Native support for XStream (Xtream Codes) API protocol
- **Android TV Support**: Optimized for Android TV with D-pad navigation
- **Compose UI**: Modern declarative UI using Jetpack Compose for TV
- **ExoPlayer/Media3**: Professional video playback with HLS streaming support
- **Kotlin Multiplatform**: Shared business logic across platforms (Android TV, future iOS/macOS/Windows)

## Project Structure

```
suptv/
├── shared/                          # Kotlin Multiplatform shared module
│   └── src/
│       ├── commonMain/kotlin/       # Shared code for all platforms
│       │   └── com/suptv/shared/
│       │       ├── model/           # Data models (Channel, Playlist)
│       │       ├── parser/          # M3U playlist parser
│       │       └── api/             # XStream API client
│       └── androidMain/kotlin/      # Android-specific implementations
│
└── androidApp/                      # Android TV application
    └── src/main/
        ├── kotlin/com/suptv/tv/     # Android TV UI and logic
        ├── res/                     # Android resources
        └── AndroidManifest.xml      # Android TV manifest
```

## Technology Stack

### Shared Module
- **Kotlin Multiplatform**: Cross-platform business logic
- **Kotlinx Serialization**: JSON parsing for API responses
- **Ktor Client**: HTTP client for XStream API integration
- **Kotlinx Coroutines**: Async/await pattern for network operations

### Android TV App
- **Jetpack Compose for TV**: Modern declarative UI framework
- **AndroidX TV Libraries**: TV-optimized components (tv-foundation, tv-material)
- **ExoPlayer/Media3**: Video playback engine with adaptive streaming
- **Material 3**: Design system for consistent UI

## Building the Project

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

## Future Roadmap

- [ ] iOS support via Kotlin Multiplatform
- [ ] macOS support
- [ ] Windows support
- [ ] VOD (Video on Demand) support
- [ ] EPG (Electronic Program Guide) integration
- [ ] Channel categories and favorites
- [ ] Playback controls and seeking
- [ ] Multiple playlist management

## License

TBD
