package com.suptv.shared.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoryRepository(private val database: SupTvDatabase) {
    
    private val historyQueries = database.historyQueries
    
    suspend fun addHistoryEntry(
        itemId: Long,
        duration: Long = 0,
        position: Long = 0
    ): Result<Long> = withContext(Dispatchers.Default) {
        try {
            historyQueries.insert(
                itemId = itemId,
                watchedAt = System.currentTimeMillis(),
                duration = duration,
                position = position
            )
            val id = historyQueries.lastInsertRowId().executeAsOne()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updatePosition(
        id: Long,
        position: Long,
        duration: Long
    ): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            historyQueries.updatePosition(position, duration, id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAllHistory() = withContext(Dispatchers.Default) {
        historyQueries.selectAll().executeAsList()
    }
    
    suspend fun getRecentHistory(limit: Long = 20) = 
        withContext(Dispatchers.Default) {
            historyQueries.selectRecent(limit).executeAsList()
        }
    
    suspend fun getHistoryForItem(itemId: Long): History? = 
        withContext(Dispatchers.Default) {
            historyQueries.selectByItemId(itemId).executeAsOneOrNull()
        }
    
    suspend fun deleteHistoryEntry(id: Long): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            historyQueries.delete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteHistoryForItem(itemId: Long): Result<Unit> = 
        withContext(Dispatchers.Default) {
            try {
                historyQueries.deleteByItemId(itemId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    suspend fun deleteOldHistory(beforeTimestamp: Long): Result<Unit> = 
        withContext(Dispatchers.Default) {
            try {
                historyQueries.deleteOldEntries(beforeTimestamp)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    suspend fun clearAllHistory(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            historyQueries.deleteAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
