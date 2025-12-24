package com.suptv.tv.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.suptv.shared.model.Channel

/**
 * Manages video playback using ExoPlayer/Media3
 * Supports HLS and direct stream URLs
 */
class VideoPlayer(context: Context) {
    
    private val player: ExoPlayer = ExoPlayer.Builder(context).build()
    
    /**
     * Play a channel's stream
     */
    fun playChannel(channel: Channel) {
        val mediaItem = MediaItem.Builder()
            .setUri(channel.url)
            .setMediaId(channel.id)
            .build()
        
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }
    
    /**
     * Play from URL directly
     */
    fun playUrl(url: String) {
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }
    
    /**
     * Get the underlying ExoPlayer instance for advanced control
     */
    fun getPlayer(): Player = player
    
    /**
     * Pause playback
     */
    fun pause() {
        player.pause()
    }
    
    /**
     * Resume playback
     */
    fun play() {
        player.play()
    }
    
    /**
     * Stop playback and reset
     */
    fun stop() {
        player.stop()
        player.clearMediaItems()
    }
    
    /**
     * Release player resources
     * Call this when done with the player
     */
    fun release() {
        player.release()
    }
    
    /**
     * Check if currently playing
     */
    fun isPlaying(): Boolean = player.isPlaying
}
