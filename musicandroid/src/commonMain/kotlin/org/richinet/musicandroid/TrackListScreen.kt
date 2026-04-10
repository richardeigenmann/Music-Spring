package org.richinet.musicandroid

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
        val apiService = koinInject<ApiService>()
        val tracksState by viewModel.tracks.collectAsState()
        val currentBaseUrl by apiService.baseUrlFlow.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val playbackState by audioPlayer.playbackState.collectAsState()

        var shuffledTracks by remember { mutableStateOf<List<Track>>(emptyList()) }

        val cachedFileNames = playbackState.cachedFileNames
        val cacheNonce = playbackState.cacheNonce

        val cachedCount = remember(shuffledTracks, cachedFileNames, cacheNonce) {
            shuffledTracks.count { track ->
                track.files.any { it.fileName in cachedFileNames }
            }
        }
        val progress = remember(shuffledTracks, cachedCount) {
            if (shuffledTracks.isNotEmpty()) cachedCount.toFloat() / shuffledTracks.size else 0f
        }

        LaunchedEffect(tagId, currentBaseUrl) {
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
                            val isDownloading = playbackState.isDownloading
                            val infiniteTransition = rememberInfiniteTransition(label = "downloading")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "rotation"
                            )

                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(end = 8.dp)) {
                                if (isDownloading) {
                                    Canvas(modifier = Modifier.size(32.dp)) {
                                        drawArc(
                                            color = Color(0xFF4CAF50),
                                            startAngle = rotation,
                                            sweepAngle = 90f,
                                            useCenter = false,
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                    }
                                } else {
                                    CircularProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                }
                                IconButton(onClick = {
                                    shuffledTracks
                                        .filter { it.trackId !in playbackState.cachedTrackIds }
                                        .forEach { audioPlayer.cacheTrack(it) }
                                }) {
                                    Icon(
                                        Icons.Default.DownloadForOffline,
                                        contentDescription = "Cache All",
                                        modifier = Modifier.size(20.dp),
                                        tint = if (isDownloading) Color(0xFF4CAF50) else LocalContentColor.current
                                    )
                                }
                            }

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
                                val isCurrent = track.trackId == playbackState.track?.trackId
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            track.trackName,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified
                                        )
                                    },
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val isCached = track.files.any { it.fileName in cachedFileNames }
                                            IconButton(onClick = { audioPlayer.cacheTrack(track) }) {
                                                Icon(
                                                    if (isCached) Icons.Default.CheckCircle else Icons.Default.Download,
                                                    contentDescription = if (isCached) "Cached" else "Cache",
                                                    tint = if (isCached) Color(0xFF4CAF50) else LocalContentColor.current
                                                )
                                            }
                                            IconButton(onClick = {
                                                val playlist = shuffledTracks.drop(index)
                                                audioPlayer.playPlaylist(playlist, tagName)
                                            }) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = "Play",
                                                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                                )
                                            }
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
