package org.richinet.musicandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class MixerScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<TrackViewModel>()
        val apiService = koinInject<ApiService>()
        val audioPlayer = koinInject<AudioPlayer>()
        val scope = rememberCoroutineScope()
        
        val tagsState by viewModel.tags.collectAsState()
        val filteredTracks by viewModel.filteredTracks.collectAsState()
        
        var mustHave by remember { mutableStateOf(setOf<Tag>()) }
        var canHave by remember { mutableStateOf(setOf<Tag>()) }
        var mustNotHave by remember { mutableStateOf(setOf<Tag>()) }
        
        var showSaveDialog by remember { mutableStateOf(false) }
        var playlistName by remember { mutableStateOf("") }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Mixing Board") },
                    actions = {
                        IconButton(onClick = {
                            viewModel.filterTracks(
                                mustHave.map { it.tagId },
                                canHave.map { it.tagId },
                                mustNotHave.map { it.tagId }
                            )
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Filter")
                        }
                        IconButton(onClick = { showSaveDialog = true }) {
                            Icon(Icons.Default.Save, contentDescription = "Save Playlist")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // Buckets
                BucketSection("MUST HAVE (AND)", mustHave, Color(0xFF2E7D32)) { mustHave = mustHave - it }
                BucketSection("CAN HAVE (OR)", canHave, Color(0xFF1565C0)) { canHave = canHave - it }
                BucketSection("MUST NOT HAVE (NOT)", mustNotHave, Color(0xFFC62828)) { mustNotHave = mustNotHave - it }
                
                HorizontalDivider()
                
                // Available Tags
                Text("Available Tags", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.titleSmall)
                Box(modifier = Modifier.weight(1f)) {
                    when (val state = tagsState) {
                        is UiState.Success -> {
                            val available = state.data.filter { 
                                it.tagTypeEdit == "S" && 
                                it !in mustHave && it !in canHave && it !in mustNotHave 
                            }
                            LazyColumn {
                                items(available) { tag ->
                                    ListItem(
                                        headlineContent = { Text(tag.tagName) },
                                        supportingContent = { Text(tag.tagTypeName) },
                                        trailingContent = {
                                            Row {
                                                IconButton(onClick = { mustHave = mustHave + tag }) { 
                                                    Text("M", color = Color(0xFF2E7D32)) 
                                                }
                                                IconButton(onClick = { canHave = canHave + tag }) { 
                                                    Text("C", color = Color(0xFF1565C0)) 
                                                }
                                                IconButton(onClick = { mustNotHave = mustNotHave + tag }) { 
                                                    Text("N", color = Color(0xFFC62828)) 
                                                }
                                            }
                                        }
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                        else -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
                
                // Results Preview
                HorizontalDivider()
                Text("Results Preview", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.titleSmall)
                Box(modifier = Modifier.weight(1f)) {
                    when (val state = filteredTracks) {
                        is UiState.Success -> {
                            LazyColumn {
                                items(state.data) { track ->
                                    ListItem(
                                        headlineContent = { Text(track.trackName) },
                                        supportingContent = { Text(track.getArtist()) },
                                        modifier = Modifier.clickable { audioPlayer.playTrack(track) }
                                    )
                                }
                            }
                        }
                        is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        else -> {}
                    }
                }
            }
        }

        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Save Playlist") },
                text = {
                    TextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        placeholder = { Text("Playlist Name") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val tracks = (filteredTracks as? UiState.Success)?.data ?: emptyList()
                        if (tracks.isNotEmpty() && playlistName.isNotBlank()) {
                            scope.launch {
                                try {
                                    apiService.createTag("Playlist", playlistName, tracks.map { it.trackId })
                                    showSaveDialog = false
                                    playlistName = ""
                                    viewModel.loadTags() // Refresh tags
                                } catch (e: Exception) {
                                    // Handle error
                                }
                            }
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
                }
            )
        }
    }

    @Composable
    private fun BucketSection(label: String, tags: Set<Tag>, color: Color, onRemove: (Tag) -> Unit) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            LazyRow(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                items(tags.toList()) { tag ->
                    SuggestionChip(
                        onClick = { onRemove(tag) },
                        label = { Text(tag.tagName) },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        }
    }
}
