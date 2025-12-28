# XStream API Integration Guide

## Overview

SupTV now supports XStream (Xtream Codes) API for importing IPTV content. XStream is a popular API protocol used by many IPTV providers worldwide.

## Features

- ✅ **Authentication** - Validates credentials before import
- ✅ **Live TV** - Import live TV channels with categories
- ✅ **VOD (Video on Demand)** - Import movies and series
- ✅ **Categories** - Automatic category organization
- ✅ **EPG Support** - EPG channel ID mapping for live streams
- ✅ **Database Storage** - All content stored locally with SQLDelight
- ✅ **Flexible Import** - Choose to import Live TV, VOD, or both

## XStream API Basics

### Server Structure

XStream servers typically use this format:
```
http://server.com:port/player_api.php?username=USER&password=PASS&action=ACTION
```

### Authentication

The API requires:
- **Server URL**: Base server address (e.g., `http://example.com:8080`)
- **Username**: Your account username
- **Password**: Your account password

### Stream URLs

**Live TV:**
```
http://server.com:port/live/USERNAME/PASSWORD/STREAM_ID.m3u8
```

**VOD:**
```
http://server.com:port/movie/USERNAME/PASSWORD/STREAM_ID.mp4
```

## Using XStream in SupTV

### 1. Import XStream Provider

#### From UI:
1. Launch SupTV
2. Click "Add XStream" button
3. Fill in the form:
   - **Provider Name**: A friendly name (e.g., "My IPTV")
   - **Server URL**: `http://cb-media1.com` (example)
   - **Username**: `username` (example)
   - **Password**: `passw` (example)
   - **Include Live TV**: ✓ Yes/No
   - **Include VOD**: ✓ Yes/No
4. Click "Import XStream"
5. Wait for import (may take a while depending on content size)

#### Programmatically:
```kotlin
val viewModel = PlaylistViewModel(database)

viewModel.importXStreamProvider(
    baseUrl = "http://cb-media1.com",
    username = "username",
    password = "passwd",
    name = "My XStream Provider",
    includeLive = true,
    includeVod = false
)

// Monitor import state
viewModel.importState.collect { state ->
    when (state) {
        is ImportState.Loading -> // Show progress
        is ImportState.Success -> // Import complete
        is ImportState.Error -> // Handle error
        else -> {}
    }
}
```

### 2. Browse Imported Content

After import:
1. Provider appears in providers list
2. Select provider → Categories screen
3. Categories organized by type:
   - Live TV categories
   - VOD categories (marked with "(VOD)")
4. Select category → Items list
5. Select item → Watch video

## API Endpoints

The XStreamClient implements these endpoints:

### Authentication
```
GET /player_api.php?username=X&password=Y
```

Response includes:
- User info (status, expiration, max connections)
- Server info (URL, ports, protocol)

### Live TV Categories
```
GET /player_api.php?username=X&password=Y&action=get_live_categories
```

Returns list of live TV categories.

### Live TV Streams
```
GET /player_api.php?username=X&password=Y&action=get_live_streams
GET /player_api.php?username=X&password=Y&action=get_live_streams&category_id=ID
```

Returns list of live streams, optionally filtered by category.

### VOD Categories
```
GET /player_api.php?username=X&password=Y&action=get_vod_categories
```

Returns list of VOD categories.

### VOD Streams
```
GET /player_api.php?username=X&password=Y&action=get_vod_streams
GET /player_api.php?username=X&password=Y&action=get_vod_streams&category_id=ID
```

Returns list of VOD streams, optionally filtered by category.

## Code Examples

### Direct API Usage

```kotlin
import com.suptv.shared.api.XStreamClient

// Create client
val client = XStreamClient(
    baseUrl = "http://server.com:port",
    username = "your_username",
    password = "your_password"
)

// Authenticate
val authResult = client.authenticate()
if (authResult.isSuccess) {
    val auth = authResult.getOrNull()
    println("Authenticated: ${auth?.user_info?.username}")
    println("Expires: ${auth?.user_info?.exp_date}")
}

// Get live categories
val categoriesResult = client.getLiveCategories()
categoriesResult.onSuccess { categories ->
    categories.forEach { category ->
        println("Category: ${category.category_name}")
    }
}

// Get live streams
val streamsResult = client.getLiveStreams()
streamsResult.onSuccess { streams ->
    streams.forEach { stream ->
        val url = client.buildLiveStreamUrl(stream.stream_id)
        println("${stream.name}: $url")
    }
}

// Get streams for specific category
val categoryStreams = client.getLiveStreams(categoryId = "123")

// Close client when done
client.close()
```

### Import Service Usage

```kotlin
import com.suptv.shared.service.XStreamImportService

val importService = XStreamImportService(database)

val result = importService.importXStreamProvider(
    baseUrl = "http://server.com",
    username = "user",
    password = "pass",
    name = "My Provider",
    includeLive = true,
    includeVod = true
)

result.onSuccess { providerId ->
    println("Import successful! Provider ID: $providerId")
}

result.onFailure { error ->
    println("Import failed: ${error.message}")
}
```

## Database Storage

### Provider Table
```sql
INSERT INTO Provider (name, url, username, password, type)
VALUES ('My XStream', 'http://server.com', 'user', 'pass', 'XStream');
```

### Categories
Live TV and VOD categories are stored separately:
- Live TV: sortOrder = 0
- VOD: sortOrder = 1000 (sorted after live)

### Items
Each stream stored as an Item:
- **type**: "LIVE" or "VOD"
- **url**: Full stream URL with credentials
- **externalId**: XStream stream_id
- **epgId**: EPG channel ID (live streams only)
- **logo**: Channel/movie poster URL

## Troubleshooting

### Authentication Failed

**Symptoms:**
- Import fails with "Authentication failed"
- Error message shows auth status

**Solutions:**
1. Verify server URL is correct (include http:// or https://)
2. Check username and password are correct
3. Ensure server is accessible from your network
4. Try opening server URL in browser
5. Contact your IPTV provider

### Import Takes Too Long

**Causes:**
- Large number of streams (thousands)
- Slow server response
- Network latency

**Solutions:**
- Import only Live TV first, add VOD later
- Be patient - large imports can take 2-5 minutes
- Check network connection
- Try again during off-peak hours

### Some Streams Don't Play

**Causes:**
- Expired subscription
- Concurrent connection limit reached
- Stream offline/unavailable
- Wrong stream format

**Solutions:**
1. Check subscription status
2. Close other connections
3. Try different streams
4. Contact provider about specific channels

### Categories Missing or Wrong

**Issue:**
- Provider doesn't return category information
- Category IDs don't match

**Solution:**
- Items without category are still imported
- Browse "All Items" view (future feature)
- Contact provider about category data

## Example Providers

### Test Provider (Example from documentation)
```
Server: http://cb-media1.com
Username: username
Password: passwd
```

⚠️ **Note**: This is an example. Always use your own credentials from your IPTV provider.

### Common XStream Providers

Most IPTV providers support XStream API. Look for:
- Login credentials (username + password)
- Server URL or DNS
- Port number (often 80, 8080, 25461)

## Security Considerations

### Credentials Storage

✅ **Stored in SQLDelight database**
- Credentials encrypted at OS level
- Only accessible by app
- Not exposed in logs

⚠️ **Transmitted in URLs**
- HTTP vs HTTPS depends on provider
- Username/password in stream URLs
- Consider VPN for additional security

### Best Practices

1. **Use HTTPS** when provider supports it
2. **VPN** recommended for privacy
3. **Don't share** credentials or database
4. **Change password** if compromised
5. **Verify provider** legitimacy

## API Response Models

### AuthResponse
```kotlin
data class AuthResponse(
    val user_info: UserInfo?,
    val server_info: ServerInfo?
)

data class UserInfo(
    val username: String?,
    val auth: Int?,  // 1 = success, 0 = failure
    val status: String?,
    val exp_date: String?,
    val is_trial: String?,
    val max_connections: String?
)
```

### Live Stream
```kotlin
data class LiveStream(
    val stream_id: Int,
    val name: String,
    val stream_icon: String?,
    val epg_channel_id: String?,
    val category_id: String?,
    val tv_archive: Int?,
    val tv_archive_duration: Int?
)
```

### VOD Stream
```kotlin
data class VodStream(
    val stream_id: Int,
    val name: String,
    val stream_icon: String?,
    val container_extension: String?,  // mp4, mkv, etc.
    val category_id: String?,
    val rating: String?,
    val added: String?
)
```

### Category
```kotlin
data class Category(
    val category_id: String,
    val category_name: String,
    val parent_id: Int?
)
```

## Performance Tips

### Large Imports

For providers with thousands of streams:

1. **Import Live TV first**
   - Usually smaller dataset
   - Faster initial setup

2. **Add VOD separately**
   - Can be done later
   - Often much larger

3. **Monitor import progress**
   - Watch loading indicator
   - Don't interrupt process

4. **Database optimization**
   - Indexes already configured
   - No action needed

### Memory Usage

- Streams loaded incrementally
- Database handles pagination
- No full dataset in memory
- Suitable for Android TV hardware

## Future Enhancements

Planned features for XStream support:

- [ ] **Series/TV Shows** - Import series with seasons/episodes
- [ ] **Catchup/Archive** - Time-shifted TV viewing
- [ ] **VOD Info** - Extended movie/series information
- [ ] **Automatic Updates** - Sync provider content periodically
- [ ] **Multi-language** - EPG and info in multiple languages
- [ ] **Adult Content Filter** - Parental control options
- [ ] **Connection Monitoring** - Track max connections usage
- [ ] **Token Refresh** - Auto-renew authentication

## References

- **XStream API Documentation**: Contact your IPTV provider
- **SupTV Database**: See DATABASE.md
- **Network Configuration**: See NETWORK_CONFIG.md

## Support

Having issues with XStream import?

1. Check server URL format
2. Verify credentials
3. Test with small import first (Live TV only)
4. Check logs: `adb logcat | grep XStream`
5. Review NETWORK_CONFIG.md for HTTP/HTTPS issues

## Summary

✅ **XStream API fully integrated**
✅ **Live TV and VOD support**
✅ **Authentication and validation**
✅ **Database storage**
✅ **Category organization**
✅ **TV-optimized UI**

Start importing your XStream IPTV provider today!
