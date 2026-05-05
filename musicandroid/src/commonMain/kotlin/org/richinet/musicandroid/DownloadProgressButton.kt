package org.richinet.musicandroid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DownloadProgressButton(
    isCached: Boolean,
    progress: Float, // 0.0 to 1.0
    isDownloading: Boolean,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Int = 24
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        if (isDownloading && !isCached) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size((iconSize + 8).dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
        
        IconButton(onClick = onDownloadClick) {
            Icon(
                imageVector = if (isCached) Icons.Default.CheckCircle else Icons.Default.Download,
                contentDescription = if (isCached) "Cached" else "Download",
                modifier = Modifier.size(iconSize.dp),
                tint = if (isCached) Color(0xFF4CAF50) else LocalContentColor.current
            )
        }
    }
}
