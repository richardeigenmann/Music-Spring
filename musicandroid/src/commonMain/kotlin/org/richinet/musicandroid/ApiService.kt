package org.richinet.musicandroid

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

class ApiService(private val settingsRepository: SettingsRepository) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    private val _baseUrl = MutableStateFlow("http://octan:8011")
    val baseUrlFlow: StateFlow<String> = _baseUrl.asStateFlow()
    val baseUrl: String get() = _baseUrl.value

    init {
        CoroutineScope(Dispatchers.Main).launch {
            settingsRepository.baseUrl.collectLatest {
                _baseUrl.value = it
            }
        }
    }

    suspend fun getVersion(): BackendVersionInfo = client.get("${baseUrl}/api/version").body()

    suspend fun getTags(): List<Tag> = client.get("${baseUrl}/api/tags").body()

    suspend fun getTrack(trackId: Long): Track {
        val jsonObject = client.get("${baseUrl}/api/track/$trackId").body<JsonObject>()
        return parseTrack(jsonObject)
    }

    suspend fun saveTrack(track: Track): Track {
        val jsonObject = buildJsonObject {
            put("trackId", track.trackId)
            put("trackName", track.trackName)
            put("files", Json.encodeToJsonElement(track.files))
            track.metadata.forEach { (key, value) ->
                put(key, value)
            }
        }
        val response = client.post("${baseUrl}/api/track/${track.trackId}") {
            contentType(ContentType.Application.Json)
            setBody(jsonObject)
        }.body<JsonObject>()
        return parseTrack(response)
    }

    suspend fun getTracksByTag(tagId: Long): List<Track> {
        val jsonList = client.get("${baseUrl}/api/tracksByTag/$tagId").body<List<JsonObject>>()
        return jsonList.map { parseTrack(it) }
    }

    suspend fun searchTracks(query: String): List<Track> {
        val jsonList = client.get("${baseUrl}/api/trackSearch") {
            parameter("query", query)
        }.body<List<JsonObject>>()
        return jsonList.map { parseTrack(it) }
    }

    suspend fun getTagUsageStats(): List<TagUsage> = client.get("${baseUrl}/api/stats/tagUsage").body()

    suspend fun filterTracks(
        mustHaveIds: List<Long>,
        canHaveIds: List<Long>,
        mustNotHaveIds: List<Long>
    ): List<Track> {
        val request = FilterRequest(mustHaveIds, canHaveIds, mustNotHaveIds)
        val jsonList = client.post("${baseUrl}/api/filterTracks") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<List<JsonObject>>()
        return jsonList.map { parseTrack(it) }
    }

    suspend fun createTag(tagType: String, tagName: String, trackIds: List<Long> = emptyList()): Tag {
        val request = CreateTagRequest(tagType, tagName, trackIds)
        return client.post("${baseUrl}/api/tags") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    private fun parseTrack(jsonObject: JsonObject): Track {
        val trackId = jsonObject["trackId"]?.jsonPrimitive?.longOrNull ?: 0L
        val trackName = jsonObject["trackName"]?.jsonPrimitive?.content ?: ""
        val filesJson = jsonObject["files"]?.let {
            Json.decodeFromJsonElement<List<TrackFile>>(it)
        } ?: emptyList()

        // Extract everything else as metadata
        val metadata = jsonObject.filter { it.key !in listOf("trackId", "trackName", "files") }

        return Track(trackId, trackName, filesJson, metadata)
    }

    fun getStreamUrl(fileId: Long): String = "${baseUrl}/api/trackFile/$fileId"

    fun getTrackImageUrl(fileId: Long): String = "${baseUrl}/api/trackFileImage/$fileId"
}
