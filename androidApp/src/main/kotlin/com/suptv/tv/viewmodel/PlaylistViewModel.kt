package com.suptv.tv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suptv.shared.db.*
import com.suptv.shared.service.PlaylistImportService
import com.suptv.shared.service.XStreamImportService
import com.suptv.shared.service.EpgImportService
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class Success(val providerId: Long, val message: String) : ImportState()
    data class Error(val message: String) : ImportState()
}

class PlaylistViewModel(private val database: SupTvDatabase) : ViewModel() {
    
    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    private val playlistRepository = PlaylistRepository(database)
    private val importService = PlaylistImportService(playlistRepository, httpClient)
    private val xstreamImportService = XStreamImportService(database)
    private val epgImportService = EpgImportService(database, httpClient)
    
    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()
    
    private val _providers = MutableStateFlow<List<Provider>>(emptyList())
    val providers: StateFlow<List<Provider>> = _providers.asStateFlow()
    
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()
    
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items.asStateFlow()
    
    private val _epgPrograms = MutableStateFlow<List<EpgProgram>>(emptyList())
    val epgPrograms: StateFlow<List<EpgProgram>> = _epgPrograms.asStateFlow()
    
    init {
        loadProviders()
    }
    
    fun importPlaylistFromUrl(
        url: String,
        name: String,
        username: String? = null,
        password: String? = null
    ) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            
            val result = importService.importFromUrl(url, name, username, password)
            
            _importState.value = result.fold(
                onSuccess = { providerId ->
                    loadProviders()
                    ImportState.Success(providerId, "Successfully imported $name")
                },
                onFailure = { error ->
                    ImportState.Error(error.message ?: "Failed to import playlist")
                }
            )
        }
    }
    
    fun importPlaylistFromString(content: String, name: String) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            
            val result = importService.importFromString(content, name)
            
            _importState.value = result.fold(
                onSuccess = { providerId ->
                    loadProviders()
                    ImportState.Success(providerId, "Successfully imported $name")
                },
                onFailure = { error ->
                    ImportState.Error(error.message ?: "Failed to import playlist")
                }
            )
        }
    }
    
    fun importXStreamProvider(
        baseUrl: String,
        username: String,
        password: String,
        name: String,
        includeLive: Boolean = true,
        includeVod: Boolean = false
    ) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            
            val result = xstreamImportService.importXStreamProvider(
                baseUrl = baseUrl,
                username = username,
                password = password,
                name = name,
                includeLive = includeLive,
                includeVod = includeVod
            )
            
            _importState.value = result.fold(
                onSuccess = { providerId ->
                    loadProviders()
                    ImportState.Success(providerId, "Successfully imported XStream provider: $name")
                },
                onFailure = { error ->
                    ImportState.Error(error.message ?: "Failed to import XStream provider")
                }
            )
        }
    }
    
    fun loadProviders() {
        viewModelScope.launch {
            val providers = playlistRepository.getAllProviders()
            _providers.value = providers
        }
    }
    
    fun loadCategories(providerId: Long) {
        viewModelScope.launch {
            val categories = playlistRepository.getCategoriesByProvider(providerId)
            _categories.value = categories
        }
    }
    
    fun loadItems(categoryId: Long) {
        viewModelScope.launch {
            val items = playlistRepository.getItemsByCategory(categoryId)
            _items.value = items
        }
    }
    
    fun loadEpgPrograms(epgId: String) {
        viewModelScope.launch {
            val currentTime = java.util.Calendar.getInstance().timeInMillis
            val startOfDay = currentTime
            val endOfDay = currentTime + (36 * 60 * 60 * 1000) // 36 hours from now
            
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            android.util.Log.i("PlaylistViewModel", "Loading EPG for channelId: $epgId")
            android.util.Log.i("PlaylistViewModel", "Current time: $currentTime (${sdf.format(java.util.Date(currentTime))})")
            android.util.Log.i("PlaylistViewModel", "Query range: $startOfDay to $endOfDay")
            
            val programs = database.epgProgramQueries
                .selectByChannelAndTimeRange(epgId, endOfDay, startOfDay)
                .executeAsList()
            
            android.util.Log.i("PlaylistViewModel", "Loaded ${programs.size} EPG programs")
            if (programs.isNotEmpty()) {
                android.util.Log.i("PlaylistViewModel", "First program: ${programs[0].title}, start: ${programs[0].startTime} (${sdf.format(java.util.Date(programs[0].startTime))})")
            }
            
            _epgPrograms.value = programs
        }
    }
    
    fun clearEpgPrograms() {
        _epgPrograms.value = emptyList()
    }
    
    fun loadItemsByProvider(providerId: Long) {
        viewModelScope.launch {
            val items = playlistRepository.getItemsByProvider(providerId)
            _items.value = items
        }
    }
    
    fun searchItems(query: String) {
        viewModelScope.launch {
            val items = playlistRepository.searchItems(query)
            _items.value = items
        }
    }
    
    fun deleteProvider(providerId: Long) {
        viewModelScope.launch {
            playlistRepository.deleteProvider(providerId)
            loadProviders()
        }
    }
    
    fun updateProviderEpgUrl(providerId: Long, epgUrl: String?) {
        viewModelScope.launch {
            playlistRepository.updateProviderEpgUrl(providerId, epgUrl)
        }
    }
    
    fun importEpgFromUrl(url: String, providerId: Long) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            
            val result = epgImportService.importEpgFromUrl(url, providerId)
            
            _importState.value = result.fold(
                onSuccess = { count ->
                    ImportState.Success(providerId, "Successfully imported $count EPG programs")
                },
                onFailure = { error ->
                    ImportState.Error(error.message ?: "Failed to import EPG")
                }
            )
        }
    }
    
    fun resetImportState() {
        _importState.value = ImportState.Idle
    }
    
    suspend fun getProvider(providerId: Long): Provider? {
        return playlistRepository.getProvider(providerId)
    }
    
    override fun onCleared() {
        super.onCleared()
        httpClient.close()
    }
}
