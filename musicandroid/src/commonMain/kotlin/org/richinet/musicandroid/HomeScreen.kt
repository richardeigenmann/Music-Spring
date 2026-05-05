package org.richinet.musicandroid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data object HomeScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<TrackViewModel>()
        val audioPlayer = koinInject<AudioPlayer>()
        val apiService = koinInject<ApiService>()
        val tagsState by viewModel.tags.collectAsState()
        val currentBaseUrl by apiService.baseUrlFlow.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

        // Refresh tags when baseUrl changes
        LaunchedEffect(currentBaseUrl) {
            viewModel.refresh()
        }

        val isRefreshing = viewModel.tags.collectAsState().value is UiState.Loading

        Scaffold { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                when (val state = tagsState) {
                    is UiState.Loading -> {
                        if (!isRefreshing) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    else -> {}
                }

                if (tagsState is UiState.Success || tagsState is UiState.Loading || tagsState is UiState.Error) {
                    PullToRefreshBox(
                        isRefreshing = tagsState is UiState.Loading,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (tagsState is UiState.Success) {
                            val state = tagsState as UiState.Success
                            val filteredTags = state.data.filter { it.tagTypeEdit == "S" || it.tagTypeName.lowercase() == "playlist" }
                            val grouped = filteredTags.groupBy { it.tagTypeName }
                            val sortedTypes = grouped.keys.sortedWith { a, b ->
                                val pA = getPriority(a)
                                val pB = getPriority(b)
                                if (pA != pB) pA - pB else a.compareTo(b)
                            }

                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                sortedTypes.forEach { typeName ->
                                    item(key = typeName) {
                                        Text(
                                            text = typeName,
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    val typeTags = grouped[typeName]?.sortedBy { it.tagName } ?: emptyList()

                                    items(typeTags, key = { it.tagId }) { tag ->
                                        ListItem(
                                            headlineContent = { Text(tag.tagName) },
                                            modifier = Modifier.clickable {
                                                navigator.push(TrackListScreen(tag.tagId, tag.tagName))
                                            },
                                            trailingContent = {
                                                IconButton(onClick = {
                                                    scope.launch {
                                                        try {
                                                            val tracks = apiService.getTracksByTag(tag.tagId)
                                                            audioPlayer.playPlaylist(tracks, tag.tagName)
                                                        } catch (e: Exception) {
                                                            // handle error
                                                        }
                                                    }
                                                }) {
                                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play ${tag.tagName}")
                                                }
                                            }
                                        )
                                    }

                                    item {
                                        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                                    }
                                }
                            }
                        } else if (tagsState is UiState.Error) {
                            val state = tagsState as UiState.Error
                            Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Error: ${state.message}",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else if (tagsState is UiState.Loading) {
                             // Empty content for PullToRefreshBox while initial loading
                             Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }

    private fun getPriority(typeName: String): Int {
        val lower = typeName.lowercase().trim()
        return when {
            lower == "playlist" -> 0
            lower.contains("playlist") -> 1
            else -> 10
        }
    }
}
