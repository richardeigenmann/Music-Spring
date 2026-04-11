package org.richinet.musicandroid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var forceShowPlayer by remember { mutableStateOf(true) }

    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        Navigator(HomeScreen) { navigator ->
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        Spacer(Modifier.height(12.dp))
                        NavigationDrawerItem(
                            label = { Text("Tags") },
                            selected = false,
                            onClick = {
                                navigator.popUntilRoot()
                                scope.launch { drawerState.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text("Search") },
                            selected = false,
                            onClick = {
                                navigator.push(SearchScreen)
                                scope.launch { drawerState.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text("Mixing Board") },
                            selected = false,
                            onClick = {
                                navigator.push(MixerScreen)
                                scope.launch { drawerState.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text("Open Player") },
                            selected = false,
                            onClick = {
                                forceShowPlayer = true
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.MusicNote, null) }
                        )
                        NavigationDrawerItem(
                            label = { Text("Settings") },
                            selected = false,
                            onClick = {
                                navigator.push(SettingsScreen)
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.Settings, null) }
                        )
                        NavigationDrawerItem(
                            label = { Text("Check Pictures") },
                            selected = false,
                            onClick = {
                                navigator.push(PictureCheckScreen)
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.Image, null) }
                        )
                        NavigationDrawerItem(
                            label = { Text("Download All") },
                            selected = false,
                            onClick = {
                                navigator.push(SyncScreen)
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.Download, null) }
                        )
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        if (navigator.size == 1) { // Only show drawer icon on root
                            TopAppBar(
                                title = { Text("Music-Spring") },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                }
                            )
                        }
                    },
                    bottomBar = {
                        NowPlayingBarWrapper(navigator, forceShowPlayer)
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        SlideTransition(navigator)
                    }
                }
            }
        }
    }
}

@Composable
fun NowPlayingBarWrapper(navigator: Navigator, forceShow: Boolean) {
    val audioPlayer = koinInject<AudioPlayer>()
    val playbackState by audioPlayer.playbackState.collectAsState()

    if (playbackState.track != null || forceShow) {
        NowPlayingBar(playbackState, audioPlayer, navigator)
    }
}

@Composable
fun NowPlayingBar(state: PlaybackState, player: AudioPlayer, navigator: Navigator) {
    val imageResolver = koinInject<ImageResolver>()
    
    Surface(
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    if (delta < -20) { // Swipe up
                        navigator.push(QueueScreen)
                    }
                }
            )
            .clickable { navigator.push(PlayerScreen) }
    ) {
        Column {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.track != null) {
                    AsyncImage(
                        model = imageResolver.getTrackImageSource(state.track),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(4.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        null,
                        modifier = Modifier.size(48.dp).padding(4.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(
                        text = state.track?.trackName ?: "No track playing",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                    Text(
                        text = state.track?.getArtist() ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }

                IconButton(onClick = { player.skipPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                }

                IconButton(onClick = { player.togglePlayPause() }) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause"
                    )
                }

                IconButton(onClick = { player.skipNext() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next")
                }
            }
        }
    }
}
