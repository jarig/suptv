# Network Configuration & Troubleshooting

## Cleartext HTTP Traffic Support

### Problem
Android 9 (API level 28) and higher block cleartext (unencrypted HTTP) traffic by default. Many IPTV providers use HTTP instead of HTTPS for their streams, which causes playback failures with the error:
```
Cleartext HTTP traffic not allowed
```

### Solution
The app is configured to allow cleartext HTTP traffic for IPTV stream compatibility.

#### 1. AndroidManifest.xml Changes
```xml
<application
    ...
    android:usesCleartextTraffic="true"
    android:networkSecurityConfig="@xml/network_security_config">
```

**Attributes added:**
- `android:usesCleartextTraffic="true"` - Allows HTTP traffic (Android 8.0+)
- `android:networkSecurityConfig` - References custom network security config

#### 2. Network Security Configuration
File: `androidApp/src/main/res/xml/network_security_config.xml`

```xml
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

**What this does:**
- Allows cleartext (HTTP) traffic globally
- Trusts system certificates (for HTTPS)
- Trusts user-installed certificates (for custom CAs)

### Security Considerations

#### Current Configuration
✅ **Allows all HTTP traffic** - Necessary for IPTV compatibility
⚠️ **Security trade-off** - HTTP traffic is not encrypted

#### For Production (Optional)
You can restrict cleartext traffic to specific domains:

```xml
<network-security-config>
    <!-- Default: HTTPS only -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    
    <!-- Allow HTTP only for specific IPTV providers -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">iptv-provider1.com</domain>
        <domain includeSubdomains="true">iptv-provider2.com</domain>
        <domain includeSubdomains="true">stream-server.net</domain>
    </domain-config>
</network-security-config>
```

**Benefits:**
- Only specified domains can use HTTP
- All other connections require HTTPS
- Better security posture

**Drawbacks:**
- Must maintain list of allowed domains
- Users with different providers may have issues
- Requires app updates for new providers

### Testing

#### Verify HTTP Support
1. Import an M3U playlist with HTTP URLs
2. Select a channel
3. Playback should start without errors

#### Check Logs
```bash
adb logcat | grep -i "cleartext\|network\|security"
```

#### Common Issues

**Issue 1: Still getting cleartext errors**
- Clean and rebuild: `./gradlew clean assembleDebug`
- Uninstall old version: `adb uninstall com.suptv.tv`
- Install fresh build: `adb install androidApp-debug.apk`

**Issue 2: HTTPS streams not working**
- Check network connectivity
- Verify SSL certificates are valid
- Check if server supports TLS 1.2+

**Issue 3: Mixed HTTP/HTTPS content**
- Current config handles both
- No additional changes needed

### Why IPTV Uses HTTP

Many IPTV providers use HTTP for various reasons:
1. **Legacy infrastructure** - Older servers and CDNs
2. **Performance** - SSL/TLS overhead on large streams
3. **CDN compatibility** - Some CDNs cache HTTP better
4. **Cost** - SSL certificates and processing costs
5. **Regional restrictions** - Some regions have older infrastructure

### Alternative Approaches

#### Option 1: Proxy/Transcoding (Future Enhancement)
- Create local HTTPS proxy
- Proxy fetches HTTP streams
- Serves to app via HTTPS
- More complex but more secure

#### Option 2: User Warning (Future Enhancement)
- Detect HTTP vs HTTPS
- Show security warning for HTTP streams
- Let user decide to continue
- Better user awareness

#### Option 3: VPN Recommendation
- Recommend users use VPN
- Encrypts all traffic including HTTP
- User responsibility
- Doesn't require app changes

### Permissions Added

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

**INTERNET** - Required for:
- Downloading playlists
- Streaming video content
- API communication

**ACCESS_NETWORK_STATE** - Optional but recommended for:
- Checking connectivity before streaming
- Handling network changes gracefully
- Better error messages

### References

- [Android Network Security Configuration](https://developer.android.com/training/articles/security-config)
- [Cleartext Traffic in Android](https://developer.android.com/guide/topics/manifest/application-element#usesCleartextTraffic)
- [Opt Out of Cleartext Traffic](https://developer.android.com/guide/topics/manifest/application-element#usesCleartextTraffic)

### Build Commands

After making network configuration changes:

```bash
# Clean build
./gradlew clean

# Rebuild with new config
./gradlew assembleDebug

# Install on device
./gradlew installDebug

# Or manual install
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

### Verification

After installation, check the configuration is applied:

```bash
# Extract and check AndroidManifest.xml
adb shell pm dump com.suptv.tv | grep -A5 "networkSecurityConfig"

# Check if cleartext is allowed
adb shell pm dump com.suptv.tv | grep "usesCleartextTraffic"
```

Expected output:
```
android:usesCleartextTraffic=true
android:networkSecurityConfig=@xml/network_security_config
```

## Summary

✅ **Problem solved** - HTTP streams now work
✅ **Configuration added** - Network security config created
✅ **Build successful** - APK includes cleartext support
⚠️ **Security note** - HTTP traffic is unencrypted (necessary for IPTV)
📝 **Production tip** - Consider domain-specific restrictions for better security

The app is now compatible with both HTTP and HTTPS IPTV streams!
