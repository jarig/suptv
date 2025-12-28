package com.suptv.tv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.suptv.shared.db.HistoryRepository
import com.suptv.shared.db.Item
import com.suptv.shared.db.SupTvDatabase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel(private val database: SupTvDatabase) : ViewModel() {
    
    private val historyRepository = HistoryRepository(database)
    
    private val _currentItem = MutableStateFlow<Item?>(null)
    val currentItem: StateFlow<Item?> = _currentItem.asStateFlow()
    
    private var historyEntryId: Long? = null
    private var positionUpdateJob: Job? = null
    
    fun startPlayback(item: Item) {
        _currentItem.value = item
        
        // Create history entry
        viewModelScope.launch {
            val result = historyRepository.addHistoryEntry(
                itemId = item.id,
                duration = 0,
                position = 0
            )
            result.onSuccess { id ->
                historyEntryId = id
            }
        }
    }
    
    fun startPositionTracking(player: Player) {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                delay(10000) // Update every 10 seconds
                
                historyEntryId?.let { id ->
                    val position = player.currentPosition
                    val duration = player.duration
                    
                    if (position > 0 && duration > 0) {
                        historyRepository.updatePosition(
                            id = id,
                            position = position,
                            duration = duration
                        )
                    }
                }
            }
        }
    }
    
    fun stopPlayback(player: Player) {
        positionUpdateJob?.cancel()
        
        // Final position update
        historyEntryId?.let { id ->
            viewModelScope.launch {
                val position = player.currentPosition
                val duration = player.duration
                
                if (position > 0 && duration > 0) {
                    historyRepository.updatePosition(
                        id = id,
                        position = position,
                        duration = duration
                    )
                }
            }
        }
        
        historyEntryId = null
        _currentItem.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        positionUpdateJob?.cancel()
    }
}
