package com.suptv.shared.model

import kotlinx.serialization.Serializable

/**
 * Represents a TV channel with its streaming information
 */
@Serializable
data class Channel(
    val id: String,
    val name: String,
    val logo: String? = null,
    val group: String? = null,
    val url: String,
    val epgId: String? = null
)

/**
 * Represents a playlist containing multiple channels
 */
@Serializable
data class Playlist(
    val name: String,
    val channels: List<Channel>
)
