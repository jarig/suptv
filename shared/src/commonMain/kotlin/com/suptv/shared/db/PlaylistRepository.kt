package com.suptv.shared.db

import com.suptv.shared.model.Channel
import com.suptv.shared.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaylistRepository(private val database: SupTvDatabase) {
    
    private val providerQueries = database.providerQueries
    private val categoryQueries = database.categoryQueries
    private val itemQueries = database.itemQueries
    
    suspend fun importPlaylist(
        playlist: Playlist,
        providerName: String,
        providerUrl: String,
        username: String? = null,
        password: String? = null
    ): Result<Long> = withContext(Dispatchers.Default) {
        try {
            val timestamp = System.currentTimeMillis()
            
            // Insert provider
            providerQueries.insert(
                name = providerName,
                url = providerUrl,
                username = username,
                password = password,
                type = "M3U",
                epgUrl = null,
                createdAt = timestamp,
                updatedAt = timestamp
            )
            
            val providerId = providerQueries.lastInsertRowId().executeAsOne()
            
            // Group channels by category
            val channelsByCategory = playlist.channels.groupBy { it.group ?: "Uncategorized" }
            
            // Insert categories and channels
            channelsByCategory.forEach { (categoryName, channels) ->
                categoryQueries.insert(
                    providerId = providerId,
                    name = categoryName,
                    sortOrder = 0
                )
                
                val categoryId = categoryQueries.lastInsertRowId().executeAsOne()
                
                // Insert channels as items
                channels.forEachIndexed { index, channel ->
                    itemQueries.insert(
                        providerId = providerId,
                        categoryId = categoryId,
                        externalId = channel.id,
                        name = channel.name,
                        logo = channel.logo,
                        url = channel.url,
                        epgId = channel.epgId,
                        type = "LIVE",
                        sortOrder = index.toLong()
                    )
                }
            }
            
            Result.success(providerId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAllProviders(): List<Provider> = withContext(Dispatchers.Default) {
        providerQueries.selectAll().executeAsList()
    }
    
    suspend fun getProvider(id: Long): Provider? = withContext(Dispatchers.Default) {
        providerQueries.selectById(id).executeAsOneOrNull()
    }
    
    suspend fun deleteProvider(id: Long) = withContext(Dispatchers.Default) {
        // Delete items first (if not using ON DELETE CASCADE)
        itemQueries.deleteByProvider(id)
        categoryQueries.deleteByProvider(id)
        providerQueries.delete(id)
    }
    
    suspend fun updateProviderEpgUrl(id: Long, epgUrl: String?) = withContext(Dispatchers.Default) {
        val provider = providerQueries.selectById(id).executeAsOneOrNull() ?: return@withContext
        providerQueries.update(
            name = provider.name,
            url = provider.url,
            username = provider.username,
            password = provider.password,
            type = provider.type,
            epgUrl = epgUrl,
            updatedAt = System.currentTimeMillis(),
            id = id
        )
    }
    
    suspend fun getCategoriesByProvider(providerId: Long): List<Category> = 
        withContext(Dispatchers.Default) {
            categoryQueries.selectByProvider(providerId).executeAsList()
        }
    
    suspend fun getItemsByCategory(categoryId: Long): List<Item> = 
        withContext(Dispatchers.Default) {
            itemQueries.selectByCategory(categoryId).executeAsList()
        }
    
    suspend fun getItemsByProvider(providerId: Long): List<Item> = 
        withContext(Dispatchers.Default) {
            itemQueries.selectByProvider(providerId).executeAsList()
        }
    
    suspend fun searchItems(query: String, limit: Long = 50): List<Item> = 
        withContext(Dispatchers.Default) {
            itemQueries.search(query, limit).executeAsList()
        }
}
