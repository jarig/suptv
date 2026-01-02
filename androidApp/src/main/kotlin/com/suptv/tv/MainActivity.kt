package com.suptv.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.suptv.shared.db.Item
import com.suptv.shared.db.Provider
import com.suptv.shared.db.createDatabase
import com.suptv.shared.db.DatabaseDriverFactory
import com.suptv.tv.ui.CategoriesScreen
import com.suptv.tv.ui.PlayerScreen
import com.suptv.tv.ui.PlaylistImportScreen
import com.suptv.tv.ui.ProvidersScreen
import com.suptv.tv.ui.XStreamImportScreen
import com.suptv.tv.ui.EpgImportScreen
import com.suptv.tv.viewmodel.PlaylistViewModel
import com.suptv.tv.viewmodel.PlayerViewModel
import com.suptv.tv.util.PreferencesManager
import com.suptv.tv.player.ImprovedPlayerConfig

class MainActivity : ComponentActivity() {
    
    private val database by lazy {
        createDatabase(DatabaseDriverFactory(applicationContext))
    }
    
    private val exoPlayer by lazy {
        ImprovedPlayerConfig.createOptimizedPlayer(applicationContext)
    }
    
    private val preferencesManager by lazy {
        PreferencesManager(applicationContext)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SupTVApp(database, exoPlayer, preferencesManager)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
        ImprovedPlayerConfig.releaseCache()
    }
    
    override fun onPause() {
        super.onPause()
        exoPlayer.pause()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SupTVApp(database: com.suptv.shared.db.SupTvDatabase, exoPlayer: ExoPlayer, preferencesManager: PreferencesManager) {
    val playlistViewModel: PlaylistViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return PlaylistViewModel(database) as T
            }
        }
    )
    
    val playerViewModel: PlayerViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return PlayerViewModel(database) as T
            }
        }
    )
    
    var currentScreen by remember { mutableStateOf(Screen.Providers) }
    var selectedProvider by remember { mutableStateOf<Provider?>(null) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var returningFromPlayer by remember { mutableStateOf(false) }
    
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            when (currentScreen) {
                Screen.Providers -> {
                    ProvidersScreen(
                        viewModel = playlistViewModel,
                        onProviderSelected = { provider ->
                            selectedProvider = provider
                            currentScreen = Screen.Categories
                        },
                        onAddM3UProvider = {
                            currentScreen = Screen.ImportPlaylist
                        },
                        onAddXStreamProvider = {
                            currentScreen = Screen.ImportXStream
                        }
                    )
                }
                
                Screen.ImportPlaylist -> {
                    PlaylistImportScreen(
                        viewModel = playlistViewModel,
                        onImportSuccess = {
                            currentScreen = Screen.Providers
                            playlistViewModel.resetImportState()
                        },
                        onBack = {
                            currentScreen = Screen.Providers
                            playlistViewModel.resetImportState()
                        }
                    )
                }
                
                Screen.ImportXStream -> {
                    XStreamImportScreen(
                        viewModel = playlistViewModel,
                        onImportSuccess = {
                            currentScreen = Screen.Providers
                            playlistViewModel.resetImportState()
                        },
                        onBack = {
                            currentScreen = Screen.Providers
                            playlistViewModel.resetImportState()
                        }
                    )
                }
                
                Screen.Categories -> {
                    selectedProvider?.let { provider ->
                        CategoriesScreen(
                            viewModel = playlistViewModel,
                            provider = provider,
                            preferencesManager = preferencesManager,
                            restoreFocusToItems = returningFromPlayer,
                            onItemSelected = { item ->
                                selectedItem = item
                                returningFromPlayer = false
                                currentScreen = Screen.Player
                                
                                // Properly stop any existing playback
                                exoPlayer.stop()
                                exoPlayer.clearMediaItems()
                                
                                // Start new playback
                                val mediaItem = MediaItem.Builder()
                                    .setUri(item.url)
                                    .setMediaId(item.id.toString())
                                    .build()
                                
                                exoPlayer.setMediaItem(mediaItem)
                                exoPlayer.prepare()
                                exoPlayer.play()
                                
                                // Track in history
                                playerViewModel.startPlayback(item)
                                playerViewModel.startPositionTracking(exoPlayer)
                            },
                            onBack = {
                                returningFromPlayer = false
                                currentScreen = Screen.Providers
                            },
                            onEpgImport = {
                                returningFromPlayer = false
                                currentScreen = Screen.ImportEpg
                            }
                        )
                    }
                }
                
                Screen.ImportEpg -> {
                    selectedProvider?.let { provider ->
                        // Refresh provider data to get latest epgUrl
                        var refreshedProvider by remember { mutableStateOf<Provider?>(provider) }
                        
                        LaunchedEffect(provider.id) {
                            refreshedProvider = playlistViewModel.getProvider(provider.id)
                        }
                        
                        refreshedProvider?.let { freshProvider ->
                            EpgImportScreen(
                                viewModel = playlistViewModel,
                                provider = freshProvider,
                                onBack = {
                                    currentScreen = Screen.Categories
                                    playlistViewModel.resetImportState()
                                }
                            )
                        }
                    }
                }
                
                Screen.Player -> {
                    selectedItem?.let { item ->
                        PlayerScreen(
                            item = item,
                            player = exoPlayer,
                            onBack = {
                                playerViewModel.stopPlayback(exoPlayer)
                                exoPlayer.stop()
                                exoPlayer.clearMediaItems()
                                returningFromPlayer = true
                                currentScreen = Screen.Categories
                            }
                        )
                    }
                }
            }
        }
    }
}

enum class Screen {
    Providers,
    ImportPlaylist,
    ImportXStream,
    ImportEpg,
    Categories,
    Player
}
