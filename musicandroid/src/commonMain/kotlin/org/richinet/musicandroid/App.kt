package org.richinet.musicandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import kotlinx.coroutines.launch
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.core.context.startKoin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    KoinContext {
        val audioPlayer = koinInject<AudioPlayer>()
        val playbackState by audioPlayer.playbackState.collectAsState()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        MaterialTheme(
            colorScheme = darkColorScheme()
        ) {
            Navigator(HomeScreen()) { navigator ->
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
                                    navigator.push(SearchScreen())
                                    scope.launch { drawerState.close() }
                                }
                            )
                            NavigationDrawerItem(
                                label = { Text("Mixing Board") },
                                selected = false,
                                onClick = { 
                                    navigator.push(MixerScreen())
                                    scope.launch { drawerState.close() }
                                }
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
                            if (playbackState.track != null) {
                                NowPlayingBar(playbackState, audioPlayer, navigator)
                            }
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
}

@Composable
fun NowPlayingBar(state: PlaybackState, player: AudioPlayer, navigator: Navigator) {
    Surface(
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { navigator.push(PlayerScreen()) }
    ) {
        Column {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.track?.trackName ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                    Text(
                        text = state.track?.getArtist() ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
                IconButton(onClick = { player.togglePlayPause() }) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause"
                    )
                }
            }
        }
    }
}

// Function to start Koin outside Composable if needed (e.g. from Platform Application)
fun initKoin(baseUrl: String, playlistSync: PlaylistSync, audioPlayer: AudioPlayer) {
    startKoin {
        modules(createCommonModule(baseUrl, playlistSync, audioPlayer))
    }
}
