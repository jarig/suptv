package com.suptv.shared.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FavoriteRepository(private val database: SupTvDatabase) {
    
    private val favoriteQueries = database.favoriteQueries
    
    suspend fun addFavorite(itemId: Long): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            favoriteQueries.insert(
                itemId = itemId,
                addedAt = System.currentTimeMillis()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun removeFavorite(itemId: Long): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            favoriteQueries.delete(itemId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun isFavorite(itemId: Long): Boolean = withContext(Dispatchers.Default) {
        favoriteQueries.isFavorite(itemId).executeAsOne()
    }
    
    suspend fun getAllFavorites() = withContext(Dispatchers.Default) {
        favoriteQueries.selectAll().executeAsList()
    }
    
    suspend fun clearAllFavorites(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            favoriteQueries.deleteAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
