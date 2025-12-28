package com.suptv.shared.service

import com.suptv.shared.api.XStreamClient
import com.suptv.shared.db.SupTvDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class XStreamImportService(
    private val database: SupTvDatabase
) {
    
    suspend fun importXStreamProvider(
        baseUrl: String,
        username: String,
        password: String,
        name: String,
        includeLive: Boolean = true,
        includeVod: Boolean = false
    ): Result<Long> = withContext(Dispatchers.Default) {
        val client = XStreamClient(baseUrl, username, password)
        
        try {
            // Authenticate first
            val authResult = client.authenticate()
            if (authResult.isFailure) {
                return@withContext Result.failure(
                    Exception("Authentication failed: ${authResult.exceptionOrNull()?.message}")
                )
            }
            
            val timestamp = System.currentTimeMillis()
            
            // Insert provider
            database.providerQueries.insert(
                name = name,
                url = baseUrl,
                username = username,
                password = password,
                type = "XStream",
                epgUrl = null,
                createdAt = timestamp,
                updatedAt = timestamp
            )
            
            val providerId = database.providerQueries.lastInsertRowId().executeAsOne()
            
            // Import live streams
            if (includeLive) {
                importLiveStreams(client, providerId)
            }
            
            // Import VOD streams
            if (includeVod) {
                importVodStreams(client, providerId)
            }
            
            Result.success(providerId)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to import XStream provider: ${e.message}", e))
        } finally {
            client.close()
        }
    }
    
    private suspend fun importLiveStreams(client: XStreamClient, providerId: Long) {
        // Get categories
        val categoriesResult = client.getLiveCategories()
        if (categoriesResult.isFailure) {
            throw Exception("Failed to fetch live categories")
        }
        
        val categories = categoriesResult.getOrThrow()
        val categoryMap = mutableMapOf<String, Long>()
        
        // Insert categories
        categories.forEach { category ->
            database.categoryQueries.insert(
                providerId = providerId,
                name = category.category_name,
                sortOrder = 0
            )
            
            val categoryId = database.categoryQueries.lastInsertRowId().executeAsOne()
            categoryMap[category.category_id] = categoryId
        }
        
        // Get all live streams
        val streamsResult = client.getLiveStreams()
        if (streamsResult.isFailure) {
            throw Exception("Failed to fetch live streams")
        }
        
        val streams = streamsResult.getOrThrow()
        
        // Insert streams as items
        streams.forEachIndexed { index, stream ->
            val categoryId = stream.category_id?.let { categoryMap[it] }
            val streamUrl = client.buildLiveStreamUrl(stream.stream_id)
            
            database.itemQueries.insert(
                providerId = providerId,
                categoryId = categoryId,
                externalId = stream.stream_id.toString(),
                name = stream.name,
                logo = stream.stream_icon,
                url = streamUrl,
                epgId = stream.epg_channel_id,
                type = "LIVE",
                sortOrder = index.toLong()
            )
        }
    }
    
    private suspend fun importVodStreams(client: XStreamClient, providerId: Long) {
        // Get VOD categories
        val categoriesResult = client.getVodCategories()
        if (categoriesResult.isFailure) {
            throw Exception("Failed to fetch VOD categories")
        }
        
        val categories = categoriesResult.getOrThrow()
        val categoryMap = mutableMapOf<String, Long>()
        
        // Insert categories
        categories.forEach { category ->
            database.categoryQueries.insert(
                providerId = providerId,
                name = "${category.category_name} (VOD)",
                sortOrder = 1000 // Sort after live categories
            )
            
            val categoryId = database.categoryQueries.lastInsertRowId().executeAsOne()
            categoryMap[category.category_id] = categoryId
        }
        
        // Get all VOD streams
        val streamsResult = client.getVodStreams()
        if (streamsResult.isFailure) {
            throw Exception("Failed to fetch VOD streams")
        }
        
        val streams = streamsResult.getOrThrow()
        
        // Insert streams as items
        streams.forEachIndexed { index, stream ->
            val categoryId = stream.category_id?.let { categoryMap[it] }
            val extension = stream.container_extension ?: "mp4"
            val streamUrl = client.buildVodStreamUrl(stream.stream_id, extension)
            
            database.itemQueries.insert(
                providerId = providerId,
                categoryId = categoryId,
                externalId = stream.stream_id.toString(),
                name = stream.name,
                logo = stream.stream_icon,
                url = streamUrl,
                epgId = null,
                type = "VOD",
                sortOrder = index.toLong()
            )
        }
    }
}
