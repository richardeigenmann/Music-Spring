package org.richinet.musicandroid

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import coil.compose.AsyncImage
import org.koin.compose.koinInject

import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

data object PlayerScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val audioPlayer = koinInject<AudioPlayer>()
        val imageResolver = koinInject<ImageResolver>()
        val playbackState by audioPlayer.playbackState.collectAsState()
        val track = playbackState.track
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Now Playing") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { navigator.push(QueueScreen) }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Queue")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section: History navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { audioPlayer.goBackHistory() },
                        enabled = playbackState.historyIndex > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back History")
                    }

                    if (playbackState.playlistName.isNotBlank()) {
                        Text(
                            playbackState.playlistName,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }

                    IconButton(
                        onClick = { audioPlayer.goForwardHistory() },
                        enabled = playbackState.historyIndex < playbackState.historySize - 1
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward History")
                    }
                }

                // Album Art Section
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (track != null) {
                        AsyncImage(
                            model = imageResolver.getTrackImageSource(track),
                            contentDescription = "Album Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(100.dp))
                    }
                }

                // Middle section: Track Info
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = track?.trackName ?: "No Track",
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 2
                    )
                    Text(
                        text = track?.getArtist() ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Bottom section: Controls
                Column {
                    // Progress Bar
                    Slider(
                        value = playbackState.progress,
                        onValueChange = {
                            val newPos = (it * playbackState.duration).toLong()
                            audioPlayer.seekTo(newPos)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime( (playbackState.progress * playbackState.duration).toLong() ))
                        Text(formatTime(playbackState.duration))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = { audioPlayer.skipPrevious() },
                            enabled = playbackState.hasPrevious
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(48.dp))
                        }

                        FilledIconButton(
                            onClick = { audioPlayer.togglePlayPause() },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        IconButton(
                            onClick = { audioPlayer.skipNext() },
                            enabled = playbackState.hasNext
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(48.dp))
                        }
                    }
                }
            }
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return "${mins}:${secs.toString().padStart(2, '0')}"
    }
}
