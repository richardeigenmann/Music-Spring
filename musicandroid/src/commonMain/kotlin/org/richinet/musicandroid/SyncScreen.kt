package org.richinet.musicandroid

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data object SyncScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val apiService = koinInject<ApiService>()
        val playlistSync = koinInject<PlaylistSync>()
        val scope = rememberCoroutineScope()

        var isSyncing by remember { mutableStateOf(false) }
        var statusMessage by remember { mutableStateOf("Ready to sync all playlists") }
        var currentPlaylistName by remember { mutableStateOf("") }
        var currentTrackStatus by remember { mutableStateOf("") }
        var progress by remember { mutableStateOf(0f) }

        fun runSyncAll() {
            scope.launch {
                isSyncing = true
                statusMessage = "Fetching tags..."
                try {
                    val allTags = apiService.getTags()
                    val playlists = allTags.filter { it.tagTypeName == "Playlist" }
                    val totalPlaylists = playlists.size
                    
                    if (totalPlaylists == 0) {
                        statusMessage = "No tags with type 'Playlist' found."
                    } else {
                        playlists.forEachIndexed { playlistIndex, tag ->
                            currentPlaylistName = tag.tagName
                            statusMessage = "Syncing playlist ${playlistIndex + 1} of $totalPlaylists"
                            progress = playlistIndex.toFloat() / totalPlaylists.toFloat()
                            
                            val tracks = apiService.getTracksByTag(tag.tagId)
                            val totalTracks = tracks.size
                            
                            tracks.forEachIndexed { trackIndex, track ->
                                currentTrackStatus = "Item ${trackIndex + 1} of $totalTracks"
                                playlistSync.syncTrack(track)
                                // Small delay to let UI update
                                kotlinx.coroutines.delay(5)
                            }
                            
                            playlistSync.registerPlaylist(tag.tagName, tracks)
                        }
                        statusMessage = "All $totalPlaylists playlists enqueued."
                    }
                    
                    currentPlaylistName = "Complete"
                    currentTrackStatus = ""
                    progress = 1f
                } catch (e: Exception) {
                    statusMessage = "Error: ${e.message}"
                } finally {
                    isSyncing = false
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Download All") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isSyncing) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    
                    if (currentPlaylistName.isNotEmpty()) {
                        Text(
                            text = currentPlaylistName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    if (currentTrackStatus.isNotEmpty()) {
                        Text(
                            text = currentTrackStatus,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Button(
                        onClick = { runSyncAll() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Download All")
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "This will fetch all tracks for all tags and enqueue them for download to your Music/Music-Spring folder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
