package org.richinet.musicandroid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil.compose.AsyncImage
import org.koin.compose.koinInject

data class TrackListScreen(val tagId: Long, val tagName: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<TrackViewModel>()
        val playlistSync = koinInject<PlaylistSync>()
        val audioPlayer = koinInject<AudioPlayer>()
        val imageResolver = koinInject<ImageResolver>()
        val tracksState by viewModel.tracks.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        var shuffledTracks by remember { mutableStateOf<List<Track>>(emptyList()) }

        LaunchedEffect(tagId) {
            viewModel.loadTracksByTag(tagId)
        }

        LaunchedEffect(tracksState) {
            if (tracksState is UiState.Success) {
                shuffledTracks = (tracksState as UiState.Success<List<Track>>).data.shuffled()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(tagName) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (shuffledTracks.isNotEmpty()) {
                            IconButton(onClick = {
                                audioPlayer.playPlaylist(shuffledTracks, tagName)
                            }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play All (Shuffled)")
                            }
                            IconButton(onClick = {
                                if (tracksState is UiState.Success) {
                                    val originalTracks = (tracksState as UiState.Success<List<Track>>).data
                                    playlistSync.sync(tagName, originalTracks)
                                }
                            }) {
                                Icon(Icons.Default.Sync, contentDescription = "Sync to Device")
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                when (val state = tracksState) {
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Success -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(shuffledTracks) { index, track ->
                                ListItem(
                                    headlineContent = { Text(track.trackName) },
                                    supportingContent = { Text("${track.getArtist()} - ${track.getAlbum()}") },
                                    leadingContent = {
                                        AsyncImage(
                                            model = imageResolver.getTrackImageSource(track),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(56.dp)
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        navigator.push(TrackEditScreen(track.trackId))
                                    },
                                    trailingContent = {
                                        IconButton(onClick = {
                                            val playlist = shuffledTracks.drop(index)
                                            audioPlayer.playPlaylist(playlist, tagName)
                                        }) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                    is UiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Error: ${state.message}",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
