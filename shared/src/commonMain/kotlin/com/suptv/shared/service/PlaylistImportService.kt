package com.suptv.shared.service

import com.suptv.shared.db.PlaylistRepository
import com.suptv.shared.parser.M3UParser
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaylistImportService(
    private val repository: PlaylistRepository,
    private val httpClient: HttpClient
) {
    private val m3uParser = M3UParser()
    
    suspend fun importFromUrl(
        url: String,
        name: String,
        username: String? = null,
        password: String? = null
    ): Result<Long> = withContext(Dispatchers.Default) {
        try {
            // Download M3U content
            val content = httpClient.get(url).bodyAsText()
            
            // Parse M3U
            val playlist = m3uParser.parse(content, name)
            
            if (playlist.channels.isEmpty()) {
                return@withContext Result.failure(Exception("No channels found in playlist"))
            }
            
            // Import to database
            repository.importPlaylist(
                playlist = playlist,
                providerName = name,
                providerUrl = url,
                username = username,
                password = password
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun importFromString(
        content: String,
        name: String,
        url: String = ""
    ): Result<Long> = withContext(Dispatchers.Default) {
        try {
            // Parse M3U content
            val playlist = m3uParser.parse(content, name)
            
            if (playlist.channels.isEmpty()) {
                return@withContext Result.failure(Exception("No channels found in playlist"))
            }
            
            // Import to database
            repository.importPlaylist(
                playlist = playlist,
                providerName = name,
                providerUrl = url
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
