package com.suptv.shared.api

import com.suptv.shared.model.Channel
import com.suptv.shared.model.Playlist
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

/**
 * Client for XStream (Xtream Codes) API
 * 
 * XStream API provides live TV, VOD, and series content
 * Standard endpoints:
 * - /player_api.php?username=X&password=Y&action=get_live_categories
 * - /player_api.php?username=X&password=Y&action=get_live_streams
 */
class XStreamClient(
    private val baseUrl: String,
    private val username: String,
    private val password: String
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    @Serializable
    data class XStreamChannel(
        val num: Int? = null,
        val name: String,
        val stream_type: String? = null,
        val stream_id: Int,
        val stream_icon: String? = null,
        val epg_channel_id: String? = null,
        val category_id: String? = null
    )
    
    suspend fun getLiveStreams(): Playlist {
        val url = "$baseUrl/player_api.php?username=$username&password=$password&action=get_live_streams"
        
        try {
            val streams: List<XStreamChannel> = client.get(url).body()
            
            val channels = streams.map { stream ->
                val streamUrl = "$baseUrl/live/$username/$password/${stream.stream_id}.m3u8"
                Channel(
                    id = stream.stream_id.toString(),
                    name = stream.name,
                    logo = stream.stream_icon,
                    group = stream.category_id,
                    url = streamUrl,
                    epgId = stream.epg_channel_id
                )
            }
            
            return Playlist(name = "XStream Playlist", channels = channels)
        } catch (e: Exception) {
            throw XStreamException("Failed to fetch live streams: ${e.message}", e)
        }
    }
    
    fun close() {
        client.close()
    }
}

class XStreamException(message: String, cause: Throwable? = null) : Exception(message, cause)
