package org.richinet.musicandroid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject

data class TrackListScreen(val tagId: Long, val tagName: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<TrackViewModel>()
        val playlistSync = koinInject<PlaylistSync>()
        val audioPlayer = koinInject<AudioPlayer>()
        val tracksState by viewModel.tracks.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(tagId) {
            viewModel.loadTracksByTag(tagId)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(tagName) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (tracksState is UiState.Success) {
                            IconButton(onClick = {
                                playlistSync.sync(tagName, (tracksState as UiState.Success<List<Track>>).data)
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
                            items(state.data) { track ->
                                ListItem(
                                    headlineContent = { Text(track.trackName) },
                                    supportingContent = { Text("${track.getArtist()} - ${track.getAlbum()}") },
                                    modifier = Modifier.clickable {
                                        audioPlayer.playTrack(track)
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
