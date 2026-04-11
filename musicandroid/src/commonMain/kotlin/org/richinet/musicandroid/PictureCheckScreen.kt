package org.richinet.musicandroid

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data object PictureCheckScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val apiService = koinInject<ApiService>()
        val pictureChecker = koinInject<PictureChecker>()
        val scope = rememberCoroutineScope()

        var isChecking by remember { mutableStateOf(false) }
        var progress by remember { mutableStateOf(0f) }
        var currentTrackName by remember { mutableStateOf("") }
        var missingPictures by remember { mutableStateOf<List<String>>(emptyList()) }
        var totalChecked by remember { mutableStateOf(0) }
        var lastCheckedIndex by remember { mutableStateOf(0) }
        var statusMessage by remember { mutableStateOf("Ready to check tracks") }
        var showPermissionRequest by remember { mutableStateOf(false) }
        var localFiles by remember { mutableStateOf<List<String>>(emptyList()) }

        fun runCheck(resume: Boolean = false) {
            scope.launch {
                isChecking = true
                try {
                    if (!resume) {
                        statusMessage = "Scanning local storage..."
                        localFiles = pictureChecker.getLocalFiles()
                        missingPictures = emptyList()
                        lastCheckedIndex = 0
                    }
                    
                    if (localFiles.isEmpty()) {
                        statusMessage = "No files found in local storage."
                    } else {
                        statusMessage = "Checking ${localFiles.size} local files..."
                        val startIndex = if (resume) lastCheckedIndex else 0
                        val results = pictureChecker.checkLocalFiles(localFiles, startIndex) { current, total, name ->
                            progress = current.toFloat() / total.toFloat()
                            currentTrackName = name
                            totalChecked = total
                            lastCheckedIndex = current - 1
                        }
                        missingPictures = missingPictures + results
                        
                        if (lastCheckedIndex >= localFiles.size - 1) {
                            statusMessage = "Check complete. Found ${missingPictures.size} issues."
                        } else {
                            statusMessage = "Check stalled at ${lastCheckedIndex + 1}/${localFiles.size}. You can try to Resume."
                        }
                    }
                } catch (e: Exception) {
                    statusMessage = "Error: ${e.message}"
                } finally {
                    isChecking = false
                }
            }
        }

        if (showPermissionRequest) {
            RequestPermissions(
                onGranted = {
                    showPermissionRequest = false
                    runCheck(resume = false)
                },
                onDenied = {
                    showPermissionRequest = false
                    statusMessage = "Error: Permission denied. Cannot read local files."
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Check Pictures") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                if (isChecking) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Checking (${lastCheckedIndex + 1}/${localFiles.size}): $currentTrackName",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showPermissionRequest = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("New Check")
                        }
                        
                        if (localFiles.isNotEmpty() && lastCheckedIndex < localFiles.size - 1) {
                            Button(
                                onClick = { runCheck(resume = true) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("Resume")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (missingPictures.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "The following files have missing or unreadable pictures. These are also logged to Logcat with prefix 'MISSING_PICTURE:'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(missingPictures) { fileName ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = fileName,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                if (pictureChecker.deleteLocalFile(fileName)) {
                                                    missingPictures = missingPictures.filter { it != fileName }
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete File",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (!isChecking && totalChecked > 0) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("All local tracks have readable pictures!")
                    }
                }
            }
        }
    }
}
