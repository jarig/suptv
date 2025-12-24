# SupTV Architecture

## Overview

SupTV is a Kotlin Multiplatform IPTV application designed with a clean architecture separating shared business logic from platform-specific implementations.

## Module Structure

### Shared Module (`shared/`)

The shared module contains all platform-agnostic code:

#### Models (`model/`)
- **Channel**: Represents a TV channel with streaming URL, metadata, and EPG information
- **Playlist**: Collection of channels with a name

#### Parsers (`parser/`)
- **M3UParser**: Parses M3U/M3U8 playlist files
  - Supports extended M3U format (#EXTM3U)
  - Extracts metadata: tvg-id, tvg-name, tvg-logo, group-title, EPG ID
  - Handles multiple channel entries
  - Returns structured `Playlist` objects

Example usage:
```kotlin
val parser = M3UParser()
val playlist = parser.parse(m3uContent, "My Playlist")
playlist.channels.forEach { channel ->
    println("${channel.name}: ${channel.url}")
}
```

#### API Clients (`api/`)
- **XStreamClient**: HTTP client for XStream (Xtream Codes) API
  - Authentication with username/password
  - Fetches live streams
  - Generates proper stream URLs
  - Parses channel metadata

Example usage:
```kotlin
val client = XStreamClient(
    baseUrl = "http://server.com:port",
    username = "user",
    password = "pass"
)

val playlist = client.getLiveStreams()
playlist.channels.forEach { channel ->
    println("${channel.name}: ${channel.url}")
}

client.close()
```

### Android TV Module (`androidApp/`)

The Android TV application provides the user interface and platform-specific functionality:

#### UI Components (`ui/`)
- **ChannelList**: TV-optimized scrollable list with D-pad navigation
- **ChannelItem**: Focusable channel entry with visual feedback
- Compose-based declarative UI
- Material Design 3 for TV

#### Player (`player/`)
- **VideoPlayer**: Wrapper around ExoPlayer/Media3
  - HLS stream support
  - Direct URL playback
  - Playback controls (play, pause, stop)
  - Resource management

#### Activities
- **MainActivity**: Entry point for Android TV
  - Leanback launcher integration
  - Compose UI setup
  - TV theme application

## Data Flow

```
M3U File / XStream API
        ↓
  Shared Module
  (Parse/Fetch)
        ↓
    Playlist
        ↓
   Android UI
   (Display)
        ↓
  User Selection
        ↓
   VideoPlayer
  (ExoPlayer)
        ↓
   Video Output
```

## Platform Support

### Current
- ✅ Android TV (API 21+)

### Planned
- 🔄 iOS (iPhone/iPad/Apple TV)
- 🔄 macOS
- 🔄 Windows

## Technology Decisions

### Kotlin Multiplatform
- Share business logic across platforms
- Reduce code duplication
- Platform-specific optimizations where needed

### Compose Multiplatform
- Modern declarative UI
- Same paradigm across platforms
- Easy to test and maintain

### ExoPlayer/Media3
- Industry-standard video playback
- HLS adaptive streaming
- Low latency
- Battery efficient

### Ktor Client
- Multiplatform HTTP client
- Kotlin-first API
- Coroutines support
- Content negotiation

### Kotlinx Serialization
- Type-safe JSON parsing
- Multiplatform support
- Kotlin compiler plugin benefits
- No reflection needed
