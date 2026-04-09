package org.richinet.musicandroid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil.compose.AsyncImage
import org.koin.compose.koinInject

data object QueueScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val audioPlayer = koinInject<AudioPlayer>()
        val imageResolver = koinInject<ImageResolver>()
        val playbackState by audioPlayer.playbackState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val queue = audioPlayer.getQueue()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Queue", style = MaterialTheme.typography.titleMedium)
                            if (playbackState.playlistName.isNotBlank()) {
                                Text(playbackState.playlistName, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        val progress = playbackState.cacheProgress
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(end = 8.dp)) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            IconButton(onClick = { audioPlayer.cacheQueue() }) {
                                Icon(
                                    Icons.Default.DownloadForOffline,
                                    contentDescription = "Cache All",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { audioPlayer.goBackHistory() },
                            enabled = playbackState.historyIndex > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back History")
                        }
                        IconButton(
                            onClick = { audioPlayer.goForwardHistory() },
                            enabled = playbackState.historyIndex < playbackState.historySize - 1
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward History")
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                itemsIndexed(queue) { index, track ->
                    val isCurrent = track.trackId == playbackState.track?.trackId
                    ListItem(
                        headlineContent = {
                            Text(
                                track.trackName,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified
                            )
                        },
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isCached = audioPlayer.isCached(track)
                                IconButton(onClick = { audioPlayer.cacheTrack(track) }) {
                                    Icon(
                                        if (isCached) Icons.Default.CheckCircle else Icons.Default.Download,
                                        contentDescription = if (isCached) "Cached" else "Cache",
                                        tint = if (isCached) Color(0xFF4CAF50) else LocalContentColor.current
                                    )
                                }
                                IconButton(onClick = { audioPlayer.jumpToQueueItem(index) }) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Play immediately",
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
    }
}
