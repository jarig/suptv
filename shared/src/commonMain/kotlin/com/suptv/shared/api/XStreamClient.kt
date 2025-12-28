package com.suptv.shared.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

/**
 * Client for XStream (Xtream Codes) API
 * 
 * XStream API provides live TV, VOD, and series content
 * Standard endpoints:
 * - /player_api.php?username=X&password=Y - Get server info and user info
 * - /player_api.php?username=X&password=Y&action=get_live_categories
 * - /player_api.php?username=X&password=Y&action=get_live_streams
 * - /player_api.php?username=X&password=Y&action=get_vod_categories
 * - /player_api.php?username=X&password=Y&action=get_vod_streams
 * - /player_api.php?username=X&password=Y&action=get_series_categories
 * - /player_api.php?username=X&password=Y&action=get_series
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

    private val lPassword = password
    private val normalizedBaseUrl = baseUrl.removeSuffix("/")
    
    @Serializable
    data class ServerInfo(
        val url: String? = null,
        val port: String? = null,
        val https_port: String? = null,
        val server_protocol: String? = null,
        val rtmp_port: String? = null,
        val timestamp_now: Long? = null,
        val time_now: String? = null
    )
    
    @Serializable
    data class UserInfo(
        val username: String? = null,
        val password: String? = null,
        val message: String? = null,
        val auth: Int? = null,
        val status: String? = null,
        val exp_date: String? = null,
        val is_trial: String? = null,
        val active_cons: String? = null,
        val created_at: String? = null,
        val max_connections: String? = null,
        val allowed_output_formats: List<String>? = null
    )
    
    @Serializable
    data class AuthResponse(
        val user_info: UserInfo? = null,
        val server_info: ServerInfo? = null
    )
    
    @Serializable
    data class Category(
        val category_id: String,
        val category_name: String,
        val parent_id: Int? = null
    )
    
    @Serializable
    data class LiveStream(
        val num: Int? = null,
        val name: String,
        val stream_type: String? = null,
        val stream_id: Int,
        val stream_icon: String? = null,
        val epg_channel_id: String? = null,
        val added: String? = null,
        val category_id: String? = null,
        val custom_sid: String? = null,
        val tv_archive: Int? = null,
        val direct_source: String? = null,
        val tv_archive_duration: Int? = null
    )
    
    @Serializable
    data class VodStream(
        val num: Int? = null,
        val name: String,
        val stream_type: String? = null,
        val stream_id: Int,
        val stream_icon: String? = null,
        val rating: String? = null,
        val rating_5based: Double? = null,
        val added: String? = null,
        val category_id: String? = null,
        val container_extension: String? = null,
        val custom_sid: String? = null,
        val direct_source: String? = null
    )
    
    /**
     * Authenticate and get server/user information
     */
    suspend fun authenticate(): Result<AuthResponse> {
        val url = buildUrl {
            // No action parameter for authentication
        }
        
        return try {
            val response: AuthResponse = client.get(url).body()
            
            if (response.user_info?.auth == 1) {
                Result.success(response)
            } else {
                Result.failure(XStreamException("Authentication failed: ${response.user_info?.message}"))
            }
        } catch (e: Exception) {
            Result.failure(XStreamException("Authentication error: ${e.message}", e))
        }
    }
    
    /**
     * Get live stream categories
     */
    suspend fun getLiveCategories(): Result<List<Category>> {
        val url = buildUrl {
            parameters.append("action", "get_live_categories")
        }
        
        return try {
            val categories: List<Category> = client.get(url).body()
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(XStreamException("Failed to fetch live categories: ${e.message}", e))
        }
    }
    
    /**
     * Get live streams
     * @param categoryId Optional category ID to filter streams
     */
    suspend fun getLiveStreams(categoryId: String? = null): Result<List<LiveStream>> {
        val url = buildUrl {
            parameters.append("action", "get_live_streams")
            categoryId?.let { parameters.append("category_id", it) }
        }
        
        return try {
            val streams: List<LiveStream> = client.get(url).body()
            Result.success(streams)
        } catch (e: Exception) {
            Result.failure(XStreamException("Failed to fetch live streams: ${e.message}", e))
        }
    }
    
    /**
     * Get VOD (Video on Demand) categories
     */
    suspend fun getVodCategories(): Result<List<Category>> {
        val url = buildUrl {
            parameters.append("action", "get_vod_categories")
        }
        
        return try {
            val categories: List<Category> = client.get(url).body()
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(XStreamException("Failed to fetch VOD categories: ${e.message}", e))
        }
    }
    
    /**
     * Get VOD streams
     * @param categoryId Optional category ID to filter streams
     */
    suspend fun getVodStreams(categoryId: String? = null): Result<List<VodStream>> {
        val url = buildUrl {
            parameters.append("action", "get_vod_streams")
            categoryId?.let { parameters.append("category_id", it) }
        }
        
        return try {
            val streams: List<VodStream> = client.get(url).body()
            Result.success(streams)
        } catch (e: Exception) {
            Result.failure(XStreamException("Failed to fetch VOD streams: ${e.message}", e))
        }
    }
    
    /**
     * Build live stream URL
     */
    fun buildLiveStreamUrl(streamId: Int, extension: String = "m3u8"): String {
        return "$normalizedBaseUrl/live/$username/$password/$streamId.$extension"
    }
    
    /**
     * Build VOD stream URL
     */
    fun buildVodStreamUrl(streamId: Int, extension: String = "mp4"): String {
        return "$normalizedBaseUrl/movie/$username/$password/$streamId.$extension"
    }
    
    /**
     * Build catchup/archive stream URL
     */
    fun buildCatchupUrl(streamId: Int, timestamp: Long, duration: Int): String {
        return "$normalizedBaseUrl/streaming/timeshift.php?username=$username&password=$password&stream=$streamId&start=$timestamp&duration=$duration"
    }
    
    private fun buildUrl(block: URLBuilder.() -> Unit = {}): String {
        return URLBuilder().apply {
            takeFrom(normalizedBaseUrl)
            path("player_api.php")
            parameters.append("username", username)
            parameters.append("password", lPassword)
            block()
        }.buildString()
    }
    
    /**
     * Close the HTTP client and release resources
     */
    fun close() {
        client.close()
    }
}

class XStreamException(message: String, cause: Throwable? = null) : Exception(message, cause)
