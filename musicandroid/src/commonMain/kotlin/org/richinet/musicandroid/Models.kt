package org.richinet.musicandroid

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class TrackFile(
    val fileId: Long,
    val fileName: String,
    val duration: Double
)

@Serializable
data class Track(
    val trackId: Long,
    val trackName: String,
    val files: List<TrackFile> = emptyList(),
    val metadata: Map<String, JsonElement> = emptyMap()
) {
    fun getArtist(): String {
        val artist = metadata["Artist"] ?: metadata["artist"]
        return artist?.jsonPrimitive?.content ?: ""
    }

    fun getAlbum(): String {
        val album = metadata["Album"] ?: metadata["album"]
        return album?.jsonPrimitive?.content ?: ""
    }
}

@Serializable
data class Tag(
    val tagId: Long,
    val tagName: String,
    val tagTypeId: Long,
    val tagTypeName: String,
    val tagTypeEdit: String? = null
)

@Serializable
data class TagUsage(
    val typeName: String,
    val tagName: String,
    val count: Int,
    val tagId: Long
)

@Serializable
data class FilterRequest(
    val mustHaveIds: List<Long>,
    val canHaveIds: List<Long>,
    val mustNotHaveIds: List<Long>
)

@Serializable
data class CreateTagRequest(
    val tagType: String,
    val tagName: String,
    val trackIds: List<Long> = emptyList()
)

@Serializable
data class BackendVersionInfo(
    val version: String,
    val totalTrackCount: Double,
    val buildTime: String,
    val runtime: String,
    val runtimeVersion: String,
    val environment: String,
    val musicDirectory: String,
    val dbConnected: Boolean,
    val dbUrl: String? = null,
    val dbUser: String? = null,
    val dbError: String? = null
)
