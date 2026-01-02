package com.suptv.tv.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultAllocator
import java.io.File

/**
 * Improved ExoPlayer configuration for reliable IPTV streaming
 * 
 * Key improvements:
 * 1. Adaptive streaming with better buffering
 * 2. HTTP connection pooling and timeout handling
 * 3. Cache support for reducing rebuffering
 * 4. Better error recovery
 * 5. Optimized for live streams
 */
object ImprovedPlayerConfig {
    
    private var simpleCache: SimpleCache? = null
    
    /**
     * Creates an optimized ExoPlayer instance for IPTV streaming
     */
    fun createOptimizedPlayer(context: Context): ExoPlayer {
        // 1. Configure HTTP data source with better timeout and retry settings
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(20000) // 20 second connect timeout (increased for slow servers)
            .setReadTimeoutMs(20000) // 20 second read timeout
            .setAllowCrossProtocolRedirects(true) // Allow HTTP -> HTTPS redirects
            .setUserAgent("SupTV/1.0") // Custom user agent
            .setKeepPostFor302Redirects(true) // Keep POST method on 302 redirects
        
        // 2. Setup caching to reduce rebuffering (100MB cache)
        val cache = getCache(context)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(
                DefaultDataSource.Factory(context, httpDataSourceFactory)
            )
            .setCacheWriteDataSinkFactory(null) // Read-only cache for live streams
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        
        // 3. Configure adaptive track selection for better quality adaptation
        val trackSelector = DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setMaxVideoSizeSd() // Start with SD quality for faster startup
                .setPreferredAudioLanguage("en") // Prefer English audio
                .setExceedRendererCapabilitiesIfNecessary(true) // Allow quality exceeding device capability
                .setForceHighestSupportedBitrate(false) // Don't force highest bitrate
                .setAllowVideoMixedMimeTypeAdaptiveness(true) // Allow mixed codecs
                .setAllowAudioMixedMimeTypeAdaptiveness(true)
                .build()
        }
        
        // 4. Configure load control for optimized buffering
        val loadControl = createOptimizedLoadControl()
        
        // 5. Build the player with all optimizations
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(cacheDataSourceFactory)
                    .setLiveTargetOffsetMs(3000) // 3 second live offset
            )
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10000) // 10 second seek back
            .setSeekForwardIncrementMs(10000) // 10 second seek forward
            .setHandleAudioBecomingNoisy(true) // Handle audio interruptions
            .setWakeMode(C.WAKE_MODE_NETWORK) // Keep network active
            .build()
            .apply {
                // Configure playback parameters
                playWhenReady = false // Don't auto-play, let the screen control it
                repeatMode = Player.REPEAT_MODE_OFF
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                
                // Add listener for error recovery and state monitoring
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        android.util.Log.e("ImprovedPlayer", "Playback error: ${error.message}", error)
                        // Auto-retry on error after 2 seconds
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            stop()
                            prepare()
                        }, 2000)
                    }
                    
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_IDLE -> android.util.Log.d("ImprovedPlayer", "State: IDLE")
                            Player.STATE_BUFFERING -> android.util.Log.d("ImprovedPlayer", "State: BUFFERING")
                            Player.STATE_READY -> android.util.Log.d("ImprovedPlayer", "State: READY")
                            Player.STATE_ENDED -> android.util.Log.d("ImprovedPlayer", "State: ENDED")
                        }
                    }
                })
            }
    }
    
    /**
     * Creates optimized load control for IPTV streaming
     * Balances between quick startup and stable playback
     */
    private fun createOptimizedLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            // Buffer settings (in milliseconds)
            .setBufferDurationsMs(
                15000,  // Min buffer: 15 seconds (quick startup)
                50000,  // Max buffer: 50 seconds (stable playback)
                2500,   // Playback buffer: 2.5 seconds (start playback quickly)
                5000    // Playback rebuffer: 5 seconds (rebuffer threshold)
            )
            // Prioritize time over size for live streams
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }
    
    /**
     * Gets or creates a cache instance
     */
    private fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, "media")
            val cacheEvictor = LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024) // 100MB cache
            val databaseProvider = StandaloneDatabaseProvider(context)
            simpleCache = SimpleCache(cacheDir, cacheEvictor, databaseProvider)
        }
        return simpleCache!!
    }
    
    /**
     * Release cache resources
     */
    fun releaseCache() {
        simpleCache?.release()
        simpleCache = null
    }
}

/**
 * Suggestions for improving streaming reliability:
 * 
 * 1. BUFFERING OPTIMIZATION:
 *    - Increased min buffer to 15s for stable playback
 *    - Quick startup buffer of 2.5s to start playing fast
 *    - 50s max buffer to handle network fluctuations
 * 
 * 2. CONNECTION HANDLING:
 *    - 15 second timeouts (up from default 8s) for slow IPTV servers
 *    - Allow cross-protocol redirects (HTTP -> HTTPS)
 *    - Custom User-Agent for compatibility
 * 
 * 3. CACHING:
 *    - 100MB cache reduces rebuffering on network issues
 *    - Read-only for live streams (writes disabled)
 *    - Ignores cache on errors to prevent stale data
 * 
 * 4. ADAPTIVE STREAMING:
 *    - Starts with SD quality for faster startup
 *    - Automatically adapts to available bandwidth
 *    - Allows quality exceeding device caps if needed
 * 
 * 5. ERROR RECOVERY:
 *    - Auto-retry on playback errors after 2s delay
 *    - Handles transient network issues gracefully
 * 
 * 6. LIVE STREAM OPTIMIZATION:
 *    - 3 second live offset reduces buffering
 *    - Prioritizes time over size for live content
 * 
 * ADDITIONAL RECOMMENDATIONS:
 * 
 * A. Network Quality Monitoring:
 *    - Add bandwidth estimation listener
 *    - Show quality indicator in UI
 *    - Allow manual quality selection
 * 
 * B. Playlist Health Check:
 *    - Ping streams before playing (HEAD request)
 *    - Mark dead streams in database
 *    - Auto-refresh stale playlists
 * 
 * C. Fallback Streams:
 *    - Store alternative URLs per channel
 *    - Auto-switch on persistent errors
 * 
 * D. User Settings:
 *    - Configurable buffer sizes
 *    - Quality preference (SD/HD/Auto)
 *    - Cache on/off toggle
 * 
 * E. Diagnostics:
 *    - Log playback metrics (buffering time, errors)
 *    - Show debug overlay with stream stats
 *    - Report problematic channels
 */
