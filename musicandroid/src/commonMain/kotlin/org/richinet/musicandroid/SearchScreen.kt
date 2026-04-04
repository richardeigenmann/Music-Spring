package org.richinet.musicandroid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil.compose.AsyncImage
import org.koin.compose.koinInject

data object SearchScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<TrackViewModel>()
        val audioPlayer = koinInject<AudioPlayer>()
        val imageResolver = koinInject<ImageResolver>()
        val searchResults by viewModel.searchResults.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        var query by remember { mutableStateOf("") }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Search Tracks") })
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                TextField(
                    value = query,
                    onValueChange = {
                        query = it
                        viewModel.searchTracks(it)
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text("Search by title or artist...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    when (val state = searchResults) {
                        is UiState.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                        is UiState.Success -> {
                            LazyColumn {
                                items(state.data) { track ->
                                    ListItem(
                                        headlineContent = { Text(track.trackName) },
                                        supportingContent = { Text(track.getArtist()) },
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
                                            IconButton(onClick = { audioPlayer.playTrack(track) }) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                                            }
                                        }
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                        is UiState.Error -> {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }
}
