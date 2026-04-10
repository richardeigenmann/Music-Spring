package org.richinet.musicandroid

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue

data class DownloadTask(
    val trackId: Long,
    val fileId: Long,
    val fileName: String
)

class DownloadQueueManager {
    private val queue = ConcurrentLinkedQueue<DownloadTask>()
    private val _enqueuedIds = MutableStateFlow<Set<Long>>(emptySet())
    val enqueuedIds = _enqueuedIds.asStateFlow()

    private val _activeCount = MutableStateFlow(0)
    val activeCount = _activeCount.asStateFlow()

    fun enqueue(tracks: List<Track>) {
        val tasks = tracks.mapNotNull { track ->
            val file = track.files.firstOrNull() ?: return@mapNotNull null
            DownloadTask(track.trackId, file.fileId, file.fileName)
        }
        queue.addAll(tasks)
        updateIds()
    }

    fun next(): DownloadTask? {
        val task = queue.poll()
        updateIds()
        return task
    }

    fun markActive(active: Boolean) {
        if (active) _activeCount.value++ else _activeCount.value--
    }

    private fun updateIds() {
        _enqueuedIds.value = queue.map { it.trackId }.toSet()
    }

    fun isEmpty() = queue.isEmpty()
}
