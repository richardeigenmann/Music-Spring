package org.richinet.musicandroid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import org.koin.compose.koinInject

data class TrackEditScreen(val trackId: Long) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val apiService = koinInject<ApiService>()
        val audioPlayer = koinInject<AudioPlayer>()
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

        var track by remember { mutableStateOf<Track?>(null) }
        var allTags by remember { mutableStateOf<List<Tag>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(trackId) {
            isLoading = true
            try {
                track = apiService.getTrack(trackId)
                allTags = apiService.getTags()
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar("Error loading track: ${e.message}") }
            } finally {
                isLoading = false
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Edit Track") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            track?.let { audioPlayer.playPlaylist(listOf(it), it.trackName) }
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                        }
                        IconButton(onClick = {
                            track?.let {
                                scope.launch {
                                    try {
                                        apiService.saveTrack(it)
                                        snackbarHostState.showSnackbar("Track saved")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Save failed: ${e.message}")
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (track != null) {
                TrackEditorContent(
                    modifier = Modifier.padding(paddingValues),
                    track = track!!,
                    allTags = allTags,
                    onTrackChanged = { track = it },
                    apiService = apiService
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrackEditorContent(
    modifier: Modifier,
    track: Track,
    allTags: List<Tag>,
    onTrackChanged: (Track) -> Unit,
    apiService: ApiService
) {
    val editableArrayFields = listOf("Artist", "Composer", "Media Name", "Original Artist")
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = track.trackName,
                onValueChange = { onTrackChanged(track.copy(trackName = it)) },
                label = { Text("Track Name") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(editableArrayFields) { field ->
            ArrayFieldEditor(
                label = field,
                values = getTrackFieldAsArray(track, field),
                onValuesChanged = { newValues ->
                    val newMetadata = track.metadata.toMutableMap()
                    newMetadata[field] = JsonArray(newValues.map { JsonPrimitive(it) })
                    onTrackChanged(track.copy(metadata = newMetadata))
                }
            )
        }

        item {
            Text("Tags", style = MaterialTheme.typography.titleMedium)
        }

        val trackTags = getTrackSelectionTags(track, allTags)
        val availableTags = getAvailableTags(trackTags, allTags)

        items(trackTags.entries.toList()) { (type, tags) ->
            Column {
                Text(type, style = MaterialTheme.typography.labelLarge)
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach { tag ->
                        InputChip(
                            selected = true,
                            onClick = {
                                removeTagFromTrack(track, tag, onTrackChanged)
                            },
                            label = { Text(tag.tagName) },
                            trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        }

        item {
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Add Tags", style = MaterialTheme.typography.titleMedium)
        }

        items(availableTags.entries.toList()) { (type, tags) ->
            Column {
                Text(type, style = MaterialTheme.typography.labelLarge)
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach { tag ->
                        SuggestionChip(
                            onClick = {
                                addTagToTrack(track, tag, onTrackChanged)
                            },
                            label = { Text(tag.tagName) }
                        )
                    }
                    var showCreateDialog by remember { mutableStateOf(false) }
                    var newTagName by remember { mutableStateOf("") }

                    AssistChip(
                        onClick = { showCreateDialog = true },
                        label = { Text("New...") },
                        leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) }
                    )

                    if (showCreateDialog) {
                        AlertDialog(
                            onDismissRequest = { showCreateDialog = false },
                            title = { Text("Create Tag for $type") },
                            text = {
                                TextField(value = newTagName, onValueChange = { newTagName = it })
                            },
                            confirmButton = {
                                Button(onClick = {
                                    scope.launch {
                                        try {
                                            val newTag = apiService.createTag(type, newTagName)
                                            addTagToTrack(track, newTag, onTrackChanged)
                                            showCreateDialog = false
                                            newTagName = ""
                                        } catch (e: Exception) {}
                                    }
                                }) { Text("Create") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArrayFieldEditor(label: String, values: List<String>, onValuesChanged: (List<String>) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        values.forEachIndexed { index, value ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { newValue ->
                        val newValues = values.toMutableList()
                        newValues[index] = newValue
                        onValuesChanged(newValues)
                    },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    val newValues = values.toMutableList()
                    newValues.removeAt(index)
                    onValuesChanged(newValues)
                }) {
                    Icon(Icons.Default.Delete, null)
                }
            }
        }
        TextButton(onClick = { onValuesChanged(values + "") }) {
            Icon(Icons.Default.Add, null)
            Text("Add $label")
        }
    }
}

fun getTrackFieldAsArray(track: Track, field: String): List<String> {
    val element = track.metadata[field] ?: return emptyList()
    return when (element) {
        is JsonArray -> element.map { it.jsonPrimitive.content }
        is JsonPrimitive -> if (element.content.isEmpty()) emptyList() else listOf(element.content)
        else -> emptyList()
    }
}

fun getTrackSelectionTags(track: Track, allTags: List<Tag>): Map<String, List<Tag>> {
    val result = mutableMapOf<String, MutableList<Tag>>()
    track.metadata.forEach { (key, value) ->
        val tagNames = when (value) {
            is JsonArray -> value.map { it.jsonPrimitive.content }
            is JsonPrimitive -> listOf(value.content)
            else -> emptyList()
        }
        tagNames.forEach { name ->
            val tag = allTags.find { it.tagTypeName == key && it.tagName == name && it.tagTypeEdit == "S" }
            if (tag != null) {
                result.getOrPut(key) { mutableListOf() }.add(tag)
            }
        }
    }
    return result
}

fun getAvailableTags(currentTrackTags: Map<String, List<Tag>>, allTags: List<Tag>): Map<String, List<Tag>> {
    val result = mutableMapOf<String, MutableList<Tag>>()
    allTags.filter { it.tagTypeEdit == "S" }.forEach { tag ->
        val currentInType = currentTrackTags[tag.tagTypeName] ?: emptyList()
        if (currentInType.none { it.tagId == tag.tagId }) {
            result.getOrPut(tag.tagTypeName) { mutableListOf() }.add(tag)
        }
    }
    return result
}

fun addTagToTrack(track: Track, tag: Tag, onTrackChanged: (Track) -> Unit) {
    val newMetadata = track.metadata.toMutableMap()
    val type = tag.tagTypeName
    val name = tag.tagName
    val existing = newMetadata[type]

    val newValues = when (existing) {
        is JsonArray -> (existing.map { it.jsonPrimitive.content } + name).distinct()
        is JsonPrimitive -> listOf(existing.content, name).distinct()
        else -> listOf(name)
    }

    newMetadata[type] = if (newValues.size == 1) JsonPrimitive(newValues[0]) else JsonArray(newValues.map { JsonPrimitive(it) })
    onTrackChanged(track.copy(metadata = newMetadata))
}

fun removeTagFromTrack(track: Track, tag: Tag, onTrackChanged: (Track) -> Unit) {
    val newMetadata = track.metadata.toMutableMap()
    val type = tag.tagTypeName
    val name = tag.tagName
    val existing = newMetadata[type]

    val newValues = when (existing) {
        is JsonArray -> existing.map { it.jsonPrimitive.content }.filter { it != name }
        is JsonPrimitive -> if (existing.content == name) emptyList() else listOf(existing.content)
        else -> emptyList()
    }

    if (newValues.isEmpty()) {
        newMetadata.remove(type)
    } else if (newValues.size == 1) {
        newMetadata[type] = JsonPrimitive(newValues[0])
    } else {
        newMetadata[type] = JsonArray(newValues.map { JsonPrimitive(it) })
    }
    onTrackChanged(track.copy(metadata = newMetadata))
}
