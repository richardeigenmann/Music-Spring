package org.richinet.musicandroid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

data object HomeScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<TrackViewModel>()
        val tagsState by viewModel.tags.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Scaffold { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                when (val state = tagsState) {
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Success -> {
                        val tags = state.data
                        val grouped = tags.groupBy { it.tagTypeName }
                        val sortedTypes = grouped.keys.sortedWith { a, b ->
                            val pA = getPriority(a, tags)
                            val pB = getPriority(b, tags)
                            if (pA != pB) pA - pB else a.compareTo(b)
                        }

                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(sortedTypes) { typeName ->
                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text(
                                        text = typeName,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    val typeTags = grouped[typeName]?.sortedBy { it.tagName } ?: emptyList()

                                    // Use FlowRow or similar for a "pill" look, or just a Row for now if small
                                    // For simplicity and matching the "list" feel but grouped:
                                    typeTags.forEach { tag ->
                                        ListItem(
                                            headlineContent = { Text(tag.tagName) },
                                            modifier = Modifier.clickable {
                                                navigator.push(TrackListScreen(tag.tagId, tag.tagName))
                                            }
                                        )
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                                }
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

    private fun getPriority(typeName: String, allTags: List<Tag>): Int {
        val lower = typeName.lowercase().trim()
        val sampleTag = allTags.find { it.tagTypeName == typeName }
        val editType = sampleTag?.tagTypeEdit ?: "T"

        return when {
            lower == "playlist" -> 0
            lower.contains("playlist") -> 1
            editType == "T" -> 10 // Based on user: "T" are preferred over "S" which are plentyful
            editType == "S" -> 100
            else -> 50
        }
    }
}
